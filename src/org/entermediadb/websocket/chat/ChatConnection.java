package org.entermediadb.websocket.chat;

import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.projects.LibraryCollection;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.ModuleManager;
import org.openedit.data.SearcherManager;

public class ChatConnection extends Endpoint implements  MessageHandler.Partial<String> {
	private static final Log log = LogFactory.getLog(ChatConnection.class);
	private RemoteEndpoint.Basic remoteEndpointBasic;
	protected JSONParser fieldJSONParser;
	protected ModuleManager fieldModuleManager;
	protected SearcherManager fieldSearcherManager;
	protected String fieldCurrentConnectionId;
	protected ChatServer fieldChatServer;
	
	
	
	
	public ChatServer getChatServer() {
		if (fieldChatServer == null) {
			fieldChatServer = (ChatServer) getModuleManager().getBean("system", "chatServer");
		}

		return fieldChatServer;
	}

	public void setChatServer(ChatServer fieldChatServer) {
		this.fieldChatServer = fieldChatServer;
	}

	public String getCurrentConnectionId() {
		return fieldCurrentConnectionId;
	}

	public void setCurrentConnectionId(String inCurrentConnectionId) {
		fieldCurrentConnectionId = inCurrentConnectionId;
	}

	public SearcherManager getSearcherManager() {
		if (fieldSearcherManager == null) {
			fieldSearcherManager = (SearcherManager) getModuleManager().getBean("searcherManager");
		}
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager) {
		fieldSearcherManager = inSearcherManager;
	}

	public ModuleManager getModuleManager() {
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager) {
		fieldModuleManager = inModuleManager;
	}

	protected StringBuffer fieldBufferedMessage;

	@Override
	public void onError(Session session, Throwable throwable) {
		// TODO Auto-generated method stub
		super.onError(session, throwable);
	}

	@Override
	public void onClose(Session session, CloseReason closeReason) {
		super.onClose(session, closeReason);

	}

	@Override
	public void onOpen(Session session, EndpointConfig endpointConfig) {
		// javax.servlet.http.HttpSession http =
		// (javax.servlet.http.HttpSession)session.getUserProperties().get("javax.servlet.http.HttpSession");

//       Enumeration<String> enuma = http.getAttributeNames();
//       while(enuma.hasMoreElements())
//       {
//           System.out.println(enuma.nextElement());
//       }

//        
//       if( getModuleManager() == null)
//       {
//	        ModuleManager manager  = (ModuleManager)http.getAttribute("moduleManager");
//	        if( manager != null )
//	        {
//	        }
//       }
		ModuleManager modulemanager = (ModuleManager) session.getUserProperties().get("moduleManager");
		if (modulemanager == null) {
			throw new RuntimeException("modulemanager did not get set, Web site must be accessed with a session");
		}
		setModuleManager(modulemanager);

		remoteEndpointBasic = session.getBasicRemote();
		// ws://localhost:8080/entermedia/services/websocket/echoProgrammatic?catalogid=emsite/catalog&collectionid=102

		// TODO: Load from spring0
		// AnnotationConnection connection = new
		// AnnotationConnection(getSearcherManager(),catalogid,
		// collectionid,http,remoteEndpointBasic, this);
		session.addMessageHandler(this);
		  getChatServer().addConnection(this);	
		// session.addMessageHandler(new EchoMessageHandlerBinary(remoteEndpointBasic));
	}

	public JSONParser getJSONParser() {
		if (fieldJSONParser == null) {
			fieldJSONParser = new JSONParser();
		}
		return fieldJSONParser;
	}

	protected StringBuffer getBufferedMessage() {
		if (fieldBufferedMessage == null) {
			fieldBufferedMessage = new StringBuffer();
		}

		return fieldBufferedMessage;
	}

	
	
	
	@Override
	public synchronized void onMessage(String inData, boolean completed){
		getBufferedMessage().append(inData);
		if (!completed) {
			return;
		}
		String message = getBufferedMessage().toString();
		fieldBufferedMessage = null;

//		if (remoteEndpointBasic != null)
//		{
//			return;
//		}
		try {
//			message = message.replaceAll("null", "\"null\"");
			JSONObject map = (JSONObject) getJSONParser().parse(new StringReader(message));
			String command = (String) map.get("command");
			
			
			if ("login".equals(command)) //Return all the annotation on this asset
			{
				receiveLogin(map);
			}
			else if("messagereceived".equals(command)){
				getChatServer().saveMessage(map);
				
			}
//			else if ("folderedited".equals(command)) //Return all the annotation on this asset
//			{
//				String foldername = (String)map.get("foldername");
//			}
//			else if ("busychanged".equals(command)) //Return all the annotation on this asset
//			{
//				boolean busy = (boolean)map.get("isbusy");
//			}
//			else if ("folderedited".equals(command)) //Return all the annotation on this asset
//			{
//				String foldername = (String)map.get("foldername");
//			}
			getChatServer().broadcastMessage(map);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e);
			e.printStackTrace();
		}
	}

	

	protected void receiveLogin(JSONObject map)
	{
//		String username = (String)map.get("username");
//		//authenticated
//		String keyorpasswordentered = (String)map.get("entermedia.key");
//		User user = (User)getSearcherManager().getData("system", "user", username);
//		if( user == null)
//		{
//			JSONObject authenticated = new JSONObject();
//			authenticated.put("command", "authenticatefail");
//			authenticated.put("reason", "User did not exist");
//			sendMessage(authenticated);
//			return;
//		}
//		String key = getStringEncrytion().getEnterMediaKey(user);
//
//		if( !key.equals(keyorpasswordentered))
//		{
//			//check password
//			String clearpassword = getStringEncrytion().decryptIfNeeded(user.getPassword());
//			if( !keyorpasswordentered.equals(clearpassword))
//			{
//		   		JSONObject authenticated = new JSONObject();
//		   		authenticated.put("command", "authenticatefail");
//		   		authenticated.put("reason", "Password did not match");
//				sendMessage(authenticated);
//				return;
//			}
//		}
//		String connectionid = (String)map.get("connectionid");
//		setCurrentConnectionId(connectionid);
//		JSONObject authenticated = new JSONObject();
//		authenticated.put("command", "authenticated");
//		authenticated.put("entermedia.key", key);
//		
//		Collection existing = (Collection)map.get("existingcollections");
//		for (Iterator iterator = existing.iterator(); iterator.hasNext();)
//		{
//			String name = (String) iterator.next();
//			//Looup this users collections by name?
//			
//		}
//		
//		sendMessage(authenticated);
	}

	public void sendMessage(JSONObject json) {
		try {
			String command = (String) json.get("command");
			json.put("connectionid", getCurrentConnectionId());
			remoteEndpointBasic.sendText(json.toJSONString());
			log.info("sent " + command + " to  " + getCurrentConnectionId());
		} catch (Exception e) {
			log.error(e);
//			throw new OpenEditException(e);
		}
	}

	public RemoteEndpoint.Basic getRemoteEndpointBasic() {
		return remoteEndpointBasic;

	}


	

}
