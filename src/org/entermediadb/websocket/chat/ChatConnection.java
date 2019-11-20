package org.entermediadb.websocket.chat;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;
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
import org.openedit.users.User;

public class ChatConnection extends Endpoint implements  MessageHandler.Partial<String> {
	private static final Log log = LogFactory.getLog(ChatConnection.class);
	private RemoteEndpoint.Basic remoteEndpointBasic;
	protected JSONParser fieldJSONParser;
	protected ModuleManager fieldModuleManager;
	protected SearcherManager fieldSearcherManager;
	protected String fieldCurrentConnectionId;
	protected ChatServer fieldChatServer;
	protected String fieldSessionID;
	protected String fieldUserId;
	
	
	public String getUserId()
	{
		return fieldUserId;
	}

	public void setUserId(String inUserId)
	{
		fieldUserId = inUserId;
	}

	public ChatServer getChatServer() {
		if (fieldChatServer == null) {
			fieldChatServer = (ChatServer) getModuleManager().getBean("system", "chatServer");
		}

		return fieldChatServer;
	}

	public void setChatServer(ChatServer fieldChatServer) {
		this.fieldChatServer = fieldChatServer;
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

		
		getChatServer().removeConnection(this);
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
		log.info(session.getId());
		Map props = endpointConfig.getUserProperties();
		HttpSession current = (HttpSession) session.getUserProperties().get(HttpSession.class.getName());
		User user = (User) current.getServletContext().getAttribute("user");
		if( user != null)
		{
			setUserId(user.getId());
		}
		String query = session.getQueryString();
		Map params = getQueryMap(query);
		
		fieldSessionID = (String) params.get("sessionid");
		
		
		
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

	public String getSessionId()
	{
		return fieldSessionID;
	}

	public void setSessionId(String inSessionID)
	{
		fieldSessionID = inSessionID;
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
			if(inData.length() == 0) {
				return;
			}
			JSONObject map = (JSONObject) getJSONParser().parse(new StringReader(message));
			String command = (String) map.get("command");
			
			//log.info("Command was: " + command);
			if ("keepalive".equals(command)) //Return all the annotation on this asset
			{
				//receiveLogin(map); 
				String userid = (String) map.get("userid");
				setUserId(userid);
			}
			else if("messagereceived".equals(command)){
			
				getChatServer().saveMessage(map);
				getChatServer().broadcastMessage(map);
				
			}
			else if("approveasset".equals(command)){
				
				getChatServer().approveAsset(map);
				getChatServer().broadcastMessage(map);
				
			}	
			else if("rejectasset".equals(command)){
				
				getChatServer().rejectAsset(map);
				getChatServer().broadcastMessage(map);
				
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e);
			e.printStackTrace();
		}
	}

	



	public void sendMessage(JSONObject json) {
		try {
			String command = (String) json.get("command");
			remoteEndpointBasic.sendText(json.toJSONString());
		} catch (Exception e) {
			log.error(e);
//			throw new OpenEditException(e);
		}
	}

	public RemoteEndpoint.Basic getRemoteEndpointBasic() {
		return remoteEndpointBasic;

	}

	 private  Map<String, String> getQueryMap(String query)  
     {  
         String[] params = query.split("&");  
         Map<String, String> map = new HashMap<String, String>();  
         for (String param : params)  
         {  
             String name = param.split("=")[0];  
             String value = param.split("=")[1];  
             map.put(name, value);  
         }  
         return map;  
    }
	

}
