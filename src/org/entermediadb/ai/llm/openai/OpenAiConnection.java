package org.entermediadb.ai.llm.openai;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.entermediadb.ai.llm.BaseLlmConnection;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.AgentContext;
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

	public LlmResponse runPageAsInput(AgentContext agentcontext, String inTemplate)
	{
		agentcontext.addContext("mediaarchive", getMediaArchive());

		String input = loadInputFromTemplate(inTemplate, agentcontext.getContext());
		log.info(inTemplate + " process chat");
		String endpoint = getApiEndpoint();

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

	
	public LlmResponse createImage(String inModel, String inPrompt)  throws Exception
	{
		return createImage(inModel, inPrompt, 1, "1024x1024");
	}
	
	public LlmResponse createImage(String inModel, String inPrompt, int imagecount, String inSize) throws Exception
	{
		if (getApiKey() == null)
		{
			log.error("No openai-key defined");
			return null;
		}

		if (inModel == null)
		{
			throw new OpenEditException("No model given for image creation");
		}
		if (inPrompt == null)
		{
			throw new OpenEditException("No prompt given for image creation");
		}
		
		log.info("Image creation prompt: " + inPrompt);
		
		JSONObject payload = new JSONObject();

		payload.put("model", inModel);
		payload.put("prompt", inPrompt);
		payload.put("n", imagecount);
		payload.put("size", inSize);
		if(!"gpt-image-1".equals(inModel)) 
		{
			payload.put("response_format", "b64_json");
		}
		else 
		{
			payload.put("moderation", "low");
		}

		String endpoint = "https://api.openai.com/v1/images/generations";
		//  String endpoint = "http://localhost:3000/generations";  // for local testing
		HttpPost method = new HttpPost(endpoint);
		method.addHeader("Authorization", "Bearer " + getApiKey());
		method.setHeader("Content-Type", "application/json");
		method.setEntity(new StringEntity(payload.toJSONString(), "UTF-8"));

		CloseableHttpResponse resp = getConnection().sharedExecute(method);
		JSONObject json = getConnection().parseJson(resp);

		// Return a OpenAiResponse object instead of raw JSON
		OpenAiResponse response = new OpenAiResponse();
		response.setRawResponse(json);
		return response;
	}

	public OutputFiller getFiller()
	{
		return filler;
	}

	public void setFiller(OutputFiller inFiller)
	{
		filler = inFiller;
	}
	
	public LlmResponse callCreateFunction(Map context, String inModel, String inFunction) 
	{
		MediaArchive archive = getMediaArchive();

		JSONObject obj = new JSONObject();
		obj.put("model", inModel);

		String contentPath = "/" + archive.getMediaDbId() + "/ai/" + getServerName() +"/createdialog/systemmessage/" + inFunction + ".html";
		boolean contentExists = archive.getPageManager().getPage(contentPath).exists();
		if (!contentExists)
		{
			contentPath = "/" + archive.getCatalogId() + "/ai/" + getServerName() +"/createdialog/systemmessage/" + inFunction + ".html";
			contentExists = archive.getPageManager().getPage(contentPath).exists();
		}
		if (!contentExists)
		{
			throw new OpenEditException("Requested Content Does Not Exist in MEdiaDB or Catatlog:" + inFunction);
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
			String functionPath = "/" + archive.getMediaDbId() + "/ai/" + getServerName() +"/createdialog/functions/" + inFunction + ".json";
			boolean functionExists = archive.getPageManager().getPage(functionPath).exists();
			if (!functionExists)
			{
				functionPath = "/" + archive.getCatalogId() + "/ai/" + getServerName() +"/createdialog/functions/" + inFunction + ".json";
				functionExists = archive.getPageManager().getPage(functionPath).exists();
			}
			if (!functionExists)
			{
				throw new OpenEditException("Requested Function Does Not Exist in MEdiaDB or Catatlog:" + inFunction);
			}
			
			String definition = loadInputFromTemplate(functionPath, context);

			JSONParser parser = new JSONParser();
			JSONObject functionDef = (JSONObject) parser.parse(definition);

			JSONArray functions = new JSONArray();
			functions.add(functionDef);
			obj.put("functions", functions);

			JSONObject func = new JSONObject();
			func.put("name", inFunction);
			obj.put("function_call", func);
		}
		
		String payload = obj.toJSONString();
		log.info(payload);
		
		JSONObject json = handleApiRequest(payload);
	    
	    OpenAiResponse response = new OpenAiResponse();
	    response.setRawResponse(json);
	    
	    return response;

	}
	
	public LlmResponse callClassifyFunction(Map params, String inModel, String inFunction, String inBase64Image)
	{
		return callClassifyFunction(params, inModel, inFunction, inBase64Image, null);
	}

	public LlmResponse callClassifyFunction(Map params, String inModel, String inFunction, String inBase64Image, String textContent)
	{
		MediaArchive archive = getMediaArchive();

		// Use JSON Simple to create request payload
		JSONObject obj = new JSONObject();
		obj.put("model", inModel);
		//obj.put("max_tokens", maxtokens);

		// Prepare messages array

		JSONArray messages = new JSONArray();
		JSONObject message = new JSONObject();
		
		if (inBase64Image != null && !inBase64Image.isEmpty())
		{
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
		if (inFunction != null)
		{
			String templatepath = "/" + archive.getMediaDbId() + "/ai/" + getServerName() +"/classify/functions/" + inFunction + ".json";
			
			Page defpage = archive.getPageManager().getPage(templatepath);
			
			if (!defpage.exists())
			{
				templatepath = "/" + archive.getCatalogId() + "/ai/" + getServerName() +"/classify/functions/" + inFunction + ".json";
				defpage = archive.getPageManager().getPage(templatepath);
			}
			
			if (!defpage.exists())
			{
				throw new OpenEditException("Requested Function Does Not Exist in MEdiaDB or Catatlog:" + inFunction);
			}
			
			if(textContent != null)
			{
				params.put("textcontent", textContent);
			}
			
			String definition = loadInputFromTemplate(templatepath, params);

			JSONParser parser = new JSONParser();
			JSONObject functionDef = (JSONObject) parser.parse(definition);

			JSONArray functions = new JSONArray();
			functions.add(functionDef);
			obj.put("functions", functions);

			JSONObject func = new JSONObject();
			func.put("name", inFunction);
			obj.put("function_call", func);
		}

		String payload = obj.toJSONString();

		JSONObject json = handleApiRequest(payload);
	    
	    OpenAiResponse response = new OpenAiResponse();
	    response.setRawResponse(json);
	    
	    return response;

	}

	public String getApiEndpoint()
	{
		return "https://api.openai.com/v1/chat/completions";
	}

	@Override
	public String getServerName()
	{
		// TODO Auto-generated method stub
		return "openai";
	}

	@Override
	public JSONObject callStructuredOutputList(String inStructureName, String inModel, Map inParams)
	{
		inParams.put("model", inModel);
		
		String inStructure = loadInputFromTemplate("/" + getMediaArchive().getMediaDbId() + "/ai/" + getServerName() +"/classify/structures/" + inStructureName + ".json", inParams);

		JSONParser parser = new JSONParser();
		JSONObject structureDef = (JSONObject) parser.parse(inStructure);

		String endpoint = "https://api.openai.com/v1/responses";
		HttpPost method = new HttpPost(endpoint);
		method.addHeader("authorization", "Bearer " + getApiKey());
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
		
		
			JSONArray outputs = (JSONArray) json.get("output");
			if (outputs == null || outputs.isEmpty())
			{
				log.info("No output found in GPT response");
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
	
	public LlmResponse callRagFunction(String inModel, String question, String textContent)
	{
		JSONObject obj = new JSONObject();
		obj.put("model", inModel);

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

		String payload = obj.toJSONString();

		JSONObject json = handleApiRequest(payload);
	    
	    OpenAiResponse response = new OpenAiResponse();
	    response.setRawResponse(json);
	    
	    return response;

	}

}
