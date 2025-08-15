package org.entermediadb.ai.llm.openai;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.entermediadb.ai.llm.BaseLlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.net.HttpSharedConnection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.CatalogEnabled;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.modules.translations.LanguageMap;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.util.OutputFiller;

public class OpenAiConnection extends BaseLlmConnection implements CatalogEnabled, LlmConnection
{
	private static Log log = LogFactory.getLog(OpenAiConnection.class);

	public LlmResponse runPageAsInput(Map params, String inModel, String inTemplate)
	{

		params.put("model", inModel);
		params.put("gpt", this);
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

	public LlmResponse createImage(Map params, String inModel, int imagecount, String inSize, String style, String inPrompt)
	{
		if (getApikey() == null)
		{
			log.error("No gpt-key defined");
			return null;
		}
		params.put("prompt", inPrompt);

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

	public LlmResponse callFunction(Map params, String inModel, String inFunction, String inQuery, int temp, int maxtokens, String inBase64Image) throws Exception
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

			String templatepath = "/" + archive.getMediaDbId() + "/gpt/functiondefs/" + inFunction + ".json";
			Page defpage = archive.getPageManager().getPage(templatepath);
			if (!defpage.exists())
			{
				templatepath = "/" + archive.getCatalogId() + "/gpt/functiondefs/" + inFunction + ".json";
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

		// API request setup
		String endpoint = "https://api.openai.com/v1/chat/completions";
		HttpPost method = new HttpPost(endpoint);
		method.addHeader("authorization", "Bearer " + getApikey());
		method.setHeader("Content-Type", "application/json");
		method.setEntity(new StringEntity(obj.toJSONString(), StandardCharsets.UTF_8));

		CloseableHttpResponse resp = getConnection().sharedExecute(method);
		
		try
		{
			if (resp.getStatusLine().getStatusCode() != 200)
			{
				log.info("Gpt Server error returned " + resp.getStatusLine().getStatusCode());
				log.info("Gpt Server error returned " + resp.getStatusLine());
				throw new OpenEditException("GPT error: " + resp.getStatusLine());
			}
	
			// Parse JSON response using JSON Simple
			JSONParser parser = new JSONParser();
			JSONObject json = (JSONObject) parser.parse(new StringReader(EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8)));

			log.info("returned: " + json.toJSONString());

			// Wrap and return `GptResponse`
			GptResponse response = new GptResponse();
			response.setRawResponse(json);
			return response;
			
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

}
