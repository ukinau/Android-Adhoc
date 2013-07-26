package android.tether.dtn.algorithm.spray_and_wait;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import android.tether.dtn.DtnMessage;
import android.tether.dtn.FormatBuilder;
import android.tether.dtn.FormatBuilder.XmlReaderInterface;
/**
 * Use B-CON,DEMAND_HAVE_MESSAGES_RESULT[MESSAGE_ONLY](Not DtnMessage but SprayAndWaitDtnMessage),DEMAND_ONLY
 * @author yukio7
 *
 * B-CON
 * <b-con>
 *	<mac-address>
 * </b-con>
 *
 * DEMAND_HAVE_MESSAGES
 * <demand>
 *		<have-message>
 *			<mac-address>
 * 		</have-message>
 *</demand>
 *
 * MEESAGE_KIND_RESULT_DEMAND_ONLY
 * <demand-result>
 * 	 <messages-count>
 *   <messages>
 *		<message>
 *			<mac-address>
 *			<body>
 *				<name>
 *				<address>
 *				<facebook>
 *			</body>
 *		</message>
 *	</messages>
 *</demand-result>
 */
public class SprayAndWaitFormatBuilder extends FormatBuilder {
	public final static int MESSAGE_KIND_RESULT_DEMAND_ONLY = 5;
	public ArrayList<SprayAndWaitDtnMessage> messages;

	public SprayAndWaitFormatBuilder() throws ParserConfigurationException {
		super();
		this.messages = new ArrayList<SprayAndWaitDtnMessage>();
	}
	
	@Override
	protected void setCustomAttribute(Element root,Document document){
		switch(this.message_kind){
			case MESSAGE_KIND_RESULT_DEMAND_ONLY:
				Element demand_result_ele = document.createElement("demand-result");
				Element messages_count_ele = document.createElement("messages-count");
				int messages_count = messages.size();
				Text messages_count_innerText = document.createTextNode(String.valueOf(messages_count));
				messages_count_ele.appendChild(messages_count_innerText);
				demand_result_ele.appendChild(messages_count_ele);
				if(messages_count>0){
					setMessages(demand_result_ele, document);
				}
				root.appendChild(demand_result_ele);
				break;
		}
	}
	
	@Override
	protected void setMessages(Element root,Document document){
		Element messages_ele = document.createElement("messages");
		for(int i=0;i<messages.size();i++){
			Element message_ele = document.createElement("message");
			Element body_ele = document.createElement("body");
			Element mac_address_ele = document.createElement("mac-address");
			Text mac_address_innerText = document.createTextNode(messages.get(i).mac_address);
			mac_address_ele.appendChild(mac_address_innerText);
			message_ele.appendChild(mac_address_ele);
			
			// token add (original spray and wait)
			Element token_ele = document.createElement("token");
			Text token_innerText = document.createTextNode(String.valueOf(messages.get(i).token));
			token_ele.appendChild(token_innerText);
			message_ele.appendChild(token_ele);
			
			Element name_ele = document.createElement("name");
			Text name_innerText = document.createTextNode(messages.get(i).name);
			name_ele.appendChild(name_innerText);
			body_ele.appendChild(name_ele);
			
			Element address_ele = document.createElement("address");
			Text address_innerText = document.createTextNode(messages.get(i).address);
			address_ele.appendChild(address_innerText);
			body_ele.appendChild(address_ele);
			
			Element facebook_ele = document.createElement("facebook");
			Text facebook_innnerText = document.createTextNode(messages.get(i).facebook);
			facebook_ele.appendChild(facebook_innnerText);
			body_ele.appendChild(facebook_ele);
			
			message_ele.appendChild(body_ele);
			messages_ele.appendChild(message_ele);
		}
		root.appendChild(messages_ele);
	}
	
	public static SprayAndWaitFormatBuilder read(String xml) throws ParserConfigurationException, SAXException, IOException{
		final SprayAndWaitFormatBuilder newSprayAndWaitBuilder = new SprayAndWaitFormatBuilder();
		FormatBuilder.read(xml, new FormatBuilder.XmlReaderInterface(){
			public FormatBuilder readNodes(NodeList nodes, FormatBuilder newBuilder, int kind) {
				newSprayAndWaitBuilder.message_kind = kind;
				switch(kind){
					case MESSAGE_KIND_B_CON:
						Node bcon_container_node = nodes.item(1);
						readBcon(bcon_container_node, newSprayAndWaitBuilder);
						break;
					case MESSAGE_KIND_REQUEST_DEMAND_ONLY:
						Node demand_container_node = nodes.item(1);
						readDemands(demand_container_node, newSprayAndWaitBuilder, DEMAND_TYPE_HAVE_MESSAGES);
						break;
					case MESSAGE_KIND_RESULT_DEMAND_ONLY:
						Node demand_result_container_node = nodes.item(1);
						readDemandHaveMessagesResult(demand_result_container_node,newSprayAndWaitBuilder);
						break;
				}
				return newSprayAndWaitBuilder;
			}
		
		});
		return newSprayAndWaitBuilder;
	}

	protected static void readDemandHaveMessagesResult(Node demand_result_container_node, SprayAndWaitFormatBuilder newBuilder){
		NodeList nodes = demand_result_container_node.getChildNodes();
		Node messages_count_node = nodes.item(0);
		int messages_count = Integer.parseInt(messages_count_node.getFirstChild().getNodeValue());
		if(messages_count > 0){
			readMessages(nodes.item(1),newBuilder);
		}
	}
	
	protected static void readMessages(Node message_container_node,SprayAndWaitFormatBuilder newBuilder){
		NodeList messageNodes = message_container_node.getChildNodes();
		if(messageNodes != null){
			for(int i=0;i<messageNodes.getLength();i++){
				SprayAndWaitDtnMessage newMessage = new SprayAndWaitDtnMessage();
				Node messageNode = messageNodes.item(i);
				Node mac_address_node = messageNode.getChildNodes().item(0);
				if(mac_address_node != null){
					newMessage.mac_address = mac_address_node.getFirstChild().getNodeValue();
				}
				Node token_node =  messageNode.getChildNodes().item(1);
				if(token_node != null){
					newMessage.token = Integer.parseInt(token_node.getFirstChild().getNodeValue());
				}
				Node body_node = messageNode.getChildNodes().item(2);
				if(body_node != null){
					NodeList body_properties = body_node.getChildNodes();
					for(int j=0;j<body_properties.getLength();j++){
						String property_name = body_properties.item(j).getNodeName();
						if(property_name.equals("name")){
							newMessage.name = body_properties.item(j).getFirstChild().getNodeValue();
						} else if(property_name.equals("address")){
							newMessage.address = body_properties.item(j).getFirstChild().getNodeValue();
						} else if(property_name.equals("facebook")){
							newMessage.facebook = body_properties.item(j).getFirstChild().getNodeValue(); 
						}
					}
				}// endif(body != null)
				newBuilder.messages.add(newMessage);
			}// endfor(i<messageNodes.getLength())
		}// endif(messageNodes != null)
	}
	
}
