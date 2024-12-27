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
import org.json.simple.parser.JSONParser;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.users.User;
import org.openedit.util.OutputFiller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

public class OllamaManager extends BaseLLMManager implements CatalogEnabled, LLMManager
{
	private static Log log = LogFactory.getLog(OllamaManager.class);

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

	public JSONObject runPageAsInput(WebPageRequest inReq, String inModel, String inTemplate)
	{
		String apikey = getMediaArchive().getCatalogSettingValue("ollama-key");
		assert apikey != null;
		String input = loadInputFromTemplate(inReq, inTemplate);
		log.info(input);
		String endpoint = getApiEndpoint();
		;

		HttpPost method = new HttpPost(endpoint);
		method.addHeader("authorization", "Bearer " + apikey);
		method.setHeader("Content-Type", "application/json");

		method.setEntity(new StringEntity(input, "UTF-8"));

		CloseableHttpResponse resp = getConnection().sharedExecute(method);

		JSONObject json = getConnection().parseJson(resp); // pretty dumb but I want to standardize on GSON

		return json;

	}

	private String getApiEndpoint()
	{
		// TODO Auto-generated method stub
		String endpoint = getMediaArchive().getCatalogSettingValue("ollama-url");
		if (endpoint == null)
		{
			endpoint = "http://localhost:11434";
		}
		return endpoint;
	}

	public JSONObject createImage(WebPageRequest inReq, String inModel, int imagecount, String inSize, String style, String inPrompt)
	{
		throw new OpenEditException("Model doesn't support images");

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

		return "Not Implemented";
	}

	public JSONObject callFunction(WebPageRequest inReq, String inModel, String inFunction, String inQuery, int temp, int maxtokens, String inBase64Image) throws Exception
	{
		MediaArchive archive = getMediaArchive();
		JsonParser parser = new JsonParser();

		log.info("inQuery: " + inQuery);

		JsonObject obj = new JsonObject();

		obj.addProperty("model", inModel); // Setting the model (e.g., llama3.1)
		obj.addProperty("stream", false);
		// Prepare the messages array
		JsonArray messages = new JsonArray();
		JsonObject message = new JsonObject();
		messages.add(message);
		message.addProperty("role", "user");

		 if (inBase64Image != null && !inBase64Image.isEmpty()) {
        
		        message.addProperty("content", inQuery); // Mixed content array
		        JsonArray images = new JsonArray();
		        images.add(inBase64Image);
		        message.add("images",images);
		        
		    } else {
		        // Add only text content if no image is provided
		        message.addProperty("content", inQuery);
		    }

		obj.add("messages", messages);

		// Load the function definition and format from the template
		String definition = loadInputFromTemplate(inReq, archive.getCatalogId() + "/gpt/functiondefs/" + inFunction + ".json");
		JsonObject functionDef = parser.parse(definition).getAsJsonObject();
		
		JsonObject parameters = functionDef.getAsJsonObject("parameters");
		obj.add("format", parameters); // Adding format definition

		
		// API endpoint setup
		String endpoint = getApiEndpoint() + "/api/chat";
		HttpPost method = new HttpPost(endpoint);
		method.setHeader("Content-Type", "application/json");
		String apikey = archive.getCatalogSettingValue("ollama-key");
		
		method.addHeader("authorization", "Bearer " + apikey);

		String string = obj.toString();
		method.setEntity(new StringEntity(string, StandardCharsets.UTF_8));

		CloseableHttpResponse resp = getConnection().sharedExecute(method);

		String returned = EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);
		log.info("returned: " + returned);

		JsonReader reader = new JsonReader(new StringReader(returned));
		reader.setLenient(true);

		JsonObject answer = (JsonObject) JsonParser.parseReader(reader);
		
		String function_call = answer.get("message").getAsJsonObject().get("content").getAsString();
		

		if (function_call != null)
		{
			JsonObject more = null;

			JsonReader paramReader = new JsonReader(new StringReader(function_call));
			paramReader.setLenient(true);
            more = (JsonObject) JsonParser.parseReader(paramReader);
			JSONObject result = (JSONObject) new JSONParser().parse(more.toString());

			return result;
		}
		return null;

	}

}
