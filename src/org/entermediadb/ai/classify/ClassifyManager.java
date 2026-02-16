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
import org.entermediadb.asset.Asset;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.users.User;
import org.openedit.util.PathUtilities;

public class ClassifyManager extends InformaticsProcessor
{
	private static final Log log = LogFactory.getLog(ClassifyManager.class);
	
	public LlmConnection getLlmNamingServer()
	{
		return getMediaArchive().getLlmConnection("classifyAsset");
	}
	
	public LlmConnection getEntityClassificationLlmConnection()
	{
		return getMediaArchive().getLlmConnection("classifyEntity");
	}
	
	@Override
	public void processInformaticsOnAssets(ScriptLogger inLog, MultiValued inConfig, Collection<MultiValued> assets)
	{
		int count = 1;

		for (Iterator iterator = assets.iterator(); iterator.hasNext();)
		{
			MultiValued asset = (MultiValued) iterator.next(); 

			String mediatype = getMediaArchive().getMediaRenderType(asset);
			if( mediatype.equals("default") )
			{
				//inLog.info(inConfig.get("bean") + " - Skipping asset " + asset);
				continue;
			}

			try{
				long startTime = System.currentTimeMillis();

				//inLog.info(inConfig.get("bean") + " - Analyzing asset ("+count+"/"+assets.size()+")" + asset.getName());
				count++;

				inLog.headline("Classifying asset: " + asset.getName());

				boolean ok = processOneAsset(inConfig, asset);
				if( !ok )
				{
					continue;
				}

				long duration = (System.currentTimeMillis() - startTime) / 1000L;
				inLog.info("Classified successfully! Took "+duration +"s");
			}
			catch(Exception e){
				inLog.error("LLM Error", e);
				asset.setValue("llmerror", true);
				continue;
			}
		}
	}

	protected boolean processOneAsset(MultiValued inConfig, MultiValued asset) throws Exception
	{
		Collection allaifields = getMediaArchive().getAssetPropertyDetails().findAiCreationProperties();
		Collection<PropertyDetail> aifields = new ArrayList();
		
		String mediatype = getMediaArchive().getMediaRenderType(asset);

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
				if(aifield.getId().equals("documenttype") && !mediatype.equals("document"))
				{
					continue;
				}
				aifields.add(aifield);
			}
		}
		

		if(!aifields.isEmpty())
		{
			Map params = new HashMap();
			params.put("asset", asset);
			params.put("data", asset);
			params.put("aifields", aifields);
			
			LlmConnection llmconnection = getLlmNamingServer();
			
			String functionname = llmconnection.getAiFunctionName();
			
			String base64EncodedString = null;
			
			String textContent = null;
			
			Collection<PropertyDetail> contextFields = new ArrayList<PropertyDetail>();
			
			Searcher assetsearcher = getMediaArchive().getAssetSearcher();
			contextFields.add(assetsearcher.getDetail("name"));
			contextFields.add(assetsearcher.getDetail("assettype"));
			
			if(asset.hasValue("longcaption") && asset.get("longcaption").length() > 0)
			{
				contextFields.add( assetsearcher.getDetail("longcaption"));
			}
			if(asset.hasValue("keywords") && asset.getValues("keywords").size() > 0)
			{
				contextFields.add( assetsearcher.getDetail("keywords"));
			}
			
			if( !aifields.isEmpty() )
			{
				if( mediatype.equals("text") )
				{
					String fulltext = getMediaArchive().getAssetSearcher().getFulltext(asset);
					if( fulltext == null)
					{
						log.error("Text has no text: " + asset);
						return false;
					}
					if( fulltext.length() > 4000)
					{
						fulltext = fulltext.substring(0,Math.min(fulltext.length(), 4000));
					}
					asset.setValue("markdowncontent",fulltext);
					mediatype = "document";	
				}
				
				if(mediatype.equals("image"))
				{
					base64EncodedString = loadBase64Image(asset, "image3000x3000");
	
					if( base64EncodedString == null)
					{
						log.error("Image missing for asset: " + asset);
						return false;
					}
					functionname = functionname + "_image";
					
				}
				else if(mediatype.equals("document"))
				{
					textContent = asset.get("markdowncontent");
					
					if( textContent == null || textContent.trim().length() == 0)
					{
						textContent = (String) asset.getValue("fulltext");
					}
	
					if( textContent == null || textContent.trim().length() == 0)
					{
						log.error("Document has no text: " + asset);
						return false;
					}
					functionname = functionname + "_document";
				}
				else if(mediatype.equals("video") || mediatype.equals("audio"))
				{
					textContent = loadTranscript(asset);
	
					if( textContent == null)
					{
						log.error("Video missing for asset: " + asset);
						return false;
					}
					functionname = functionname + "_transcript";
				}
				else
				{
					///Check for text type
					log.info("Skipping media type: " + mediatype + " for asset: " + asset);
					return false;
				}
			}

			if( textContent != null && textContent.length() > 4000)
			{
				textContent = textContent.substring(0, Math.min(4000, textContent.length()));
			}
			
			params.put("contextfields", contextFields);
			
			LlmResponse results = llmconnection.callClassifyFunction(params, functionname, base64EncodedString, textContent);

			if (results != null)
			{
				JSONObject arguments = results.getMessageStructured();
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
		inLog.headline("Classifying " + hits.size() + " entities");

		for (Iterator iterator = hits.iterator(); iterator.hasNext();) 
		{
			MultiValued entity = (MultiValued) iterator.next();

			String moduleid = entity.get("entitysourcetype");
			if( moduleid == null)
			{
				log.info("Skipping entity with no source type: " + entity.getId() + " " + entity.getName());
				continue;
			}

			try {
				long startTime = System.currentTimeMillis();

				inLog.info("Classifying entity: " + entity.getName());

				boolean complete = processOneEntity(inConfig, entity, moduleid);
				if( !complete )
				{
					continue;
				}

				long duration = (System.currentTimeMillis() - startTime) / 1000L;
				inLog.info("Took "+duration +"s to process entity: " + entity.getId() + " " + entity.getName());

			} catch (Exception e) {
				inLog.error("LLM Error for entity: " + entity.getName(), e);
				entity.setValue("llmerror", true);
			}
		}
	}
	
	protected boolean processOneEntity(MultiValued inConfig, MultiValued inEntity, String inModuleId) throws Exception
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
		
		Map params = new HashMap();
		
		String assetid = inEntity.get("primarymedia");
		if( assetid == null)
		{
			assetid = inEntity.get("primaryimage");
		}
		
		Asset primaryasset = getMediaArchive().getAsset(assetid);
		
		if (primaryasset != null)
		{
			params.put("primaryasset", primaryasset);
			
			Searcher assetsearcher = getMediaArchive().getAssetSearcher();
			
			contextFields.add(assetsearcher.getDetail("longcaption"));
			contextFields.add(assetsearcher.getDetail("keywordsai"));
			contextFields.add(assetsearcher.getDetail("semantictopics"));
			contextFields.add(assetsearcher.getDetail("headline"));
			
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
			
			params.put("entity", inEntity);
			params.put("data", inEntity);
			params.put("contextfields", contextFields);
			params.put("fieldstofill", fieldsToFill);
			
			
			boolean isDocPage = inEntity.get("entitydocument") != null;
			if(isDocPage)
			{
				params.put("docpage", isDocPage);
//				base64EncodedString = loadImageContent(inEntity);
			}

			try 
			{
				LlmConnection llmconnection = getEntityClassificationLlmConnection();

				String functionname = llmconnection.getAiFunctionName();
				
				LlmResponse results = llmconnection.callClassifyFunction(params, functionname, null); 
				
				if (results != null)
				{
					JSONObject arguments = results.getMessageStructured();
					if (arguments != null) {
						
						Map metadata =  (Map) arguments.get("metadata");
						if (metadata == null || metadata.isEmpty())
						{
							return false;
						}
						Map datachanges = new HashMap();
						
						Searcher searcher = getMediaArchive().getSearcher(inModuleId);
						for (Iterator iterator2 = metadata.keySet().iterator(); iterator2.hasNext();)
						{
							String inKey = (String) iterator2.next();
							PropertyDetail detail = searcher.getDetail(inKey);
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

}
