package org.entermediadb.projects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
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
import org.openedit.users.User;
public class BaseProjectManager implements ProjectManager
{
	private static final Log log = LogFactory.getLog(BaseProjectManager.class);
	
	protected SearcherManager fieldSearcherManager;
	protected String fieldCatalogId;
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
	
	public Collection<UserCollection> loadCollections(WebPageRequest inReq)
	{
		//get a library
		Collection<UserCollection> usercollections = (Collection<UserCollection>)inReq.getPageValue("usercollections");
		if( usercollections != null)
		{
			return usercollections;
		}
		
		Data library = getCurrentLibrary(inReq.getUserProfile());
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
					HitTracker collectionassets = collectionassetsearcher.query().orgroup("librarycollection",ids).named("sidebar").search(inReq); //todo: Cache?
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
				//found.setProperty("asset", assetid);
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
		Data found = searcher.query().match("userid",inUser.getId()).match("libraryid", inLibrary.getId()).searchOne();	
		if( found == null )
		{
			found = searcher.createNewData();
			found.setProperty("userid",inUser.getId());
			found.setProperty("libraryid",inLibrary.getId());
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
	public void addCategoryToCollection(MediaArchive inArchive, String inCollectionid, String inCategoryid)
	{
		Searcher librarycollectioncategorySearcher = inArchive.getSearcher("librarycollectioncategory");
		
		Data data = librarycollectioncategorySearcher.query().match("librarycollection", inCollectionid).match("categoryid", inCategoryid).searchOne();
		
		if(data == null)
		{
			Data newfolder = librarycollectioncategorySearcher.createNewData();
			newfolder.setProperty("librarycollection", inCollectionid);
			newfolder.setProperty("categoryid", inCategoryid);
			librarycollectioncategorySearcher.saveData(newfolder, null);
		}
	}
	
	@Override
	public void removeCategoryFromCollection(MediaArchive inArchive, String inCollectionid, String inCategoryid)
	{
		Searcher librarycollectioncategorySearcher = inArchive.getSearcher("librarycollectioncategory");
		
		Data data = librarycollectioncategorySearcher.query().match("librarycollection", inCollectionid).match("categoryid", inCategoryid).searchOne();
		

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

	@Override
	public void moveCollectionTo(WebPageRequest inReq, MediaArchive inArchive, String inCollectionid, String inLibraryid)
	{
		//Find all the assets and move them to the library
		
		//Get all the assets in collection
		Collection assets = loadAssetsInCollection(inReq, inArchive, inCollectionid);
		
		//Get destination library 
		Data library = inArchive.getData("library", inLibraryid);
		
		//Get library hot folder
		String sourcepath = library.get("folder");
		
		if( sourcepath == null)
		{
			throw new OpenEditException("No folder set on library");
		}
		
		Data collection = inArchive.getData("librarycollection", inCollectionid);
		String assetssourcepath = sourcepath + "/" + collection.getName(); 
		
		moveAssets(inReq, inArchive, assets, assetssourcepath);
		
		//Find all the categories and move them to the library
		HitTracker categories = loadCategoriesOnCollection(inArchive,inCollectionid);
		
		for (Iterator iterator = categories.iterator(); iterator.hasNext();)
		{
			Data catData = (Data) iterator.next();
			
			Category cat = (Category) inArchive.getCategorySearcher().loadData(catData);
			
			String categoryroot = cat.getSourcePath();
			
			Collection catassets = inArchive.getAssetSearcher().query().match("category", cat.getId()).search();
			for (Iterator iterator2 = catassets.iterator(); iterator2.hasNext();)
			{
				Data data = (Data) iterator2.next();
				Asset asset = (Asset)inArchive.getAssetSearcher().loadData(data);
				Collection collections = new ArrayList(asset.getCategories());
				for (Iterator iterator3 = collections.iterator(); iterator3.hasNext();)
				{
					Category assetcat = (Category) iterator3.next();
					if( assetcat.getSourcePath().startsWith(categoryroot))
					{
						asset.removeCategory(assetcat);
						String newcategorypath = assetssourcepath + "/" + assetcat.getSourcePath().substring(categoryroot.length());
						
						Category newcategory = inArchive.getCategoryArchive().createCategoryTree(newcategorypath); //
						asset.addCategory(newcategory);
						asset.setSourcePath(newcategorypath + "/" + asset.getName());
						
					}
				}
			}
			
			String oldpath = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/" + categoryroot;
			String newpath = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/" + assetssourcepath + "/" + cat.getName();
			
			Page oldpage = inArchive.getPageManager().getPage(oldpath);
			Page newpage = inArchive.getPageManager().getPage(newpath);
			if( oldpage.exists() )
			{
				inArchive.getPageManager().movePage(oldpage, newpage);
			}

			Page oldthumbs = inArchive.getPageManager().getPage("/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + categoryroot);
			Page newthumbs = inArchive.getPageManager().getPage("/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + assetssourcepath + "/" + cat.getName());

			if( oldthumbs.exists() )
			{
				inArchive.getPageManager().movePage(oldthumbs, newthumbs);
			}
	
			
		}
		
		
		collection.setProperty("library",inLibraryid);
		inArchive.getSearcher("librarycollection").saveData(collection, inReq.getUser());
		//moveAssets(inReq, inArchive, assets, sourcepath);
		
		
	}

	protected void moveAssets(WebPageRequest inReq, MediaArchive inArchive, Collection assets, String sourcepath)
	{
		for (Iterator iterator = assets.iterator(); iterator.hasNext();)
		{
			Data asset = (Data) iterator.next();
			
			asset = inArchive.getAssetSearcher().loadData(asset);
			
			//Change sourcepath
			//Move images
			String dest = sourcepath + "/" + asset.getName(); 
			//TODO: Check for duplicates
			
			String oldpath = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/" + asset.getSourcePath();
			String newpath = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/" + dest;
			
			//Move to the new page
			Page oldpage = inArchive.getPageManager().getPage(oldpath);
			Page newpage = inArchive.getPageManager().getPage(newpath);
            inArchive.getPageManager().movePage(oldpage, newpage);
            
            
            //Move the thumbs to the new page
			Page oldthumbs = inArchive.getPageManager().getPage("/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + asset.getSourcePath());
			Page newthumbs = inArchive.getPageManager().getPage("/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + dest);
			inArchive.getPageManager().movePage(oldthumbs, newthumbs);
			
			
			asset.setSourcePath(dest);
			
			//tosave.add(asset);
			inArchive.getAssetSearcher().saveData(asset, inReq.getUser());
			
		}
	}

}

