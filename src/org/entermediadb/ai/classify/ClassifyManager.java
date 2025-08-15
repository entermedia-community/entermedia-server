package org.entermediadb.ai.classify;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.manager.BaseManager;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.modules.translations.LanguageMap;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;

public class ClassifyManager extends BaseManager
{
	private static final Log log = LogFactory.getLog(ClassifyManager.class);


	public void scanEntityMetadataWithAI()
	{
		
	}	
	
	public void scanAssetMetadataWithAI()
	{

		Searcher searcher = getMediaArchive().getAssetSearcher();

		String model = getMediaArchive().getCatalogSettingValue("llmvisionmodel");
		if(model == null) {
			model = "gpt-4o-mini";
		}
		
		LlmConnection llmconnection = getMediaArchive().getLlmConnection(model);
		
		if (!llmconnection.isReady())
		{
			log.info("LLM Manager is not Model: " + model + ". Verify LLM Server and Key.");
			return; // Not ready, so we cannot proceed
		}
		
		
		String categoryid	 = getMediaArchive().getCatalogSettingValue("llmmetadatastartcategory");
		
		if (categoryid == null)
	    {
	        categoryid = "index";
	    }
		
		
		//Refine this to use a hit tracker?
		HitTracker assets = getMediaArchive().query("asset").exact("previewstatus", "2").exact("category", categoryid).exact("taggedbyllm",false).exact("llmerror",false).search();
		if(assets.size() < 1)
		{
			log.info("No assets to tag in category: " + categoryid);
			return;
		}

		log.info("AI manager selected: Model: "+ model + " - Adding metadata to: " + assets.size() + " assets in category: " + categoryid);
		
		assets.enableBulkOperations();
		int count = 1;
		List tosave = new ArrayList();
		
		Exec exec = (Exec)getMediaArchive().getBean("exec");
		
		for (Iterator iterator = assets.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			Asset asset = getMediaArchive().getAsset(hit.getId());

			String mediatype = getMediaArchive().getMediaRenderType(asset);
			String imagesize = null;
			if (mediatype == "image")
			{
				imagesize = "image3000x3000.jpg";
			}
			else if (mediatype == "video")
			{
				imagesize = "image1900x1080.jpg";
			}
			else {
				log.info("Skipping asset " + asset.getName() + " - Not an image or video.");
				continue;
			}
			ContentItem item = getMediaArchive().getGeneratedContent(asset, imagesize);
			if(!item.exists()) 
			{
				
				log.info("Missing " + imagesize + " generated image for asset ("+asset.getId()+") " + asset.getName());
				continue;
			}

			ByteArrayOutputStream output = new ByteArrayOutputStream();
			
			ArrayList<String> args = new ArrayList<String>();
			args.add(item.getAbsolutePath());
			args.add("-resize");
			args.add("1500x1500");
			args.add("jpg:-");
			
			String base64EncodedString = "";
			try {
				ExecResult result = exec.runExecStream("convert", args, output, 5000);
				byte[] bytes = output.toByteArray();  // Read InputStream as bytes
				base64EncodedString = Base64.getEncoder().encodeToString(bytes); // Encode to Base64
			} catch (Exception e) {
				log.info("Error encoding asset to Base64: ${e}");
				asset.setValue("llmerror", true);
				tosave.add(asset);
				continue;
			} 

			log.info("Analyzing asset ("+count+"/"+assets.size()+") Id: " + asset.getId() + " " + asset.getName());
			count++;

			
			try{
				long startTime = System.currentTimeMillis();
				
				Collection allaifields = getMediaArchive().getAssetPropertyDetails().findAiCreationProperties();
				Collection aifields = new ArrayList();
				for (Iterator iterator2 = allaifields.iterator(); iterator2.hasNext();)
				{
					PropertyDetail aifield = (PropertyDetail)iterator2.next();
					if(asset.getValue(aifield.getId()) == null || asset.getValue(aifield.getId()) == "")
					{
						aifields.add(aifield);
					}
				}
				if(!aifields.isEmpty())
				{	 
					Map params = new HashMap();
					params.put("asset", asset);
					params.put("aifields", aifields);
					
					String template = llmconnection.loadInputFromTemplate("/" +  getMediaArchive().getMediaDbId() + "/gpt/systemmessage/analyzeasset.html", params);
					LlmResponse results = llmconnection.callFunction(params, model, "generate_metadata", template, 0, 5000, base64EncodedString);

					boolean wasUpdated = false;
					if (results != null)
					{
						JSONObject arguments = results.getArguments();
						if (arguments != null) {

							Map metadata =  (Map) arguments.get("metadata");
							Map datachanges = new HashMap();
							for (Iterator iterator2 = metadata.keySet().iterator(); iterator2.hasNext();)
							{
								String inKey = (String) iterator2.next();
								PropertyDetail detail = getMediaArchive().getAssetPropertyDetails().getDetail(inKey);
								if (detail != null)
								{
									String value = (String)metadata.get(inKey);
									if (detail.isMultiValue())
									{
										Collection<String> values = Arrays.asList(value.split(","));
										datachanges.put(detail.getId(), values);
									}
									else 
									{
										datachanges.put(detail.getId(), value);
									}
								}
							}
							
							//Save change event
							User agent = getMediaArchive().getUser("agent");
							if( agent != null)
							{
								getMediaArchive().getEventManager().fireDataEditEvent(getMediaArchive().getAssetSearcher(), agent, "assetgeneral", asset, datachanges);
							}
							
							for (Iterator iterator2 = datachanges.keySet().iterator(); iterator2.hasNext();)
							{
								String inKey = (String) iterator2.next();
								Object value = datachanges.get(inKey);
								
								asset.setValue(inKey, value);
								log.info("AI updated field "+ inKey + ": "+metadata.get(inKey));
							}
						}
						else {
							log.info("Asset "+asset.getId() +" "+asset.getName()+" - Nothing Detected.");
						}
					}
				}

				if(asset.getValue("semantictopics") == null || asset.getValues("semantictopics").isEmpty())
				{
					Map params = new HashMap();
					params.put("asset", asset);
					params.put("aifields", aifields);

					Collection<String> semantic_topics = getSemanticTopics(params, model);
					if(semantic_topics != null && !semantic_topics.isEmpty())
					{
						asset.setValue("semantictopics", semantic_topics);
						log.info("AI updated semantic topics: " + semantic_topics);
					}
					else 
					{
						log.info("No semantic topics detected for asset: " + asset.getId() + " " + asset.getName());
					}
				}

				asset.setValue("taggedbyllm", true);
				tosave.add(asset);
				//getMediaArchive().saveAsset(asset);

				long duration = (System.currentTimeMillis() - startTime) / 1000L;
				log.info("Took "+duration +"s");
				
				if( tosave.size() == 25)	{
					getMediaArchive().saveAssets(tosave);
					//searcher.saveAllData(tosave, null);
					log.info("Saved: " + tosave.size() + " assets - " + searcher.getSearchType());
					tosave.clear();
				}
			}
			catch(Exception e){
				log.error("LLM Error", e);
				asset.setValue("llmerror", true);
				getMediaArchive().saveAsset(asset);
				continue;
			}	
		}
		if( tosave.size() > 0)	{
			getMediaArchive().saveAssets(tosave);
			log.info("Saved: " + tosave.size() + " assets - " + searcher.getSearchType());
		}
		
		getMediaArchive().fireSharedMediaEvent("llm/translatefields");

	}
	
	public Collection<String> getSemanticTopics(Map params, String inModel) throws Exception
	{
		MediaArchive archive = getMediaArchive();

		Asset asset = (Asset) params.get("asset");

		Collection<HashMap> fields = new ArrayList<>();
		
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

				HashMap fieldMap = new HashMap();
				fieldMap.put("name", name);
				fieldMap.put("value", value);
				fields.add(fieldMap);

			}
		}

		params.put("fields", fields);
		params.put("model", inModel);
		
		Collection<String> results = new ArrayList<>();
		/*

		String inStructure = loadInputFromTemplate("/" + archive.getMediaDbId() + "/gpt/structures/semantic_topics.json", params);

		JSONParser parser = new JSONParser();
		JSONObject structureDef = (JSONObject) parser.parse(inStructure);

		String endpoint = "https://api.openai.com/v1/responses";
		HttpPost method = new HttpPost(endpoint);
		method.addHeader("authorization", "Bearer " + getApikey());
		method.setHeader("Content-Type", "application/json");
		method.setEntity(new StringEntity(structureDef.toJSONString(), StandardCharsets.UTF_8));

		CloseableHttpResponse resp = getConnection().sharedExecute(method);
		
		
		
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
		*/
		
		return results;

	}
	
}
