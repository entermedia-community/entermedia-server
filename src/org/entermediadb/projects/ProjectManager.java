package org.entermediadb.projects;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.AssetUtilities;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.scanner.PresetCreator;
import org.entermediadb.asset.util.JsonUtil;
import org.entermediadb.asset.xmldb.CategorySearcher;
import org.entermediadb.users.UserProfileManager;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.event.WebEvent;
import org.openedit.hittracker.FilterNode;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.hittracker.Term;
import org.openedit.profile.UserProfile;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.users.authenticate.PasswordGenerator;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.FileUtils;
import org.openedit.util.PathUtilities;

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

	public Data setCurrentLibrary(UserProfile inProfile, String libraryid)
	{
		if (inProfile != null)
		{
			inProfile.setProperty("last_selected_library", libraryid);
			if (libraryid != null)
			{
				Data library = getSearcherManager().getData(getCatalogId(), "library", libraryid);
				return library;
			}
		}
		return null;
	}

	public Collection<LibraryCollection> loadCollections(WebPageRequest inReq, MediaArchive inArchive)
	{
		// get a library
		Collection<LibraryCollection> usercollections = (Collection<LibraryCollection>) inReq.getPageValue("usercollections");
		if (usercollections != null)
		{
			return usercollections;
		}

		Data library = getCurrentLibrary(inReq.getUserProfile());
		// if (library == null)
		// {
		// library = loadUserLibrary(inArchive, inReq.getUserProfile());
		// }
		if (library != null)
		{
			inReq.putPageValue("selectedlibrary", library);

			Searcher searcher = getSearcherManager().getSearcher(getCatalogId(), "librarycollection");
			// String reloadcollectoin = inReq.getRequestParameter("reloadcollection");
			HitTracker allcollections = searcher.query().exact("library", library.getId()).sort("name").named("sidebar").search();

			// Mach up all the top level categories
			usercollections = loadUserCollections(inReq, allcollections, inArchive, library);

			inReq.putPageValue("usercollections", usercollections);
			return usercollections;
		}
		return Collections.EMPTY_LIST;

		// search
		// Searcher searcher =
		// getSearcherManager().getSearcher(getMediaArchive().getCatalogId(),"librarycollection")
		// )
		// HitTracker labels =
		// searcher.query().match("library",$library.getId()).sort("name").search() )

	}

	public Collection<LibraryCollection> loadOpenCollections(WebPageRequest inReq, MediaArchive inArchive, int count)
	{
		// get a library
		// inReq.putPageValue("selectedlibrary",library);
		//log.info("working?");
		HitTracker usercollections = (HitTracker) inReq.getPageValue("usercollections");
		if (usercollections != null)
		{
			return usercollections;
		}
		// enable filters to show the asset count on each collection node

		Collection opencollections = inReq.getUserProfile().getValues("opencollections");
		if (opencollections == null)
		{
			return Collections.EMPTY_LIST;
		}
		opencollections = new ArrayList(opencollections);
		opencollections.remove(inReq.getUserName() + "-favorites");

		if (opencollections.size() > 0) // May not have any collections
		{
			// Build list of ID's
			Searcher searcher = getSearcherManager().getSearcher(getCatalogId(), "librarycollection");

			List array = new ArrayList(opencollections);
			Collections.reverse(array);

			final List sortorder = array.subList(0, Math.min(array.size(), count));
			HitTracker allcollections = searcher.query().orgroup("id", sortorder).named("sidebar").search(); // todo:
																												// Cache?
			List allusercollections = loadUserCollections(inReq, allcollections, inArchive, null);

			//List<Data> sorted = new ArrayList(usercollections);
			Collections.sort(allusercollections, new Comparator<LibraryCollection>()
			{
				public int compare(LibraryCollection o1, LibraryCollection o2)
				{
					int one = sortorder.indexOf(o1.getId());
					int two = sortorder.indexOf(o2.getId());
					if (one == two)
						return 0;
					if (one > two)
						return 1;
					return -1;
				};
			});
			inReq.putPageValue("usercollections", new ListHitTracker(allusercollections));
		}

		return usercollections;

	}

	protected List<LibraryCollection> loadUserCollections(WebPageRequest inReq, HitTracker allcollections, MediaArchive inArchive, Data library)
	{
		Searcher lcsearcher = inArchive.getSearcher("librarycollection");

		List<LibraryCollection> usercollections = new ArrayList<LibraryCollection>();

		Collection categoryids = new ArrayList();
		// Add the base library
		if (library != null)
		{
			String parent = library.get("categoryid");
			if (parent != null)
			{
				categoryids.add(parent);
			}
		}
		allcollections.setHitsPerPage(500);
		for (Iterator iterator = allcollections.getPageOfHits().iterator(); iterator.hasNext();)
		{
			Data collection = (Data) iterator.next();
			LibraryCollection uc = (LibraryCollection) lcsearcher.loadData(collection);

			if (uc.hasRootCategory())
			{
				String catid = uc.getRootCategoryId();
				categoryids.add(catid);
			}
			usercollections.add(uc);
		}

		//Why are we doing this? This should come from facets
		QueryBuilder query = inArchive.query("asset");
		query.orgroup("category", categoryids);
		query.hitsPerPage(1);
		query.addFacet("category");
		query.named("librarysidebar");
		if (inReq.hasPermission("showonlyapprovedassets"))
		{
			query.exact("editstatus", "6");
		}

		HitTracker hits = query.search(inReq);

		// log.info( hits.getSearchQuery() );
		int assetsize = 0;
		if (hits != null)
		{
			assetsize = hits.size();

		}
		else
		{
			assetsize = 0;
		}
		inReq.putPageValue("librarysize", assetsize);

		//TODO: This seems like a long way around

		//		LibraryCollection favorites = getFavoritesCollection(inReq.getUser());
		//		LibraryCollection loadedfavs = null;
		//		for (Iterator iterator = usercollections.iterator(); iterator.hasNext();) {
		//			LibraryCollection col = (LibraryCollection) iterator.next();
		//			if (col.getId().equals(inReq.getUserName() + "-favorites")) {
		//				loadedfavs = col;
		//			}
		//		}
		//
		//		if (loadedfavs != null) {
		//
		//			usercollections.remove(loadedfavs);
		//		}
		//		usercollections.add(0, favorites);

		// Show all the collections for a library
		inReq.putPageValue("allcollections", usercollections);

		// enable filters to show the asset count on each collection node
		FilterNode collectionhits = null;
		if (hits != null && allcollections.size() > 0) // May not have any collections
		{
			FilterNode node = hits.findFilterNode("category");
			if (node != null)
			{
				for (Iterator iterator = usercollections.iterator(); iterator.hasNext();)
				{
					LibraryCollection collection = (LibraryCollection) iterator.next();
					if (collection.hasRootCategory())
					{
						int counted = node.getCount(collection.getRootCategoryId());
						if (counted == -1)
						{
							// These fell off the radar of the agregation because there are too many random
							// categories
							QueryBuilder q = inArchive.getAssetSearcher().query().exact("category", collection.getRootCategoryId()).named("librarysidebarexact");
							if (inReq.hasPermission("showonlyapprovedassets"))
							{
								q.exact("editstatus", "6");
							}
							Collection assets = q.search();
							log.info("Too many other categories within collection:" + collection.getName());
							counted = assets.size();
						}
						collection.setAssetCount(counted);
					}
				}
			}
		}

		return usercollections;
	}

	public LibraryCollection getFavoritesCollection(User inUser)
	{
		Searcher collections = getMediaArchive().getSearcher("librarycollection");
		if (inUser == null)
		{
			return null;
		}
		LibraryCollection collection = (LibraryCollection) collections.searchById(inUser.getId() + "-favorites");
		if (collection == null)
		{
			collection = (LibraryCollection) collections.createNewData();
			collection.setName(inUser.toString() + " Favorites");
			collection.setId(inUser.getId() + "-favorites");
			Searcher categories = getMediaArchive().getSearcher("category");

			String collectionroot = getMediaArchive().getCatalogSettingValue("collection_root");
			if (collectionroot == null)
			{
				//collectionroot = "Collections/Favorites/";
				collectionroot = "Favorites/";  //Finder
			}

			Category newcat = getMediaArchive().createCategoryPath(collectionroot + collection.getName());

			newcat.setName(collection.getName());

			categories.saveData(newcat);

			collection.setValue("rootcategory", newcat.getId());
			collection.setValue("creationdate", new Date());
			collection.setValue("owner", inUser.getId());

			/**
			 * We cant make this private because then assets would become hidden
			 * from regular users collection.setValue("visibility", "3");
			 */
			collection.setValue("collectiontype", "2");
			collections.saveData(collection);
		}

		HitTracker hits = getMediaArchive().getAssetSearcher().query().hitsPerPage(1).exact("category", collection.getRootCategoryId()).search();
		collection.setAssetCount(hits.size());

		return collection;
	}

	public LibraryCollection getMessagesCollection(User inUser)
	{
		Searcher collections = getMediaArchive().getSearcher("librarycollection");
		if (inUser == null)
		{
			return null;
		}
		LibraryCollection collection = (LibraryCollection) collections.searchById(inUser.getId() + "-messages");
		if (collection == null)
		{
			collection = (LibraryCollection) collections.createNewData();
			collection.setName(inUser.getScreenName());
			collection.setId(inUser.getId() + "-messages");
			Searcher categories = getMediaArchive().getSearcher("category");

			String collectionroot = getMediaArchive().getCatalogSettingValue("collection_root");
			if (collectionroot == null)
			{
				collectionroot = "System";
			}

			Category newcat = getMediaArchive().createCategoryPath(collectionroot + "/Messages/" + collection.getName());

			newcat.setName(collection.getName());

			categories.saveData(newcat);

			collection.setValue("rootcategory", newcat.getId());
			collection.setValue("creationdate", new Date());
			collection.setValue("owner", inUser.getId());

			/**
			 * We cant make this private because then assets would become hidden
			 * from regular users collection.setValue("visibility", "3");
			 */
			collection.setValue("collectiontype", "3");
			collections.saveData(collection);
		}

		HitTracker hits = getMediaArchive().getAssetSearcher().query().hitsPerPage(1).exact("category", collection.getRootCategoryId()).search();
		collection.setAssetCount(hits.size());

		return collection;
	}
	// public void addAssetToCollection(MediaArchive archive, String libraryid,
	// String collectionid, HitTracker assets)
	// {
	// if (libraryid != null)
	// {
	// addAssetToLibrary(archive, libraryid, assets);
	// }
	// addAssetToCollection(archive, collectionid, assets);
	// }

	public void addAssetToCollection(MediaArchive archive, String librarycollection, HitTracker assets)
	{
		List tosave = new ArrayList();
		//	assets.enableBulkOperations();
		LibraryCollection collection = (LibraryCollection) getLibraryCollection(archive, librarycollection);
		if (collection == null)
		{
			log.error("Could not add assets to deleted collection. " + librarycollection);
			return;
		}
		Category root = getRootCategory(archive, collection);

		HitTracker existing = archive.getAssetSearcher().query().match("category", root.getId()).search();
		existing.enableBulkOperations();

		Set assetids = new HashSet(existing.size());

		for (Iterator iterator = existing.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			assetids.add(hit.getId());
		}
		//log.info("Hits size was " + assets.size());
		//log.info("Show only selected was: " + assets.isShowOnlySelected());

		for (Iterator iterator = assets.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			if (!assetids.contains(data.getId()))
			{
				Asset asset = (Asset) archive.getAssetSearcher().loadData(data);
				Category specific = findSpecificRoot(archive, root, asset);
				asset.addCategory(specific);
				tosave.add(asset);
				if (tosave.size() > 1000)
				{
					archive.getAssetSearcher().saveAllData(tosave, null);
					tosave.clear();
				}

			}

		}
		archive.getAssetSearcher().saveAllData(tosave, null);
	}

	// public void addAssetToCollection(MediaArchive archive, String libraryid,
	// String collectionid, String assetid)
	// {
	// addAssetToLibrary(archive, libraryid, assetid);
	// //String librarycollection = inReq.getRequestParameter("librarycollection");
	// addAssetToCollection(archive, collectionid, assetid);
	// }
	//
	public void addAssetToCollection(MediaArchive archive, String collectionid, String assetid)
	{
		Asset asset = (Asset) archive.getAssetSearcher().searchById(assetid);
		addAssetToCollection(archive, collectionid, asset);
	}

	public void addAssetToCollection(MediaArchive archive, String collectionid, Asset asset)
	{
		if (asset != null)
		{
			// If the asset sourcepath starts with the root then put it right into the right
			// place
			Category root = getRootCategory(archive, collectionid);
			root = findSpecificRoot(archive, root, asset);
			asset.addCategory(root);
			archive.getAssetSearcher().saveData(asset);
		}
	}

	private Category findSpecificRoot(MediaArchive archive, Category root, Asset asset)
	{
		String path = null;
		if (asset.isFolder())
		{
			path = asset.getSourcePath();
		}
		else
		{
			path = PathUtilities.extractDirectoryPath(asset.getSourcePath());

		}
		if (path.startsWith(root.getCategoryPath()))
		{
			root = archive.createCategoryPath(path);
		}
		return root;
	}

	// public void addAssetToLibrary(MediaArchive archive, String libraryid,
	// HitTracker assets)
	// {
	// List tosave = new ArrayList();
	// for (Object data : assets)
	// {
	// //TODO: Skip loading?
	// MultiValued toadd = (MultiValued) data;
	// Collection libraries = toadd.getValues("libraries");
	// if (libraries != null && libraries.contains(libraryid))
	// {
	// continue;
	// }
	// Asset asset = (Asset) archive.getAssetSearcher().loadData(toadd);
	//
	// if (asset != null && !asset.getLibraries().contains(libraryid))
	// {
	// asset.addLibrary(libraryid);
	// tosave.add(asset);
	// if (tosave.size() > 500)
	// {
	// archive.saveAssets(tosave);
	// tosave.clear();
	// }
	// }
	// }
	// archive.saveAssets(tosave);
	//
	// }
	//
	// public void addAssetToLibrary(MediaArchive archive, String libraryid, String
	// assetid)
	// {
	// Asset asset = archive.getAsset(assetid);
	//
	// if (asset != null && !asset.getLibraries().contains(libraryid))
	// {
	// asset.addLibrary(libraryid);
	// archive.saveAsset(asset, null);
	// }
	// }
	//
	// public HitTracker loadAssetsInLibrary(Data inLibrary, MediaArchive archive,
	// WebPageRequest inReq)
	// {
	// HitTracker hits = archive.getAssetSearcher().query().match("libraries",
	// inLibrary.getId()).search(inReq);
	// return hits;
	// }
	//
	public HitTracker loadAssetsInCollection(WebPageRequest inReq, MediaArchive archive, String collectionid)
	{
		if (collectionid == null)
		{
			return null;
		}
		Searcher searcher = archive.getAssetSearcher();

		HitTracker all = null;
		SearchQuery assetsearch = searcher.addStandardSearchTerms(inReq);
		Category root = getRootCategory(archive, collectionid);

		if (root == null)
		{
			log.error("No root category found " + collectionid);
			return null;
		}

		if (assetsearch == null)
		{
			assetsearch = searcher.createSearchQuery();
			String categoryId = inReq.getRequestParameter("categoryid");
			if (categoryId == null)
			{
				categoryId = inReq.getRequestParameter("nodeID");
			}
			if (categoryId != null)
			{
//				String search = inReq.getRequestParameter("searchchildren");
//				if (Boolean.parseBoolean(search))
//				{
					assetsearch.addExact("category", categoryId);
//				}
//				else
//				{
//					assetsearch.addExact("category-exact", categoryId);
//				}
			}
			else
			{
				assetsearch.addExact("category", root.getId());
			}
		}
		if (assetsearch.getTermByDetailId("category") == null)
		{
			assetsearch.addExact("category", root.getId());
		}

		//		if (inShowOnlyEditStatus != null) {
		//			SearchQuery child = searcher.createSearchQuery();
		//			child.addOrsGroup("editstatus", inShowOnlyEditStatus);
		//			assetsearch.addChildQuery(child);
		//		}

		String sort = (String) root.findValue("assetsort");
		
		if (sort == null)
		{
			sort = inReq.findValue("asset" + "sortby");
		}
		if (sort == null)
		{
			sort = inReq.findValue("sortby");
		}
		if(sort == null && inReq.getUserProfile()!=null) {
			sort = inReq.getUserProfileValue("assetsort");
		}
		if (sort == null)
		{
			sort = "assetaddeddateDown";
		}
		if (sort != null) {
			assetsearch.setSortBy(sort);
		}


		assetsearch.setProperty("collectionid", collectionid);
		//assetsearch.setHitsName("collectionassets");

		assetsearch.setEndUserSearch(true);

		//The old tracker does not have the same hitsname so it's cached diferently

		//		HitTracker oldtracker = searcher.loadHits(inReq,"hits");
		//		if( oldtracker != null)
		//		{
		//			//This is the new way
		//			for (Iterator iterator = oldtracker.getSearchQuery().getUserFilters().iterator(); iterator.hasNext();)
		//			{
		//				Term term = (Term) iterator.next();
		//				//see if it's already in there
		//				if( assetsearch.getTermByDetailId(term.getDetail().getId()) == null )
		//				{
		//					assetsearch.addTerm(term);
		//				}
		//			}
		//		}

		all = archive.getAssetSearcher().cachedSearch(inReq, assetsearch);

		//		if (inShowOnlyEditStatus != null && inShowOnlyEditStatus.equals("1")) {
		//			all.selectAll();
		//		}
		// Is this needed?
		String hpp = inReq.getRequestParameter("page");
		if (hpp != null)
		{
			all.setPage(Integer.parseInt(hpp));
		}
		// UserProfile usersettings = (UserProfile) inReq.getUserProfile();
		// if (usersettings != null)
		// {
		// all.setHitsPerPage(usersettings.getHitsPerPageForSearchType("asset"));
		// }
		return all;
	}

	// public void removeAssetFromLibrary(MediaArchive inArchive, String
	// inLibraryid, HitTracker inAssets)
	// {
	// Searcher librarycollectionsearcher =
	// inArchive.getSearcher("librarycollection");
	// HitTracker<Data> collections = (HitTracker<Data>)
	// librarycollectionsearcher.query().match("library", inLibraryid).search();
	// for (Object collection : collections)
	// {
	// removeAssetFromCollection(inArchive, ((Data) collection).getId(), inAssets);
	// }
	// for (Object toadd : inAssets)
	// {
	// Asset asset = (Asset) inArchive.getAssetSearcher().loadData((Data) toadd);
	//
	// if (asset != null && asset.getLibraries().contains(inLibraryid))
	// {
	// asset.removeLibrary(inLibraryid);
	// inArchive.saveAsset(asset, null);
	// }
	// }
	// }

	public void removeAssetFromCollection(MediaArchive inArchive, String inCollectionid, HitTracker inAssets)
	{
		LibraryCollection col = getLibraryCollection(inArchive, inCollectionid);
		Category cat = getRootCategory(inArchive, col);
		Collection tosave = new ArrayList();
		for (Iterator iterator = inAssets.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			Asset asset = (Asset) inArchive.getAssetSearcher().loadData(data);
			asset.removeChildCategory(cat); //
			tosave.add(asset);
		}
		inArchive.getAssetSearcher().saveAllData(tosave, null);
	}

	public void removeAssetFromCollection(MediaArchive inArchive, String inCollectionid, String assetid)
	{
		LibraryCollection col = getLibraryCollection(inArchive, inCollectionid);
		Category cat = getRootCategory(inArchive, col);
		Asset asset = (Asset) inArchive.getAssetSearcher().searchById(assetid);
		asset.removeChildCategory(cat); //
		inArchive.getAssetSearcher().saveData(asset, null);
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
				collection.setValue("library", inUser.getId()); // Make one now?
				collection.setValue("owner", inUser.getId());
				Category cat = inArchive.getCategory(inCategoryid);
				collection.setName(cat.getName());
				librarycolsearcher.saveData(collection, null);
				inCollectionid = collection.getId();
			}
			else
			{
				collection = getLibraryCollection(inArchive, inCollectionid);
				Category cat = inArchive.getCategory(inCategoryid);
				Category rootcat = getRootCategory(inArchive, inCollectionid);

				ArrayList list = new ArrayList();
				copyAssets(list, inUser, inArchive, collection, cat, rootcat, false);// will actually create
																						// librarycollectionasset
																						// entries
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

		LibraryCollection collection = getLibraryCollection(inArchive, inCollectionid);
		Category newroot = createRevision(inArchive, collection, inUser, inNote);

		ArrayList list = new ArrayList();
		copyAssets(list, inUser, inArchive, collection, rootcat, newroot, true);// will actually create
																				// librarycollectionasset entries
		Searcher assets = inArchive.getAssetSearcher();
		assets.saveAllData(list, null);

	}

	// Add new assets are added to both the root and the current version

	// When we snapshot we just increment the count and add existing to the new
	// count as well

	public void importCollection(WebPageRequest inReq, User inUser, MediaArchive inArchive, String inCollectionid, String inImportPath, String inNote)
	{
		LibraryCollection collection = getLibraryCollection(inArchive, inCollectionid);
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
		String oldcatid = root.getId();
		searcher.deleteCategoryTree(root);
		// remove all the assets?
		root = createRootCategory(inArchive, collection);
		if (root.getId().equals(oldcatid))
		{
			throw new OpenEditException("Did not delete old category");
		}
		collection.setValue("rootcategory", root.getId());
		inArchive.getSearcher("librarycollection").saveData(collection);

		FileUtils utils = new FileUtils();
		String path = inArchive.getPageManager().getPage(inImportPath).getContentItem().getAbsolutePath();
		File fname = new File(path);
		long totalbytes = utils.sizeOf(fname);

		try
		{
			importAssets(inArchive, collection, inImportPath, root, ids);
		}
		catch (Exception e)
		{
			inReq.putPageValue("errormsg", "There was an error importing.  Your files have not been deleted but you should import again.");
			inReq.putPageValue("exception", e);
			log.error("Failed importing Collection " + inCollectionid, e);
		}

		for (Iterator iterator = ids.iterator(); iterator.hasNext();)
		{
			String assetid = (String) iterator.next();
			Asset asset = inArchive.getAsset(assetid);
			inArchive.saveAsset(asset, inUser);
		}

		long finalbytes = utils.sizeOf(fname);

		if (finalbytes == totalbytes)
		{
			ContentItem item = inArchive.getPageManager().getRepository().getStub(inImportPath);
			inArchive.getPageManager().getRepository().remove(item);
		}
		else
		{
			inReq.putPageValue("errormsg", "There was an error importing.  Your files have not been deleted but you should import again. Size before: " + totalbytes + " after: " + finalbytes);

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
		// inCurrentParent.setChildren(null);
		String sourcepathmask = inArchive.getCatalogSettingValue("projectassetupload"); // ${division.uploadpath}/${user.userName}/${formateddate}

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
				// Who cares about the id
				inCurrentParent.addChild(newfolder);
				// inArchive.getCategorySearcher().saveData(inCurrentParent);
				inArchive.getCategorySearcher().saveData(newfolder);

				importAssets(inArchive, inCollection, inImportPath + "/" + item.getName(), newfolder, assets);
			}
			else
			{
				// MD5
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
				if (namedetail.isMultiLanguage())
				{
					target = (Data) inArchive.getAssetSearcher().query().exact("md5hex", md5).exact("name_int.en", item.getName()).searchOne();
				}
				else
				{
					target = (Data) inArchive.getAssetSearcher().query().exact("md5hex", md5).exact("name", item.getName()).searchOne();
				}
				Asset asset = (Asset) inArchive.getAssetSearcher().loadData(target);

				if (asset == null)
				{

					String savesourcepath = inArchive.getAssetImporter().getAssetUtilities().createSourcePathFromMask(inArchive, null, null, item.getName(), sourcepathmask, vals);

					String destpath = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/" + savesourcepath;
					if (!destpath.endsWith("/"))
					{
						destpath = "/";
					}
					destpath = destpath + item.getName();
					ContentItem finalitem = inArchive.getPageManager().getRepository().getStub(destpath);
					inArchive.getPageManager().getRepository().copy(item, finalitem);
					asset = inArchive.getAssetImporter().getAssetUtilities().createAssetIfNeeded(finalitem, false, inArchive, null);
					// asset.setCategories(null);
					asset.addCategory(inCurrentParent);
					asset.setValue("md5hex", md5);
					inArchive.saveAsset(asset, null); // TODO: is this needed?

					PresetCreator presets = inArchive.getPresetManager();
					Searcher tasksearcher = inArchive.getSearcherManager().getSearcher(inArchive.getCatalogId(), "conversiontask");
					presets.createMissingOnImport(inArchive, tasksearcher, asset);
				}
				else
				{
					// inArchive.getPageManager().getRepository().remove(item);
					asset.addCategory(inCurrentParent);
					inArchive.saveAsset(asset, null);
				}
				assets.remove(asset.getId());
			}
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
		Category collectioncategory = inCollection.getCategory();

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
		if (collection == null)
		{
			log.error("No collection found");
			return null;
		}
		Searcher librarysearcher = inArchive.getSearcher("library");
		String libraryid = collection.get("library");
		if (libraryid == null)
		{
			libraryid = "default";
			collection.setValue("library", libraryid);
			inArchive.getSearcher("librarycollection").saveData(collection);
		}
		Data library = inArchive.getData("library", libraryid);
		if ("default".equals(libraryid) && library == null)
		{
			library = librarysearcher.createNewData();
			library.setId("default");
			library.setName("General");
			librarysearcher.saveData(library);
		}

		if (library == null)
		{
			library = librarysearcher.createNewData();
			library.setId(libraryid);
			library.setName(libraryid);
			librarysearcher.saveData(library);

		}
		String collectionroot = inArchive.getCatalogSettingValue("collection_root");
		if (collectionroot == null)
		{
			collectionroot = "Collections";
		}

		Category collectioncategory = inArchive.createCategoryPath(collectionroot + "/" + library.getName() + "/" + collection.getName());
		return collectioncategory;
	}

	public Category createLibraryCategory(MediaArchive inArchive, Data library)
	{
		Category librarycategory = null;
		if (library.get("categoryid") != null)
		{
			librarycategory = inArchive.getCategory(library.get("categoryid"));
		}

		if (librarycategory == null)
		{
			String folder = library.get("folder");
			if (folder == null || folder.isEmpty())
			{
				String collectionroot = inArchive.getCatalogSettingValue("collection_root");
				if (collectionroot == null)
				{
					collectionroot = "Collections";
				}

				folder = collectionroot + "/" + library.getName();
			}
			librarycategory = inArchive.createCategoryPath(folder);
			library.setValue("categoryid", librarycategory.getId());
			inArchive.getSearcher("library").saveData(library);
		}
		return librarycategory;
	}

	public Category getRootVersionCategory(MediaArchive inArchive, LibraryCollection collection)
	{
		Searcher cats = inArchive.getSearcher("category");
		Searcher librarycolsearcher = inArchive.getSearcher("librarycollection");
		long revisions = collection.getCurentRevision();
		String libraryid = collection.get("library");
		Data library = inArchive.getData("library", libraryid);

		// Use the String path to load up the category from the hot folder import

		String id = collection.getId() + "_" + revisions;
		Category root = inArchive.getCategory(id);
		if (root == null)
		{
			root = (Category) cats.createNewData();
			String name = collection.getName();
			// if( revisions > 0)
			// {
			// name = name + " " + revisions;
			// }
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

	public LibraryCollection loadUserCollection(MediaArchive inArchive, UserProfile inProfile)
	{
		User user = inProfile.getUser();
		Data userlibrary = inArchive.getCachedData("library", "users");
		if( userlibrary == null)
		{
			userlibrary = inArchive.getSearcher("library").createNewData();
			userlibrary.setId("users");
			userlibrary.setName("Users");
			inArchive.saveData("library", userlibrary );
		}
		
		LibraryCollection usercollection = (LibraryCollection)inArchive.getCachedData("librarycollection", "users-" + user.getId());
		if( usercollection == null )
		{
			usercollection =  (LibraryCollection)inArchive.getSearcher("librarycollection").createNewData();
			usercollection.setId( "users-" + user.getId());
			usercollection.setName( user.getScreenName() );
			usercollection.setValue("library",userlibrary.getId());
			
			String basefolder = inArchive.getCatalogSettingValue("userbasefolder");
			if (basefolder == null) {
				basefolder = "Albums/Users"; //New default: "User Folder"
			}
			String folder = basefolder + "/" + user.getScreenName();
			Category category = inArchive.createCategoryPath(folder);
			((MultiValued) category).addValue("viewusers", user.getId());
			inArchive.getCategorySearcher().saveData(category);
			
			usercollection.setValue("rootcategory", category.getId());
			inArchive.saveData("librarycollection",usercollection);
			inProfile.getViewCategories().add(category); // Make sure I am in the list of users for the library
		}
		inProfile.addValue("opencollections", usercollection.getId());

		return usercollection;
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
		copyAssets(list, inUser, inArchive, collection, revisionroot, newroot, true);// will actually create
																						// librarycollectionasset
																						// entries
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
		//Searcher librarycolsearcher = inArchive.getSearcher("librarycollection");
		//LibraryCollection collection = (LibraryCollection) librarycolsearcher.searchById(inCollectionid);
		LibraryCollection collection = (LibraryCollection) inArchive.getCachedData("librarycollection", inCollectionid);
		return collection;
	}

	public LibraryCollection findCollectionForCategory(Category inCategory)
	{
		Searcher librarycolsearcher = getMediaArchive().getSearcher("librarycollection");
		Collection<Category> parents = inCategory.getParentCategories();
		Data hit = librarycolsearcher.query().orgroup("rootcategory", parents).searchOne();
		return (LibraryCollection) hit;
	}

	protected MediaArchive getMediaArchive()
	{
		MediaArchive archive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		return archive;
	}

	public Boolean canEditCollection(WebPageRequest inReq, String inCollectionid)
	{
		LibraryCollection collection = getLibraryCollection(getMediaArchive(), inCollectionid);
		return canEditCollection(inReq, collection);

	}

	protected Boolean canEditCollection(WebPageRequest inReq, LibraryCollection collection)
	{
		if (collection != null)
		{
			String ownerid = collection.get("owner");
			if (ownerid != null && ownerid.equals(inReq.getUserName()))
			{
				return true;
			}
			User user = inReq.getUser();
			if (user != null && user.isInGroup("administrators"))
			{
				// dont filter since its the admin
				return true;
			}

			Object caneditall = inReq.getPageValue("editallcollections");
			if (caneditall != null && Boolean.parseBoolean(caneditall.toString()))
			{
				return true;
			}

			Category root = collection.getCategory();
			if (root == null)
			{
				configureCollection(collection, inReq.getUserName());
				root = collection.getCategory();
			}
			if ("true".equals(inReq.findValue("legacycollectionpermissions")))
			{
				UserProfile profile = inReq.getUserProfile();
				if (profile != null && profile.getViewCategories() != null)
				{
					for (Category cat : profile.getViewCategories())
					{
						boolean wasview = false;
						if (root.hasParent(cat.getId()))
						{
							Collection vals = cat.findValues("viewonlygroups");
							if (vals != null)
							{
								for (Iterator iterator = vals.iterator(); iterator.hasNext();)
								{
									String groupid = (String) iterator.next();
									if (profile.isInGroup(groupid))
									{
										wasview = true;
										break;
									}
								}
							}
							if (!wasview)
							{
								vals = cat.findValues("viewonlyroles");
								if (vals != null)
								{
									for (Iterator iterator = vals.iterator(); iterator.hasNext();)
									{
										String roleid = (String) iterator.next();
										if (profile.isInRole(roleid))
										{
											wasview = true;
											break;
										}
									}
								}
							}
							if (wasview)
							{
								continue;
							}
							return true;
						}
					}
				}

			}
		}
		return false;
	}

	public boolean canViewCollection(WebPageRequest inReq, String inCollectionid)
	{
		LibraryCollection collection = getLibraryCollection(getMediaArchive(), inCollectionid);
		if (collection == null)
		{
			return false;
		}
		return canViewCollection(inReq, collection);
	}

	public boolean canViewCollection(WebPageRequest inReq, LibraryCollection collection)
	{
		User user = inReq.getUser();
		UserProfile profile = inReq.getUserProfile();
		boolean found = canViewCollection(user, profile,collection);
		return found;
	}
	public boolean canViewCollection(User inUser, UserProfile inProfile, LibraryCollection collection)
	{
		if (collection != null)
		{
			String ownerid = collection.get("owner");
			if (ownerid != null && inUser != null && ownerid.equals(inUser.getUserName()))
			{
				return true;
			}
			if (inUser != null && inUser.isInGroup("administrators"))
			{
				// dont filter since its the admin
				return true;
			}

			//Legacy?
			String visibility = collection.get("visibility");
			if (visibility != null)
			{
				if (visibility.equals("1") || visibility.equals("2"))
				{
					return true;
				}
			}
			Category root = collection.getCategory();
			if (root != null)
			{
				if (inProfile != null && inProfile.getViewCategories() != null)
				{
					for (Category cat : inProfile.getViewCategories())
					{
						if (root.hasParent(cat.getId()))
						{
							return true;
						}
					}
				}
			}
			return false;
		}
		if (inUser == null)
		{
			return false;
		}
		return true;
	}

	public boolean addUserToLibrary(MediaArchive inArchive, Data inSavedLibrary, User inUser)
	{
		Category librarycategory = createLibraryCategory(inArchive, inSavedLibrary);
		librarycategory.addValue("viewusers", inUser.getId());
		inArchive.getSearcher("category").saveData(librarycategory);
		return true;
	}

	public int approveSelection(WebPageRequest inReq, HitTracker inHits, String inCollectionid, User inUser, String inNote)
	{
		Collection tosave = new ArrayList();
		int approved = 0;
		Searcher searcher = getMediaArchive().getAssetSearcher();

		for (Iterator iterator = inHits.getSelectedHitracker().iterator(); iterator.hasNext();)
		{
			Data asset = (Data) iterator.next();
			asset = searcher.loadData(asset);
			asset.setValue("editstatus", "6");
			tosave.add(asset);
			approved++;
			if (tosave.size() > 400)
			{
				searcher.saveAllData(tosave, null);
				logAssetEvent(tosave, "approved", inReq.getUser(), inNote, inCollectionid, true);
				tosave.clear();
			}
		}
		searcher.saveAllData(tosave, null);
		inReq.putPageValue("approvedlist", inHits);
		logAssetEvent(tosave, "approved", inReq.getUser(), inNote, inCollectionid, true);
		return approved;
	}

	public void approveAsset(Asset inAsset, User inUser, String inNote, String inCollectionid, boolean addChatEntry)
	{
		inAsset.setValue("editstatus", "6");
		ArrayList tosave = new ArrayList();
		tosave.add(inAsset);
		logAssetEvent(tosave, "approved", inUser, inNote, inCollectionid, addChatEntry);
		getMediaArchive().getAssetSearcher().saveAllData(tosave, null);

	}

	public void rejectAsset(Asset inAsset, User inUser, String inNote, String inCollectionid, boolean addChatEntry)
	{
		inAsset.setValue("editstatus", "rejected");
		ArrayList tosave = new ArrayList();
		tosave.add(inAsset);
		logAssetEvent(tosave, "rejected", inUser, inNote, inCollectionid, addChatEntry);
		getMediaArchive().getAssetSearcher().saveAllData(tosave, null);

	}

	protected void logAssetEvent(Collection<Asset> inTosave, String inOperation, User inUser, String inNote, String inCollectionid, boolean addChat)
	{
		HashMap ownerassetmap = new HashMap();
		ArrayList chatentries = new ArrayList();
		Searcher chats = getMediaArchive().getSearcher("chatterbox");

		for (Iterator iterator = inTosave.iterator(); iterator.hasNext();)
		{
			Asset asset = (Asset) iterator.next();
			String owner = asset.get("owner");
			ArrayList assetlist = (ArrayList) ownerassetmap.get(owner);
			if (assetlist == null)
			{
				assetlist = new ArrayList();
				ownerassetmap.put(owner, assetlist);
			}
			assetlist.add(asset.getId());

			// Allow people to register and listen
			// getMediaArchive().fireMediaEvent(inOperation, null, asset);
			WebEvent event = new WebEvent();
			event.setSearchType("asset");
			event.setCatalogId(getCatalogId());
			event.setOperation(inOperation);
			event.setSource(this);
			event.setUser(inUser);
			event.setSourcePath(asset.getSourcePath()); // TODO: This should not be needed any more
			event.setProperty("sourcepath", asset.getSourcePath());
			event.setProperty("assetids", asset.getId());
			event.setProperty("owner", owner);

			event.setProperty("dataid", asset.getId());
			event.setProperty("note", inNote);
			event.setProperty("librarycollection", inCollectionid);
			// TODO: Log in one database table called collectionevents
			// archive.getWebEventListener()
			getMediaArchive().getEventManager().fireEvent(event);

			if (addChat)
			{

				Data chat = chats.createNewData();
				chat.setValue("date", new Date());
				chat.setValue("message", inNote);
				chat.setValue("user", inUser);
				String channel = "asset" + asset.getId();
				chat.setValue("channel", channel);
				chat.setValue("type", inOperation);
				chat.setValue("collectionid", inCollectionid);

				chatentries.add(chat);

			}

		}
		chats.saveAllData(chatentries, inUser);

		for (Iterator iterator = ownerassetmap.keySet().iterator(); iterator.hasNext();)
		{
			String key = (String) iterator.next();
			List values = (List) ownerassetmap.get(key);
			WebEvent event = new WebEvent();
			event.setSearchType("librarycollection");
			event.setCatalogId(getCatalogId());
			event.setOperation(inOperation);
			event.setSource(this);
			event.setUser(inUser);
			event.setProperty("owner", key);
			event.setValue("assetids", values);

			event.setProperty("note", inNote);
			event.setProperty("librarycollection", inCollectionid);
			getMediaArchive().getEventManager().fireEvent(event);

		}

	}

	public int rejectSelection(WebPageRequest inReq, HitTracker inHits, String inCollectionid, User inUser, String inNote)
	{
		Collection tosave = new ArrayList();
		int approved = 0;
		Searcher searcher = getMediaArchive().getAssetSearcher();
		for (Iterator iterator = inHits.getSelectedHitracker().iterator(); iterator.hasNext();)
		{
			Data asset = (Data) iterator.next();
			asset = searcher.loadData(asset);
			asset.setValue("editstatus", "rejected");
			tosave.add(asset);
			approved++;
			if (tosave.size() > 400)
			{
				searcher.saveAllData(tosave, null);
				logAssetEvent(tosave, "rejected", inReq.getUser(), inNote, inCollectionid, true);
				tosave.clear();
			}
		}
		// TODO: Save this event to a log
		searcher.saveAllData(tosave, null);
		logAssetEvent(tosave, "rejected", inReq.getUser(), inNote, inCollectionid, true);

		return approved;
	}

	public void configureCollection(LibraryCollection collection, String inUser)
	{
		// Make sure the root folder is within the library root folder
		MediaArchive mediaArchive = getMediaArchive();
		// Make sure we have a root category
		String type = collection.get("collectiontype");
		if (type == null)
		{
			collection.setValue("collectiontype", "1");
			mediaArchive.getSearcher("librarycollection").saveData(collection);
		}

		//		String collectionroot = mediaArchive.getCatalogSettingValue("collection_root");
		//		if (collectionroot == null) {
		//			collectionroot = "Collections";
		//		}

		Category collectioncategory = getRootCategory(mediaArchive, collection);

		// Make sure the name still matches
		if (collectioncategory != null && !collectioncategory.getName().equals(collection.getName()))
		{
			collectioncategory.setName(collection.getName());
		}

		//ProjectManager manager = mediaArchive.getProjectManager();
		((MultiValued) collectioncategory).addValue("viewusers", collection.get("owner"));

		mediaArchive.getCategorySearcher().saveCategory(collectioncategory);

		// Move the parents if needed
		// if( !collectioncategory.hasParent(librarycategory.getId()))
		// {
		// //Move the child into the parent
		// librarycategory.addChild(collectioncategory);
		// mediaArchive.getCategorySearcher().saveData(collectioncategory);
		// }

	}


	/*
	 * 
	 * public void processCheckinRequest(Desktop desktop, JSONObject inMap) {
	 * String collectionid = (String) inMap.get("collectionid"); MediaArchive
	 * archive = getMediaArchive(); LibraryCollection collection =
	 * getLibraryCollection(archive, collectionid);
	 * 
	 * Category topcategory = collection.getCategory(); Map remotefolder =
	 * (Map)inMap.get("root");
	 * 
	 * //get a version name long revision = collection.getCurentRevision();
	 * importClientFolder(remotefolder, topcategory);
	 * 
	 * } protected Collection importClientFolder(Map inRemotefolder, Category
	 * inTopcategory) { //String filename =
	 * (String)inRemotefolder.get("foldername"); Collection childfolders =
	 * (Collection)inRemotefolder.get("childfolders"); Map toremoveonserver =
	 * new HashMap(); for (Iterator iterator =
	 * inTopcategory.getChildren().iterator(); iterator.hasNext();) { Category
	 * child = (Category) iterator.next();
	 * toremoveonserver.put(child.getName(),child); } for (Iterator iterator =
	 * childfolders.iterator(); iterator.hasNext();) { Map remotechildfolder =
	 * (Map) iterator.next(); String foldername =
	 * (String)remotechildfolder.get("foldername"); if(
	 * toremoveonserver.containsKey(foldername)) {
	 * toremoveonserver.remove(foldername); } else { //addem Category toadd =
	 * (Category)getMediaArchive().getCategorySearcher().createNewData();
	 * toadd.setName(foldername); inTopcategory.addChild(toadd); } } for
	 * (Iterator iterator = toremoveonserver.values().iterator();
	 * iterator.hasNext();) { String key = (String) iterator.next(); Category
	 * child = (Category)toremoveonserver.get(key);
	 * getMediaArchive().getCategorySearcher().delete(child, null); } Collection
	 * filelist = (Collection)inRemotefolder.get("filelist");
	 * 
	 * //search for assets within a category HitTracker hits =
	 * getMediaArchive().query("asset").exact("category-exact",
	 * inTopcategory.getId()).search();
	 * 
	 * //Save the files we need to get from the client in an uploadqueue table
	 * Collection uploadlist = new ArrayList();
	 * 
	 * 
	 * }
	 * 
	 */

	// public void createCollectionFromSelection(HitTracker inSelection, User
	// inUser)
	// {
	// //Collection assets =
	// inBasket.getOrderManager().findOrderAssets(inBasket.getCatalogId(),
	// inBasket.getId());
	// Collection assets = inBasket.findOrderAssets();
	// LibraryCollection collection = new LibraryCollection();
	//
	// String collectionid;
	//
	// addAssetToCollection(getMediaArchive(), collectionid, assets);

	// }
	public Collection listEditableCollections(WebPageRequest inReq)
	{
		Collection hits = null;
		UserProfile profile = inReq.getUserProfile();

		if (profile != null)
		{
			if ("administrator".equals(profile.get("settingsgroup")))
			{
				hits = getMediaArchive().query("librarycollection").all().search();
			}
			else
			{
				hits = new ArrayList();
				HitTracker filtered = getMediaArchive().query("librarycollection").all().enduser(true).search(inReq);
				int count = 0;
				for (Iterator iterator = filtered.iterator(); iterator.hasNext();)
				{
					count++;
					if (count > 2000)
					{
						break;
					}
					Data data = (Data) iterator.next();
					LibraryCollection collection = (LibraryCollection) getMediaArchive().getSearcher("librarycollection").loadData(data);
					if (canEditCollection(inReq, collection))
					{
						hits.add(collection);
					}
				}
			}
		}
		return hits;
	}

	public HitTracker loadUploads(WebPageRequest inReq)
	{
		//See if we have a station
//		String selectedlibrary = inReq.getRequestParameter("libraryid");
//		if( selectedlibrary == null)
//		{
//			Data library = (Data) inReq.getPageValue("library");
//			if( library != null)
//			{
//				selectedlibrary = library.getId();
//			}
//		}
		String collectionid = inReq.getRequestParameter("collectionid");
		LibraryCollection collection = (LibraryCollection) inReq.getPageValue("librarycol");
		if( collection != null)
		{
			collectionid = collection.getId();
		}
		

		SearchQuery collectionquery = null;
		if( inReq.getJsonRequest() != null )
		{
			collectionquery = new JsonUtil().parseJson(getMediaArchive().getSearcher("librarycollection"), inReq);
		}
		else
		{
			collectionquery = getMediaArchive().getSearcher("librarycollection").addStandardSearchTerms(inReq);
			if( collectionquery == null )
			{
				collectionquery = getMediaArchive().getSearcher("librarycollection").createSearchQuery();
				
				String communitytagcategory = inReq.findPathValue("communitytagcategory");
				if(communitytagcategory != null)
				{
					collectionquery.addExact("communitytagcategory",communitytagcategory);
				}
				
			}
		}
		QueryBuilder builder = getMediaArchive().query("userpost");
		
		builder.exact("poststatus", "published");
		
		HitTracker topuploads = null;

		//If we are on a special URL
		Data communitytag = (Data) inReq.getPageValue("communitytag");
		if( communitytag != null)
		{
			builder.exact("exclusivecontent", false);
			HitTracker 	collections = (HitTracker)inReq.getPageValue("communityprojects");
			if (collections != null && !collections.isEmpty())
			{
				builder.orgroup("librarycollection", collections);
			}
			else 
			{
				
				builder.exact("librarycollection", "NONE");
			}
		}
		else
		{
			if (collectionid != null)
			{
				builder.exact("librarycollection",collectionid);
				if( collection == null)
				{
					collection = (LibraryCollection)getMediaArchive().getCachedData("librarycollection", collectionid);
				}
				if(	!canEditCollection(inReq, collection))
				{
					builder.exact("exclusivecontent", false);
				}
			}
			if( !collectionquery.isEmpty() )
			{
				collectionquery.setHitsPerPage(200);
				collectionquery.addSortBy("name");
				log.info("Searching all user posts " + inReq.getPage().getPath() + " " + collectionquery);
				HitTracker ids = getMediaArchive().getSearcher("librarycollection").search( collectionquery);
				
				SearchQuery orchild = builder.getSearcher().createSearchQuery();
				if( !ids.isEmpty() )
				{
					orchild.addOrsGroup("librarycollection", ids.getPageOfHits());
				}
				Term fulltext = collectionquery.getTermByDetailId("description");
				if( fulltext != null)
				{
					orchild.setAndTogether(false);
					orchild.addFreeFormQuery("description", fulltext.getValue());  //(TExt1 or Text2 ) and Collection ID
				}
				builder.getQuery().addChildQuery(orchild);
				builder.hitsPerPage(ids.getHitsPerPage());
			}
			else
			{
				builder.all();
			}
		}
	
		String topic = inReq.getRequestParameter("topic");
		if (topic != null)
		{
			builder.exact("collectiveproject", topic);
		}

		UserProfile prof = inReq.getUserProfile();
		if( prof != null)
		{
			Collection blocked = prof.getValues("blockedusers");
			if( blocked != null && !blocked.isEmpty() )
			{
				builder.notgroup("owner", blocked );
			}
		}
		
		//#if( !$userprofile.containsValue("blockedusers",$upload.owner) )

		
		topuploads = builder.named("topuploads").sort("uploaddateDown").search(inReq);
		
		String page = inReq.getRequestParameter("page");
		if( page != null)
		{
			topuploads.setPage(Integer.parseInt(page));
		}
		
		inReq.putPageValue("userpost", topuploads);
		
		return topuploads;

	}

	public Boolean isOnTeam(LibraryCollection inCollection, String inUserid)
	{
		Data subscription = getMediaArchive().query("librarycollectionusers").exact("collectionid", inCollection.getId()).exact("ontheteam", "true").exact("followeruser", inUserid).searchOne();
		return subscription != null;
	}
	
	public Set<String> listTeam(Data inCollection)
	{
		Collection users = getMediaArchive().query("librarycollectionusers").exact("collectionid", inCollection.getId()).exact("ontheteam", "true").search();
		Set userids = new HashSet();
		for (Iterator iterator = users.iterator(); iterator.hasNext();)
		{
			Data coluser = (Data) iterator.next();
			String userid  = coluser.get("followeruser");
			userids.add(userid);
		}
		return userids;
	}

	public Collection listCollectionsForFollower(User inUser)
	{
		Collection subscriptions = getMediaArchive().query("librarycollectionusers").exact("followeruser", inUser.getId()).search();
		Set ids = new HashSet();
		for (Iterator iterator = subscriptions.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			ids.add(hit.get("collectionid"));
		}
		HitTracker hits = getMediaArchive().query("librarycollection").ids(ids).sort("name").search();
		hits.setHitsPerPage(50);
		return hits;
	}
	
	public Collection<LibraryCollection> listCollectionsOnTeam(User inUser)
	{
		if( inUser == null)
		{
			return null;
		}
		
		List<LibraryCollection> libraryCollections = (List<LibraryCollection>)getMediaArchive().getCacheManager().get("collections", inUser.getId());

		if( libraryCollections == null)
		{
			libraryCollections = new ArrayList();
			Collection colusers = getMediaArchive().query("librarycollectionusers").exact("ontheteam", "true").exact("followeruser", inUser.getId()).search();
			if( !colusers.isEmpty())
			{
				List ids = new ArrayList(colusers.size());
				for (Iterator iterator = colusers.iterator(); iterator.hasNext();)
				{
					Data hit = (Data) iterator.next();
					ids.add(hit.get("collectionid"));
				}
				Collection instances = getMediaArchive().query("librarycollection").ids(ids).search();
				Searcher searcher = getMediaArchive().getSearcher("librarycollection");
				for (Iterator iterator = instances.iterator(); iterator.hasNext();)
				{
					Data hit = (Data) iterator.next();
					LibraryCollection col = (LibraryCollection)searcher.loadData(hit);
					libraryCollections.add(col);
				}
			}	
			getMediaArchive().getCacheManager().put("collections", inUser.getId(),libraryCollections);
		}
				
		return libraryCollections;
	}

	public boolean doesLike(User inUser, LibraryCollection inCollection)
	{
		Searcher searcher = getMediaArchive().getSearcher("librarycollectionsaved");
		Data liked = searcher.query().exact("librarycollection", inCollection.getId()).exact("userid", inUser.getId()).searchOne();
		return liked != null;
	}

	public void toggleLike(String inCollectionId, String inUserId)
	{
		Searcher searcher = getMediaArchive().getSearcher("librarycollectionsaved");
		Data liked = searcher.query().exact("librarycollection", inCollectionId).exact("userid", inUserId).searchOne();
		if (liked == null)
		{
			liked = searcher.createNewData();
			liked.setValue("librarycollection", inCollectionId);
			liked.setValue("userid", inUserId);
			searcher.saveData(liked);
		}
		else
		{
			searcher.delete(liked, null);
		}
	}

	public HitTracker listLikedCollections(WebPageRequest inReq)
	{
		Searcher searcher = getMediaArchive().getSearcher("librarycollectionsaved");
		HitTracker hits = searcher.query().exact("userid", inReq.getUserName()).search(inReq);

		List ids = new ArrayList();
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			String id = data.get("librarycollection");
			ids.add(id);
		}

		HitTracker liked = getMediaArchive().query("librarycollection").enduser(true).ids(ids).sort("name").named("likedcollections").search(inReq);

		inReq.putPageValue("likedcollections", liked);
		return liked;
	}

	public LibraryCollection recalculateSessions(String inCollectionid)
	{
		Searcher goalsearcher = getMediaArchive().getSearcher("projectgoal"); //tickets
		Searcher tasksearcher = getMediaArchive().getSearcher("goaltask"); //tasks on tickets

		Searcher invoicessearcher = getMediaArchive().getSearcher("collectioninvoice");

		HitTracker invoices = invoicessearcher.query().exact("collectionid", inCollectionid).exact("paymentstatus", "paid").search();

		long sessionspurchased = 0;

		for (Iterator iterator = invoices.iterator(); iterator.hasNext();)
		{
			Data invoice = (Data) iterator.next();
			Collection productlist = invoice.getValues("productlist");
			if (productlist != null)
			{
				for (Iterator iterator2 = productlist.iterator(); iterator2.hasNext();)
				{
					Map map = (Map) iterator2.next();
					String productid = (String) map.get("productid");
					Data product = getMediaArchive().getData("collectiveproduct", productid);
					if (product != null)
					{
						Integer sessioncount = (Integer) product.getValue("sessioncount");
						if(sessioncount != null) {
							sessionspurchased += sessioncount;
						}
					}
				}
			}
		}

		long sessionsused = 0;

		HitTracker sessiontickets = goalsearcher.query().exact("collectionid", inCollectionid).exact("ticketlevel", "3").exact("projectstatus", "completed").search();

		for (Iterator iterator = sessiontickets.iterator(); iterator.hasNext();)
		{
			Data ticket = (Data) iterator.next();
			Integer sessioncount = (Integer) ticket.getValue("sessioncount");
			if (sessioncount != null)
			{
				sessionsused += sessioncount;
			}
		}

		LibraryCollection col = getLibraryCollection(getMediaArchive(), inCollectionid);
		
		if(col.getLong("totalsessionspurchased") != sessionspurchased || col.getLong("totalsessionsused") != sessionsused)
		{
			col.setValue("totalsessionspurchased", sessionspurchased);
			col.setValue("totalsessionsused", sessionsused);
			col.setValue("sessioncalcdate", new Date());
			col.setValue("remainingsessions", sessionspurchased - sessionsused);
			getMediaArchive().saveData("librarycollection", col);
		}	
		return col;
	}

	public UserProfileManager getUserProfileManager()
	{
		return (UserProfileManager)getMediaArchive().getBean("userProfileManager");
	}
	
	public boolean canViewCollection(String inUserId, String inCollectionid)
	{
		LibraryCollection collection = getLibraryCollection(getMediaArchive(), inCollectionid);
		User user = getMediaArchive().getUser(inUserId);
		UserProfile profile = getUserProfileManager().getUserProfile(getMediaArchive().getCatalogId(), inUserId);
		boolean found = canViewCollection(user, profile , collection);

		return found;
	}
	
	public void addMemberToTeam(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive();
		String collectionid= inReq.getRequestParameter("collectionid");
		String firstName = inReq.getRequestParameter("firstName");
		String lastName = inReq.getRequestParameter("lastName");
		String email = inReq.getRequestParameter("email").trim().toLowerCase();
		
		User teamuser = archive.getUserManager().getUserByEmail(email);
		if( teamuser == null)
		{
			String	password = new PasswordGenerator().generate();				
			teamuser = archive.getUserManager().createUser(null, password);
			teamuser.setFirstName(firstName);
			teamuser.setLastName(lastName);
			teamuser.setEmail(email.trim().toLowerCase());
			teamuser.setEnabled(true);
			archive.getUserManager().saveUser(teamuser);
		}
		log.info("Adding user to team " + teamuser.getId());		
		
		Data newUser = archive.query("librarycollectionusers").exact("followeruser", teamuser.getId()).exact("collectionid", collectionid).searchOne();
		if (newUser != null)
		{
			newUser.setValue("ontheteam", true);
			archive.getSearcher("librarycollectionusers").saveData(newUser);
		}
		else
		{
			newUser = archive.getSearcher("librarycollectionusers").createNewData();
			newUser.setValue("collectionid", collectionid);
			newUser.setValue("followeruser", teamuser.getId());
			newUser.setValue("ontheteam",true);
			newUser.setValue("addeddate",new Date());
			archive.getSearcher("librarycollectionusers").saveData(newUser);
		}
		
		//TODO: send email to new user?
	}
	
	public List getTeamUsers(String collectionid) {
		Collection teamUsers = getMediaArchive().query("librarycollectionusers").exact("collectionid", collectionid).exact("ontheteam", "true").search();
		List users = new ArrayList();
		for (Iterator iterator = teamUsers.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			Data user = getMediaArchive().query("user").exact("id", hit.getValue("followeruser").toString()).searchOne();
			users.add(user);
		}
		return users;
	}

	
	public Collection getTasksForGoal(String inGoalId)
	{
		Searcher tasksearcher = (Searcher)getMediaArchive().getSearcher("goaltask");
		QueryBuilder query = tasksearcher.query().exact("projectgoal", inGoalId);
		query.sort("creationdate");
		
		Collection all = query.search();
		return all;
	}
	
	public ListHitTracker mergeEvents(HitTracker uploads, Collection messages)
	{
		Stack messageinputs = new Stack();
		if( messages != null)
		{
			List reversed = new ArrayList(messages);
			Collections.reverse(reversed);
			messageinputs.addAll(reversed);
		}

		//Make an object for each in order
		ListHitTracker combinedEvents =  new ListHitTracker();

		Stack inputUploads = new Stack();
		List reversed = new ArrayList(uploads.getPageOfHits());
		Collections.reverse(reversed);
		inputUploads.addAll(reversed);
		while( !inputUploads.isEmpty() || !messageinputs.isEmpty() )
		{
			addNext(combinedEvents,inputUploads,messageinputs);
		}
		return combinedEvents;
	}

	private void addNext(ListHitTracker inCombinedEvents, Stack inInputUploads, Stack inMessageinputs)
	{
		Event newEvent = new Event();
		MultiValued upload = null;
		if( !inInputUploads.isEmpty() )
		{
			upload = (MultiValued)inInputUploads.peek();
		}
		MultiValued message = null;
		if( !inMessageinputs.isEmpty() )
		{
			message = (MultiValued)inMessageinputs.peek();
		}
		if( upload == null)
		{
			newEvent.setType("chatterbox");
			newEvent.setData(message);				
			inMessageinputs.pop();
		}
		else if( message == null)
		{
			newEvent.setType("userupload");
			newEvent.setData(upload);
			inInputUploads.pop();
		}
		else if( DateStorageUtil.getStorageUtil().newerThan(upload.getDate("uploaddate"),message.getDate("date")) )
		{
			newEvent.setType("userupload");
			newEvent.setData(upload);
			inInputUploads.pop();
		}
		else
		{
			newEvent.setType("chatterbox");
			newEvent.setData(message);						
			inMessageinputs.pop();
		}
		inCombinedEvents.add(newEvent);
	}

	public HitTracker viewUserProjects(WebPageRequest inReq)
	{
		HitTracker organizationsuser = getMediaArchive().query("librarycollectionusers").exact("followeruser",inReq.getUserName()).exact("ontheteam","true").search(inReq);
		Collection ids = organizationsuser.collectValues("collectionid"); 
				
		if( ids.isEmpty())
		{
			ids.add("NONE");
		}
		HitTracker hits = getMediaArchive().query("librarycollection").ids(ids).named("hits").orgroup("collectiontype","1").not("organizationstatus","disabled").not("organizationstatus","closed").not("organizationstatus","pendingdelete").sort("name").search(inReq);
		hits.setHitsPerPage(100);

		return hits;
	}


	public int calcDaysRemaining(LibraryCollection inCollection)
	{
		if( inCollection != null )
		{
			Date renewal = inCollection.getDate("startdate");
			if( renewal == null)
			{
				renewal = new Date();
				inCollection.setValue("startdate", renewal);
				MediaArchive archive = getMediaArchive();
				archive.saveData("librarycollection", inCollection);
			}
			Calendar enddate = new GregorianCalendar();
			enddate.setTime(renewal);
			enddate.add(Calendar.YEAR,1);
			float days = DateStorageUtil.getStorageUtil().daysBetweenDates(enddate.getTime(),new Date());
			return (int)days - 1;
		}
		return -1;
	}
}
