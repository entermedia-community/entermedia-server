package org.entermediadb.websocket.annotation;

import java.io.StringReader;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.ModuleManager;
import org.openedit.data.SearcherManager;

//@ServerEndpoint(value = "/org/entermediadb/websocket/annotation/AnnotationConnection") 
public class AnnotationConnection  extends Endpoint implements MessageHandler.Partial<String>
{
	private static final Log log = LogFactory.getLog(AnnotationConnection.class);
	private  RemoteEndpoint.Basic remoteEndpointBasic;
	protected SearcherManager fieldSearcherManager;
	protected String fieldCollectionId;
	protected String fieldCatalogId;
	protected JSONParser fieldJSONParser;
	protected AnnotationManager fieldAnnotationManager;
	protected ModuleManager fieldModuleManager;
	
	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	protected StringBuffer fieldBufferedMessage;

	 @Override
	public void onError(Session session, Throwable throwable)
	{
		// TODO Auto-generated method stub
		super.onError(session, throwable);
	}
	 
	@Override
	public void onClose(Session session, CloseReason closeReason) {
		getAnnotationManager().removeConnection(this);
		super.onClose(session, closeReason);
	}
   @Override
   public void onOpen(Session session, EndpointConfig endpointConfig) 
   {
      // javax.servlet.http.HttpSession http = (javax.servlet.http.HttpSession)session.getUserProperties().get("javax.servlet.http.HttpSession");
	   
//       Enumeration<String> enuma = http.getAttributeNames();
//       while(enuma.hasMoreElements())
//       {
//           System.out.println(enuma.nextElement());
//       }
       
       String catalogid = session.getPathParameters().get("catalogid");
//        
//       if( getModuleManager() == null)
//       {
//	        ModuleManager manager  = (ModuleManager)http.getAttribute("moduleManager");
//	        if( manager != null )
//	        {
//	        }
//       }
       
        ModuleManager modulemanager = (ModuleManager)session.getUserProperties().get("moduleManager");
        if( modulemanager == null)
        {
        	throw new RuntimeException("modulemanager did not get set, lacking session?");
        }
    	setModuleManager(modulemanager);
    	
        AnnotationManager server = (AnnotationManager) modulemanager.getBean("system","annotationManager");
        fieldAnnotationManager = server;
        
     	remoteEndpointBasic = session.getBasicRemote();
   		fieldCatalogId = catalogid;
       
       //ws://localhost:8080/entermedia/services/websocket/echoProgrammatic?catalogid=emsite/catalog&collectionid=102
       
       //TODO: Load from spring0
       //AnnotationConnection connection = new AnnotationConnection(getSearcherManager(),catalogid, collectionid,http,remoteEndpointBasic, this);
       getAnnotationManager().addConnection(this);	
       session.addMessageHandler(this);
     //  session.addMessageHandler(new EchoMessageHandlerBinary(remoteEndpointBasic));
   }
	
	
	public AnnotationManager getAnnotationManager()
	{
		
		return fieldAnnotationManager;
	}
	
	public JSONParser getJSONParser()
	{
		if (fieldJSONParser == null) {
			fieldJSONParser = new JSONParser();
		}
		return fieldJSONParser;
	}

	protected StringBuffer getBufferedMessage()
	{
		if (fieldBufferedMessage == null)
		{
			fieldBufferedMessage = new StringBuffer();
		}

		return fieldBufferedMessage;
	}
		
	
	@Override
	public synchronized void onMessage(String inData, boolean completed)
	{		
		getBufferedMessage().append(inData);
		if(!completed)
		{
			return;
		}
		String message = getBufferedMessage().toString();
		if( message != null && message.isEmpty())
		{
			return; //Ping?
		}
		fieldBufferedMessage = null;
		
//		if (remoteEndpointBasic != null)
//		{
//			return;
//		}
		try
		{
//			message = message.replaceAll("null", "\"null\"");
			JSONObject map = (JSONObject)getJSONParser().parse(new StringReader(message));
			String command = (String)map.get("command");
			String catalogid = (String)map.get("catalogid");
			String assetid = (String)map.get("assetid");
			
			if ("annotation.modified".equals(command))
			{
//				JSONObject obj = new JSONObject();
				getAnnotationManager().annotationModified(this, map, message, catalogid,assetid);
			}
			else if ("annotation.removed".equals(command))
			{
//				JSONObject obj = new JSONObject();
				getAnnotationManager().annotationRemoved(this, map, message, catalogid, assetid);
			}
			else if ("annotation.added".equals(command)) //Return all the annotation on this asset
			{
				//see if ID is set
//				JSONObject json = new JSONObject();
//				json.putAll(map);
				//command.annotationdata
				//obj.put("stuff", "array of annotations");
				//remoteEndpointBasic.sendText(message);
				getAnnotationManager().annotationAdded(this, map, message, catalogid, assetid);
			} 
			else if ("removeall".equals(command))
			{
				getAnnotationManager().annotationsRemoved(this, map, message, catalogid, assetid);
			}
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			log.error(e);
			e.printStackTrace();
		}
	}
	/*
	 * private static void broadcast(String msg) { for (ChatAnnotation client :
	 * connections) { try { synchronized (client) {
	 * client.session.getBasicRemote().sendText(msg); } } catch (IOException e)
	 * { log.debug("Chat Error: Failed to send message to client", e);
	 * connections.remove(client); try { client.session.close(); } catch
	 * (IOException e1) { // Ignore } String message = String.format("* %s %s",
	 * client.nickname, "has been disconnected."); broadcast(message); } }
	 */

	public void sendMessage(JSONObject json)
	{
		try
		{
			remoteEndpointBasic.sendText(json.toJSONString());
			log.info("sent message");
		}
		catch (Exception e)
		{
			log.error(e);
//			throw new OpenEditException(e);
		}
	}
	
	public  RemoteEndpoint.Basic getRemoteEndpointBasic()
	{
		return remoteEndpointBasic;
		
	}
}
