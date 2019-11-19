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

		manager.processPull(archive, logger);

	}

	public void processDataQueue(WebPageRequest inReq)
	{
		//log.info("Starting pulling");
		MediaArchive archive = getMediaArchive(inReq);
		PullManager pullManager = getPullManager(archive.getCatalogId());
		ScriptLogger logger = (ScriptLogger) inReq.getPageValue("log");

		pullManager.processAllData(archive, logger);

	}

	@Deprecated
	public void listChanges(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String searchtype = inReq.findValue("searchtype");
		if (searchtype == null)
		{
			searchtype = "asset";
		}
		String fulldownload = inReq.getRequestParameter("fulldownload");
		HitTracker hits = null;
		if (fulldownload != null && Boolean.parseBoolean(fulldownload))
		{
			hits = archive.getSearcher(searchtype).getAllHits(inReq);
		}
		else
		{
			String lastpullago = inReq.getRequestParameter("lastpullago");
			if (lastpullago != null)
			{
				Date ago = DateStorageUtil.getStorageUtil().subtractFromNow(Long.parseLong(lastpullago));
				hits = getPullManager(archive.getCatalogId()).listRecentChanges(searchtype, ago);
			}
			else
			{
				String lastpulldate = inReq.getRequestParameter("lastpulldate");
				if (lastpulldate == null)
				{
					throw new OpenEditException("lastpullago not defined");
				}
				else
				{
					Date startingfrom = DateStorageUtil.getStorageUtil().parseFromStorage(lastpulldate);
					hits = getPullManager(archive.getCatalogId()).listRecentChanges(searchtype, startingfrom);
				}
			}
		}
		hits.enableBulkOperations();
		hits.setHitsPerPage(200);//TMP
		hits.getSearchQuery().setHitsName(inReq.findValue("hitsname"));
		inReq.putPageValue(hits.getHitsName(), hits);
		inReq.putPageValue("searcher", hits.getSearcher());

		//hitsassetassets/catalog
		inReq.putSessionValue("hitssessionid", hits.getSessionId());
		inReq.putSessionValue(hits.getSessionId(), hits);
		//inReq.putPageValue("mediaarchive",archive); 
	}

	@Deprecated
	public void listIDs(WebPageRequest inReq)
	{

		MediaArchive archive = getMediaArchive(inReq);
		String searchtype = inReq.findValue("searchtype");
		if (searchtype == null)
		{
			searchtype = "asset";
		}
		HitTracker hits = archive.getSearcher(searchtype).getAllHits(inReq);

		hits.enableBulkOperations();
		hits.setHitsPerPage(9000);//TMP
		hits.getSearchQuery().setHitsName(inReq.findValue("hitsname"));
		inReq.putPageValue(hits.getHitsName(), hits);
		inReq.putPageValue("searcher", hits.getSearcher());

		//hitsassetassets/catalog
		inReq.putSessionValue("hitssessionid", hits.getSessionId());
		inReq.putSessionValue(hits.getSessionId(), hits);
		//inReq.putPageValue("mediaarchive",archive); 
	}

	public void loadAll(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		String sessionid = inReq.getRequestParameter("hitssessionid");
		String page = inReq.getRequestParameter("page");
		HashSet types = new HashSet();
		HitTracker hits = null;
		ElasticNodeManager manager = (ElasticNodeManager) archive.getNodeManager();
		if (sessionid != null)
		{
			hits = (HitTracker) inReq.getSessionValue(sessionid);
		}
		if (hits == null)
		{
			String lastpullago = inReq.getRequiredParameter("lastpullago"); //force them to pick a date
			Date ago = null;
			if (lastpullago != null)
			{
				ago = DateStorageUtil.getStorageUtil().subtractFromNow(Long.parseLong(lastpullago));
			}
			hits = manager.getEditedDocuments(archive.getCatalogId(), ago);
		}
		if (page != null)
		{
			hits.setPage(Integer.parseInt(page));
		}
		
		JSONObject finaldata = createJsonFromHits(inReq, hits);

		String jsonString = finaldata.toJSONString();
		inReq.putPageValue("jsonString", jsonString);
	}

	protected JSONObject createJsonFromHits(WebPageRequest inReq, HitTracker hits)
	{
		
		
		MediaArchive archive = getMediaArchive(inReq);
		ElasticNodeManager manager = (ElasticNodeManager) archive.getNodeManager();

		JSONObject finaldata = new JSONObject();

		JSONObject response = new JSONObject();
		if (hits.isEmpty())
		{
			response.put("status", "empty");
		}
		else
		{
			response.put("status", "ok");
		}
		String sessionid = inReq.getRequestParameter("hitssessionid");

		response.put("totalhits", hits.size());
		response.put("hitsperpage", hits.getHitsPerPage());
		response.put("page", hits.getPage());
		response.put("pages", hits.getTotalPages());
		response.put("hitssessionid", sessionid);
		response.put("catalogid", archive.getCatalogId());

		finaldata.put("response", response);
		JSONArray generated = new JSONArray();

		JSONArray results = new JSONArray();
		for (Iterator iterator = hits.getPageOfHits().iterator(); iterator.hasNext();)
		{
			SearchHitData data = (SearchHitData) iterator.next();
			JSONObject indiHit = new JSONObject();
			String searchtype = data.getSearchHit().getType();
			indiHit.put("searchtype", searchtype);
			String index = data.getSearchHit().getIndex();
			indiHit.put("index", index);
			indiHit.put("catalog", manager.getAliasForIndex(index));
			indiHit.put("source", data.getSearchData());
			indiHit.put("id", data.getId());
			results.add(indiHit);

			if (searchtype.equals("asset")) ///Also add stuff to the generated list
			{
				JSONObject details = new JSONObject();
				details.put("id", data.getId());
				String sourcepath = (String) data.getSearchData().get("sourcepath");
				details.put("sourcepath", sourcepath);
				JSONArray files = new JSONArray();
				for (ContentItem item : archive.listGeneratedFiles(sourcepath))
				{
					JSONObject contentdetails = new JSONObject();
					contentdetails.put("filename", item.getName());
					contentdetails.put("path", item.getPath());
					contentdetails.put("lastmodified", item.getLastModified());
					files.add(contentdetails);
				}
				details.put("files", files);
				generated.add(details);
			}
		}
		finaldata.put("results", results);
		finaldata.put("generated", generated);
		
		return finaldata;
	}

	
}
