package org.entermediadb.asset.modules;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.upload.FileUpload;
import org.entermediadb.asset.upload.FileUploadItem;
import org.entermediadb.asset.upload.UploadRequest;
import org.entermediadb.desktops.Desktop;
import org.entermediadb.find.FolderManager;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.SearchQuery;
import org.openedit.repository.ContentItem;

import model.assets.AssetTypeManager;
import model.assets.LibraryManager;

public class AssetImportModule  extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(AssetImportModule.class);

	public FolderManager getFolderManager(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		FolderManager manager = (FolderManager) getModuleManager().getBean(archive.getCatalogId(), "folderManager");
		inReq.putPageValue("foldermanager", manager);
		return manager;
	}	
	public void assetsImported(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Collection hits = loadAssetHits(archive, inReq);
		if( hits == null)
		{
			log.error("No hits found");
		}
		//Set the asset type
		AssetTypeManager manager = new AssetTypeManager();
		manager.setContext(inReq);
		ScriptLogger logger = (ScriptLogger)inReq.getPageValue("log");
		manager.setLog(logger);
		manager.saveAssetTypes(hits, true);

		//TODO: Move this to AssetUtilities
//		boolean assigncategory = mediaArchive.isCatalogSettingTrue("assigncategoryonupload");
//		if(assigncategory) {
//			hits.each{
//				Asset current = it;
//				Category defaultcat = mediaArchive.getCategorySearcher().createCategoryPath(current.sourcePath);
//				
//				current.clearCategories();
//				current.addCategory(defaultcat);
//				mediaArchive.saveAsset(current, inReq.getUser());
//			}
//		}	
		
		//Look for collections and libraries
		LibraryManager librarymanager = new LibraryManager();
		librarymanager.setLog(logger);
		librarymanager.assignLibraries(archive, hits);
	}

	protected Collection loadAssetHits(MediaArchive archive, WebPageRequest inReq)
	{
		Collection hits = (Collection)inReq.getPageValue("hits");
		if( hits == null)
		{
			String ids = inReq.getRequestParameter("assetids");
			if( ids == null)
			{
			   log.info("AssetIDS required");
			   return null;
			}
			Searcher assetsearcher = archive.getAssetSearcher();
			SearchQuery q = assetsearcher.createSearchQuery();
			String assetids = ids.replace(","," ");
			q.addOrsGroup( "id", assetids );
		
			hits = assetsearcher.search(q);
		}
		return hits;
	}
	

	private long getLong(Object inObject)
	{
		if( inObject == null)
		{
			return -1;
		}
		if( inObject instanceof String)
		{
			return Long.parseLong((String)inObject);
		}
		if( inObject instanceof Integer)
		{
			return Integer.valueOf((int)inObject);
		}
		return (long)inObject;
	}

	
	public void desktopOpenFolder(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		FolderManager manager = getFolderManager(inReq);
		String collectionid = inReq.getRequestParameter("collectionid");
//		LibraryCollection col = manager.getLibraryCollection(archive, collectionid);
//		Desktop desktop = manager.getDesktopManager().getDesktop(inReq.getUserName());
//		desktop.openRemoteFolder(archive, col);
		
	}
	
	
	public void desktopOpenAsset(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String assetid = inReq.getRequestParameter("assetid");
		FolderManager manager = getFolderManager(inReq);

		Desktop desktop = manager.getDesktopManager().getDesktop(inReq.getUserName());
		desktop.openAsset(archive, assetid);
		Asset asset = getAsset(inReq);
		
		
		if(!asset.isLocked()) {
		asset.toggleLock(inReq.getUser());
		archive.saveAsset(asset);
		}
		
	}
	
	
	public void sendDesktopCommand(WebPageRequest inReq)
	{
		try
		{
			MediaArchive archive = getMediaArchive(inReq);
			String commands = inReq.getRequestParameter("command");
			FolderManager manager = getFolderManager(inReq);

			Desktop desktop = manager.getDesktopManager().getDesktop(inReq.getUserName());
			JSONParser parser = new JSONParser();
			JSONObject command = (JSONObject) parser.parse(commands);
			
			desktop.sendCommand(archive, command);
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		
	}
	
	public void downloadCollectionToClient(WebPageRequest inReq, MediaArchive inMediaArchive, String inCollectionid)
	{
		//Data collection = inMediaArchive.getData("librarycollection",inCollectionid);
		//LibraryCollection collection = getLibraryCollection(inMediaArchive, inCollectionid);
		//ContentItem childtarget = inMediaArchive.getPageManager().getRepository().getStub(inFolder);
		//utilities.exportCategoryTree(inMediaArchive, root, childtarget);

		//Send the client a download request
		FolderManager manager = getFolderManager(inReq);

		Desktop desktop = manager.getDesktopManager().getDesktop(inReq.getUserName());
		if (desktop == null)
		{
			throw new OpenEditException("Desktop disconnected");
		}
		//desktop.checkoutCollection(inMediaArchive, collection);
	}
	public void checkPullRemoteFolder(WebPageRequest inReq)
	{
		/*
		"entityid": "1234",
		"moduleid": "entityactivimoduleid,
		"rootpath": "/home/user/eMedia/",		
		"categorypath": "Activities/Paris",
        "files": [{path: filepath, size: 43232}], 
			"folders":  [{path: "/home/user/eMedia/Activities/Sub1/Sub2"}] 
		*/
		Map params = inReq.getJsonRequest();
		
		MediaArchive archive = getMediaArchive(inReq);
		Data entity = archive.getCachedData( (String)params.get("moduleid"),(String)params.get("entityid"));
		
		String categorypath = inReq.getRequestParameter("categorypath"); 
		Category cat = archive.getCategorySearcher().loadCategoryByPath(categorypath);
		FolderManager manager = getFolderManager(inReq);
		Map assetmap = manager.listAssetMap(archive, cat);
		
		//List remoteassets = (List)params.get("files");
		Map finallist = manager.removeDuplicateAssetsFrom(assetmap,params);
		
		inReq.putPageValue("assetmap", new JSONObject(finallist));
		
	}
	public void checkPushRemoteFolder(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String categorypath = inReq.getRequestParameter("categorypath"); 
		Category cat = archive.getCategorySearcher().loadCategoryByPath(categorypath);
		FolderManager manager = getFolderManager(inReq);
		Map assets = manager.listAssetMap(archive, cat);
		inReq.putPageValue("assetmap", new JSONObject(assets));
		
	}
	public void listConnectedDesktop(WebPageRequest inReq)
	{
		FolderManager manager = getFolderManager(inReq);
		Desktop desktop = manager.getDesktopManager().getDesktop(inReq.getUser());
		inReq.putPageValue("desktop",desktop);
	}

	public void exportCollection(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		FolderManager manager = getFolderManager(inReq);
		
		Desktop desktop = manager.getDesktopManager().getDesktop(inReq.getUserName());
		if( desktop.isBusy())
		{
			log.info("Desktop still busy");
			return;
		}
		/*
		 * String collectionid = loadCollectionId(inReq);
		 
		User user = inReq.getUser();
		if (user == null) 
		{
			throw new OpenEditException("User required ");
		}
//		Data collection = archive.getData("librarycollection", collectionid);

		// The trailing slash is needed for the recursive algorithm. Don't
		// delete.
//		String infolder = "/WEB-INF/data/" + archive.getCatalogId() + "/workingfolders/" + user.getId() + "/"
//				+ collection.getName();
//
//		Date now = new Date();
//
//		String stamp = DateStorageUtil.getStorageUtil().formatDateObj(now, "yyyy-MM-dd-HH-mm-ss");
//
//		infolder = infolder + "/" + stamp + "/";

		manager.downloadCollectionToClient(inReq, archive, collectionid);
		
		
		boolean lock = Boolean.parseBoolean(inReq.getRequestParameter("lockcollection"));
		if(lock) {
			archive.updateAndSave("librarycollection", collectionid, "lockedby", inReq.getUserName());
		}
		*/
		//inReq.putPageValue("exportpath", infolder);

		// if(getWebEventListener() != null)
		// {
		// WebEvent event = new WebEvent();
		// event.setSearchType(searcher.getSearchType());
		// event.setCatalogId(searcher.getCatalogId());
		// event.setOperation(searcher.getSearchType() + "/saved");
		// event.setProperty("dataid", data.getId());
		// event.setProperty("id", data.getId());
		//
		// event.setProperty("applicationid", inReq.findValue("applicationid"));
		//
		// getWebEventListener().eventFired(event);
		// }

		// Searcher librarycollectiondownloads =
		// archive.getSearcher("librarycollectiondownloads");
		//
		// Data history = librarycollectiondownloads.createNewData();
		//
		// history.setValue("owner", inReq.getUserName());
		// history.setValue("librarycollection", collectionid);
		// history.setValue("date", new Date());
		// history.setValue("revision", collection.get("revisions"));
		//
		// String fields[] = inReq.getRequestParameters("field");
		// librarycollectiondownloads.updateData(inReq, fields, history);
		// librarycollectiondownloads.saveData(history);

		// boolean zip = Boolean.parseBoolean(inReq.findValue("zip"));

	}

	
	public void uploadMedia(WebPageRequest inReq) throws Exception
	{
		FileUpload command = new FileUpload();
		command.setPageManager(getPageManager());
		UploadRequest properties = command.parseArguments(inReq);
		if (properties == null)
		{
			return;
		}

		//MediaArchive archive = getMediaArchive(inReq);

		String savepath = inReq.getRequestParameter("savepath");
		FileUploadItem item = properties.getFirstItem();
		
		//String savepath = "/WEB-INF/data/" + catalogid +"/origin//als/" + sourcepath;
		ContentItem contentitem = properties.saveFileAs(item, savepath, inReq.getUser());
		
	}
	
	
}
