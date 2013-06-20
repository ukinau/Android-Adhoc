package android.tether.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import android.os.Handler;

public class TcpHandleConnectionThread extends Thread {
	private ServerSocket serverSock;
	private boolean endFlg;
	private Handler handler;
	
	
	public TcpHandleConnectionThread(int port, Handler handler) throws IOException {
		this.serverSock = new ServerSocket(port);	
		this.handler = handler;
	}
	
	@Override
	public void run(){
		while(true){
			try{
				final Socket connection = this.serverSock.accept();
				new TcpServerThread(connection,this.handler).start();
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
