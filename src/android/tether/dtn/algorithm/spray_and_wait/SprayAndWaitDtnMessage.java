package android.tether.dtn.algorithm.spray_and_wait;

import android.tether.dtn.DtnMessage;

public class SprayAndWaitDtnMessage extends DtnMessage {
	public int token;
	public static SprayAndWaitDtnMessage create(DtnMessage msg){
		SprayAndWaitDtnMessage newMsg = new SprayAndWaitDtnMessage();
		newMsg.address = msg.address;
		newMsg.facebook = msg.facebook;
		newMsg.name = msg.name;
		newMsg.mac_address = msg.mac_address;
		return newMsg;
	}
}
