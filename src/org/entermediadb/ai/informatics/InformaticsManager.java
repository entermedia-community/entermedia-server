package org.entermediadb.ai.informatics;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.asset.Asset;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.QueryBuilder;
import org.openedit.hittracker.HitTracker;
import org.openedit.util.DateStorageUtil;

public class InformaticsManager extends BaseAiManager
{
	public void processAll(ScriptLogger inLog)
	{
		processAssets(inLog);
		processEntities(inLog);
	}
	public void processAssets(ScriptLogger inLog)
	{
//		Map<String, String> models = getModels();
		inLog.info("Assets");

		QueryBuilder query = getMediaArchive().localQuery("asset").
				exact("previewstatus", "2").
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
		inLog.info("Asset search query: " + pendingrecords + " " +date);

		if (!pendingrecords.isEmpty())
		{
			//inLog.info("Adding metadata to: " + pendingrecords.size() + " assets in category: " + categoryid + ", added after: " + startdate);

			for (int i = 0; i < pendingrecords.getTotalPages(); i++)
			{
				pendingrecords.setPage(i+1);
				Collection pageofhits = new ArrayList();
				try
				{
					
					for (Iterator iterator = pendingrecords.getPageOfHits().iterator(); iterator.hasNext();)
					{
						Data data = (Data) iterator.next();
						Asset asset = (Asset)getMediaArchive().getAssetSearcher().loadData(data);
						pageofhits.add(asset);
						
					}
					for (Iterator iterator2 = getInformatics().iterator(); iterator2.hasNext();)
					{
						MultiValued config = (MultiValued) iterator2.next();
						InformaticsProcessor processor = loadProcessor(config.get("bean"));
						inLog.info(config.get("bean") +  " Processing " + pageofhits.size() + " assets" );
						processor.processInformaticsOnAssets(inLog, config, pageofhits);
						getMediaArchive().saveData("asset", pageofhits);
					}
					//Save Records here?
					for (Iterator iterator = pageofhits.iterator(); iterator.hasNext();)
					{
						Data data = (Data) iterator.next();
						data.setValue("taggedbyllm", true);
					}
					getMediaArchive().saveData("asset", pageofhits);
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
			inLog.info("AI assets to tag:` " + pendingrecords.getFriendlyQuery());
		}
	}

	public void processEntities(ScriptLogger inLog)
	{
		inLog.info("Processing Entities Informatics");
		HitTracker allmodules = getMediaArchive().query("module").exact("semanticenabled", true).search();
		Collection<String> ids = allmodules.collectValues("id");

		if(ids.isEmpty())
		{
			inLog.info("No modules with semantic enabled found. Please enable semantic indexing for modules.");
			return;
		}
		ids.remove("asset");

		QueryBuilder query = getMediaArchive().localQuery("modulesearch")
				//.exact("semantictopicsindexed", false)
				//.missing("semantictopics")
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
		query.after("entity_date", date);

		//inLog.info("Running entity search query: " + query);

		HitTracker pendingrecords = query.search();
		pendingrecords.enableBulkOperations();
		pendingrecords.setHitsPerPage(5); //TODO:

		inLog.info("Entities  " + ids + " with " + pendingrecords + " from date: " + date );
		

		if (!pendingrecords.isEmpty())
		{
			//inLog.info("Adding metadata to: " + pendingrecords);

			for (int i = 0; i < pendingrecords.getTotalPages(); i++)
			{
				pendingrecords.setPage(i+1);
				Collection pageofhits = pendingrecords.getPageOfHits();
				
				for (Iterator iterator2 = getInformatics().iterator(); iterator2.hasNext();)
				{
					MultiValued config = (MultiValued) iterator2.next();
					InformaticsProcessor processor = loadProcessor(config.get("bean"));
					inLog.info("Processing : " + config);
					processor.processInformaticsOnEntities(inLog, config, pageofhits);
				}
				//Group them by type
				for (Iterator iterator = pageofhits.iterator(); iterator.hasNext();)
				{
					Data data = (Data) iterator.next();
					data.setValue("taggedbyllm", true);
				}
				Map<String, Collection> groupbymodule = groupByModule(pageofhits);
				for (Iterator iterator = groupbymodule.keySet().iterator(); iterator.hasNext();)
				{
					String moduleid = (String) iterator.next();
					Collection tosave = groupbymodule.get(moduleid);
					getMediaArchive().saveData(moduleid,tosave);
					
				}
			}
		}
	
	}

	protected InformaticsProcessor loadProcessor(String inName)
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
}
