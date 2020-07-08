package org.entermediadb.websocket.usernotify;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.desktops.Desktop;
import org.entermediadb.desktops.DesktopEventListener;
import org.entermediadb.desktops.DesktopManager;
import org.entermediadb.projects.LibraryCollection;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openedit.ModuleManager;
import org.openedit.data.SearcherManager;
import org.openedit.users.User;
import org.openedit.util.StringEncryption;

public class UserNotifyConnection  extends Endpoint implements MessageHandler.Partial<String>
{
	private static final Log log = LogFactory.getLog(UserNotifyConnection.class);
	private  RemoteEndpoint.Basic remoteEndpointBasic;
	protected JSONParser fieldJSONParser;
	protected ModuleManager fieldModuleManager;
	protected UserNotifyManager fieldUserNotifyManager;
	protected String fieldCurrentConnectionId;
	protected String fieldUserId;
	
	public String getUserId()
	{
		return fieldUserId;
	}


	public void setUserId(String inUserId)
	{
		fieldUserId = inUserId;
	}


	public UserNotifyManager getUserNotifyManager()
	{
		if (fieldUserNotifyManager == null)
		{
			fieldUserNotifyManager = (UserNotifyManager)getModuleManager().getBean("userNotifyManager");
		}

		return fieldUserNotifyManager;
	}

	
	public String getCurrentConnectionId()
	{
		return fieldCurrentConnectionId;
	}

	public void setCurrentConnectionId(String inCurrentConnectionId)
	{
		fieldCurrentConnectionId = inCurrentConnectionId;
	}

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
		super.onClose(session, closeReason);
   		getUserNotifyManager().removeConnection(this);

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
        	throw new RuntimeException("modulemanager did not get set, Web site must be accessed with a session");
        }
    	setModuleManager(modulemanager);
    	
     	remoteEndpointBasic = session.getBasicRemote();
       //ws://localhost:8080/entermedia/services/websocket/echoProgrammatic?catalogid=emsite/catalog&collectionid=102
       
       //TODO: Load from spring0
       //AnnotationConnection connection = new AnnotationConnection(getSearcherManager(),catalogid, collectionid,http,remoteEndpointBasic, this);
       session.addMessageHandler(this);
     //  session.addMessageHandler(new EchoMessageHandlerBinary(remoteEndpointBasic));
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
		JSONObject map;
		try
		{
			map = (JSONObject)getJSONParser().parse(new StringReader(message));
			getUserNotifyManager().onMessage(this,map);
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (ParseException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	public boolean sendMessage(JSONObject json)
	{
		try
		{
			String command = (String)json.get("command");
			json.put("connectionid",getCurrentConnectionId());
			remoteEndpointBasic.sendText(json.toJSONString());
			log.info("sent " + command + " to  " + getCurrentConnectionId() );
		}
		catch (Exception e)
		{
			log.error(e);
//			throw new OpenEditException(e);
			return false;
		}
		return true;
	}
	
	public  RemoteEndpoint.Basic getRemoteEndpointBasic()
	{
		return remoteEndpointBasic;
		
	}

}
