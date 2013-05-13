package org.entermedia.email;

import java.io.Serializable;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.openedit.OpenEditException;

public class ElasticPostMailStatus extends PostMailStatus implements Serializable{

	private static final long serialVersionUID = -4640935230997089177L;
	
	protected int fieldRecipients;
	protected int fieldDelivered;
	protected int fieldFailed;
	protected int fieldPending;
	protected int fieldOpened;
	protected int fieldClicked;
	protected int fieldUnsubscribed;
	protected int fieldAbusereports;
	
	public int getRecipients() {
		return fieldRecipients;
	}
	public void setRecipients(int inRecipients) {
		fieldRecipients = inRecipients;
	}
	public int getDelivered() {
		return fieldDelivered;
	}
	public void setDelivered(int inDelivered) {
		fieldDelivered = inDelivered;
	}
	public int getFailed() {
		return fieldFailed;
	}
	public void setFailed(int inFailed) {
		fieldFailed = inFailed;
	}
	public int getPending() {
		return fieldPending;
	}
	public void setPending(int inPending) {
		fieldPending = inPending;
	}
	public int getOpened() {
		return fieldOpened;
	}
	public void setOpened(int inOpened) {
		fieldOpened = inOpened;
	}
	public int getClicked() {
		return fieldClicked;
	}
	public void setClicked(int inClicked) {
		fieldClicked = inClicked;
	}
	public int getUnsubscribed() {
		return fieldUnsubscribed;
	}
	public void setUnsubscribed(int inUnsubscribed) {
		fieldUnsubscribed = inUnsubscribed;
	}
	public int getAbusereports() {
		return fieldAbusereports;
	}
	public void setAbusereports(int inAbusereports) {
		fieldAbusereports = inAbusereports;
	}
	
	public static ElasticPostMailStatus parseXML(String xml){
		ElasticPostMailStatus status = new ElasticPostMailStatus();
		status.setSent(false);
		try{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder dBuilder = factory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(xml)));
			if (doc.hasChildNodes()){
				NodeList nl = doc.getChildNodes();
				processNodes(nl,status);
			}
		} catch (Exception e){
			e.printStackTrace();
			throw new OpenEditException(e.getMessage(),e);
		}
		return status;
	}
	
	protected static void processNodes(NodeList nl, ElasticPostMailStatus status){
		for (int i=0; i < nl.getLength(); i++){
			Node node = nl.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE || node.getNodeName()==null)
				continue;
			if (node.hasAttributes()){
				NamedNodeMap nm = node.getAttributes();
				for (int j=0; j < nm.getLength(); j++){
					Node n = nm.item(j);
					if (n.getNodeName()!=null && n.getNodeName().equalsIgnoreCase("id")){
						status.setId(n.getNodeValue());
						status.setSent(true);
					}
				}
			}
			if (node.getNodeName().equalsIgnoreCase("status")){
				status.setStatus(node.getTextContent());
			} else if (node.getNodeName().equalsIgnoreCase("recipients")){
				status.setRecipients(Integer.parseInt(node.getTextContent()));
			} else if (node.getNodeName().equalsIgnoreCase("failed")){
				status.setRecipients(Integer.parseInt(node.getTextContent()));
			} else if (node.getNodeName().equalsIgnoreCase("delivered")){
				status.setDelivered(Integer.parseInt(node.getTextContent()));
			} else if (node.getNodeName().equalsIgnoreCase("pending")){
				status.setPending(Integer.parseInt(node.getTextContent()));
			} else if (node.getNodeName().equalsIgnoreCase("opened")){
				status.setOpened(Integer.parseInt(node.getTextContent()));
			} else if (node.getNodeName().equalsIgnoreCase("clicked")){
				status.setClicked(Integer.parseInt(node.getTextContent()));
			} else if (node.getNodeName().equalsIgnoreCase("unsubscribed")){
				status.setUnsubscribed(Integer.parseInt(node.getTextContent()));
			} else if (node.getNodeName().equalsIgnoreCase("abusereports")){
				status.setAbusereports(Integer.parseInt(node.getTextContent()));
			}
			if (node.hasChildNodes()){
				processNodes(node.getChildNodes(),status);
			}
		}
	}
	
	public String toString(){
		StringBuilder buf = new StringBuilder();
		buf.append("id: ").append(fieldId).append(" [")
			.append("isSent: ").append(fieldSent).append(", ")
			.append("status: ").append(fieldStatus).append(", ")
			.append("recipients: ").append(fieldRecipients).append(", ")
			.append("failed: ").append(fieldFailed).append(", ")
			.append("delivered: ").append(fieldDelivered).append(", ")
			.append("pending: ").append(fieldPending).append(", ")
			.append("opened: ").append(fieldOpened).append(", ")
			.append("clicked: ").append(fieldClicked).append(", ")
			.append("unsubscribed: ").append(fieldUnsubscribed).append(", ")
			.append("abusereports: ").append(fieldAbusereports).append("]");
		return buf.toString();
	}

}
