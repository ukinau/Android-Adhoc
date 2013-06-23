package android.tether;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.tether.system.AndroidTetherConstants;
import android.tether.tcp.TcpHandleConnectionThread;
import android.tether.udp.UdpReceiveThread;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DtnActivity extends Activity {
	
	public static final String MSG_TAG = "TETHER -> DtnAcitivity";
	private TetherApplication application;
	private ArrayAdapter<String> dtnMsgBoxAdapter;
	
	
	// to deal with UI by other thread
	private final Handler  messageBoxListHandler=new Handler() {
		 public void handleMessage(Message msg) {
			 Bundle data=msg.getData();
			 String body = data.getString("msg");
			 String protocol = data.getString("protocol"); 
			 body += " :"+protocol;
	  		 Toast.makeText(DtnActivity.this, body , Toast.LENGTH_LONG).show();
	  		 dtnMsgBoxAdapter.add(body);
	  		 dtnMsgBoxAdapter.notifyDataSetChanged();
		 }
	};
	
	public void onCreate(Bundle savedInstanced){
		super.onCreate(savedInstanced);
		setContentView(R.layout.dtn_content);
		// Init Application
        this.application = (TetherApplication)this.getApplication();
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
	  }
	  @Override
	  public void onPause(){
		super.onPause();
		Log.d(MSG_TAG,"onPause");
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

}
