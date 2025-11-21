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
	
	protected Data fieldModelData;
	
	public Data getModelData() {
		return fieldModelData;
	}
	
	public void setModelData(Data inModelData) {
		fieldModelData = inModelData;
	}
	
	public String getServerRoot() 
	{
		
		//TODO: lookuo from new servers table
		// - create llama-vision and llama implementation, they may be in 2 different servers
		// - put the extension part (v1/....) inside each place we call getServerRoot.
		// - cleanup catalogsettings server ids.
		
		
		String llmtype = getLlmType();
		
		if(llmtype.equals("openai"))
		{
			return "https://api.openai.com/v1/chat/completions";
		}
		else if(llmtype.equals("ollama"))
		{
			return "https://ollama.entermediadb.net";
		}
		else if(llmtype.equals("llama"))
		{
			String server = getMediaArchive().getCatalogSettingValue("ai_asset_classification_server");
			return server + "/v1/chat/completions";
		}
		else if(llmtype.equals("llamaopenai"))
		{
			return "https://llamam50.entermediadb.net/v1/chat/completions";
		}
		else if(llmtype.equals("gemini"))
		{
			return "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
		}
			
		if( endpoint == null)
		{
			throw new OpenEditException("No endpoint defined for model: " + getModelData().getId());
		}
		return endpoint;
	}
	
	public String getModelIdentifier()
	{
		return getModelData().get("modelid");
	}
	
	public String getLlmType()
	{
		return getModelData().get("llmtype");
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
			apikey = getMediaArchive().getCatalogSettingValue(getLlmType()+"-key");
		}
		if (apikey == null)
		{
			apikey = getMediaArchive().getCatalogSettingValue("openai-key");
		}
		if (apikey == null)
		{
			log.error("No key defined in catalog settings");
			//throw new OpenEditException("No openai-key defined in catalog settings");
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
		AgentContext agentcontext = new AgentContext();
		agentcontext.setContext(inContext);
		
		return loadInputFromTemplate(inTemplate, agentcontext);
	}
	public String loadInputFromTemplate(String inTemplate, AgentContext agentcontext) 
	{
		Map<String,Object> inContext = agentcontext.getContext();
		if(inTemplate == null) {
			throw new OpenEditException("Cannot load input, template is null" + inContext);
		}
		try {
			Page template = getPageManager().getPage(inTemplate);
			log.info("Loading input: " + inTemplate);
			
			User user = getMediaArchive().getUserManager().getUser("agent");
			
			WebPageRequest request = getRequestUtils().createPageRequest(template, user);
			
			if(agentcontext != null)
			{				
				loadagentcontextParameters(agentcontext, request);
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

	protected void loadagentcontextParameters(AgentContext agentcontext, WebPageRequest request)
	{
		Map inParameters = agentcontext.getProperties();
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
	
	public LlmResponse renderLocalAction(AgentContext agentcontext) 
	{
		String functionName = agentcontext.getFunctionName();
		if(functionName == null) 
		{
			throw new OpenEditException("Cannot load function response, functionName is null");
		}
		
		log.info("Loading response for function: " + functionName);
		
		String apphome = (String) agentcontext.getContextValue("apphome");
		
		String templatepath = apphome + "/views/modules/modulesearch/results/agentresponses/" + functionName + ".html";
		
		try 
		{
			Page template = getPageManager().getPage(templatepath);
			log.info("Loading response: " + functionName);
			
			User user = getMediaArchive().getUserManager().getUser("agent");
			
			WebPageRequest inReq = getRequestUtils().createPageRequest(template, user);
			inReq.putPageValues(agentcontext.getContext());
			inReq.putPageValue("agentcontext", agentcontext);
			loadagentcontextParameters(agentcontext, inReq);
			
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
	
	protected JSONObject handleApiRequest(String payload)
	{
		String endpoint = getServerRoot();
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
				throw new OpenEditException("handleApiRequest error: " + resp.getStatusLine());
			}

			JSONObject json = (JSONObject) connection.parseJson(resp);

			log.info("returned: " + json.toJSONString());
			 
			return json;
		}
		catch (Exception ex)
		{
			log.error("Error calling handleApiRequest", ex);
			throw new OpenEditException(ex);
		}
		finally
		{
			connection.release(resp);
		}
	}

	public LlmResponse callPlainMessage(AgentContext agentcontext, String inPageName)
	{
		agentcontext.addContext("mediaarchive", getMediaArchive());
		String input = loadInputFromTemplate("/" + getMediaArchive().getMediaDbId() + "/ai/" + getLlmType() +"/assistant/messages/" + inPageName + ".json", agentcontext.getContext());
		log.info(inPageName + " process chat");
		String endpoint = getServerRoot();

		HttpPost method = new HttpPost(endpoint);
		method.addHeader("Authorization", "Bearer " + getApiKey());
		method.setHeader("Content-Type", "application/json");

		method.setEntity(new StringEntity(input, "UTF-8"));

		CloseableHttpResponse resp = getConnection().sharedExecute(method);

		JSONObject json = getConnection().parseJson(resp);

		OpenAiResponse response = new OpenAiResponse();
		response.setRawResponse(json);
		
		String nextFunction = response.getFunctionName();
		if( nextFunction != null)
		{
			agentcontext.setFunctionName(nextFunction);
		}

		getMediaArchive().saveData("agentcontext",agentcontext);
		return response;

	}
	
	@Override
	public LlmResponse createImage(String inPrompt)  throws Exception
	{
		throw new OpenEditException("Model doesn't support images");
	}
	
	@Override
	public LlmResponse createImage(String inPrompt, int imagecount, String inSize)
	{
		throw new OpenEditException("Model doesn't support images");
	}
}