package android.tether.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import android.os.Handler;
import android.tether.dtn.ReceivedBehaver;

public class TcpHandleConnectionThread extends Thread {
	private ServerSocket serverSock;
	private boolean endFlg;
	private Handler handler;
	public ReceivedBehaver behaver;
	
	
	public TcpHandleConnectionThread(int port, Handler handler) throws IOException {
		this.serverSock = new ServerSocket(port);	
		this.handler = handler;
	}
	public TcpHandleConnectionThread(int port, ReceivedBehaver behaver) throws IOException{
		this.serverSock = new ServerSocket(port);
		this.behaver = behaver;
	}
	
	@Override
	public void run(){
		while(true){
			try{
				final Socket connection = this.serverSock.accept();
				if(this.handler != null){
					new TcpServerThread(connection,this.handler).start();
				} else if(this.behaver != null){
					new TcpServerThread(connection,this.behaver).start();
				}
			}catch(Exception e){
				e.printStackTrace();
				break;
			}
		}
	}
	
	public void stopThread(){
		try { this.serverSock.close(); } catch (IOException e) { e.printStackTrace(); }
	}
}
