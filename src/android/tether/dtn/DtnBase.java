package android.tether.dtn;

import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import android.os.Handler;
import android.tether.system.AndroidTetherConstants;
import android.tether.tcp.TcpHandleConnectionThread;
import android.tether.udp.UdpReceiveThread;
import android.tether.udp.UdpSendThread;
import android.util.Log;

public abstract class DtnBase {
	/**
	 * アルゴリズム実行時に最初に一度だけ呼ばれる処理
	 * パケットを受け取るスレッドをstartしたりする
	 */
	public abstract void setup();
	/**
	 * アルゴリズム実行時に
	 * 繰り返し実行される処理を書く
	 */
	public abstract void loop();
	public final static int MODE_NEED_RESCUE = 0;
	public final static int MODE_CAN_MOVE = 1;
	public final static int MODE_CAN_MOVE_HAVE_MESSAGE = 2;
	public int rescueMode;
	public boolean executeStatus;
	public int interval = 1;
	

	public static final String MSG_TAG = "DtnImplements";
	private DtnMessage targetMsg;
	
	public void setTargetMsg(DtnMessage msg){
		targetMsg = msg;
	}
	public DtnMessage getTargetMsg(){
		return targetMsg;
	}
	public void start(){
		this.executeStatus = true;
		setup();
		while(this.executeStatus){
			loop();
			try { Thread.sleep(interval*1000); } catch (InterruptedException e) { e.printStackTrace(); }
		}	
	}
	public void stop(){
		this.executeStatus = false;
	}
}
