package org.entermediadb.ai.llm.ollama;

import java.io.StringReader;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.BasicLlmResponse;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.ai.llm.openai.OpenAiConnection;
import org.entermediadb.asset.MediaArchive;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.util.JSONParser;



public class OllamaConnection extends OpenAiConnection implements CatalogEnabled, LlmConnection
{
	
	private static Log log = LogFactory.getLog(OllamaConnection.class);
	
	@Override
	public String getLlmProtocol()
	{
		return "ollama";
	}
	
	/*
	
	public BasicLlmResponse runPageAsInput(AgentContext llmrequest, String inTemplate)
	{
		String input = loadInputFromTemplate(inTemplate, llmrequest);
		log.info(input);
		String endpoint = getServerRoot() + "/api/chat";
		

		HttpPost method = new HttpPost(endpoint);
		method.addHeader("Authorization", "Bearer " + getApiKey());
		method.setHeader("Content-Type", "application/json");

		method.setEntity(new StringEntity(input, "UTF-8"));
		
//		HttpSharedConnection connection = ;

		CloseableHttpResponse resp = getConnection().sharedExecute(method);

		JSONObject json = getConnection().parseMap(resp); // pretty dumb but I want to standardize on GSON

		OllamaResponse response = new OllamaResponse();
		response.setRawResponse(json);
		return response;

	}
	
	@Override
	public LlmResponse callClassifyFunction(Map params, String inFunction, String inBase64Image)
	{
		return callClassifyFunction(params, inFunction, inBase64Image, null);
	}

	@Override
	public LlmResponse callClassifyFunction(Map params, String inFunction, String inBase64Image, String textContent)
	{
	    MediaArchive archive = getMediaArchive();

	    // Use JSON Simple to create request payload
	    JSONObject obj = new JSONObject();
	    obj.put("model", getModelName());
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
	    
	    LlmResponse response = callJson("/api/chat",null,obj);
	    return response;
	}

	@Override
	public LlmResponse callCreateFunction(Map inParams, String inFunction) 
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public LlmResponse callStructure(Map inParams, String inFuction) 
	{
		inParams.put("model", getModelName());
		
		String inStructure = loadInputFromTemplate("/" + getMediaArchive().getMediaDbId() + "/ai/ollama/classify/structures/" + inFuction + ".json", inParams);

		JSONObject req = new JSONParser().parse(inStructure);
		
		LlmResponse res = callJson("/api/chat",req); //Tools?

		log.info("Returned: " + res);
		return res;
	}
	*/
	
}
