package android.tether.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import android.tether.system.AndroidTetherConstants;
import android.util.Log;

public class TcpClientThread extends Thread {
	public static final String MSG_TAG_SEND = "TcpSend";
	private TcpClient tcpClient;
	private String endString="¥0¥0";
	private String msg;
	
	public TcpClientThread(String distinationAddress, int distinationPort, String msg) throws UnknownHostException, IOException{
		this.tcpClient = new TcpClient(distinationAddress, distinationPort);
		this.msg = msg;
	}
	
	@Override
	public void run(){
		try{
			this.msg += endString;
			Log.d(MSG_TAG_SEND,"DistinationIp:"+tcpClient.distinationAddress+"\n"+this.msg.split(endString)[0]);
			tcpClient.sendMessage(this.msg);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			tcpClient.close();
		}
	}
	
	public class TcpClient{
		private Socket sock;
		private String distinationAddress;
		private int distinationPort;
		
		public TcpClient(String distinationAddress, int distinationPort) throws UnknownHostException, IOException{
			sock = new Socket(distinationAddress,distinationPort);
			this.distinationAddress = distinationAddress;
			this.distinationPort = distinationPort;
		}
		
		public void sendMessage(String msg) throws IOException{
			OutputStream out = this.sock.getOutputStream();
			int bufferMax = AndroidTetherConstants.TCP_BUFFER_SIZE;
			byte []buf = msg.getBytes();//バイト列に変換
			byte []sendBuf = new byte[bufferMax];
			int base=0;
			while(true){
				int i=0;
				for(i=0;i<bufferMax;i++){
					if(i+base < buf.length){
						sendBuf[i] = buf[i+base];
					}else{
						break;
					}
				}
				out.write(sendBuf);
				base += i;
				if(base > buf.length)
					break;
			}
			out.write(new byte[0]);
			out.close();
		}
		
		public void close(){
			try {
				this.sock.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
}
