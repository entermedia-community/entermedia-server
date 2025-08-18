package org.entermediadb.ai.classify;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
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
import org.openedit.data.BaseData;
import org.openedit.data.PropertyDetail;
import org.openedit.data.QueryBuilder;
import org.openedit.hittracker.HitTracker;
import org.openedit.modules.translations.LanguageMap;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;

public class ClassifyManager extends BaseManager
{
	private static final Log log = LogFactory.getLog(ClassifyManager.class);
	
	public Map<String, String> getModels()
	{
		Map<String, String> models = new HashMap<>();
		String visionmodel = getMediaArchive().getCatalogSettingValue("llmvisionmodel");
		if(visionmodel == null) {
			visionmodel = "gpt-5-nano";
		}
		models.put("vision", visionmodel);

		String semanticmodel = getMediaArchive().getCatalogSettingValue("llmsemanticmodel");
		if(semanticmodel == null) {
			semanticmodel = "gpt-4o-mini";
		}
		models.put("semantic", semanticmodel);
		
		return models;
	}


	public void scanMetadataWithAIEntity(ScriptLogger inLog) throws Exception
	{
		Map<String, String> models = getModels();
		HitTracker allmodules = getMediaArchive().query("module").exact("semanticenabled", true).search();
		Collection<String> ids = allmodules.collectValues("id");
		
		QueryBuilder query = getMediaArchive().getSearcher("modulesearch").query();
		query.exact("semanticindexed", false);
		query.exists("semantictopics");
		query.put("searchtypes", ids);
		
		String startdate = getMediaArchive().getCatalogSettingValue("ai_metadata_startdate");
		
		DateFormat format = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH);
		
		if (startdate == null || startdate.isEmpty())
		{
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_YEAR, -30);
			Date thirtyDaysAgo = cal.getTime();
			
			startdate = format.format(thirtyDaysAgo);
		}
		
		Date date = format.parse(startdate);			
		query.after("entity_date", date);
		
		HitTracker hits = query.search();
		hits.enableBulkOperations();
		
		Collection<Data> toSave = new ArrayList();
		
		for (Iterator iterator = hits.iterator(); iterator.hasNext();) {
			Data hit = (Data) iterator.next();
			
			try {
				long startTime = System.currentTimeMillis();
				inLog.info("Analyzing entity Id: " + hit.getId() + " " + hit.getName());
				if(hit.getValue("semantictopics") == null || hit.getValues("semantictopics").isEmpty())
				{
					LlmConnection llmconnection = getMediaArchive().getLlmConnection(models.get("semantic"));
					Data primaryAsset = getMediaArchive().getAsset(hit.get("primarymedia"));
					if(primaryAsset == null)
					{
						primaryAsset = getMediaArchive().getAsset(hit.get("primaryimage"));
						inLog.info("No primary asset for entity: " + hit.getId() + " " + hit.getName());
						continue;
					}
					processOneEntity(llmconnection, models, hit, primaryAsset);
					toSave.add(hit);
				}
				
			} catch (Exception e) {
				// TODO: handle exception
			}
			
		}
		if( !toSave.isEmpty())
		{
			getMediaArchive().getSearcher("modulesearch").saveAllData(toSave, null);
			inLog.info("Saved: " + toSave.size() + " entities ");
		}
		
	}	
	
	public void scanMetadataWithAIAsset(ScriptLogger inLog) throws Exception
	{
		Map<String, String> models = getModels();

		LlmConnection llmconnection = getMediaArchive().getLlmConnection(models.get("vision"));
		
		if (!llmconnection.isReady())
		{
			inLog.info("LLM Manager is not Model: " + models + ". Verify LLM Server and Key.");
			return; // Not ready, so we cannot proceed
		}
		
		String categoryid = getMediaArchive().getCatalogSettingValue("llmmetadatastartcategory");
		
		if (categoryid == null)
	    {
	        categoryid = "index";
	    }
		
		QueryBuilder query = getMediaArchive().query("asset").exact("previewstatus", "2").exact("category", categoryid).exact("taggedbyllm",false).exact("llmerror",false);
		
		String startdate = getMediaArchive().getCatalogSettingValue("ai_metadata_startdate");
		
		DateFormat format = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH);
		
		if (startdate == null || startdate.isEmpty())
		{
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_YEAR, -30);
			Date thirtyDaysAgo = cal.getTime();
			
			startdate = format.format(thirtyDaysAgo);
		}
		
		Date date = format.parse(startdate);
		
		query.after("assetaddeddate", date);
		
		
		//Refine this to use a hit tracker?
		HitTracker assets = query.search();
		if(assets.size() < 1)
		{
			inLog.info("No assets to tag in category: " + categoryid);
			return;
		}

		inLog.info("AI manager selected: Model: "+ models + " - Adding metadata to: " + assets.size() + " assets in category: " + categoryid);
		
		assets.enableBulkOperations();
		processAssets(inLog, llmconnection, models, assets);
		
		getMediaArchive().fireSharedMediaEvent("llm/translatefields");

	}

	protected void processAssets(ScriptLogger inLog, LlmConnection llmconnection, Map<String, String> models, HitTracker assets)
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
				inLog.info("Skipping asset " + asset);
				continue;
			}

			tosave.add(asset);
			count++;
			
			try{
				long startTime = System.currentTimeMillis();

				inLog.info("Analyzing asset ("+count+"/"+assets.size()+") Id: " + asset.getId() + " " + asset.getName());

				boolean complete = processOneAsset(llmconnection, models, asset);
				if( !complete )
				{
					continue;
				}
				tosave.add(asset);
				//getMediaArchive().saveAsset(asset);

				long duration = (System.currentTimeMillis() - startTime) / 1000L;
				inLog.info("Took "+duration +"s");
				
				if( tosave.size() == 25)	{
					getMediaArchive().saveAssets(tosave);
					//searcher.saveAllData(tosave, null);
					inLog.info("Saved: " + tosave.size() + " assets ");
					tosave.clear();
				}
			}
			catch(Exception e){
				inLog.error("LLM Error", e);
				asset.setValue("llmerror", true);
				getMediaArchive().saveAsset(asset);
				continue;
			}	
		}
		if( !tosave.isEmpty())	
		{
			getMediaArchive().saveAssets(tosave);
			inLog.info("Saved: " + tosave.size() + " assets ");
		}
	}

	protected boolean processOneAsset(LlmConnection llmconnection, Map<String, String> models, Asset asset) throws Exception
	{
		String mediatype = getMediaArchive().getMediaRenderType(asset);
		String base64EncodedString = null;
		if(mediatype == "image" || mediatype == "video")
		{			
			String imagesize = null;
			if (mediatype == "image")
			{
				imagesize = "image3000x3000";
			}
			else if (mediatype == "video")
			{
				imagesize = "image1900x1080";
			}
			
			base64EncodedString = loadBase64Image(asset, imagesize);
			
			if( base64EncodedString == null)
			{
				return false;
			}
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
				
				String requestPayload = llmconnection.loadInputFromTemplate("/" +  getMediaArchive().getMediaDbId() + "/gpt/systemmessage/analyzeasset.html", params);
				LlmResponse results = llmconnection.callFunction(params, models.get("vision"), "generate_metadata", requestPayload,base64EncodedString);

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

				Collection<String> semantic_topics = getSemanticTopics(params, fieldIdsToCheck, models.get("semantic"), asset);
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
	
	private String loadBase64Image(Asset inAsset, String imagesize)
	{
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
				else if(name.equals("assettitle") || name.equals("name"))
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
		
		if(fields.isEmpty())
		{
			log.info("No fields to check for semantic topics in " + inData.getId() + " " + inData.getName());
			return null;
		}

		LlmConnection connection = getMediaArchive().getLlmConnection(inModel);
		
		Collection values = connection.callStructuredOutputList("semantic_topics", inModel, fields, params);
	
		return values;

	}

	protected boolean processOneEntity(LlmConnection llmconnection, Map<String, String> models, Data inEntity, Data inPrimaryAsset) throws Exception
	{
		Collection<String> entityFieldIdsToCheck = Arrays.asList("keywords", "longcaption", "name");
		
		Collection<String> fieldIdsToCheck = new ArrayList();
		
		
		Data fieldsdata = new BaseData();

		for (Iterator iterator = entityFieldIdsToCheck.iterator(); iterator.hasNext();)
		{
			String fieldId = (String) iterator.next();
			if(inEntity.getValue(fieldId) == null || inEntity.getValue(fieldId).toString().isEmpty())
			{
				if(inPrimaryAsset != null)
				{
					if(inPrimaryAsset.getValue(fieldId) != null && !inPrimaryAsset.getValue(fieldId).toString().isEmpty())
					{
						if (fieldId.equals("keywords") || fieldId.equals("keywordsai"))
						{
							fieldsdata.setValue(fieldId, inPrimaryAsset.getValues(fieldId));
						}
						else if (fieldId.equals("name"))
						{
							String name = (String) inEntity.getValue("name");
							if (name == null || name.isEmpty())
							{
								name = (String) inPrimaryAsset.getValue("assettitle");
							}
							fieldsdata.setValue(fieldId, name);
							fieldIdsToCheck.add(fieldId);
						}
						else
						{
							fieldsdata.setValue(fieldId, inPrimaryAsset.getValue(fieldId));
							fieldIdsToCheck.add(fieldId);
						}
					}
				}
				else 
				{
					log.info("Skipping empty field: " + fieldId + " for entity: " + inEntity.getId() + " " + inEntity.getName());
					continue;
				}
			}
			else
			{
				fieldsdata.setValue(fieldId, inEntity.getValue(fieldId));
				fieldIdsToCheck.add(fieldId);
			}
		}
		
		Collection<String> semantic_topics = getSemanticTopics(null, fieldIdsToCheck, models.get("semantic"), fieldsdata);
		if(semantic_topics == null || semantic_topics.isEmpty())
		{
			if(inPrimaryAsset != null)
			{
				Collection assetsemantictopics = inPrimaryAsset.getValues("semantictopics");
				if(assetsemantictopics != null && !assetsemantictopics.isEmpty())
				{
					inEntity.setValue("semantictopics", assetsemantictopics);
					log.info("No semantic topics found from AI, using primary asset's sematic topics: " + assetsemantictopics);
					return true;
				}
			}
		}
		else
		{
			inEntity.setValue("semantictopics", semantic_topics);
			log.info("AI updated semantic topics: " + semantic_topics);
			return true;
		}
		
		log.info("No semantic topics detected for inEntity: " + inEntity.getId() + " " + inEntity.getName());
		return false;
		
	}
	
}
