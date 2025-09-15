package org.entermediadb.ai.infomatics;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.PropertyDetail;
import org.openedit.data.QueryBuilder;
import org.openedit.hittracker.HitTracker;
import org.openedit.util.DateStorageUtil;

public class InformaticsManager extends BaseAiManager
{
	public void processAssets(ScriptLogger inLog)
	{
		Map<String, String> models = getModels();

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

		HitTracker assets = query.search();

		if (assets.size() > 0)
		{
			inLog.info("Adding metadata to: " + assets.size() + " assets in category: " + categoryid + ", added after: " + startdate);
			assets.enableBulkOperations();

			for (Iterator iterator2 = getInformatics().iterator(); iterator2.hasNext();)
			{
				MultiValued config = (MultiValued) iterator2.next();
				InformaticsProcessor processor = (InformaticsProcessor) getModuleManager().getBean(getCatalogId(), config.get("beanname"));
				processor.processIformaticsOnAssets(inLog, config, assets);
			}
			//Save Records here?
		}
		else
		{
			inLog.info("AI assets to tag:` " + assets.getFriendlyQuery());
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
		inLog.info("Processing entity uploaded after: " + date);
		query.after("entity_date", date);

		inLog.info("Running entity search query: " + query);

		HitTracker hits = query.search();

		if(hits.size() > 0)
		{
			inLog.info("Adding metadata to: " + hits.size() + " entities, added after: " + startdate);
			hits.enableBulkOperations();
			for (Iterator iterator2 = getInformatics().iterator(); iterator2.hasNext();)
			{
				MultiValued config = (MultiValued) iterator2.next();
				InformaticsProcessor processor = (InformaticsProcessor) getModuleManager().getBean(getCatalogId(), config.get("beanname"));
				processor.processIformaticsOnEntities(inLog, config, hits);
			}
			//Save Records here?
		}
		else
		{
			inLog.info("AI entities to tag: " + hits.getFriendlyQuery());
		}

	
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
