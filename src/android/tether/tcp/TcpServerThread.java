package android.tether.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketAddress;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.tether.CommunicateActivity;
import android.tether.dtn.ReceivedBehaver;
import android.tether.system.AndroidTetherConstants;
import android.util.Log;

public class TcpServerThread extends Thread{
	private Socket sock;
    private String clientAddress;
	private String endString="¥0¥0";
    private Handler handler;
    private ReceivedBehaver behaver;
    
	public TcpServerThread(Socket sock, Handler handler){
		this.sock = sock;
		this.clientAddress = sock.getInetAddress().getHostAddress();
		this.handler = handler;
	}
	public TcpServerThread(Socket sock, ReceivedBehaver behaver){
		this.sock = sock;
		this.clientAddress = sock.getInetAddress().getHostAddress();
		this.behaver = behaver;
	}
	
	@Override
	public void run(){
		
		String []recvInfo = receive();
		String res = recvInfo[0];
		String ipAddress = recvInfo[1];
		if(this.handler != null){
			// reflect the result received
			Message msg = new Message();
			Bundle data=new Bundle();
			data.putString("msg", res);
			data.putString("protocol", "TCP" );
			msg.setData(data);
			handler.sendMessage(msg);
			System.out.print(res.length());
		}
		if(this.behaver != null){
			behaver.after_packet_received(ipAddress,res);
		}
		try {
			this.sock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String receiveString(){
    	byte []receiveBuf = new byte[AndroidTetherConstants.TCP_BUFFER_SIZE]; // 受信バッファ
		int recvMsgSize; // 受信メッセージサイズ
		String recvMsg=null;
		try {
			InputStream in = this.sock.getInputStream();
			recvMsgSize = in.read(receiveBuf);
			recvMsg = new String(receiveBuf , 0 , recvMsgSize);
			recvMsg = recvMsg.split(endString)[0]; //終端文字以降切り捨て
			in.close();
    	} catch (IOException e) {
			e.printStackTrace();
		}
		return recvMsg;
	}
	private String[] receive(){
		byte []receiveBuf = new byte[AndroidTetherConstants.TCP_BUFFER_SIZE]; // 受信バッファ
		int recvMsgSize; // 受信メッセージサイズ
		String []recvInfo = new String[2];
		try {
			InputStream in = this.sock.getInputStream();
			recvMsgSize = in.read(receiveBuf);
			String recvMsg;
			recvMsg = new String(receiveBuf , 0 , recvMsgSize);
			recvMsg = recvMsg.split(endString)[0]; //終端文字以降切り捨て
			recvInfo[0] = recvMsg;
			recvInfo[1] = this.clientAddress;
			in.close();
    	} catch (IOException e) {
			e.printStackTrace();
		}
		return recvInfo;	
	}

}
