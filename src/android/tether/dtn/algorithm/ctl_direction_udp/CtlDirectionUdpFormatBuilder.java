package android.tether.dtn.algorithm.ctl_direction_udp;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
/**
 * @author yukio7
 * 
 * Use MESSAGE_KIND_MESSAFE_ONLY
 * 
 * MESSAGE_KIND_MESSAGE_ONLY
 * <angle> 
 * <messages>
 * 		<message>
 * 			<mac-address>	
 * 			<body>
 * 				<name>
 * 				<address>
 * 			</body>
 * 		</message>
 * </messages>
 * 
 * 
 */
import android.tether.dtn.FormatBuilder;

public class CtlDirectionUdpFormatBuilder extends FormatBuilder {
	public double[] angle;
	public static final String delimitter = "/";
	public CtlDirectionUdpFormatBuilder() throws ParserConfigurationException {
		super();
		angle = new double[2];
	}

	@Override
	protected void setMessages(Element root,Document document){
		this.setAngle(root, document);
		super.setMessages(root, document);
	}
	
	private void setAngle(Element root, Document document){
		Element angle_ele =  document.createElement("angle");
		Text angle_innerText = document.createTextNode(String.valueOf(this.angle[0])+delimitter+String.valueOf(this.angle[1]));
		angle_ele.appendChild(angle_innerText);
		root.appendChild(angle_ele);
	}
	
	public static CtlDirectionUdpFormatBuilder readAngle(Node angle_container_node, CtlDirectionUdpFormatBuilder newBuilder){
		String[] tmp = angle_container_node.getFirstChild().getNodeValue().split(delimitter);
		newBuilder.angle[0] = Double.parseDouble(tmp[0]);
		newBuilder.angle[1] = Double.parseDouble(tmp[1]);
		return newBuilder;
	}
	
	public static CtlDirectionUdpFormatBuilder read(String xml) throws ParserConfigurationException, SAXException, IOException{
		final CtlDirectionUdpFormatBuilder newCtlDirectionUdpFormatBuilder = new CtlDirectionUdpFormatBuilder();
		FormatBuilder.read(xml, new FormatBuilder.XmlReaderInterface(){
			public FormatBuilder readNodes(NodeList nodes, FormatBuilder newBuilder, int kind) {
				newCtlDirectionUdpFormatBuilder.message_kind = kind;
				switch(kind){
					case MESSAGE_KIND_MESSAGE_ONLY:
						Node angle_container_node = nodes.item(1);
						Node message_container_node = nodes.item(2);
						readAngle(angle_container_node, newCtlDirectionUdpFormatBuilder);
						readMessages(message_container_node, newCtlDirectionUdpFormatBuilder);
						break;
				}
				return newCtlDirectionUdpFormatBuilder;
			}
		});
		
		return newCtlDirectionUdpFormatBuilder;
	}
	
}
