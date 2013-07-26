package android.tether.dtn.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.tether.CommunicateActivity;
import android.tether.TetherApplication;
import android.tether.dtn.DtnMessage;
import android.tether.dtn.FormatBuilder;
import android.tether.dtn.ReceivedBehaver;
import android.tether.system.AndroidTetherConstants;
import android.tether.udp.UdpReceiveThread;
import android.tether.udp.UdpSendThread;
import android.util.Log;

/**
 * 
 * @author yukio7
 * B-CONなしのフラッティング方式
 * Without B-CON,the flatting method 
 * 
 * UDPで定期的に持っているメッセージをブロードキャストアルゴリズム
 * The algorithm that constantly send the message I have through UDP 
 */
public class DtnFlattingOnlyUdpBroadCastSample extends DtnBaseAlgorithm {
	public final static String toSTRING = "DtnFlattingOnlyUdpBroadCastSample";
	public final static String MSG_TAG = "DTN -> DtnFlattingOnlyUdpBroadCast";
	private UdpReceiveThread udpReceiver;
	private ReceivedBehaver revBehaver;
	
	public DtnFlattingOnlyUdpBroadCastSample(int FirstMode,TetherApplication app,Handler handle){
		super(app,handle,FirstMode,5);
		this.app.resetDtnMessage();
		this.revBehaver = new ReceivedBehaver(){
			public void after_packet_received(String ipAddress,String xml) {
				try {
					FormatBuilder newBuilder = FormatBuilder.read(xml);
					for(int i=0;i<newBuilder.messages.size();i++){
						// Validate MacAddress using regular expression
						String regex = "..:..:..:..:..:..";
						Pattern p = Pattern.compile(regex);
						Matcher m = p.matcher(newBuilder.messages.get(i).mac_address);
						Log.d(MSG_TAG_RECV, "Receive packet from: "+ipAddress);
						if(m.find()&&!DtnFlattingOnlyUdpBroadCastSample.this.app.containsDtnMessage(newBuilder.messages.get(i))){
							DtnFlattingOnlyUdpBroadCastSample.this.app.addDtnMessage(newBuilder.messages.get(i));
							// Reflect the ListView
							Message msg = new Message();
							DtnMessage dtnM = newBuilder.messages.get(i);
							msg.obj = dtnM;
							DtnFlattingOnlyUdpBroadCastSample.this.handler.sendMessage(msg);
							
							switch(DtnFlattingOnlyUdpBroadCastSample.this.getDtnMode()){
								case MODE_CAN_MOVE:
									DtnFlattingOnlyUdpBroadCastSample.this.changeModeTo(MODE_CAN_MOVE_HAVE_MESSAGE);
									break;
							}
						}
					}
				} catch (ParserConfigurationException e) {
					Log.d(MSG_TAG,"Can't parse xml because the xml is wrong format");
				} catch (SAXException e) {
					Log.d(MSG_TAG,"Can't parse xml because the xml is wrong format");
				} catch (IOException e) {
					Log.d(MSG_TAG,"Can't parse xml because the xml is wrong format");
				}
			}
		};
	}
	@Override
	public void setup(){
		try{
			Log.d(MSG_TAG,"Create Udp receive thread");
			this.udpReceiver = new UdpReceiveThread(AndroidTetherConstants.DTN_UDP_RECEIVE_PORT,this.revBehaver);
			Log.d(MSG_TAG,"Start Udp receive thread");
			this.udpReceiver.start();
		}catch(Exception e){
			Log.d(MSG_TAG,"Can't start udpReceiver thread: "+e.getMessage());
		}
	}
	@Override
	public void loop(){
		switch(this.getDtnMode()){
			case MODE_NEED_RESCUE:
				String ipAddress = this.app.getIpAddress_fromIfconfig(); 
				int lastIpNumberPoint = ipAddress.lastIndexOf(".");
				String lastIp = ipAddress.substring(lastIpNumberPoint);
				String subnet = ipAddress.split(lastIp)[0];
				Log.d(MSG_TAG,"subnet:"+subnet);
				FormatBuilder msg;
				try {
					msg = new FormatBuilder();
					msg.message_kind = FormatBuilder.MESSAGE_KIND_MESSAGE_ONLY;
					msg.messages.add(getTargetMsg());
					try {
						new UdpSendThread(subnet+".255", 
								AndroidTetherConstants.DTN_UDP_RECEIVE_PORT, msg.buildXml(), true).start();
						Log.d(MSG_TAG_SEND,"Send DtnMessage as: "+"MODE_NEED_RESCUE");
					} catch (TransformerException e) {
						Log.d(MSG_TAG,"Fail to create xml from FormatBuilder obj");
					} catch (Exception e) {
						Log.d(MSG_TAG,"Fail to create UdpSendThread");
					}
				} catch (ParserConfigurationException e1) {
					Log.d(MSG_TAG,"Fail to create the FormatBuilder");
				}
				break;
			case MODE_CAN_MOVE:
				break;
			case MODE_CAN_MOVE_HAVE_MESSAGE:
				ipAddress = this.app.getIpAddress_fromIfconfig(); 
				lastIpNumberPoint = ipAddress.lastIndexOf(".");
				lastIp = ipAddress.substring(lastIpNumberPoint);
				subnet = ipAddress.split(lastIp)[0];
				try {
					msg = new FormatBuilder();
					msg.message_kind = FormatBuilder.MESSAGE_KIND_MESSAGE_ONLY;
					ArrayList<DtnMessage> dtnMs =  this.app.getDtnMessages();
					for(int i=0;i<dtnMs.size();i++){
						msg.messages.add(dtnMs.get(i));
					}
					try {
						new UdpSendThread(subnet+".255", 
								AndroidTetherConstants.DTN_UDP_RECEIVE_PORT, msg.buildXml(), true).start();
						Log.d(MSG_TAG_SEND,"Send DtnMessages as: "+"MODE_CAN_MOVE_HAVE_MESSAGE");
					} catch (TransformerException e) {
						Log.d(MSG_TAG,"Fail to create xml from FormatBuilder obj");
					} catch (Exception e) {
						Log.d(MSG_TAG,"Fail to create UdpSendThread");
					}
				} catch (ParserConfigurationException e1) {
					Log.d(MSG_TAG,"Fail to create the FormatBuilder");
				}
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
	}

}
