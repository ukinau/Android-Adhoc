package android.tether.dtn;

import java.net.SocketAddress;

import android.os.Handler;
import android.tether.TetherApplication;
import android.tether.system.AndroidTetherConstants;
import android.tether.tcp.TcpHandleConnectionThread;
import android.tether.udp.UdpReceiveThread;

public class DtnFlatting  {
	public final static int MODE_NEED_RESCUE = 0;
	public final static int MODE_CAN_MOVE = 1;
	public final static int MODE_CAN_MOVE_HAVE_MESSAGE = 2;
	
	private UdpReceiveThread udpReceiver;
	private TcpHandleConnectionThread tcpHandleThread;
	private TetherApplication app;
	private int rescueMode;
	
	public DtnFlatting(int mode,TetherApplication app){
		rescueMode = mode;
		this.app = app;
		try{
			this.udpReceiver = new UdpReceiveThread(AndroidTetherConstants.DTN_UDP_RECEIVE_PORT,new ReceivedBehaver(){
				public void after_packet_received(String ipAddress,String res) {
					//初めて通信する相手なら
					if(!DtnFlatting.this.app.knownIpAddress.contains(ipAddress)){
						
					}
				}
			});
			this.tcpHandleThread = new TcpHandleConnectionThread(AndroidTetherConstants.DTN_TCP_SERVER_PORT,new ReceivedBehaver(){
				public void after_packet_received(String ipAddress,String res){
					
				}
			});
			
		}catch(Exception e){
			
		}
	}
	
	public void start(){
		// start udp-receiver and tcp-handler
		// startThread();
		while(true){
			switch(this.rescueMode){
				case MODE_NEED_RESCUE:
					break;
				case MODE_CAN_MOVE:
					sendBcon();
					break;
				case MODE_CAN_MOVE_HAVE_MESSAGE:
					sendBcon();
					break;
			}
			try{Thread.sleep(1000);}catch(Exception e){}
		}
	}
	public void stop(){
	}
	public void restart(){
	}
	private void sendBcon(){
		
	}
	
}
