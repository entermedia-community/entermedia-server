package org.entermediadb.websocket.mediaboat;

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
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.desktops.Desktop;
import org.entermediadb.desktops.DesktopEventListener;
import org.entermediadb.desktops.DesktopManager;
import org.entermediadb.projects.LibraryCollection;
import org.json.simple.JSONObject;
import org.openedit.util.JSONParser;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.data.SearcherManager;
import org.openedit.users.User;
import org.openedit.util.StringEncryption;

public class MediaBoatConnection extends Endpoint implements MessageHandler.Partial<String>, DesktopEventListener
{
	private static final Log log = LogFactory.getLog(MediaBoatConnection.class);
	private  RemoteEndpoint.Basic remoteEndpointBasic;
	protected JSONParser fieldJSONParser;
	protected ModuleManager fieldModuleManager;
	protected Desktop fieldDesktop;
	protected DesktopManager fieldDesktopManager;
	protected StringEncryption fieldStringEncrytion;
	protected SearcherManager fieldSearcherManager;
	protected String fieldCurrentConnectionId;
	protected TransactionQueue fieldTransactionQueue;
	
	public TransactionQueue getTransactionQueue()
	{
		if (fieldTransactionQueue == null)
		{
			fieldTransactionQueue = new TransactionQueue();
		}

		return fieldTransactionQueue;
	}
	public String getCurrentConnectionId()
	{
		return fieldCurrentConnectionId;
	}
	public void setCurrentConnectionId(String inCurrentConnectionId)
	{
		fieldCurrentConnectionId = inCurrentConnectionId;
	}

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
			
			getTransactionQueue().onMessage(map);
			if ("login".equals(command)) //Return all the annotation on this asset
			{
				receiveLogin(map);
			}	
			else if ("renew_entermediakey".equals(command)) //Return all the annotation on this asset
			{
				String newkey = getStringEncrytion().getTempEnterMediaKey(getDesktop().getUser());
				JSONObject authenticated = new JSONObject();
				authenticated.put("command", "renew_completed");
				authenticated.put("entermedia.key", newkey);  //TODO: Refresh every 24hours
				
				sendMessage(authenticated);
			}	
			else if ("folderedited".equals(command)) //Return all the annotation on this asset
			{
				String foldername = (String)map.get("foldername");
				getDesktop().addEditedCollection(foldername);
			}
			else if ("busychanged".equals(command)) //Return all the annotation on this asset
			{
				boolean busy = (boolean)map.get("isbusy");
				getDesktop().setBusy(busy);
			}
			else if ("folderedited".equals(command)) //Return all the annotation on this asset
			{
				String foldername = (String)map.get("foldername");
				getDesktop().addEditedCollection(foldername);
			}
			else if( "downloadasset_completed".equals(command)) //Return all the annotation on this asset
			{
				String id = (String)map.get("userdownloadid");
				String catalogid = (String)map.get("catalogid");
				//Update DB					
				MediaArchive archive = getMediaArchive(catalogid);
				getDesktop().setUserDownloadComplete(archive,id);
			}
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			log.error(e);
			e.printStackTrace();
		}
	}
	
	public MediaArchive getMediaArchive(String inCatalogid)
	{
		if (inCatalogid == null)
		{
			return null;
		}
		MediaArchive archive = (MediaArchive) getModuleManager().getBean(inCatalogid, "mediaArchive");
		return archive;
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

	protected void receiveLogin(JSONObject map)
	{
		String username = (String)map.get("username");
		User user = (User)getSearcherManager().getData("system", "user", username);
		getDesktop().setUser(user);
		getDesktop().setDesktopId((String)map.get("desktopid"));
		getDesktop().setHomeFolder((String)map.get("homefolder"));
		getDesktop().setLastCompletedPercent(100);
		getDesktop().setServerName((String)map.get("server"));
		getDesktop().setListener(this);
		getDesktopManager().setDesktop(getDesktop());
		//authenticated
		String keyonly = (String)map.get("entermedia.key");
		if( keyonly == null)
		{
			throw new OpenEditException("Key not found");
		}
		if( user == null) //TODO: Authenticate key (with expiration)
		{
			JSONObject authenticated = new JSONObject();
			authenticated.put("command", "authenticatefail");
			authenticated.put("reason", "User did not exist");
			sendMessage(authenticated);
			return;
		}
		boolean ok = getStringEncrytion().verifyEnterMediaKey(user, user.getPassword(), keyonly);

		if(!ok)
		{
			//check password
	   		JSONObject authenticated = new JSONObject();
	   		authenticated.put("command", "authenticatefail");
	   		authenticated.put("reason", "Key was not validated or was expired");
			sendMessage(authenticated);
		}
		String connectionid = (String)map.get("connectionid");
		setCurrentConnectionId(connectionid);
		
		JSONObject authenticated = new JSONObject();
		authenticated.put("command", "authenticated");
		authenticated.put("entermedia.key", keyonly);  //TODO: Refresh every 24hours
		
		Collection existing = (Collection)map.get("existingcollections");
		for (Iterator iterator = existing.iterator(); iterator.hasNext();)
		{
			String name = (String) iterator.next();
			//Looup this users collections by name?
			getDesktop().addEditedCollection(name);
		}
		
		sendMessage(authenticated);
	}

	public void sendMessage(JSONObject json)
	{
		try
		{
			String command = (String)json.get("command");
			//json.put("connectionid",getCurrentConnectionId());
			remoteEndpointBasic.sendText(json.toJSONString());
			log.info("sent " + command + " to  " + getCurrentConnectionId() );
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
	public void openRemoteFolder(String fullpath)
	{
		JSONObject command = new JSONObject();
		command.put("command", "openremotefolder");
		command.put("fullpath", fullpath);
		sendMessage(command);
		
	}

	public void openCategoryPath(MediaArchive inArchive, String inCategoryPath)
	{
		JSONObject command = new JSONObject();
		command.put("command", "opencategorypath");
		command.put("categorypath", inCategoryPath);
		sendMessage(command);
		
	}

	@Override
	public void downloadFolders(MediaArchive inArchive, LibraryCollection inCollection, Map inRoot)
	{
		JSONObject command = new JSONObject();
		command.put("command", "downloadfolders");
		command.put("folderdetails", inRoot);
		command.put("rootname", inCollection.getName());
		command.put("catalogid",inArchive.getCatalogId());
		command.put("mediadbid",inArchive.getMediaDbId());
		command.put("collectionid",inCollection.getId());
		sendMessage(command);
	}

	
	public void openAsset(MediaArchive inArchive, Asset inAsset)
	{
		JSONObject command = new JSONObject();
		command.put("command", "openasset");
		command.put("catalogid",inArchive.getCatalogId());
		command.put("mediadbid",inArchive.getMediaDbId());
		command.put("assetid",inAsset.getId());
		command.put("filename",inAsset.getName());

		long time = inAsset.getDate("assetmodificationdate").getTime();
		if (time > 0)
		{
			command.put("assetmodificationdate", String.valueOf(time));
		}

		String urlroot = inArchive.asLinkToOriginal(inAsset);
		command.put("url",urlroot);

		sendMessage(command);
	}
	
	
	
	
	
	@Override
	public void importFiles(MediaArchive inArchive,LibraryCollection inCollection,String path) {
		JSONObject command = new JSONObject();
		command.put("command", "checkincollection");
		command.put("rootfolder", path);
		command.put("collectionid", inCollection.getId());
		command.put("catalogid", inArchive.getCatalogId());
		command.put("mediadbid", inArchive.getMediaDbId());
		command.put("revision", inCollection.getCurentRevision());
		sendMessage(command);
		
	}
	
	

	public void uploadFile(String inPath, Map inVariables)
	{
		JSONObject command = new JSONObject();
		command.put("path", inPath);
		for (Iterator iterator = inVariables.keySet().iterator(); iterator.hasNext();)
		{
			String k	= (String) iterator.next();
			Object val = inVariables.get(k);
			command.put(k, val);
		}
		
		command.put("command", "uploadtoserver");

		sendMessage(command);
		
	}

	@Override
	public void replacedWithNewDesktop(Desktop inOldDesktop)
	{
		JSONObject command = new JSONObject();
		command.put("command", "replaceddesktop");
		command.put("desktopid", inOldDesktop.getDesktopId());
		sendMessage(command);

	}

	@Override
	public void sendCommand(MediaArchive inArchive, Map inCommandData)
	{
		JSONObject command = new JSONObject(inCommandData);
		sendMessage(command);
		
	}
	public Map sendCommandAndWait(MediaArchive inArchive, JSONObject inCommand)
	{
		Map map = getTransactionQueue().sendCommandAndWait(remoteEndpointBasic, inCommand);
		return map;
	}
	
	public void downloadCategory(MediaArchive inArchive, Category inCategory, Data inUserDownload, Map inChildren)
	{
		JSONObject command = new JSONObject();
		command.put("command", "downloadcategory");
		command.put("rootname", inCategory.getName());
		command.put("folderdetails", inChildren);
		command.put("catalogid",inArchive.getCatalogId());
		command.put("mediadbid",inArchive.getMediaDbId());
		command.put("userdownloadid",inUserDownload.getId());
		
		
		//command.put("collectionid",inCollection.getId());
		sendMessage(command);
	}

	
	@Override
	public void downloadAsset(MediaArchive inArchive, Asset inAsset, Data inUserDownload)
	{
		JSONObject command = new JSONObject();
		command.put("command", "downloadasset");
		command.put("catalogid",inArchive.getCatalogId());
		command.put("mediadbid",inArchive.getMediaDbId());
		command.put("userdownloadid",inUserDownload.getId());
		
		command.put("assetid",inAsset.getId());
		command.put("filename",inAsset.getName());

		long time = inAsset.getDate("assetmodificationdate").getTime();
		if (time > 0)
		{
			command.put("assetmodificationdate", String.valueOf(time));
		}

		String urlroot = inArchive.asLinkToOriginal(inAsset);
		command.put("url",urlroot);

		sendMessage(command);
		
	}
}
