package org.entermediadb.ai.llm;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.entermediadb.ai.llm.openai.OpenAiResponse;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.net.HttpSharedConnection;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
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
	
	
	public String getApiKey()
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
		if (getApiKey() == null || getApiKey().length() == 0) {
			log.error("No apikey defined in catalog settings");
			return false;
		}
		return true;
	}

	public String loadInputFromTemplate(String inTemplate) {
		return loadInputFromTemplate(inTemplate, new HashMap());
	}

	public String loadInputFromTemplate(String inTemplate, Map<String, Object> inContext) 
	{
		LlmRequest llmrequest = new LlmRequest();
		llmrequest.setContext(inContext);
		
		return loadInputFromTemplate(inTemplate, llmrequest);
	}
	public String loadInputFromTemplate(String inTemplate, LlmRequest llmrequest) 
	{
		Map<String,Object> inContext = llmrequest.getContext();
		if(inTemplate == null) {
			throw new OpenEditException("Cannot load input, template is null" + inContext);
		}
		try {
			Page template = getPageManager().getPage(inTemplate);
			log.info("Loading input: " + inTemplate);
			
			User user = getMediaArchive().getUserManager().getUser("agent");
			
			WebPageRequest request = getRequestUtils().createPageRequest(template, user);
			
			if(llmrequest != null)
			{				
				loadLlmRequestParameters(llmrequest, request);
			}
			
			request.putPageValues(inContext);
			
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

	protected void loadLlmRequestParameters(LlmRequest llmrequest, WebPageRequest request)
	{
		JSONObject inParameters = llmrequest.getParameters();
		if( inParameters != null)
		{
			for (Iterator iterator = inParameters.keySet().iterator(); iterator.hasNext();)
			{
				String key = (String) iterator.next();
				Object obj = inParameters.get(key);
				if( obj instanceof String)
				{
					request.setRequestParameter(key, (String)obj);
				}
				else if( obj instanceof JSONObject)
				{
					JSONObject json = (JSONObject)obj;
					request.setRequestParameter(key, json.toJSONString());
				}
				else if( obj instanceof Collection)
				{
					Collection<String> col = (Collection<String>)obj;
					obj = (String[])col.toArray(new String[col.size()]);
					request.setRequestParameter(key, (String[])obj);
				}
				else if( obj instanceof String[])
				{
					request.setRequestParameter(key, (String[])obj);
				}
			}
		}
	}
	
	public LlmResponse loadResponseFromTemplate(LlmRequest llmrequest) 
	{
		String functionName = llmrequest.getFunctionName();
		if(functionName == null) 
		{
			throw new OpenEditException("Cannot load function response, functionName is null");
		}
		
		log.info("Loading response for function: " + functionName);
		
		String apphome = (String) llmrequest.getContextValue("apphome");
		
		String templatepath = apphome + "/views/modules/modulesearch/results/agentresponses/" + functionName + ".html";
		
		try 
		{
			Page template = getPageManager().getPage(templatepath);
			log.info("Loading response: " + functionName);
			
			User user = getMediaArchive().getUserManager().getUser("agent");
			
			WebPageRequest inReq = getRequestUtils().createPageRequest(template, user);
			inReq.putPageValues(llmrequest.getContext());
			inReq.putPageValue("llmrequest", llmrequest);
			loadLlmRequestParameters(llmrequest, inReq);
			
			StringWriter output = new StringWriter();
			inReq.setWriter(output);
			
			PageStreamer streamer = getEngine().createPageStreamer(template, inReq);
			getEngine().executePathActions(inReq);
			if( !inReq.hasRedirected())
			{
				getModuleManager().executePageActions( template,inReq );
			}
			if( inReq.hasRedirected())
			{
				log.info("action was redirected");
			}
			
			streamer.include(template, inReq);
			
			String string = output.toString();
			log.info("Output: " + string);
			
			BasicLlmResponse response = new BasicLlmResponse();
			
			loadMessageResponse(string, response);
			
			return response;
		} 
		catch (OpenEditException e) 
		{
			throw e;
		} 
	}
	
	public void loadMessageResponse(String inMessage, BasicLlmResponse response)
	{
		String dataMessage = "";
		String mainMessage = inMessage;

		int dataStart = mainMessage.indexOf("<messageplain>");
		while(dataStart >= 0)
		{
			int dataEnd = mainMessage.indexOf("</messageplain>");
			if( dataEnd <= dataStart)
			{
				break;
			}
			String dm = mainMessage.substring(dataStart + 14, dataEnd).trim();
			if(!dm.isEmpty())
			{
				dataMessage += dm + " \n ";
			}
			mainMessage = mainMessage.substring(0, dataStart).trim() + mainMessage.substring(dataEnd + 15).trim();
			dataStart = mainMessage.indexOf("<messageplain>");
		}

		response.setMessage(mainMessage);
		
		if(dataMessage.length() > 0)
		{
			response.setMessagePlain(dataMessage.trim());
		}
	}

	public int copyData(JSONObject source, Data data)
	{
		int i = 0; 
		Map metadata =  (Map) source.get("metadata");
		for (Iterator iterator = metadata.keySet().iterator(); iterator.hasNext();) 
		{
			String key = (String) iterator.next();
			Object value = metadata.get(key);

			data.setValue(key, value);
			i ++;
		}
		return i;
	}
	
	protected LlmResponse handleApiRequest(String payload)
	{
		String endpoint = getApiEndpoint();
		HttpPost method = new HttpPost(endpoint);
		method.addHeader("Authorization", "Bearer " + getApiKey());
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

			OpenAiResponse response = new OpenAiResponse();
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