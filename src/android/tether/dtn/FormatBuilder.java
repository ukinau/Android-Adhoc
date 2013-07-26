package android.tether.dtn;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

public class FormatBuilder {
	public static final int MESSAGE_KIND_B_CON = 0;
	/*<b-con>
	 * 	<my-mac-address>
	 *</b-con>
	 */
	public static final int MESSAGE_KIND_REQUEST_DEMAND_ONLY = 1;
	/*<demand>
		<have-message>
			<mac-address>
		</have-message>
	</demand>*/
	public static final int MESSAGE_KIND_MESSAGE_ONLY = 2;
	/*
	<messages>
		<message>
			<body>
				<name>
				<address>
				<facebook>
			</body>
			<mac-address>
		</message>
	</messages>
	 */
	public static final int MESSAGE_KIND_REQUEST_MESSAGE_AND_DEMAND = 3;
	/*
	<messages>
		<message>
			<mac-address>
			<body>
				<name>
				<address>
				<facebook>
			</body>
		</message>
	</messages>
	<demand>
		<demand-message>
			<mac-address>
		</demand-message>
	</demand>
	 */
	public static final int DEMAND_TYPE_HAVE_MESSAGES = 99;
	public static final int DEMAND_TYPE_DEMAND_MESSAGES = 100;

	
	public int message_kind;
	public ArrayList<String> demand_messages;
	public ArrayList<String> have_messages;
	public ArrayList<DtnMessage> messages;
	public String myMacAddress; // for b-con
	
	public FormatBuilder() throws ParserConfigurationException{
		demand_messages = new ArrayList<String>();
		have_messages = new ArrayList<String>();
		messages = new ArrayList<DtnMessage>();
	}
	
	public String buildXml() throws TransformerException, ParserConfigurationException{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		DOMImplementation domImpl  = builder.getDOMImplementation();
		
		Document document = domImpl.createDocument("","ROOT",null);
		Element root = document.getDocumentElement();
		Element kind_ele = document.createElement("kind");
		Text kind_innerText = document.createTextNode(String.valueOf(this.message_kind));
		kind_ele.appendChild(kind_innerText);
		root.appendChild(kind_ele);
		
		switch(this.message_kind){
			case FormatBuilder.MESSAGE_KIND_B_CON:
				setBcon(root,document);
			case FormatBuilder.MESSAGE_KIND_REQUEST_DEMAND_ONLY:
				setDemand(root,document,DEMAND_TYPE_HAVE_MESSAGES);
				break;
			case FormatBuilder.MESSAGE_KIND_MESSAGE_ONLY:
				setMessages(root,document);
				break;
			case FormatBuilder.MESSAGE_KIND_REQUEST_MESSAGE_AND_DEMAND:
				setDemand(root,document,DEMAND_TYPE_DEMAND_MESSAGES);
			    setMessages(root,document);
				break;
			default:
				setCustomAttribute(root,document);
				break;
		}
		
		TransformerFactory tf = TransformerFactory.newInstance();
	    
	    Transformer transformer = tf.newTransformer();
	    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
	    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		
		StringWriter writer = new StringWriter();// <-ここでは文字列として出力している
	    StreamResult result = new StreamResult(writer);

	    DOMSource source = new DOMSource(document);
	    transformer.transform(source, result);

		
	    return writer.toString(); 
	}
	
	protected void setCustomAttribute(Element root,Document document){
	}
	
	protected void setBcon(Element root,Document document){
		Element bcon_ele = document.createElement("b-con");
		Element myMacAddress_ele = document.createElement("my-mac-address");
		Text myMacAddress_innerText = document.createTextNode(myMacAddress);
		myMacAddress_ele.appendChild(myMacAddress_innerText);
		bcon_ele.appendChild(myMacAddress_ele);
		root.appendChild(bcon_ele);
	}
	
	protected void setDemand(Element root,Document document,int demand_kind){
		switch(demand_kind){
			case DEMAND_TYPE_HAVE_MESSAGES:
				Element demand_ele = document.createElement("demand");
				for(int i=0;i<have_messages.size();i++){
					Element have_message_ele = document.createElement("have-message");
					Element mac_address_ele = document.createElement("mac-address");
					Text mac_address_innerText = document.createTextNode(have_messages.get(i));
					mac_address_ele.appendChild(mac_address_innerText);
					have_message_ele.appendChild(mac_address_ele);
					demand_ele.appendChild(have_message_ele);
				}
				root.appendChild(demand_ele);
				break;
			
			case DEMAND_TYPE_DEMAND_MESSAGES:
				demand_ele = document.createElement("demand");
				for(int i=0;i<demand_messages.size();i++){
					Element demand_message_ele = document.createElement("demand_message");
					Element mac_address_ele = document.createElement("mac-address");
					Text mac_address_innerText = document.createTextNode(demand_messages.get(i));
					mac_address_ele.appendChild(mac_address_innerText);
					demand_message_ele.appendChild(mac_address_ele);
					demand_ele.appendChild(demand_message_ele);
				}
				root.appendChild(demand_ele);
				break;
		}
	}
	
	protected void setMessages(Element root,Document document){
		Element messages_ele = document.createElement("messages");
		for(int i=0;i<messages.size();i++){
			Element message_ele = document.createElement("message");
			Element body_ele = document.createElement("body");
			Element mac_address_ele = document.createElement("mac-address");
			Text mac_address_innerText = document.createTextNode(messages.get(i).mac_address);
			mac_address_ele.appendChild(mac_address_innerText);
			message_ele.appendChild(mac_address_ele);
			
			
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
	
	
	public static FormatBuilder read(String xml) throws ParserConfigurationException, SAXException, IOException{
		FormatBuilder newBuilder = new FormatBuilder();
		// function reading xml
		InputStream bais = new ByteArrayInputStream(xml.getBytes("UTF-8")); 
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();
		factory.setIgnoringElementContentWhitespace(true);
		factory.setIgnoringComments(true);
		Node document    = builder.parse(bais);
		
		Node root = document.getFirstChild();
		NodeList nodes = root.getChildNodes();
		Node kindNode = nodes.item(0);
		
		int kind = Integer.parseInt(kindNode.getFirstChild().getNodeValue());
		newBuilder.message_kind = kind;
		
		switch(kind){
			case FormatBuilder.MESSAGE_KIND_B_CON:
				Node bcon_container_node = nodes.item(1);
				newBuilder = readBcon(bcon_container_node, newBuilder);
				break;
			case FormatBuilder.MESSAGE_KIND_REQUEST_DEMAND_ONLY:
				Node demand_container_node = nodes.item(1);
				newBuilder = readDemands(demand_container_node,newBuilder,DEMAND_TYPE_HAVE_MESSAGES);
				break;
			case FormatBuilder.MESSAGE_KIND_MESSAGE_ONLY:
				Node message_container_node = nodes.item(1);
				newBuilder = readMessages(message_container_node,newBuilder);
				break;
			case FormatBuilder.MESSAGE_KIND_REQUEST_MESSAGE_AND_DEMAND:
				demand_container_node = nodes.item(1);
				message_container_node = nodes.item(2);
				newBuilder = readMessages(message_container_node,newBuilder);
				newBuilder = readDemands(demand_container_node,newBuilder,DEMAND_TYPE_DEMAND_MESSAGES);
				break;
		}
		return newBuilder; 
	}
	
	public static FormatBuilder read(String xml,XmlReaderInterface readerImpl) throws ParserConfigurationException, SAXException, IOException{
		FormatBuilder newBuilder = new FormatBuilder();
		// function reading xml
		InputStream bais = new ByteArrayInputStream(xml.getBytes("UTF-8")); 
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();
		factory.setIgnoringElementContentWhitespace(true);
		factory.setIgnoringComments(true);
		Node document    = builder.parse(bais);
		
		Node root = document.getFirstChild();
		NodeList nodes = root.getChildNodes();
		Node kindNode = nodes.item(0);
		
		int kind = Integer.parseInt(kindNode.getFirstChild().getNodeValue());
		newBuilder.message_kind = kind;		
		newBuilder = readerImpl.readNodes(nodes,newBuilder,kind);
		return newBuilder; 
	}
	
	public interface XmlReaderInterface{
		public FormatBuilder readNodes(NodeList nodes,FormatBuilder newBuilder,int kind);
	}
	
	protected static int readMessageKind(String xml) throws ParserConfigurationException, SAXException, IOException{
		InputStream bais = new ByteArrayInputStream(xml.getBytes("UTF-8")); 
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();
		factory.setIgnoringElementContentWhitespace(true);
		factory.setIgnoringComments(true);
		Node document    = builder.parse(bais);
		
		Node root = document.getFirstChild();
		NodeList nodes = root.getChildNodes();
		Node kindNode = nodes.item(0);
		
		int kind = Integer.parseInt(kindNode.getFirstChild().getNodeValue());
		return kind;
	}
	
	protected static FormatBuilder readBcon(Node bcon_container_node, FormatBuilder newBuilder){
		NodeList my_mac_address_nodes = bcon_container_node.getChildNodes();
		Node my_mac_address_node = my_mac_address_nodes.item(0);
		newBuilder.myMacAddress = my_mac_address_node.getFirstChild().getNodeValue();
		return newBuilder;
	}
	
	protected static FormatBuilder readDemands(Node demand_container_node,FormatBuilder newBuilder,int demand_kind){
		switch(demand_kind){
			case DEMAND_TYPE_HAVE_MESSAGES:
				NodeList have_message_nodes = demand_container_node.getChildNodes();
				if(have_message_nodes != null){
					for(int i=0;i<have_message_nodes.getLength();i++){
						Node have_message_node = have_message_nodes.item(i);
						Node mac_address_node = have_message_node.getFirstChild();
						newBuilder.have_messages.add(mac_address_node.getFirstChild().getNodeValue());
					}
				}
				break;
			case DEMAND_TYPE_DEMAND_MESSAGES:
				NodeList demand_message_nodes = demand_container_node.getChildNodes();
				if(demand_message_nodes != null){
					for(int i=0;i<demand_message_nodes.getLength();i++){
						Node demand_message_node = demand_message_nodes.item(i);
						Node mac_address_node = demand_message_node.getFirstChild();
						newBuilder.demand_messages.add(mac_address_node.getFirstChild().getNodeValue());
					}
				}
				break;
		}
		return newBuilder;
	}
	
	protected static FormatBuilder readMessages(Node message_container_node,FormatBuilder newBuilder){
		NodeList messageNodes = message_container_node.getChildNodes();
		if(messageNodes != null){
			for(int i=0;i<messageNodes.getLength();i++){
				DtnMessage newMessage = new DtnMessage();
				Node messageNode = messageNodes.item(i);
				Node mac_address_node = messageNode.getChildNodes().item(0);
				if(mac_address_node != null){
					newMessage.mac_address = mac_address_node.getFirstChild().getNodeValue();
				}
				Node body_node = messageNode.getChildNodes().item(1);
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
		return newBuilder;
	}
	
	
}
