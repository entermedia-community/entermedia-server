package org.entermediadb.ai.llm.openai;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.BaseLlmConnection;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.MediaArchive;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.util.JSONParser;
import org.openedit.util.OutputFiller;

public class OpenAiConnection extends BaseLlmConnection implements CatalogEnabled, LlmConnection
{
	private static Log log = LogFactory.getLog(OpenAiConnection.class);

	@Override
	public String getLlmProtocol()
	{
		return "openai";
	}	
	
	public LlmResponse runPageAsInput(AgentContext agentcontext, String inTemplate)
	{
		agentcontext.addContext("mediaarchive", getMediaArchive());

		String input = loadInputFromTemplate(inTemplate, agentcontext.getContext());
		log.info(inTemplate + " process chat");
		String endpoint = getServerRoot();

		HttpPost method = new HttpPost(endpoint);
		method.addHeader("Authorization", "Bearer " + getApiKey());
		method.setHeader("Content-Type", "application/json");

		method.setEntity(new StringEntity(input, "UTF-8"));

		CloseableHttpResponse resp = getConnection().sharedExecute(method);

		JSONObject json = getConnection().parseMap(resp);

		LlmResponse response = (LlmResponse) createResponse();
		response.setRawResponse(json);
		
		String nextFunction = response.getFunctionName();
		if( nextFunction != null)
		{
			agentcontext.setFunctionName(nextFunction);
		}

		getMediaArchive().saveData("agentcontext",agentcontext);
		return response;

	}

	
	public LlmResponse createImage(String inPrompt) 
	{
		return createImage(inPrompt, 1, "1024x1024");
	}
	
	public LlmResponse createImage(String inPrompt, int imagecount, String inSize)
	{
		if (getApiKey() == null)
		{
			throw new OpenEditException("No API key configured for OpenAI image creation");
		}

		if (inPrompt == null)
		{
			throw new OpenEditException("No prompt given for image creation");
		}
		
		JSONObject payload = new JSONObject();
		payload.put("model", getModelName());
		payload.put("prompt", inPrompt);
		payload.put("n", imagecount);
		payload.put("size", inSize);
		if(!"gpt-image-1".equals(getModelName())) 
		{
			payload.put("response_format", "b64_json");
		}
		else 
		{
			payload.put("moderation", "low");
		}

		//String endpoint = "https://api.openai.com/v1/images/generations";
		//String endpoint = "http://localhost:3000/generations";  // for local testing
		
		log.info("Creating image with prompt: " + inPrompt + "  Model: " + getModelName());
		LlmResponse res = callJson("/images/generations",payload);
		return res;
		
	}

	public OutputFiller getFiller()
	{
		return filler;
	}

	public void setFiller(OutputFiller inFiller)
	{
		filler = inFiller;
	}
	
	public LlmResponse callCreateFunction(Map context, String inFunction) 
	{
		MediaArchive archive = getMediaArchive();
		

		JSONObject obj = new JSONObject();
		obj.put("model", getModelName());

		String contentPath = "/" + archive.getMediaDbId() + "/ai/" + getLlmProtocol() +"/createdialog/systemmessage/" + inFunction + ".html";
		boolean contentExists = archive.getPageManager().getPage(contentPath).exists();
		if (!contentExists)
		{
			contentPath = "/" + archive.getCatalogId() + "/ai/" + getLlmProtocol() +"/createdialog/systemmessage/" + inFunction + ".html";
			contentExists = archive.getPageManager().getPage(contentPath).exists();
		}
		if (!contentExists)
		{
			throw new OpenEditException("Requested Content Does Not Exist in MediaDB or Catalog:" + inFunction);
		}
		
		String content = loadInputFromTemplate(contentPath, context);
		
		JSONArray messages = new JSONArray();
		JSONObject message = new JSONObject();
		message.put("role", "user");
		message.put("content", content);
		messages.add(message);
		
		obj.put("messages", messages);

		// Handle function call definition
		if (inFunction != null)
		{
			String functionPath = "/" + archive.getMediaDbId() + "/ai/" + getLlmProtocol() +"/createdialog/functions/" + inFunction + ".json";
			boolean functionExists = archive.getPageManager().getPage(functionPath).exists();
			if (!functionExists)
			{
				functionPath = "/" + archive.getCatalogId() + "/ai/" + getLlmProtocol() +"/createdialog/functions/" + inFunction + ".json";
				functionExists = archive.getPageManager().getPage(functionPath).exists();
			}
			if (!functionExists)
			{
				throw new OpenEditException("Requested Function Does Not Exist in MediaDB or Catalog:" + inFunction);
			}
			
			String definition = loadInputFromTemplate(functionPath, context);

			JSONParser parser = new JSONParser();
			JSONObject functionDef = (JSONObject) parser.parse(definition);

			JSONArray functions = new JSONArray();
			functions.add(functionDef);
			
			
			JSONArray tools = new JSONArray();
			JSONObject toolfunction = new JSONObject();
			
			toolfunction.put("function", functionDef);
			toolfunction.put("type", "function");
			tools.add(toolfunction);
			
			obj.put("tools", tools);

			JSONObject toolchoice = new JSONObject();
			toolchoice.put("type", "function");
			JSONObject functionname = new JSONObject();
			functionname.put("name", inFunction);
			toolchoice.put("function", functionname);
			obj.put("tool_choice", toolchoice);
			
			
			
		}
		
		log.info("Call Function: " + obj.toJSONString());
		
		LlmResponse res = callJson("/chat/completions",obj);
	    return res;

	}
	
	public LlmResponse callClassifyFunction(Map params, String inFunction, String inBase64Image)
	{
		return callClassifyFunction(params, inFunction, inBase64Image, null);
	}

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
	
	public LlmResponse callToolsFunction(Map params, String inFunction)
	{
		MediaArchive archive = getMediaArchive();
		
		params.put("model", getModelName());

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

		LlmResponse res = callJson("/chat/completions", payload);
	    return res;
	}
	
	public JSONObject attachImageMessage(JSONObject payload, String inBase64Image) {
		if (inBase64Image != null && !inBase64Image.isEmpty())
		{
			JSONArray messages = (JSONArray) payload.get("messages");
			
			JSONObject message = new JSONObject();
			message.put("role", "user");

			JSONArray contentArray = new JSONArray();

			JSONObject imageContent = new JSONObject();
			imageContent.put("type", "image_url");
			JSONObject imageUrl = new JSONObject();
			imageUrl.put("url", inBase64Image);
			imageContent.put("image_url", imageUrl);
			
			contentArray.add(imageContent);

			message.put("content", contentArray);

			messages.add(message);
		}
		
		return payload;
	}

	@Override
	public LlmResponse callStructuredOutputList(Map inParams)
	{
		inParams.put("model", getModelName());
		
		String jsonfilename = getAiFunctionName();
		
		if(inParams.containsKey("jsonfilename"))
		{
			jsonfilename = (String) inParams.get("jsonfilename");
		}
		
		String templatepath = "/" + getMediaArchive().getMediaDbId() + "/ai/"+getLlmProtocol()+"/calls/" + jsonfilename + ".json";
		
		String inStructure = loadInputFromTemplate(templatepath, inParams);

		JSONParser parser = new JSONParser();
		JSONObject structureDef = (JSONObject) parser.parse(inStructure);
		
		log.info( "Sent: " + structureDef.toJSONString());
		
		HttpPost method = new HttpPost(getServerRoot() + "/chat/completions");
		method.addHeader("authorization", "Bearer " + getApiKey());
		method.setHeader("Content-Type", "application/json");
		method.setEntity(new StringEntity(structureDef.toJSONString(), StandardCharsets.UTF_8));

		CloseableHttpResponse resp = getConnection().sharedExecute(method);
		
//		JSONObject results = new JSONObject();

		try
		{
			if (resp.getStatusLine().getStatusCode() != 200)
			{
				throw new OpenEditException("OpenAI error: " + resp.getStatusLine());
			}
	
			JSONObject json = (JSONObject)getConnection().parseMap(resp);

			log.info("Returned: " + json.toJSONString());
		
			LlmResponse response = createResponse();
			response.setRawResponse(json);
			return response;
			
			/*
			JSONArray outputs = (JSONArray) json.get("output");
			if (outputs == null || outputs.isEmpty())
			{
				log.info("No output found in OpenAI response");
				return results;
			}
			
			JSONObject output = null;
			for (Object outputObj : outputs)
			{
				if (!(outputObj instanceof JSONObject))
				{
					log.info("Output is not a JSONObject: " + outputObj);
					continue;
				}
				JSONObject obj = (JSONObject) outputObj;
				String role = (String) obj.get("role");
				if(role != null && role.equals("assistant"))
				{
					output = obj;
					break;
				}
			}
			if (output == null || !output.get("status").equals("completed"))
			{
				log.info("No completed output found in GPT response");
				return results;
			}
			JSONArray contents = (JSONArray) output.get("content");
			if (contents == null || contents.isEmpty())
			{
				log.info("No content found in GPT response");
				return results;
			}
			JSONObject content = (JSONObject) contents.get(0);
			
			if (content == null || !content.containsKey("text"))
			{
				log.info("No structured data found in GPT response");
				return results;
			}
			String text = (String) content.get("text");
			if (text == null || text.isEmpty())
			{
				log.info("No text found in structured data");
				return results;
			}
			results = (JSONObject) parser.parse(new StringReader(text));

			if(results.containsKey("type") && results.get("type").equals("object") && results.containsKey("properties"))
			{
				results = (JSONObject) results.get("properties"); // gpt-4o-mini sometimes wraps in properties
			}
			*/
		}
		finally
		{
			getConnection().release(resp);
		}
	}
	
	@Override
	public LlmResponse callSmartCreatorAiAction(Map inParams, String inActionName)
	{
		inParams.put("model", getModelName());
		
		String templatepath = "/" + getMediaArchive().getMediaDbId() + "/ai/"+getLlmProtocol()+"/calls/smartcreator/" + inActionName + ".json";
		
		String inStructure = loadInputFromTemplate(templatepath, inParams);

		JSONParser parser = new JSONParser();
		JSONObject structureDef = (JSONObject) parser.parse(inStructure);
		
		log.info( "Sent: " + structureDef.toJSONString());
		
		HttpPost method = new HttpPost(getServerRoot() + "/chat/completions");
		method.addHeader("authorization", "Bearer " + getApiKey());
		method.setHeader("Content-Type", "application/json");
		method.setEntity(new StringEntity(structureDef.toJSONString(), StandardCharsets.UTF_8));

		CloseableHttpResponse resp = getConnection().sharedExecute(method);

		try
		{
			if (resp.getStatusLine().getStatusCode() != 200)
			{
				throw new OpenEditException("OpenAI error: " + resp.getStatusLine());
			}
	
			JSONObject json = (JSONObject)getConnection().parseMap(resp);

			log.info("Returned: " + json.toJSONString());
		
			LlmResponse response = createResponse();
			response.setRawResponse(json);
			return response;
		}
		finally
		{
			getConnection().release(resp);
		}
	}
	
	public LlmResponse callRagFunction(String question, String textContent)
	{
		JSONObject obj = new JSONObject();
		obj.put("model", getModelName());

		JSONArray messages = new JSONArray();
		
		JSONObject message = new JSONObject();
		message.put("role", "system");
		message.put("content", "You are a helpful assistant that answers questions based only on the provided context.");
		messages.add(message);
		
		
		JSONObject usermessage = new JSONObject();
		usermessage.put("role", "user");
		usermessage.put("content", "Context: " + textContent + "\n\nQuestion: " + question);
		messages.add(usermessage);

		obj.put("messages", messages);

		LlmResponse res = callJson("/chat/completions",obj);
		
		JSONArray choices = (JSONArray) res.getRawResponse().get("choices");
        JSONObject choice = (JSONObject) choices.get(0);
        JSONObject resmessage = (JSONObject) choice.get("message");
        
        String ocrResponse = (String) resmessage.get("content");
        res.setMessage(ocrResponse);
        
	    return res;

	}
	
	@Override
	public LlmResponse callOCRFunction(Map inParams, String inBase64Image)
	{
		throw new OpenEditException("Not implemented yet. Only available in Llama connection.");
	}

	public LlmResponse createResponse()
	{
		return new OpenAiResponse();
	}
}
