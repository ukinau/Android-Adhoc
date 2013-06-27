package android.tether;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.tether.dtn.DtnBase;
import android.tether.dtn.DtnFlattingOnlyUdpBroadCast;
import android.tether.dtn.DtnMessage;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DtnActivity extends Activity {
	
	public static final String MSG_TAG = "TETHER -> DtnAcitivity";
	private TetherApplication application;
	private ArrayAdapter<String> dtnMsgBoxAdapter;
	private DtnBase dtnImplement;
	
	// to deal with UI by other thread
	private final Handler  messageBoxListHandler=new Handler() {
		 public void handleMessage(Message msg) {
			 Bundle data=msg.getData();
			 String body = data.getString("msg");
	  		 Toast.makeText(DtnActivity.this, body , Toast.LENGTH_LONG).show();
	  		 dtnMsgBoxAdapter.add(body);
	  		 dtnMsgBoxAdapter.notifyDataSetChanged();
		 }
	};
	private DtnBase getDtnImplement(int mode,TetherApplication app,Handler handle){
		DtnBase dtnImplement = new DtnFlattingOnlyUdpBroadCast( mode, app, handle);
		return dtnImplement;
	 }
	
	public void onCreate(Bundle savedInstanced){
		super.onCreate(savedInstanced);
		setContentView(R.layout.dtn_content);
		// Init Application
        this.application = (TetherApplication)this.getApplication();
        
        ListView messageBoxList = (ListView)findViewById(R.id.dtn_message_box_listView);
        dtnMsgBoxAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
	   	messageBoxList.setAdapter(this.dtnMsgBoxAdapter);
	   	
        Button setButton = (Button)findViewById(R.id.setTargetText);
        setButton.setOnClickListener(new OnClickListener(){
			public void onClick(View paramView) {
				DtnMessage dtnMessage = new DtnMessage();
				EditText name = (EditText)findViewById(R.id.target_name);
				EditText address = (EditText)findViewById(R.id.target_address);
				EditText facebook = (EditText)findViewById(R.id.target_facebook);
				dtnMessage.name = name.getText().toString();
				dtnMessage.address = address.getText().toString(); 
				dtnMessage.facebook = facebook.getText().toString(); 
				dtnMessage.mac_address = DtnActivity.this.application.getMacAddress();
				if(DtnActivity.this.dtnImplement != null){
					DtnActivity.this.dtnImplement.setTargetMsg(dtnMessage);
					DtnActivity.this.dtnImplement.rescueMode = DtnBase.MODE_NEED_RESCUE;
				}
			}
        });
        
	}
	
	@Override
	  public void onStart(){
		super.onStart();
		Log.d(MSG_TAG,"onStart");
	  }
	  @Override
	  public void onResume(){
		super.onResume();
		Log.d(MSG_TAG,"onResume");
		startDtnAlgorithm();
	  }
	  @Override
	  public void onPause(){
		super.onPause();
		Log.d(MSG_TAG,"onPause");
		stopDtnAlgorithm();
	  }
	  @Override
	  public void onStop(){
		super.onStop();
		Log.d(MSG_TAG,"onStop");
	  }
	
	  public void onDestory(){
		  super.onDestroy();
		  Log.d(MSG_TAG,"onDestroy");
	  }
	  
	 private void startDtnAlgorithm(){
		 Log.d(MSG_TAG,"Start Dtn Algorithm");
		 // Initialize listView
		 ListView messageBoxList = (ListView)findViewById(R.id.dtn_message_box_listView);
	     dtnMsgBoxAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		 messageBoxList.setAdapter(this.dtnMsgBoxAdapter);
		 
		 Thread thread = new Thread(new Runnable(){
				public void run() {
					DtnActivity.this.dtnImplement = getDtnImplement(DtnBase.MODE_CAN_MOVE,
							DtnActivity.this.application,DtnActivity.this.messageBoxListHandler);
					DtnActivity.this.dtnImplement.start();
				}
				
			});
		thread.start();
	 }
	 private void stopDtnAlgorithm(){
		Log.d(MSG_TAG,"Stop Dtn Algorithm");
		this.dtnImplement.executeStatus = false;
		this.dtnImplement.stop();
	}
	 

}
