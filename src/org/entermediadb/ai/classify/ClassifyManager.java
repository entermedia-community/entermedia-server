package org.entermediadb.ai.classify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.informatics.InformaticsProcessor;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.PropertyDetail;
import org.openedit.users.User;

public class ClassifyManager extends InformaticsProcessor
{
	private static final Log log = LogFactory.getLog(ClassifyManager.class);
	
	@Override
	public void processInformaticsOnAssets(ScriptLogger inLog, MultiValued inConfig, Collection<MultiValued> assets)
	{
		int count = 1;

		Map<String, String> models = getModels();

		LlmConnection llmvisionconnection = getMediaArchive().getLlmConnection(models.get("vision"));
		LlmConnection llmsemanticconnection = getMediaArchive().getLlmConnection(models.get("semantic"));

		for (Iterator iterator = assets.iterator(); iterator.hasNext();)
		{
			MultiValued asset = (MultiValued) iterator.next(); 

			String mediatype = getMediaArchive().getMediaRenderType(asset);
			if( mediatype.equals("default") )
			{
				inLog.info(inConfig.get("bean") + " - Skipping asset " + asset);
				continue;
			}

			try{
				long startTime = System.currentTimeMillis();

				inLog.info(inConfig.get("bean") + " - Analyzing asset ("+count+"/"+assets.size()+")" + asset.getName());
				count++;

				boolean complete = processOneAsset(inConfig, llmvisionconnection, llmsemanticconnection, models, asset);
				if( !complete )
				{
					continue;
				}

				long duration = (System.currentTimeMillis() - startTime) / 1000L;
				inLog.info(inConfig.get("bean") + " - Took "+duration +"s");
			}
			catch(Exception e){
				inLog.error("LLM Error", e);
				asset.setValue("llmerror", true);
				continue;
			}
		}
	}

	protected boolean processOneAsset(MultiValued inConfig, LlmConnection llmvisionconnection, LlmConnection llmsemanticconnection, Map<String, String> models, MultiValued asset) throws Exception
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
		//	if( mediatype.equals("document") )
			//{
				// TODO: add better way to have media type specific fields
			//	continue;
		//	}
			if(!asset.hasValue(aifield.getId()) )
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
			String functionname = inConfig.get("aifunctionname") + "_asset";
			LlmResponse results = llmvisionconnection.callClassifyFunction(params, models.get("vision"), functionname, requestPayload, base64EncodedString);

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
							if (detail.isMultiValue())
							{
								Collection<String> values = Arrays.asList(value.split(","));
								datachanges.put(detail.getId(), values);
								asset.addValues(detail.getId(), values);
							}
							else if(detail.isList())
							{
								String listId = value.split("\\|")[0];
								datachanges.put(detail.getId(), listId);
								asset.setValue(detail.getId(), listId);
							}
							else
							{
								datachanges.put(detail.getId(), value);
								asset.setValue(detail.getId(), value);
							}
						}
					}

					//Save change event
					User agent = getMediaArchive().getUser("agent");
					if( agent != null)
					{
						getMediaArchive().getEventManager().fireDataEditEvent(getMediaArchive().getAssetSearcher(), agent, "assetgeneral", asset, datachanges);
					}
				}
				else {
					log.info("Asset "+asset.getId() +" "+asset.getName()+" - Nothing Detected.");
				}
			}
		}
		return true;
	}

	@Override
	public void processInformaticsOnEntities(ScriptLogger inLog, MultiValued inConfig, Collection<MultiValued> hits)
	{
		Map<String, String> models = getModels();

		LlmConnection llmvisionconnection = getMediaArchive().getLlmConnection(models.get("vision"));
		LlmConnection llmsemanticconnection = getMediaArchive().getLlmConnection(models.get("semantic"));

		Map<String, List<Data>> entitiestoprocess = new HashMap();

		for (Iterator iterator = hits.iterator(); iterator.hasNext();) 
		{
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

				boolean complete = processOneEntity(inConfig, llmvisionconnection, llmsemanticconnection, models, entity, moduleid);
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
	}
	
	protected boolean processOneEntity(MultiValued inConfig, LlmConnection llmvisionconnection, LlmConnection llmsemanticconnection, Map<String, String> models, MultiValued inEntity, String inModuleId) throws Exception
	{
		Collection detailsfields = getMediaArchive().getSearcher(inModuleId).getDetailsForView(inModuleId+"general");

		Collection<PropertyDetail> fieldsToFill = new ArrayList<PropertyDetail>();
		Collection<PropertyDetail> contextFields = new ArrayList<PropertyDetail>();

		for (Iterator iterator = detailsfields.iterator(); iterator.hasNext();)
		{
			PropertyDetail field = (PropertyDetail) iterator.next();
			if(inEntity.hasValue(field.getId()))
			{
				contextFields.add(field);
			}
			else if(field.get("aicreationcommand") != null)
			{
				fieldsToFill.add(field);
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
			
			String base64EncodedString = null;
			boolean isDocPage = inEntity.get("entitydocument") != null;
			if(isDocPage)
			{
				params.put("docpage", isDocPage);
//				base64EncodedString = loadImageContent(inEntity);
			}

			try 
			{
				
				String requestPayload = llmvisionconnection.loadInputFromTemplate("/" +  getMediaArchive().getMediaDbId() + "/ai/default/systemmessage/analyzeentity.html", params); 

				String functionname = inConfig.get("aifunctionname") + "_entity";
				LlmResponse results = llmvisionconnection.callClassifyFunction(params, models.get("vision"), functionname, requestPayload, base64EncodedString);
				
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
				return false;
			}
		}
		return true;

	}

		
//	public Map<String, Collection> createSemanticFieldsValues(LlmConnection llmconnection, String inModel, String inModuleId, MultiValued inData) throws Exception
//	{
//			MediaArchive archive = getMediaArchive();
//			
//			Collection detailsviews = getMediaArchive().query("view").exact("moduleid", inModuleId).exact("systemdefined", false).search();
//			
//			Map<String, PropertyDetail> detailsfields = new HashMap();
//			
//			for (Iterator iterator = detailsviews.iterator(); iterator.hasNext();) {
//				Data view = (Data) iterator.next();
//				Collection viewfields = getMediaArchive().getSearcher(inModuleId).getDetailsForView(view);
//				for (Iterator iterator2 = viewfields.iterator(); iterator2.hasNext();) {
//					PropertyDetail detail = (PropertyDetail) iterator2.next();
//					detailsfields.put(detail.getId(), detail);
//				}
//			}
//			
//			Map contextfields = new HashMap();
//			
//			for (Iterator iter = detailsfields.keySet().iterator(); iter.hasNext();)
//			{
//				String key = (String) iter.next();
//				PropertyDetail detail = (PropertyDetail) detailsfields.get(key);
//				
//				String fieldId = detail.getId();
//				
//				
//				String stringValue = null;
//				
//				if(detail.isBoolean() || detail.isDate())
//				{
//					continue;
//				}
//				else if(detail.isMultiLanguage())
//				{
//					stringValue = inData.getText(fieldId, "en");
//				}
//				else if(detail.isMultiValue() || detail.isList())
//				{
//					Collection<String> values = inData.getValues(fieldId);
//					if(values == null || values.isEmpty())
//					{
//						log.info("Skipping empty field: " + fieldId);
//						continue;
//					}
//					
//					Collection<String> textValues = new ArrayList<>();
//					if(detail.isMultiValue())
//					{
//						textValues.addAll(values);
//					}
//					else if (detail.isList())
//					{
//						for (Iterator iter2 = values.iterator(); iter2.hasNext();) 
//						{
//							String val = (String) iter2.next();
//							Data data = archive.getCachedData(detail.getListId(), val);
//							if(data != null)
//							{
//								String v = data.getName();
//								textValues.add(v);
//							}
//						}
//					}
//					stringValue = String.join(", ", textValues);
//				}
//				else 
//				{
//					stringValue = inData.get(fieldId);
//				}
//				
//				
//				if (stringValue == null)
//				{
//					log.info("Skipping empty field: " + fieldId);
//					continue;
//				}
//				
//				String label = detail.getName();
//
//				HashMap fieldMap = new HashMap();
//				fieldMap.put("label", label);
//				fieldMap.put("text", stringValue);
//				
//				contextfields.put(detail.getId(), fieldMap);
//			}
//			
//			if(inData.getBoolean("hasfulltext"))
//			{
//				String fulltext = inData.get("fulltext");
//				if(fulltext != null)
//				{				
//					fulltext = fulltext.replaceAll("\\s+", " ");
//					fulltext = fulltext.substring(0, Math.min(fulltext.length(), 5000));
//					HashMap fieldMap = new HashMap();
//					fieldMap.put("label", "Parsed Document Content");
//					fieldMap.put("text", fulltext);
//					
//					contextfields.put("fulltext", fieldMap);
//				}
//			}
//
//			if(contextfields.isEmpty())
//			{
//				log.info("No fields to check for semantic topics in " + inData.getId() + " " + inData.getName());
//				return null;
//			}

	


}
