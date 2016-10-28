package org.entermediadb.projects;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.AssetUtilities;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.scanner.PresetCreator;
import org.entermediadb.asset.xmldb.CategorySearcher;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.FilterNode;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.profile.UserProfile;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.FileUtils;

public class ProjectManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(ProjectManager.class);

	protected SearcherManager fieldSearcherManager;
	protected String fieldCatalogId;
	protected AssetUtilities fieldAssetUtilities;
	private int COPY = 1;
	private int MOVE = 2;
	protected ModuleManager fieldModuleManager;
	
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

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.entermediadb.asset.push.PushManager#setSearcherManager(org.openedit.
	 * data.SearcherManager)
	 */
	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public Data getCurrentLibrary(UserProfile inProfile)
	{
		if (inProfile != null)
		{
			String lastselectedid = inProfile.get("last_selected_library");
			if (lastselectedid != null)
			{
				Data library = getSearcherManager().getData(getCatalogId(), "library", lastselectedid);
				return library;
			}
		}
		return null;
	}

	public Collection<LibraryCollection> loadCollections(WebPageRequest inReq, MediaArchive inArchive)
	{
		//get a library
		Collection<LibraryCollection> usercollections = (Collection<LibraryCollection>) inReq.getPageValue("usercollections");
		if (usercollections != null)
		{
			return usercollections;
		}

		Data library = getCurrentLibrary(inReq.getUserProfile());
		if (library == null)
		{
			library = loadUserLibrary(inArchive, inReq.getUserProfile());
		}
		if (library != null)
		{
			inReq.putPageValue("selectedlibrary", library);

			Searcher searcher = getSearcherManager().getSearcher(getCatalogId(), "librarycollection");
			//String reloadcollectoin = inReq.getRequestParameter("reloadcollection");
			HitTracker allcollections = searcher.query().exact("library", library.getId()).sort("name").named("sidebar").search();

			//Mach up all the top level categories
			usercollections = loadUserCollections(inReq, allcollections, inArchive, library);
			inReq.putPageValue("usercollections", usercollections);
			return usercollections;
		}
		return Collections.EMPTY_LIST;

		//search
		//Searcher searcher = getSearcherManager().getSearcher(getMediaArchive().getCatalogId(),"librarycollection") )
		//HitTracker labels = searcher.query().match("library",$library.getId()).sort("name").search() )

	}

	public Collection<LibraryCollection> loadOpenCollections(WebPageRequest inReq, MediaArchive inArchive)
	{
		//get a library
		//inReq.putPageValue("selectedlibrary",library);

		Collection<LibraryCollection> usercollections = (Collection<LibraryCollection>) inReq.getPageValue("usercollections");
		if (usercollections != null)
		{
			return usercollections;
		}
		//enable filters to show the asset count on each collection node

		Collection opencollections = inReq.getUserProfile().getValues("opencollections");
		if (opencollections == null)
		{
			return Collections.EMPTY_LIST;
		}

		if (opencollections.size() > 0) //May not have any collections
		{
			//Build list of ID's
			Searcher searcher = getSearcherManager().getSearcher(getCatalogId(), "librarycollection");
			HitTracker allcollections = searcher.query().orgroup("id", opencollections).named("sidebar").search(); //todo: Cache?
			usercollections = loadUserCollections(inReq, allcollections, inArchive, null);
			inReq.putPageValue("usercollections", usercollections);
		}
		return usercollections;

	}

	protected Collection<LibraryCollection> loadUserCollections(WebPageRequest inReq, Collection<Data> allcollections, MediaArchive inArchive, Data library)
	{
		Searcher lcsearcher = inArchive.getSearcher("librarycollection");
		List usercollections = new ArrayList(allcollections.size());

		Collection categoryids = new ArrayList();
		for (Data collection : allcollections)
		{
			LibraryCollection uc = (LibraryCollection) lcsearcher.loadData(collection);
			
			if( uc.hasRootCategory())
			{
				categoryids.add(uc.getRootCategoryId());
			}
			usercollections.add(uc);
		}
		HitTracker hits = inArchive.getAssetSearcher().query().orgroup("category", categoryids).addFacet("category").named("librarysidebar").search();
		int assetsize = 0;
		if(hits != null){
			 assetsize = hits.size();

		} else{
			assetsize = 0;
		}
		inReq.putPageValue("librarysize", assetsize);

		//Show all the collections for a library
		inReq.putPageValue("allcollections", usercollections);

		//enable filters to show the asset count on each collection node
		FilterNode collectionhits = null;
		if (hits != null && allcollections.size() > 0) //May not have any collections
		{
			FilterNode node = hits.findFilterNode("category");
			if (node != null)
			{
				for (Iterator iterator = usercollections.iterator(); iterator.hasNext();)
				{
					LibraryCollection collection = (LibraryCollection) iterator.next();
					if( collection.hasRootCategory())
					{
						collection.setAssetCount(node.getCount(collection.getRootCategoryId()));
					}
				}
			}
		}

		return usercollections;
	}

	//	public void addAssetToCollection(MediaArchive archive, String libraryid, String collectionid, HitTracker assets)
	//	{
	//		if (libraryid != null)
	//		{
	//			addAssetToLibrary(archive, libraryid, assets);
	//		}
	//		addAssetToCollection(archive, collectionid, assets);
	//	}

	public void addAssetToCollection(MediaArchive archive, String librarycollection, HitTracker assets)
	{
		List tosave = new ArrayList();
		assets.setHitsPerPage(200);
		for (int i = 0; i < assets.getTotalPages(); i++)
		{
			assets.setPage(i + 1);
			Map assetids = new HashMap();
			for (Object hit : assets.getPageOfHits())
			{
				Data asset = (Data) hit;
				assetids.put(asset.getId(), asset);
			}
			Category root = getRootCategory(archive, librarycollection);
			Collection existing = archive.getAssetSearcher().query().match("category", root.getId()).search();
			for (Iterator iterator = existing.iterator(); iterator.hasNext();)
			{
				Data hit = (Data) iterator.next();
				assetids.remove(hit.getId());
			}
			for (Iterator iterator = assetids.values().iterator(); iterator.hasNext();)
			{
				Data hit = (Data) iterator.next();
				Asset asset = (Asset) archive.getAssetSearcher().loadData(hit);
				asset.addCategory(root);
				tosave.add(asset);
				if (tosave.size() > 200)
				{
					archive.getAssetSearcher().saveAllData(tosave, null);
					tosave.clear();
				}
			}
		}
		archive.getAssetSearcher().saveAllData(tosave, null);

	}

	//	public void addAssetToCollection(MediaArchive archive, String libraryid, String collectionid, String assetid)
	//	{
	//		addAssetToLibrary(archive, libraryid, assetid);
	//		//String librarycollection = inReq.getRequestParameter("librarycollection");
	//		addAssetToCollection(archive, collectionid, assetid);
	//	}
	//
	public void addAssetToCollection(MediaArchive archive, String collectionid, String assetid)
	{
		Category root = getRootCategory(archive, collectionid);
		Asset asset = (Asset) archive.getAssetSearcher().searchById(assetid);
		if (asset != null)
		{
			asset.addCategory(root);
			archive.getAssetSearcher().saveData(asset);
		}
	}

	//	public void addAssetToLibrary(MediaArchive archive, String libraryid, HitTracker assets)
	//	{
	//		List tosave = new ArrayList();
	//		for (Object data : assets)
	//		{
	//			//TODO: Skip loading?
	//			MultiValued toadd = (MultiValued) data;
	//			Collection libraries = toadd.getValues("libraries");
	//			if (libraries != null && libraries.contains(libraryid))
	//			{
	//				continue;
	//			}
	//			Asset asset = (Asset) archive.getAssetSearcher().loadData(toadd);
	//
	//			if (asset != null && !asset.getLibraries().contains(libraryid))
	//			{
	//				asset.addLibrary(libraryid);
	//				tosave.add(asset);
	//				if (tosave.size() > 500)
	//				{
	//					archive.saveAssets(tosave);
	//					tosave.clear();
	//				}
	//			}
	//		}
	//		archive.saveAssets(tosave);
	//
	//	}
	//
	//	public void addAssetToLibrary(MediaArchive archive, String libraryid, String assetid)
	//	{
	//		Asset asset = archive.getAsset(assetid);
	//
	//		if (asset != null && !asset.getLibraries().contains(libraryid))
	//		{
	//			asset.addLibrary(libraryid);
	//			archive.saveAsset(asset, null);
	//		}
	//	}
	//
	//	public HitTracker loadAssetsInLibrary(Data inLibrary, MediaArchive archive, WebPageRequest inReq)
	//	{
	//		HitTracker hits = archive.getAssetSearcher().query().match("libraries", inLibrary.getId()).search(inReq);
	//		return hits;
	//	}
	//
	public HitTracker loadAssetsInCollection(WebPageRequest inReq, MediaArchive archive, String collectionid)
	{
		Searcher searcher = archive.getAssetSearcher();
		HitTracker all = null;
		//		if( assetsearch instanceof LuceneSearchQuery)
		//		{
		//			SearchQuery collectionassetsearch = archive.getSearcher("librarycollectionasset").query().match("librarycollection",collectionid).getQuery();
		//			assetsearch.addJoinFilter(collectionassetsearch,"asset",false,"librarycollectionasset","id");
		////			all = archive.getAssetSearcher().cachedSearch(inReq, assetsearch);
		//			all = archive.getAssetSearcher().search(assetsearch);
		//		}
		//		else
		//		{	
		//SearchQuery collectionassetsearch = archive.getSearcher("librarycollectionasset").query().match("librarycollection",collectionid).getQuery();
		SearchQuery assetsearch = searcher.addStandardSearchTerms(inReq);
		Category root = getRootCategory(archive, collectionid);

		if (assetsearch == null)
		{
			assetsearch = searcher.createSearchQuery();
			assetsearch.addExact("category", root.getId());

		}
		all = archive.getAssetSearcher().search(assetsearch);

		String hpp = inReq.getRequestParameter("page");
		if (hpp != null)
		{
			all.setPage(Integer.parseInt(hpp));
		}
		UserProfile usersettings = (UserProfile) inReq.getUserProfile();
		if (usersettings != null)
		{
			all.setHitsPerPage(usersettings.getHitsPerPageForSearchType("asset"));
		}
		all.getSearchQuery().setProperty("collectionid", collectionid);
		all.getSearchQuery().setHitsName("collectionassets");
		return all;
	}

	//	public void removeAssetFromLibrary(MediaArchive inArchive, String inLibraryid, HitTracker inAssets)
	//	{
	//		Searcher librarycollectionsearcher = inArchive.getSearcher("librarycollection");
	//		HitTracker<Data> collections = (HitTracker<Data>) librarycollectionsearcher.query().match("library", inLibraryid).search();
	//		for (Object collection : collections)
	//		{
	//			removeAssetFromCollection(inArchive, ((Data) collection).getId(), inAssets);
	//		}
	//		for (Object toadd : inAssets)
	//		{
	//			Asset asset = (Asset) inArchive.getAssetSearcher().loadData((Data) toadd);
	//
	//			if (asset != null && asset.getLibraries().contains(inLibraryid))
	//			{
	//				asset.removeLibrary(inLibraryid);
	//				inArchive.saveAsset(asset, null);
	//			}
	//		}
	//	}

	public void removeAssetFromCollection(MediaArchive inArchive, String inCollectionid, HitTracker inAssets)
	{
		LibraryCollection col = getLibraryCollection(inArchive, inCollectionid);
		Category cat = getRootCategory(inArchive, col);
		Collection tosave = new ArrayList();
		for (Iterator iterator = inAssets.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			Asset asset = (Asset)inArchive.getAssetSearcher().loadData(data);
			asset.removeChildCategory(cat); //
			tosave.add(asset);
		}
		inArchive.getAssetSearcher().saveAllData(tosave, null);
	}
	/*
	 * public Collection<LibraryCollection> loadRecentCollections(WebPageRequest
	 * inReq) { //enable filters to show the asset count on each collection node
	 * UserProfile profile = inReq.getUserProfile(); Searcher
	 * librarycollectionsearcher =
	 * getSearcherManager().getSearcher(getCatalogId(), "librarycollection");
	 * Collection combined = profile.getCombinedLibraries(); if
	 * (inReq.getUser().isInGroup("administrators")) { combined.clear();
	 * combined.add("*"); } if (combined.size() == 0) { return
	 * Collections.EMPTY_LIST; } HitTracker allcollections =
	 * librarycollectionsearcher.query().orgroup("library",
	 * profile.getCombinedLibraries()).sort("name").named("sidebar").search(
	 * inReq); FilterNode collectionhits = null; if (allcollections.size() > 0)
	 * //May not have any collections { Searcher collectionassetsearcher =
	 * getSearcherManager().getSearcher(getCatalogId(),
	 * "librarycollectionasset");
	 * 
	 * //Build list of ID's List ids = new ArrayList(allcollections.size()); for
	 * (Iterator iterator = allcollections.iterator(); iterator.hasNext();) {
	 * Data collection = (Data) iterator.next(); ids.add(collection.getId()); }
	 * if (ids.size() > 0) { HitTracker collectionassets =
	 * collectionassetsearcher.query().orgroup("librarycollection",
	 * ids).sort("recorddate").named("homecollections").search(inReq); if
	 * (collectionassets != null && collectionassets.size() > 0) //No assets
	 * found at all { collectionhits =
	 * collectionassets.findFilterNode("librarycollection"); } } }
	 * Collection<LibraryCollection> usercollections =
	 * loadUserCollections(allcollections, collectionhits);
	 * inReq.putPageValue("usercollections", usercollections); return
	 * usercollections; }
	 */

	public Data addCategoryToCollection(User inUser, MediaArchive inArchive, String inCollectionid, String inCategoryid)
	{
		if (inCategoryid != null)
		{
			LibraryCollection collection = null;
			Data data = null;
			Searcher librarycolsearcher = inArchive.getSearcher("librarycollection");
			if (inCollectionid == null)
			{
				collection = (LibraryCollection) librarycolsearcher.createNewData();
				collection.setValue("library", inUser.getId()); //Make one now?
				collection.setValue("owner", inUser.getId());
				Category cat = inArchive.getCategory(inCategoryid);
				collection.setName(cat.getName());
				librarycolsearcher.saveData(collection, null);
				inCollectionid = collection.getId();
			}
			else
			{
				collection = getLibraryCollection(inArchive,inCollectionid);
				Category cat = inArchive.getCategory(inCategoryid);
				Category rootcat = getRootCategory(inArchive, inCollectionid);

				ArrayList list = new ArrayList();
				copyAssets(list, inUser, inArchive, collection, cat, rootcat, false);// will actually create librarycollectionasset entries
				Searcher assets = inArchive.getAssetSearcher();
				assets.saveAllData(list, null);

			}
			return collection;
		}
		return null;
	}

	public void snapshotCollection(WebPageRequest inReq, User inUser, MediaArchive inArchive, String inCollectionid, String inNote)
	{
		Category rootcat = getRootCategory(inArchive, inCollectionid);

		LibraryCollection collection = getLibraryCollection(inArchive,inCollectionid);
		Category newroot = createRevision(inArchive, collection, inUser, inNote);

		ArrayList list = new ArrayList();
		copyAssets(list, inUser, inArchive, collection, rootcat, newroot, true);// will actually create librarycollectionasset entries
		Searcher assets = inArchive.getAssetSearcher();
		assets.saveAllData(list, null);

	}

	//Add new assets are added to both the root and the current version

	//When we snapshot we just increment the count and add existing to the new count as well

	public void importCollection(WebPageRequest inReq, User inUser, MediaArchive inArchive, String inCollectionid, String inImportPath, String inNote)
	{
		LibraryCollection collection = getLibraryCollection(inArchive,inCollectionid);
		snapshotCollection(inReq, inUser, inArchive, inCollectionid, inNote);
		CategorySearcher searcher = inArchive.getCategorySearcher();
		Category root = getRootCategory(inArchive, inCollectionid);
		HitTracker allids = inArchive.getAssetSearcher().fieldSearch("category", root.getId());
		ArrayList ids = new ArrayList();
		for (Iterator iterator = allids.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			ids.add(hit.getId());
		}
		searcher.deleteCategoryTree(root);
		root = getRootCategory(inArchive, inCollectionid);
		importAssets(inArchive, collection, inImportPath, root, ids);

		for (Iterator iterator = ids.iterator(); iterator.hasNext();)
		{
			String assetid = (String) iterator.next();
			Asset asset = inArchive.getAsset(assetid);
			inArchive.saveAsset(asset, inUser);
		}

		inArchive.fireSharedMediaEvent("conversions/runconversions");
	}

	public Category createRevision(MediaArchive inArchive, LibraryCollection collection, User inUser, String inNote)
	{
		Searcher librarycolsearcher = inArchive.getSearcher("librarycollection");
		long revisions = collection.getCurentRevision();
		revisions++;
		collection.setValue("revisions", revisions);
		librarycolsearcher.saveData(collection);

		Searcher librarycollectionuploads = inArchive.getSearcher("librarycollectionsnapshot");

		Data history = librarycollectionuploads.createNewData();

		history.setValue("owner", inUser.getId());
		history.setValue("librarycollection", collection.getId());
		history.setValue("date", new Date());
		history.setValue("revision", revisions);
		history.setValue("note", inNote);

		librarycollectionuploads.saveData(history);

		return getRootVersionCategory(inArchive, collection);
	}

	protected void importAssets(MediaArchive inArchive, Data inCollection, String inImportPath, Category inCurrentParent, Collection assets)
	{
		//inCurrentParent.setChildren(null);
		String sourcepathmask = inArchive.getCatalogSettingValue("projectassetupload"); //${division.uploadpath}/${user.userName}/${formateddate}

		Map vals = new HashMap();
		vals.put("librarycollection", inCollection.getId());
		vals.put("library", inCollection.get("library"));

		Collection paths = inArchive.getPageManager().getChildrenPaths(inImportPath);
		for (Iterator iterator = paths.iterator(); iterator.hasNext();)
		{
			String child = (String) iterator.next();
			ContentItem item = inArchive.getPageManager().getRepository().getStub(child);
			if (item.isFolder())
			{
				Category newfolder = (Category) inArchive.getCategorySearcher().createNewData();
				newfolder.setName(item.getName());
				//Who cares about the id
				inCurrentParent.addChild(newfolder);
				//inArchive.getCategorySearcher().saveData(inCurrentParent);
				inArchive.getCategorySearcher().saveData(newfolder);

				importAssets(inArchive, inCollection, inImportPath + "/" + item.getName(), newfolder, assets);
			}
			else
			{
				//MD5
				String md5;
				InputStream inputStream = item.getInputStream();

				try
				{
					md5 = DigestUtils.md5Hex(inputStream);
				}
				catch (Exception e)
				{
					throw new OpenEditException(e);
				}
				finally
				{
					FileUtils.safeClose(inputStream);
				}
				PropertyDetail namedetail = inArchive.getAssetSearcher().getDetail("name");
				Data target = null;
				if(namedetail.isMultiLanguage()){
				 target = (Data) inArchive.getAssetSearcher().query().exact("md5hex", md5).exact("name_int.en.sort", item.getName()).searchOne();
				} else{
				 target = (Data) inArchive.getAssetSearcher().query().exact("md5hex", md5).exact("name.sort", item.getName()).searchOne();
				}
				Asset asset = (Asset) inArchive.getAssetSearcher().loadData(target);
				
				if (asset == null)
				{

					String savesourcepath = inArchive.getAssetImporter().getAssetUtilities().createSourcePathFromMask(inArchive, null, item.getName(), sourcepathmask, vals);
					ContentItem destination = inArchive.getPageManager().getRepository().getStub("/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/" + savesourcepath);
					ContentItem finalitem = inArchive.getPageManager().getPage(destination.getPath() + "/" + item.getName()).getContentItem();

					inArchive.getPageManager().getRepository().move(item, destination);
					asset = inArchive.getAssetImporter().getAssetUtilities().createAssetIfNeeded(finalitem, true, inArchive, null); //this also extracts the md5
					asset.setCategories(null);
					inArchive.saveAsset(asset, null);

					//asset.setValue("md5hex", md5);
					PresetCreator presets = inArchive.getPresetManager();
					Searcher tasksearcher = inArchive.getSearcherManager().getSearcher(inArchive.getCatalogId(), "conversiontask");
					presets.createMissingOnImport(inArchive, tasksearcher, asset);
				}
				else
				{
					inArchive.getPageManager().getRepository().remove(item);

				}
				asset.addCategory(inCurrentParent);
				inArchive.saveAsset(asset, null);
				assets.remove(asset.getId());
			}
		}
		
		paths = inArchive.getPageManager().getChildrenPaths(inImportPath);
		if(paths.isEmpty()){
			ContentItem item = inArchive.getPageManager().getRepository().getStub(inImportPath);
			inArchive.getPageManager().getRepository().remove(item);
		}
		
	}
	public Category getRootCategory(MediaArchive inArchive, String inCollectionId)
	{
		Searcher librarycolsearcher = inArchive.getSearcher("librarycollection");
		LibraryCollection collection = (LibraryCollection) librarycolsearcher.searchById(inCollectionId);
		return getRootCategory(inArchive, collection);
		
	}
	public Category getRootCategory(MediaArchive inArchive, LibraryCollection inCollection)
	{
		if (inCollection == null)
		{
			return null;
		}
		Searcher librarycolsearcher = inArchive.getSearcher("librarycollection");
		String categoryid = inCollection.get("rootcategory");
		Category collectioncategory = null;
		if (categoryid != null)
		{
			collectioncategory = (Category) inArchive.getCategorySearcher().searchById(categoryid);
		}
		
		if (collectioncategory == null)
		{
			collectioncategory = createRootCategory(inArchive, inCollection);
			inCollection.setValue("rootcategory", collectioncategory.getId());
			librarycolsearcher.saveData(inCollection);
		}

		return collectioncategory;
	}

	private Category createRootCategory(MediaArchive inArchive, LibraryCollection collection)
	{
		Searcher librarysearcher = inArchive.getSearcher("library");
		String libraryid = collection.get("library");
		if(libraryid == null){
			libraryid = "default";
			collection.setValue("library", "default");
			inArchive.getSearcher("librarycollection").saveData(collection);
		}
		Data library = inArchive.getData("library", libraryid);
		if(library == null && "default".equals(libraryid)){
			library = librarysearcher.createNewData();
			library.setId("default");
			library.setName("Default");
			librarysearcher.saveData(library);
		}
		Category librarycategory = null;

		if (library.get("categoryid") != null)
		{
			librarycategory = inArchive.getCategory(library.get("categoryid"));
		}
		else
		{

			String folder = library.get("folder");
			if (folder == null)
			{
				folder = "Index/Libraries/" + library.getName();
			}
			librarycategory = inArchive.createCategoryPath(folder);
			library.setValue("categoryid", librarycategory.getId());
			//			if( inUser != null && librarycategory.getValue("viewusers") == null)
			//			{
			//				librarycategory.addValue("viewusers",inUser.getUserName());
			//			}
			inArchive.getSearcher("library").saveData(library);
		}
		Category collectioncategory = inArchive.createCategoryPath(librarycategory.getCategoryPath() + "/" + collection.getName());
		return collectioncategory;
	}

	public Category getRootVersionCategory(MediaArchive inArchive, LibraryCollection collection)
	{
		Searcher cats = inArchive.getSearcher("category");
		Searcher librarycolsearcher = inArchive.getSearcher("librarycollection");
		long revisions = collection.getCurentRevision();
		String libraryid = collection.get("library");
		Data library = inArchive.getData("library", libraryid);

		//Use the String path to load up the category from the hot folder import

		String id = collection.getId() + "_" + revisions;
		Category root = inArchive.getCategory(id);
		if (root == null)
		{
			root = (Category) cats.createNewData();
			String name = collection.getName();
			//			if( revisions > 0)
			//			{
			//				name = name + " " + revisions;
			//			}
			root.setName(name);
			root.setId(id);
			cats.saveData(root, null);

		}

		return root;

	}

	protected void copyAssets(ArrayList savelist, User inUser, MediaArchive inArchive, LibraryCollection inCollection, Category inParentSource, Category inDestinationCat, boolean skip)
	{

		Searcher assets = inArchive.getAssetSearcher();
		Searcher cats = inArchive.getSearcher("category");
		String id = inCollection.getId() + "_" + inParentSource.getId() + "_" + inCollection.getCurentRevision();
		Category copy = inDestinationCat.getChild(id);
		if (copy == null && !skip)
		{
			copy = (Category) cats.createNewData();
			copy.setName(inParentSource.getName());
			copy.setId(id);
			inDestinationCat.addChild(copy);
			cats.saveData(copy, null);
		}
		else
		{
			copy = inDestinationCat;
		}

		HitTracker assetlist = assets.fieldSearch("category-exact", inParentSource.getId());
		for (Iterator iterator = assetlist.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			Asset asset = (Asset) assets.loadData(hit);
			asset.addCategory(copy);
			savelist.add(asset);
		}
		for (Iterator iterator = inParentSource.getChildren().iterator(); iterator.hasNext();)
		{
			Category child = (Category) iterator.next();
			copyAssets(savelist, inUser, inArchive, inCollection, child, copy, false);

		}

	}

	//	public void removeCategoryFromCollection(MediaArchive inArchive, String inCollectionid, String inCategoryid)
	//	{
	//		Searcher librarycollectioncategorySearcher = inArchive.getSearcher("librarycollectioncategory");
	//
	//		Data data = librarycollectioncategorySearcher.query().match("librarycollection", inCollectionid).match("categoryid", inCategoryid).searchOne();
	//		librarycollectioncategorySearcher.delete(data, null);
	//
	//	}

	//	public Map loadFileSizes(WebPageRequest inReq, MediaArchive inArchive, String inCollectionid)
	//	{
	//		Map sizes = new HashMap();
	//		HitTracker assets = loadAssetsInCollection(inReq, inArchive, inCollectionid);
	//		long size = 0;
	//		for (Iterator iterator = assets.iterator(); iterator.hasNext();)
	//		{
	//			Data asset = (Data) iterator.next();
	//			Asset loaded = (Asset) inArchive.getAssetSearcher().loadData(asset);
	//			Page orig = inArchive.getOriginalDocument(loaded);
	//			size = size + orig.length();
	//
	//		}
	//		sizes.put("assetsize", size);
	//		Collection categories = loadCategoriesOnCollection(inArchive, inCollectionid);
	//
	//		if (categories != null)
	//		{
	//			for (Iterator iterator = categories.iterator(); iterator.hasNext();)
	//			{
	//				Data catData = (Data) iterator.next();
	//				Category cat = (Category) inArchive.getCategorySearcher().loadData(catData);
	//				String path = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/" + cat.getCategoryPath();
	//				long catsize = fileSize(inArchive, path);
	//				sizes.put(cat.getId(), catsize);
	//			}
	//		}
	//
	//		return sizes;
	//	}
	//
	//	protected long fileSize(MediaArchive inArchive, String inPath)
	//	{
	//		long size = 0;
	//		Collection children = inArchive.getPageManager().getChildrenPaths(inPath);
	//		for (Iterator iterator = children.iterator(); iterator.hasNext();)
	//		{
	//			String child = (String) iterator.next();
	//			Page page = inArchive.getPageManager().getPage(child);
	//			if (!page.isFolder())
	//			{
	//				size = size + page.length();
	//			}
	//			else
	//			{
	//				size = size + fileSize(inArchive, child);
	//			}
	//		}
	//		return size;
	//	}

	//	public HitTracker loadCategoriesOnCollection(MediaArchive inArchive, String inCollectionid)
	//	{
	//		Searcher librarycollectioncategorySearcher = inArchive.getSearcher("librarycollectioncategory");
	//		HitTracker hits = librarycollectioncategorySearcher.query().match("librarycollection", inCollectionid).search();
	//		if (hits.size() > 0)
	//		{
	//			List catids = new ArrayList();
	//			for (Iterator iterator = hits.iterator(); iterator.hasNext();)
	//			{
	//				Data libcat = (Data) iterator.next();
	//				catids.add(libcat.get("categoryid"));
	//			}
	//			HitTracker cats = inArchive.getCategorySearcher().query().orgroup("id", catids).search();
	//
	//			return cats;
	//		}
	//		return null;
	//	}

	/*
	 * public void loadCategoriesOnCollections(MediaArchive inArchive,
	 * Collection inCollections) { if (inCollections.size() > 0) { Map
	 * usercollections = new HashMap(); for (Iterator iterator =
	 * inCollections.iterator(); iterator.hasNext();) { LibraryCollection
	 * collection = (LibraryCollection) iterator.next();
	 * collection.clearCategories(); usercollections.put(collection.getId(),
	 * collection); }
	 * 
	 * Searcher librarycollectioncategorySearcher =
	 * inArchive.getSearcher("librarycollectioncategory"); HitTracker hits =
	 * librarycollectioncategorySearcher.query().orgroup("librarycollection",
	 * usercollections.keySet()).search(); if (hits.size() > 0) { for (Iterator
	 * iterator = hits.iterator(); iterator.hasNext();) { Data libcat = (Data)
	 * iterator.next(); LibraryCollection col = (LibraryCollection)
	 * usercollections.get(libcat.get("librarycollection"));
	 * col.addCategory(libcat.get("categoryid")); }
	 * 
	 * } } }
	 */

	/**
	 * Import process
	 * 
	 * public void importCollection(WebPageRequest inReq, MediaArchive
	 * inArchive, String inCollectionid) { //Find all the assets and move them
	 * to the library
	 * 
	 * //Get destination library Data collection =
	 * inArchive.getData("librarycollection", inCollectionid); Data library =
	 * inArchive.getData("library", collection.get("library"));
	 * 
	 * //Get library hot folder String librarysourcepath =
	 * library.get("folder");
	 * 
	 * if (librarysourcepath == null) { throw new OpenEditException("No folder
	 * set on library"); }
	 * 
	 * String collectionpath = librarysourcepath + "/" + collection.getName();
	 * 
	 * Searcher librarycollectionassetSearcher =
	 * inArchive.getSearcher("librarycollectionasset"); Collection colassets =
	 * librarycollectionassetSearcher.query().match("librarycollection",
	 * inCollectionid).search(); Map assetrecords = new HashMap(); for (Iterator
	 * iterator = colassets.iterator(); iterator.hasNext();) { Data colasset =
	 * (Data) iterator.next(); assetrecords.put(colasset.get("_parent"),
	 * colasset); }
	 * 
	 * //1: Check on existing assets Collection assets =
	 * loadAssetsInCollection(inReq, inArchive, inCollectionid); for (Iterator
	 * iterator = assets.iterator(); iterator.hasNext();) { //Check the existing
	 * assets for a move Data assetdata = (Data) iterator.next(); Asset asset =
	 * (Asset) inArchive.getAssetSearcher().loadData(assetdata); Asset copy =
	 * copyAssetIfNeeded(inReq, inArchive, asset, collectionpath); if (copy !=
	 * asset) { //Move the record Data colasset = (Data)
	 * assetrecords.get(assetdata.getId());
	 * librarycollectionassetSearcher.delete(colasset, null); colasset =
	 * librarycollectionassetSearcher.createNewData();
	 * colasset.setProperty("librarycollection", inCollectionid);
	 * colasset.setProperty("_parent", copy.getId());
	 * colasset.setProperty("asset", copy.getId()); //needed?
	 * librarycollectionassetSearcher.saveData(colasset, null); } } //2: Find
	 * all the categories and move them to the library Searcher
	 * librarycollectioncategorySearcher =
	 * inArchive.getSearcher("librarycollectioncategory"); HitTracker colcathits
	 * = librarycollectioncategorySearcher.query().match("librarycollection",
	 * inCollectionid).search();
	 * 
	 * List tosave = new ArrayList(); for (Iterator iterator =
	 * colcathits.iterator(); iterator.hasNext();) { Data catData = (Data)
	 * librarycollectioncategorySearcher.loadData((Data) iterator.next()); //
	 * if( "true".equals( catData.get("importedcat")) ) // { //
	 * log.info("Already imported this category" + catData.get("categoryid"));
	 * // continue; // } Category parentCat = (Category)
	 * inArchive.getCategory(catData.get("categoryid")); //2. Now move the old
	 * category parent to the new parent and save it. The assets will just need
	 * their sourcepath updated Collection catassets =
	 * inArchive.getAssetSearcher().query().match("category",
	 * parentCat.getId()).search();
	 * 
	 * String folder = parentCat.getCategoryPath(); //TODO: Turn off
	 * notifications String catpath = "/WEB-INF/data/" +
	 * inArchive.getCatalogId() + "/originals/" + folder; List childrenfiles =
	 * inArchive.getPageManager().getChildrenPaths(catpath);
	 * 
	 * //this is every asset and the children for (Iterator iterator2 =
	 * catassets.iterator(); iterator2.hasNext();) { Data assetdata = (Data)
	 * iterator2.next(); Asset asset = (Asset)
	 * inArchive.getAssetSearcher().loadData(assetdata); List catpaths = new
	 * ArrayList(); for (Iterator iterator3 = asset.getCategories().iterator();
	 * iterator3.hasNext();) { Category cat = (Category) iterator3.next();
	 * catpaths.add(cat.getCategoryPath()); } Collections.sort(catpaths);
	 * Collections.reverse(catpaths); String newpath = null; for (Iterator
	 * iterator3 = catpaths.iterator(); iterator3.hasNext();) { String path =
	 * (String) iterator3.next(); if (path.startsWith(folder)) { newpath =
	 * collectionpath + "/" + parentCat.getName() +
	 * path.substring(folder.length()); break; } }
	 * 
	 * if (newpath == null) { log.error("someone deleted cats" + folder +
	 * asset.getId()); continue; } //Remove the old category? Asset
	 * existingasset = (Asset) inArchive.getAssetSearcher().loadData(assetdata);
	 * Asset copy = copyAssetIfNeeded(inReq, inArchive, existingasset, newpath);
	 * if (copy != existingasset) { inArchive.saveAsset(copy, null);
	 * 
	 * // Data old = (Data)assetrecords.get(existingasset.getId()); //
	 * librarycollectionassetSearcher.delete(old, null);
	 * 
	 * Data found = librarycollectionassetSearcher.createNewData();
	 * //found.setSourcePath(libraryid + "/" + librarycollection);
	 * found.setProperty("librarycollection", inCollectionid);
	 * found.setProperty("asset", copy.getId()); //legacy
	 * found.setProperty("_parent", copy.getId()); tosave.add(found); } } for
	 * (Iterator iterator2 = childrenfiles.iterator(); iterator2.hasNext();) {
	 * String path = (String) iterator2.next(); String dest = collectionpath +
	 * "/" + PathUtilities.extractFileName(path); ContentItem existing =
	 * inArchive.getPageManager().getRepository().getStub(dest); if
	 * (!existing.exists()) { ContentItem source =
	 * inArchive.getPageManager().getRepository().getStub(path);
	 * inArchive.getPageManager().getRepository().copy(source, existing); } }
	 * parentCat.setProperty("foldertype", "10");
	 * inArchive.getCategorySearcher().saveData(parentCat, null); //Save the
	 * cateory //catData.setProperty("importedcat","true");
	 * //librarycollectioncategorySearcher.saveData(catData, null); //Marked as
	 * imported or remove the category?
	 * librarycollectionassetSearcher.saveAllData(tosave, null); tosave.clear();
	 * } // assets = loadAssetsInCollection(inReq, inArchive, inCollectionid);
	 * //Get all the assets in collection
	 * 
	 * }
	 * 
	 * 
	 * 
	 * public String exportCollectionTo(WebPageRequest inReq, MediaArchive
	 * inArchive, String inCollectionid, String inLibraryid) { //move the
	 * collection root folder //TODO: Check for bool importCollection(inReq,
	 * inArchive, inCollectionid); //make copies of everything
	 * 
	 * //grab all the assets and update thier sourcepath and move them with
	 * images // collection.setProperty("library",inLibraryid); //
	 * inArchive.getSearcher("librarycollection").saveData(collection,
	 * inReq.getUser()); Data collection =
	 * inArchive.getData("librarycollection", inCollectionid);
	 * 
	 * //Get destination library Data oldlibrary = inArchive.getData("library",
	 * collection.get("library")); Data newlibrary =
	 * inArchive.getData("library", inLibraryid);
	 * 
	 * //Get library hot folder String librarysourcepath =
	 * newlibrary.get("folder");
	 * 
	 * if (librarysourcepath == null) { throw new OpenEditException("No folder
	 * set on library"); } String oldcollectionpath = oldlibrary.get("folder") +
	 * "/" + collection.getName(); String path =
	 * inArchive.getCatalogSettingValue("movecollectionpath");
	 * 
	 * Map args = new HashMap(); args.put("collection", collection);
	 * args.put("oldlibrary", oldlibrary); args.put("newlibrary", newlibrary);
	 * args.put("splitname", makeChunks(collection.getName())); String date =
	 * DateStorageUtil.getStorageUtil().formatDateObj(new Date(), "yyyy/MM");
	 * //TODO: Use DataStorage args.put("year", date.substring(0, 4));
	 * args.put("month", date.substring(5, 7)); args.put("user",
	 * inReq.getUser());
	 * 
	 * //librarysourcepath + "/" + collection.getName();
	 * //${newlibrary.folder}/${year}/${splitname} String collectionpath =
	 * inArchive.getReplacer().replace(path, args);
	 * 
	 * //Move this folder and update all the sourcepaths on assets. Also add a
	 * new Category Collection assets = loadAssetsInCollection(inReq, inArchive,
	 * inCollectionid);
	 * 
	 * for (Iterator iterator = assets.iterator(); iterator.hasNext();) { Data
	 * data = (Data) iterator.next();
	 * 
	 * //Take old path and replace it in the sourcepath Asset existingasset =
	 * (Asset) inArchive.getAssetSearcher().loadData(data); String newsourcepath
	 * = collectionpath +
	 * existingasset.getSourcePath().substring(oldcollectionpath.length());
	 * 
	 * String oldpathprimary = existingasset.getSourcePath(); String oldpath =
	 * "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/" +
	 * oldpathprimary; Page oldpage =
	 * inArchive.getPageManager().getPage(oldpath); if (!oldpage.exists()) {
	 * log.info("Asset missing " + oldpath); continue; } String newpath =
	 * "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/" +
	 * newsourcepath; Page newpage =
	 * inArchive.getPageManager().getPage(newpath); if (newpage.exists()) {
	 * log.info("Duplicated entry " + newpath); continue; //Put into a weird sub
	 * directory? }
	 * 
	 * Map props = new HashMap(); try { props.put("absolutepath",
	 * newpage.getContentItem().getAbsolutePath());
	 * inArchive.fireMediaEvent("savingoriginal", "asset",
	 * existingasset.getSourcePath(), props, inReq.getUser());
	 * existingasset.setSourcePath(newsourcepath);
	 * inArchive.getAssetSearcher().saveData(existingasset, inReq.getUser());
	 * //avoid Hot folder detection inArchive.getPageManager().movePage(oldpage,
	 * newpage); Page oldthumbs =
	 * inArchive.getPageManager().getPage("/WEB-INF/data/" +
	 * inArchive.getCatalogId() + "/generated/" + oldpathprimary); Page
	 * newthumbs = inArchive.getPageManager().getPage("/WEB-INF/data/" +
	 * inArchive.getCatalogId() + "/generated/" + newsourcepath); if
	 * (oldthumbs.exists()) { inArchive.getPageManager().movePage(oldthumbs,
	 * newthumbs); } } finally {
	 * inArchive.fireMediaEvent("savingoriginalcomplete", "asset",
	 * existingasset.getSourcePath(), props, inReq.getUser()); } }
	 * 
	 * //Clean up Page leftovers =
	 * inArchive.getPageManager().getPage("/WEB-INF/data/" +
	 * inArchive.getCatalogId() + "/generated/" + oldcollectionpath); if
	 * (leftovers.exists()) { Page dest =
	 * inArchive.getPageManager().getPage("/WEB-INF/data/" +
	 * inArchive.getCatalogId() + "/generated/" + collectionpath);
	 * inArchive.getPageManager().movePage(leftovers, dest); } leftovers =
	 * inArchive.getPageManager().getPage("/WEB-INF/data/" +
	 * inArchive.getCatalogId() + "/originals/" + oldcollectionpath); if
	 * (leftovers.exists()) { Page dest =
	 * inArchive.getPageManager().getPage("/WEB-INF/data/" +
	 * inArchive.getCatalogId() + "/originals/" + collectionpath);
	 * inArchive.getPageManager().movePage(leftovers, dest); }
	 * //inArchive.getPageManager().movePage(oldthumbs, newthumbs);
	 * 
	 * collection.setValue("library", newlibrary.getId());
	 * inArchive.getSearcher("librarycollection").saveData(collection, null);
	 * 
	 * return collectionpath;
	 * 
	 * } protected String makeChunks(String inName) { int split = 3; if
	 * (inName.length() < 3) { return inName; } String fixed =
	 * inName.replace("-", ""); fixed = fixed.substring(0, 3) + "/" + inName;
	 * return fixed; }
	 */
	/*
	 * protected Asset copyAssetIfNeeded(WebPageRequest inReq, MediaArchive
	 * inArchive, Asset existingasset, String folderpath) { //Change sourcepath
	 * String oldsourcepath = existingasset.getSourcePath(); if
	 * (oldsourcepath.startsWith(folderpath)) { return existingasset; } String
	 * sourcepath = null; if (existingasset.isFolder() ||
	 * existingasset.getPrimaryFile() == null) { sourcepath = folderpath +
	 * oldsourcepath.substring(oldsourcepath.lastIndexOf('/')); } else {
	 * sourcepath = folderpath + "/" + existingasset.getPrimaryFile(); } //
	 * String oldpathprimary = existingasset.getSourcePath(); // if(
	 * existingasset.isFolder() ) // { // oldpathprimary = oldpathprimary + "/"
	 * + existingasset.getPrimaryFile(); // } // String dest = collectionpath +
	 * "/" + existingasset.getPrimaryFile(); Asset newasset =
	 * inArchive.getAssetBySourcePath(sourcepath);
	 * 
	 * if (newasset != null) { log.info("Asset already imported " + sourcepath);
	 * return newasset; } //Check for duplicates
	 * 
	 * //use Categories for multiple files //These are single files with
	 * conflict checking newasset =
	 * inArchive.getAssetEditor().copyAsset(existingasset, null);
	 * newasset.setFolder(existingasset.isFolder());
	 * newasset.setSourcePath(sourcepath); newasset.clearCategories(); Category
	 * newparent =
	 * inArchive.getCategoryArchive().createCategoryTree(sourcepath);
	 * newasset.addCategory(newparent);
	 * 
	 * String oldpath = "/WEB-INF/data/" + inArchive.getCatalogId() +
	 * "/originals/" + oldsourcepath; Page oldpage =
	 * inArchive.getPageManager().getPage(oldpath); boolean copyorig = true; if
	 * (!oldpage.exists()) { log.info("Original missing   " + oldpath); copyorig
	 * = false; } String newpath = "/WEB-INF/data/" + inArchive.getCatalogId() +
	 * "/originals/" + sourcepath; Page newpage =
	 * inArchive.getPageManager().getPage(newpath); if (newpage.exists()) {
	 * log.info("Duplicated entry  " + newpath); copyorig = false; } if
	 * (copyorig) { Map props = new HashMap(); try { props.put("absolutepath",
	 * newpage.getContentItem().getAbsolutePath());
	 * inArchive.fireMediaEvent("savingoriginal", "asset",
	 * newasset.getSourcePath(), props, inReq.getUser());
	 * inArchive.getAssetSearcher().saveData(newasset, inReq.getUser()); //avoid
	 * Hot folder detection inArchive.getPageManager().copyPage(oldpage,
	 * newpage); Page oldthumbs =
	 * inArchive.getPageManager().getPage("/WEB-INF/data/" +
	 * inArchive.getCatalogId() + "/generated/" + oldsourcepath); Page newthumbs
	 * = inArchive.getPageManager().getPage("/WEB-INF/data/" +
	 * inArchive.getCatalogId() + "/generated/" + sourcepath); if
	 * (oldthumbs.exists()) { inArchive.getPageManager().copyPage(oldthumbs,
	 * newthumbs); } } finally {
	 * inArchive.fireMediaEvent("savingoriginalcomplete", "asset",
	 * newasset.getSourcePath(), props, inReq.getUser()); } } return newasset; }
	 */
	public Data loadUserLibrary(MediaArchive inArchive, UserProfile inProfile)
	{
		User user = inProfile.getUser();
		Data userlibrary = inArchive.getData("library", user.getId());
		if (userlibrary != null)
		{
			return userlibrary;
		}

		userlibrary = inArchive.getSearcher("library").createNewData();
		userlibrary.setId(user.getUserName());
		userlibrary.setName(user.getScreenName());

		String folder = "Users/" + user.getScreenName();

		Category librarynode = inArchive.createCategoryPath(folder);
		((MultiValued) librarynode).addValue("viewusers", user.getId());
		inArchive.getCategorySearcher().saveData(librarynode);
		//reload profile?
		inProfile.getViewCategories().add(userlibrary.getId()); //Make sure I am in the list of users for the library

		userlibrary.setValue("categoryid", librarynode.getId());
		inArchive.getSearcher("library").saveData(userlibrary, null);

		return userlibrary;
	}

	public void exportCollection(MediaArchive inMediaArchive, String inCollectionid, String inFolder)
	{
		//Data collection = inMediaArchive.getData("librarycollection", inCollectionid);
		AssetUtilities utilities = inMediaArchive.getAssetImporter().getAssetUtilities();
		Category root = getRootCategory(inMediaArchive, inCollectionid);
		ContentItem childtarget = inMediaArchive.getPageManager().getRepository().getStub(inFolder);
		utilities.exportCategoryTree(inMediaArchive, root, childtarget);

	}

	public void restoreSnapshot(WebPageRequest inReq, User inUser, MediaArchive inArchive, String inCollectionid, String inRevision, String inNote)
	{
		Searcher librarycolsearcher = inArchive.getSearcher("librarycollection");
		LibraryCollection collection = (LibraryCollection) librarycolsearcher.searchById(inCollectionid);
		if (collection == null)
		{
			return;
		}

		snapshotCollection(inReq, inUser, inArchive, inCollectionid, inNote);
		Category newroot = getRootCategory(inArchive, inCollectionid);
		CategorySearcher searcher = inArchive.getCategorySearcher();
		searcher.deleteCategoryTree(newroot);
		newroot = getRootCategory(inArchive, inCollectionid);

		Category revisionroot = getRevisionRoot(inArchive, inCollectionid, inRevision);

		ArrayList list = new ArrayList();
		copyAssets(list, inUser, inArchive, collection, revisionroot, newroot, true);// will actually create librarycollectionasset entries
		Searcher assets = inArchive.getAssetSearcher();
		assets.saveAllData(list, null);

	}

	protected Category getRevisionRoot(MediaArchive inArchive, String inCollectionid, String inRevision)
	{
		Searcher cats = inArchive.getSearcher("category");
		LibraryCollection collection = getLibraryCollection(inArchive, inCollectionid);
		String id = collection.getId() + "_" + inRevision;
		Category revisionroot = (Category) cats.searchById(id);
		return revisionroot;

	}

	public LibraryCollection getLibraryCollection(MediaArchive inArchive, String inCollectionid)
	{
		Searcher librarycolsearcher = inArchive.getSearcher("librarycollection");
		LibraryCollection collection = (LibraryCollection) librarycolsearcher.searchById(inCollectionid);
		return collection;
	}

	public LibraryCollection findCollectionForCategory(Category inCategory)
	{
		Searcher librarycolsearcher = getMediaArchive().getSearcher("librarycollection");
		Collection<Category> parents = inCategory.getParentCategories();
		Data hit = librarycolsearcher.query().orgroup("rootcategory",parents).searchOne();
		return (LibraryCollection)hit; 
	}
	
	protected MediaArchive getMediaArchive()
	{
		MediaArchive archive = (MediaArchive)getModuleManager().getBean(getCatalogId(),"mediaArchive");
		return archive;
	}
}
