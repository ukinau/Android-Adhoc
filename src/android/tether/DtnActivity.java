package android.tether;

import java.util.ArrayList;
import java.util.List;
import it.sephiroth.demo.slider.widget.MultiDirectionSlidingDrawer;
import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.tether.dtn.DtnMessage;
import android.tether.dtn.DtnMessageArrayAdapter;
import android.tether.dtn.FetchModeThread;
import android.tether.dtn.algorithm.DtnBase;
import android.tether.dtn.algorithm.DtnFlattingOnlyUdpBroadCast;
import android.tether.dtn.sensor.DtnAccelerateSensorEvent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
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
	private DtnBase dtnImplement;
	private FetchModeThread fetchModeThread;
	private SensorManager accelManager;
	private DtnAccelerateSensorEvent accelEvent;


	private MultiDirectionSlidingDrawer mDrawerBottom;
	private TextView dtn_status_myMode;
	private Button startButton;
	private Button stopButton;
	private Button changeToRescueButton;
	
	
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

	private DtnBase getDtnImplement(int mode, TetherApplication app, Handler handle) {
		DtnBase dtnImplement = new DtnFlattingOnlyUdpBroadCast(mode, app, handle);
		return dtnImplement;
	}

	public void onCreate(Bundle savedInstanced) {
		super.onCreate(savedInstanced);
		setContentView(R.layout.dtn_content);
		// Init Application
		this.application = (TetherApplication) this.getApplication();
		// Set diplay  adding Message 
		bottomSlidingDrawerEventRegist();
		bottonEventRegist();
		this.dtn_status_myMode = (TextView)findViewById(R.id.dtn_status_my_mode);
		
		this.accelManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		this.accelEvent = new DtnAccelerateSensorEvent();
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
		// Listenerの登録
		List<Sensor> sensors = accelManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if(sensors.size() > 0) {
			Sensor s = sensors.get(0);
			accelManager.registerListener(this.accelEvent, s, SensorManager.SENSOR_DELAY_UI);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d(MSG_TAG, "onPause");
		stopDtnAlgorithm();
		accelManager.unregisterListener(this.accelEvent);
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

	private boolean startDtnAlgorithm() {
		Log.d(MSG_TAG, "Start Dtn Algorithm");
		if(this.dtnImplement == null){
			TextView dtn_status = (TextView)findViewById(R.id.dtn_status);
			dtn_status.setText("DTNステータス：アルゴリズムスタート");
			// Initialize listView
			ListView messageBoxList = (ListView) findViewById(R.id.dtn_message_box_listView);
			dtnMsgBoxAdapter = new DtnMessageArrayAdapter(this, R.layout.dtnmessages_list_cell ,new ArrayList<DtnMessage>(),getResources()); 
			messageBoxList.setAdapter(this.dtnMsgBoxAdapter);
	
			DtnActivity.this.dtnImplement = getDtnImplement( DtnBase.MODE_CAN_MOVE,
					DtnActivity.this.application, DtnActivity.this.messageBoxListHandler);
		
			Thread thread = new Thread(new Runnable() {
				public void run() {
					DtnActivity.this.dtnImplement.start();
				}
			});
			thread.start();
			this.fetchModeThread = new FetchModeThread(this.fetchModeHandler, this.dtnImplement, 5);
			this.fetchModeThread.start();
			return true;
		}
		return false;
	}

	private void stopDtnAlgorithm() {
		Log.d(MSG_TAG, "Stop Dtn Algorithm");
		TextView dtn_status = (TextView)findViewById(R.id.dtn_status);
		dtn_status.setText("DTNステータス： アルゴリズムストップ");
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
	}

	private void changeModeToNeedRescue(){
		if(DtnActivity.this.dtnImplement != null){
			DtnMessage myMsg = DtnActivity.this.application.getMyRescueMessage();
			DtnActivity.this.dtnImplement .setTargetMsg(myMsg);
			DtnActivity.this.dtnImplement.changeModeTo(DtnBase.MODE_NEED_RESCUE);
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
