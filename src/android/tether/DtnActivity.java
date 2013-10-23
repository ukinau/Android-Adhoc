package android.tether;

import java.util.ArrayList;
import java.util.List;
import it.sephiroth.demo.slider.widget.MultiDirectionSlidingDrawer;
import android.app.Activity;
import android.app.AlertDialog;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.http.SslCertificate.DName;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.tether.dtn.DtnMessage;
import android.tether.dtn.DtnMessageArrayAdapter;
import android.tether.dtn.FetchModeThread;
import android.tether.dtn.algorithm.DtnBaseAlgorithm;
import android.tether.dtn.algorithm.DtnFlattingOnlyUdpBroadCastSample;
import android.tether.dtn.algorithm.ctl_direction_udp.CtlDirectionUdpAlgorithm;
import android.tether.dtn.algorithm.spray_and_wait.SprayAndWaitAlgorithm;
import android.tether.dtn.sensor.DtnAccelerateSensorEvent;
import android.tether.dtn.sensor.DtnMagnetismSensorEvent;
import android.tether.dtn.sensor.FetchShakeAlgorithm;
import android.tether.dtn.sensor.ShookBehaverInterface;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DtnActivity extends Activity {

	public static final String MSG_TAG = "TETHER -> DtnAcitivity";
	private TetherApplication application;
	private DtnMessageArrayAdapter dtnMsgBoxAdapter;
	private DtnBaseAlgorithm dtnImplement;
	private FetchModeThread fetchModeThread;
	private SensorManager sensorManager;
	private DtnAccelerateSensorEvent accelEvent;
	private DtnMagnetismSensorEvent magenetismEvent;


	private MultiDirectionSlidingDrawer mDrawerBottom;
	private TextView dtn_status_myMode;
	private Button startButton;
	private Button stopButton;
	private Button changeToRescueButton;
	private TextView algorithmKind;
	private AlertDialog dig = null;
	private List<String> algorithmSet = new ArrayList<String>();
	private boolean startAlgorithmFlg = false;
	
	// to deal with UI by other thread
	private final Handler messageBoxListHandler = new Handler() {
		public void handleMessage(Message msg) {
			Toast.makeText(DtnActivity.this, "受信", Toast.LENGTH_LONG).show();
			dtnMsgBoxAdapter.add((DtnMessage)msg.obj);
			dtnMsgBoxAdapter.notifyDataSetChanged();
		}
	};
	
	private final Handler fetchModeHandler = new Handler(){
		public void handleMessage(Message msg){
			Bundle data = msg.getData();
			String mode = data.getString("mode");
			String base = "モード：";
			dtn_status_myMode.setText(base+mode);
		}
	};
	
	private void initAlgorithmSet(){
		this.algorithmSet = new ArrayList<String>();
		this.algorithmSet.add(DtnFlattingOnlyUdpBroadCastSample.toSTRING);
		this.algorithmSet.add(SprayAndWaitAlgorithm.toSTRING);
		this.algorithmSet.add(CtlDirectionUdpAlgorithm.toSTRING);
	}
	
	private void setDtnImplement(String dtnAlgorithm){
		if(DtnFlattingOnlyUdpBroadCastSample.toSTRING.equals(dtnAlgorithm)){
			this.dtnImplement = new DtnFlattingOnlyUdpBroadCastSample(DtnBaseAlgorithm.MODE_CAN_MOVE, this.application, this.messageBoxListHandler);
			this.algorithmKind.setText("DTNアルゴリズム:"+dtnAlgorithm);
		} else if(SprayAndWaitAlgorithm.toSTRING.equals(dtnAlgorithm)){
			this.dtnImplement = new SprayAndWaitAlgorithm(DtnBaseAlgorithm.MODE_CAN_MOVE, this.application, this.messageBoxListHandler);
			this.algorithmKind.setText("DTNアルゴリズム:"+dtnAlgorithm);
		} else if(CtlDirectionUdpAlgorithm.toSTRING.equals(dtnAlgorithm)){
			this.dtnImplement = new CtlDirectionUdpAlgorithm(DtnBaseAlgorithm.MODE_CAN_MOVE, this.application, this.messageBoxListHandler);
			this.algorithmKind.setText("DTNアルゴリズム:"+dtnAlgorithm);
		}
		//default
		else{
			this.dtnImplement = new DtnFlattingOnlyUdpBroadCastSample(DtnBaseAlgorithm.MODE_CAN_MOVE, this.application, this.messageBoxListHandler);
			this.algorithmKind.setText("DTNアルゴリズム:"+DtnFlattingOnlyUdpBroadCastSample.toSTRING);
		}
	}

	public void onCreate(Bundle savedInstanced) {
		super.onCreate(savedInstanced);
		setContentView(R.layout.dtn_content);
		// Init Application
		this.application = (TetherApplication) this.getApplication();
		// Set diplay  adding Message 
		bottomSlidingDrawerEventRegist();
		bottonEventRegist();
		initAlgorithmSet();
		this.algorithmKind = (TextView)findViewById(R.id.dtn_algorithm_kind);
		this.dtn_status_myMode = (TextView)findViewById(R.id.dtn_status_my_mode);
		this.sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		this.accelEvent = new DtnAccelerateSensorEvent(
				new FetchShakeAlgorithm(new ShookBehaverInterface() {
					public void after_shook_behaver() {
						if(DtnActivity.this.dtnImplement != null){
							DtnActivity.this.dtnImplement.changeModeTo(DtnBaseAlgorithm.MODE_NEED_RESCUE);
						}
					}
				}));
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d(MSG_TAG, "onStart");
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(MSG_TAG, "onResume");
		this.magenetismEvent = new DtnMagnetismSensorEvent(this.application);
		// Listenerの登録
		List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if(sensors.size() > 0) {
			Sensor s = sensors.get(0);
			sensorManager.registerListener(this.accelEvent, s, SensorManager.SENSOR_DELAY_UI);
		}
		sensors = sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
		if(sensors.size() > 0){
			Sensor s = sensors.get(0);
			sensorManager.registerListener(this.magenetismEvent, s, SensorManager.SENSOR_DELAY_UI); //about 80ms cycle 
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d(MSG_TAG, "onPause");
		stopDtnAlgorithm();
		sensorManager.unregisterListener(this.accelEvent);
		sensorManager.unregisterListener(this.magenetismEvent);
		this.magenetismEvent = null;
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.d(MSG_TAG, "onStop");
	}

	public void onDestory() {
		super.onDestroy();
		Log.d(MSG_TAG, "onDestroy");
	}
	// Change Algorithm
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean ret = super.onCreateOptionsMenu(menu);
		menu.add(0 , Menu.FIRST , Menu.NONE , "アルゴリズムの変更");
		return ret;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		showDialog();
		return true;
	}
	private void showDialog(){
		final ArrayList<String> rows = new ArrayList<String>();
		for(int i=0;i<this.algorithmSet.size();i++){
			rows.add(this.algorithmSet.get(i));
		}
		ListView lv = new ListView(this);
		lv.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, rows));
		lv.setScrollingCacheEnabled(false);
		lv.setOnItemClickListener(new OnItemClickListener(){
			public void onItemClick(AdapterView<?> items, View view, int position, long id) {
				String selectedAlgorithm = rows.get(position);
				DtnActivity.this.setDtnImplement(selectedAlgorithm);
				dig.dismiss();
			}
		});
		this.dig = new AlertDialog.Builder(this)
		.setTitle("アルゴリズムを変更")
		.setPositiveButton("Cancel",null)
		.setView(lv)
		.create();
		this.dig.show();
	}
	
	private boolean startDtnAlgorithm() {
		Log.d(MSG_TAG, "Start Dtn Algorithm");
		if(this.dtnImplement != null && !startAlgorithmFlg){
			TextView dtn_status = (TextView)findViewById(R.id.dtn_status);
			dtn_status.setText("DTNステータス：アルゴリズムスタート");
			// Initialize listView
			ListView messageBoxList = (ListView) findViewById(R.id.dtn_message_box_listView);
			dtnMsgBoxAdapter = new DtnMessageArrayAdapter(this, R.layout.dtnmessages_list_cell ,new ArrayList<DtnMessage>(),getResources()); 
			messageBoxList.setAdapter(this.dtnMsgBoxAdapter);
			
			Thread thread = new Thread(new Runnable() {
				public void run() {
					DtnActivity.this.dtnImplement.start();
				}
			});
			thread.start();
			this.fetchModeThread = new FetchModeThread(this.fetchModeHandler, this.dtnImplement, 1);
			this.fetchModeThread.start();
			this.startAlgorithmFlg = true;
			return true;
		}
		return false;
	}

	private void stopDtnAlgorithm() {
		Log.d(MSG_TAG, "Stop Dtn Algorithm");
		TextView dtn_status = (TextView)findViewById(R.id.dtn_status);
		dtn_status.setText("DTNステータス： アルゴリズムストップ");
		this.algorithmKind.setText("DTNアルゴリズム: not Set");
		if(this.fetchModeThread != null){
			this.fetchModeThread.stopThread();
			this.fetchModeThread = null;
		}
		if(this.dtnImplement != null){
			this.dtnImplement.stop();
			this.dtnImplement = null;
		}
		
		// Reset listView
		ListView messageBoxList = (ListView) findViewById(R.id.dtn_message_box_listView);
		dtnMsgBoxAdapter = new DtnMessageArrayAdapter(this, R.layout.dtnmessages_list_cell , new ArrayList<DtnMessage>(),getResources());
		messageBoxList.setAdapter(this.dtnMsgBoxAdapter);
		// Reset TextView
		this.dtn_status_myMode.setText("");
		this.startAlgorithmFlg = false;
	}

	private void changeModeToNeedRescue(){
		if(DtnActivity.this.dtnImplement != null){
			DtnActivity.this.dtnImplement.changeModeTo(DtnBaseAlgorithm.MODE_NEED_RESCUE);
		}
	}
	
	private void bottonEventRegist(){
		this.startButton = (Button)findViewById(R.id.algolithm_startButton);
		this.startButton.setOnClickListener(new OnClickListener() {
			public void onClick(View paramView) {
				startDtnAlgorithm();
			}
		});
		this.stopButton = (Button)findViewById(R.id.algolithm_stopButton);
		this.stopButton.setOnClickListener(new OnClickListener() {
			public void onClick(View paramView) {
				stopDtnAlgorithm();
			}
		});
		this.changeToRescueButton = (Button)findViewById(R.id.changeToRescueMode);
		this.changeToRescueButton.setOnClickListener(new OnClickListener() {
			public void onClick(View paramView) {
				changeModeToNeedRescue();
			}
		});
	}
	
	private void bottomSlidingDrawerEventRegist() {
		// SlidingDrawer Layout to correspond scanning SSID
		mDrawerBottom = (MultiDirectionSlidingDrawer) findViewById(R.id.drawerBottom);
		mDrawerBottom
				.setOnDrawerOpenListener(new it.sephiroth.demo.slider.widget.MultiDirectionSlidingDrawer.OnDrawerOpenListener() {
					public void onDrawerOpened() {
						LinearLayout a = (LinearLayout) findViewById(R.id.main_box);
						Button c = (Button) findViewById(R.id.handle);
						c.setText("↓閉じる↓");
						a.setVisibility(View.GONE);
						
						DtnMessage myMsg = DtnActivity.this.application.getMyRescueMessage();
						final EditText name = (EditText) findViewById(R.id.target_name);
						final EditText address = (EditText) findViewById(R.id.target_address);
						final EditText facebook = (EditText) findViewById(R.id.target_facebook);
						name.setText(myMsg.name);
						address.setText(myMsg.address);
						facebook.setText(myMsg.facebook);
						
						Button setButton = (Button) findViewById(R.id.setTargetText);
						setButton.setOnClickListener(new OnClickListener() {
							public void onClick(View paramView) {
								DtnMessage dtnMessage = new DtnMessage();
								dtnMessage.name = name.getText().toString();
								dtnMessage.address = address.getText().toString();
								dtnMessage.facebook = facebook.getText().toString();
								dtnMessage.mac_address = DtnActivity.this.application.getMacAddress();
								DtnActivity.this.application.settingMyRescueMessage(dtnMessage);
							}
						});

					}
				});
		mDrawerBottom
				.setOnDrawerCloseListener(new it.sephiroth.demo.slider.widget.MultiDirectionSlidingDrawer.OnDrawerCloseListener() {
					public void onDrawerClosed() {
						LinearLayout a = (LinearLayout) findViewById(R.id.main_box);
						Button c = (Button) findViewById(R.id.handle);
						c.setText("↑レスキューメッセージをセット↑");
						a.setVisibility(View.VISIBLE);
					}
				});
	}

}
