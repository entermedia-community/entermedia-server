package org.entermediadb.ai.llm.llama;


import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.ai.llm.openai.OpenAiResponse;
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

		params.put("model", getModelName());
		
		if(textContent != null)
		{
			params.put("textcontent", textContent);
		}

		String templatepath = "/" + archive.getMediaDbId() + "/ai/"+getLlmProtocol()+"/calls/" + inFunction + ".json";
			
		Page template = archive.getPageManager().getPage(templatepath);
			
		if (!template.exists())
		{
			templatepath = "/" + archive.getCatalogId() + "/ai/"+getLlmProtocol()+"/calls/" + inFunction + ".json";
			template = archive.getPageManager().getPage(templatepath);
		}
		
		if (!template.exists())
		{
			throw new OpenEditException("Requested Function Does Not Exist in MediaDB or Catalog:" + inFunction);
		}
			
		String definition = loadInputFromTemplate(templatepath, params);

		JSONParser parser = new JSONParser();
		JSONObject payload = (JSONObject) parser.parse(definition);
		
		log.info(payload);
		
		attachImageMessage(payload, inBase64Image);
		
		LlmResponse res = callJson("/chat/completions", payload);
	    return res;
	}
	
	@Override
	public LlmResponse callStructure(Map inParams, String inFunctionName)
	{
		inParams.put("model", getModelName());
		
		String templatepath = "/" + getMediaArchive().getMediaDbId() + "/ai/" + getLlmProtocol() +"/calls/" + inFunctionName + ".json";
		String prompt = loadInputFromTemplate(templatepath, inParams);

		JSONParser parser = new JSONParser();
		JSONObject structureDef = (JSONObject) parser.parse(prompt);

		log.info("Sending: " + structureDef.toJSONString());
		
		LlmResponse res = callJson("/chat/completions", structureDef);

        
		return res;
	}
	
	@Override
	public LlmResponse callOCRFunction(Map inParams, String inBase64Image, String inFunctioName)
	{
		MediaArchive archive = getMediaArchive();
		String templatepath = "/" + archive.getMediaDbId() + "/ai/" + getLlmProtocol() +"/calls/" + inFunctioName + ".json";
		Page defpage = archive.getPageManager().getPage(templatepath);
		if (!defpage.exists())
		{
			templatepath = "/" + archive.getCatalogId() + "/ai/" + getLlmProtocol() +"/calls/" + inFunctioName + ".json";
			defpage = archive.getPageManager().getPage(templatepath);
		}
		
		if (!defpage.exists())
		{
			throw new OpenEditException("Requested Function Does Not Exist in MediaDB or Catalog:" + inFunctioName);
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
		return new OpenAiResponse();
	}
}
