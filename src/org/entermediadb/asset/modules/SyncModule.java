package org.entermediadb.asset.modules;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.pull.PullManager;
import org.entermediadb.asset.push.PushManager;
import org.entermediadb.elasticsearch.ElasticNodeManager;
import org.entermediadb.elasticsearch.SearchHitData;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.repository.ContentItem;
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

	public void processPullQueue(WebPageRequest inReq)
	{
		//log.info("Starting pulling");
		MediaArchive archive = getMediaArchive(inReq);
		PullManager manager = getPullManager(archive.getCatalogId());
		ScriptLogger logger = (ScriptLogger) inReq.getPageValue("log");

		//TODO Deprecated ?
		//manager.processPull(archive, logger);

	}

	public void processDataQueue(WebPageRequest inReq)
	{
		//log.info("Starting pulling");
		MediaArchive archive = getMediaArchive(inReq);
		PullManager pullManager = getPullManager(archive.getCatalogId());
		ScriptLogger logger = (ScriptLogger) inReq.getPageValue("log");

		pullManager.processAllData(archive, logger);

	}

	public void loadAllChanges(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		String sessionid = inReq.getRequestParameter("hitssessionid");
		String page = inReq.getRequestParameter("page");
		HashSet types = new HashSet();
		HitTracker hits = null;
		ElasticNodeManager manager = (ElasticNodeManager) archive.getNodeManager();
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
			hits = manager.getEditedDocuments(archive.getCatalogId(), ago);
		}
		if (page != null)
		{
			hits.setPage(Integer.parseInt(page));
		}
		PullManager pullManager = getPullManager(archive.getCatalogId());
		JSONObject finaldata = pullManager.createJsonFromHits(archive,ago,hits);

		String jsonString = finaldata.toJSONString();
		inReq.putPageValue("jsonString", jsonString);
	}
	
	public void receiveDataChanges(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		PullManager pullManager = getPullManager(archive.getCatalogId());
		
		pullManager.receiveDataChanges(archive, inReq.getJsonRequest());
	}
	public void receiveFile(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		PullManager pullManager = getPullManager(archive.getCatalogId());
		pullManager.receiveFile(inReq, archive);
	}
	
}
