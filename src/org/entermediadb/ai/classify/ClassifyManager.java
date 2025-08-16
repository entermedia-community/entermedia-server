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
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.manager.BaseManager;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.data.PropertyDetail;
import org.openedit.hittracker.HitTracker;
import org.openedit.modules.translations.LanguageMap;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;

public class ClassifyManager extends BaseManager
{
	private static final Log log = LogFactory.getLog(ClassifyManager.class);


	public void scanMetadataWithAIEntity()
	{
		
	}	
	
	public void scanMetadataWithAIAsset(ScriptLogger inLog)
	{
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
		
		String categoryid = getMediaArchive().getCatalogSettingValue("llmmetadatastartcategory");
		
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

		inLog.info("AI manager selected: Model: "+ model + " - Adding metadata to: " + assets.size() + " assets in category: " + categoryid);
		
		assets.enableBulkOperations();
		processAssets(llmconnection, model, assets);
		
		getMediaArchive().fireSharedMediaEvent("llm/translatefields");

	}

	protected void processAssets(LlmConnection llmconnection, String model, HitTracker assets)
	{
		int count = 1;
		List tosave = new ArrayList();
		
		for (Iterator iterator = assets.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			Asset asset = getMediaArchive().getAsset(hit.getId());
			
			String mediatype = getMediaArchive().getMediaRenderType(asset);
			if( mediatype.equals("default") )
			{
				//Skip? Transcript in MetadataExtractor
				log.info("Skipping asset " + asset);
				continue;
			}
//			else {
//				log.info("Skipping asset " + inAsset.getName() + " - Not an image or video.");
//				return null;
//			}
			tosave.add(asset);
			count++;
			
			try{
				long startTime = System.currentTimeMillis();

				log.info("Analyzing asset ("+count+"/"+assets.size()+") Id: " + asset.getId() + " " + asset.getName());
				
				boolean complete = processOneAsset(llmconnection, model,asset);
				if( !complete )
				{
					continue;
				}
				tosave.add(asset);
				//getMediaArchive().saveAsset(asset);

				long duration = (System.currentTimeMillis() - startTime) / 1000L;
				log.info("Took "+duration +"s");
				
				if( tosave.size() == 25)	{
					getMediaArchive().saveAssets(tosave);
					//searcher.saveAllData(tosave, null);
					log.info("Saved: " + tosave.size() + " assets ");
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
		if( !tosave.isEmpty())	
		{
			getMediaArchive().saveAssets(tosave);
			log.info("Saved: " + tosave.size() + " assets ");
		}
	}
	
	protected boolean processOneAsset(LlmConnection llmconnection, String model, Asset asset) throws Exception
	{
		String base64EncodedString = loadBase64Image(asset);

		if( base64EncodedString == null)
		{
			return false;
		}
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
				LlmResponse results = llmconnection.callFunction(params, model, "generate_metadata", template,base64EncodedString);

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

				Collection<String> fieldIdsToCheck = Arrays.asList("keywords", "longcaption", "assettitle", "headline", "alternatetext", "fulltext");

				Collection<String> semantic_topics = getSemanticTopics(params, fieldIdsToCheck, model,asset);
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
			return true;
	}
	
	private String loadBase64Image(Asset inAsset)
	{
		String mediatype = getMediaArchive().getMediaRenderType(inAsset);
		String imagesize = null;
		if (mediatype == "image")
		{
			imagesize = "image3000x3000.jpg";
		}
		else if (mediatype == "video")
		{
			imagesize = "image1900x1080.jpg";
		}
		ContentItem item = getMediaArchive().getGeneratedContent(inAsset, imagesize);
		if(!item.exists()) 
		{
			log.info("Missing " + imagesize + " generated image for asset ("+inAsset.getId()+") " + inAsset.getName());
			return null;
		}

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		
		ArrayList<String> args = new ArrayList<String>();
		args.add(item.getAbsolutePath());
		args.add("-resize");
		args.add("1500x1500");
		args.add("jpg:-");
		Exec exec = (Exec)getMediaArchive().getBean("exec");

		ExecResult result = exec.runExecStream("convert", args, output, 5000);
		byte[] bytes = output.toByteArray();  // Read InputStream as bytes
		String base64EncodedString = Base64.getEncoder().encodeToString(bytes); // Encode to Base64
		return base64EncodedString;
		
	}

	public Collection<String> getSemanticTopics(Map params, Collection inFieldToCheck, String inModel, Data inData) throws Exception
	{
		MediaArchive archive = getMediaArchive();

		Collection<HashMap> fields = new ArrayList<>();
		
		for (Iterator<String> iter = inFieldToCheck.iterator(); iter.hasNext();)
		{
			String fieldId = (String) iter.next();
			if (fieldId != null)
			{
				Object valueObj = inData.getValue(fieldId);
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
					Collection<String> aikeywords = inData.getValues("keywordsai");
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

		LlmConnection connection = getMediaArchive().getLlmConnection(inModel);
		
		Collection values = connection.callStructuredOutputList("semantic_topics",inModel,fields, params);
	
		return values;

	}
	
	protected boolean processOneEntity(LlmConnection llmconnection, String model, Data inEntity) throws Exception
	{
			Collection allaifields = getMediaArchive().getAssetPropertyDetails().findAiCreationProperties();
			Collection aifields = new ArrayList();
			for (Iterator iterator2 = allaifields.iterator(); iterator2.hasNext();)
			{
				PropertyDetail aifield = (PropertyDetail)iterator2.next();
				if(inEntity.getValue(aifield.getId()) == null || inEntity.getValue(aifield.getId()) == "")
				{
					aifields.add(aifield);
				}
			}
			if(!aifields.isEmpty())
			{	 
				Map params = new HashMap();
				params.put("entity", inEntity);
				params.put("aifields", aifields);
				
				String query = llmconnection.loadInputFromTemplate("/" +  getMediaArchive().getMediaDbId() + "/gpt/systemmessage/analyzentity.html", params);
				LlmResponse results = llmconnection.callFunction(params, model, "generate_metadata_entity", query);

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
//						User agent = getMediaArchive().getUser("agent");
//						if( agent != null)
//						{
//							getMediaArchive().getEventManager().fireDataEditEvent(getMediaArchive().getAssetSearcher(), agent, "assetgeneral", asset, datachanges);
//						}
						for (Iterator iterator2 = datachanges.keySet().iterator(); iterator2.hasNext();)
						{
							String inKey = (String) iterator2.next();
							Object value = datachanges.get(inKey);
							
							inEntity.setValue(inKey, value);
							log.info("AI updated field "+ inKey + ": "+metadata.get(inKey));
						}
					}
					else {
						log.info("inEntity "+inEntity.getId() +" "+inEntity.getName()+" - Nothing Detected.");
					}
				}
			}

			if(inEntity.getValue("semantictopics") == null || inEntity.getValues("semantictopics").isEmpty())
			{
				Map params = new HashMap();
				params.put("entity", inEntity);
				params.put("aifields", aifields);

				Collection<String> fieldIdsToCheck = Arrays.asList("name","keywords","longcaption");

				//Preload the image semantics
				//,"primarymedia","primaryimage"
				Asset attached = getMediaArchive().getAsset(inEntity.get(""));
				if(attached != null)
				{
					Collection semantics = attached.getValues(""); //TODO
					params.put("extrasemantics",semantics);
				}
				
				Collection<String> semantic_topics = getSemanticTopics(params,fieldIdsToCheck, model, inEntity);
				if(semantic_topics != null && !semantic_topics.isEmpty())
				{
					inEntity.setValue("semantictopics", semantic_topics);
					log.info("AI updated semantic topics: " + semantic_topics);
				}
				else 
				{
					log.info("No semantic topics detected for inEntity: " + inEntity.getId() + " " + inEntity.getName());
				}
			}
			inEntity.setValue("taggedbyllm", true);
			return true;
	}
	
}
