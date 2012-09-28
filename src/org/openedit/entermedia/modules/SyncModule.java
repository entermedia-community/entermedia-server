package org.openedit.entermedia.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermedia.upload.FileUpload;
import org.entermedia.upload.FileUploadItem;
import org.entermedia.upload.UploadRequest;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.Category;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.push.PushManager;

import com.openedit.WebPageRequest;
import com.openedit.util.PathUtilities;

public class SyncModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(SyncModule.class);
	protected PushManager fieldPushManager;
	

	public PushManager getPushManager()
	{
		return fieldPushManager;
	}

	public void setPushManager(PushManager inPushManager)
	{
		fieldPushManager = inPushManager;
	}

	public void acceptPush(WebPageRequest inReq)
	{
		FileUpload command = new FileUpload();
		command.setPageManager(getPageManager());
		UploadRequest properties = command.parseArguments(inReq);

		String sourcepath = inReq.getRequestParameter("sourcepath");
		//String original = inReq.getRequestParameter("original");
		MediaArchive archive = getMediaArchive(inReq);
		Asset target = archive.getAssetBySourcePath(sourcepath);
		if (target == null)
		{
			String id = inReq.getRequestParameter("id");
			target = archive.createAsset(id, sourcepath);
		}
		
//		String categories = inReq.getRequestParameter("categories");
//		String[] vals = categories.split(";");
//		archive.c
//		target.setCategories(cats);
		String categorypath = PathUtilities.extractDirectoryPath(sourcepath);
		Category category = archive.getCategoryArchive().createCategoryTree(categorypath);
		target.addCategory(category);
		
		String[] fields = inReq.getRequestParameters("field");
		archive.getAssetSearcher().updateData(inReq, fields, target);
		archive.saveAsset(target, inReq.getUser());
		List<FileUploadItem> uploadFiles = properties.getUploadItems();

		
		String type = inReq.findValue("uploadtype");
		if( type == null )
		{
			type = "generated";
		}
		String	saveroot = "/WEB-INF/data/" + archive.getCatalogId() + "/" + type + "/" + sourcepath;
			
		//String originalsroot = "/WEB-INF/data/" + archive.getCatalogId() + "/originals/" + sourcepath + "/";

		if (uploadFiles != null)
		{
			Iterator<FileUploadItem> iter = uploadFiles.iterator();
			while (iter.hasNext())
			{
				FileUploadItem fileItem = iter.next();

				String filename = fileItem.getName();
				if (type.equals("originals"))
				{
					properties.saveFileAs(fileItem, saveroot, inReq.getUser());
				}
				else
				{
					properties.saveFileAs(fileItem, saveroot + "/" + filename, inReq.getUser());
				}
			}
		}

	}

	public void resetPushStatus(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);

		String old = inReq.findValue("oldpushstatus");
		String newstatus = inReq.findValue("newpushstatus");
		
		getPushManager().resetPushStatus(archive, old, newstatus);
	}

	public void processPushQueue(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String ids = inReq.getRequestParameter("assetids");
		getPushManager().processPushQueue(archive, ids, inReq.getUser());
	}
	public void processDeletedAssets(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		//String ids = inReq.getRequestParameter("assetids");
		getPushManager().processDeletedAssets(archive, inReq.getUser());
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

		Collection all = archive.getAssetSearcher().getAllHits(inReq);
		inReq.putPageValue("assets", all);

		
		Collection importpending = getPushManager().getImportPendingAssets(archive);
		inReq.putPageValue("importpending", importpending);

		Collection importcomplete = getPushManager().getImportCompleteAssets(archive);
		inReq.putPageValue("importcomplete", importcomplete);

		Collection importerror = getPushManager().getImportErrorAssets(archive);
		inReq.putPageValue("importerror", importerror);
		
		//
		Collection pusherror = getPushManager().getErrorAssets(archive);
		inReq.putPageValue("pusherror", pusherror);

		Collection nogenerated = getPushManager().getNoGenerated(archive);
		inReq.putPageValue("nogenerated", nogenerated);

		Collection pushcomplete = getPushManager().getCompletedAssets(archive);
		inReq.putPageValue("pushcomplete", pushcomplete);

		Collection pushpending = getPushManager().getPendingAssets(archive);
		inReq.putPageValue("pushpending", pushpending);


	}
	
	public void pollRemotePublish(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
	
		getPushManager().pollRemotePublish(archive); //search for publish tasks and complete them with a push
	}
	
}
