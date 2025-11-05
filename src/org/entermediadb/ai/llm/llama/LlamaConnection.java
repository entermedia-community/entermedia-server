package org.entermediadb.ai.llm.llama;

import java.util.Map;

import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.ai.llm.openai.OpenAiConnection;
import org.entermediadb.ai.llm.openai.OpenAiResponse;
import org.entermediadb.asset.MediaArchive;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.util.JSONParser;

public class LlamaConnection extends OpenAiConnection {
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
	
}
