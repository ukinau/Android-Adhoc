package android.tether.dtn;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class FetchModeThread extends Thread {
	private boolean excute_status;
	private Handler handler;
	private DtnBase dtnImplement;
	private int interval;
	
	public FetchModeThread(Handler handle,DtnBase dtnImplement,int interval){
		this.excute_status = true;
		this.handler = handle;
		this.dtnImplement = dtnImplement;
		this.interval = interval;
	}
	
	@Override
	public void run(){
		while(this.excute_status){
			Bundle data = new Bundle();
			String mode = "";
			switch(this.dtnImplement.getDtnMode()){
				case DtnBase.MODE_CAN_MOVE:
					mode = "移動者";
					break;
				case DtnBase.MODE_CAN_MOVE_HAVE_MESSAGE:
					mode = "移動者（レスキューメッセージ所持）";
					break;
				case DtnBase.MODE_NEED_RESCUE:
					mode = "要救助者";
					break;
			}
			data.putString("mode",mode);
			Message msg = new Message();
			msg.setData(data);
			this.handler.sendMessage(msg);
			try { sleep(interval*1000); } catch (InterruptedException e) { e.printStackTrace(); }
		}
	}
	
	public void stopThread(){
		this.excute_status = false;
	}
}
