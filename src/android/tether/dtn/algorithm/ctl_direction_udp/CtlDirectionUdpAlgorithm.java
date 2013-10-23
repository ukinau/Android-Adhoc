package android.tether.dtn.algorithm.ctl_direction_udp;

import java.util.ArrayList;

import android.accounts.NetworkErrorException;
import android.os.Handler;
import android.os.Message;
import android.tether.TetherApplication;
import android.tether.dtn.DtnMessage;
import android.tether.dtn.ReceivedBehaver;
import android.tether.dtn.algorithm.DtnBaseAlgorithm;
import android.tether.system.AndroidTetherConstants;
import android.tether.udp.UdpReceiveThread;
import android.tether.udp.UdpSendThread;
import android.util.Log;

public class CtlDirectionUdpAlgorithm extends DtnBaseAlgorithm{
	public final static String toSTRING = "相対方向制御アルゴリズム(UDP)";
	public final static String MSG_TAG = "DTN -> CtlDirectionUdpAlgorithm";

	private UdpReceiveThread udpReceiver;
	private ReceivedBehaver revUdpBehaver;
	private int min_inverse_direction = 68; // 68 ~ 293°が逆方向
	private int max_inverse_direction = 293;
	
	public static final int SAME_DIRECTION = 11;
	public static final int INVERSE_DIRECTION = -11;
	
	public static final int LEFT_CONTAIN_RIGHT = 1; 
	public static final int RIGHT_CONTAIN_LEFT = 2; //LEFTの要素が０の場合もこれに含まれる
	public static final int SAME_RELATION = 3;
	public static final int NOT_CONTAIN_RELATION = 4;
	
	public static final int LEFT_IS_BIG = 5;
	public static final int LEFT_IS_SMALL = 6;
	public static final int SAME = 7;
	
	public CtlDirectionUdpAlgorithm(int firstMode, TetherApplication app, Handler handle){
		super(app,handle,firstMode,5);
		this.app.resetDtnMessage();
		this.revUdpBehaver = new ReceivedBehaver() {
			public void after_packet_received(String ipAddress, String res) {
				String myIpAddress = CtlDirectionUdpAlgorithm.this.app.getIpAddress_fromIfconfig();
				if(!(myIpAddress.equals(ipAddress))){
					try{
						CtlDirectionUdpFormatBuilder response = CtlDirectionUdpFormatBuilder.read(res);
						switch(response.message_kind){
							case CtlDirectionUdpFormatBuilder.MESSAGE_KIND_MESSAGE_ONLY:
								if(getDtnMode() != MODE_NEED_RESCUE){
									//含有関係を求める
									int contain_relation = comparedMessages(CtlDirectionUdpAlgorithm.this.app.getDtnMessages(),
											response.messages);
									int direction_relation = judge_relative_direction(CtlDirectionUdpAlgorithm.this.app.getAngle(
											TetherApplication.DIRECTION_TYPE_Y), response.angle);
									// update messages
									for(int i=0;i<response.messages.size();i++){
										DtnMessage msg = response.messages.get(i);
										if(!CtlDirectionUdpAlgorithm.this.app.containsDtnMessage(msg)){
											CtlDirectionUdpAlgorithm.this.app.addDtnMessage(msg);
											// Reflect the ListView
											Message m = new Message();
											m.obj = msg;
											CtlDirectionUdpAlgorithm.this.handler.sendMessage(m);
										}
									}
									switch(direction_relation){
										case INVERSE_DIRECTION:
											Log.d(MSG_TAG,"認識：逆方向");
											changeModeTo(MODE_CAN_MOVE_HAVE_MESSAGE);
											break;
										case SAME_DIRECTION:
											Log.d(MSG_TAG,"認識：同方向");
											switch(contain_relation){
												case SAME_RELATION:
													int ip_judge = compared_ipAddress(myIpAddress, ipAddress);
													switch(ip_judge){
														case LEFT_IS_BIG:
															changeModeTo(MODE_CAN_MOVE);
															break;
														case LEFT_IS_SMALL:
															changeModeTo(MODE_CAN_MOVE_HAVE_MESSAGE);
															break;
													}
													break;
												case RIGHT_CONTAIN_LEFT:
													changeModeTo(MODE_CAN_MOVE);
													break;
												case LEFT_CONTAIN_RIGHT:
												case NOT_CONTAIN_RELATION:
													changeModeTo(MODE_CAN_MOVE_HAVE_MESSAGE);
													break;
											}
									
									}
								}
								break;
						}
					}catch(Exception e){
						System.out.print(e);
					}
				}
			}
		};
		

	}

	@Override
	public void setup() {
		DtnMessage myMsg = this.app.getMyRescueMessage();
		this.app.settingMyRescueMessage(myMsg);
		// udp start
		try{
			Log.d(MSG_TAG,"Create Udp receive thread");
			this.udpReceiver = new UdpReceiveThread(AndroidTetherConstants.DTN_UDP_RECEIVE_PORT,this.revUdpBehaver);
			Log.d(MSG_TAG,"Start Udp receive thread");
			this.udpReceiver.start();
		}catch(Exception e){
			Log.d(MSG_TAG,"Can't start udpReceiver thread: "+e.getMessage());
		}
	}

	@Override
	public void loop() {
		switch(this.getDtnMode()){
			case MODE_CAN_MOVE:
				break;
			case MODE_CAN_MOVE_HAVE_MESSAGE:
				sendMessagesIhave();
				break;
			case MODE_NEED_RESCUE:
				sendMyRescueMessage();
				break;
		}
	}
	
	@Override
	public void stop(){
		super.stop();
		if(this.udpReceiver != null){
			this.udpReceiver.stop();
			Log.d(MSG_TAG,"Stop Udp receiver thread");
		}
	}
	
	public synchronized void sendMyRescueMessage(){
		String ipAddress = this.app.getIpAddress_fromIfconfig(); 
		int lastIpNumberPoint = ipAddress.lastIndexOf(".");
		String lastIp = ipAddress.substring(lastIpNumberPoint);
		String subnet = ipAddress.split(lastIp)[0];
		try {
			CtlDirectionUdpFormatBuilder msg;
			msg = new CtlDirectionUdpFormatBuilder();
			msg.message_kind = CtlDirectionUdpFormatBuilder.MESSAGE_KIND_MESSAGE_ONLY;
			msg.angle = this.app.getAngle(TetherApplication.DIRECTION_TYPE_Y);
			msg.messages.add(getTargetMsg());
			String sendXml = msg.buildXml();
			new UdpSendThread(subnet+".255", AndroidTetherConstants.DTN_UDP_RECEIVE_PORT , sendXml, true).start();
		} catch (Exception e) {
			Log.d(MSG_TAG,"can't send my rescue message");
			e.printStackTrace();
		}
	}
	
	public synchronized void sendMessagesIhave(){
		String ipAddress = this.app.getIpAddress_fromIfconfig(); 
		int lastIpNumberPoint = ipAddress.lastIndexOf(".");
		String lastIp = ipAddress.substring(lastIpNumberPoint);
		String subnet = ipAddress.split(lastIp)[0];
		try {
			CtlDirectionUdpFormatBuilder msg;
			msg = new CtlDirectionUdpFormatBuilder();
			msg.message_kind = CtlDirectionUdpFormatBuilder.MESSAGE_KIND_MESSAGE_ONLY;
			msg.angle = this.app.getAngle(TetherApplication.DIRECTION_TYPE_Y);
			msg.messages = this.app.getDtnMessages();
			String sendXml = msg.buildXml();
			new UdpSendThread(subnet+".255", AndroidTetherConstants.DTN_UDP_RECEIVE_PORT , sendXml, true).start();
		} catch (Exception e) {
			Log.d(MSG_TAG,"can't send rescue message I have");
			e.printStackTrace();
		}
	}
	
	/**
	 * IPアドレスを比較する
	 */
	public int compared_ipAddress(String left, String right){
		String []t = left.split("\\.");
		int host_left = Integer.parseInt(left.split("\\.")[3]);
		int host_right = Integer.parseInt(right.split("\\.")[3]);
		int compared_value = host_left-host_right;
		if(compared_value<0){
			return LEFT_IS_SMALL;
		}
		else if(compared_value > 0){
			return LEFT_IS_BIG;
		}
		else{
			return SAME;
		}
		
	}

	/**
	 * 角度情報を引数に渡すと、[同方向、逆方向]を判定します
	 * @param left
	 * @param right
	 * @return 
	 *   11 => 同方向
	 *  -11 => 逆方向
	 */
	public int judge_relative_direction(double[] left, double[] right){
		double relative_angle = calculate_ralative_angle(left, right);
		Log.d(MSG_TAG,"compared angle: my["+left[0]+"]["+left[1]+"]"+"other["+right[0]+"]["+right[1]+"]");
		Log.d(MSG_TAG,"相対角度:"+relative_angle);
		if(relative_angle > min_inverse_direction && 
				relative_angle < max_inverse_direction){
			Log.d(MSG_TAG,"逆方向");
			return INVERSE_DIRECTION;
		}else{
			Log.d(MSG_TAG,"同方向");
			return SAME_DIRECTION;
		}
	}
	/**
	 * 第一引数と第二引数の相対角度を算出します
	 * @param left
	 * @param right
	 * @return
	 */
	private double calculate_ralative_angle(double []left, double[] right){
		double result;
		if(left[1]*right[1] > 0){
			result = left[0] - right[0];
		}else{
			result = left[0] + right[0];
		}
		if(result < 0){
			result = 360 + result;
		}
		return result;
	}
	
	/**
	 * メッセージ情報の含有関係を比較します
	 * return 
	 *   1 => left ∋ right
	 *   2 => left ∈ right 
	 *   3 => left = right 
	 *   4 => 含有関係にない
	 */
	public int comparedMessages(ArrayList<DtnMessage> src, ArrayList<DtnMessage> dst){
		int match_count = 0;
		for(int i=0;i<src.size();i++){
			for(int j=0;j<dst.size();j++){
				if(src.get(i).mac_address.equals( dst.get(j).mac_address ) ){
					match_count++;
				}
			}
		}
		if(match_count == src.size() && match_count == dst.size()){
			return SAME_RELATION;
		}
		else if(match_count == src.size()){
			return RIGHT_CONTAIN_LEFT;
		}
		else if(match_count == dst.size()){
			return LEFT_CONTAIN_RIGHT;
		}else{
			return NOT_CONTAIN_RELATION;
		}
	}
}
