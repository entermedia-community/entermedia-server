package org.entermediadb.ai.informatics;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
import org.openedit.MultiValued;
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

		String categoryid = getMediaArchive().getCatalogSettingValue("llmmetadatastartcategory");

		if (categoryid == null)
		{
			categoryid = "index";
		}

		QueryBuilder query = getMediaArchive().localQuery("asset").exact("previewstatus", "2").exact("category", categoryid).exact("taggedbyllm", false).exact("llmerror", false);

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
			date = DateStorageUtil.getStorageUtil().parseFromStorage(startdate);
		}
		inLog.info("Processing assets uploaded after: " + date);
		query.after("assetaddeddate", date);

		inLog.info("Running asset search query: " + query);

		HitTracker pendingrecords = query.search();
		pendingrecords.enableBulkOperations();
		pendingrecords.setHitsPerPage(25);

		if (!pendingrecords.isEmpty())
		{
			inLog.info("Adding metadata to: " + pendingrecords.size() + " assets in category: " + categoryid + ", added after: " + startdate);

			for (int i = 0; i < pendingrecords.getTotalPages(); i++)
			{
				pendingrecords.setPage(i+1);
				Collection pageofhits = pendingrecords.getPageOfHits();
				for (Iterator iterator2 = getInformatics().iterator(); iterator2.hasNext();)
				{
					MultiValued config = (MultiValued) iterator2.next();
					InformaticsProcessor processor = loadProcessor(config.get("bean"));
					processor.processInformaticsOnAssets(inLog, config, pageofhits);
				}
				//Save Records here?
				for (Iterator iterator = pageofhits.iterator(); iterator.hasNext();)
				{
					Data data = (Data) iterator.next();
					data.setValue("taggedbyllm", true);
				}
				getMediaArchive().saveData("asset", pageofhits);
			}
		}
		else
		{
			inLog.info("AI assets to tag:` " + pendingrecords.getFriendlyQuery());
		}
	}

	public void processEntities(ScriptLogger inLog)
	{
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

		inLog.info("Running entity search query: " + query);

		HitTracker pendingrecords = query.search();
		pendingrecords.enableBulkOperations();
		pendingrecords.setHitsPerPage(5); //TODO:

		inLog.info("Processing  " + ids + " modules " + pendingrecords);

		if (!pendingrecords.isEmpty())
		{
			inLog.info("Adding metadata to: " + pendingrecords);

			for (int i = 0; i < pendingrecords.getTotalPages(); i++)
			{
				pendingrecords.setPage(i+1);
				Collection pageofhits = pendingrecords.getPageOfHits();
				
				for (Iterator iterator2 = getInformatics().iterator(); iterator2.hasNext();)
				{
					MultiValued config = (MultiValued) iterator2.next();
					InformaticsProcessor processor = loadProcessor(config.get("bean"));
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
