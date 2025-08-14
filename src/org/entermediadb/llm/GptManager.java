package org.entermediadb.llm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.entermediadb.asset.Asset;
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
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.modules.translations.LanguageMap;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.util.OutputFiller;

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
	
	public String getApikey()
	{
		if (apikey == null)
		{
			apikey = getMediaArchive().getCatalogSettingValue("gpt-key");
			setApikey(apikey);
		}
		if (apikey == null)
		{
			log.error("No gpt-key defined in catalog settings");
			//throw new OpenEditException("No gpt-key defined in catalog settings");
		}
		
		return apikey;
	}

	public LLMResponse runPageAsInput(WebPageRequest inReq, String inModel, String inTemplate)
	{

		inReq.putPageValue("model", inModel);
		inReq.putPageValue("gpt", this);
		inReq.putPageValue("mediaarchive", getMediaArchive());

		String input = loadInputFromTemplate(inReq, inTemplate);
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

	public LLMResponse createImage(WebPageRequest inReq, String inModel, int imagecount, String inSize, String style, String inPrompt)
	{
		if (getApikey() == null)
		{
			log.error("No gpt-key defined");
			return null;
		}
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

	public LLMResponse callFunction(WebPageRequest inReq, String inModel, String inFunction, String inQuery, int temp, int maxtokens, String inBase64Image) throws Exception
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
	
	public Collection<String> getSemanticTopics(WebPageRequest inReq, String inModel) throws Exception
	{
		MediaArchive archive = getMediaArchive();

		Asset asset = (Asset) inReq.getPageValue("asset");

		Collection<HashedMap> fields = new ArrayList<>();
		
		Collection<String> fieldIdsToCheck = Arrays.asList("keywords", "longcaption", "assettitle", "headline", "alternatetext", "fulltext");

		for (Iterator<String> iter = fieldIdsToCheck.iterator(); iter.hasNext();)
		{
			String fieldId = (String) iter.next();
			if (fieldId != null)
			{
				Object valueObj = asset.getValue(fieldId);
				if (valueObj == null)
				{
					log.info("Skipping empty field: " + fieldId);
					continue;
				}
				if(valueObj instanceof ArrayList)
				{
					ArrayList<String> val = (ArrayList<String>) valueObj;
					valueObj = String.join(", ", val);
				}
				else if (valueObj instanceof LanguageMap)
				{
					LanguageMap val = (LanguageMap) valueObj;
					valueObj = val.getText("en");
				}
				if (!(valueObj instanceof String))
				{
					log.info("Skipping empty field: " + fieldId);
					continue;
				}
				
				String value = (String) valueObj;
				if (value == null || value.isEmpty())
				{
					log.info("Skipping empty field: " + fieldId);
					continue;
				}
				String name = fieldId;
				
				if(name.equals("keywords"))
				{
					name = "Keywords";
					Collection<String> aikeywords = asset.getValues("keywordsai");
					if(aikeywords != null && !aikeywords.isEmpty())
					{
						String extraKeys = String.join(", ", aikeywords);
						value = value.isEmpty() ? extraKeys : value + ", " + extraKeys;
					}
				}
				else if(name.equals("longcaption"))
				{
					name = "Description";
				}
				else if(name.equals("assettitle"))
				{
					name = "Title";
				}
				else if(name.equals("headline"))
				{
					name = "Caption";
				}
				else if(name.equals("alternatetext"))
				{
					name = "Alt Text";
				}
				else if(name.equals("fulltext"))
				{
					name = "Contents";
					value = value.substring(0, 500);
				}

				HashedMap fieldMap = new HashedMap();
				fieldMap.put("name", name);
				fieldMap.put("value", value);
				fields.add(fieldMap);

			}
		}

		inReq.putPageValue("fields", fields);
		inReq.putPageValue("model", inModel);

		String inStructure = loadInputFromTemplate(inReq, "/" + archive.getMediaDbId() + "/gpt/structures/semantic_topics.json");

		JSONParser parser = new JSONParser();
		JSONObject structureDef = (JSONObject) parser.parse(inStructure);

		String endpoint = "https://api.openai.com/v1/responses";
		HttpPost method = new HttpPost(endpoint);
		method.addHeader("authorization", "Bearer " + getApikey());
		method.setHeader("Content-Type", "application/json");
		method.setEntity(new StringEntity(structureDef.toJSONString(), StandardCharsets.UTF_8));

		CloseableHttpResponse resp = getConnection().sharedExecute(method);
		
		Collection<String> results = new ArrayList<>();
		
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
			JSONObject responseData = (JSONObject) parser.parse(new StringReader(text));
			JSONArray topics = (JSONArray) responseData.get("topics");
			if (topics == null || topics.isEmpty())
			{
				log.info("No topics found in structured data");
				return results;
			}
			
			for (Object topicObj : topics)
			{
				String topic = (String) topicObj;
				if (topic != null && !topic.isEmpty())
				{
					results.add(topic);
				}
			}
		}
		finally
		{
			connection.release(resp);
		}
		
		return results;

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
