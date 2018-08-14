package org.entermediadb.websocket.mediaboat;

import java.io.StringReader;
import java.util.Collection;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.desktops.Desktop;
import org.entermediadb.desktops.DesktopEventListener;
import org.entermediadb.desktops.DesktopManager;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.ModuleManager;
import org.openedit.data.SearcherManager;
import org.openedit.users.User;
import org.openedit.util.StringEncryption;

public class MediaBoatConnection  extends Endpoint implements MessageHandler.Partial<String>, DesktopEventListener
{
	private static final Log log = LogFactory.getLog(MediaBoatConnection.class);
	private  RemoteEndpoint.Basic remoteEndpointBasic;
	protected JSONParser fieldJSONParser;
	protected ModuleManager fieldModuleManager;
	protected Desktop fieldDesktop;
	protected DesktopManager fieldDesktopManager;
	protected StringEncryption fieldStringEncrytion;

	protected SearcherManager fieldSearcherManager;
	
	
	public SearcherManager getSearcherManager()
	{
		if (fieldSearcherManager == null)
		{
			fieldSearcherManager = (SearcherManager)getModuleManager().getBean("searcherManager");
		}
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public StringEncryption getStringEncrytion()
	{
		if (fieldStringEncrytion == null)
		{
			fieldStringEncrytion = (StringEncryption)getModuleManager().getBean("stringEncryption");
		}
		return fieldStringEncrytion;
	}

	public void setStringEncrytion(StringEncryption inStringEncrytion)
	{
		fieldStringEncrytion = inStringEncrytion;
	}

	public DesktopManager getDesktopManager()
	{
		if (fieldDesktopManager == null)
		{
			fieldDesktopManager = (DesktopManager)getModuleManager().getBean("desktopManager");
		}

		return fieldDesktopManager;
	}

	public void setDesktopManager(DesktopManager inDesktopManager)
	{
		fieldDesktopManager = inDesktopManager;
	}

	public Desktop getDesktop()
	{
		if (fieldDesktop == null)
		{
			fieldDesktop = (Desktop)getModuleManager().getBean("desktop");
		}

		return fieldDesktop;
	}

	public void setDesktop(Desktop inDesktop)
	{
		fieldDesktop = inDesktop;
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
   		getDesktopManager().removeDesktop(getDesktop());

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
		try
		{
//			message = message.replaceAll("null", "\"null\"");
			JSONObject map = (JSONObject)getJSONParser().parse(new StringReader(message));
			String command = (String)map.get("command");
			getDesktop().setLastCommand(command);
			if ("login".equals(command)) //Return all the annotation on this asset
			{
				String username = (String)map.get("username");
		        getDesktop().setUserId(username);
				getDesktop().setDesktopId((String)map.get("desktopid"));
				getDesktop().setHomeFolder((String)map.get("homefolder"));
		   		getDesktop().setLastCompletedPercent(100);
		   		getDesktop().setServerName((String)map.get("server"));
		   		getDesktop().setListener(this);
		   		getDesktopManager().addDesktop(getDesktop());
		   		//authenticated
		   		User user = (User)getSearcherManager().getData("system", "user", username);
		   		String key = getStringEncrytion().getEnterMediaKey(user);
		   		
		   		//TODO: Authenticate
		   		
		   		JSONObject authenticated = new JSONObject();
		   		authenticated.put("command", "authenticated");
		   		authenticated.put("entermedia.key", key);
				sendMessage(authenticated);
		   		
			}
			else if("handledesktopsync".equals(command)){
				log.info("See what we got back");
				
				
				
			}
			else if ("annotation.modified".equals(command))
			{
			}
			else if ("annotation.removed".equals(command))
			{
			}
			else if ("annotation.added".equals(command)) //Return all the annotation on this asset
			{
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

	@Override
	public void downloadFiles(Collection inAssets)
	{
		JSONObject command = new JSONObject();
		command.put("command", "downloadto");
		command.put("assetpaths", inAssets);
		sendMessage(command);
		
	}

	@Override
	public void collectFileList(String path) {
		JSONObject command = new JSONObject();
		command.put("command", "sendfilelist");
		command.put("rootfolder", path);
		sendMessage(command);
		
	}

	
}
