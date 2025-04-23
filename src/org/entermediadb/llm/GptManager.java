package org.entermediadb.llm;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.llm.openai.GptResponse;
import org.entermediadb.net.HttpSharedConnection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.CatalogEnabled;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.page.Page;
import org.openedit.util.OutputFiller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

public class GptManager extends BaseLLMManager implements CatalogEnabled, LLMManager
{
	private static Log log = LogFactory.getLog(GptManager.class);

	protected String fieldCatalogId;
	protected MediaArchive fieldMediaArchive;
	protected HttpSharedConnection connection;

	protected HttpSharedConnection getConnection()
	{

		connection = new HttpSharedConnection();

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

	public LLMResponse runPageAsInput(WebPageRequest inReq, String inModel, String inTemplate)
	{

		String apikey = getMediaArchive().getCatalogSettingValue("gpt-key");
		assert apikey != null;
		inReq.putPageValue("model", inModel);
		inReq.putPageValue("gpt", this);
		inReq.putPageValue("mediaarchive", getMediaArchive());

		String input = loadInputFromTemplate(inReq, inTemplate);
		log.info(inTemplate + " process chat");
		String endpoint = getApiEndpoint();

		HttpPost method = new HttpPost(endpoint);
		method.addHeader("authorization", "Bearer " + apikey);
		method.setHeader("Content-Type", "application/json");

		method.setEntity(new StringEntity(input, "UTF-8"));

		CloseableHttpResponse resp = getConnection().sharedExecute(method);

		JSONObject json = getConnection().parseJson(resp);

		GptResponse response = new GptResponse();
		response.setRawResponse(json);

		return response;

	}

	public LLMResponse createImage(WebPageRequest inReq, String inModel, int imagecount, String inSize, String style, String inPrompt)
	{
		String apikey = getMediaArchive().getCatalogSettingValue("gpt-key");
		inReq.putPageValue("prompt", inPrompt);

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

		log.info("prompt was " + inPrompt);

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
		method.addHeader("authorization", "Bearer " + apikey);
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
		String apikey = getMediaArchive().getCatalogSettingValue("gpt-key");
		assert apikey != null;
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
		method.addHeader("authorization", "Bearer " + apikey);
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

	public LLMResponse callFunction(WebPageRequest inReq, String inModel, String inFunction, String inQuery, int temp, int maxtokens, String inBase64Image) throws Exception
	{
		MediaArchive archive = getMediaArchive();
		String apikey = archive.getCatalogSettingValue("gpt-key");

		assert apikey != null;

		log.info("inQuery: " + inQuery);

		// Use JSON Simple to create request payload
		JSONObject obj = new JSONObject();
		obj.put("model", inModel);
		obj.put("max_tokens", maxtokens);

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
			String definition = loadInputFromTemplate(inReq, templatepath);

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
		method.addHeader("authorization", "Bearer " + apikey);
		method.setHeader("Content-Type", "application/json");
		method.setEntity(new StringEntity(obj.toJSONString(), StandardCharsets.UTF_8));

		CloseableHttpResponse resp = getConnection().sharedExecute(method);

		// Parse JSON response using JSON Simple
		JSONParser parser = new JSONParser();
		JSONObject json = (JSONObject) parser.parse(new StringReader(EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8)));

		log.info("returned: " + json.toJSONString());

		// Wrap and return `GptResponse`
		GptResponse response = new GptResponse();
		response.setRawResponse(json);
		return response;
	}

	public String getApiEndpoint()
	{
		return "https://api.openai.com/v1/chat/completions";
	}

	@Override
	public String getType()
	{
		// TODO Auto-generated method stub
		return "openai";
	}

}
