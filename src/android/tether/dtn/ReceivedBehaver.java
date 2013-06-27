package android.tether.dtn;

import java.net.SocketAddress;

public interface ReceivedBehaver {
	public void after_packet_received(String ipAddress,String res);
}
