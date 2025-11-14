package org.entermediadb.ai.llm.ollama;

import java.io.StringReader;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.entermediadb.ai.llm.BaseLlmConnection;
import org.entermediadb.ai.llm.BasicLlmResponse;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.net.HttpSharedConnection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.util.JSONParser;
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
	
	public String getApiKey()
	{
		if (apikey == null)
		{
			apikey = getMediaArchive().getCatalogSettingValue("ollama-key");
			setApikey(apikey);
		}
		if (apikey == null)
		{
			log.error("No ollama-key defined in catalog settings");
		}
		
		return apikey;
	}

	public BasicLlmResponse runPageAsInput(AgentContext llmrequest, String inTemplate)
	{
		String input = loadInputFromTemplate(inTemplate, llmrequest);
		log.info(input);
		String endpoint = getApiEndpoint() + "/api/chat";
		

		HttpPost method = new HttpPost(endpoint);
		method.addHeader("Authorization", "Bearer " + getApiKey());
		method.setHeader("Content-Type", "application/json");

		method.setEntity(new StringEntity(input, "UTF-8"));
		
//		HttpSharedConnection connection = ;

		CloseableHttpResponse resp = getConnection().sharedExecute(method);

		JSONObject json = connection.parseJson(resp); // pretty dumb but I want to standardize on GSON

		OllamaResponse response = new OllamaResponse();
		response.setRawResponse(json);
		return response;

	}

	public String getApiEndpoint()
	{
		// TODO Auto-generated method stub
		String apihost = getMediaArchive().getCatalogSettingValue("ai_ollama_server");
		if (apihost == null)
		{
			apihost = "http://localhost:11434";
		}
		String endpoint = apihost + "/api/chat";
		return endpoint;
	}

	public void setFiller(OutputFiller inFiller)
	{
		filler = inFiller;
	}
	
	public LlmResponse callClassifyFunction(Map params, String inFunction, String inBase64Image)
	{
		return callClassifyFunction(params, inFunction, inBase64Image, null);
	}

	public LlmResponse callClassifyFunction(Map params, String inFunction, String inBase64Image, String textContent)
	{
	    MediaArchive archive = getMediaArchive();

	    // Use JSON Simple to create request payload
	    JSONObject obj = new JSONObject();
	    obj.put("model", getModelIdentifier());
	    obj.put("stream", false);


	    JSONArray messages = new JSONArray();
		JSONObject message = new JSONObject();
		
		if (inBase64Image != null && !inBase64Image.isEmpty())
		{
			message.put("role", "user");
			JSONArray images = new JSONArray();
			images.add(inBase64Image);
			message.put("images", images);
		}
		else
		{
			message.put("role", "system");
			String systemMessage = loadInputFromTemplate("/" +  getMediaArchive().getMediaDbId() + "/ai/default/systemmessage/"+inFunction+".html");
			message.put("content", systemMessage);
		}
		
		messages.add(message);
		obj.put("messages", messages);


	    // Handle function call definition
	    if (inFunction != null) {
			String templatepath = "/" + archive.getMediaDbId() + "/ai/ollama/classify/functions/" + inFunction + ".json";
			
			Page defpage = archive.getPageManager().getPage(templatepath);
			
			if(!defpage.exists()) {
				templatepath  ="/" + archive.getCatalogId() + "/ai/ollama/classify/functions/" + inFunction + ".json";
				defpage = archive.getPageManager().getPage(templatepath);
			}
			
			if(!defpage.exists()) {
				throw new OpenEditException("Requested Content Does Not Exist in MediaDB or Catalog:" + inFunction);
		    }
			
			if(textContent == null)
			{
				params.put("textcontent", textContent);
			}
			
	        String definition = loadInputFromTemplate(templatepath, params);
	        
	        JSONParser parser = new JSONParser();
	        JSONObject functionDef = (JSONObject) parser.parse(definition);

	        JSONObject parameters = (JSONObject) functionDef.get("parameters");
	        obj.put("format", parameters);
	    }
	    String payload = obj.toJSONString();
	    
	    JSONObject json = handleApiRequest(payload);
	    
	    OllamaResponse response = new OllamaResponse();
	    response.setRawResponse(json);
	    
	    return response;
	}

	@Override
	public LlmResponse callCreateFunction(Map inParams, String inFunction) 
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public JSONObject callStructuredOutputList(String inStructureName, Map inParams) 
	{
		inParams.put("model", getModelIdentifier());
		
		String inStructure = loadInputFromTemplate("/" + getMediaArchive().getMediaDbId() + "/ai/ollama/classify/structures/" + inStructureName + ".json", inParams);
		
		JSONObject json = handleApiRequest(inStructure);

		log.info("Returned: " + json);
			
		JSONObject results = new JSONObject();

		JSONObject message = (JSONObject) json.get("message");
		if (message == null || !message.get("role").equals("assistant"))
		{
			log.info("No message found in GPT response");
			return results;
		}

		String content = (String) message.get("content");
			
		if (content == null || content.isEmpty())
		{
			log.info("No structured data found in GPT response");
			return results;
		}
		JSONParser parser = new JSONParser();
		results = (JSONObject) parser.parse(new StringReader(content));

		return results;
	}
	
	@Override
	public JSONObject callOCRFunction(Map inParams, String inOCRInstruction, String inBase64Image)
	{
		throw new OpenEditException("Not implemented yet");
	}
}
