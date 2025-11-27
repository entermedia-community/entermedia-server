package org.entermediadb.ai.llm.llama;


import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.MediaArchive;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.util.JSONParser;

public class LlamaVisionConnection extends LlamaOpenAiConnection {
	private static Log log = LogFactory.getLog(LlamaVisionConnection.class);
	
	@Override
	public String getLlmProtocol()
	{
		return "llamavision";
	}
	
	public String getModelPath()
	{
		String modelname = getModelName();
		return "/root/.cache/llama.cpp/"+modelname;
	}
	
	@Override
	public LlmResponse callClassifyFunction(Map params, String inFunction, String inBase64Image, String textContent)
	{
		MediaArchive archive = getMediaArchive();

		JSONObject obj = new JSONObject();
		obj.put("model", getModelName());

		JSONArray messages = new JSONArray();
	
		JSONObject systemmessage = new JSONObject();
		systemmessage.put("role", "system");
		
		JSONArray contentarray = new JSONArray();
		
		JSONObject contentitem = new JSONObject();
		contentitem.put("type", "text");
		contentitem.put("text", "You are a metadata generator. You are given an instruction in Open AI tool format, parse it and give a response in JSON with all the required fields.");
		
		contentarray.add(contentitem);
		
		systemmessage.put("content", contentarray);
		
		messages.add(systemmessage);

		// Handle function call definition
		if (inFunction != null)
		{
			String templatepath = "/" + archive.getMediaDbId() + "/ai/" + getLlmProtocol() +"/calls/" + inFunction + ".json";
			
			Page defpage = archive.getPageManager().getPage(templatepath);
			
			if (!defpage.exists())
			{
				templatepath = "/" + archive.getCatalogId() + "/ai/" + getLlmProtocol() +"/calls/" + inFunction + ".json";
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
			imageUrl.put("url", inBase64Image); // Base64 as a data URL
			imageContent.put("image_url", imageUrl);
			contentArray.add(imageContent);

			message.put("content", contentArray);

			messages.add(message);
		}
		

		obj.put("messages", messages);

		//log.info("Sending to server: " + obj.toJSONString());
		
		LlmResponse res = callJson("/api/chat",obj); //Todo: Confirm is ok
	    return res;
	}
	
	@Override
	public LlmResponse callStructuredOutputList(Map inParams)
	{
		inParams.put("model", getModelName());
		
		String templatepath = "/" + getMediaArchive().getMediaDbId() + "/ai/" + getLlmProtocol() +"/calls/" + getAiFunctionName() + ".json";
		String prompt = loadInputFromTemplate(templatepath, inParams);

		JSONParser parser = new JSONParser();
		JSONObject structureDef = (JSONObject) parser.parse(prompt);

		log.info("Sending: " + structureDef.toJSONString());
		
		LlmResponse res = callJson("/chat/completions", structureDef);

        
		return res;
	}
	
	@Override
	public LlmResponse callOCRFunction(Map inParams, String inBase64Image)
	{
		MediaArchive archive = getMediaArchive();
		String templatepath = "/" + archive.getMediaDbId() + "/ai/" + getLlmProtocol() +"/calls/" + getAiFunctionName() + ".json";
		Page defpage = archive.getPageManager().getPage(templatepath);
		if (!defpage.exists())
		{
			templatepath = "/" + archive.getCatalogId() + "/ai/" + getLlmProtocol() +"/calls/" + getAiFunctionName() + ".json";
			defpage = archive.getPageManager().getPage(templatepath);
		}
		
		if (!defpage.exists())
		{
			throw new OpenEditException("Requested Function Does Not Exist in MediaDB or Catalog:" + getAiFunctionName());
		}

		String template = loadInputFromTemplate(templatepath, inParams);

		JSONParser parser = new JSONParser();
		JSONObject templateObject = (JSONObject) parser.parse(template);

		JSONArray messages = (JSONArray) templateObject.get("messages");

		JSONObject usermessage = (JSONObject) messages.get(messages.size() - 1);
		JSONArray contentarray = (JSONArray) usermessage.get("content");

		JSONObject imagecontentitem = new JSONObject();
		imagecontentitem.put("type", "image_url");

		JSONObject imageurl = new JSONObject();
		imageurl.put("url", inBase64Image); // Base64 as a data URL
		
		imagecontentitem.put("image_url", imageurl);
		contentarray.add(imagecontentitem);
		
		LlmResponse res = callJson("/chat/completions",templateObject);
		
		//TODO Parse out the OCR to message
		
	    return res;

	}

	@Override
	public LlmResponse createResponse()
	{
		return new LlamaVisionResponse();
	}
}
