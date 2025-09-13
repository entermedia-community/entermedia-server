package org.entermediadb.ai.classify;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.ai.semantics.SemanticFieldsManager;
import org.entermediadb.ai.semantics.SemanticInstructions;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.PropertyDetail;
import org.openedit.data.QueryBuilder;
import org.openedit.hittracker.HitTracker;
import org.openedit.modules.translations.LanguageMap;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;

public class ClassifyManager extends BaseAiManager
{
	private static final Log log = LogFactory.getLog(ClassifyManager.class);


	public void scanMetadataWithAIEntity(ScriptLogger inLog) throws Exception
	{
		Map<String, String> models = getModels();

		HitTracker allmodules = getMediaArchive().query("module").exact("semanticenabled", true).search();
		Collection<String> ids = allmodules.collectValues("id");

		if(ids.isEmpty())
		{
			inLog.info("No modules with semantic enabled found. Please enable semantic indexing for modules.");
			return;
		}
		ids.remove("asset");

		QueryBuilder query = getMediaArchive().getSearcher("modulesearch").query()
				.exact("semantictopicsindexed", false)
				.missing("semantictopics")
				.exact("taggedbyllm", false)
				.exact("llmerror", false)
				.put("searchtypes", ids);

		String startdate = getMediaArchive().getCatalogSettingValue("ai_metadata_startdate");
		Date date = null;
		if (startdate == null || startdate.isEmpty())
		{
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_YEAR, -30);
			Date thirtyDaysAgo = cal.getTime();
			date = DateStorageUtil.getStorageUtil().parseFromObject(thirtyDaysAgo);
		}
		else {
			date = DateStorageUtil.getStorageUtil().parseFromStorage(startdate);
		}
		inLog.info("Processing entity uploaded after: " + date);
		query.after("entity_date", date);

		inLog.info("Running entity search query: " + query);

		HitTracker hits = query.search();

		if(hits.size() > 0)
		{
			inLog.info("Adding metadata to: " + hits.size() + " entities, added after: " + startdate);
			hits.enableBulkOperations();
			processEntities(inLog, hits);
		}
		else
		{
			inLog.info("AI entities to tag: " + hits.getFriendlyQuery());
		}
	}

	protected void processEntities(ScriptLogger inLog, HitTracker entities)
	{
		int count = 1;
		List tosave = new ArrayList();

		Map<String, String> models = getModels();

		LlmConnection llmvisionconnection = getMediaArchive().getLlmConnection(models.get("vision"));
		LlmConnection llmsemanticconnection = getMediaArchive().getLlmConnection(models.get("semantic"));

		for(int i = 0; i < entities.getTotalPages(); i++)
		{
			entities.setPage(i + 1);

			Collection<Data> hits = entities.getPageOfHits();

			Map<String, List<Data>> entitiestoprocess = new HashMap();

			for (Iterator iterator = hits.iterator(); iterator.hasNext();) {
				MultiValued entity = (MultiValued) iterator.next();

				String moduleid = entity.get("entitysourcetype");
				if( moduleid == null)
				{
					log.info("Skipping entity with no source type: " + entity.getId() + " " + entity.getName());
					continue;
				}

				List<Data> bytype = entitiestoprocess.get(moduleid);
				if( bytype == null)
				{
					bytype = new ArrayList<Data>();
					entitiestoprocess.put(moduleid, bytype);
				}
				bytype.add(entity);

				try {
					long startTime = System.currentTimeMillis();

					inLog.info("Analyzing entity Id: " + entity.getId() + " " + entity.getName());

					boolean complete = processOneEntity(llmvisionconnection, llmsemanticconnection, models, entity, moduleid);
					if( !complete )
					{
						continue;
					}

					long duration = (System.currentTimeMillis() - startTime) / 1000L;
					inLog.info("Took "+duration +"s to process entity: " + entity.getId() + " " + entity.getName());

				} catch (Exception e) {
					inLog.error("LLM Error for entity: " + entity.getId() + " " + entity.getName(), e);
				}

			}
			for (Iterator iterator = entitiestoprocess.keySet().iterator(); iterator.hasNext();)
			{
				String moduleid = (String) iterator.next();
				tosave = entitiestoprocess.get(moduleid);
				getMediaArchive().saveData(moduleid,tosave);
			}
		}

	}
	
	protected boolean processOneEntity(LlmConnection llmvisionconnection, LlmConnection llmsemanticconnection, Map<String, String> models, MultiValued inEntity, String inModuleId) throws Exception
	{
		String primarymedia = inEntity.get("primarymedia");
		Asset inPrimaryAsset = getMediaArchive().getAsset(primarymedia);
		if(inPrimaryAsset == null)
		{
			primarymedia = inEntity.get("primaryimage");
			inPrimaryAsset = getMediaArchive().getAsset(primarymedia);
		}

		boolean isDocPage = inEntity.getValue("entitydocument") != null;

		String base64EncodedString = null;
//		if(isDocPage)
//		{
//			// TODO: get page thumbnail
//			String parentasset = inEntity.get("parentasset");
//			if(parentasset != null)
//			{
//				Asset parentAsset = getMediaArchive().getAsset(parentasset);
//				if(parentAsset != null)
//				{
//					base64EncodedString = loadBase64Image(parentAsset, "image3000x3000");
//				}
//			}
//		}
		
		Collection detailsfields = getMediaArchive().getSearcher(inModuleId).getDetailsForView(inModuleId+"general");

		Collection<PropertyDetail> fieldsToFill = new ArrayList<PropertyDetail>();
		Collection<PropertyDetail> contextFields = new ArrayList<PropertyDetail>();

		for (Iterator iterator = detailsfields.iterator(); iterator.hasNext();)
		{
			PropertyDetail field = (PropertyDetail) iterator.next();
			
			if(!inEntity.hasValue(field.getId()))
			{
				if(field.get("aicreationcommand") != null)
				{
					fieldsToFill.add(field);
				}
			}
			else 
			{
				contextFields.add(field);
			}
			
		}

		if(contextFields.size() == 0)
		{
			log.info("No context fields found for entity: " + inEntity.getId() + " " + inEntity.getName());
			return false;
		}

		
		if(fieldsToFill.isEmpty())
		{
			log.info("No fields to fill for entity: " + inEntity.getId() + " " + inEntity.getName());	
		}
		else
		{
			Map params = new HashMap();
			params.put("entity", inEntity);
			params.put("contextfields", contextFields);
			params.put("fieldstofill", fieldsToFill);
			
			if(isDocPage)
			{
				params.put("docpage", isDocPage);
			}

			try 
			{
				
				String requestPayload = llmvisionconnection.loadInputFromTemplate("/" +  getMediaArchive().getMediaDbId() + "/ai/default/systemmessage/analyzeentity.html", params); 
				LlmResponse results = llmvisionconnection.callClassifyFunction(params, models.get("vision"), "generate_entity_metadata", requestPayload, base64EncodedString);
				
				if (results != null)
				{
					JSONObject arguments = results.getArguments();
					if (arguments != null) {
						
						Map metadata =  (Map) arguments.get("metadata");
						if (metadata == null || metadata.isEmpty())
						{
							return false;
						}
						Map datachanges = new HashMap();
						for (Iterator iterator2 = metadata.keySet().iterator(); iterator2.hasNext();)
						{
							String inKey = (String) iterator2.next();
							PropertyDetail detail = getMediaArchive().getAssetPropertyDetails().getDetail(inKey);
							if (detail != null)
							{
								String value = (String)metadata.get(inKey);
								if(detail.isList())
								{
									String listId = value.split("\\|")[0];
									datachanges.put(detail.getId(), listId);
								}
								else if (detail.isMultiValue())
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
						
						for (Iterator iterator2 = datachanges.keySet().iterator(); iterator2.hasNext();)
						{
							String inKey = (String) iterator2.next();
							Object value = datachanges.get(inKey);
							
							inEntity.setValue(inKey, value);
							log.info("AI updated field "+ inKey + ": "+metadata.get(inKey));
						}
					}
					else {
						log.info("Entity "+inEntity.getId() +" "+inEntity.getName()+" - Nothing Detected.");
					}
				}
				inEntity.setValue("taggedbyllm", true);
				inEntity.setValue("llmerror", false);
			}
			catch (Exception e) 
			{
				log.error("Error generating metadata for entity "+ inEntity, e);
				inEntity.setValue("llmerror", true);
			}
			

		}
		

		if(inEntity.getValue("semantictopics") == null || inEntity.getValues("semantictopics").isEmpty())
		{
			Map params = new HashMap();
			params.put("docpage", isDocPage);
			
			Collection<PropertyDetail> fieldsToCheck = new ArrayList<>();
			Collection<String> fieldIds = Arrays.asList("keywords", "longcaption", "synapsis", "name");
			
			for (Iterator iterator = detailsfields.iterator(); iterator.hasNext();) {
				PropertyDetail detail = (PropertyDetail) iterator.next();
				if (fieldIds.contains(detail.getId()))
				{
					fieldsToCheck.add(detail);
				}

			}
			Map semantics = createSemanticFieldsValues(llmsemanticconnection, params, fieldsToCheck, models.get("semantic"), inEntity);
			
			if(semantics != null)
			{
				Collection<SemanticInstructions> fieldinstructions = getSemanticFieldsManager().getInstructions();
				
				for (Iterator<SemanticInstructions> iterator = fieldinstructions.iterator(); iterator.hasNext();) {
					SemanticInstructions instruction = iterator.next();
					
					String fieldname = instruction.getFieldName();
					
					Collection<String> semanticvalues = (Collection) semantics.get(fieldname);
					
					if(semanticvalues == null || semanticvalues.isEmpty())
					{
						if(inPrimaryAsset != null)
						{
							semanticvalues = inPrimaryAsset.getValues(fieldname);

							inEntity.setValue("semantictopics", semanticvalues);
							log.info("No semantic topics found from AI, using primary asset's sematic topics: " + semanticvalues);
						}
					}
					else
					{
						inEntity.setValue(fieldname, semanticvalues);
						log.info("AI updated semantic topics: " + semanticvalues);
					}
					
				}
			}
			else 
			{
				log.info("Semantics creation failed");
			}
		}
		
		return true;

	}

	public void scanMetadataWithAIAsset(ScriptLogger inLog) throws Exception
	{
		Map<String, String> models = getModels();

		String categoryid = getMediaArchive().getCatalogSettingValue("llmmetadatastartcategory");

		if (categoryid == null)
		{
			categoryid = "index";
		}

		QueryBuilder query = getMediaArchive().query("asset")
				.exact("previewstatus", "2")
				.exact("category", categoryid)
				.exact("taggedbyllm", false).exact("llmerror",false);

		String startdate = getMediaArchive().getCatalogSettingValue("ai_metadata_startdate");
		Date date = null;
		if (startdate == null || startdate.isEmpty())
		{
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_YEAR, -30);
			Date thirtyDaysAgo = cal.getTime();
			date = DateStorageUtil.getStorageUtil().parseFromObject(thirtyDaysAgo);
		}
		else {
			date = DateStorageUtil.getStorageUtil().parseFromStorage(startdate);
		}
		inLog.info("Processing assets uploaded after: " + date);
		query.after("assetaddeddate", date);

		inLog.info("Running asset search query: " + query);

		HitTracker assets = query.search();

		if(assets.size() > 0)
		{
			inLog.info("Adding metadata to: " + assets.size() + " assets in category: " + categoryid + ", added after: " + startdate);
			assets.enableBulkOperations();
			processAssets(inLog, assets);
		}
		else
		{
			inLog.info("AI assets to tag:` " + assets.getFriendlyQuery());
		}
	}

	protected void processAssets(ScriptLogger inLog, HitTracker assets)
	{
		int count = 1;
		List tosave = new ArrayList();

		Map<String, String> models = getModels();

		LlmConnection llmvisionconnection = getMediaArchive().getLlmConnection(models.get("vision"));
		LlmConnection llmsemanticconnection = getMediaArchive().getLlmConnection(models.get("semantic"));

		for (Iterator iterator = assets.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			Asset asset = getMediaArchive().getAsset(hit.getId());

			String mediatype = getMediaArchive().getMediaRenderType(asset);
			if( mediatype.equals("default") )
			{
				inLog.info("Skipping asset " + asset);
				continue;
			}

			try{
				long startTime = System.currentTimeMillis();

				inLog.info("Analyzing asset ("+count+"/"+assets.size()+") Id: " + asset.getId() + " " + asset.getName());

				boolean complete = processOneAsset(llmvisionconnection, llmsemanticconnection, models, asset);
				if( !complete )
				{
					continue;
				}

				tosave.add(asset);
				count++;

				long duration = (System.currentTimeMillis() - startTime) / 1000L;
				inLog.info("Took "+duration +"s");

				if( tosave.size() >= 10)	{
					getMediaArchive	().saveAssets(tosave);
					//searcher.saveAllData(tosave, null);
					inLog.info("Saved: " + tosave.size() + " assets ");
					tosave.clear();
				}
			}
			catch(Exception e){
				inLog.error("LLM Error", e);
				asset.setValue("llmerror", true);
				tosave.add(asset);
				continue;
			}
		}
		if( !tosave.isEmpty())
		{
			getMediaArchive().saveAssets(tosave);
			inLog.info("Saved: " + tosave.size() + " assets ");
		}
	}

	protected boolean processOneAsset(LlmConnection llmvisionconnection, LlmConnection llmsemanticconnection, Map<String, String> models, Asset asset) throws Exception
	{
		String mediatype = getMediaArchive().getMediaRenderType(asset);
		String base64EncodedString = null;
		if(mediatype.equals("image") || mediatype.equals("video"))
		{
			String imagesize = null;
			if (mediatype.equals("image"))
			{
				imagesize = "image3000x3000";
			}
			else if ( mediatype.equals("video"))
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
			if( mediatype.equals("document") && aifield.getId().equals("complexitylevel") )
			{
				// TODO: add better way to have media type specific fields
				continue;
			}
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

			String requestPayload = llmvisionconnection.loadInputFromTemplate("/" +  getMediaArchive().getMediaDbId() + "/ai/default/systemmessage/analyzeasset.html", params);
			LlmResponse results = llmvisionconnection.callClassifyFunction(params, models.get("vision"), "generate_asset_metadata", requestPayload, base64EncodedString);

			if (results != null)
			{
				JSONObject arguments = results.getArguments();
				if (arguments != null) {

					Map metadata =  (Map) arguments.get("metadata");
					if (metadata == null || metadata.isEmpty())
					{
						return false;
					}
					Map datachanges = new HashMap();
					for (Iterator iterator2 = metadata.keySet().iterator(); iterator2.hasNext();)
					{
						String inKey = (String) iterator2.next();
						PropertyDetail detail = getMediaArchive().getAssetPropertyDetails().getDetail(inKey);
						if (detail != null)
						{
							String value = (String)metadata.get(inKey);
							if(detail.isList())
							{
								String listId = value.split("\\|")[0];
								datachanges.put(detail.getId(), listId);
							}
							else if (detail.isMultiValue())
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

			Collection<String> fieldIds = Arrays.asList("keywords", "longcaption", "assettitle", "headline", "alternatetext", "fulltext");

			Collection<PropertyDetail> assetDetails = getMediaArchive().getAssetPropertyDetails();
			Collection<PropertyDetail> fieldsToCheck = new ArrayList<>();
			for (Iterator iterator = assetDetails.iterator(); iterator.hasNext();) {
				PropertyDetail detail = (PropertyDetail) iterator.next();
				if (fieldIds.contains(detail.getId()))
				{
					fieldsToCheck.add(detail);
				}

			}

			Map semantics = createSemanticFieldsValues(llmsemanticconnection, params, fieldsToCheck, models.get("semantic"), asset);
			Collection<String> semantic_topics = (Collection) semantics.get("semantictopics");
			
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
		long starttime = System.currentTimeMillis();
		ArrayList<String> args = new ArrayList<String>();
		args.add(item.getAbsolutePath());
		args.add("-resize");
		args.add("1500x1500");
		args.add("jpg:-");
		Exec exec = (Exec)getMediaArchive().getBean("exec");

		ExecResult result = exec.runExecStream("convert", args, output, 5000);
		byte[] bytes = output.toByteArray();  // Read InputStream as bytes
		String base64EncodedString = Base64.getEncoder().encodeToString(bytes); // Encode to Base64
		long duration = (System.currentTimeMillis() - starttime) ;
		log.info("Loaded and encoded " + inAsset.getName() + " in "+duration+"ms");
		return base64EncodedString;

	}

	public Map createSemanticFieldsValues(LlmConnection llmconnection, Map params, Collection<PropertyDetail> inFieldToCheck, String inModel, Data inData) throws Exception
	{
		MediaArchive archive = getMediaArchive();

		Collection<HashMap> fields = new ArrayList<>();

		for (Iterator<PropertyDetail> iter = inFieldToCheck.iterator(); iter.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iter.next();
			String fieldId = detail.getId();
			if (fieldId != null)
			{
				String dataType = detail.getDataType();
				if
					(
						fieldId.startsWith("semantic") ||
						fieldId.equals("keywordsai") || // included in keywords
						(dataType != null && !dataType.equals("list")) ||
						fieldId.equals("primaryimage") ||
						fieldId.equals("primarymedia") ||
						fieldId.equals("entity_date") ||
						fieldId.equals("primaryaudio")
					)
				{
					continue;
				}

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
				String name = detail.getName();

				if(fieldId.equals("keywords"))
				{
					name = "Keywords";
					Collection<String> aikeywords = inData.getValues("keywordsai");
					if(aikeywords != null && !aikeywords.isEmpty())
					{
						String extraKeys = String.join(", ", aikeywords);
						value = value.isEmpty() ? extraKeys : value + ", " + extraKeys;
					}
				}
				else if(dataType != null && dataType.equals("list"))
				{
					Collection<String> values = inData.getValues(fieldId);
					Collection<String> textValues = new ArrayList<>();
					for (Iterator iter2 = values.iterator(); iter2.hasNext();) {
						String val = (String) iter2.next();
						Data data = archive.getCachedData(detail.getListId(), val);
						if(data != null)
						{
							String v = data.getName();
							textValues.add(v);
						}
					}
					value = String.join(", ", textValues);
				}
				else if(fieldId.equals("fulltext"))
				{
					name = "Text Contents";
					value = value.substring(0, 500);
				}

				HashMap fieldMap = new HashMap();
				fieldMap.put("name", name);
				fieldMap.put("value", value);
				fields.add(fieldMap);

			}
		}

		Map results = new HashMap();
		results.put("semantictopics", new ArrayList<String>());
		results.put("synapsis", null);

		if(fields.isEmpty())
		{
			log.info("No fields to check for semantic topics in " + inData.getId() + " " + inData.getName());
			return results;
		}

		Collection<SemanticInstructions> fieldinstructions = getSemanticFieldsManager().getInstructions();
		params.put("fieldinstructions", fieldinstructions);
		
		JSONObject structure = llmconnection.callStructuredOutputList("semantic_topics", inModel, fields, params);
		if (structure == null)
		{
			log.info("No structured data returned");
			return results;
		}

		String synapsis = (String) structure.get("synapsis");
		if (synapsis != null && !synapsis.isEmpty())
		{
			results.put("synapsis", synapsis);
		}

		JSONArray topics = (JSONArray) structure.get("topics");
		if (topics != null && !topics.isEmpty())
		{
			Collection<String> topicresults = new ArrayList();
			for (Object topicObj : topics)
			{
				String topic = (String) topicObj;
				if (topic != null && !topic.isEmpty())
				{
					topicresults.add(topic);
				}
			}
			results.put("semantictopics", topicresults);
		}

		return results;

	}

	public Map getSemanticAbstractAndTargetAudiences(LlmConnection llmconnection, Map params, String inModel, Data inData) throws Exception
	{
		Map results = new HashMap();

		if(inData.getValues("semantic_targetaudience") == null || !inData.getValues("semantic_targetaudience").isEmpty())
		{
			JSONObject structure = llmconnection.callStructuredOutputList("semantic_targetaudience", inModel, null, params);
			if (structure != null)
			{
				JSONArray audiences = (JSONArray) structure.get("audiences");
				if (audiences != null && !audiences.isEmpty())
				{
					Collection<String> targetaudience = new ArrayList<String>();

					for (Object audienceObj : audiences)
					{
						String audience = (String) audienceObj;
						if (audience != null && !audience.isEmpty())
						{
							targetaudience.add(audience);
						}
					}

					results.put("semantic_targetaudience", targetaudience);
				}
			}
		}

		if(inData.getValue("semantic_abstract") == null || inData.getValue("semantic_abstract").toString().isEmpty())
		{
			JSONObject structure = llmconnection.callStructuredOutputList("semantic_abstract", inModel, null, params);
			if (structure != null)
			{
				String semantic_abstract = (String) structure.get("abstract");
				if (semantic_abstract != null && !semantic_abstract.isEmpty())
				{
					results.put("semantic_abstract", semantic_abstract);
				}
			}
		}

		return results;
	}

	protected SemanticFieldsManager fieldSemanticFieldsManager;
	public SemanticFieldsManager getSemanticFieldsManager()
	{
		if (fieldSemanticFieldsManager == null)
		{
			fieldSemanticFieldsManager = (SemanticFieldsManager)getModuleManager().getBean(getCatalogId(),"semanticFieldsManager");
		}

		return fieldSemanticFieldsManager;
	}
}
