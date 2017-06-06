package org.entermediadb.asset.modules;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.push.PushManager;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.SearchQuery;

public class SyncModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(SyncModule.class);
	protected PushManager fieldPushManager;

	public PushManager getPushManager(MediaArchive inArchive)
	{
		return (PushManager) inArchive.getModuleManager().getBean(inArchive.getCatalogId(), "pushManager");
	}

	
	public void acceptPush(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		getPushManager(archive).acceptPush(inReq,archive);
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
		Data setting = (Data)searcher.searchById("push_masterswitch");

		String switchvalue = setting.get("value");
		
		if( Boolean.parseBoolean(switchvalue) )  
		{
			switchvalue = "false";
		}
		else
		{
			switchvalue = "true";
		}
		setting.setProperty("value",switchvalue);
		searcher.saveData(setting, inReq.getUser());
		if( "true".equals(switchvalue)) 
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

	
	public void loadQueue(WebPageRequest inReq ) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);

		SearchQuery q = archive.getAssetSearcher().createSearchQuery().append("category","index");
		q.addNot("editstatus","7");
		Collection all =  archive.getAssetSearcher().search(q);
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
	
}
