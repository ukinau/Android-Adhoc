package android.tether.dtn.algorithm;

import android.os.Handler;
import android.tether.TetherApplication;
import android.tether.dtn.DtnMessage;

public abstract class DtnBaseAlgorithm {
	public final static String MSG_TAG_SEND = "SEND_PACKET";
	public final static String MSG_TAG_RECV = "RECV_PACKET";
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
	public int interval;
	protected TetherApplication app;
	protected Handler handler;
	
	private int rescueMode;
	private boolean executeStatus;
	private DtnMessage targetMsg;
	
	public DtnBaseAlgorithm(TetherApplication app, Handler handler,int firstMode,int interval){
		this.rescueMode = firstMode;
		this.interval = interval;
		this.handler = handler;
		this.app = app;
		this.executeStatus = false;
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
	
	public void setTargetMsg(DtnMessage msg){
		targetMsg = msg;
	}
	public DtnMessage getTargetMsg(){
		return this.targetMsg;
	}
	
	public void changeModeTo(int newMode){
		switch(newMode){
			case MODE_NEED_RESCUE:
				if(this.targetMsg == null){
					setTargetMsg(this.app.getMyRescueMessage());
				}
				break;
		}
		this.rescueMode = newMode;
	}
	public int getDtnMode(){
		return this.rescueMode;
	}
}
