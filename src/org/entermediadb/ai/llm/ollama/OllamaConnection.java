package org.entermediadb.ai.llm.ollama;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.entermediadb.ai.llm.BaseLlmConnection;
import org.entermediadb.ai.llm.BaseLlmResponse;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.net.HttpSharedConnection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.CatalogEnabled;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.util.OutputFiller;



public class OllamaConnection extends BaseLlmConnection implements CatalogEnabled, LlmConnection
{
	private static Log log = LogFactory.getLog(OllamaConnection.class);

	protected String fieldCatalogId;
	protected MediaArchive fieldMediaArchive;
	protected HttpSharedConnection connection;

	protected HttpSharedConnection getConnection()
	{
		
		if (connection == null)
		{
			connection = new HttpSharedConnection();
		}

		return connection;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
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
			apikey = getMediaArchive().getCatalogSettingValue("ollama-key");
			setApikey(apikey);
		}
		if (apikey == null)
		{
			log.error("No ollama-key defined in catalog settings");
			//throw new OpenEditException("No gpt-key defined in catalog settings");
		}
		
		return apikey;
	}

	public BaseLlmResponse runPageAsInput(Map params, String inModel, String inTemplate)
	{

		String input = loadInputFromTemplate(inTemplate, params);
		log.info(input);
		String endpoint = getApiEndpoint() + "/api/chat";
		

		HttpPost method = new HttpPost(endpoint);
		method.addHeader("authorization", "Bearer " + getApikey());
		method.setHeader("Content-Type", "application/json");

		method.setEntity(new StringEntity(input, "UTF-8"));
		
		HttpSharedConnection connection = getConnection();

		CloseableHttpResponse resp = connection.sharedExecute(method);

		JSONObject json = connection.parseJson(resp); // pretty dumb but I want to standardize on GSON

		OllamaResponse response = new OllamaResponse();
		response.setRawResponse(json);
		return response;

	}

	public String getApiEndpoint()
	{
		// TODO Auto-generated method stub
		String endpoint = getMediaArchive().getCatalogSettingValue("ollama-url");
		if (endpoint == null)
		{
			endpoint = "http://localhost:11434";
		}
		return endpoint;
	}

	public BaseLlmResponse createImage(Map params)
	{
		throw new OpenEditException("Model doesn't support images");
	}
	public BaseLlmResponse createImage(Map inParams, int inCount, String inSize)
	{
		throw new OpenEditException("Model doesn't support images");
	}

	public String getEmbedding(String inQuery) throws Exception
	{
		return getEmbedding("text-embedding-ada-002", inQuery);
	}

	public OutputFiller getFiller()
	{
		return filler;
	}

	public void setFiller(OutputFiller inFiller)
	{
		filler = inFiller;
	}

	public String getEmbedding(String inModel, String inQuery) throws Exception
	{

		return "Not Implemented";
	}

	public LlmResponse callClassifyFunction(Map params, String inModel, String inFunction, String inQuery, String inBase64Image) throws Exception {
	    MediaArchive archive = getMediaArchive();

	    log.info("Llama function: " + inFunction + " Query: " + inQuery);

	    // Use JSON Simple to create request payload
	    JSONObject obj = new JSONObject();
	    obj.put("model", inModel);
	    obj.put("stream", false);

	    // Prepare messages array
	    JSONArray messages = new JSONArray();
	    JSONObject message = new JSONObject();
	    message.put("role", "user");

        message.put("content", inQuery);
	    if (inBase64Image != null && !inBase64Image.isEmpty()) 
	    {
	        // Add image content separately
	        JSONArray images = new JSONArray();
	        images.add(inBase64Image);
	        message.put("images", images);
	    }

	    messages.add(message);
	    obj.put("messages", messages);

	    // Handle function call definition
	    if (inFunction != null) {
	    	
	        String templatepath = "/" + archive.getMediaDbId() + "/gpt/functiondefs/" + inFunction + ".json";
	        Page defpage = archive.getPageManager().getPage(templatepath);
	        if(!defpage.exists()) {
		        templatepath  ="/" + archive.getCatalogId() + "/gpt/functiondefs/" + inFunction + ".json";
		        defpage = archive.getPageManager().getPage(templatepath);
	        }
	        if(!defpage.exists()) {
			       throw new OpenEditException("Requested Function Does Not Exist in MediaDB or Catalog:" + inFunction);
		    }
	        String definition = loadInputFromTemplate(templatepath, params);
	        
	        JSONParser parser = new JSONParser();
	        JSONObject functionDef = (JSONObject) parser.parse(definition);

	        JSONObject parameters = (JSONObject) functionDef.get("parameters");
	        obj.put("format", parameters);
	    }

	    // API request setup
	    String endpoint = getApiEndpoint() + "/api/chat";
	    HttpPost method = new HttpPost(endpoint);
	    method.addHeader("authorization", "Bearer " + getApikey());
	    method.setHeader("Content-Type", "application/json");
	    method.setEntity(new StringEntity(obj.toJSONString(), StandardCharsets.UTF_8));

	    HttpSharedConnection connection = getConnection();
	    
	    CloseableHttpResponse resp = connection.sharedExecute(method);

	    // Parse JSON response using JSON Simple
	    //JSONParser parser = new JSONParser();
	    //JSONObject json = (JSONObject) parser.parse(new StringReader(EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8)));
	    
	    JSONObject json = connection.parseJson(resp);

	    log.info("Llama returned: " + json.toJSONString());

	    // Wrap and return `OllamaResponse`
	    OllamaResponse response = new OllamaResponse();
	    response.setRawResponse(json);
	    return response;
	}

	@Override
	public String getServerName()
	{
		return "ollama";
	}

	@Override
	public JSONObject callStructuredOutputList(String inStructureName, String inModel, Collection inFields, Map inParams) throws Exception
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LlmResponse callCreateFunction(Map inParams, String inModel, String inFunction) throws Exception 
	{
		// TODO Auto-generated method stub
		return null;
	}
}
