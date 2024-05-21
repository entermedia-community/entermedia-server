package org.entermediadb.find;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultRowSorter;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.sum.SumBuilder;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.AssetUtilities;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.desktops.Desktop;
import org.entermediadb.desktops.DesktopManager;
import org.entermediadb.projects.LibraryCollection;
import org.entermediadb.projects.ProjectManager;
import org.entermediadb.projects.ProjectModule;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.cache.CacheManager;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.PathUtilities;

public class FolderManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(FolderManager.class);
	
	protected String fieldCatalogId;
	protected ModuleManager fieldModuleManager;
	protected DesktopManager fieldDesktopManager;

	public DesktopManager getDesktopManager()
	{
		if (fieldDesktopManager == null)
		{
			fieldDesktopManager = (DesktopManager) getModuleManager().getBean("desktopManager");
		}

		return fieldDesktopManager;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see model.projects.ProjectManager#setCatalogId(java.lang.String)
	 */

	public void setCatalogId(String inCatId)
	{
		fieldCatalogId = inCatId;
	}
	protected MediaArchive getMediaArchive()
	{
		MediaArchive archive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		return archive;
	}
//	public void downloadRemoteFolder(S inReq)
//	{
//		Map params = inReq.getJsonRequest();
//			
//		MediaArchive archive = getMediaArchive();
//		String categoryid = (String)params.get("categoryid");
//		String collectionid = (String)params.get("collectionid");
//		String server = (String)params.get("server");
//	
//		LibraryCollection collection = archive.getProjectManager().getLibraryCollection(archive,collectionid);
//		
//		Category cat = archive.getCategory(categoryid);
//		
//		Map assets = listAssetMap(archive, cat);
//		inReq.putPageValue("assetmap", new JSONObject(assets));
//		
//	}
	public void uploadRemoteFolder(WebPageRequest inReq)
	{
		
		Map params = inReq.getJsonRequest();
		
		String catalogid = (String)params.get("catalogid");
		MediaArchive archive = getMediaArchive();
		Map folderdetails = (Map)params.get("folderdetails");
		
		//Loop over the existing files and diff it
		String collectionid  = (String)params.get("collectionid");
		LibraryCollection collection = archive.getProjectManager().getLibraryCollection(archive, collectionid);
		String catpath = collection.getCategory().getCategoryPath();
		String subfolder = (String)folderdetails.get("subfolder");
		Category subcat = null;
		if( subfolder != null && !subfolder.isEmpty() )
		{
			catpath = catpath + subfolder;
			subcat = archive.createCategoryPath(catpath);
		}
		else
		{
			subcat = collection.getCategory();
		}
		HitTracker tracker = archive.query("asset").exact("category-exact", subcat.getId() ).search();
		Map existingassets = new HashMap(tracker.size());
		for (Iterator iterator = tracker.iterator(); iterator.hasNext();)
		{
			Data asset = (Data) iterator.next();
			existingassets.put(asset.getName(), asset);
		}
		
		Collection toupload = new ArrayList();
		Collection files = (Collection)folderdetails.get("filelist");
		log.info("Keep these files: " + files);
		for (Iterator iterator = files.iterator(); iterator.hasNext();)
		{
			Map fileinfo = (Map) iterator.next();
			String filename = (String)fileinfo.get("filename");
			long filesize = getLong(fileinfo.get("filesize"));
			Data data = (Data)existingassets.get(filename);
			existingassets.remove(filename);
			if( data == null)
			{
				data = (Asset)archive.getAssetSearcher().query().exact("name",filename ).exact("filesize", String.valueOf(filesize)).searchOne();
			}
			if( data != null)
			{
				fileinfo.put("assetid", data.getId());
			}
			
			boolean addit = false;
			if( data == null)
			{
				addit = true;
			}
			else if( filesize != -1)
			{
				Asset asset  = (Asset)archive.getAssetSearcher().loadData(data);
				ContentItem item = archive.getOriginalContent(asset);
				if( item.getLength() != filesize)
				{
					addit = true;
				} else{
					asset.addCategory(subcat);//make sure it's in the category!
					archive.saveAsset(asset);
				}
			}
			//TODO: md5?
			if( addit )
			{
				toupload.add(fileinfo);
			} 
		}
		Collection toremove = new ArrayList();
		for (Iterator iterator = existingassets.values().iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			Asset asset  = (Asset)archive.getAssetSearcher().loadData(data);
			log.info("removed old asset " + data.getName());
			asset.removeCategory(subcat);
			toremove.add(asset);
		}
		archive.getAssetSearcher().saveAllData(toremove, null);
		
		Map existingcats = new HashMap();
		for (Iterator iterator = subcat.getChildren().iterator(); iterator.hasNext();)
		{
			Category cat = (Category) iterator.next();
			existingcats.put(cat.getName(), cat);
		}
		Collection childfolders = (Collection)folderdetails.get("childfolders");
		for (Iterator iterator = childfolders.iterator(); iterator.hasNext();)
		{
			Map clientfolder = (Map) iterator.next();
			String foldername = (String)clientfolder.get("foldername");
			if( existingcats.containsKey(foldername))
			{
				existingcats.remove(foldername);
			}
			else
			{
				//add child
				Category newsub = (Category)archive.getCategorySearcher().createNewData();
				newsub.setName(foldername);
				subcat.addChild(newsub);
				archive.getCategorySearcher().saveCategory(newsub);
			}
		}
		for (Iterator iterator = existingcats.values().iterator(); iterator.hasNext();)
		{
			Category child = (Category) iterator.next();
			//TODO: Remove this form it's assets
			HitTracker existingcatassets = archive.query("asset").exact("category-exact", child.getId() ).search();
			Collection tosave = new ArrayList();
			for (Iterator iterator2 = existingcatassets.iterator(); iterator2.hasNext();)
			{
				Data data = (Data) iterator2.next();
				Asset asset = (Asset)archive.getAssetSearcher().loadData(data);
				asset.removeCategory(child);
				tosave.add(asset);
			}
			archive.getAssetSearcher().saveAllData(tosave, null);
			archive.getCategorySearcher().delete(child, null);
		}
		
		params.remove("folderdetails");
		params.put("toupload",toupload);
		log.info("Requesting to upload " + toupload);
		inReq.putPageValue("params",new JSONObject(params));
				
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
	public Map listAssetMap(MediaArchive inArchive, Category inCat)
	{
		List tosend = new ArrayList();

		HitTracker assets = inArchive.query("asset").exact("category-exact", inCat.getId()).search();
		assets.enableBulkOperations();
		for (Iterator iterator = assets.iterator(); iterator.hasNext();)
		{
			MultiValued asset = (MultiValued) iterator.next();
			Map map = new HashMap();
			map.put("id", asset.getId());

			String assetpath = inArchive.asLinkToOriginal(asset);

			String url = "/" + inArchive.getMediaDbId() + "/services/module/asset/downloads/originals/" + assetpath;
			map.put("url", url);

			String primaryImageName = asset.get("primaryfile");
			if (primaryImageName == null)
			{
				primaryImageName = asset.getName();
			}
			String savepath = inCat.getCategoryPath() + "/" + primaryImageName;
			map.put("savepath", savepath);

			map.put("filesize", asset.get("filesize"));
			long time = asset.getDate("assetmodificationdate").getTime();
			if (time > 0)
			{
				map.put("assetmodificationdate", String.valueOf(time));
			}
			tosend.add(map);
		}

		Collection<String> subfolders = new ArrayList();
		for (Iterator iterator = inCat.getChildren().iterator(); iterator.hasNext();)
		{
			Category subcat = (Category) iterator.next();
			subfolders.add(subcat.getName());
		}
		Map response = new HashMap();
		//response.put("folder", inRootCategory.getName());
		response.put("subpath", inCat.getCategoryPath());
		response.put("subfolders", subfolders);
		response.put("assets", tosend);
		return response;
		//		getDesktopListener().downloadFiles(foldername,subfolders,tosend);
		//		for (Iterator iterator = inCat.getChildren().iterator(); iterator.hasNext();)
		//		{
		//			Category child = (Category) iterator.next();
		//			downloadCat(inArchive, inCollection, child);
		//		}

	}
//	public void importCollection(WebPageRequest inReq) {
//		MediaArchive archive = getMediaArchive(inReq);
//		ProjectManager manager = getProjectManager(inReq);
//		Desktop desktop = manager.getDesktopManager().getDesktop(inReq.getUserName());
//		if( desktop.isBusy())
//		{
//			log.info("Desktop still busy");
//			return;
//		}
//
//		String collectionid = loadCollectionId(inReq);
//		User user = inReq.getUser();
//		if (user == null) 
//		{
//			throw new OpenEditException("User required ");
//		}
//
//	}

	/*
	public void retrieveFilesFromClient(WebPageRequest inReq, MediaArchive inMediaArchive, String inCollectionid)
	{
		//Data collection = inMediaArchive.getData("librarycollection",inCollectionid);
		LibraryCollection collection = getLibraryCollection(inMediaArchive, inCollectionid);
		//ContentItem childtarget = inMediaArchive.getPageManager().getRepository().getStub(inFolder);
		//utilities.exportCategoryTree(inMediaArchive, root, childtarget);

		//Send the client a download request
		Desktop desktop = getDesktopManager().getDesktop(inReq.getUserName());
		desktop.importCollection(inMediaArchive, collection); //This eventually will cause saveCheckinRequest to get called by the desktop 

	}
	*/
		
}