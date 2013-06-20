package android.tether;


import android.tether.system.AndroidTetherConstants;
import android.tether.tcp.TcpHandleConnectionThread;
import android.tether.tcp.TcpClientThread;
import android.tether.udp.UdpReceiveThread;
import android.tether.udp.UdpSendThread;
import android.util.Log;
import android.view.View;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class CommunicateActivity extends Activity {
	
	public static final String MSG_TAG = "TETHER -> CommunicateAcitivity";
	private TetherApplication application;
	private ArrayAdapter<String> messageBoxAdapter;
	private UdpReceiveThread chatUdpReceiver=null;
	private TcpHandleConnectionThread chatHandleTcpConnectionThread=null;
	
	
	// to deal with UI by other thread
	private final Handler  messageBoxListHandler=new Handler() {
		 public void handleMessage(Message msg) {
 			 Bundle data=msg.getData();
 			 String body = data.getString("msg");
 			 String protocol = data.getString("protocol"); 
 			 body += " :"+protocol;
    		 Toast.makeText(CommunicateActivity.this, body , Toast.LENGTH_LONG).show();
    		 messageBoxAdapter.add(body);
    		 messageBoxAdapter.notifyDataSetChanged();
 		 }
 	};
	
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.communicate_content);
	    Log.d(MSG_TAG,"onCreate");
		// Init Application
        this.application = (TetherApplication)this.getApplication();
	    
	    // Register the event of inner content 
     	Button broadCastButton = (Button) findViewById(R.id.broadCastButton);
     	broadCastButton.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v) {
				EditText msgForm = (EditText) findViewById(R.id.message_form);
				String msg = msgForm.getText().toString();				
				String ipAddress = CommunicateActivity.this.application.getIpAddress_fromIfconfig(); 
				int lastIpNumberPoint = ipAddress.lastIndexOf(".");
				String lastIp = ipAddress.substring(lastIpNumberPoint);
				String subnet = ipAddress.split(lastIp)[0];
				Log.d(MSG_TAG,subnet);
				try {
					new UdpSendThread(subnet+".255", 
							AndroidTetherConstants.CHAT_UDP_RECEIVE_PORT, msg, true).start();
				} catch (Exception e) {
					Log.d(MSG_TAG,"fail to create UdpSendClass:"+e.getMessage());
					e.printStackTrace();
				}
			}
     	});
     	
     	Button tcpSendButton = (Button) findViewById(R.id.tcpSendButton);
     	tcpSendButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				EditText msgForm = (EditText) findViewById(R.id.message_form);
				EditText distIpForm = (EditText) findViewById(R.id.distination_ip_form);
				String msg = msgForm.getText().toString();
				String distIp = distIpForm.getText().toString();
				try {
					new TcpClientThread(distIp, AndroidTetherConstants.CHAT_TCP_SERVER_PORT,msg).start();
				} catch (Exception e) {
 	    			Log.d(MSG_TAG,"Fail to create TcpClientClass:"+e.getMessage());
					e.printStackTrace();
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
     	reflectStatus();
     	ListView messageBoxList = (ListView)findViewById(R.id.message_box_listView);
	   	messageBoxAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
	   	messageBoxList.setAdapter(this.messageBoxAdapter);
		
		// Thread receiving udp packet	
	    try{
			Log.d(MSG_TAG,"Start udp receving Thread");
			chatUdpReceiver = new UdpReceiveThread(AndroidTetherConstants.CHAT_UDP_RECEIVE_PORT,
				messageBoxListHandler);
			chatUdpReceiver.start();
		}catch(Exception e){
			Log.d(MSG_TAG,"Fail to start rceiving Thread:"+e.getMessage());
		}
	        
	    // Thread waiting the connection from tcp-client
	    try{
			Log.d(MSG_TAG,"Start waiting tcp connection Thread");
			chatHandleTcpConnectionThread = new TcpHandleConnectionThread(AndroidTetherConstants.CHAT_TCP_SERVER_PORT,
					messageBoxListHandler);
			chatHandleTcpConnectionThread.start();
			
	    }catch(Exception e){
	       	Log.d(MSG_TAG, "Fail to start waiting tcp connection Thread:"+e.getMessage());
        }
	    
	  }
	  @Override
	  public void onPause(){
		super.onPause();
		Log.d(MSG_TAG,"onPause");
		if(chatUdpReceiver != null){
			chatUdpReceiver.stopThread();
	       	Log.d(MSG_TAG, "Stop udp receiver thread");
		  }
		  if(chatHandleTcpConnectionThread != null){
			chatHandleTcpConnectionThread.stopThread();
	       	Log.d(MSG_TAG, "Stop hanlde tcp connection thread");
		}
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
	  
	  public void reflectStatus(){
		 String status = "";
	     String ipAddress = this.application.getIpAddress_fromIfconfig();
	     String wirelessStatus = this.application.getWirelessStatus_fromIwconfig();
	     TextView statusLabel = (TextView)findViewById(R.id.connection_status);
	     if(ipAddress != null){
	     	status += "IP:"+ipAddress+"\n";
	     }
	     if(wirelessStatus  != null){
	     	status += wirelessStatus;
	     }
	     if(status.length() == 0){
	     	status = "未接続";
	     }
	     statusLabel.setText(status);
	     Log.d(MSG_TAG,status);
	  }
	
}
