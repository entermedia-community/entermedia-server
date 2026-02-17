package org.entermediadb.ai.informatics;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.assistant.QuestionsManager;
import org.entermediadb.asset.Asset;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.QueryBuilder;
import org.openedit.hittracker.HitTracker;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;

public class InformaticsManager extends BaseAiManager
{
	
	private static final Log log = LogFactory.getLog(InformaticsManager.class);
	
	public void processAll(ScriptLogger inLog)
	{
		processAssets(inLog);
		processEntities(inLog);
	}
	
	public void processAsset(ScriptLogger inLog, Asset inAsset)
	{
		Collection pageofhits = new ArrayList();
		pageofhits.add(inAsset);
		processAssets(inLog, pageofhits);
	}
	
	public void processAssets(ScriptLogger inLog)
	{
		QueryBuilder query = null;

		String allowclassifyothernodes = getMediaArchive().getCatalogSettingValue("allowclassifyothernodes");
		
		if (Boolean.valueOf(allowclassifyothernodes) )
		{
			//Classify assets from other nodes
			query = getMediaArchive().query("asset");
		}
		else {
			//Scan only local files
			query = getMediaArchive().localQuery("asset");
		}
		
		QueryBuilder orquery = getMediaArchive().query("asset").or().exact("previewstatus", "2").exact("previewstatus", "mime");

		query.addchild(orquery.getQuery()).
				exact("taggedbyllm", false).
				exact("llmerror", false);
		
		String categoryid = getMediaArchive().getCatalogSettingValue("llmmetadatastartcategory");
		if (categoryid != null)
		{
			//categoryid = "index";
			query.exact("category", categoryid);
		}

		String startdate = getMediaArchive().getCatalogSettingValue("ai_metadata_startdate");
		Date date = null;
		if (startdate == null || startdate.isEmpty())
		{
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_YEAR, -30);
			Date thirtyDaysAgo = cal.getTime();
			date = DateStorageUtil.getStorageUtil().parseFromObject(thirtyDaysAgo);
		}
		else
		{
			date = DateStorageUtil.getStorageUtil().parseFromObject(startdate);
		}
		query.after("assetaddeddate", date);


		HitTracker pendingrecords = query.search();
		pendingrecords.enableBulkOperations();
		pendingrecords.setHitsPerPage(25);
		
		if (Boolean.valueOf(allowclassifyothernodes) ) 
		{
			log.info("Asset search query: " + pendingrecords + " since:" +date);
		}
		else 
		{
			log.info("Asset local search query: " + pendingrecords + " since:" +date);
		}
		
		inLog.info("Processing Assets Informatics");

		if (!pendingrecords.isEmpty())
		{
			//inLog.info("Adding metadata to: " + pendingrecords.size() + " assets in category: " + categoryid + ", added after: " + startdate);

			for (int i = 0; i < pendingrecords.getTotalPages(); i++)
			{
				pendingrecords.setPage(i+1);
				Collection pageofhits = new ArrayList();
				try
				{
					long startTime = System.currentTimeMillis();
					for (Iterator iterator = pendingrecords.getPageOfHits().iterator(); iterator.hasNext();)
					{
						Data data = (Data) iterator.next();
						Asset asset = (Asset)getMediaArchive().getAssetSearcher().loadData(data);
						pageofhits.add(asset);
						
					}
					
					processAssets(inLog, pageofhits);
					
					long duration = (System.currentTimeMillis() - startTime) / 1000L;
					inLog.info("Processing " + pageofhits.size() + " records took "+duration +"s");
				}
				catch(Exception e)
				{
					inLog.error(e);
					getMediaArchive().saveData("asset", pageofhits);
					
					if (e instanceof OpenEditException) 
					{
						throw (OpenEditException) e;
					}
					throw new OpenEditException(e);
					
				}
			}
		}
		else
		{
			inLog.info("No Assets to Process:` " + pendingrecords.getFriendlyQuery());
			//inLog.info("AI assets to tag:` " + pendingrecords.getFriendlyQuery());
		}
	}

	protected void processAssets(ScriptLogger inLog, Collection pageofhits)
	{
		//Lock Assets
		User agent = getMediaArchive().getUser("agent");
		for (Iterator iterator = pageofhits.iterator(); iterator.hasNext();)
		{
			Asset asset = (Asset) iterator.next();
			asset.toggleLock(agent);
		}
		getMediaArchive().saveData("asset", pageofhits);
		
		Collection workinghits = new ArrayList(pageofhits);

		for (Iterator iterator2 = getInformatics().iterator(); iterator2.hasNext();)
		{
			MultiValued config = (MultiValued) iterator2.next();
			InformaticsProcessor processor = loadProcessor(config.get("bean"));
			//inLog.info(config.get("bean") +  " Processing " + pageofhits.size() + " assets" ); //Add Header Logs in each Bean
			processor.processInformaticsOnAssets(inLog, config, workinghits);
			
			for (Iterator iterator = pageofhits.iterator(); iterator.hasNext();)
			{
				MultiValued data = (MultiValued) iterator.next();
				if(data.getBoolean("llmerror"))
				{
					workinghits.remove(data); //We do not process more.
				}
			}
			getMediaArchive().saveData("asset", pageofhits); //Not need it?
		}
		
		for (Iterator iterator = pageofhits.iterator(); iterator.hasNext();)
		{
			Asset asset = (Asset) iterator.next();
			if(!asset.getBoolean("llmerror"))
			{
				asset.setValue("taggedbyllm", true);
			}
		}

		for (Iterator iterator = pageofhits.iterator(); iterator.hasNext();)
		{
			Asset asset = (Asset) iterator.next();
			asset.toggleLock(agent); //Todo: Implement Release
		}
		getMediaArchive().saveData("asset", pageofhits);
		
		inLog.info("Processing Informatics on Assets Complete");

	}

	public void processEntities(ScriptLogger inLog)
	{
		
		HitTracker allmodules = getMediaArchive().query("module").exact("semanticenabled", true).search();
		Collection<String> ids = allmodules.collectValues("id");
		
		if(!ids.isEmpty())
		{
			ids.remove("asset");
		}
		if(ids.isEmpty())
		{
			inLog.info("No modules with semantic enabled found.");
			return;
		}
		
		inLog.info("Processing Entities Informatics");
		
		QueryBuilder query = null;
		String allowclassifyothernodes = getMediaArchive().getCatalogSettingValue("allowclassifyothernodes");
		if (Boolean.valueOf(allowclassifyothernodes) )
		{
			//Classify assets from other nodes
			query = getMediaArchive().query("modulesearch");	
		}
		else {
			//Scan only local entities
			query = getMediaArchive().localQuery("modulesearch");
		}
				//.exact("semantictopicsindexed", false)
				//.missing("semantictopics")
		query.exact("taggedbyllm", false)
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
		query.after("entity_date", date);

		//inLog.info("Running entity search query: " + query);

		HitTracker pendingrecords = query.search();
		pendingrecords.enableBulkOperations();
		pendingrecords.setHitsPerPage(5); //TODO:

		//inLog.info("Entities  " + ids + " with " + pendingrecords + " from date: " + date );
		
		Collection validhits = new ArrayList();
		if (!pendingrecords.isEmpty())
		{
			//inLog.info("Adding metadata to: " + pendingrecords);

			for (int i = 0; i < pendingrecords.getTotalPages(); i++)
			{
				pendingrecords.setPage(i+1);
				Collection pageofhits = pendingrecords.getPageOfHits();
				
				validhits = findValidRecords(pendingrecords.getPageOfHits());
				
				if (validhits.isEmpty())
				{
					continue;
				}

				Collection workinghits = new ArrayList(validhits);
				for (Iterator iterator2 = getInformatics().iterator(); iterator2.hasNext();)
				{
					MultiValued config = (MultiValued) iterator2.next();
					InformaticsProcessor processor = loadProcessor(config.get("bean"));
					//inLog.info("Processing : " + config);
					processor.processInformaticsOnEntities(inLog, config, workinghits);
					for (Iterator iterator = validhits.iterator(); iterator.hasNext();)
					{
						MultiValued data = (MultiValued) iterator.next();
						if(data.getBoolean("llmerror"))
						{
							workinghits.remove(data); //We do not process more.
						}
					}
				}
				//Group them by type
				
				for (Iterator iterator = validhits.iterator(); iterator.hasNext();)
				{
					MultiValued data = (MultiValued) iterator.next();
					if(!data.getBoolean("llmerror"))
					{
						data.setValue("taggedbyllm", true);
					}
				}		

				Map<String, Collection> groupbymodule = groupByModule(validhits);
				for (Iterator iterator = groupbymodule.keySet().iterator(); iterator.hasNext();)
				{
					String moduleid = (String) iterator.next();
					Collection tosave = groupbymodule.get(moduleid);
					getMediaArchive().saveData(moduleid,tosave);
					
				}

			}
			
			
		}
		if (!validhits.isEmpty())
		{
			inLog.info("Processing " + validhits.size() +" Entities Informatics Complete.");
		}
		else
		{
			inLog.info("No Entities to Process:` " + pendingrecords.getFriendlyQuery());
		}
	
	}
	
	
	

	private Collection findValidRecords(List inPageOfHits)
	{
		// TODO Auto-generated method stub
		Collection valid = new ArrayList();
		for (Iterator iterator = inPageOfHits.iterator(); iterator.hasNext();)
		{
			MultiValued entity = (MultiValued) iterator.next();
			String assetid = entity.get("primarymedia");
			if(assetid == null)
			{
				assetid = entity.get("primaryimage");
			}
			if (assetid != null || entity.get("markdowncontent") != null || entity.get("longcaption") != null)
			{
				valid.add(entity);
			}
		}
		return valid;
	}

	public InformaticsProcessor loadProcessor(String inName)
	{
		if(inName == null)
		{
			throw new IllegalArgumentException("Bean name not provided");
		}
		InformaticsProcessor processor = (InformaticsProcessor) getMediaArchive().getCacheManager().get("ai", "processor" + inName);
		if (processor == null)
		{
			processor = (InformaticsProcessor) getModuleManager().getBean(getCatalogId(), inName );
			getMediaArchive().getCacheManager().put("ai", "processor" + inName, processor);
		}
		return processor;
		
	}


	
	public Collection<MultiValued> getInformatics()
	{
		Collection<MultiValued> records = (Collection<MultiValued>) getMediaArchive().getCacheManager().get("ai", "informatics");
		if (records == null)
		{
			records = getMediaArchive().query("informatics").exact("enabled", true).sort("ordering").search();
			getMediaArchive().getCacheManager().put("ai", "informatics", records);
		}
		return records;
	}
	
	
	public void resetInformatics(String inEntityModuleId, Collection inEntities) 
	{
		
		ArrayList saveAll = new ArrayList();
		int count = 0;
		Collection ids = new HashSet();
		for (Iterator iterator = inEntities.iterator(); iterator.hasNext();)
		{
			Data entity = (Data) iterator.next();
			
			entity.setValue("taggedbyllm", false);
			entity.setValue("llmerror", false);
				
			entity.setValue("semantictopics", null);
			
			entity.setValue("entityembeddingstatus", null);
			entity.setValue("pagescreatedfor", null);
			entity.setValue("totalpages", null);
			
			//entity.setValue("searchcategory", null);
				
			//entity.setValue("keywords", null);
			//entity.setValue("longcaption", null);
			ids.add(entity.getId());
			saveAll.add(entity);
				
		}
		getMediaArchive().saveData(inEntityModuleId, saveAll);
		log.info("Saved " + saveAll.size() + " in " + inEntityModuleId);
		
		HitTracker todelete = getMediaArchive().query("semanticembedding").orgroup("dataid", ids).exact("moduleid", inEntityModuleId).search();
		getMediaArchive().getSearcher("semanticembedding").deleteAll(todelete, null);
		
		
		getMediaArchive().fireSharedMediaEvent("llm/addmetadata");
		
		
		
		
	}
}
