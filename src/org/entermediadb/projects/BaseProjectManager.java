package org.entermediadb.projects;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.entermediadb.asset.AssetUtilities;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.ChunkySourcePathCreator;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.FilterNode;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.page.Page;
import org.openedit.profile.UserProfile;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.PathUtilities;

public class BaseProjectManager implements ProjectManager
{
	private static final Log log = LogFactory.getLog(BaseProjectManager.class);
	
	protected SearcherManager fieldSearcherManager;
	protected String fieldCatalogId;
	protected AssetUtilities fieldAssetUtilities;
	private int COPY = 1;
	private int MOVE = 2;
	
	public AssetUtilities getAssetUtilities()
	{
		return fieldAssetUtilities;
	}

	public void setAssetUtilities(AssetUtilities inAssetUtilities)
	{
		fieldAssetUtilities = inAssetUtilities;
	}

	/* (non-Javadoc)
	 * @see model.projects.ProjectManager#getCatalogId()
	 */
	@Override
	public String getCatalogId()
	{
		return fieldCatalogId;
	}
	
	/* (non-Javadoc)
	 * @see model.projects.ProjectManager#setCatalogId(java.lang.String)
	 */
	@Override
	public void setCatalogId(String inCatId)
	{
		fieldCatalogId = inCatId;
	}
	
	
	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.push.PushManager#setSearcherManager(org.openedit.data.SearcherManager)
	 */
	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}
	
	public Data getCurrentLibrary( UserProfile inProfile)
	{
		if( inProfile != null)
		{
			String lastselectedid = inProfile.get("last_selected_library" );
			if( lastselectedid != null)
			{
				Data library = getSearcherManager().getData(getCatalogId(), "library", lastselectedid );
				return library;
			}
		}
		return null;
	}
	
	public Collection<UserCollection> loadCollections(WebPageRequest inReq, MediaArchive inArchive)
	{
		//get a library
		Collection<UserCollection> usercollections = (Collection<UserCollection>)inReq.getPageValue("usercollections");
		if( usercollections != null)
		{
			return usercollections;
		}
		
		Data library = getCurrentLibrary(inReq.getUserProfile());
		if( library == null)
		{
			library = loadUserLibrary(inArchive, inReq.getUserProfile());
		}
		if( library != null)
		{
			inReq.putPageValue("selectedlibrary",library);
			
			Searcher assetsearcher = getSearcherManager().getSearcher(getCatalogId(),"asset");
			
			HitTracker hits = assetsearcher.query().match("libraries",library.getId()).named("sidebar").search(inReq);
			if(hits != null)
			{
				int assetsize = hits.size();
				inReq.putPageValue("librarysize",assetsize);
			}
			Searcher searcher = getSearcherManager().getSearcher(getCatalogId(),"librarycollection");
			String reloadcollectoin = inReq.getRequestParameter("reloadcollection");
			HitTracker allcollections = searcher.query().exact("library",library.getId()).sort("name").named("sidebar").search();

			//Show all the collections for a library
			inReq.putPageValue("allcollections", allcollections);
			
			//enable filters to show the asset count on each collection node
			FilterNode collectionhits = null;
			if( allcollections.size() > 0 ) //May not have any collections
			{
				Searcher collectionassetsearcher = getSearcherManager().getSearcher(getCatalogId(),"librarycollectionasset");
				
				//Build list of ID's
				List ids = new ArrayList(allcollections.size());
				for (Iterator iterator = allcollections.iterator(); iterator.hasNext();)
				{
					Data collection = (Data) iterator.next();
					ids.add( collection.getId() );
				}
				if(ids.size() > 0)
				{
					HitTracker collectionassets = collectionassetsearcher.query().orgroup("librarycollection",ids).named("sidebar").search(); //todo: Cache?
					if(collectionassets != null && collectionassets.size() > 0) //No assets found at all
					{
						collectionhits = collectionassets.findFilterNode("librarycollection");
					}
				}
			}			
			usercollections = loadUserCollections(allcollections, collectionhits);
			inReq.putPageValue("usercollections", usercollections);
			return usercollections;
		}
		return Collections.EMPTY_LIST;
				
		//search
		//Searcher searcher = getSearcherManager().getSearcher(getMediaArchive().getCatalogId(),"librarycollection") )
		//HitTracker labels = searcher.query().match("library",$library.getId()).sort("name").search() )
		
	}
	
	public Collection<UserCollection> loadOpenCollections(WebPageRequest inReq)
	{
		//get a library
		//inReq.putPageValue("selectedlibrary",library);

		
		Collection<UserCollection> usercollections = (Collection<UserCollection>)inReq.getPageValue("usercollections");
		if( usercollections != null)
		{
			return usercollections;
		}
		//enable filters to show the asset count on each collection node
		
	  	Collection opencollections = inReq.getUserProfile().getValues("opencollections");
	  	if( opencollections == null)
	  	{
	  		return Collections.EMPTY_LIST;
	  	}

		FilterNode collectionhits = null;
		if( opencollections.size() > 0 ) //May not have any collections
		{
			Searcher collectionassetsearcher = getSearcherManager().getSearcher(getCatalogId(),"librarycollectionasset");
			
			//Build list of ID's
			HitTracker collectionassets = collectionassetsearcher.query().orgroup("librarycollection",opencollections).named("sidebar").search(); //todo: Cache?
			if(collectionassets != null && collectionassets.size() > 0) //No assets found at all
			{
				collectionhits = collectionassets.findFilterNode("librarycollection");
			}
		}
		Collection collections = getSearcherManager().getSearcher(getCatalogId(),"librarycollection").query().orgroup("id", opencollections).search(inReq);
		usercollections = loadUserCollections(collections, collectionhits);
		inReq.putPageValue("usercollections", usercollections);
		return usercollections;

				
		//search
		//Searcher searcher = getSearcherManager().getSearcher(getMediaArchive().getCatalogId(),"librarycollection") )
		//HitTracker labels = searcher.query().match("library",$library.getId()).sort("name").search() )
		
	}
	
	protected Collection<UserCollection> loadUserCollections(Collection<Data> allcollections, FilterNode collectionhits)
	{
		List usercollections = new ArrayList(allcollections.size());
		for (Data collection : allcollections)
		{
			UserCollection uc = new UserCollection();
			uc.setCollection(collection);
			if( collectionhits != null)
			{
				int assetcount = collectionhits.getCount(collection.getId());
				uc.setAssetCount(assetcount);
			}
			usercollections.add(uc);
		}
		return usercollections;
	}
	public void addAssetToCollection(MediaArchive archive, String libraryid, String collectionid, HitTracker assets)
	{
		if( libraryid != null)
		{
			addAssetToLibrary(archive, libraryid, assets);
		}
		addAssetToCollection(archive, collectionid, assets);
	}
	public void addAssetToCollection(MediaArchive archive, String librarycollection, HitTracker assets)
	{
		Searcher librarycollectionassetSearcher = archive.getSearcher("librarycollectionasset");
	
		List tosave = new ArrayList();
		assets.setHitsPerPage(200);
		for(int i=0;i < assets.getTotalPages();i++)
		{
			assets.setPage(i+1);
			Set assetids = new HashSet();
			for(Object hit: assets.getPageOfHits())
			{
				Data asset = (Data)hit;
				assetids.add(asset.getId());
			}
			Collection existing = librarycollectionassetSearcher.query().match("librarycollection", librarycollection).orgroup("_parent", assetids).search();
			for (Iterator iterator = existing.iterator(); iterator.hasNext();)
			{
				Data collasset = (Data)iterator.next();
				assetids.remove(collasset.get("_parent"));
			}
			for (Iterator iterator = assetids.iterator(); iterator.hasNext();)
			{
				String assetid = (String)iterator.next();
				Data found = librarycollectionassetSearcher.createNewData();
				//found.setSourcePath(libraryid + "/" + librarycollection);
				found.setProperty("librarycollection", librarycollection);
				found.setProperty("asset", assetid);
				found.setProperty("_parent", assetid);

				tosave.add(found);
				if( tosave.size() > 200)
				{
					librarycollectionassetSearcher.saveAllData(tosave,null);
					tosave.clear();
				}
				//log.info("Saved " + found.getId());
			}
		}
		librarycollectionassetSearcher.saveAllData(tosave,null);
		
	}
	public void addAssetToCollection(MediaArchive archive, String libraryid, String collectionid, String assetid)
	{
		addAssetToLibrary(archive, libraryid, assetid);
		//String librarycollection = inReq.getRequestParameter("librarycollection");
		addAssetToCollection(archive, collectionid, assetid);
	}
	public void addAssetToCollection(MediaArchive archive, String collectionid, String assetid)
	{
		Searcher librarycollectionassetSearcher = archive.getSearcher("librarycollectionasset");
	
		Data found = librarycollectionassetSearcher.query().match("librarycollection", collectionid).match("_parent", assetid).searchOne();
	
		if (found == null)
		{
			found = librarycollectionassetSearcher.createNewData();
			//found.setSourcePath(libraryid + "/" + collectionid);
			found.setProperty("librarycollection", collectionid);
			found.setProperty("_parent", assetid);
			librarycollectionassetSearcher.saveData(found, null);
			log.info("Saved " + found.getId());
		}
	}
	public void addAssetToLibrary(MediaArchive archive, String libraryid, HitTracker assets)
	{
		List tosave = new ArrayList();
		for(Object data: assets)
		{
			//TODO: Skip loading?
			MultiValued toadd = (MultiValued)data;
			Collection libraries = toadd.getValues("libraries");
			if ( libraries != null && libraries.contains(libraryid))
			{
				continue;
			}
			Asset asset = (Asset)archive.getAssetSearcher().loadData(toadd);
		
			if (asset != null && !asset.getLibraries().contains(libraryid))
			{
				asset.addLibrary(libraryid);
				tosave.add(asset);
				if( tosave.size() > 500)
				{
					archive.saveAssets(tosave);
					tosave.clear();
				}
			}
		}
		archive.saveAssets(tosave);
		
	}

	public void addAssetToLibrary(MediaArchive archive, String libraryid, String assetid)
	{
		Asset asset = archive.getAsset(assetid);
	
		if (asset != null && !asset.getLibraries().contains(libraryid))
		{
			asset.addLibrary(libraryid);
			archive.saveAsset(asset, null);	
		}
	}
	
	public HitTracker loadAssetsInLibrary(Data inLibrary,  MediaArchive archive, WebPageRequest inReq)
	{
		HitTracker hits = archive.getAssetSearcher().query().match("libraries",inLibrary.getId()).search(inReq);
		return hits;
	}
	
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
		if( assetsearch == null)
		{
			assetsearch = searcher.createSearchQuery();
		}
		assetsearch.addChildFilter("librarycollectionasset","librarycollection",collectionid);
		all = archive.getAssetSearcher().search(assetsearch);
		
/*		
		
//			Collection<String> ids = loadAssetIdsInCollection(inReq, archive, collectionid );
//			//Do an asset search with permissions, showing only the assets on this collection
//			all = archive.getAssetSearcher().query().match("id", "*").not("editstatus", "7").search();
//			all.setSelections(ids);
//			all.setShowOnlySelected(true);
			//log.info("Searching for assets " + all.size() + " ANND " + ids.size() );
			//create script that syncs up the assets that have been removed
			if( all.size() != ids.size() )
			{
				//Collection<String> ids = loadAssetIdsInCollection(archive, collectionid );
				//Some assets got deleted, lets remove them from the collection
				Set<String> extras = new HashSet(ids);
				for (Object hit : all)
				{
					Data data = (Data)hit;
					extras.remove(data.getId());
				}
				//log.info("remaining " + extras );
				Searcher collectionassetsearcher = archive.getSearcher("librarycollectionasset");
				for (String id : extras)
				{
					Data toremove = collectionassetsearcher.query().match("asset",id).match("librarycollection", collectionid).searchOne();
					if( toremove != null)
					{
						collectionassetsearcher.delete(toremove, null);
					}
				}
			}
		*/	
//		}
		String hpp = inReq.getRequestParameter("page");
		if( hpp != null)
		{
			all.setPage(Integer.parseInt( hpp ) );
		}
		UserProfile usersettings = (UserProfile) inReq.getUserProfile();
		if( usersettings != null )
		{
			all.setHitsPerPage(usersettings.getHitsPerPageForSearchType("asset"));
		}
		all.getSearchQuery().setProperty("collectionid", collectionid);
		all.getSearchQuery().setHitsName("collectionassets");
		return all;
	}
	//TODO: delete this
	private Collection<String> loadAssetIdsInCollection(WebPageRequest inReq, MediaArchive archive, String inCollectionId)
	{
		Searcher librarycollectionassetSearcher = archive.getSearcher("librarycollectionasset");
		HitTracker found = librarycollectionassetSearcher.query().match("librarycollection", inCollectionId).search(inReq);
		Collection<String> ids = new ArrayList();
		for(Object hit : found)
		{
			Data data = (Data)hit;
			String id = data.get("_parent");
			if( id != null)
			{
				ids.add(id);
			}
		}
		return ids;
	}
	
	public boolean addUserToLibrary( MediaArchive archive, Data inLibrary, User inUser) 
	{
		Searcher searcher = archive.getSearcher("libraryusers");
		Data found = searcher.query().match("userid",inUser.getId()).match("_parent", inLibrary.getId()).searchOne();	
		if( found == null )
		{
			found = searcher.createNewData();
			found.setProperty("userid",inUser.getId());
			found.setProperty("_parent",inLibrary.getId());
			searcher.saveData(found,null);
			return true;
		}				
		return false;	
	}

	public void removeAssetFromLibrary(MediaArchive inArchive, String inLibraryid, HitTracker inAssets)
	{
		Searcher librarycollectionsearcher = inArchive.getSearcher("librarycollection");
		HitTracker<Data> collections = (HitTracker<Data>)librarycollectionsearcher.query().match("library",inLibraryid).search();
		for(Object collection : collections) 
		{
			removeAssetFromCollection(inArchive,((Data)collection).getId(),inAssets);
		}
		for(Object toadd: inAssets)
		{
			Asset asset = (Asset)inArchive.getAssetSearcher().loadData((Data)toadd);
		
			if (asset != null && asset.getLibraries().contains(inLibraryid))
			{
				asset.removeLibrary(inLibraryid);
				inArchive.saveAsset(asset, null);
			}
		}
	}

	public void removeAssetFromCollection(MediaArchive inArchive, String inCollectionid, HitTracker inAssets)
	{
			Searcher librarycollectionassetSearcher = inArchive.getSearcher("librarycollectionasset");
		
			List tosave = new ArrayList();
			for(Object hit: inAssets)
			{
				Data asset = (Data)hit;
				Data found = librarycollectionassetSearcher.query().match("librarycollection", inCollectionid).match("_parent", asset.getId()).searchOne();
			
				if (found != null)
				{
					librarycollectionassetSearcher.delete(found,null);
				}
			}
	}
	public Collection<UserCollection> loadRecentCollections(WebPageRequest inReq)
	{
		//enable filters to show the asset count on each collection node
		UserProfile profile = inReq.getUserProfile();
		Searcher librarycollectionsearcher = getSearcherManager().getSearcher(getCatalogId(),"librarycollection");
		Collection combined = profile.getCombinedLibraries();
		if( inReq.getUser().isInGroup("administrators"))
		{
			combined.clear();
			combined.add("*");
		}
		if(combined.size() == 0 )
		{
			return Collections.EMPTY_LIST;
		}
		HitTracker allcollections = librarycollectionsearcher.query().orgroup("library",profile.getCombinedLibraries()).sort("name").named("sidebar").search(inReq);
		FilterNode collectionhits = null;
		if( allcollections.size() > 0 ) //May not have any collections
		{
			Searcher collectionassetsearcher = getSearcherManager().getSearcher(getCatalogId(),"librarycollectionasset");
			
			//Build list of ID's
			List ids = new ArrayList(allcollections.size());
			for (Iterator iterator = allcollections.iterator(); iterator.hasNext();)
			{
				Data collection = (Data) iterator.next();
				ids.add( collection.getId() );
			}
			if(ids.size() > 0)
			{
				HitTracker collectionassets = collectionassetsearcher.query().orgroup("librarycollection",ids).sort("recorddate").named("homecollections").search(inReq); 
				if(collectionassets != null && collectionassets.size() > 0) //No assets found at all
				{
					collectionhits = collectionassets.findFilterNode("librarycollection");
				}
			}
		}
		Collection<UserCollection> usercollections = loadUserCollections(allcollections, collectionhits);
		inReq.putPageValue("usercollections", usercollections);
		return usercollections;
	}

	@Override
	public Data addCategoryToCollection(User inUser, MediaArchive inArchive, String inCollectionid, String inCategoryid)
	{
		if(inCategoryid != null)
		{
			Searcher librarycollectioncategorySearcher = inArchive.getSearcher("librarycollectioncategory");
			Data collection = null;
			Data data = null;
			Searcher librarycolsearcher = inArchive.getSearcher("librarycollection");
			if( inCollectionid == null)
			{
				collection = librarycolsearcher.createNewData();
				collection.setValue("library", inUser.getId()); //Make one now?
				collection.setValue("owner", inUser.getId());
				Category cat = inArchive.getCategory(inCategoryid);
				collection.setName(cat.getName());
				librarycolsearcher.saveData(collection,null);
				inCollectionid = collection.getId();
			}
			else
			{
				collection = (Data)librarycolsearcher.searchById(inCollectionid);
				data = librarycollectioncategorySearcher.query().match("librarycollection", inCollectionid).match("categoryid", inCategoryid).searchOne();
			}			
			if(data == null)
			{
				Data newfolder = librarycollectioncategorySearcher.createNewData();
				newfolder.setProperty("librarycollection", inCollectionid);
				newfolder.setProperty("categoryid", inCategoryid);
				librarycollectioncategorySearcher.saveData(newfolder, null);
			}
			return collection;
		}
		return null;
	}
	
	@Override
	public void removeCategoryFromCollection(MediaArchive inArchive, String inCollectionid, String inCategoryid)
	{
		Searcher librarycollectioncategorySearcher = inArchive.getSearcher("librarycollectioncategory");
		
		Data data = librarycollectioncategorySearcher.query().match("librarycollection", inCollectionid).match("categoryid", inCategoryid).searchOne();
		librarycollectioncategorySearcher.delete(data, null);

	}
	
	public Map loadFileSizes(WebPageRequest inReq, MediaArchive inArchive, String inCollectionid)
	{
		Map sizes = new HashMap();
		HitTracker assets = loadAssetsInCollection(inReq,inArchive,inCollectionid);
		long size = 0;
		for (Iterator iterator = assets.iterator(); iterator.hasNext();)
		{
			Data asset = (Data) iterator.next();
			Asset loaded = (Asset)inArchive.getAssetSearcher().loadData(asset);
			Page orig = inArchive.getOriginalDocument(loaded);
			size = size + orig.length();
			
		}
		sizes.put("assetsize", size);
		Collection categories  = loadCategoriesOnCollection(inArchive,inCollectionid);
		
		if(categories != null){
			for (Iterator iterator = categories.iterator(); iterator.hasNext();)
			{
				Data catData = (Data) iterator.next();
				Category cat = (Category) inArchive.getCategorySearcher().loadData(catData);
				String path = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/" + cat.getCategoryPath();
				long catsize = fileSize(inArchive,path);
				sizes.put(cat.getId(), catsize);
			}
		}
		
		
		return sizes;
	}

	protected long fileSize( MediaArchive inArchive, String inPath)
	{
		long size = 0;
		Collection children = inArchive.getPageManager().getChildrenPaths(inPath);
		for (Iterator iterator = children.iterator(); iterator.hasNext();)
		{
			String child = (String) iterator.next();
			Page page = inArchive.getPageManager().getPage(child);
			if( !page.isFolder() )
			{
				size = size + page.length();
			}
			else
			{
				size = size + fileSize(inArchive,child);
			}
		}
		return size;
	}

	public HitTracker loadCategoriesOnCollection(MediaArchive inArchive, String inCollectionid)
	{
		Searcher librarycollectioncategorySearcher = inArchive.getSearcher("librarycollectioncategory");
		HitTracker hits = librarycollectioncategorySearcher.query().match("librarycollection", inCollectionid).search();
		if( hits.size() > 0)
		{
			List catids = new ArrayList();
			for (Iterator iterator = hits.iterator(); iterator.hasNext();)
			{
				Data libcat = (Data) iterator.next();
				catids.add(libcat.get("categoryid"));
			}
			HitTracker cats = inArchive.getCategorySearcher().query().orgroup("id", catids).search();
			
			return cats;
		}	
		return null;
	}

	public void loadCategoriesOnCollections(MediaArchive inArchive, Collection inCollections)
	{
		if(inCollections.size()>0)
		{
			Map usercollections = new HashMap();
			for (Iterator iterator = inCollections.iterator(); iterator.hasNext();)
			{
				UserCollection collection = (UserCollection) iterator.next();
				collection.clearCategories();
				usercollections.put(collection.getId(), collection );
			}
			
			Searcher librarycollectioncategorySearcher = inArchive.getSearcher("librarycollectioncategory");
			HitTracker hits = librarycollectioncategorySearcher.query().orgroup("librarycollection", usercollections.keySet()).search();
			if( hits.size() > 0)
			{
				for (Iterator iterator = hits.iterator(); iterator.hasNext();)
				{
					Data libcat = (Data) iterator.next();
					UserCollection col = (UserCollection)usercollections.get(libcat.get("librarycollection"));
					col.addCategory(libcat.get("categoryid"));
				}
				
			}
		}
	}
	/**
	 * Import process
	 */
	public void importCollection(WebPageRequest inReq, MediaArchive inArchive, String inCollectionid)
	{
		//Find all the assets and move them to the library
		
		//Get destination library 
		Data collection = inArchive.getData("librarycollection", inCollectionid);
		Data library = inArchive.getData("library", collection.get("library"));
		
		//Get library hot folder
		String librarysourcepath = library.get("folder");
		
		if( librarysourcepath == null)
		{
			throw new OpenEditException("No folder set on library");
		}

		String collectionpath = librarysourcepath + "/" + collection.getName(); 

		Searcher librarycollectionassetSearcher = inArchive.getSearcher("librarycollectionasset");
		Collection colassets = librarycollectionassetSearcher.query().match("librarycollection", inCollectionid).search();
		Map assetrecords = new HashMap();
		for (Iterator iterator = colassets.iterator(); iterator.hasNext();)
		{
			Data colasset = (Data) iterator.next();
			assetrecords.put(colasset.get("_parent"),colasset);
		}
		
		//1: Check on existing assets
		Collection assets = loadAssetsInCollection(inReq, inArchive, inCollectionid);
		for (Iterator iterator = assets.iterator(); iterator.hasNext();)
		{
			//Check the existing assets for a move
			Data assetdata = (Data) iterator.next();
			Asset asset = (Asset)inArchive.getAssetSearcher().loadData(assetdata);
			Asset copy = copyAssetIfNeeded(inReq,inArchive,asset,collectionpath);
			if( copy != asset)
			{
				//Move the record
				Data colasset = (Data)assetrecords.get(assetdata.getId());
				librarycollectionassetSearcher.delete(colasset,null);
				colasset = librarycollectionassetSearcher.createNewData();
				colasset.setProperty("librarycollection", inCollectionid);
				colasset.setProperty("_parent", copy.getId());
				colasset.setProperty("asset", copy.getId()); //needed?
				librarycollectionassetSearcher.saveData(colasset, null);
			}
		}
		//2: Find all the categories and move them to the library
		Searcher librarycollectioncategorySearcher = inArchive.getSearcher("librarycollectioncategory");
		HitTracker colcathits = librarycollectioncategorySearcher.query().match("librarycollection", inCollectionid).search();
		
		List tosave = new ArrayList();
		for (Iterator iterator = colcathits.iterator(); iterator.hasNext();)
		{
			Data catData = (Data)librarycollectioncategorySearcher.loadData( (Data)iterator.next());
//			if( "true".equals( catData.get("importedcat")) )
//			{
//				log.info("Already imported this category" + catData.get("categoryid"));
//				continue;
//			}
			Category parentCat = (Category)inArchive.getCategory(catData.get("categoryid"));
			//2. Now move the old category parent to the new parent and save it. The assets will just need their sourcepath updated
			Collection catassets = inArchive.getAssetSearcher().query().match("category", parentCat.getId()).search();
			
			String folder = parentCat.getCategoryPath();
			//TODO: Turn off notifications
			String catpath = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/" + folder;
			List childrenfiles = inArchive.getPageManager().getChildrenPaths(catpath);
			
			//this is every asset and the children
			for (Iterator iterator2 = catassets.iterator(); iterator2.hasNext();)
			{
				Data assetdata = (Data) iterator2.next();
				Asset asset = (Asset)inArchive.getAssetSearcher().loadData(assetdata);
				List catpaths = new ArrayList();
				for (Iterator iterator3 = asset.getCategories().iterator(); iterator3.hasNext();)
				{
					Category cat = (Category) iterator3.next();
					catpaths.add(cat.getCategoryPath());
				}
				Collections.sort(catpaths);
				Collections.reverse(catpaths);
				String newpath = null;
				for (Iterator iterator3 = catpaths.iterator(); iterator3.hasNext();)
				{
					String path = (String) iterator3.next();
					if( path.startsWith(folder))
					{
						newpath = collectionpath + "/" + parentCat.getName() + path.substring(folder.length());
						break;
					}
				}
				
				if( newpath== null)
				{
					log.error("someone deleted cats" + folder + asset.getId() );
					continue;
				}
				//Remove the old category?
				Asset existingasset = (Asset)inArchive.getAssetSearcher().loadData(assetdata);
				Asset copy = copyAssetIfNeeded(inReq, inArchive, existingasset, newpath);
				if( copy != existingasset)
				{
					inArchive.saveAsset(copy, null);
	
//					Data old = (Data)assetrecords.get(existingasset.getId());
//					librarycollectionassetSearcher.delete(old, null);
					
					Data found = librarycollectionassetSearcher.createNewData();
					//found.setSourcePath(libraryid + "/" + librarycollection);
					found.setProperty("librarycollection", inCollectionid);
					found.setProperty("asset", copy.getId()); //legacy
					found.setProperty("_parent", copy.getId()); 
					tosave.add(found);
				}	
			}
			for (Iterator iterator2 = childrenfiles.iterator(); iterator2.hasNext();)
			{
				String path = (String) iterator2.next();
				String dest = collectionpath + "/" + PathUtilities.extractFileName(path);
				ContentItem existing = inArchive.getPageManager().getRepository().getStub(dest); 
				if( !existing.exists() )
				{
					ContentItem source = inArchive.getPageManager().getRepository().getStub(path);
					inArchive.getPageManager().getRepository().copy(source, existing);
				}
			}
			
			//Save the cateory
			//catData.setProperty("importedcat","true");
			//librarycollectioncategorySearcher.saveData(catData, null);
			//Marked as imported or remove the category?
			librarycollectionassetSearcher.saveAllData(tosave, null);
			tosave.clear();
		}
//		assets = loadAssetsInCollection(inReq, inArchive, inCollectionid);
		//Get all the assets in collection
		
	}

	public String exportCollectionTo(WebPageRequest inReq, MediaArchive inArchive, String inCollectionid, String inLibraryid)
	{
		//move the collection root folder
		//TODO: Check for bool
		importCollection(inReq,inArchive,inCollectionid); //make copies of everything
		
		//grab all the assets and update thier sourcepath and move them with images
//		collection.setProperty("library",inLibraryid);
//		inArchive.getSearcher("librarycollection").saveData(collection, inReq.getUser());
		Data collection = inArchive.getData("librarycollection", inCollectionid);

		//Get destination library 
		Data oldlibrary = inArchive.getData("library", collection.get("library"));
		Data newlibrary = inArchive.getData("library", inLibraryid);
		
		//Get library hot folder
		String librarysourcepath = newlibrary.get("folder");
		
		if( librarysourcepath == null)
		{
			throw new OpenEditException("No folder set on library");
		}
		String oldcollectionpath = oldlibrary.get("folder") + "/" + collection.getName(); 
		String path = inArchive.getCatalogSettingValue("movecollectionpath");

		Map args = new HashMap();
		args.put("collection", collection);
		args.put("oldlibrary", oldlibrary);
		args.put("newlibrary", newlibrary);
		args.put("splitname", makeChunks(collection.getName()));
		String date  = DateStorageUtil.getStorageUtil().formatDateObj(new Date(), "yyyy/MM"); //TODO: Use DataStorage
		args.put("year",date.substring(0, 4));
		args.put("month",date.substring(5, 7));
		args.put("user",inReq.getUser());

		//librarysourcepath + "/" + collection.getName();
		//${newlibrary.folder}/${year}/${splitname}
		String collectionpath = inArchive.getReplacer().replace(path, args); 

		//Move this folder and update all the sourcepaths on assets. Also add a new Category
		Collection assets = loadAssetsInCollection(inReq, inArchive, inCollectionid);
	
		
		for (Iterator iterator = assets.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();

			//Take old path and replace it in the sourcepath
			Asset existingasset = (Asset)inArchive.getAssetSearcher().loadData(data);
			String newsourcepath = collectionpath + existingasset.getSourcePath().substring(oldcollectionpath.length());
			
			String oldpathprimary = existingasset.getSourcePath();
			String oldpath = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/" + oldpathprimary;
			Page oldpage = inArchive.getPageManager().getPage(oldpath);
			if( !oldpage.exists() )
			{
				log.info("Asset missing   " + oldpath);
				continue;
			}
			String newpath = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/" + newsourcepath;
			Page newpage = inArchive.getPageManager().getPage(newpath);
			if( newpage.exists() )
			{
				log.info("Duplicated entry  " + newpath);
				continue; //Put into a weird sub directory?
			}
			
			Map props = new HashMap();
			try
			{
				props.put("absolutepath", newpage.getContentItem().getAbsolutePath());
				inArchive.fireMediaEvent("savingoriginal","asset",existingasset.getSourcePath(),props,inReq.getUser());
				existingasset.setSourcePath(newsourcepath);
				inArchive.getAssetSearcher().saveData(existingasset, inReq.getUser()); //avoid Hot folder detection
				inArchive.getPageManager().movePage(oldpage, newpage);
				Page oldthumbs = inArchive.getPageManager().getPage("/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + oldpathprimary);
				Page newthumbs = inArchive.getPageManager().getPage("/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + newsourcepath);
				if( oldthumbs.exists())
				{
					inArchive.getPageManager().movePage(oldthumbs, newthumbs);
				}
			}
			finally
			{
				inArchive.fireMediaEvent("savingoriginalcomplete","asset",existingasset.getSourcePath(),props,inReq.getUser());
			}
		}

		//Clean up
		Page leftovers = inArchive.getPageManager().getPage("/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + oldcollectionpath);
		if( leftovers.exists())
		{
			Page dest = inArchive.getPageManager().getPage("/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + collectionpath);
			inArchive.getPageManager().movePage(leftovers, dest);
		}
		leftovers = inArchive.getPageManager().getPage("/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/" + oldcollectionpath);
		if( leftovers.exists())
		{
			Page dest = inArchive.getPageManager().getPage("/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/" + collectionpath);
			inArchive.getPageManager().movePage(leftovers, dest);
		}
		//inArchive.getPageManager().movePage(oldthumbs, newthumbs);

		collection.setValue("library", newlibrary.getId());
		inArchive.getSearcher("librarycollection").saveData(collection, null);
		
		return collectionpath;
		
	}
	
	protected String makeChunks(String inName)
	{
		int split = 3;
		if( inName.length() < 3 )
		{
			return inName;
		}
		String fixed = inName.replace("-", "");
		fixed = fixed.substring(0,3) + "/" + inName;
		return fixed;
	}	
	
	protected Asset copyAssetIfNeeded(WebPageRequest inReq, MediaArchive inArchive, Asset existingasset, String folderpath)
	{
		//Change sourcepath
		String oldsourcepath= existingasset.getSourcePath();
		if( oldsourcepath.startsWith(folderpath))
		{
			return existingasset;
		}
		String sourcepath = null;
		if( existingasset.isFolder() || existingasset.getPrimaryFile() == null)
		{
			sourcepath = folderpath + oldsourcepath.substring(oldsourcepath.lastIndexOf('/'));
		}
		else
		{
			sourcepath = folderpath + "/" + existingasset.getPrimaryFile();
		}
//		String oldpathprimary = existingasset.getSourcePath();
//		if( existingasset.isFolder() )
//		{
//			oldpathprimary =  oldpathprimary + "/" + existingasset.getPrimaryFile();
//		}
//		String dest = collectionpath + "/" + existingasset.getPrimaryFile();
		Asset newasset = inArchive.getAssetBySourcePath(sourcepath);
		
		if( newasset != null)
		{
			log.info("Asset already imported " + sourcepath);
			return newasset;
		}
		//Check for duplicates
		
		//use Categories for multiple files
		//These are single files with conflict checking
		newasset = inArchive.getAssetEditor().copyAsset(existingasset,null);
		newasset.setFolder(existingasset.isFolder());
		newasset.setSourcePath(sourcepath);
		newasset.clearCategories();
		Category newparent = inArchive.getCategoryArchive().createCategoryTree(sourcepath);
		newasset.addCategory(newparent);

		
		String oldpath = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/" + oldsourcepath;
		Page oldpage = inArchive.getPageManager().getPage(oldpath);
		boolean copyorig = true;
		if( !oldpage.exists() )
		{
			log.info("Original missing   " + oldpath);
			copyorig = false;
		}
		String newpath = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/" + sourcepath;
		Page newpage = inArchive.getPageManager().getPage(newpath);
		if( newpage.exists() )
		{
			log.info("Duplicated entry  " + newpath);
			copyorig = false;
		}
		if( copyorig )
		{
			Map props = new HashMap();
			try
			{
				props.put("absolutepath", newpage.getContentItem().getAbsolutePath());
				inArchive.fireMediaEvent("savingoriginal","asset",newasset.getSourcePath(),props,inReq.getUser());
				inArchive.getAssetSearcher().saveData(newasset, inReq.getUser()); //avoid Hot folder detection
				inArchive.getPageManager().copyPage(oldpage, newpage);
				Page oldthumbs = inArchive.getPageManager().getPage("/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + oldsourcepath);
				Page newthumbs = inArchive.getPageManager().getPage("/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + sourcepath);
				if(oldthumbs.exists())
				{
					inArchive.getPageManager().copyPage(oldthumbs, newthumbs);
				}
			}
			finally
			{
				inArchive.fireMediaEvent("savingoriginalcomplete","asset",newasset.getSourcePath(),props,inReq.getUser());
			}
		}	
		return newasset;
	}

	public Data loadUserLibrary(MediaArchive inArchive, UserProfile inProfile)
	{
		User user = inProfile.getUser();
		Data userlibrary = inArchive.getData("library",user.getId());
		if( userlibrary != null)
		{
			return userlibrary;
		}
		
		userlibrary = inArchive.getSearcher("library").createNewData();
		userlibrary.setId(user.getUserName());
		userlibrary.setName(user.getScreenName());
		userlibrary.setProperty("folder", "Users/" + user.getScreenName());
		inArchive.getSearcher("library").saveData(userlibrary, null);

		//Make sure I am in the list of users for the library
		if( addUserToLibrary(inArchive,userlibrary,user) )
		{
			//reload profile?
			inProfile.getCombinedLibraries().add(userlibrary.getId());
		}
		return userlibrary;
	}

	
}

