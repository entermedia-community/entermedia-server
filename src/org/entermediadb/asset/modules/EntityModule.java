package org.entermediadb.asset.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.fetch.YoutubeImporter;
import org.entermediadb.asset.fetch.YoutubeMetadataSnippet;
import org.entermediadb.asset.fetch.YoutubeParser;
import org.entermediadb.asset.importer.CsvImporter;
import org.entermediadb.asset.importer.XlsImporter;
import org.entermediadb.asset.upload.FileUpload;
import org.entermediadb.asset.upload.FileUploadItem;
import org.entermediadb.asset.upload.UploadRequest;
import org.entermediadb.asset.util.Row;
import org.entermediadb.data.AddedPermission;
import org.entermediadb.find.EntityManager;
import org.entermediadb.projects.LibraryCollection;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.BaseData;
import org.openedit.data.CompositeData;
import org.openedit.data.PropertyDetail;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.data.ValuesMap;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.util.PathUtilities;

public class EntityModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(EntityModule.class);

	public void addEntityToAsset(WebPageRequest inPageRequest) throws Exception 
	{
		
		String entityid = inPageRequest.getRequestParameter("entityid");
		String moduleid = inPageRequest.getRequestParameter("moduleid");
		MediaArchive archive = getMediaArchive(inPageRequest);
		if (entityid == null || moduleid == null) 
		{
			return;
		}
		
		// boolean moveentity = Boolean.parseBoolean( inPageRequest.getRequestParameter("moveasset") );
			
		Asset asset = getAsset(inPageRequest);
		if (asset == null) {
			log.error("No asset id passed in");
			return;
		}

		// if( moveentity )
		// {
		// 	//remove from the other entity
		// }
		Boolean saved = false;
		//Use standard in CategoryEditModule?
		if (moduleid.equals("librarycollection")) {
			//rootcategoryid passed
			String rootcategory = inPageRequest.getRequestParameter("rootcategory");
			Category c = archive.getCategory(rootcategory);
			if (c != null) {
				asset.addCategory(c);
				saved = true;
			}
		}
		else if(moduleid.equals("faceprofilegroup")) {
			List<ValuesMap> otherprofiles = createListMap((Collection)asset.getValue("faceprofiles"));
			Boolean alreadyinprofile = false;
			for (int i = 0; i < otherprofiles.size(); i++) {
				ValuesMap profilegroups = (ValuesMap)otherprofiles.get(i);
				if( profilegroups.containsInValues("faceprofilegroup",entityid) ) {
					alreadyinprofile = true;
				}
			}
			if (!alreadyinprofile) {
				List<Map> profilemap = new ArrayList<Map>();
				Map profile = new HashMap();
				profile.put("faceprofilegroup", entityid);
				otherprofiles.add(new ValuesMap(profile));
				asset.setValue("faceprofiles", otherprofiles);
				saved = true;
			}
		}
		else {
			//Defualt entity
			//asset.addValue(moduleid, entityid);
			String categoryid = inPageRequest.getRequestParameter("categoryid");
			Category c = archive.getCategory(categoryid);
			if (c != null) {
				asset.addCategory(c);
				saved = true;
			}
			saved = true;
		}

		if (saved) {
			archive.saveAsset(asset, inPageRequest.getUser());
			//Assign primaryimage if not exists
			Data entity = archive.getData(moduleid, entityid);
			if (entity != null) {
				if (entity.get("primaryimage") == null) {
					entity.setValue("primaryimage", asset.getId());
					Searcher searcher = archive.getSearcher(moduleid);
					searcher.saveData(entity);
				}
			}
			archive.fireMediaEvent("saved", inPageRequest.getUser(), asset);
			inPageRequest.putPageValue("added" , "1");
		}
	}
	
	
	public void addAssetsToEntity(WebPageRequest inPageRequest) throws Exception 
	{
//		data-copyingsearchtype="$copyingsearchtype"
//		data-copyinghitssessionid="$copyinghitssessionid"

		MediaArchive archive = getMediaArchive(inPageRequest);
		EntityManager entityManager = getEntityManager(inPageRequest);
		
		String pickedmoduleid = inPageRequest.findPathValue("module");
		String pickedentityid = inPageRequest.getRequestParameter("dataid");
		if (pickedentityid == null)
		{
			pickedentityid = inPageRequest.getRequestParameter("id");
		}
		if (pickedmoduleid == null || pickedentityid == null)
		{
			return;
		}
		Data entity = archive.getCachedData(pickedmoduleid ,pickedentityid );
		
		if (entity == null)
		{
			return;
		}
		
		String pickedassetid = inPageRequest.getRequestParameter("assetid");
		if( pickedassetid == null)
		{
			pickedassetid = inPageRequest.getRequestParameter("pickedassetid");
		}
		if( pickedassetid != null && !pickedassetid.startsWith("multiedit:"))
		{
			Asset asset = archive.getAsset(pickedassetid);
			if(entityManager.addAssetToEntity(inPageRequest.getUser(), pickedmoduleid, pickedentityid, asset))
			{
				entityManager.fireAssetAddedToEntity(null, inPageRequest.getUser(),asset, entity);
			}
			inPageRequest.putPageValue("asset", asset);
		}
		else
		{
				Collection assets = findPickedAssets(inPageRequest, pickedassetid);
				try
				{
					List<Data> tosave = new ArrayList();
					Integer count = 0;
					for (Iterator iterator = assets.iterator(); iterator.hasNext();)
					{
						Asset asset = (Asset)archive.getAssetSearcher().loadData((Data)iterator.next());
						if (asset == null) {
							log.error("No asset id passed in");
							return;
						}
						if(entityManager.addAssetToEntity(inPageRequest.getUser(), pickedmoduleid, pickedentityid, asset))
						{
							tosave.add(asset);
							if( tosave.size() > 100)
							{
								archive.getAssetSearcher().saveAllData(tosave, inPageRequest.getUser());
								tosave.clear();
							}
							count = count +1;
						}
					}
					if( tosave.size() > 0)
					{
						archive.getAssetSearcher().saveAllData(tosave, inPageRequest.getUser());
						entityManager.fireAssetsAddedToEntity(null, inPageRequest.getUser(),tosave, entity);
						inPageRequest.putPageValue("assets", tosave);
					}
					log.info("Added to entity: " + count + " assets.");
				}
				catch (Exception e)
				{
					//continue;
					log.error("Cant save, ",e);
				}
			}
	}
	
	private Collection findPickedAssets(WebPageRequest inPageRequest, String pickedassetid) 
	{
	
		List<Data> found =  new ArrayList();;
		if (pickedassetid != null && pickedassetid.startsWith("multiedit:"))
		{
			CompositeData assets  = (CompositeData) inPageRequest.getSessionValue(pickedassetid);
			for (Iterator iterator = assets.iterator(); iterator.hasNext();)
			{
				Data asset = (Data) iterator.next();
				found.add(asset);
			}
		}
		else
		{
			String copyinghitssessionid = inPageRequest.getRequestParameter("copyinghitssessionid");
			HitTracker tracker = (HitTracker)inPageRequest.getSessionValue(copyinghitssessionid);
			if( tracker != null)
			{
				//found.addAll(tracker.getSelectedHitracker())
				HitTracker selected = tracker.getSelectedHitracker();
				for (Iterator iterator = selected.iterator(); iterator.hasNext();)
				{
					Data asset = (Data) iterator.next();
					found.add(asset);
				}
			}
		}
		return found;
	}


	public void addCategoryToEntity(WebPageRequest inPageRequest) throws Exception 
	{
		MediaArchive archive = getMediaArchive(inPageRequest);
		EntityManager entityManager = getEntityManager(inPageRequest);
		
		String pickedmoduleid = inPageRequest.findPathValue("module");
		String pickedentityid = inPageRequest.getRequestParameter("id");
		
		if(pickedentityid == null)
		{
			Data data = (Data)inPageRequest.getPageValue("data");
			if(data != null)
			{
				pickedentityid = data.getId();
			}
		}
		
		if(pickedentityid == null)
		{
			throw new OpenEditException("Missing entity id");
		}
		
		String copyingcategoryid = inPageRequest.getRequestParameter("copyingcategoryid");
		if(copyingcategoryid != null) {
			if(entityManager.addCategoryToEntity(inPageRequest.getUser(), pickedmoduleid, pickedentityid, copyingcategoryid))
			{
				inPageRequest.putPageValue("categories", "1");
			}
		}
	}
	
	public void addToSearchCategory(WebPageRequest inPageRequest) throws Exception 
	{
	
		MediaArchive archive = getMediaArchive(inPageRequest);
		EntityManager entityManager = getEntityManager(inPageRequest);
		
		//String sourcemoduleid = inPageRequest.getRequestParameter("copyingsearchtype");
		String id = inPageRequest.getRequestParameter("id");
		String hitssessionid = inPageRequest.getRequestParameter("copyinghitssessionid");
		String entitymoduleid = inPageRequest.getRequestParameter("entitymoduleid");
		HitTracker hits = (HitTracker) inPageRequest.getSessionValue(hitssessionid);
		log.info("Saving:" + hits.getSearcher().getSearchType() + " " + hits.getSelectionSize()  + " hash " + hits.hashCode());
		Integer added = entityManager.addToSearchCategory(inPageRequest, entitymoduleid, hits, id);
		inPageRequest.putPageValue("saved", added);
	}
	
	public void copyEntities(WebPageRequest inPageRequest) throws Exception 
	{
		MediaArchive archive = getMediaArchive(inPageRequest);	
		EntityManager entityManager = getEntityManager(inPageRequest);
		
		String sourcemoduleid = inPageRequest.getRequestParameter("copyingsearchtype");
		String sourceentityid = inPageRequest.getRequestParameter("copyingentityid");
		String entitymoduleid = inPageRequest.getRequestParameter("entitymoduleid");
		String hitssessionid = inPageRequest.getRequestParameter("copyinghitssessionid");
		HitTracker hits = (HitTracker) inPageRequest.getSessionValue(hitssessionid);
		Data newentity = null;
		if(hits == null) {
			newentity = entityManager.copyEntity(inPageRequest, sourcemoduleid, entitymoduleid, sourceentityid);
			inPageRequest.putPageValue("saveddata", newentity);
			inPageRequest.putPageValue("saved", "1");
		}
		else {
			Collection saved = entityManager.copyEntities(inPageRequest, sourcemoduleid, entitymoduleid, hits);
			inPageRequest.putPageValue("saved", saved.size());
		}

		inPageRequest.putPageValue("moduleid", sourcemoduleid);
		
		
		String action = inPageRequest.getRequestParameter("action");
		
		if("moveentity".equals(action)) {
			Data entitymodule = archive.getCachedData("module", entitymoduleid);
			
			Boolean deleted = entityManager.deleteEntity(inPageRequest, sourcemoduleid, sourceentityid);
			inPageRequest.putPageValue("entitymodule", entitymodule);
			inPageRequest.putPageValue("entity", newentity);
			inPageRequest.putPageValue("deleted", deleted);
		}
		
	}
	
	
	public void copyCategoriesAndFilesToEntity(WebPageRequest inReq) throws Exception 
	{
		MediaArchive archive = getMediaArchive(inReq);	
		EntityManager entityManager = getEntityManager(inReq);
		
		String moduleid = inReq.getRequestParameter("moduleid");
		String entityid = inReq.getRequestParameter("entityid");
		
		Data module = archive.getCachedData("module", moduleid);
		Data entity = archive.getCachedData(moduleid, entityid);
		
		
		String destinationcategorypath = inReq.getRequestParameter("destinationcategorypath");
		Category destinationCategory = null;
		if(destinationcategorypath!= null)
		{
			destinationCategory = archive.getCategorySearcher().createCategoryPath(destinationcategorypath);
		}
		else {
			destinationCategory = entityManager.loadDefaultFolder(module, entity, inReq.getUser());
		}
		
		String[] categoryid = inReq.getRequestParameters("categoryid");
		if (categoryid != null)
		{
			for (int i = 0; i < categoryid.length; i++)
			{
				entityManager.addCategoryToEntity(inReq.getUser(), module, entity, categoryid[i], destinationCategory);
			}
		}
		
		String[] assetids = inReq.getRequestParameters("assetid");
		if (assetids != null)
		{
			for (int i = 0; i < assetids.length; i++)
			{
				Asset asset = archive.getAsset(assetids[i]);
				entityManager.addAssetToEntity(inReq.getUser(), module, entity, asset, destinationCategory);
			}
		}
		
	}
	
	public void removeAssetsFromEntity(WebPageRequest inPageRequest) throws Exception 
	{
	
		MediaArchive archive = getMediaArchive(inPageRequest);
		EntityManager entityManager = getEntityManager(inPageRequest);
		
		String entitymoduleid = inPageRequest.getRequestParameter("entitymoduleid");
		String entityid = inPageRequest.getRequestParameter("entityid");
		String assetid = inPageRequest.getRequestParameter("assetid");
		
		if(assetid != null) {
			if(entityManager.removeAssetFromEntity(inPageRequest.getUser(), entitymoduleid, entityid, assetid))
			{
				inPageRequest.putPageValue("assets", "1");
			}
		}
		else 
		{
			String assethitssessionid = inPageRequest.getRequestParameter("removinghitssessionid");
			HitTracker assethits = (HitTracker) inPageRequest.getSessionValue(assethitssessionid);
			//Collection<String> ids = assethits.getSelectedHitracker().collectValues("id");
			Integer removed = entityManager.removeAssetsFromEntity(inPageRequest.getUser(), entitymoduleid, entityid, assethits);
			inPageRequest.putPageValue("assets", removed);
			
		}
		inPageRequest.putPageValue("entityid", entityid);
		inPageRequest.putPageValue("assetclearselection", true);
	}
	
	public void removeOneToMany(WebPageRequest inPageRequest) throws Exception 
	{
		MediaArchive archive = getMediaArchive(inPageRequest);
		EntityManager entityManager = getEntityManager(inPageRequest);
		
		String moduleid = inPageRequest.getRequestParameter("moduleid");
		
		String entitymoduleid = inPageRequest.getRequestParameter("entitymoduleid");
		String entityid = inPageRequest.getRequestParameter("entityid");
		
		String removinghitssessionid = inPageRequest.getRequestParameter("removinghitssessionid");
		HitTracker removinghits = (HitTracker) inPageRequest.getSessionValue(removinghitssessionid);
		
		if(removinghits!= null && entityid != null && entitymoduleid != null) {
			Integer removed = entityManager.removeRecordsFromEntity(inPageRequest.getUser(), entitymoduleid, entityid, removinghits);
			inPageRequest.putPageValue("removed", removed);
		}


		
	}
	
	protected EntityManager getEntityManager(WebPageRequest inPageRequest) 
	{
		String catalogid = inPageRequest.findValue("catalogid");
		EntityManager entity = (EntityManager) getModuleManager().getBean(catalogid, "entityManager");
		return entity;
	}


	private List<ValuesMap> createListMap(Collection inValues)
	{
		ArrayList copy = new ArrayList();
		if (inValues != null) 
		{
			for (Iterator iterator = inValues.iterator(); iterator.hasNext();)
			{
				Map map = (Map) iterator.next();
				copy.add(new ValuesMap(map));
			}
		}
		return copy;
	}
	
	public void makePrimaryImageEntity(WebPageRequest inPageRequest) throws Exception 
	{
		
		String entityid = inPageRequest.getRequestParameter("entityid");
		String moduleid = inPageRequest.getRequestParameter("moduleid");
		String assetid = inPageRequest.getRequestParameter("assetid");
		MediaArchive archive = getMediaArchive(inPageRequest);
		if (entityid == null || moduleid == null || assetid == null) 
		{
			return;
		}
		
		Data entity = archive.getData(moduleid, entityid);
		if (entity != null) {
				entity.setValue("primaryimage", assetid);
				Searcher searcher = archive.getSearcher(moduleid);
				searcher.saveData(entity);
		}
	}

	public Data loadSelectedEntity(WebPageRequest inPageRequest) throws Exception 
	{
		
		String entityid = inPageRequest.getRequestParameter("entityid");
		if(entityid == null) {
			//get it from URL
			entityid = PathUtilities.extractDirectoryName(inPageRequest.getPath());
		}
		inPageRequest.putPageValue("selectedentityid", entityid);
		String moduleid = inPageRequest.getRequestParameter("entitytype");
		if(moduleid == null) {
			moduleid = inPageRequest.findValue("module");
		}
		if( entityid == null || moduleid == null)
		{
			return null;
		}
		MediaArchive archive = getMediaArchive(inPageRequest);
		Data entity = archive.getCachedData(moduleid, entityid);
		
		inPageRequest.putPageValue("selectedentity", entity);
		//inPageRequest.putPageValue(entityid+"sortby", entity);
		
		
		return entity;
	}
	
	
	/**
	 * This is called after new assets are uploaded.
	 * It is also called when assets are added to one specific category
	 * @param inPageRequest
	 * @throws Exception
	 */
	
	public void handleAssetsImported(WebPageRequest inPageRequest) throws Exception
	{
		MediaArchive archive = getMediaArchive(inPageRequest);
		String appid = inPageRequest.findValue("applicationid");
		
		//TODO: Get the actual category added
		
		//Search the hits for category
		Collection<Asset> assets = (Collection<Asset>)inPageRequest.getPageValue("hits"); 
		if( assets == null)
		{
			String[] assetids = inPageRequest.getRequestParameters("assetids");
			if( assetids == null)
			{
				//log.info("No assets ");
				return;
			}
			assets = archive.query("asset").ids(assetids).search();
		}
		
		Map<String,Data> foundentites = new HashMap();
		Map<String,Collection> foundentitesassets = new HashMap();

		for (Iterator iterator = assets.iterator(); iterator.hasNext();) 
		{
			Data data = (Data) iterator.next();
			
			Asset asset = archive.getAsset(data.getId()); //Is this needed?
			
			Collection<Data> entities = archive.getEntityManager().getEntitiesForCategories(inPageRequest, asset.getCategories());
			for (Iterator iterator2 = entities.iterator(); iterator2.hasNext();) {
				//Asset asset2 = (Asset) iterator2.next();
				Data entity = (Data) iterator2.next();
				
				Collection<Data> assetstoentity = foundentitesassets.get(entity.getId());
				if( assetstoentity == null)
				{
					assetstoentity = new ArrayList();
					foundentites.put(entity.getId(),entity);
					foundentitesassets.put(entity.getId(),assetstoentity);
				}
				assetstoentity.add(asset);
			}
		}
		for (Iterator iterator = foundentites.keySet().iterator(); iterator.hasNext();)
		{
			String entityid = (String) iterator.next();
			Data entity = foundentites.get(entityid);
			Collection<Data> bulkassets = foundentitesassets.get(entityid);
			archive.getEntityManager().fireAssetsAddedToEntity(appid, inPageRequest.getUser(),bulkassets, entity);
		}
		
	}
	public void handleAssetModified(WebPageRequest inPageRequest) throws Exception
	{
		MediaArchive archive = getMediaArchive(inPageRequest);
		String appid = inPageRequest.findValue("applicationid");
		
		String assetid = inPageRequest.getRequestParameter("assetid");
		
		//Search the hits for category
		if( assetid == null)
		{
			log.error("Invalid assetid ");
			return;
		}
		Asset asset = archive.getAsset(assetid);
		
		Collection assets = new ArrayList();
		assets.add(asset);
		
		Collection<Data> entities = archive.getEntityManager().getEntitiesForCategories(inPageRequest, asset.getCategories());
		String applicationid = inPageRequest.findPathValue("applicationid");	
		
		
		for (Iterator iterator = entities.iterator(); iterator.hasNext();)
		{
			Data entity = (Data)iterator.next();
			//Log event
			archive.getEntityManager().saveAssetActivity(applicationid, inPageRequest.getUser(), entity, assets, "assetsmodified");
		}
		
	}
	
	/**
	 * 
	 * This sould be called from the category edit operations?
	 * @param inPageRequest
	 * @throws Exception
	 */
	
	public void handleAssetsAddedToCategory(WebPageRequest inPageRequest) throws Exception
	{
		MediaArchive archive = getMediaArchive(inPageRequest);
		String appid = inPageRequest.findValue("applicationid");
		
		//TODO: Get the actual category added
		
		//Search the hits for category
		Collection<Asset> assets = (Collection<Asset>)inPageRequest.getPageValue("hits"); 
		if( assets == null)
		{
			String[] assetids = inPageRequest.getRequestParameters("assetids");
			if( assetids == null)
			{
				log.info("No assets ");
				return;
			}
			assets = archive.query("asset").ids(assetids).search();
		}

		Map<String,Collection> foundentitesassets = new HashMap();

		Map<String, Data> foundentites = findEntityAssets(inPageRequest, archive, assets, foundentitesassets);
		for (Iterator iterator = foundentites.keySet().iterator(); iterator.hasNext();)
		{
			String entityid = (String) iterator.next();
			Data entity = foundentites.get(entityid);
			Collection<Data> bulkassets = foundentitesassets.get(entityid);
			archive.getEntityManager().fireAssetsAddedToEntity(appid, inPageRequest.getUser(),bulkassets, entity);
		}
		
	}


	protected Map<String, Data> findEntityAssets(WebPageRequest inPageRequest,  MediaArchive archive, Collection<Asset> assets,
			Map<String, Collection> foundentitesassets) {
		Map<String,Data> foundentites = new HashMap();

		for (Iterator iterator = assets.iterator(); iterator.hasNext();) 
		{
			Asset asset = (Asset) iterator.next();
			
			Collection<Data> entities = archive.getEntityManager().getEntitiesForCategories(inPageRequest, asset.getCategories());
			for (Iterator iterator2 = entities.iterator(); iterator2.hasNext();) {
				//Asset asset2 = (Asset) iterator2.next();
				Data entity = (Data) iterator2.next();
				
				Collection<Data> assetstoentity = foundentitesassets.get(entity.getId());
				if( assetstoentity == null)
				{
					assetstoentity = new ArrayList();
					foundentites.put(entity.getId(),entity);
					foundentitesassets.put(entity.getId(),assetstoentity);
				}
				assetstoentity.add(asset);
			}
		}
		return foundentites;
	}
	public void handleAssetsRemovedFromCategory(WebPageRequest inPageRequest) throws Exception
	{
		//Search the hits for category
		 //* It is also called when assets are added to one specific category

	}
	
	public void scanForNewEntitiesNames(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);

		String moduleid = inReq.getRequestParameter("moduleid");

		String[] existingfoldernames = inReq.getRequestParameters("name");
		List all = Arrays.asList(existingfoldernames);
		HitTracker existing = archive.getSearcher(moduleid).query().orgroup("name",all).search();
		
		Set newfolders = new HashSet(all);
		for (Iterator iterator = existing.iterator(); iterator.hasNext();) {
			Data found = (Data) iterator.next();
			newfolders.remove(found.getName());
		}
		inReq.putPageValue("existingfolders",existing);
		inReq.putPageValue("newfolders",newfolders);
	}
	
//	public void updateLocalSyncFolders(WebPageRequest inReq) throws Exception
//	{
//		MediaArchive archive = getMediaArchive(inReq);
//
//		String[] ids = inReq.getRequestParameters("id");
//
//		Searcher searcher = archive.getSearcher("desktopsyncfolder");
//		Collection folders = searcher.query().ids(ids).sort("name").search();
//		
//		List tosave = new ArrayList();
//		
//		for (Iterator iterator = folders.iterator(); iterator.hasNext();) {
//			Data folder = (Data) iterator.next();
//			String localitemcount = inReq.getRequestParameter(folder.getId() + ".localitemcount");
//			folder.setValue("localitemcount",localitemcount);
//			String localsubfoldercount = inReq.getRequestParameter(folder.getId() + ".localsubfoldercount");
//			folder.setValue("localsubfoldercount",localsubfoldercount);
//			String localtotalsize = inReq.getRequestParameter(folder.getId() + ".localtotalsize");
//			folder.setValue("localtotalsize",localtotalsize);
//			
//			//TODO: add status field & lastscanned
//			
//			tosave.add(folder);
//		}
//		searcher.saveAllData(tosave, null);
//		inReq.putPageValue("syncfolders",tosave);
//	}
	
	public void startDesktopSync(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		Searcher searcher = archive.getSearcher("desktopsyncfolder");
		
		String desktop = inReq.getRequestParameter("desktop");
		String startcategory = inReq.getRequestParameter("startcategory");
		
		QueryBuilder query = searcher.query().exact("desktop", desktop).exact("startcategory", startcategory);
		
		Data syncfolder = (Data) query.searchOne();
		
		if(syncfolder == null)
		{
			syncfolder = archive.getSearcher("desktopsyncfolder").createNewData();
			syncfolder.setValue("startcategory", startcategory);
		}
		
		String entityid = inReq.getRequestParameter("entityid");
		syncfolder.setValue("entityid", entityid);
		
		String entitymoduleid = inReq.getRequestParameter("entitymoduleid");
		syncfolder.setValue("entitymoduleid", entitymoduleid);
		
		String categorypath = inReq.getRequestParameter("categorypath");
		syncfolder.setValue("categorypath", categorypath);
		syncfolder.setValue("currentcategorypath", categorypath);
		 
		syncfolder.setValue("currentfoldertotalsize", 0); 
		syncfolder.setValue("currentfoldertotalcount", 0); 
		
		syncfolder.setValue("desktopimportstatus", "scan-started"); 
		
		Boolean isdownload = Boolean.parseBoolean(inReq.getRequestParameter("isdownload"));
		syncfolder.setValue("isdownload", isdownload);
		
		String namebreadcrumb = categorypath.replace("/", " &rsaquo; ");
		syncfolder.setName(namebreadcrumb);

		syncfolder.setValue("desktop", desktop);
		
		syncfolder.setValue("completeddate", null);
		syncfolder.setValue("createddate", new Date());
		

		searcher.saveData(syncfolder);

		inReq.putPageValue("syncfolder", syncfolder);
		inReq.putPageValue("searcher", searcher);
	}
	
	public void restartDesktopSync(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		Searcher searcher = archive.getSearcher("desktopsyncfolder");
		
		String syncfolderid = (String) inReq.getRequestParameter("syncfolderid");
		
		MultiValued syncfolder = (MultiValued) archive.getCachedData("desktopsyncfolder", syncfolderid);

		String desktop = (String) inReq.getRequestParameter("desktop");
		syncfolder.setValue("desktop", desktop);
		
		syncfolder.setValue("desktopimportstatus", "scan-started");
		
		String categorypath = syncfolder.get("categorypath");
		syncfolder.setValue("currentcategorypath", categorypath);
		
		syncfolder.setValue("currentfoldertotalsize", 0);
		syncfolder.setValue("currentfoldertotalcount", 0);
		
		syncfolder.setValue("createddate", new Date());
		
		searcher.saveData(syncfolder);
		
		inReq.putPageValue("syncfolder", syncfolder);
		inReq.putPageValue("searcher", searcher);
	}

	public void completeDesktopSync(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		Searcher searcher = archive.getSearcher("desktopsyncfolder");

		Map params = inReq.getJsonRequest();
		String syncfolderid = (String) params.get("syncfolderid");
		
		MultiValued syncfolder = (MultiValued) archive.getCachedData("desktopsyncfolder", syncfolderid);
		
		if(syncfolder == null)
		{
			log.info("No syncfolder found with id: " + syncfolderid);
			return;
		}

		syncfolder.setValue("desktopimportstatus", "sync-completed");
		
		syncfolder.setValue("completeddate", new Date());
		
		searcher.saveData(syncfolder);
		
		inReq.putPageValue("syncfolder", syncfolder);
		inReq.putPageValue("searcher", searcher);
	}
	
	public void cancelDesktopSync(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		Searcher searcher = archive.getSearcher("desktopsyncfolder");

		Map params = inReq.getJsonRequest();
		String syncfolderid = (String) params.get("syncfolderid");
		
		MultiValued syncfolder = (MultiValued) archive.getCachedData("desktopsyncfolder", syncfolderid);
		
		searcher.delete(syncfolder, inReq.getUser());
	}
	
//	public synchronized void updateScanStatus(WebPageRequest inReq) throws Exception
//	{
//		MediaArchive archive = getMediaArchive(inReq);
//		Searcher searcher = archive.getSearcher("desktopsyncfolder");
//
//		String desktop = inReq.getRequestParameter("desktop");
//		String categorypath = inReq.getRequestParameter("categorypath");
//		if(desktop != null && categorypath != null)
//		{
//			categorypath = categorypath.replace("\\", "/");
//			categorypath = categorypath.replaceAll("/+", "/");
//			categorypath = categorypath.replaceAll("/$", "");
//
//			String desktopimportstatus = inReq.getRequestParameter("desktopimportstatus");
//
//			if(desktopimportstatus != null) 
//			{				
//				Boolean isdownload = Boolean.parseBoolean(inReq.getRequestParameter("isdownload"));
//				QueryBuilder query = searcher.query().exact("desktop", desktop).exact("categorypath", categorypath).exact("isdownload", isdownload);
//				
//				Data folder = (Data) query.searchOne();
//				
//				if(folder == null && !"sync-cancelled".equals(desktopimportstatus))
//				{
//					String entityid = inReq.getRequestParameter("entityid");
//					String moduleid = inReq.getRequestParameter("moduleid");
//					
//					Data entity = archive.getData(moduleid, entityid);
//					
//					folder = archive.getSearcher("desktopsyncfolder").createNewData();
//					
//					folder.setValue("categorypath", categorypath);
//					
//					folder.setValue("isdownload", isdownload);
//					
//					String namebreadcrumb = categorypath.replace("/", " &rsaquo; ");
//					folder.setName(namebreadcrumb);
//					folder.setValue("desktop",desktop);
//					folder.setValue("module",moduleid);
//					if(entity != null)
//					{						
//						folder.setValue("entityid", entity.getId());
//					}
//					folder.setValue("createddate", new Date());
//					archive.saveData("desktopsyncfolder", folder);
//				}
//				
//				if(folder != null)
//				{					
//					String completedfiles = inReq.getRequestParameter("completedfiles");
//					if(completedfiles != null)
//					{			
//						folder.setValue("completedfiles", completedfiles);
//					}
//					
//					String failedfiles = inReq.getRequestParameter("failedfiles");
//					if(failedfiles != null)
//					{
//						folder.setValue("failedfiles", failedfiles);
//					}
//					
//					
//					folder.setValue("desktopimportstatus", desktopimportstatus);
//					if(desktopimportstatus.equals("scan-started"))
//					{
//						folder.setValue("completeddate", new Date());
//					}
//					
//					searcher.saveData(folder, null);
//				}
//			}
//		}
//		Collection syncfolders = searcher.query().exact("desktop", desktop).sort("completeddateDown").search();
//		inReq.putPageValue("syncfolders", syncfolders);
//	}
	
	public void createEntitiesForFolders(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);

		String moduleid = inReq.getRequestParameter("moduleid");
		Data tmpdata = archive.getSearcher(moduleid).createNewData(); //tmp
		tmpdata.setValue("entitysourcetype", moduleid);

		String[] existingfoldernames = inReq.getRequestParameters("name");
		String[] fields = inReq.getRequestParameters("field");
		if (fields != null) {
			for (int i = 0; i < fields.length; i++) {
				String[] values = inReq.getRequestParameters(fields[i] + ".value");
				tmpdata.setValue(fields[i],values);
			}
		}
		List all = Arrays.asList(existingfoldernames);
		if(all.isEmpty()) {
			log.info("No folders selected");
			return;
		}
		HitTracker existing = archive.getSearcher(moduleid).query().orgroup("name",all).search();
		
		Set newfolders = new HashSet(all);
		for (Iterator iterator = existing.iterator(); iterator.hasNext();) {
			Data found = (Data) iterator.next();
			newfolders.remove(found.getName());
		}
		List tosave = new ArrayList();
		Searcher entitysearcher = archive.getSearcher(moduleid);
		for (Iterator iterator = newfolders.iterator(); iterator.hasNext();) {
			String name = (String) iterator.next();
			Data entity = ((BaseData)tmpdata).copy();
			entity.setName(name);
			archive.getEntityManager().createDefaultFolder(entity, inReq.getUser());
			
			tosave.add(entity);
			
		}
		entitysearcher.saveAllData(tosave, null);
		HitTracker existingconfirmed = archive.getSearcher(moduleid).query().orgroup("name",all).search();

		inReq.putPageValue("existingfolders",existingconfirmed);
	}

	public void assetDeletingHandler(WebPageRequest inReq)
	{
		//Look for entities
		MediaArchive archive = getMediaArchive(inReq);
		String id = inReq.getRequestParameter("dataid"); //From the event
		Asset deleted = archive.getAsset(id);
		String appid = inReq.findPathValue("applicationid");
		Collection entities = archive.getEntityManager().getEntitiesForCategories(inReq, deleted.getCategories());
		for (Iterator iterator = entities.iterator(); iterator.hasNext();) {
			Data entity = (Data) iterator.next();
			archive.getEntityManager().fireAssetRemovedFromEntity(appid, inReq.getUser(), deleted, entity);
		}
		
	}
	//Bulk delete
	public void assetsDeleteingHandler(WebPageRequest inReq)
	{
		String appid = inReq.findPathValue("applicationid");
		//Look for entities
		MediaArchive archive = getMediaArchive(inReq);
		String[] assetids = inReq.getRequestParameters("assetids");
		
		//Nullcheck?
		
		Map<String,Data> foundentites = new HashMap();
		Map<String,Collection> foundentitesassets = new HashMap();

		for (int i = 0; i < assetids.length; i++) {
			String assetid = assetids[i];
			Asset deleted = archive.getAsset(assetid);
			Collection entities = archive.getEntityManager().getEntitiesForCategories(inReq, deleted.getCategories());
			for (Iterator iterator2 = entities.iterator(); iterator2.hasNext();) {
				Data entity = (Data) iterator2.next();
				Collection<Data> assetstoentity = foundentitesassets.get(entity.getId());
				if( assetstoentity == null)
				{
					assetstoentity = new ArrayList();
					foundentites.put(entity.getId(),entity);
					foundentitesassets.put(entity.getId(),assetstoentity);
				}
				assetstoentity.add(deleted);
			}
		}
		
		for (Iterator iterator = foundentites.keySet().iterator(); iterator.hasNext();)
		{
			String entityid = (String) iterator.next();
			Data entity = foundentites.get(entityid);
			Collection<Data> bulkassets = foundentitesassets.get(entityid);
			archive.getEntityManager().fireAssetsRemovedFromEntity(appid, inReq.getUser(),bulkassets, entity);
		}
		
	}
	
	/*
	public void createEntitiesForFolders(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);

		String moduleid = inReq.getRequestParameter("moduleid");
		Data tmpdata = archive.getSearcher(moduleid).createNewData(); //tmp
		tmpdata.setValue("entitysourcetype",moduleid);
		String[] existingfolders = inReq.getRequestParameters("name");
		String[] fields = inReq.getRequestParameters("field");
		for (int i = 0; i < fields.length; i++) {
			String[] values = inReq.getRequestParameters(fields + ".value");
			tmpdata.setValue(fields[i],values);
		}
		List tosave = new ArrayList();
		for (int i = 0; i < existingfolders.length; i++) {
			Data newfolder = ((BaseData)tmpdata).copy();
			tmpdata.setName(existingfolders[i]);
			archive.getEntityManager().loadDefaultFolder(tmpdata, inReq.getUser());  //This saves it
			tosave.add(newfolder);
			
		}
		archive.getSearcher(moduleid).saveAllData(tosave, null);
		inReq.putPageValue("existingfolders",tosave);
		
	}
	*/
	
	public void createEntityForLocalFolder(WebPageRequest inReq) throws Exception
	{	
		MediaArchive archive = getMediaArchive(inReq);
		String moduleid = inReq.getRequestParameter("module");
		Data module = archive.getCachedData("module", moduleid);
		String desktopid = inReq.getRequestParameter("desktop");
		
		String[] fields = inReq.getRequestParameters("field");
		
		String[] localpaths = inReq.getRequestParameters("localpath");
		String[] names = inReq.getRequestParameters("name.value");
		String[] localsubfoldercounts = inReq.getRequestParameters("localsubfoldercount");
		String[] localitemcounts = inReq.getRequestParameters("localitemcount");
		String[] localtotalsizes = inReq.getRequestParameters("localtotalsize");
		
		Data[] folders = new Data[localpaths.length];
		for (int i = 0; i < localpaths.length; i++) {
			
			Data folder = archive.query("desktopsyncfolder").exact("name", names[i]).exact("desktop", desktopid).searchOne();
			if( folder != null)
			{
				log.info("Existing folder found. Skipping");
				return;
			}
			folder = archive.getSearcher("desktopsyncfolder").createNewData(); //tmp
			folder.setValue("desktop",desktopid);
			folder.setValue("module",moduleid);
			folder.setValue("localpath",localpaths[i]);
			folder.setValue("localsubfoldercount",localsubfoldercounts[i]);
			folder.setValue("localitemcount",localitemcounts[i]);
			folder.setValue("localtotalsize",localtotalsizes[i]);
			folder.setValue("completeddate", new Date());
			folder.setName(names[i]);
			
			Data tmpentity = archive.getSearcher(moduleid).createNewData(); //tmp
			archive.getSearcher(moduleid).updateData(inReq, fields, tmpentity);
			tmpentity.setValue("entitysourcetype", moduleid);
			tmpentity.setName(names[i]);
			
			String path = archive.getEntityManager().loadUploadSourcepath(module,tmpentity,inReq.getUser(), true);
			
			Data existing = archive.query(moduleid).exact("sourcepath", path).searchOne();
			
			if( existing == null)
			{
				log.info("Existing folder found. Skipping");
				archive.getEntityManager().createDefaultFolder(tmpentity, inReq.getUser());
				archive.saveData(moduleid,tmpentity);
			}
			else
			{
				tmpentity = existing;
			}
			
			folder.setValue("categorypath",tmpentity.getValue("sourcepath"));
			folder.setValue("entityid",tmpentity.getId());
			
			archive.saveData("desktopsyncfolder",folder);
			
			folders[i] = folder;
		}
		
		inReq.putPageValue("newFolders", folders);
		inReq.putPageValue("verifynow", true);
		inReq.putPageValue("status", "OK");
	}
	
	public void createSyncFolderForEntity(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		String entityid = inReq.getRequestParameter("entityid");
		String moduleid = inReq.getRequestParameter("moduleid");
		String desktopid = inReq.getRequestParameter("desktop");
		String categorypath = inReq.getRequestParameter("categorypath");
		
		Data entity = archive.getData(moduleid, entityid);
		
		Data folder = archive.query("desktopsyncfolder").exact("categorypath", categorypath).exact("desktop", desktopid).searchOne();
		if( folder == null)
		{
			folder = archive.getSearcher("desktopsyncfolder").createNewData();
		}
		
		folder.setValue("desktop",desktopid);
		folder.setValue("module",moduleid);
		folder.setName(entity.getName());

		//Optional
		String localpath = inReq.getRequestParameter("localpath");
		folder.setValue("localpath", localpath);
		folder.setValue("completeddate", new Date());
		folder.setValue("categorypath", categorypath);
		folder.setValue("entityid", entity.getId());
		archive.saveData("desktopsyncfolder", folder);
	}

	
	public Data getEntity(WebPageRequest inPageRequest) 
	{
		String entitymoduleid = null;
		
		String entitymoduleviewid = inPageRequest.findValue("entitymoduleviewid");
		if(entitymoduleviewid != null)
		{
			inPageRequest.putPageValue("entitymoduleviewid",entitymoduleviewid);
			Data entitymoduleviewdata = getMediaArchive(inPageRequest).getCachedData("view", entitymoduleviewid);
			if( entitymoduleviewdata != null)
			{
				inPageRequest.putPageValue("entitymoduleviewdata",entitymoduleviewdata);
			}
		}

		String entityid = inPageRequest.getRequestParameter("entityid");
		
		if(entityid == null)
		{
			entityid = inPageRequest.getRequestParameter("dataid");
		}
		
		
		if( entitymoduleid == null )
		{
			entitymoduleid = inPageRequest.getRequestParameter("entitymoduleid");  //TODO: remove, not secure

//			//Is this correct? Not sure what module this entity is part of
//			entitymoduleid = entitymoduleviewdata.get("rendertable");
//			if( entitymoduleid == null)
//			{
//				entitymoduleid = entitymoduleviewdata.get("moduleid");
//			}

			if( entitymoduleid == null )
			{
				entitymoduleid = inPageRequest.findPathValue("module");
			}
		}
		
		Data entitymodule = getMediaArchive(inPageRequest).getCachedData("module", entitymoduleid);
		inPageRequest.putPageValue("entitymodule",entitymodule);

		if (entityid == null)
		{
			return null;
		}
		if (entityid.startsWith("multiedit:"))
		{
			inPageRequest.putPageValue("ismulti",true);
		}
		
		Data entity = getMediaArchive(inPageRequest).getCachedData(inPageRequest, entitymoduleid, entityid);
		inPageRequest.putPageValue("entity",entity);
		
		return entity;
	}
	public void addAssetsToLightbox(WebPageRequest inPageRequest) throws Exception 
	{
	
		MediaArchive archive = getMediaArchive(inPageRequest);
		EntityManager entityManager = getEntityManager(inPageRequest);
		
		String moduleid = inPageRequest.findPathValue("module");
		String entityid = inPageRequest.getRequestParameter("entityid");
		
		String assetid = inPageRequest.getRequestParameter("assetid");
		
	
		String lightboxtype = inPageRequest.getRequestParameter("lightboxtype");
		String dataid = inPageRequest.getRequestParameter("lightboxid");
		Data selectedbox = archive.getCachedData(lightboxtype,dataid);
		
		Data module = archive.getCachedData("module", moduleid);
		Data entity = archive.getCachedData(moduleid, entityid);
		
		Category category = entityManager.loadLightboxCategory(module, entity, lightboxtype, selectedbox, inPageRequest.getUser() );
		
		if(category != null) {
			HitTracker assethits = (HitTracker) loadHitTracker(inPageRequest, moduleid);
			Collection finallist = null;
			if( assethits != null && assethits.hasSelections())
			{
				finallist = assethits.getSelectedHitracker();
			}
			else if( assetid != null)
			{
				finallist  = new ArrayList(1);
				Asset asset = archive.getAsset(assetid);
				asset.addCategory(category);
				archive.saveAsset(asset);
				finallist.add(asset);
			}
			int added = entityManager.addAssetsToCategory(archive, category, finallist);
			if( assethits != null )
			{
				assethits.deselectAll();
			}
			
			inPageRequest.putPageValue("assetsadded", added);
			
		}
		
		/*
		
		String lightboxid = inPageRequest.getRequestParameter("lightboxid");

		
		Integer added = entityManager.addToWorkflowStatus(inPageRequest.getUser(),moduleid,entityid,assethits,lightboxid);
		inPageRequest.putPageValue("assetsadded", added);
		assethits.deselectAll();*/
	}


	public void lightBoxRemoveAssets(WebPageRequest inPageRequest) throws Exception 
	{
	
		MediaArchive archive = getMediaArchive(inPageRequest);
		EntityManager entityManager = getEntityManager(inPageRequest);
		
		String entityid = inPageRequest.getRequestParameter("entityid");
		String lightboxid = inPageRequest.getRequestParameter("lightboxid");
		String categoryid = inPageRequest.getRequestParameter("categoryid");
		
	  String moduleid = inPageRequest.findPathValue("module");
		HitTracker assethits = (HitTracker) loadHitTracker(inPageRequest, moduleid);
		
		if( assethits != null && assethits.hasSelections())
		{
			HitTracker assethitscopy = assethits.getSelectedHitracker(); 
			assethits = assethitscopy;
			archive.getEntityManager().lightBoxRemoveAssets(inPageRequest.getUser(), categoryid, assethits);
			assethits.deselectAll();
		}

		
	}

//	public void loadLightBoxResults(WebPageRequest inReq)
//	{
//		MediaArchive archive = getMediaArchive(inReq);
//		EntityManager entityManager = getEntityManager(inReq);
//		String moduleid = inReq.findPathValue("module");
//		String entityid = inReq.getRequestParameter("entityid");
//		String lightboxid = inReq.getRequestParameter("lightboxid");
//		Map hitassetlookup = entityManager.loadLightBoxResults(inReq.getUser(), moduleid,entityid,lightboxid);
//		inReq.putPageValue("hitassetlookup",hitassetlookup);
//		HitTracker emedialightboxassets = (HitTracker) hitassetlookup.get("emedialightboxasset");
//		inReq.putPageValue("lightboxassets",emedialightboxassets);
//		inReq.putSessionValue(emedialightboxassets.getSessionId(), emedialightboxassets);
//
//		HitTracker assethits = (HitTracker) hitassetlookup.get("asset");
//		inReq.putPageValue("assethits",assethits);
//		inReq.putSessionValue(assethits.getSessionId(), assethits);
//	
//	}

	


	
	public void uploadSubTable(WebPageRequest inReq) throws Exception
	{
		FileUpload command = new FileUpload();
		command.setPageManager(getPageManager());
		UploadRequest properties = command.parseArguments(inReq);
		if (properties == null)
		{
			return;
		}
		if (properties.getFirstItem() == null)
		{
			return;
		}

		String entityid = inReq.getRequestParameter("entityid");
		String entitymoduleid = inReq.getRequestParameter("entitymoduleid");

		final String externalfieldname = entitymoduleid;
		final String externalfieldvalue = entityid;
		
		ScriptLogger logger = new ScriptLogger();
		for (Iterator iterator = properties.getUploadItems().iterator(); iterator.hasNext();)
		{
			FileUploadItem item = (FileUploadItem) iterator.next();
			Page tmp = getPageManager().getPage("/WEB-INF/temp/uploads/" + inReq.getUserName() + "/uploaded" + item.getName());
			ContentItem saveditem = properties.saveFileAs(item, tmp.getPath(), inReq.getUser());
			//tmp.setContentItem(saveditem);
			tmp = getPageManager().getPage("/WEB-INF/temp/uploads/" + inReq.getUserName() + "/uploaded" + item.getName());
			if( !tmp.exists() )
			{
				throw new OpenEditException("Upload issue");
			}
			//Now process it
			String mime = tmp.getMimeType();
			if( mime.endsWith("csv"))
			{
				CsvImporter csvimporter = new CsvImporter()
				{
					protected void addProperties(Row inRow, Data inData) 
					{
						super.addProperties(inRow, inData);
						inData.setValue(externalfieldname,externalfieldvalue);
					}
				};
				csvimporter.setModuleManager(getModuleManager());
				csvimporter.setContext(inReq);
				csvimporter.setImportPage(tmp);
				csvimporter.setLog(logger);
				csvimporter.setMakeId(false);
				csvimporter.setNewdDetailPrefix("user");
				csvimporter.importData();
				inReq.putPageValue("importtotal", csvimporter.getImportTotal());
			}
			else if (mime.contains("ms-excel"))
			{
				XlsImporter csvimporter = new XlsImporter();
				csvimporter.setModuleManager(getModuleManager());
				csvimporter.setContext(inReq);
				csvimporter.setImportPage(tmp);
				csvimporter.setLog(logger);
				csvimporter.setMakeId(false);
				csvimporter.importData();
				
			}
			else if (mime.endsWith("ditamap"))
			{
				
			}
			else if (mime.endsWith("dita"))
			{
				
			}
			getPageManager().removePage(tmp);
			
		}

		// inIn.delete();

	}
	
	public void saveSubModule(WebPageRequest inPageRequest) throws Exception 
	{
		
		String moduleid = inPageRequest.findPathValue("module"); //Submodule
		MediaArchive archive = getMediaArchive(inPageRequest);
		String pickedid = inPageRequest.getRequestParameter("id");
		MultiValued data = (MultiValued)inPageRequest.getPageValue("data");
		if( data == null)
		{
			data = (MultiValued) archive.getData(moduleid, pickedid);
		}
		if (data != null) 
		{
			String entitytype = inPageRequest.getRequestParameter("entitymoduleid"); //Parent Module
			String entityid = inPageRequest.getRequestParameter("entityid");
			Data entity =  archive.getCachedData(entitytype, entityid);
			
			String renderexternalid = entitytype;
			
			String entitymoduleviewid = inPageRequest.findValue("entitymoduleviewid");
			if(entitymoduleviewid != null)
			{
				Data entitymoduleviewdata = getMediaArchive(inPageRequest).getCachedData("view", entitymoduleviewid);
				if( entitymoduleviewdata != null)
				{
					renderexternalid = entitymoduleviewdata.get("renderexternalid");
				}
			}
			
			
			Searcher searcher = archive.getSearcher(moduleid);
			PropertyDetail detail =  searcher.getDetail(renderexternalid);
			
			if(detail != null)
			{
				if(detail.isMultiValue()) 
				{
					data.addValue(renderexternalid, entityid);
				}
				else {
					data.setValue(renderexternalid, entityid);
				}
			}
			//Copy the custom permissions
			String[] types = {"customusers","customgroups","customroles","editorusers","editorroles","editorgroups"};
			for (int i = 0; i < types.length; i++)
			{
				data.setValue(types[i], entity.getValue(types[i]) );
			}
			data.setValue("securityalwaysvisible", entity.getValue("securityalwaysvisible") );
			
			searcher.saveData(data);
		}
		
	}
	
	
	public void updatePermissions(WebPageRequest inReq) {
		
		MediaArchive archive = getMediaArchive(inReq);
		archive.getPermissionManager().handleModulePermissionsUpdated();
	}
	
	public void saveEntityCategoryPermissions(WebPageRequest inReq) {
		
		MediaArchive archive = getMediaArchive(inReq);
		Data entity  = (Data) inReq.getPageValue("data");
		if(entity == null)
		{
			entity  = (Data) inReq.getPageValue("entity");
		}
		String moduleid  = inReq.findPathValue("module");
		Data module = archive.getCachedData("module", moduleid);
		if (module != null && entity != null)
		{
			archive.getPermissionManager().checkEntityCategoryPermission(module, (MultiValued)entity);
		}
	}
	
	//Use in entity dialog
	public Collection<AddedPermission> loadEntityPermissions(WebPageRequest inReq) 
	{
		MediaArchive archive = getMediaArchive(inReq);
		Data entity  = (Data) inReq.getPageValue("entity");
		String moduleid  = inReq.findPathValue("module");
		Data module = archive.getData("module", moduleid);
		Collection<AddedPermission> all = archive.getPermissionManager().loadEntityPermissions(module, entity);
		inReq.putPageValue("entitypermissions", all);
		
		Collection<AddedPermission> parentviewers = archive.getPermissionManager().loadParentPermissions(module, entity);
		inReq.putPageValue("parentpermissions", parentviewers);
		
		
		return all;
	}
	
	//Adds ok
	public void entityPermissionAdd(WebPageRequest inReq) 
	{
		MediaArchive archive = getMediaArchive(inReq);
		Data entity  = (Data) inReq.getPageValue("entity");
		String moduleid  = inReq.findPathValue("module");
		Data module = archive.getData("module", moduleid);
		
		String[] fields = inReq.getRequestParameters("field");
		Map<String,String[]> tosave = new HashMap();
		for (int i = 0; i < fields.length; i++)
		{
			String[] values = inReq.getRequestParameters(fields[i] + ".values");
			if( values != null && values.length > 0 )
			{
				tosave.put(fields[i],values);
			}
		}
		
		archive.getPermissionManager().addEntityPermissions(module, (MultiValued)entity, tosave);
		archive.getPermissionManager().checkEntityCategoryPermission(module, (MultiValued)entity);
		
		archive.saveData(module.getId(),entity);
		inReq.putPageValue("saved", tosave);
	}
	
	//Change the status
	public void entityPermissionsSave(WebPageRequest inReq) 
	{
		//Copy to the editor fields but leave inthe view fields
		MediaArchive archive = getMediaArchive(inReq);
		MultiValued entity  = (MultiValued) inReq.getPageValue("entity");
		String moduleid  = inReq.findPathValue("module");
		Data module = archive.getData("module", moduleid);
		
		String securityalwaysvisible = inReq.getRequestParameter("securityalwaysvisible");
		entity.setValue("securityalwaysvisible",Boolean.parseBoolean(securityalwaysvisible));
		
		String[] dataid = inReq.getRequestParameters("dataid");
		if( dataid != null )
		{
			String[] iseditor = inReq.getRequestParameters("iseditor");
			String[] permissiontype = inReq.getRequestParameters("permissiontype");
			for (int i = 0; i < dataid.length; i++)
			{
				String fieldname = permissiontype[i];
				String plural =fieldname;
				if( !fieldname.endsWith("s") )
				{
					plural = plural + "s";
				}
				entity.removeValue("viewer" + plural, dataid[i]);
				entity.removeValue("editor" + plural, dataid[i]);
	
				if( iseditor != null && iseditor[i].equals("true") ) //Must match or error
				{
					entity.addValue("editor" + plural, dataid[i]);
				}
				else
				{
					entity.addValue("viewer" + plural, dataid[i]);
				}
			}
		}
		archive.getPermissionManager().checkEntityCategoryPermission(module, (MultiValued)entity);
		
		archive.saveData(module.getId(),entity);
		
		
	}	
	public void entityPermissionRemove(WebPageRequest inReq) 
	{
		//Copy to the editor fields but leave inthe view fields
		MediaArchive archive = getMediaArchive(inReq);
		String moduleid  = inReq.findPathValue("module");
		Data module = archive.getData("module", moduleid);
		MultiValued entity  = (MultiValued) inReq.getPageValue("entity");
		//String permissiontype = inReq.getRequestParameter("permissiontype");
		String dataid = inReq.getRequestParameter("dataid");
		
		//Add to the type we want
		
		//Check the type
		entity.removeValue("viewerusers", dataid);
		entity.removeValue("viewerroles", dataid);			
		entity.removeValue("viewergroups", dataid);			

		entity.removeValue("editorusers", dataid);
		entity.removeValue("editorroles", dataid);			
		entity.removeValue("editorgroups", dataid);			
		
		archive.saveData(module.getId(),entity);

		archive.getPermissionManager().checkEntityCategoryPermission(module, (MultiValued)entity);
	}
/*
	public void removeEntityPermission(WebPageRequest inReq) 
	{
		MediaArchive archive = getMediaArchive(inReq);
		Collection<AddedPermission> all = loadEntityPermissions(inReq);
		
		//TODO: Loop over and remove the one
		inReq.putPageValue("entitypermissions", all);
	}
	*/
	
	
	public void assignUserToEntities(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		
		MultiValued entity = (MultiValued) inReq.getPageValue("entity"); 
		
		String moduleid = inReq.findPathValue("module");
	
		if( entity != null)
		{
	
			if( !entity.containsValue("viewerusers", inReq.getUserName()) )
			{
				entity.addValue("viewerusers", inReq.getUserName());
				archive.saveData(moduleid, entity);
				
				Data module = archive.getCachedData("module", moduleid);
				if (module != null && entity != null)
				{
					archive.getPermissionManager().checkEntityCategoryPermission(module, (MultiValued)entity);
				}
			}	
			inReq.putPageValue("openentityid", entity.getId());
		}
		
		
	}
	
	public void snapshotData(WebPageRequest inReq) {

		MediaArchive archive = getMediaArchive(inReq);
		
		String id = inReq.getRequestParameter("id");
		if (id == null) {
			//New Record
			return;
		}
		String moduleid = inReq.findPathValue("module");
		
		List<Data> tosave = new ArrayList();
		if (id.startsWith("multiedit:"))
		{
			CompositeData entities = (CompositeData) inReq.getSessionValue(id);
			for (Iterator iterator = entities.iterator(); iterator.hasNext();)
			{
				Data entityd = (Data) iterator.next();
				Data entity = archive.query(moduleid).id(entityd.getId()).searchOne();
				tosave.add(entity);
			}
		}
		else 
		{
			Data entity = archive.query(moduleid).id(id).searchOne();
			tosave.add(entity);
		}
			
		//changes = archive.getEventManager().readChanges(inReq, archive.getSearcher(moduleid), entity);
		String changes = (String)inReq.getPageValue("datachanges");
		for (Iterator iterator = tosave.iterator(); iterator.hasNext();) {
			MultiValued entity = (MultiValued) iterator.next();
			archive.getEntityManager().createEntitySnapshot( inReq.getUser(), entity, changes );
		}
		
		
	}
	

	public void restoreSnapshot(WebPageRequest inReq) throws Exception{

		MediaArchive archive = getMediaArchive(inReq);
		String historyid = inReq.getRequestParameter("historyid");
		archive.getEntityManager().restoreSnapshot( inReq.getUser(), historyid);
		
	}

	
	/*
	 * Moved to UserManager.loadChatChannel
	 * 
	public void loadChatChannel( WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String channel = inReq.findValue("channel");
		if (channel == null)
		{
			channel = inReq.getRequestParameter("channel");
		}
		Data currentchannel = archive.getCachedData("channel", channel);
		
		Searcher topicsearcher = archive.getSearcher("channel");
		MultiValued entity  = (MultiValued) inReq.getPageValue("entity");
		String module = inReq.findValue("module");
		if (currentchannel == null) {
			currentchannel = topicsearcher.query().match("dataid",entity.getId()).match("searchtype", module).sort("name").searchOne();
		}
		
		if (currentchannel == null) {
			currentchannel = topicsearcher.createNewData();
			currentchannel.setValue("searchtype", module);
			currentchannel.setValue("dataid", entity.getId() );
			
			String channeltype = inReq.findValue("channeltype");
			if ("agententitychat".equals(channeltype))
			{
				currentchannel.setName("Guided Chat");
				currentchannel.setValue("channeltype", "agententitychat");
			}
			else 
			{
				currentchannel.setName("General");
				currentchannel.setValue("channeltype", "entity");
			}
			
			currentchannel.setValue("refreshdate", new Date() );
			topicsearcher.saveData(currentchannel);
		}
		
		inReq.putPageValue("currentchannel", currentchannel);
	}
	*/
	
	
	
	public void subModuleDelete(WebPageRequest inPageRequest) throws Exception 
	{
		String moduleid = inPageRequest.findPathValue("module"); //Parent module
		
		MediaArchive archive = getMediaArchive(inPageRequest);
			
		String entitymoduleviewid = inPageRequest.findValue("entitymoduleviewid");
		if(entitymoduleviewid != null)
		{
			Data entitymoduleviewdata = getMediaArchive(inPageRequest).getCachedData("view", entitymoduleviewid);
			if( entitymoduleviewdata != null)
			{
				String submodule = entitymoduleviewdata.get("rendertable"); //SubModule
				Searcher searcher = getMediaArchive(inPageRequest).getSearcher(submodule);
				String id = inPageRequest.getRequestParameter("hitssessionid");
				HitTracker hits = (HitTracker) inPageRequest.getSessionValue(id);
				Collection todelete = new ArrayList(hits.getSelectedHitracker());
				
				for (Iterator iterator = todelete.iterator(); iterator.hasNext();)
				{
					Data hit = (Data) iterator.next();
					hits.removeSelection(hit.getId());
				}
				searcher.deleteAll(todelete, inPageRequest.getUser());
			}
		}
	}	
	
	public void loadDefaultFolder(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		Data entity = getEntity(inReq);
		if(entity == null)
		{
			entity = (Data) inReq.getPageValue("data");
		}
		
		archive.getEntityManager().loadDefaultFolder(entity, inReq.getUser());

		inReq.putPageValue("entity", entity);
	}
	
	public void createEntityFromFiles(WebPageRequest inReq) throws Exception
	{	
		UploadRequest uploadRequest = (UploadRequest) inReq.getPageValue("uploadrequest");
		
		MediaArchive archive = getMediaArchive(inReq);
		String moduleid = inReq.getRequestParameter("moduleid");
		Data module = archive.getCachedData("module", moduleid);
		
		archive.getEntityManager().createEntitiesFromPages(inReq, uploadRequest, module);
	}
	
	public void createEntityFromYoutube(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		
		String parentmoduleid = inReq.getRequestParameter("parentmoduleid");
		String parententityid = inReq.getRequestParameter("parententityid");
		
		String moduleid = inReq.getRequestParameter("moduleid");
		Data module = archive.getCachedData("module", moduleid);
		
		String confirmed = inReq.getRequestParameter("confirmed");
		
		String url = inReq.getRequestParameter("youtubeurl");
		String overwriteurl = inReq.getRequestParameter("overwriteurl");
		if(overwriteurl != null && overwriteurl.length() > 0)
		{
			url = overwriteurl;
			confirmed = "true";
		}
		
		inReq.putPageValue("youtubeurl", url);
		if(url != null)
		{
			
			YoutubeImporter importer = (YoutubeImporter) archive.getBean("youtubeImporter");

			if( confirmed == null || !confirmed.equals("true"))
			{
				YoutubeParser ytParser = importer.getParser(url);
				String type = ytParser.getType();
				int count = 0;
				
				if(type.equals("CHANNEL") || type.equals("HANDLE"))
				{
					count = importer.countVideosInChannel(archive, ytParser);
				}
				else if(type.equals("PLAYLIST"))
				{
					count = importer.countVideosInPlaylist(archive, ytParser);
				}
				
				if(count > 1)
				{						
					inReq.putPageValue("videocount", count);
					inReq.putPageValue("confirmimport", true);
					inReq.putPageValue("overwriteurl", "https://youtube.com/playlist?list=" + ytParser.getId());
					return;
				}
				else 
				{
					inReq.putPageValue("confirmimport", false);
				}
			}
			
			Collection<YoutubeMetadataSnippet> metadatas = importer.importMetadataFromUrl(archive, url);
			
			
			Collection<String> existingIds = archive.query(moduleid).exists("embeddedid").exact("embeddedtype", "youtube").search().collectValues("embeddedid");
			
			for (YoutubeMetadataSnippet metadata : metadatas) {
				if(existingIds.contains( metadata.getVideoId()))
				{
					log.info("Skipping existing video: " + metadata.getTitle());
					continue;
				}
				
				String thumbnailname = PathUtilities.extractFileName(metadata.getTitle(), true);
				String sourcepath = archive.getAssetImporter().getAssetUtilities().createSourcePath(inReq, archive, thumbnailname);
				
				archive.getEntityManager().createEntityFromYoutubeMetadata(inReq.getUser(), module, metadata, parentmoduleid, parententityid, sourcepath);
			}
			inReq.putPageValue("importedcount", metadatas.size());
			archive.fireSharedMediaEvent("importing/fetchdownloads");
		}
	}
	
	public void createCollection(WebPageRequest inReq)
	{
		MediaArchive mediaArchive = getMediaArchive(inReq);
		Searcher librarysearcher = mediaArchive.getSearcher("librarycollection");
		LibraryCollection saved = (LibraryCollection) librarysearcher.createNewData();
		librarysearcher.updateData(inReq, inReq.getRequestParameters("field"), saved);
		
		String rootcategory = inReq.getRequestParameter("rootcategory.value");
		Category category = mediaArchive.getCategory(rootcategory);
		
		saved.setValue("archivesourcepath", category.getCategoryPath());
		saved.setName(category.getName());
		saved.setValue("entity_date", new Date());
		saved.setValue("owner", inReq.getUserName());

		librarysearcher.saveData(saved, null); //this fires event ProjectManager.configureCollection



	}
	
	
}
