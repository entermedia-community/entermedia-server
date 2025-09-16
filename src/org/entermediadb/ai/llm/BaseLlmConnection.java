package org.entermediadb.ai.llm;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.entermediadb.ai.llm.openai.GptResponse;
import org.entermediadb.asset.MediaArchive;
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
import org.openedit.page.PageStreamer;
import org.openedit.page.manage.PageManager;
import org.openedit.servlet.OpenEditEngine;
import org.openedit.users.User;
import org.openedit.util.OutputFiller;
import org.openedit.util.RequestUtils;

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
			
			User user = getMediaArchive().getUserManager().getUser("agent");
			
			WebPageRequest request = getRequestUtils().createPageRequest(template, user);
			
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
	
	protected LlmResponse handleApiRequest(String payload)
	{
		String endpoint = getApiEndpoint() + "/api/chat";
		HttpPost method = new HttpPost(endpoint);
		method.addHeader("Authorization", "Bearer " + getApikey());
		method.setHeader("Content-Type", "application/json");
		method.setEntity(new StringEntity(payload, StandardCharsets.UTF_8));

		HttpSharedConnection connection = getConnection();
		CloseableHttpResponse resp = connection.sharedExecute(method);
		
		try
		{
			if (resp.getStatusLine().getStatusCode() != 200)
			{
				log.info("AI Server error status: " + resp.getStatusLine().getStatusCode());
				log.info("AI Server error response: " + resp.toString());
				try
				{
					String error = EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);
					log.info(error);
				}
				catch(Exception e)
				{}
				throw new OpenEditException("GPT error: " + resp.getStatusLine());
			}

			JSONObject json = (JSONObject) connection.parseJson(resp);

			log.info("returned: " + json.toJSONString());

			GptResponse response = new GptResponse();
			response.setRawResponse(json);
			return response;
		}
		catch (Exception ex)
		{
			log.error("Error calling GPT", ex);
			throw new OpenEditException(ex);
		}
		finally
		{
			connection.release(resp);
		}
	}
	
}