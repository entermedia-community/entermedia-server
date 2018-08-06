package org.entermediadb.websocket.annotation;

import java.io.StringReader;

import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

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
	protected AnnotationServer fieldAnnotationServer;
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
		getAnnotationServer().removeConnection(this);
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
       String collectionid = session.getPathParameters().get("collectionid");
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
    	
        AnnotationServer server = (AnnotationServer) modulemanager.getBean("system","annotationServer");
        fieldAnnotationServer = server;
        
     	remoteEndpointBasic = session.getBasicRemote();
   		fieldCatalogId = catalogid;
   		fieldCollectionId = collectionid;
       
       //ws://localhost:8080/entermedia/services/websocket/echoProgrammatic?catalogid=emsite/catalog&collectionid=102
       
       //TODO: Load from spring0
       //AnnotationConnection connection = new AnnotationConnection(getSearcherManager(),catalogid, collectionid,http,remoteEndpointBasic, this);
       getAnnotationServer().addConnection(this);	
       session.addMessageHandler(this);
     //  session.addMessageHandler(new EchoMessageHandlerBinary(remoteEndpointBasic));
   }
	
	
	public AnnotationServer getAnnotationServer()
	{
		
		return fieldAnnotationServer;
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
			String collectionid = (String)map.get("collectionid");
			String assetid = (String)map.get("assetid");
			
			if ("server.loadAnnotatedAsset".equals(command)) //Return all the annotation on this asset
			{
				getAnnotationServer().loadAnnotatedAsset(this,catalogid, collectionid,assetid);
			}
			else if ("annotation.modified".equals(command))
			{
//				JSONObject obj = new JSONObject();
				getAnnotationServer().annotationModified(this, map, message, catalogid, collectionid,assetid);
			}
			else if ("annotation.removed".equals(command))
			{
//				JSONObject obj = new JSONObject();
				getAnnotationServer().annotationRemoved(this, map, message, catalogid, collectionid,assetid);
			}
			else if ("annotation.added".equals(command)) //Return all the annotation on this asset
			{
				//see if ID is set
//				JSONObject json = new JSONObject();
//				json.putAll(map);
				//command.annotationdata
				//obj.put("stuff", "array of annotations");
				//remoteEndpointBasic.sendText(message);
				getAnnotationServer().annotationAdded(this, map, message, catalogid, collectionid,assetid);
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
