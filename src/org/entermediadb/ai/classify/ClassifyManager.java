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

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.llm.LLMResponse;
import org.entermediadb.llm.LlmConnection;
import org.entermediadb.manager.BaseManager;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;
import org.openedit.util.RequestUtils;

public class ClassifyManager extends BaseManager
{
	private static final Log log = LogFactory.getLog(ClassifyManager.class);
	protected RequestUtils fieldRequestUtils;
	
	public RequestUtils getRequestUtils() {
		return fieldRequestUtils;
	}

	public void setRequestUtils(RequestUtils inRequestUtils) {
		fieldRequestUtils = inRequestUtils;
	}


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
		String type = "gptManager";
		
		Data modelinfo = getMediaArchive().query("llmmodel").exact("modelid",model).searchOne();
		
		if(modelinfo != null)
		{
			type = modelinfo.get("llmtype") + "Manager";
		}
		
		LlmConnection llmconnection = (LlmConnection)getMediaArchive().getBean(type);
		
		if (!llmconnection.isReady())
		{
			log.info("LLM Manager is not ready: " + type + " Model: " + model + ". Verify LLM Server and Key.");
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

		log.info("AI manager selected: " + type + " Model: "+ model + " - Adding metadata to: " + assets.size() + " assets in category: " + categoryid);
		
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
					
					String template = llmconnection.loadInputFromTemplate(params, "/" +  getMediaArchive().getMediaDbId() + "/gpt/systemmessage/analyzeasset.html");
					LLMResponse results = llmconnection.callFunction(params, model, "generate_metadata", template, 0, 5000, base64EncodedString);

					boolean wasUpdated = false;
					if (results != null)
					{
						JSONObject arguments = results.getArguments();
						if (arguments != null) {

							Map metadata =  (Map) arguments.get("metadata");
							Map datachanges = new HashedMap();
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

					Collection<String> semantic_topics = llmconnection.getSemanticTopics(params, model);
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
		
		getMediaArchive().firePathEvent("llm/translatefields", inReq.getUser(), null);

	}
	
}
