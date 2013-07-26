package android.tether.dtn.algorithm.spray_and_wait;

import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import android.os.Handler;
import android.os.Message;
import android.tether.TetherApplication;
import android.tether.dtn.DtnMessage;
import android.tether.dtn.ReceivedBehaver;
import android.tether.dtn.algorithm.DtnBaseAlgorithm;
import android.tether.system.AndroidTetherConstants;
import android.tether.tcp.TcpClientThread;
import android.tether.tcp.TcpHandleConnectionThread;
import android.tether.udp.UdpReceiveThread;
import android.tether.udp.UdpSendThread;
import android.util.Log;

/**
 * Bcon(A→ALL) → Demand[持っているメッセージ情報](B→A) → Message(A→B) → メッセージ受信[msg情報更新]
 * Bcon(A→ALL) → Demand[持っているメッセージ情報](B→A) → 無視(Bがメッセージを持っているので）
 * Bcon(A→ALL) → Demand[持っているメッセージ情報](B→A) → 無視(Aが持ってるメッセージのtokenが1）
 * 
 * IPアドレスと端末が１対１でマップされていること前提
 * 
 * @author yukio7
 *
 */
public class SprayAndWaitAlgorithm extends DtnBaseAlgorithm {
	public final static String toSTRING = "SprayAndWaitAlgorithm";
	public final static String MSG_TAG = "DTN -> SprayAndWaitAlgorithm";
	public final static int TOKEN_SIZE = 4;
	
	private UdpReceiveThread udpReceiver;
	private TcpHandleConnectionThread tcpHandle;
	private ReceivedBehaver revUdpBehaver;
	private ReceivedBehaver revTcpBehaver;
	private ArrayList<HashMap<Integer,String>> requestTask; // ArrayList<HashMap< Request_Kind , ipAddress>>
	
	public SprayAndWaitAlgorithm(int FirstMode,TetherApplication app,Handler handle){
		super(app,handle,FirstMode,5);
		this.app.resetDtnMessage();
		this.revUdpBehaver = new ReceivedBehaver(){
			public void after_packet_received(String ipAddress,String xml) {
				String myIpAddress = SprayAndWaitAlgorithm.this.app.getIpAddress_fromIfconfig();
				if(!myIpAddress.equals(ipAddress)){
					try {
						SprayAndWaitFormatBuilder responce = SprayAndWaitFormatBuilder.read(xml);
						switch(SprayAndWaitAlgorithm.this.getDtnMode()){
							case MODE_NEED_RESCUE:
								break;
							case MODE_CAN_MOVE:
							case MODE_CAN_MOVE_HAVE_MESSAGE:
								// b-con
								switch(responce.message_kind){
									case SprayAndWaitFormatBuilder.MESSAGE_KIND_B_CON:
										Log.d(MSG_TAG_RECV, "Receive packet(B-CON) using UDP from: "+ipAddress);
										sendDemand(ipAddress);
										break;
								}
								break;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		this.revTcpBehaver = new ReceivedBehaver() {
			public void after_packet_received(String ipAddress, String xml) {
				try {
					SprayAndWaitFormatBuilder responce = SprayAndWaitFormatBuilder.read(xml);
					// Message or Have_message
					switch (responce.message_kind) {
						case SprayAndWaitFormatBuilder.MESSAGE_KIND_REQUEST_DEMAND_ONLY:
							Log.d(MSG_TAG_RECV, "Receive packet(MESSAGE_KIND_REQUEST_DEMAND_ONLY) using TCP from: "+ipAddress);
							switch(SprayAndWaitAlgorithm.this.getDtnMode()){
								case MODE_CAN_MOVE_HAVE_MESSAGE:
									sendMessagesIhave(ipAddress, responce.have_messages);
									break;
								case MODE_NEED_RESCUE:
									sendMyRescueMessage(ipAddress, responce.have_messages);
									break;
							}
							break;
						case SprayAndWaitFormatBuilder.MESSAGE_KIND_RESULT_DEMAND_ONLY:
							Log.d(MSG_TAG_RECV, "Receive packet(MESSAGE_KIND_RESULT_DEMAND_ONLY) using TCP from: "+ipAddress);
							if(responce.messages.size() > 0){
								ArrayList<DtnMessage> messages = toDtnMessageList(responce.messages);
								for(int i=0;i<messages.size();i++){
									SprayAndWaitAlgorithm.this.app.addDtnMessage(messages.get(i));
									// Reflect the ListView
									Message msg = new Message();
									DtnMessage dtnM = messages.get(i);
									msg.obj = dtnM;
									SprayAndWaitAlgorithm.this.handler.sendMessage(msg);
								}
								switch(SprayAndWaitAlgorithm.this.getDtnMode()){
									case MODE_CAN_MOVE:
										SprayAndWaitAlgorithm.this.changeModeTo(MODE_CAN_MOVE_HAVE_MESSAGE);
										break;
								}
							}
							break;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
	}
	@Override
	public void setup(){
		// init requestTask
		this.requestTask = new ArrayList<HashMap<Integer,String>>();
		// Set my rescue message
		Log.d(MSG_TAG,"Set my rescue message");
		SprayAndWaitDtnMessage myMsg = SprayAndWaitDtnMessage.create(this.app.getMyRescueMessage());
		myMsg.token = TOKEN_SIZE;
		this.app.settingMyRescueMessage((DtnMessage)myMsg);
		// Start Udp receiver thread
		try{
			Log.d(MSG_TAG,"Create Udp receive thread");
			this.udpReceiver = new UdpReceiveThread(AndroidTetherConstants.DTN_UDP_RECEIVE_PORT,this.revUdpBehaver);
			Log.d(MSG_TAG,"Start Udp receive thread");
			this.udpReceiver.start();
		}catch(Exception e){
			Log.d(MSG_TAG,"Can't start udpReceiver thread: "+e.getMessage());
		}
		// Start Tcp Handle thread
		try{
			Log.d(MSG_TAG,"Create Tcp handle thread");
			this.tcpHandle = new TcpHandleConnectionThread(AndroidTetherConstants.DTN_TCP_SERVER_PORT, this.revTcpBehaver);
			Log.d(MSG_TAG,"Start Tcp handle thread");
			this.tcpHandle.start();
		}catch(Exception e){
			Log.d(MSG_TAG,"Can't start tcpHandler thread: "+e.getMessage());
		}
		
	}
	@Override
	public void loop(){
		switch(this.getDtnMode()){
			case MODE_NEED_RESCUE:
				sendBcon();
				break;
			case MODE_CAN_MOVE:
				break;
			case MODE_CAN_MOVE_HAVE_MESSAGE:
				sendBcon();
				break;
		}
	}
	@Override
	public void stop() {
		super.stop();
		if(this.udpReceiver != null){
			this.udpReceiver.stopThread();
			Log.d(MSG_TAG,"Stop Udp receiver thread");
		}
		if(this.tcpHandle != null){
			this.tcpHandle.stopThread();
			Log.d(MSG_TAG,"Stop Tcp handler thread");
		}
	}
	
	public SprayAndWaitDtnMessage updateDtnMessage(SprayAndWaitDtnMessage dtnMsg){
		SprayAndWaitDtnMessage result = (SprayAndWaitDtnMessage)dtnMsg.clone();
		if(result.token > 1){
			result.token = result.token/2;
		}
		return result;
	}
	
	public ArrayList<SprayAndWaitDtnMessage> updateDtnMessages(ArrayList<SprayAndWaitDtnMessage> dtnMsgList){
		ArrayList<SprayAndWaitDtnMessage> results = new ArrayList<SprayAndWaitDtnMessage>();
		for(int i=0;i<dtnMsgList.size();i++){
			SprayAndWaitDtnMessage msg = dtnMsgList.get(i);
			msg = updateDtnMessage(msg);
			results.add(msg);
		}
		return results;
	}
	
	public ArrayList<SprayAndWaitDtnMessage> filterSendableMessage(ArrayList<SprayAndWaitDtnMessage> sendableCandidateList, ArrayList<String> have_messages){
		ArrayList<SprayAndWaitDtnMessage> results = new ArrayList<SprayAndWaitDtnMessage>(sendableCandidateList);
		for(int i=0;i<have_messages.size();i++){
			for(int j=0;j<results.size();j++){
				String have_macAddress = have_messages.get(i);
				if(have_macAddress.equals(results.get(j).mac_address)){
					results.remove(j);
				}
			}
		}
		for(int j=0;j<results.size();j++){
			if(results.get(j).token <= 1 ){
				results.remove(j);
			}
		}
		return results;
	}
	
	public ArrayList<DtnMessage> toDtnMessageList(ArrayList<SprayAndWaitDtnMessage> msgList){
		ArrayList<DtnMessage> results = new ArrayList<DtnMessage>();
		for(int i=0;i<msgList.size();i++){
			results.add((DtnMessage)msgList.get(i));
		}
		return results;
	}
	public ArrayList<SprayAndWaitDtnMessage> toSprayAndWaitDtnMessageList(ArrayList<DtnMessage> msgList){
		ArrayList<SprayAndWaitDtnMessage> results = new ArrayList<SprayAndWaitDtnMessage>();
		for(int i=0;i<msgList.size();i++){
			results.add((SprayAndWaitDtnMessage)msgList.get(i));
		}
		return results;
	}
	
	public boolean containsRequestTask(int messageKind,String ipAddress){
		boolean result = false;
		for(int i=0;i<this.requestTask.size();i++){
			if(this.requestTask.get(i).get(messageKind).equals(ipAddress)){
				result = true;
			}
		}
		return result;
	}
	
	public void sendBcon(){
		String ipAddress = this.app.getIpAddress_fromIfconfig(); 
		int lastIpNumberPoint = ipAddress.lastIndexOf(".");
		String lastIp = ipAddress.substring(lastIpNumberPoint);
		String subnet = ipAddress.split(lastIp)[0];
		try {
			SprayAndWaitFormatBuilder msg;
			msg = new SprayAndWaitFormatBuilder();
			msg.message_kind = SprayAndWaitFormatBuilder.MESSAGE_KIND_B_CON;
			msg.myMacAddress = this.app.getMacAddress();
			String sendXml = msg.buildXml();
			new UdpSendThread(subnet+".255", AndroidTetherConstants.DTN_UDP_RECEIVE_PORT , sendXml, true).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	synchronized void sendDemand(String ipAddress) throws Exception{
		if(!(SprayAndWaitAlgorithm.this.containsRequestTask(SprayAndWaitFormatBuilder.MESSAGE_KIND_REQUEST_DEMAND_ONLY, ipAddress))){
			SprayAndWaitFormatBuilder newBuilder = new SprayAndWaitFormatBuilder();
			newBuilder.message_kind = SprayAndWaitFormatBuilder.MESSAGE_KIND_REQUEST_DEMAND_ONLY;
			newBuilder.have_messages = SprayAndWaitAlgorithm.this.app.getHave_macAddressList();
			try {
				String sendXml = newBuilder.buildXml();
				new TcpClientThread(ipAddress,
						AndroidTetherConstants.DTN_TCP_SERVER_PORT,sendXml).start();
				Log.d(MSG_TAG_SEND, "Send packet(DEMAND_ONLY) using TCP to: "+ipAddress);
				
				// Add request-task
				HashMap<Integer, String> requestMap = new HashMap<Integer, String>();
				requestMap.put(SprayAndWaitFormatBuilder.MESSAGE_KIND_REQUEST_DEMAND_ONLY, ipAddress);
				this.requestTask.add(requestMap);
	
			} catch (TransformerException e) {
				e.printStackTrace();
			}
		}
	}
	
	synchronized void sendMessagesIhave(String ipAddress,ArrayList<String> have_messages) throws Exception{
		//ArrayList<SprayAndWaitDtnMessage> updatedDtnMessagesList = updateDtnMessages(SprayAndWaitAlgorithm.this.app.getDtnMessages());
		ArrayList<SprayAndWaitDtnMessage> messages = toSprayAndWaitDtnMessageList(SprayAndWaitAlgorithm.this.app.getDtnMessages());
		ArrayList<SprayAndWaitDtnMessage> sendableCandidateList = filterSendableMessage(messages, have_messages);
		if(sendableCandidateList.size()>0){
			sendableCandidateList = updateDtnMessages(sendableCandidateList);
			SprayAndWaitFormatBuilder newBuilder = new SprayAndWaitFormatBuilder();
			newBuilder.message_kind = SprayAndWaitFormatBuilder.MESSAGE_KIND_RESULT_DEMAND_ONLY;
			newBuilder.messages = sendableCandidateList;
			try {
				String sendXml = newBuilder.buildXml();
				new TcpClientThread(ipAddress,
						AndroidTetherConstants.DTN_TCP_SERVER_PORT,sendXml).start();
				Log.d(MSG_TAG_SEND, "Send packet(DEMAND_HAVE_MESSAGES_RESULT) using TCP to: "+ipAddress);
				// Set token updated list 
				SprayAndWaitAlgorithm.this.app.setDtnMessagesList(toDtnMessageList(updateDtnMessages(messages)));
			} catch (TransformerException e) {
				e.printStackTrace();
			}
		}
	}
	synchronized void sendMyRescueMessage(String ipAddress,ArrayList<String> have_messages) throws Exception{
		DtnMessage myRescueMessage = SprayAndWaitAlgorithm.this.app.getMyRescueMessage();
		ArrayList<SprayAndWaitDtnMessage> sendableCandidateList = new ArrayList<SprayAndWaitDtnMessage>();
		sendableCandidateList.add((SprayAndWaitDtnMessage)myRescueMessage);
		sendableCandidateList = filterSendableMessage(sendableCandidateList, have_messages);
		if(sendableCandidateList.size()==1){
			SprayAndWaitDtnMessage sendCandidateMsg = updateDtnMessage(sendableCandidateList.get(0));
			SprayAndWaitFormatBuilder newBuilder = new SprayAndWaitFormatBuilder();
			newBuilder.message_kind = SprayAndWaitFormatBuilder.MESSAGE_KIND_RESULT_DEMAND_ONLY;
			newBuilder.messages.add(sendCandidateMsg);
			try {
				String sendXml = newBuilder.buildXml();
				new TcpClientThread(ipAddress,
						AndroidTetherConstants.DTN_TCP_SERVER_PORT,sendXml).start();
				Log.d(MSG_TAG_SEND, "Send packet(DEMAND_HAVE_MESSAGES_RESULT) using TCP to: "+ipAddress);

				// Set token updated msg 
				SprayAndWaitAlgorithm.this.app.settingMyRescueMessage((DtnMessage)sendCandidateMsg);
			} catch (TransformerException e) {
				e.printStackTrace();
			}
		}
	}
	
	
}
