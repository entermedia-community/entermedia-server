package org.entermediadb.asset.modules;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.pull.PullManager;
import org.entermediadb.asset.push.PushManager;
import org.entermediadb.elasticsearch.ElasticNodeManager;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.util.DateStorageUtil;

public class SyncModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(SyncModule.class);
	protected PushManager fieldPushManager;

	public PushManager getPushManager(MediaArchive inArchive)
	{
		return (PushManager) getModuleManager().getBean(inArchive.getCatalogId(), "pushManager");
	}

	public PullManager getPullManager(String inCatalogId)
	{
		return (PullManager) getModuleManager().getBean(inCatalogId, "pullManager");
	}

	public void acceptPush(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		getPushManager(archive).acceptPush(inReq, archive);
	}

	public void resetPushStatus(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);

		String old = inReq.findValue("oldpushstatus");
		String newstatus = inReq.findValue("newpushstatus");

		getPushManager(archive).resetPushStatus(archive, old, newstatus);
	}

	public void processPushQueue(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String ids = inReq.getRequestParameter("assetids");
		getPushManager(archive).processPushQueue(archive, ids, inReq.getUser());
	}

	public void processDeletedAssets(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		//String ids = inReq.getRequestParameter("assetids");
		getPushManager(archive).processDeletedAssets(archive, inReq.getUser());
	}

	//	private boolean isOkToSendX(Data inHotfolder)
	//	{
	//		boolean active = Boolean.parseBoolean(inHotfolder.get("auto"));
	//		// TODO: check dates, times.
	//		return active;
	//	}

	public void togglePush(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		Searcher searcher = getSearcherManager().getSearcher(archive.getCatalogId(), "catalogsettings");
		Data setting = (Data) searcher.searchById("push_masterswitch");

		String switchvalue = setting.get("value");

		if (Boolean.parseBoolean(switchvalue))
		{
			switchvalue = "false";
		}
		else
		{
			switchvalue = "true";
		}
		setting.setProperty("value", switchvalue);
		searcher.saveData(setting, inReq.getUser());
		if ("true".equals(switchvalue))
		{
			archive.fireSharedMediaEvent("push/pushassets");
		}
		getPushManager(archive).toggle(archive.getCatalogId());
	}

	public void clearQueue(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		Searcher pushsearcher = archive.getSearcherManager().getSearcher(archive.getCatalogId(), "pushrequest");
		pushsearcher.deleteAll(inReq.getUser());
	}
	//	public void addAssetToQueue(WebPageRequest inReq) throws Exception
	//	{
	//		MediaArchive archive = getMediaArchive(inReq);
	//		
	//		//String enabled = archive.getCatalogSettingValue("push_convertpresets");
	//		
	//		Searcher pushsearcher = archive.getSearcherManager().getSearcher(archive.getCatalogId(), "pushrequest");
	//		//Searcher hot = archive.getSearcherManager().getSearcher( archive.getCatalogId(), "hotfolder");
	//
	//		//SearchQuery query = pushsearcher.createSearchQuery();
	//
	//		boolean foundone = false;
	//		String assetid = inReq.getRequestParameter("assetid");
	//		if( assetid == null)
	//		{
	//			//TODO: Remove bad assets?
	//			log.info("Warning: Checking all assets");
	//			Collection hits = archive.getAssetSearcher().getAllHits();
	//			for (Iterator iterator = hits.iterator(); iterator.hasNext();)
	//			{
	//				Data row = (Data)iterator.next();
	//				boolean found = getPushManager().checkPublish(archive, pushsearcher, row.getId(), inReq.getUser());
	//				if( found )
	//				{
	//					foundone = true;
	//				}
	//			}
	//		}
	//		else
	//		{
	//			foundone = getPushManager().checkPublish(archive, pushsearcher, assetid, inReq.getUser());
	//		}
	//		if( foundone )
	//		{
	//			archive.fireSharedMediaEvent("push/pushassets");
	//		}
	//		
	//	}

	public void loadQueue(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);

		SearchQuery q = archive.getAssetSearcher().createSearchQuery().append("category", "index");
		q.addNot("editstatus", "7");
		Collection all = archive.getAssetSearcher().search(q);
		inReq.putPageValue("assets", all);

		Collection importpending = getPushManager(archive).getImportPendingAssets(archive);
		inReq.putPageValue("importpending", importpending);

		Collection importcomplete = getPushManager(archive).getImportCompleteAssets(archive);
		inReq.putPageValue("importcomplete", importcomplete);

		Collection importerror = getPushManager(archive).getImportErrorAssets(archive);
		inReq.putPageValue("importerror", importerror);

		//
		Collection pusherror = getPushManager(archive).getErrorAssets(archive);
		inReq.putPageValue("pusherror", pusherror);

		Collection nogenerated = getPushManager(archive).getNoGenerated(archive);
		inReq.putPageValue("nogenerated", nogenerated);

		Collection pushcomplete = getPushManager(archive).getCompletedAssets(archive);
		inReq.putPageValue("pushcomplete", pushcomplete);

		Collection pushpending = getPushManager(archive).getPendingAssets(archive);
		inReq.putPageValue("pushpending", pushpending);

	}

	public void pollRemotePublish(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		getPushManager(archive).pollRemotePublish(archive);
		//getPublishChecker().addCatalogToMonitor(archive.getCatalogId());
		//getPushManager().pollRemotePublish(archive); //search for publish tasks and complete them with a push
	}
/**
	public void processPullQueue(WebPageRequest inReq)
	{
		//log.info("Starting pulling");
		MediaArchive archive = getMediaArchive(inReq);
		PullManager manager = getPullManager(archive.getCatalogId());
		ScriptLogger logger = (ScriptLogger) inReq.getPageValue("log");

		//TODO Deprecated ?
		//manager.processPull(archive, logger);

	}
*/
	
	/** 
	 * This is the main event
	 * @param inReq
	 */
	public void pullRemoteChanges(WebPageRequest inReq)
	{
		//log.info("Starting pulling");
		MediaArchive archive = getMediaArchive(inReq);
		PullManager pullManager = getPullManager(archive.getCatalogId());
		ScriptLogger logger = (ScriptLogger) inReq.getPageValue("log");

		pullManager.getDataPuller().pull(archive, logger);
		archive.fireGeneralEvent(inReq.getUser(), "cluster", "pulloriginals", null);
		

	}
	public void pullRecentUploads(WebPageRequest inReq)
	{
		//log.info("Starting pulling");
		MediaArchive archive = getMediaArchive(inReq);
		PullManager pullManager = getPullManager(archive.getCatalogId());
		ScriptLogger logger = (ScriptLogger) inReq.getPageValue("log");

		pullManager.getOriginalPuller().pull(archive, logger);

	}

	public void loadAllDataChanges(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		HitTracker hits = null;

		String sessionid = inReq.getRequestParameter("hitssessionid");
		String page = inReq.getRequestParameter("page");

		String lastpullago = inReq.getRequestParameter("lastpullago"); //force them to pick a date
		Date ago = null;
		if( lastpullago != null)
		{
			ago = DateStorageUtil.getStorageUtil().subtractFromNow(Long.parseLong(lastpullago));
		}
		if (sessionid != null)
		{
			hits = (HitTracker) inReq.getSessionValue(sessionid);
		}
		if (hits == null)
		{
			if( ago == null)
			{
				throw new OpenEditException("lastpull required");
			}
			ElasticNodeManager manager = (ElasticNodeManager) archive.getNodeManager();
			hits = manager.getEditedDocuments(archive.getCatalogId(), ago);
		}
		if (page != null)
		{
			hits.setPage(Integer.parseInt(page));
		}
		hits.setSessionId("hitsallchanges");
		
		PullManager pullManager = getPullManager(archive.getCatalogId());
		JSONObject finaldata = pullManager.getDataPuller().createJsonFromHits(archive,ago,hits);

		String jsonString = finaldata.toJSONString();
		inReq.putPageValue("jsonString", jsonString);
		inReq.putSessionValue(hits.getSessionId(), hits);
		
	}
	
	public void receiveDataChanges(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		PullManager pullManager = getPullManager(archive.getCatalogId());
		
		JSONArray todownload = pullManager.getDataPuller().receiveDataChanges(archive, inReq.getJsonRequest());
		
		JSONObject finaldata = new JSONObject();
		finaldata.put("catalogid", archive.getCatalogId());
		finaldata.put("fileuploads", todownload);
		
		inReq.putPageValue("finaldata", finaldata);
	}
	public void receiveFile(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		PullManager pullManager = getPullManager(archive.getCatalogId());
		pullManager.receiveFile(inReq, archive);
	}

	public void loadRecentUploads(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		String sessionid = inReq.getRequestParameter("hitssessionid");
		String page = inReq.getRequestParameter("page");
		ElasticNodeManager manager = (ElasticNodeManager) archive.getNodeManager();
		HitTracker hits = null;
		String lastpullago = inReq.getRequiredParameter("lastpullago"); //force them to pick a date
		Date ago = DateStorageUtil.getStorageUtil().subtractFromNow(Long.parseLong(lastpullago));
		if( ago == null)
		{
			throw new OpenEditException("lastpull required");
		}
			
		if (sessionid != null)
		{
			hits = (HitTracker) inReq.getSessionValue(sessionid);
		}
		if (hits == null)
		{
			String mastereditid = archive.getNodeManager().getLocalClusterId();
			/*
			SearchQuery basequery = archive.query("asset").exact("emrecordstatus.mastereditclusterid", mastereditid).sort("sourcepath").getQuery();
			
			SearchQuery orquery = archive.query("asset").or().after("assetaddeddate", ago).after("assetmodificationdate", ago).getQuery();
			basequery.addChildQuery(orquery);
			
			hits = archive.getSearcher("asset").search(basequery);
			hits.enableBulkOperations();
			*/
			hits = manager.getEditedDocuments(archive.getCatalogId(), ago);
			
		}
		if( !hits.isEmpty() )
		{
			log.info("Listed " + hits.size()  + " changes ");
		}
		if (page != null)
		{
			hits.setPage(Integer.parseInt(page));
		}
		JSONArray array = new JSONArray();
		if (hits.size() > 0) 
		{
			PullManager pullManager = getPullManager(archive.getCatalogId());
			array = pullManager.getOriginalPuller().listOriginalFiles(archive,hits);	
		}
		inReq.putPageValue("searcher", archive.getAssetSearcher());		
		inReq.putPageValue("hits", hits);
		inReq.putPageValue("results", array);
	}
	
	public void receiveOriginalsChanges(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		PullManager pullManager = getPullManager(archive.getCatalogId());
		inReq.putPageValue("searcher", archive.getAssetSearcher() );
		try
		{
			List todownload = pullManager.getOriginalPuller().receiveOriginalsChanges(archive, inReq.getJsonRequest());
			ListHitTracker hits = new ListHitTracker(todownload);
			inReq.putPageValue("hits", hits);
			
		}
		catch(Throwable ex)
		{
			inReq.putPageValue("error", ex);
		}
	}
}
