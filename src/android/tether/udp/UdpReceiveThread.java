package android.tether.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.tether.dtn.ReceivedBehaver;
import android.tether.system.AndroidTetherConstants;
import android.util.Log;
import android.widget.Toast;


/**
 * @author ukinau
 * 
 * this class depend on Android
 */
public class UdpReceiveThread extends Thread {
	public DatagramSocket recSocket;
	public Handler handler;
	public ReceivedBehaver behaver;
	public boolean socketOpen;
	public UdpReceiveThread(int portNumber , Handler handle) throws SocketException{
		recSocket = new DatagramSocket(portNumber);
		socketOpen = true;
		this.handler = handle;
	}
	
	public UdpReceiveThread(int portNumber , ReceivedBehaver behaver) throws SocketException{
		recSocket = new DatagramSocket(portNumber);
		socketOpen = true;
		this.behaver = behaver;
	}
	
	@Override
	public void run(){
		while(socketOpen){
			try{
				String res = receive();
				
				if(this.handler != null){
					// reflect the result received
					Message msg = new Message();
					Bundle data=new Bundle();
					data.putString("msg", res);
					data.putString("protocol", "UDP" );
					msg.setData(data);
					handler.sendMessage(msg);
				}
				if(this.behaver != null){
					behaver.after_packet_received("","");
				}
				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	public void stopThread(){
		socketOpen = false;
		recSocket.close();
		//this.stop(); this method cause Runtime Exception
	}
	
	private String receive() throws Exception{
		byte []buf = new byte[AndroidTetherConstants.UDP_BUFFER_SIZE];
		DatagramPacket packet= new DatagramPacket(buf,buf.length);
		recSocket.receive(packet);//receive & wait
		SocketAddress macAddress = packet.getSocketAddress();
		macAddress.toString();
		int len = packet.getLength();
		String msg = new String(buf, 0, len);
		return msg+" mac:"+macAddress.toString();
	}

}
