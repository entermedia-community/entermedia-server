package org.entermediadb.ai.llm.llama;


import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.ai.llm.openai.OpenAiConnection;
import org.entermediadb.asset.MediaArchive;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.util.JSONParser;

public class LlamaConnection extends OpenAiConnection {
	private static Log log = LogFactory.getLog(LlamaConnection.class);
	
	@Override
	public String getModelIdentifier()
	{
		String modelname = getModelData().getId();
		return "/root/.cache/llama.cpp/unsloth_"+modelname+".gguf";
	}
	
	
	public LlmResponse callClassifyFunction(Map params, String inFunction, String inBase64Image, String textContent)
	{
		MediaArchive archive = getMediaArchive();

		// Use JSON Simple to create request payload
		JSONObject obj = new JSONObject();
		obj.put("model", getModelIdentifier());
		//obj.put("max_tokens", maxtokens);

		// Prepare messages array

		JSONArray messages = new JSONArray();
		
		if("llama".equals(getLlmType()))
		{
			JSONObject systemmessage = new JSONObject();
			systemmessage.put("role", "system");
			
			JSONArray contentarray = new JSONArray();
			
			JSONObject contentitem = new JSONObject();
			contentitem.put("type", "text");
			contentitem.put("text", "You are a metadata generator. You are given an instruction in Open AI tool format, parse it and give a response in JSON with all the required fields.");
			
			contentarray.add(contentitem);
			
			systemmessage.put("content", contentarray);
			
			messages.add(systemmessage);
		}

		// Handle function call definition
		if (inFunction != null)
		{
			String templatepath = "/" + archive.getMediaDbId() + "/ai/" + getLlmType() +"/classify/functions/" + inFunction + ".json";
			
			Page defpage = archive.getPageManager().getPage(templatepath);
			
			if (!defpage.exists())
			{
				templatepath = "/" + archive.getCatalogId() + "/ai/" + getLlmType() +"/classify/functions/" + inFunction + ".json";
				defpage = archive.getPageManager().getPage(templatepath);
			}
			
			if (!defpage.exists())
			{
				throw new OpenEditException("Requested Function Does Not Exist in MediaDB or Catalog:" + inFunction);
			}
			
			if(textContent != null)
			{
				params.put("textcontent", textContent);
			}
			
			String definition = loadInputFromTemplate(templatepath, params);

			JSONParser parser = new JSONParser();
			JSONObject functionDef = (JSONObject) parser.parse(definition);
			
			JSONObject message = new JSONObject();
			message.put("role", "user");
			// Use an array for content if an image is provided
			JSONArray contentArray = new JSONArray();

			// Add image content
			JSONObject functionContent = new JSONObject();
			functionContent.put("type", "text");
			functionContent.put("text", functionDef.toJSONString());
			
			contentArray.add(functionContent);
			
			message.put("content", contentArray);
			
			messages.add(message);

		}
		
		if (inBase64Image != null && !inBase64Image.isEmpty())
		{
			JSONObject message = new JSONObject();
			message.put("role", "user");
			// Use an array for content if an image is provided
			JSONArray contentArray = new JSONArray();

			// Add image content
			JSONObject imageContent = new JSONObject();
			imageContent.put("type", "image_url");
			JSONObject imageUrl = new JSONObject();
			imageUrl.put("url", "data:image/png;base64," + inBase64Image); // Base64 as a data URL
			imageContent.put("image_url", imageUrl);
			contentArray.add(imageContent);

			message.put("content", contentArray);

			messages.add(message);
		}
		

		obj.put("messages", messages);

		String payload = obj.toJSONString();

		JSONObject json = handleApiRequest(payload);
	    
		LlamaResponse response = new LlamaResponse();
	    response.setRawResponse(json);
	    
	    return response;

	}
	
	@Override
	public JSONObject callStructuredOutputList(String inStructureName, Map inParams)
	{
		MediaArchive archive = getMediaArchive();
		
		inParams.put("model", getModelIdentifier());
		
		
		String structurepath = "/" + getMediaArchive().getMediaDbId() + "/ai/" + getLlmType() +"/classify/structures/" + inStructureName + "_structure.json";
		Page defpage = archive.getPageManager().getPage(structurepath);
		
		if (defpage.exists())
		{
			String structure = loadInputFromTemplate(structurepath, inParams);
			inParams.put("structure", structure);
		}
		
		String prompt = loadInputFromTemplate("/" + getMediaArchive().getMediaDbId() + "/ai/" + getLlmType() +"/classify/structures/" + inStructureName + ".json", inParams);

		JSONParser parser = new JSONParser();
		JSONObject structureDef = (JSONObject) parser.parse(prompt);

		String endpoint = getApiEndpoint();
		HttpPost method = new HttpPost(endpoint);
		method.addHeader("Authorization", "Bearer " + getApiKey());
		method.setHeader("Content-Type", "application/json");
		method.setEntity(new StringEntity(structureDef.toJSONString(), StandardCharsets.UTF_8));

		CloseableHttpResponse resp = getConnection().sharedExecute(method);
		
		JSONObject results = new JSONObject();

		try
		{
			if (resp.getStatusLine().getStatusCode() != 200)
			{
				throw new OpenEditException("GPT error: " + resp.getStatusLine());
			}
	
			JSONObject json = (JSONObject) parser.parse(new StringReader(EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8)));

			log.info("Returned: " + json.toJSONString());
		
		
			JSONArray choices = (JSONArray) json.get("choices");
	        JSONObject choice = (JSONObject) choices.get(0);
	        JSONObject message = (JSONObject) choice.get("message");
	        
	        String contentString = (String) message.get("content");
	        
	        results = parser.parse(contentString);
	        
	        
		}
		catch (Exception e) 
		{
			throw new OpenEditException(e);
		}
		finally
		{
			connection.release(resp);
		}
		return results;
	}
	
}
