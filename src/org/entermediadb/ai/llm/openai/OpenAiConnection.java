package org.entermediadb.ai.llm.openai;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.entermediadb.ai.llm.BaseLlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.asset.MediaArchive;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.CatalogEnabled;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.util.OutputFiller;

public class OpenAiConnection extends BaseLlmConnection implements CatalogEnabled, LlmConnection
{
	private static Log log = LogFactory.getLog(OpenAiConnection.class);

	public LlmResponse runPageAsInput(Map params, String inModel, String inTemplate)
	{

		params.put("model", inModel);
		params.put("mediaarchive", getMediaArchive());

		String input = loadInputFromTemplate(inTemplate, params);
		log.info(inTemplate + " process chat");
		String endpoint = getApiEndpoint();

		HttpPost method = new HttpPost(endpoint);
		method.addHeader("authorization", "Bearer " + getApikey());
		method.setHeader("Content-Type", "application/json");

		method.setEntity(new StringEntity(input, "UTF-8"));

		CloseableHttpResponse resp = getConnection().sharedExecute(method);

		JSONObject json = getConnection().parseJson(resp);

		GptResponse response = new GptResponse();
		response.setRawResponse(json);

		return response;

	}

	public LlmResponse createImage(Map params)
	{
		return createImage(params, 1, "1024x1024");
	}
	
	public LlmResponse createImage(Map params, int imagecount, String inSize)
	{
		if (getApikey() == null)
		{
			log.error("No gpt-key defined");
			return null;
		}
		
		String inModel = (String) params.get("model");
		String style = (String) params.get("style");
		String inPrompt = (String) params.get("prompt");
		
		// params.put("prompt", inPrompt);

		// Use JSON Simple's JSONObject
		JSONObject obj = new JSONObject();

		if (inModel == null)
		{
			inModel = "dall-e-3";
		}
		if (inPrompt == null)
		{
			inPrompt = "Surprise ME";
		}

		log.info("Image creation prompt: " + inPrompt);

		obj.put("model", inModel);
		obj.put("prompt", inPrompt);
		obj.put("n", imagecount);
		obj.put("size", inSize);

		if (style != null)
		{
			obj.put("style", style);
		}

		String endpoint = "https://api.openai.com/v1/images/generations";
		HttpPost method = new HttpPost(endpoint);
		method.addHeader("authorization", "Bearer " + getApikey());
		method.setHeader("Content-Type", "application/json");
		method.setEntity(new StringEntity(obj.toJSONString(), "UTF-8"));

		CloseableHttpResponse resp = getConnection().sharedExecute(method);
		JSONObject json = getConnection().parseJson(resp);

		// Return a GptResponse object instead of raw JSON
		GptResponse response = new GptResponse();
		response.setRawResponse(json);
		return response;
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

		//text-embedding-ada-002 is best

		MediaArchive bench = getMediaArchive();
		// {"model": "text-davinci-003", "prompt": "Say this is a test", "temperature":
		// 0, "max_tokens": 7}
		JSONObject obj = new JSONObject();

		obj.put("model", inModel);
		obj.put("input", inQuery);

		// obj.addProperty("temperature", temp);//
		// obj.addProperty("max_tokens", maxtokens);
		String endpoint = "https://api.openai.com/v1/embeddings";
		// String endpoint = "http://localhost:4891/v1/completions";

		HttpPost method = new HttpPost(endpoint);
		method.addHeader("authorization", "Bearer " + getApikey());
		method.setHeader("Content-Type", "application/json");
		String string = obj.toString();
		method.setEntity(new StringEntity(string, "UTF-8"));

		CloseableHttpResponse resp = getConnection().sharedExecute(method);
		JSONObject json = getConnection().parseJson(resp);
		JSONArray dataArray = (JSONArray) json.get("data");
		JSONObject firstDataObject = (JSONObject) dataArray.get(0);
		JSONArray embeddingArray = (JSONArray) firstDataObject.get("embedding");

		return embeddingArray.toJSONString(); // Convert to string for returning
	}
	
	public LlmResponse callCreateFunction(Map params, String inModel, String inFunction) throws Exception
	{
		MediaArchive archive = getMediaArchive();

		JSONObject obj = new JSONObject();
		obj.put("model", inModel);

		String contentPath = "/" + archive.getMediaDbId() + "/ai/openai/createdialog/systemmessage/" + inFunction + ".html";
		boolean contentExists = archive.getPageManager().getPage(contentPath).exists();
		if (!contentExists)
		{
			contentPath = "/" + archive.getCatalogId() + "/ai/openai/createdialog/systemmessage/" + inFunction + ".html";
			contentExists = archive.getPageManager().getPage(contentPath).exists();
		}
		if (!contentExists)
		{
			throw new OpenEditException("Requested Content Does Not Exist in MEdiaDB or Catatlog:" + inFunction);
		}
		
		String content = loadInputFromTemplate(contentPath, params);
		
		JSONArray messages = new JSONArray();
		JSONObject message = new JSONObject();
		message.put("role", "user");
		message.put("content", content);
		messages.add(message);
		
		obj.put("messages", messages);

		// Handle function call definition
		if (inFunction != null)
		{
			String functionPath = "/" + archive.getMediaDbId() + "/ai/openai/createdialog/functions/" + inFunction + ".json";
			boolean functionExists = archive.getPageManager().getPage(functionPath).exists();
			if (!functionExists)
			{
				functionPath = "/" + archive.getCatalogId() + "/ai/openai/createdialog/functions/" + inFunction + ".json";
				functionExists = archive.getPageManager().getPage(functionPath).exists();
			}
			if (!functionExists)
			{
				throw new OpenEditException("Requested Function Does Not Exist in MEdiaDB or Catatlog:" + inFunction);
			}
			
			String definition = loadInputFromTemplate(functionPath, params);

			JSONParser parser = new JSONParser();
			JSONObject functionDef = (JSONObject) parser.parse(definition);

			JSONArray functions = new JSONArray();
			functions.add(functionDef);
			obj.put("functions", functions);

			JSONObject func = new JSONObject();
			func.put("name", inFunction);
			obj.put("function_call", func);
		}
		
		log.info(obj.toJSONString());

		return handleApiRequest(obj);

	}

	public LlmResponse callClassifyFunction(Map params, String inModel, String inFunction, String inQuery, String inBase64Image) throws Exception
	{
		MediaArchive archive = getMediaArchive();

		log.info("inQuery: " + inQuery);

		// Use JSON Simple to create request payload
		JSONObject obj = new JSONObject();
		obj.put("model", inModel);
		//obj.put("max_tokens", maxtokens);

		// Prepare messages array
		JSONArray messages = new JSONArray();
		JSONObject message = new JSONObject();
		message.put("role", "user");

		if (inBase64Image != null && !inBase64Image.isEmpty())
		{
			// Use an array for content if an image is provided
			JSONArray contentArray = new JSONArray();

			// Add text content
			JSONObject textContent = new JSONObject();
			textContent.put("type", "text");
			textContent.put("text", inQuery);
			contentArray.add(textContent);

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
			// Just text content
			message.put("content", inQuery);
		}

		messages.add(message);
		obj.put("messages", messages);

		// Handle function call definition
		if (inFunction != null)
		{
			String templatepath = "/" + archive.getMediaDbId() + "/ai/openai/classify/functions/" + inFunction + ".json";
			Page defpage = archive.getPageManager().getPage(templatepath);
			if (!defpage.exists())
			{
				templatepath = "/" + archive.getCatalogId() + "/ai/openai/classify/functions/" + inFunction + ".json";
				defpage = archive.getPageManager().getPage(templatepath);
			}
			if (!defpage.exists())
			{
				throw new OpenEditException("Requested Function Does Not Exist in MEdiaDB or Catatlog:" + inFunction);
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

		return handleApiRequest(obj);

	}
	
	protected LlmResponse handleApiRequest(JSONObject payload) throws Exception
	{
		// API request setup
		String endpoint = "https://api.openai.com/v1/chat/completions";
		HttpPost method = new HttpPost(endpoint);
		method.addHeader("authorization", "Bearer " + getApikey());
		method.setHeader("Content-Type", "application/json");
		method.setEntity(new StringEntity(payload.toJSONString(), StandardCharsets.UTF_8));

		CloseableHttpResponse resp = getConnection().sharedExecute(method);
		
	
			if (resp.getStatusLine().getStatusCode() != 200)
			{
				log.info("Gpt Server error status: " + resp.getStatusLine().getStatusCode());
				log.info("Gpt Server error response: " + resp.toString());
				try
				{
					String error = EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);
					log.info(error);
				}
				catch(Exception e)
				{}
				throw new OpenEditException("GPT error: " + resp.getStatusLine());
			}
		try
		{
			// Parse JSON response using JSON Simple
			JSONParser parser = new JSONParser();
			JSONObject json = (JSONObject) parser.parse(new StringReader(EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8)));

			log.info("returned: " + json.toJSONString());

			// Wrap and return `GptResponse`
			GptResponse response = new GptResponse();
			response.setRawResponse(json);
			return response;
			
		}
		catch (Exception ex)
		{
			log.error("Error calling GPT", ex);
			throw new OpenEditException(ex);
		}
		finally
		{
			connection.release(resp);
		}
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
	public JSONObject callStructuredOutputList(String inStructureName, String inModel, Map inParams) throws Exception
	{
		inParams.put("model", inModel);
		
		String inStructure = loadInputFromTemplate("/" + getMediaArchive().getMediaDbId() + "/ai/openai/classify/structures/" + inStructureName + ".json", inParams);

		JSONParser parser = new JSONParser();
		JSONObject structureDef = (JSONObject) parser.parse(inStructure);

		String endpoint = "https://api.openai.com/v1/responses";
		HttpPost method = new HttpPost(endpoint);
		method.addHeader("authorization", "Bearer " + getApikey());
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
		}
		finally
		{
			connection.release(resp);
		}
		return results;
	}

}
