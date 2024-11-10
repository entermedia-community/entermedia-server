package org.entermediadb.llm;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.net.HttpSharedConnection;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.ModuleManager;
import org.openedit.WebPageRequest;
import org.openedit.users.User;
import org.openedit.util.OutputFiller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

	public String runTemplate(WebPageRequest inReq, String inModel, String inTemplate) throws Exception
	{
		User user = inReq.getUser();

		String gptinput = loadInputFromTemplate(inReq, inTemplate);

		JsonObject response = runQuery(inModel, gptinput, 0, 2000);
		// inReq.putPageValue("responsetext", response.toString());
		JsonArray choices = response.getAsJsonArray("choices");
		JsonObject first = (JsonObject) choices.get(0);
		String output = first.get("message").getAsJsonObject().get("content").getAsString();

//		String output = first.get("text").getAsString();
		output = output.replace("\n", "<br>");
		String param = inReq.findActionValue("param");
		inReq.putPageValue(param, output);
		return output;
	}
	
	
	
	public JsonObject runPageAsInput(WebPageRequest inReq, String inModel, String inTemplate) {
		JsonParser parser = new JsonParser();

		String apikey = getMediaArchive().getCatalogSettingValue("gpt-key");
		assert apikey != null;
		String input = loadInputFromTemplate(inReq, inTemplate);
		log.info(input);
		String endpoint = "https://api.openai.com/v1/chat/completions";

		HttpPost method = new HttpPost(endpoint);
		method.addHeader("authorization", "Bearer " + apikey);
		method.setHeader("Content-Type", "application/json");
		
		method.setEntity(new StringEntity(input, "UTF-8"));

		CloseableHttpResponse resp = getConnection().sharedExecute(method);

		JSONObject json = getConnection().parseJson(resp); // pretty dumb but I want to standardize on GSON

		return (JsonObject) parser.parse(json.toJSONString());
		
		
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

	public String retrieveResponse(String inModel, String inQuery, int temp, int maxtokens)
	{
		JsonObject response = runQuery(inModel, inQuery, temp, maxtokens);
		JsonArray choices = response.getAsJsonArray("choices");
		JsonObject first = (JsonObject) choices.get(0);
		String output = first.get("message").getAsJsonObject().get("content").getAsString();
		return output;
	}
	
	
	

	public JsonObject runQuery(String inModel, String inQuery, int temp, int maxtokens)
	{

		MediaArchive bench = getMediaArchive();
		JsonParser parser = new JsonParser();
		String apikey = getMediaArchive().getCatalogSettingValue("gpt-key");
		assert apikey != null;
		// {"model": "text-davinci-003", "prompt": "Say this is a test", "temperature":
		// 0, "max_tokens": 7}
		JsonObject obj = new JsonObject();

		obj.addProperty("model", inModel);
		// obj.addProperty("prompt", inQuery);
		// [{"role": "user", "content": 'Translate the following English text to French:
		// "{text}"'}]
		JsonArray messages = new JsonArray();
		JsonObject message = new JsonObject();
		messages.add(message);
		message.addProperty("role", "user");
		message.addProperty("content", inQuery);
		obj.add("messages", messages);

		// obj.addProperty("temperature", temp);//
		// obj.addProperty("max_tokens", maxtokens);
		String endpoint = "https://api.openai.com/v1/chat/completions";
		// String endpoint = "http://localhost:4891/v1/completions";

		HttpPost method = new HttpPost(endpoint);
		method.addHeader("authorization", "Bearer " + apikey);
		method.setHeader("Content-Type", "application/json");
		String string = obj.toString();
		method.setEntity(new StringEntity(string, "UTF-8"));

		CloseableHttpResponse resp = getConnection().sharedExecute(method);

		JSONObject json = getConnection().parseJson(resp); // pretty dumb but I want to standardize on GSON

		return (JsonObject) parser.parse(json.toJSONString());
	}

	public String getEmbedding(String inModel, String inQuery) throws Exception
	{

		//text-embedding-ada-002 is best

		MediaArchive bench = getMediaArchive();
		JsonParser parser = new JsonParser();
		String apikey = getMediaArchive().getCatalogSettingValue("gpt-key");
		assert apikey != null;
		// {"model": "text-davinci-003", "prompt": "Say this is a test", "temperature":
		// 0, "max_tokens": 7}
		JsonObject obj = new JsonObject();

		obj.addProperty("model", inModel);
		obj.addProperty("input", inQuery);

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
		JsonObject object = (JsonObject) parser.parse(new InputStreamReader(resp.getEntity().getContent(), "UTF-8"));
		String data = object.get("data").getAsJsonArray().get(0).getAsJsonObject().get("embedding").toString();
		return data;
	}

	
	public JsonObject callFunction(WebPageRequest inReq, String inModel, String inFunction, String inQuery, int temp, int maxtokens) throws Exception {
		return callFunction(inReq, inModel, inFunction, inQuery, temp, maxtokens, null);
	}
	
	public JsonObject callFunction(WebPageRequest inReq, String inModel, String inFunction, String inQuery, int temp, int maxtokens, String inBase64Image) throws Exception
	{
		MediaArchive bench = getMediaArchive();
		JsonParser parser = new JsonParser();
		String apikey = getMediaArchive().getCatalogSettingValue("gpt-key");
		assert apikey != null;
		// {"model": "text-davinci-003", "prompt": "Say this is a test", "temperature":
		// 0, "max_tokens": 7}
	
		log.info("inQuery: "+ inQuery);

		JsonObject obj = new JsonObject();

		obj.addProperty("model", inModel);
		// obj.addProperty("prompt", inQuery);
		// [{"role": "user", "content": 'Translate the following English text to French:
		// "{text}"'}]
		JsonArray messages = new JsonArray();
		JsonObject message = new JsonObject();
		messages.add(message);
		message.addProperty("role", "user");
		
		
		
		if (inBase64Image != null && !inBase64Image.isEmpty()) {
	        // Use an array for content if an image is provided
	        JsonArray contentArray = new JsonArray();

	        // Add text content to the content array
	        JsonObject textContent = new JsonObject();
	        textContent.addProperty("type", "text");
	        textContent.addProperty("text", inQuery);
	        contentArray.add(textContent);

	        // Add image content to the content array
	        JsonObject imageContent = new JsonObject();
	        imageContent.addProperty("type", "image_url");
	        JsonObject imageUrl = new JsonObject();
	        imageUrl.addProperty("url", "data:image/png;base64," + inBase64Image); // Base64 as a data URL
	        imageContent.add("image_url", imageUrl);
	        contentArray.add(imageContent);

	        message.add("content", contentArray); // Array format for mixed content
	    } else {
	        // Use a string for content if no image is provided
	        message.addProperty("content", inQuery);
	    }
		
		
		obj.add("messages", messages);
		if (inFunction != null)
		{

			String definition = loadInputFromTemplate(inReq, bench.getCatalogId() + "/gpt/functiondefs/" + inFunction + ".json");
			JsonArray functions = new JsonArray();
			JsonObject function = (JsonObject) parser.parse(definition);
			functions.add(function);
			obj.add("functions", functions);
			JsonObject func = new JsonObject();
			func.addProperty("name", inFunction);
			obj.add("function_call", func);
		}

		// obj.addProperty("temperature", temp);//
		obj.addProperty("max_tokens", maxtokens);
		String endpoint = "https://api.openai.com/v1/chat/completions";
		// String endpoint = "http://localhost:4891/v1/completions";

		HttpPost method = new HttpPost(endpoint);
		method.addHeader("authorization", "Bearer " + apikey);
		method.setHeader("Content-Type", "application/json");
		String string = obj.toString();
		method.setEntity(new StringEntity(string, StandardCharsets.UTF_8));

		CloseableHttpResponse resp = getConnection().sharedExecute(method);

		String returned = EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);
		log.info("returned: "+ returned);
		
		JsonReader reader = new JsonReader(new StringReader(returned));
		reader.setLenient(true);
		
		
		JsonObject answer = (JsonObject) JsonParser.parseReader(reader);
		JsonArray choices = answer.getAsJsonArray("choices");
		JsonObject first = (JsonObject) choices.get(0);
		JsonObject function_call = first.get("message").getAsJsonObject().get("function_call").getAsJsonObject();
		if (function_call != null)
		{
			String params = function_call.get("arguments").getAsString();

			JsonObject more = null;
			try
			{
				   JsonReader paramReader = new JsonReader(new StringReader(params));
		            paramReader.setLenient(true);
		            more = (JsonObject) JsonParser.parseReader(paramReader);
			}
			catch (JsonSyntaxException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return more;
		}
		return null;

	}

	

	

}
