package android.tether.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.util.Log;


public class UdpSendThread extends Thread{
	public static final String MSG_TAG_SEND = "UdpSend";
	public UdpSend udpSocket;
	String msg;
	
	public UdpSendThread(String distinationAddress, int distinationPort, String msg, boolean isBroadCast) throws Exception{
		this.udpSocket = new UdpSend(distinationAddress, distinationPort, isBroadCast);
		this.msg = msg;
	}
	
	@Override
	public void run(){
		try {
			udpSocket.send(this.msg);
			Log.d(MSG_TAG_SEND,this.msg);
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			udpSocket.close();
		}
	}
	
	
	public class UdpSend {
		private DatagramSocket sendSocket;
		private InetAddress inetAddress;
		private int port;
		
		public UdpSend(String distinationAddress, int distinationPort, boolean isBroadCast) throws Exception{
			sendSocket = new DatagramSocket();
			sendSocket.setBroadcast(isBroadCast);
			this.setDistinationAddress(distinationAddress);
			this.setDistionationPort(distinationPort);
		}

		public void setDistinationAddress(String distinationAddress) throws UnknownHostException{
			inetAddress = InetAddress.getByName(distinationAddress);
		}
		public void setDistionationPort(int distinationPort){
			port = distinationPort;
		}
		public void send(String msg) throws Exception{
			byte []buf = msg.getBytes();//バイト列に変換
			DatagramPacket packet= new DatagramPacket(buf,buf.length, this.inetAddress, this.port);//IPアドレス、ポート番号も指定
			sendSocket.send(packet);//送信
		}
		public void close(){
			sendSocket.close();
		}
	}

}
