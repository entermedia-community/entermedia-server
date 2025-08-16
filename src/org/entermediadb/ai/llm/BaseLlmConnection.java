package org.entermediadb.ai.llm;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.llm.openai.OpenAiConnection;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.events.PathEventManager;
import org.entermediadb.net.HttpSharedConnection;
import org.entermediadb.websocket.chat.ChatServer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.page.Page;
import org.openedit.page.PageRequestKeys;
import org.openedit.page.PageStreamer;
import org.openedit.page.manage.PageManager;
import org.openedit.servlet.OpenEditEngine;
import org.openedit.users.User;
import org.openedit.util.OutputFiller;
import org.openedit.util.RequestUtils;
import org.openedit.util.URLUtilities;

public abstract class BaseLlmConnection implements LlmConnection {
	private static Log log = LogFactory.getLog(LlmConnection.class);

	protected ModuleManager fieldModuleManager;
	protected PageManager fieldPageManager;
	protected RequestUtils fieldRequestUtils;
	protected OutputFiller filler = new OutputFiller();
	protected OpenEditEngine fieldEngine;
	protected String apikey;
	protected String fieldEndpoint;
	
	public String getApiEndpoint() {
	    return fieldEndpoint;
	}

	protected String fieldCatalogId;
	protected MediaArchive fieldMediaArchive;
	protected HttpSharedConnection connection;

	protected HttpSharedConnection getConnection()
	{
		connection = new HttpSharedConnection();
		return connection;
	}

	public LlmResponse callFunction(Map params, String inModel, String inFunction, String inQuery) throws Exception 
		{
			return callFunction(params, inModel, inFunction, inQuery, null);
		}
		
	public MediaArchive getMediaArchive()
	{
		if (fieldMediaArchive == null)
		{
			fieldMediaArchive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		}
		return fieldMediaArchive;
	}

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}
	
	
	public String getApikey()
	{
		if (apikey == null)
		{
			apikey = getMediaArchive().getCatalogSettingValue(getServerName()+"-key");
		}
		if (apikey == null)
		{
			apikey = getMediaArchive().getCatalogSettingValue("gpt-key");
		}
		if (apikey == null)
		{
			log.error("No key defined in catalog settings");
			//throw new OpenEditException("No gpt-key defined in catalog settings");
		}
		
		setApikey(apikey);
		return apikey;
	}
	
	public void setApikey(String inApikey)
	{
		apikey = inApikey;
	}

	public ModuleManager getModuleManager() {
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager) {
		fieldModuleManager = inModuleManager;
	}

	public BaseLlmConnection() {
		super();
	}

	public PageManager getPageManager() {
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager) {
		fieldPageManager = inPageManager;
	}
	
	public RequestUtils getRequestUtils() {
		return fieldRequestUtils;
	}
	
	public void setRequestUtils(RequestUtils inRequestUtils) {
		fieldRequestUtils = inRequestUtils;
	}

	public OpenEditEngine getEngine() {
		if (fieldEngine == null) {
			fieldEngine = (OpenEditEngine) getModuleManager().getBean("OpenEditEngine");

		}

		return fieldEngine;
	}
	
	public Boolean isReady() {
		if (getApikey() == null || getApikey().length() == 0) {
			log.error("No apikey defined in catalog settings");
			return false;
		}
		return true;
	}

	public String loadInputFromTemplate(String inTemplate) {
		return loadInputFromTemplate(inTemplate, new HashMap());
	}

	public String loadInputFromTemplate(String inTemplate, Map inMap) {
		if(inTemplate == null) {
			throw new OpenEditException("Cannot load input, template is null" + inMap);
		}
		try {
//			User user = params.getUser();
			Page template = getPageManager().getPage(inTemplate);
			log.info("Loading input: " + inTemplate);
			
			WebPageRequest request = getRequestUtils().createPageRequest(template, null);
			
			request.putPageValues(inMap);
			
			StringWriter output = new StringWriter();
			request.setWriter(output);
			PageStreamer streamer = getEngine().createPageStreamer(template, request);
				getEngine().executePathActions(request);
				if( !request.hasRedirected())
				{
					getModuleManager().executePageActions( template,request );
				}
				if( request.hasRedirected())
				{
					log.info("action was redirected");
				}
			
			streamer.include(template, request);
			String string = output.toString();
			log.info(inTemplate +" Output: " + string);
			return string;
		} catch (OpenEditException e) {
			throw e;
		} 
	}
	
	
	public LlmResponse callFunction(Map params, String inModel, String inFunction, String inQuery, int temp, int maxtokens) throws Exception {
		return callFunction(params, inModel, inFunction, inQuery, temp, maxtokens);
	}
	
	public void callChatFunction(Data messageToUpdate, String functionName, Map params) throws Exception
	{

		MediaArchive archive = getMediaArchive();

		//get the channel
		Data channel = archive.getCachedData("channel", messageToUpdate.get("channel"));
		params.put("channel", channel);
		
		
		ChatServer server = (ChatServer) archive.getBean("chatServer");

		//String function = messageToUpdate.get("function");
			//params.putPageValue("args", args);
		String response;
		
		try
		{
			String filename = functionName;
			if( functionName.equals("showHintOrHelpInfo") )
			{
				filename = "show-hints";
			}
			
			params.put("data", messageToUpdate);

			String args = (String) messageToUpdate.get("arguments");
			JSONObject arguments = (JSONObject) new JSONParser().parse(args);
			params.put("arguments", arguments);
			
			response = loadInputFromTemplate("/" + archive.getMediaDbId() + "/gpt/functions/" + filename + ".html", params);
			//log.info("Function " + functionName + " returned : " + response);

			messageToUpdate.setValue("functionresponse", response);
			messageToUpdate.setValue("message", response);
			
			Searcher chats = archive.getSearcher("chatterbox");
			chats.saveData(messageToUpdate);
			
			JSONObject functionMessageUpdate = new JSONObject();
			functionMessageUpdate.put("messagetype", "airesponse");
			functionMessageUpdate.put("catalogid", archive.getCatalogId());
			functionMessageUpdate.put("user", "agent");
			functionMessageUpdate.put("channel", messageToUpdate.get("channel"));
			functionMessageUpdate.put("messageid", messageToUpdate.getId());
			functionMessageUpdate.put("message", response);

			server.broadcastMessage(functionMessageUpdate);
			
		}
		catch (Exception e)
		{
			log.error("Error: " + e.toString());
			messageToUpdate.setValue("functionresponse", e.toString());
			messageToUpdate.setValue("processingcomplete", true);

		}

	}
	
	
	public int copyData(JSONObject source, Data data)
	{
		int i = 0; 
		Map metadata =  (Map) source.get("metadata");
		for (Iterator iterator = metadata.keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			Object value = metadata.get(key);

			data.setValue(key, value);
			i ++;
		}
		return i;
	}
	
	
}