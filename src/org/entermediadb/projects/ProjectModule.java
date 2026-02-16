package org.entermediadb.projects;

import java.util.ArrayList;
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
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.asset.upload.FileUpload;
import org.entermediadb.asset.upload.FileUploadItem;
import org.entermediadb.asset.upload.UploadRequest;
import org.entermediadb.websocket.chat.ChatServer;
import org.entermediadb.webui.tree.CategoryCollectionCache;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.cache.CacheManager;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.page.PageRequestKeys;
import org.openedit.profile.UserProfile;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.PathUtilities;
import org.openedit.util.URLUtilities;

public class ProjectModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(ProjectModule.class);

	public void loadCollections(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.findPathValue("catalogid");
		ProjectManager manager = (ProjectManager) getModuleManager().getBean(catalogid, "projectManager");
		manager.loadCollections(inReq, getMediaArchive(inReq));
	}
 
	/*
	 * public void redirectToCollection(WebPageRequest inReq) throws Exception {
	 * String catalogid = inReq.findPathValue("catalogid"); ProjectManager
	 * manager = (ProjectManager) getModuleManager().getBean(catalogid,
	 * "projectManager"); String collectionid =
	 * inReq.getRequestParameter("newcollectionid"); if(collectionid == null) {
	 * collectionid = loadCollectionId(inReq); } String appid =
	 * inReq.findValue("applicationid"); String finalpath = "/" + appid +
	 * "/views/modules/librarycollection/media/" + collectionid + ".html";
	 * //Selected Sub folder? String nodeID =
	 * inReq.getRequestParameter("nodeID"); if (nodeID != null) { finalpath =
	 * finalpath + "?nodeID="+nodeID; } inReq.redirect(finalpath); }
	 */

	public void redirectToCollectionRoot(WebPageRequest inReq) throws Exception
	{
		LibraryCollection collection = loadCollection(inReq);
		if (collection == null)
		{
			return;
		}
		String collectionid = collection.getId();
		if (collectionid != null && collectionid.startsWith("multiedit:"))
		{
			return;
		}

		String collectionroot = inReq.getRequestParameter("collectionroot");
		if (collectionroot == null)
		{
			collectionroot = inReq.findValue("collectionroot");
		}
		if (collectionroot == null)
		{
			throw new OpenEditException("collectionroot not set");
		}
		String finalpath = "";

		if (collectionroot.endsWith(".html"))
		{
			finalpath = collectionroot + "?collectionid=" + collection.getId();
		}
		else
		{

			finalpath = collectionroot + "/" + collection.getId() + "/index.html";
		}

		//Selected Sub folder?
		String nodeID = inReq.getRequestParameter("nodeID");
		if (nodeID != null)
		{
			if (finalpath.contains("?"))
			{
				finalpath = finalpath + "&";
			}
			else
			{
				finalpath = finalpath + "?";
			}
			finalpath = finalpath + "nodeID=" + nodeID;

		}
		String args = inReq.getRequestParameter("args");
		if (args != null)
		{
			if (finalpath.contains("?"))
			{
				finalpath = finalpath + "&";
			}
			else
			{
				finalpath = finalpath + "?";
			}
			finalpath = finalpath + args;

		}
		inReq.redirect(finalpath);

	}

	public void redirectToCategory(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.findPathValue("catalogid");
		ProjectManager manager = (ProjectManager) getModuleManager().getBean(catalogid, "projectManager");
		String categoryid = inReq.getRequestParameter("categoryid");
		String nodeID = inReq.getRequestParameter("nodeID");
		if (categoryid == null)
		{
			if (nodeID != null)
			{
				categoryid = nodeID;
			}
		}
		if (categoryid != null)
		{
			String appid = inReq.findValue("applicationid");
			String finalpath = "/" + appid + "/views/modules/asset/showcategory.html?categoryid=" + categoryid + "";
			if (nodeID != null)
			{
				finalpath = finalpath + "&nodeID=" + nodeID;
			}
			inReq.redirect(finalpath);
		}

	}

	public void loadOpenCollections(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.findPathValue("catalogid");
		ProjectManager manager = (ProjectManager) getModuleManager().getBean(catalogid, "projectManager");
		manager.loadOpenCollections(inReq, getMediaArchive(inReq), 10);
	}

	public void loadMostRecentCollection(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.findPathValue("catalogid");
		ProjectManager manager = (ProjectManager) getModuleManager().getBean(catalogid, "projectManager");
		Collection<LibraryCollection> list = manager.loadOpenCollections(inReq, getMediaArchive(inReq), 1);
		if (!list.isEmpty())
		{
			inReq.putPageValue("librarycol", list.iterator().next());
		}
	}

	public void loadSelectedLibrary(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.findPathValue("catalogid");
		ProjectManager manager = (ProjectManager) getModuleManager().getBean(catalogid, "projectManager");
		Data selected = manager.getCurrentLibrary(inReq.getUserProfile());
		inReq.putPageValue("selectedlibrary", selected);

	}

	public void listConnectedDesktop(WebPageRequest inReq) throws Exception
	{
		//NOOP
	}

	public void setCurrentLibrary(WebPageRequest inReq)
	{
		String catalogid = inReq.findPathValue("catalogid");
		String libraryid = inReq.findValue("selectedlibrary");
		if (libraryid != null)
		{
			ProjectManager manager = (ProjectManager) getModuleManager().getBean(catalogid, "projectManager");
			Data library = manager.setCurrentLibrary(inReq.getUserProfile(), libraryid);
			inReq.putPageValue("selectedlibrary", library);
		}
	}
	// public void savedCollection(WebPageRequest inReq)
	// {
	// MediaArchive archive = getMediaArchive(inReq);
	// Data collection = (Data)inReq.getPageValue("data");
	// if( collection != null)
	// {
	// ProjectManager manager = getProjectManager(inReq);
	// manager.savedCollection(archive,collection,inReq.getUser());
	// }
	// }

	/*
	 * public void addAssetToLibrary(WebPageRequest inReq) { //TODO: Support
	 * multiple selections MediaArchive archive = getMediaArchive(inReq); String
	 * libraryid = inReq.getRequestParameter("libraryid"); String hitssessionid
	 * = inReq.getRequestParameter("hitsses/sionid"); ProjectManager manager =
	 * getProjectManager(inReq);
	 * 
	 * if( hitssessionid != null ) { HitTracker tracker =
	 * (HitTracker)inReq.getSessionValue(hitsses/sionid); if( tracker != null )
	 * { tracker = tracker.getSelectedHitracker(); if( tracker != null &&
	 * tracker.size() > 0 ) { manager.addAssetToLibrary(archive, libraryid,
	 * tracker); inReq.putPageValue("added" , String.valueOf( tracker.size() )
	 * ); return; } } }
	 * 
	 * String assetid = inReq.getRequestParameter("assetid");
	 * manager.addAssetToLibrary(archive, libraryid, assetid);
	 * inReq.putPageValue("added" , "1" );
	 * 
	 * } public void removeFromLibrary(WebPageRequest inReq) { //TODO: Support
	 * multiple selections MediaArchive archive = getMediaArchive(inReq); String
	 * libraryid = inReq.getRequestParameter("libraryid"); String hitssessionid
	 * = inReq.getRequestParameter("hitsses/sionid"); ProjectManager manager =
	 * getProjectManager(inReq);
	 * 
	 * if( hitssessionid != null ) { HitTracker tracker =
	 * (HitTracker)inReq.getSessionValue(hitssessionid); if( tracker != null ) {
	 * tracker = tracker.getSelectedHitracker(); if( tracker != null &&
	 * tracker.size() > 0 ) { manager.removeAssetFromLibrary(archive, libraryid,
	 * tracker); inReq.putPageValue("count" , String.valueOf( tracker.size() )
	 * ); return; } } } }
	 */
	/*
	public void addAssetToCollection(WebPageRequest inReq)
	{
		// TODO: Support multiple selections
		MediaArchive archive = getMediaArchive(inReq);

		// String libraryid = inReq.getRequestParameter("libraryid");
		String librarycollection = inReq.getRequestParameter("collectionid");
		if (librarycollection == null)
		{
			librarycollection = inReq.getRequestParameter("id");
		}
		if (librarycollection == null)
		{
			Data data = (Data) inReq.getPageValue("data");
			if (data != null)
			{
				librarycollection = data.getId();
			}
		}
		if (librarycollection == null)
		{
			log.error("librarycollection not found");
			return;
		}
		inReq.putPageValue("collectionid", librarycollection);
		ProjectManager manager = getProjectManager(inReq);
		String moduleid = inReq.findPathValue("module");
		HitTracker tracker = loadHitTracker(inReq, moduleid);
		if (tracker != null)
		{
			tracker = tracker.getSelectedHitracker();
		}
		if (tracker != null && tracker.size() > 0)
		{
			manager.addAssetToCollection(archive, librarycollection, tracker);
			inReq.putPageValue("added", String.valueOf(tracker.size()));
			return;
		}

		String assetid = inReq.getRequestParameter("assetid");
		if (assetid != null)
		{
			manager.addAssetToCollection(archive, librarycollection, assetid);
			inReq.putPageValue("added", "1");

		}
	}

	public void addAssetsToCollection(WebPageRequest inReq)
	{
		String[] assetids = inReq.getRequestParameters("assetid");
		MediaArchive archive = getMediaArchive(inReq);
		String librarycollection = inReq.getRequestParameter("collectionid");

		if (assetids != null)
		{

			ProjectManager manager = getProjectManager(inReq);
			for (int i = 0; i < assetids.length; i++)
			{
				String assetid = assetids[i];
				manager.addAssetToCollection(archive, librarycollection, assetid);

			}

		}

		ProjectManager manager = getProjectManager(inReq);

		String moduleid = inReq.findPathValue("module");
		HitTracker tracker = loadHitTracker(inReq, moduleid);
		if (tracker != null)
		{
			tracker = tracker.getSelectedHitracker();
		}
		if (tracker != null && tracker.size() > 0)
		{
			log.info("tracker size was" + tracker.size());
			manager.addAssetToCollection(archive, librarycollection, tracker);
			inReq.putPageValue("count", String.valueOf(tracker.size()));
			return;
		}
		inReq.putPageValue("collectionid", librarycollection);
	}

	public void removeAssetFromCollection(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);

		String collectionid = loadCollectionId(inReq);

		ProjectManager manager = getProjectManager(inReq);

		String moduleid = inReq.findPathValue("module");
		HitTracker tracker = loadHitTracker(inReq, moduleid);
		if (tracker != null)
		{
			tracker = tracker.getSelectedHitracker();
		}
		if (tracker != null && tracker.size() > 0)
		{
			manager.removeAssetFromCollection(archive, collectionid, tracker);
			inReq.putPageValue("count", String.valueOf(tracker.size()));
			return;
		}

		String assetid = inReq.findValue("assetid");
		if (assetid != null)
		{

			manager.removeAssetFromCollection(archive, collectionid, assetid);
		}

	}

	// public void searchForAssetsInLibrary(WebPageRequest inReq)
	// {
	// MediaArchive archive = getMediaArchive(inReq);
	// ProjectManager manager = getProjectManager(inReq);
	// Data library = manager.getCurrentLibrary(inReq.getUserProfile());
	// if( library != null)
	// {
	// HitTracker hits = manager.loadAssetsInLibrary(library,archive,inReq);
	// inReq.putPageValue("hits", hits);
	// }
	// }
	public void searchForPendingAssetsOnCollection(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String collectionid = loadCollectionId(inReq);
		if (collectionid == null)
		{
			return;
		}
		ProjectManager manager = getProjectManager(inReq);
		HitTracker all = manager.loadAssetsInCollection(inReq, archive, collectionid);
		if (all == null)
		{
			return;
		}

		//Object caneditdata = inReq.getPageValue("caneditdata");
		//all.getSearchQuery().setValue("caneditdata", caneditdata);
		all.selectAll();
		// String hitsname = inReq.findValue("hitsname");
		inReq.putPageValue("hits", all);
		String sessionId = all.getSessionId();
		inReq.putSessionValue(sessionId, all);
	}

	public void searchForAssetsOnCollection(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String collectionid = loadCollectionId(inReq);
		if (collectionid == null)
		{
			return;
		}
		ProjectManager manager = getProjectManager(inReq);

		HitTracker all = manager.loadAssetsInCollection(inReq, archive, collectionid);
		if (all == null)
		{
			return;
		}
		if (Boolean.parseBoolean(inReq.findValue("alwaysresetpage")))
		{
			all.setPage(1);
		}
		//all.getSearchQuery().setValue("caneditcollection", caneditdata);

		// String hitsname = inReq.findValue("hitsname");
		inReq.putPageValue("hits", all);
		String sessionId = all.getSessionId();
		inReq.putSessionValue(sessionId, all);
	}
*/
	public void getFirstUserCollection(WebPageRequest inReq)
	{
		String collectionid = loadCollectionId(inReq);
		if (collectionid == null)
		{
			Asset asset = getAsset(inReq);
			if (asset != null && asset.getCategories() != null)
			{
				for (Iterator iterator2 = asset.getCategories().iterator(); iterator2.hasNext();)
				{
					Category child = (Category) iterator2.next();
					String foundcollectionid = getCategoryCollectionCache(inReq).findCollectionId(child);
					if (foundcollectionid != null)
					{
						inReq.setRequestParameter("collectionid", foundcollectionid);
						return;
					}
				}
			}
		}

	}

	public LibraryCollection loadCollectionFromFolder(WebPageRequest inReq)
	{
		String colid = PathUtilities.extractDirectoryName(inReq.getPath());
		ProjectManager manager = getProjectManager(inReq);
		LibraryCollection col = manager.getLibraryCollection(getMediaArchive(inReq), colid);
		inReq.putPageValue("librarycol", col);
		return col;

	}
	
	public void loadUserPostFromPathParams(WebPageRequest inReq)
	{
		String template = (String) inReq.getPage().get("route-template");
		String path = PathUtilities.extractNonIndexPagePath(inReq.getPath());
		
		if(template == null || path == null) 
		{
			return;
		}
	
		Map<String, String> params = PathUtilities.extractPathParams(template, path);
		
		Data userpost = getMediaArchive(inReq).query("userpost").exact("urlname", params.get("slug")).searchOne();
		
		inReq.putPageValue("userpost", userpost);
		
	}

	public LibraryCollection loadCollection(WebPageRequest inReq)
	{
		LibraryCollection collection = (LibraryCollection) inReq.getPageValue("librarycol");
		if (collection == null)
		{
			String collectionid = loadCollectionId(inReq);
			
			if (collectionid == null)
			{
				collection = loadCollectionFromFolder(inReq);
			}
			else
			{
				collection = getProjectManager(inReq).getLibraryCollection(getMediaArchive(inReq), collectionid);
			}
			//log.error("No collection id found on " + inReq.getPath());
			//collection = loadCollectionFromCommunityTagFolder(inReq);

		}
		if (collection != null)
		{
			inReq.putPageValue("librarycol", collection);
			inReq.putPageValue("library", collection.getLibrary());
			inReq.putPageValue("projectlink", "/" + collection.get("urlname"));
		}
		return collection;
	}

	public String loadCollectionId(WebPageRequest inReq)
	{
		String collectionid = inReq.getRequestParameter("collectionid");
		if (collectionid == null)
		{
			collectionid = inReq.getRequestParameter("librarycollection");
			if (collectionid == null)
			{
				String collectionidinpath = inReq.getContentProperty("collectionidinpath");
				if (Boolean.parseBoolean(collectionidinpath))
				{
					String assetrootfolder = inReq.getContentProperty("assetrootfolder");
					String ending = inReq.getPath().substring(assetrootfolder.length() + 1);
					int endslash = ending.indexOf("/");
					if (endslash > -1)
					{
						collectionid = ending.substring(0, endslash);
					}
				}
				if (collectionid == null)
				{
					String collectionidonpagename = inReq.findActionValue("collectionidonpagename");
					if (Boolean.parseBoolean(collectionidonpagename))
					{
						collectionid = PathUtilities.extractPageName(inReq.getPath());
					}
				}
			}
			if (collectionid == null)
			{
				collectionid = (String) inReq.getPageValue("collectionid");
				//collectionid = inReq.getRequestParameter("id"); //Causing issues
				if (collectionid == null)
				{
					Data coll = (Data) inReq.getPageValue("librarycol");
					if (coll != null)
					{
						collectionid = coll.getId();
					}
				}
			}

			collectionid = inReq.findValue("collectionid");
		}
		if (collectionid == null)
		{
			String collectionidinpath = inReq.getContentProperty("collectionidinfilename");
			if (Boolean.parseBoolean(collectionidinpath))
			{
				collectionid = PathUtilities.extractPageName(inReq.getContentPage().getName());
			}
		}
		//			LibraryCollection coll = loadCollectionFromFolder(inReq);
		//			if (coll != null) {
		//				collectionid = coll.getId();
		//			}
		//		}
		if (collectionid != null)
		{

			inReq.setRequestParameter("collectionid", collectionid); // This was breaking redirects. Not sure it's needed?

		}
		return collectionid;
	}

	public void savedLibrary(WebPageRequest inReq)
	{
		Data saved = (Data) inReq.getPageValue("data");
		if (saved != null)
		{
			inReq.setRequestParameter("profilepreference", "last_selected_library");
			inReq.setRequestParameter("profilepreference.value", saved.getId());
		}

		// Make libraries public by default. Allow a hidden library check box
		// and user/groups/roles on the hidden ones use a QueryFilter to enforce

		// //Make sure I am in the list of users for the library
		// MediaArchive archive = getMediaArchive(inReq);
		// ProjectManager manager = getProjectManager(inReq);
		// if( manager.addUserToLibrary(archive,saved,inReq.getUser()) )
		// {
		// //reload profile?
		// UserProfile profile = inReq.getUserProfile();
		// profile.getViewCategories().add(saved.getId());
		// }
	}

	public void createCollection(WebPageRequest inReq)
	{
		MediaArchive mediaArchive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);
		Searcher librarysearcher = mediaArchive.getSearcher("librarycollection");
		LibraryCollection saved = (LibraryCollection) librarysearcher.createNewData();
		librarysearcher.updateData(inReq, inReq.getRequestParameters("field"), saved);
		saved.setValue("creationdate", new Date());
		saved.setValue("owner", inReq.getUserName());
		//saved.setValue("library", inReq.getRequestParameter("library.value"));  //optional
		//Data userlibrary = manager.loadUserLibrary(mediaArchive, inReq.getUserProfile());

		librarysearcher.saveData(saved, null); //this fires event ProjectManager.configureCollection

		/*
		 * Category cat = manager.getRootCategory(mediaArchive,
		 * (LibraryCollection) saved); //This creates the library ((MultiValued)
		 * cat).addValue("viewusers", inReq.getUserName());
		 * mediaArchive.getCategorySearcher().saveData(cat);
		 */
		inReq.setRequestParameter("librarycollection", saved.getId());
		inReq.setRequestParameter("collectionid", saved.getId());
		inReq.setRequestParameter("newcollectionid", saved.getId());

		inReq.setRequestParameter("nodeID", (String) saved.getValue("rootcategory"));

		manager.configureCollection(saved, inReq.getUserName());
		inReq.putPageValue("librarycol", saved);
		inReq.putPageValue("librarycollection", saved.getId());

		getCategoryCollectionCache(inReq).addCollection(saved);

	}

	/*
	 * public Data createUserLibrary(WebPageRequest inReq) { MediaArchive
	 * archive = getMediaArchive(inReq); UserProfile profile =
	 * inReq.getUserProfile(); ProjectManager manager =
	 * getProjectManager(inReq); Data userlibrary =
	 * manager.loadUserLibrary(archive, profile);
	 * inReq.setRequestParameter("profilepreference", "last_selected_library");
	 * inReq.setRequestParameter("profilepreference.value",
	 * userlibrary.getId()); return userlibrary; }
	 */
	public void addOpenCollection(WebPageRequest inReq)
	{
		UserProfile profile = inReq.getUserProfile();
		Collection cols = profile.getValues("opencollections");
		String collectionid = null;
		Data collection = (Data) inReq.getPageValue("librarycol");
		if (collection == null)
		{
			collectionid = (String) inReq.getRequestParameter("collectionid");
		}
		if (collectionid == null)
		{
			collection = (Data) inReq.getPageValue("data");
		}
		if (collection != null)
		{
			collectionid = collection.getId();
		}

		if (collectionid != null)
		{
			if (cols == null || !cols.contains(collectionid))
			{
				profile.addValue("opencollections", collectionid);
				cols = profile.getValues("opencollections");
				if (cols.size() > 30)
				{
					List cut = new ArrayList(cols);
					cut = cut.subList(cols.size() - 30, cols.size());
					profile.setValues("opencollections", cut);
				}
			}
			profile.setProperty("selectedcollection", collectionid);
			profile.save(inReq.getUser());
		}
	}

	// Not used anymore
	public void addCollectionTab(WebPageRequest inReq)
	{
		UserProfile profile = inReq.getUserProfile();
		String collectionid = null;
		Data collection = (Data) inReq.getPageValue("librarycol");
		if (collection != null)
		{
			collectionid = collection.getId();
		}
		else
		{
			collectionid = inReq.getRequestParameter("collectionid");
		}
		if (collectionid == null)
		{
			String newcollection = inReq.getRequestParameter("collectionname.value");
			if (newcollection != null)
			{
				MediaArchive archive = getMediaArchive(inReq);
				Searcher searcher = archive.getSearcher("librarycollection");
				Data newcol = searcher.createNewData();
				String libraryid = inReq.getRequestParameter("library.value");
				if (libraryid == null)
				{
					Searcher lsearcher = archive.getSearcher("library");

					Data lib = (Data) searcher.searchById(inReq.getUserName());
					if (lib == null)
					{
						lib = lsearcher.createNewData();
						lib.setId(inReq.getUserName());
						lib.setName(inReq.getUser().getShortDescription());
						lsearcher.saveData(lib, inReq.getUser());
					}
					libraryid = lib.getId();
					inReq.getUserProfile().setProperty("last_selected_library", libraryid);
				}
				newcol.setProperty("library", libraryid);
				newcol.setName(newcollection);
				// searcher.updateData(inReq, fields, data);

				searcher.saveData(newcol, inReq.getUser());
				inReq.setRequestParameter("collectionid", newcol.getId());
				collectionid = newcol.getId();
			}
		}
		Collection cols = profile.getValues("opencollections");
		if (cols == null || !cols.contains(collectionid))
		{
			profile.addValue("opencollections", collectionid);
			profile.save(inReq.getUser());
		}
		profile.setProperty("selectedcollection", collectionid);
	}

	public void closeCollectionTab(WebPageRequest inReq)
	{
		UserProfile profile = inReq.getUserProfile();
		String collectionid = inReq.getRequestParameter("collectionid");
		profile.removeValue("opencollections", collectionid);
		String selcol = profile.get("selectedcollection");
		if (collectionid.equals(selcol))
		{
			profile.setProperty("selectedcollection", null);
		}
		profile.save(inReq.getUser());
	}

	public ProjectManager getProjectManager(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = (ProjectManager) getModuleManager().getBean(archive.getCatalogId(), "projectManager");
		inReq.putPageValue("projectmanager", manager);
		return manager;
	}
/*
	public void addCategoryToCollection(WebPageRequest inReq)
	{
		String[] categoryid = inReq.getRequestParameters("categoryid");
		// if( categoryid == null)
		// {
		// String vals = inReq.getRequestParameter("category.values");
		// if( vals != null)
		// {
		// categoryid = vals.replace(" ","").trim().split(" +");
		// }
		// }
		if (categoryid != null)
		{
			MediaArchive archive = getMediaArchive(inReq);
			ProjectManager manager = getProjectManager(inReq);
			String collectionid = inReq.getRequestParameter("collectionid");
			Data collection = null;
			for (int i = 0; i < categoryid.length; i++)
			{
				collection = manager.addCategoryToCollection(inReq.getUser(), archive, collectionid, categoryid[i]);
			}
			inReq.putPageValue("librarycol", collection);
		}
	}
*/
	// public void loadCategoriesOnCollection(WebPageRequest inReq)
	// {
	// MediaArchive archive = getMediaArchive(inReq);
	// ProjectManager manager = getProjectManager(inReq);
	// String collectionid = loadCollectionId(inReq);
	// if( collectionid == null)
	// {
	// return;
	// }
	// HitTracker categories =
	// manager.loadCategoriesOnCollection(archive,collectionid);
	// //log.info("Found: " + categories.size());
	// inReq.putPageValue("collectioncategories", categories);
	// //inReq.putSessionValue(all.getSessionId(),all);
	// }
	//	public void importCollection(WebPageRequest inReq) {
	//		MediaArchive archive = getMediaArchive(inReq);
	//		ProjectManager manager = getProjectManager(inReq);
	//		String collectionid = loadCollectionId(inReq);
	//		LibraryCollection collection = (LibraryCollection) archive.getData("librarycollection", collectionid);
	//
	//		User user = inReq.getUser();
	//		String outfolder = "/WEB-INF/data/" + archive.getCatalogId() + "/workingfolders/" + user.getId() + "/"
	//				+ collection.getName() + "/";
	//
	//		List paths = archive.getPageManager().getChildrenPathsSorted(outfolder);
	//		if (paths.isEmpty()) {
	//			log.info("No import folders found ");
	//			return;
	//		}
	//		Collections.reverse(paths);
	//		String latest = (String) paths.iterator().next();
	//		latest = latest + "/";
	//		// Need to check if this is unique - increment a counter?
	//		String note = inReq.getRequestParameter("note.value");
	//		if (note == null) {
	//			note = "Auto Created Revision on Import";
	//		}
	//		manager.importCollection(inReq, inReq.getUser(), archive, collectionid, latest, note);
	//		inReq.putPageValue("importstatus", "completed");
	//
	//	}
/*
	public void copyCollection(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);
		LibraryCollection collection = loadCollection(inReq);

		Searcher colsearcher = archive.getSearcher("librarycollection");
		LibraryCollection newcollection = (LibraryCollection) colsearcher.createNewData();
		String[] fields = inReq.getRequestParameters("field");
		colsearcher.updateData(inReq, fields, newcollection);
		colsearcher.saveData(newcollection);

		Category destination = manager.getRootCategory(archive, (LibraryCollection) newcollection);
		Category source = manager.getRootCategory(archive, (LibraryCollection) collection);
		ArrayList assetstosave = new ArrayList();

		manager.copyAssets(assetstosave, inReq.getUser(), archive, newcollection, source, destination, true);

		archive.getAssetSearcher().saveAllData(assetstosave, null);

		inReq.putPageValue("newcollection", newcollection);

	}
*/
	/*
	public void createSnapshot(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);
		String collectionid = loadCollectionId(inReq);

		User user = inReq.getUser();

		String note = inReq.getRequestParameter("note.value");
		if (note == null)
		{
			note = "Snapshot Created";
		}
		manager.snapshotCollection(inReq, user, archive, collectionid, note);

	}

	public void restoreSnapshot(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);
		String collectionid = loadCollectionId(inReq);
		String revision = inReq.getRequestParameter("revision");

		User user = inReq.getUser();

		String note = inReq.getRequestParameter("note.value");
		if (note == null)
		{
			note = "Auto Snapshot on Restore from " + revision;
		}
		manager.restoreSnapshot(inReq, user, archive, collectionid, revision, note);

	}

	public void changeLock(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String collectionid = loadCollectionId(inReq);
		boolean lock = Boolean.parseBoolean(inReq.getRequestParameter("lockcollection"));
		if (lock)
		{
			archive.updateAndSave("librarycollection", collectionid, "lockedby", inReq.getUserName());
		}
		else
		{
			archive.updateAndSave("librarycollection", collectionid, "lockedby", null);
		}

	}
*/
	public boolean checkViewCollection(WebPageRequest inReq)
	{
		ProjectManager manager = getProjectManager(inReq);
		LibraryCollection collection = loadCollection(inReq);

		boolean canview = manager.canViewCollection(inReq, collection);
		if (canview)
		{
			inReq.putPageValue("librarycol", collection);
		}
		return canview;
	}

	public Boolean canEditCollection(WebPageRequest inReq)
	{
		ProjectManager manager = getProjectManager(inReq);
		LibraryCollection collection = loadCollection(inReq);
		boolean caneditcollection = manager.canEditCollection(inReq, collection);

		if (caneditcollection)
		{
			inReq.putPageValue("librarycol", collection);
		}
		return caneditcollection;
	}
	
	public Boolean canViewProfitAndLoss(WebPageRequest inReq)
	{
		ProjectManager manager = getProjectManager(inReq);
		LibraryCollection collection = loadCollection(inReq);
		
		String privacylevel = collection.get("priviacylevel");
		
		if(privacylevel == null || privacylevel.equals("community")) 
		{
			return true;
		}
		
		return isOnTeam(inReq);
	}
	
	
	
	
	//
	// public void loadCategoriesOnCollections(WebPageRequest inReq)
	// {
	// MediaArchive archive = getMediaArchive(inReq);
	// ProjectManager manager = getProjectManager(inReq);
	// Collection<LibraryCollection> collections =
	// manager.loadOpenCollections(inReq,archive);
	//
	// manager.loadCategoriesOnCollections(inReq, archive, collections);
	//
	// }
	
	public void approveAssets(WebPageRequest inReq)
	{
		HitTracker hits = (HitTracker) inReq.getPageValue("hits");
		if (hits != null)
		{
			approveSelection(inReq);
		}
		else 
		{
			approveAsset(inReq);
		}
	}
	
	public void approveAsset(WebPageRequest inReq)
	{
		ProjectManager manager = getProjectManager(inReq);
		MediaArchive archive = getMediaArchive(inReq);
		
		String comment = inReq.getRequestParameter("comment");
		Asset asset = archive.getAsset((String) inReq.getRequestParameter("assetid"));
		if (asset != null)
		{
			manager.approveAsset(asset, inReq.getUser(), comment, null, false);
			inReq.putPageValue("approved", 1);
		}
	}

	public void approveSelection(WebPageRequest inReq)
	{
		ProjectManager manager = getProjectManager(inReq);
		String collectionid = loadCollectionId(inReq);
		String comment = inReq.getRequestParameter("comment");
		
		HitTracker hits = (HitTracker) inReq.getPageValue("hits");
		if (hits != null)
		{
			int count = manager.approveSelection(inReq, hits, collectionid, inReq.getUser(), comment);
			inReq.putPageValue("approved", count);
			Searcher searcher = getMediaArchive(inReq).getAssetSearcher();
			inReq.setRequestParameter(searcher.getSearchType() + "clearselection", "true");
			searcher.loadHits(inReq);
		}
	}
	
	public void rejectAssets(WebPageRequest inReq)
	{
		HitTracker hits = (HitTracker) inReq.getPageValue("hits");
		if (hits != null)
		{
			rejectSelection(inReq);
		}
		else 
		{
			rejectAsset(inReq);
		}
	}
	
	public void rejectAsset(WebPageRequest inReq)
	{
		ProjectManager manager = getProjectManager(inReq);
		MediaArchive archive = getMediaArchive(inReq);
		
		String comment = inReq.getRequestParameter("comment");
		Asset asset = archive.getAsset((String) inReq.getRequestParameter("assetid"));
		if (asset != null)
		{
			manager.rejectAsset(asset, inReq.getUser(), comment, null, false);
			inReq.putPageValue("rejected", 1);
		}
	}

	public void rejectSelection(WebPageRequest inReq)
	{
		HitTracker hits = (HitTracker) inReq.getPageValue("hits");
		ProjectManager manager = getProjectManager(inReq);
		String collectionid = loadCollectionId(inReq);
		String comment = inReq.getRequestParameter("comment");
		if (hits != null)
		{
			int count = manager.rejectSelection(inReq, hits, collectionid, inReq.getUser(), comment);
			inReq.putPageValue("rejected", count);
			Searcher searcher = getMediaArchive(inReq).getAssetSearcher();
			inReq.setRequestParameter(searcher.getSearchType() + "clearselection", "true");
			searcher.loadHits(inReq);
		}

	}

	public void createQuickGallery(WebPageRequest inReq)
	{
		String moduleid = inReq.findPathValue("module");
		HitTracker tracker = loadHitTracker(inReq, moduleid);

		MediaArchive archive = getMediaArchive(inReq);
		Searcher assetsearcher = archive.getAssetSearcher();
		Searcher collections = archive.getSearcher("librarycollection");
		LibraryCollection collection = (LibraryCollection) collections.createNewData();
		String[] fields = inReq.getRequestParameters("field");
		collections.updateData(inReq, fields, collection);

		// collection.setValue("visibility", "hidden");

		Searcher categories = archive.getSearcher("category");

		String collectionroot = archive.getCatalogSettingValue("gallery_root");
		if (collectionroot == null)
		{
			collectionroot = "Collections";
		}

		Category newcat = archive.createCategoryPath(collectionroot + "/Galleries/" + inReq.getUserName() + "/" + collection.getName());

		// newcat.setValue("visibility", "hidden");
		newcat.setName(collection.getName());

		categories.saveData(newcat);

		collection.setValue("rootcategory", newcat.getId());
		collection.setValue("creationdate", new Date());
		collection.setValue("owner", inReq.getUserName());
		collection.setValue("visibility", "3");
		ArrayList assets = new ArrayList();

		for (Iterator iterator = tracker.getSelectedHitracker().iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			Asset asset = (Asset) assetsearcher.loadData(hit);
			asset.addCategory(newcat);
			assets.add(asset);
			if (assets.size() > 1000)
			{
				assetsearcher.saveAllData(assets, null);
				assets.clear();
			}

		}
		assetsearcher.saveAllData(assets, null);
		collections.saveData(collection);

		inReq.putPageValue("librarycol", collection);
	}

	public void loadUserUpload(WebPageRequest inReq) throws Exception
	{
		String page = inReq.getPage().getName();
		MediaArchive archive = getMediaArchive(inReq);
		Searcher userupload = archive.getSearcher("userupload");
		Data upload = userupload.query().exact("uploadcategory", PathUtilities.extractPageName(page)).searchOne();
		inReq.putPageValue("userupload", upload);
	}

	/*
	 *
	 * Server ProjectManager.retrieveFilesFromClient Server
	 * Desktop.importCollection Client AppController.checkinFiles Client
	 * EnterMediaModel.pushFolder Server ProjectModule.syncRemoteFolder Client
	 * EnterMediaModule.uploadFilesIntoCollection Server
	 * ProjectModule.uploadFile
	 * 
	 */
	//	public void uploadMedia(WebPageRequest inReq) throws Exception
	//	{
	//		FileUpload command = new FileUpload();
	//		command.setPageManager(getPageManager());
	//		UploadRequest properties = command.parseArguments(inReq);
	//		if (properties == null)
	//		{
	//			return;
	//		}
	//
	//		//MediaArchive archive = getMediaArchive(inReq);
	//
	//		String savepath = inReq.getRequestParameter("savepath");
	//		FileUploadItem item = properties.getFirstItem();
	//		
	//		//String savepath = "/WEB-INF/data/" + catalogid +"/origin//als/" + sourcepath;
	//		ContentItem contentitem = properties.saveFileAs(item, savepath, inReq.getUser());
	//		
	//	}

	public void uploadFile(WebPageRequest inReq) throws Exception
	{
		//MediaArchive archive = getMediaArchive(inReq);
		FileUpload command = new FileUpload();
		command.setPageManager(getPageManager());
		UploadRequest properties = command.parseArguments(inReq);
		if (properties == null)
		{
			return;
		}
		//String folderdetailsstr = inReq.getRequestParameter("folderdetails");
		//Map folderdetails = (Map)new JSONParser().parse(folderdetailsstr);
		String subfolder = (String) inReq.getRequestParameter("subfolder");
		String collectionid = (String) inReq.getRequestParameter("collectionid");
		String assetid = inReq.getRequestParameter("assetid");
		String catalogid = inReq.getRequestParameter("catalogid");
		String assetmodificationdate = inReq.getRequestParameter("assetmodificationdate");
		//Should only be one asset
		FileUploadItem item = properties.getFirstItem();
		//Move to right place
		log.info("save to " + collectionid + subfolder + "/" + item.getFileItem().getName());
		MediaArchive archive = getMediaArchive(catalogid);
		LibraryCollection collection = archive.getProjectManager().getLibraryCollection(archive, collectionid);
		String sourcepath = collection.getCategory().getCategoryPath();
		if (subfolder != null)
		{
			sourcepath = sourcepath + subfolder + "/" + item.getFileItem().getName();
		}
		else
		{
			sourcepath = sourcepath + "/" + item.getFileItem().getName();
		}
		String savepath = "/WEB-INF/data/" + catalogid + "/originals/" + sourcepath;
		ContentItem contentitem = properties.saveFileAs(item, savepath, inReq.getUser());
		//set mod date assetmodificationdate
		Asset asset = null;
		if (assetid != null)
		{
			asset = archive.getAsset(assetid);
		}
		if (asset == null)
		{
			asset = (Asset) archive.getAssetSearcher().createNewData();
			asset.setSourcePath(sourcepath);
		}
		asset.setProperty("importstatus", "needsmetadata");
		asset.setValue("assetmodificationdate", contentitem.lastModified()); //This needs to be set or it will keep thinking it's changed
		asset.setProperty("editstatus", "1"); //pending

		//Category use ID?
		Category cat = null;
		if (subfolder == null)
		{
			cat = collection.getCategory();
		}
		else
		{
			cat = archive.createCategoryPath(collection.getCategory().getCategoryPath() + subfolder);
		}
		asset.addCategory(cat);
		archive.saveAsset(asset);
		//Fire event
		archive.fireSharedMediaEvent("importing/assetscreated");

	}

	public void searchCategories(WebPageRequest inPageRequest) throws Exception
	{
		MediaArchive archive = getMediaArchive(inPageRequest);
		//ProjectManager manager = getProjectManager(inPageRequest);

		Category category = null;
		String categoryId = inPageRequest.getRequestParameter("selectedcategory");
		String CATEGORYID = "categoryid";
		if (categoryId == null)
		{
			categoryId = inPageRequest.getRequestParameter(CATEGORYID);
		}
		if (categoryId == null)
		{
			categoryId = inPageRequest.getRequestParameter("nodeID");
		}
		//get it from collectionid
		LibraryCollection librarycol = loadCollection(inPageRequest);
		if (categoryId == null && librarycol != null)
		{
			categoryId = librarycol.getRootCategoryId();
		}
		if (categoryId != null)
		{
			inPageRequest.putPageValue("categoryid", categoryId);
			category = archive.getCategory(categoryId);
		}

		if (category != null)
		{
			inPageRequest.putPageValue("category", category);
			inPageRequest.putPageValue("selectedcategory", category);
			//LibraryCollection librarycol = loadCollection(inPageRequest);

			QueryBuilder q = archive.getAssetSearcher().query().enduser(true);
			//if( Boolean.parseBoolean( inPageRequest.getRequestParameter("showchildassets") ) )
			//			{
			q.exact("category", category.getId());
			//			}
			//			else
			//			{
			//				q.exact("category-exact",category.getId());
			//			}
			//			Boolean caneditdata = (Boolean) inPageRequest.getPageValue("caneditcollection");
			//			
			//			if (!caneditdata) 
			//			{
			//				Searcharchive.getAssetSearcher().createSearchQuery();
			//				q.orgroup("editstatus", "6");
			//			}

			HitTracker tracker = q.search(inPageRequest);
			if (tracker != null)
			{
				tracker.setDataSource(archive.getCatalogId() + "/categories/" + category.getId());
				if (librarycol != null)
				{
					tracker.getSearchQuery().setProperty("collectionid", librarycol.getId());
				}
				tracker.getSearchQuery().setProperty("categoryid", category.getId());
				tracker.setPage(1);

			}
		}
	}

	public CategoryCollectionCache getCategoryCollectionCache(WebPageRequest inPageRequest)
	{
		String catalogid = inPageRequest.findPathValue("catalogid");
		CategoryCollectionCache cache = (CategoryCollectionCache) getModuleManager().getBean(catalogid, "categoryCollectionCache", true);
		inPageRequest.putPageValue("categoryCollectionCache", cache);
		return cache;
	}

	public void loadUploads(WebPageRequest inReq)
	{
		ProjectManager manager = getProjectManager(inReq);
		manager.loadUploads(inReq);
	}

	public Boolean isOnTeam(WebPageRequest inReq)
	{
		if (inReq.getUser() == null)
		{
			return false;
		}
		ProjectManager manager = getProjectManager(inReq);
		LibraryCollection collection = loadCollection(inReq);
		if (collection == null)
		{
			return false;
		}
		String userid = inReq.getUserName();
		Boolean isOnEditTeam = manager.isOnTeam(collection, userid);
		inReq.putPageValue("ison_" + collection.getId(), isOnEditTeam);
		inReq.putPageValue("isonteam", isOnEditTeam);
		return isOnEditTeam;
	}

	public void listCollectionsOnTeam(WebPageRequest inReq)
	{
		if (inReq.getUser() == null)
		{
			return;
		}
		ProjectManager manager = getProjectManager(inReq);
		Collection workspaces = manager.listCollectionsOnTeam(inReq.getUser());
		inReq.putPageValue("workspaces", workspaces);
	}

	public void toggleLike(WebPageRequest inReq)
	{
		ProjectManager manager = getProjectManager(inReq);
		LibraryCollection collection = loadCollection(inReq);
		manager.toggleLike(collection.getId(), inReq.getUserName());
	}

	public void listLikedCollections(WebPageRequest inReq)
	{
		ProjectManager manager = getProjectManager(inReq);
		manager.listLikedCollections(inReq);
	}

	public void loadMessagesCollection(WebPageRequest inReq)
	{
		ProjectManager manager = getProjectManager(inReq);
		LibraryCollection collection = manager.getMessagesCollection(inReq.getUser());
		inReq.putPageValue("librarycol", collection);

		TopicLabelPicker labels = new TopicLabelPicker();
		labels.setArchive(getMediaArchive(inReq));
		labels.setLibraryCollection(collection);
		inReq.putPageValue("topiclabels", labels);
	}

	public void loadLibraryByDomain(WebPageRequest inReq)
	{

		URLUtilities utils = (URLUtilities) inReq.getPageValue("url_util");
		if (utils != null)
		{
			String domain = utils.getDomain();
			MediaArchive archive = getMediaArchive(inReq);
			String libraryid = (String) archive.getCacheManager().get("domaincache", domain);
			if (libraryid == CacheManager.NULLVALUE)
			{
				return;
			}
			if (libraryid != null)
			{
				Data library = archive.getCachedData("library", libraryid);
				if (library != null)
				{
					inReq.putPageValue("library", library);
					return;
				}
			}
			//Cache
			Data found = archive.query("library").startsWith("communitysubdomain", domain).searchOne();
			if (found == null)
			{
				libraryid = CacheManager.NULLVALUE;
			}
			else
			{
				inReq.putPageValue("library", found);
				libraryid = found.getId();
			}
			archive.getCacheManager().put("domaincache", domain, libraryid);
		}
	}
	/*
	 * public void uploadRemoteFolderCache(WebPageRequest inReq) { MediaArchive
	 * archive = getMediaArchive(inReq); ProjectManager manager =
	 * getProjectManager(inReq);
	 * 
	 * Map params = inReq.getJsonRequest(); Desktop desktop =
	 * manager.getDesktopManager().getDesktop(inReq.getUserName()); String
	 * rootfolder = (String)params.get("rootfolder"); Map folderdetails =
	 * (Map)params.get("folderdetails"); desktop.addLocalFileCache(archive,
	 * inReq.getUserName(), rootfolder, folderdetails);
	 * 
	 * //TODO: See error }
	 */

	public void loadActivity(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);

		Collection collections = manager.listCollectionsForFollower(inReq.getUser());
		if (collections.isEmpty())
		{
			collections.add("NONE");
		}

		Collection topics = archive.query("collectiveproject").orgroup("parentcollectionid", collections).hitsPerPage(100).search();

		Searcher chats = archive.getSearcher("chatterbox");

		HitTracker recent = chats.query().orgroup("channel", topics).sort("dateDown").search();
		inReq.putPageValue("messages", recent);
	}

	public void updateCatalogSetting(WebPageRequest inReq)
	{
		String id = inReq.getRequestParameter("id");
		String label = inReq.getRequestParameter("label");
		String value = inReq.getRequestParameter("value");
		if (id == null || id.equals(""))
		{
			inReq.putPageValue("status", false);
			inReq.putPageValue("reason", "id cannot be empty");
			return;
		}
		if (value == null)
		{
			inReq.putPageValue("status", false);
			inReq.putPageValue("reason", "value is null");
			return;
		}
		MediaArchive mediaArchive = getMediaArchive(inReq);
		Searcher instanceSearcher = mediaArchive.getSearcher("catalogsettings");

		Data catalogSetting = instanceSearcher.query().exact("id", id).searchOne();
		inReq.putPageValue("id", id);
		inReq.putPageValue("oldValue", catalogSetting.getValue("value"));
		catalogSetting.setProperty("label", label);
		catalogSetting.setProperty("value", value);
		instanceSearcher.saveData(catalogSetting);
		inReq.putPageValue("newValue", value);
		inReq.putPageValue("status", true);
	}

	public void clearDataCache(WebPageRequest inReq)
	{
		MediaArchive mediaArchive = getMediaArchive(inReq);
		mediaArchive.clearAll();
		inReq.putPageValue("status", "ok");
	}

	public void addMemberToTeam(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String collectionid = inReq.getRequestParameter("collectionid");
		String email = inReq.getRequestParameter("email");

		if (email == null)
		{
			inReq.putPageValue("reason", "You must provide email");
			return;
		}
		email = email.trim().toLowerCase();
		inReq.putPageValue("status", false);
		User user = inReq.getUser();
		if (user == null)
		{
			inReq.putPageValue("reason", "Invalid user");
			return;
		}

		ProjectManager projectManager = getProjectManager(inReq);
		if (collectionid == null)
		{
			inReq.putPageValue("reason", "You must provide a collectionid");
			return;
		}
		LibraryCollection collection = projectManager.getLibraryCollection(archive, collectionid);
		// checking requesting user belongs to team
		if (collection == null || !projectManager.isOnTeam(collection, user.getId()))
		{
			inReq.putPageValue("reason", "Invalid Collectionid");
			return;
		}
		// check if user is already on team
		User newUser = archive.getUserManager().getUserByEmail(email);
		if (newUser == null || !projectManager.isOnTeam(collection, newUser.getId()))
		{
			projectManager.addMemberToTeam(inReq);
		}
		inReq.putPageValue("status", true);
	}

	public void getTeamUsers(WebPageRequest inReq)
	{
		String collectionid = inReq.getRequestParameter("collectionid");
		inReq.putPageValue("status", false);
		if (collectionid == null)
		{
			inReq.putPageValue("reason", "Invalid Collectionid");
			return;
		}
		ProjectManager projectManager = getProjectManager(inReq);
		Collection users = projectManager.getTeamUsers(collectionid);
		if (users == null)
		{
			inReq.putPageValue("reason", "Invalid Collectionid");
			return;
		}
		inReq.putPageValue("users", users);
		inReq.putPageValue("status", true);

	}

	public void getDockerId(WebPageRequest inReq)
	{
		inReq.putPageValue("instancemonitorid", inReq.getRequestParameter("instancemonitorid"));
	}

	public void loadCommunityTag(WebPageRequest inReq)
	{
		if (inReq.getPageValue("communitytag") != null)
		{
			return;
		}
		URLUtilities util = (URLUtilities) inReq.getPageValue(PageRequestKeys.URL_UTILITIES);
		String subdomain = util.buildRoot();
		if (subdomain != null)
		{
			String[] parts = subdomain.split("\\.");
			if (parts.length > 2)
			{
				String tag = parts[0].toLowerCase();
				tag = tag.substring(tag.lastIndexOf("/") + 1);
				MediaArchive archive = getMediaArchive(inReq);
				Data data = (Data) archive.getCacheManager().get("communitytag", tag);
				if (data == null)
				{
					data = archive.query("communitytag").exact("subdomain", tag).searchOne();
					if (data == null)
					{
						data = CacheManager.NULLDATA;
					}
					else
					{
						HitTracker collections = archive.query("librarycollection").exact("communitytag", data.getId()).search(inReq);
						inReq.putPageValue("communityprojects", collections);
						archive.getCacheManager().put("communityprojects", tag, collections);
					}
					archive.getCacheManager().put("communitytag", tag, data);
				}
				if (data != CacheManager.NULLDATA)
				{
					inReq.putPageValue("communitytag", data);
					Collection communityprojects = (Collection) archive.getCacheManager().get("communityprojects", tag);
					inReq.putPageValue("communityprojects", communityprojects);
				}
			}
		}
	}

	public Map loadGoalsInMessages(WebPageRequest inReq)
	{
		Map goalspermessage = new HashMap<String, Data>();

		HitTracker messages = (HitTracker) inReq.getPageValue("messages");
		String messageid = inReq.getRequestParameter("chatid");
		List ids = new ArrayList();
		if (messages != null)
		{
			for (Iterator iterator = messages.getPageOfHits().iterator(); iterator.hasNext();)
			{
				Data message = (Data) iterator.next();
				ids.add(message.getId());
			}
			if (ids.isEmpty())
			{
				ids.add("NONE");
			}

		}
		else if (messageid != null)
		{
			ids.add(messageid);
		}

		if (ids.size() < 1)
		{
			//log.error("No messages found");
			return null;
		}

		Collection goals = getMediaArchive(inReq).query("projectgoal").orgroup("chatparentid", ids).search();
		inReq.putPageValue("goalhits", goals);
		for (Iterator iterator = goals.iterator(); iterator.hasNext();)
		{
			Data goal = (Data) iterator.next();
			goalspermessage.put(goal.get("chatparentid"), goal);
		}

		inReq.putPageValue("goalsinmessages", goalspermessage);
		return goalspermessage;
	}

	public void loadGoalInMessage(WebPageRequest inReq)
	{
		Map goalspermessage = new HashMap<String, Data>();
		Data chat = (Data) inReq.getPageValue("chat");
		Data goal = getMediaArchive(inReq).query("projectgoal").orgroup("chatparentid", chat.getId()).searchOne();
		if (goal != null)
		{
			goalspermessage.put(goal.get("chatparentid"), goal);
		}
		inReq.putPageValue("goalsinmessages", goalspermessage);

	}

	public HitTracker loadMessages(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);

		LibraryCollection collection = loadCollection(inReq);

		if (collection == null)
		{
			ListHitTracker empty = new ListHitTracker();
			inReq.putPageValue("messages", empty);
			return empty;
		}
		//Check permissions
		QueryBuilder q = archive.query("collectiveproject").exact("parentcollectionid", collection).named("topics").hitsPerPage(10);

		boolean only = Boolean.parseBoolean(inReq.findValue("onlyshowpublictopics"));
		if (only)
		{
			q.exact("teamproject", false);
		}
		Collection topics = q.search(inReq);
		if (topics.isEmpty())
		{
			return new ListHitTracker();
		}
		inReq.putPageValue("topics", topics);

		Searcher chats = archive.getSearcher("chatterbox");

		HitTracker recent = chats.query().orgroup("channel", topics).named("hits").sort("dateDown").hitsPerPage(200).search(inReq);
		inReq.putPageValue("messages", recent);

		List messageids = new ArrayList(recent.size());
		for (Iterator iterator = recent.getPageOfHits().iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			messageids.add(data.getId());
		}
		inReq.putPageValue("messageids", messageids);

		if (recent != null)
		{
			Set users = new HashSet();
			for (Iterator iterator = recent.iterator(); iterator.hasNext();)
			{
				Data chat = (Data) iterator.next();
				User person = archive.getUser(chat.get("user"));
				if (person != null)
				{
					users.add(person);
				}
			}
			inReq.putPageValue("persons", users);
		}
		return recent;
	}

	public void loadTopMessages(WebPageRequest inReq)
	{
		if (inReq.getUser() == null)
		{
			return;
		}

		String userid = inReq.getRequestParameter("userid");
		if (userid == null)
		{
			userid = inReq.getUserName();
		}
		else
		{
			if (!inReq.getUserProfile().isInRole("administrator"))
			{
				inReq.putPageValue("error", "Non Admin is trying to get other users messages");
				//return;
				userid = inReq.getUserName();
			}
		}

		MediaArchive mediaArchive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);

		Collection librarycollections = null;

		boolean loadAll = Boolean.parseBoolean(inReq.findValue("loadAllMessages"));
		if (loadAll && inReq.getUserProfile().isInRole("administrator"))
		{
			HitTracker workspaces = mediaArchive.query("librarycollection").exact("library", "userscollections").hitsPerPage(100).search(inReq);
			librarycollections = workspaces.collectValues("id");
		}
		else
		{
			HitTracker workspaces = mediaArchive.query("librarycollectionusers").exact("ontheteam", "true").hitsPerPage(100).exact("followeruser", userid).search(inReq);
			librarycollections = workspaces.collectValues("collectionid");
		}

		if (librarycollections.isEmpty())
		{
			inReq.putPageValue("error", "No such user in any collection");
			return;
		}

		HitTracker moddifiedcol = mediaArchive.query("chattopiclastmodified").orgroup("collectionid", librarycollections).hitsPerPage(100).search(inReq);
		if (moddifiedcol.isEmpty())
		{
			inReq.putPageValue("error", "No messages modifield");
			return;
		}
		Collection messages = moddifiedcol.collectValues("messageid");

		Searcher chats = mediaArchive.getSearcher("chatterbox");
		HitTracker recent = chats.query().orgroup("id", messages).named("hits").sort("dateDown").hitsPerPage(50).search(inReq);
		inReq.putPageValue("messages", recent);
		if (recent != null)
		{
			List messageids = new ArrayList(recent.size());
			for (Iterator iterator = recent.iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				messageids.add(data.getId());
			}
			inReq.putPageValue("messageids", messageids);
		}
	}

	public void loadUploadsInMessages(WebPageRequest inReq)
	{
		ProjectManager manager = getProjectManager(inReq);
		HitTracker uploads = manager.loadUploads(inReq);

		inReq.setRequestParameter("onlyshowpublictopics", "true");
		Collection messages = (Collection) loadMessages(inReq);
		ListHitTracker combinedEvents = manager.mergeEvents(uploads, messages);
		inReq.putPageValue("combinedevents", combinedEvents);
	}

	public void viewProjects(WebPageRequest inReq)
	{
		ProjectManager manager = getProjectManager(inReq);
		HitTracker hits = manager.viewUserProjects(inReq);
		inReq.putPageValue("hits", hits);

	}

	public void checkUrlName(WebPageRequest inReq)
	{
		LibraryCollection collection = (LibraryCollection) inReq.getPageValue("librarycol");
		if (collection != null)
		{
			Searcher searcher = loadSearcher(inReq); //Product?
			String name = inReq.getContentPage().getName();
			Data found = searcher.query().exact("urlname", name).searchOne();

			inReq.putPageValue(searcher.getSearchType(), found);
		}

	}

	public void loadCollectionChild(WebPageRequest inReq)
	{
		if (inReq.getContentPage().exists())
		{
			return;
		}

		Data collection = (Data) inReq.getPageValue("librarycol");
		if (collection != null)
		{
			Searcher searcher = loadSearcher(inReq); //Product?
			String name = inReq.getContentPage().getDirectoryName();
			Data found = searcher.query().exact("urlname", name).searchOne();
			if (found != null)
			{
				inReq.putPageValue(searcher.getSearchType(), found);
			}
		}

	}

	public void trackReferralCode(WebPageRequest inReq)
	{
		String referralcode = inReq.getRequestParameter("fbclid");

		if (referralcode != null)
		{
			MediaArchive mediaArchive = getMediaArchive(inReq);
			Data exists = (Data) mediaArchive.getCachedData("collectivelinkreferralcode", referralcode);
			if (exists == null)
			{
				exists = (Data) mediaArchive.getSearcher("collectivelinkreferralcode").createNewData();
				exists.setId(referralcode);
				exists.setName("Auto Created");
				exists.setValue("collectionid", referralcode); //Assume this is a project could be OI from community area tho
				mediaArchive.saveData("collectivelinkreferralcode", exists);

//				Data collection = (Data) mediaArchive.getCachedData("librarycollection", referralcode);
//				if (collection == null)
//				{
//					Searcher searcher = mediaArchive.getSearcher("librarycollection");
//					Data librarycol = searcher.createNewData();
//					librarycol.setId(referralcode);
//					librarycol.setName(referralcode);
//					librarycol.setValue("collectiontype", "4");
//					searcher.saveData(librarycol);
//				}

			}
			Data newcode = mediaArchive.getSearcher("collectivelinktracker").createNewData();
			//newcode.setValue("collectionid", exists.getValue("collectionid"));
			newcode.setValue("referralcode", exists.getId());

			populateTracker(inReq, newcode);

			mediaArchive.saveData("collectivelinktracker", newcode);
			inReq.putSessionValue("trackingsession", newcode);

		}
		else
		{
			MediaArchive mediaArchive = getMediaArchive(inReq);

			Data trackingsession = (Data) inReq.getSessionValue("trackingsession");
			if (trackingsession != null)
			{
				Data newcode = mediaArchive.getSearcher("collectivelinktracker").createNewData();
				newcode.setValue("collectionid", trackingsession.getValue("collectionid"));
				newcode.setValue("referralcode", trackingsession.getValue("referralcode"));
				newcode.setValue("firsttrakedlink", trackingsession.getId());
				populateTracker(inReq, newcode);
				mediaArchive.saveData("collectivelinktracker", newcode);
			}
		}

	}

	private void populateTracker(WebPageRequest inReq, Data newcode)
	{
		newcode.setValue("date", new Date());
		newcode.setValue("user", inReq.getUserName());
		String ipaddress = inReq.getRequest().getHeader("X-Forwarded-For");
		if(ipaddress == null)
		{
			ipaddress = inReq.getRequest().getHeader("X-Real-IP");
		}
		if(ipaddress == null)
		{
			ipaddress = inReq.getRequest().getRemoteAddr();
		}
		newcode.setValue("ipaddress", ipaddress);
		String referral = inReq.getRequest().getHeader("Referer");
		newcode.setValue("referralurl", referral);
		newcode.setValue("path", inReq.getPath());
	}

	public Map loadAttachmentsInMessages(WebPageRequest inReq)
	{
		HitTracker messages = (HitTracker) inReq.getPageValue("messages");
		//String messageid = inReq.getRequestParameter("chatid");
		List ids = new ArrayList();
		if (messages != null)
		{
			for (Iterator iterator = messages.getPageOfHits().iterator(); iterator.hasNext();)
			{
				Data message = (Data) iterator.next();
				ids.add(message.getId());
			}
			if (ids.isEmpty())
			{
				ids.add("NONE");
			}

		}
		else {
			//single message reload
			String messageid = null;
			Data chat = (Data) inReq.getPageValue("chat");
			if (chat != null)
			{
				messageid = chat.getId();
			}
			if (messageid == null)
			{
				messageid = inReq.getRequestParameter("messageid");
			}
			if (messageid != null)
			{
				ids.add(messageid);
			}
			
		}
	

		if (ids.size() < 1)
		{
			//log.error("No messages found");
			return null;
		}
		Map<String, Collection> messageidwithassets = new HashMap<String, Collection>();

		Collection assets = getMediaArchive(inReq).query("asset").orgroup("attachedtomessageid", ids).search();
		inReq.putPageValue("assethits", assets);
		for (Iterator iterator = assets.iterator(); iterator.hasNext();)
		{
			Data asset = (Data) iterator.next();
			Collection messageids = asset.getValues("attachedtomessageid");
			for (Iterator iterator2 = messageids.iterator(); iterator2.hasNext();)
			{
				String messageid = (String) iterator2.next();
				Collection assetsfound = messageidwithassets.get(messageid);
				if (assetsfound == null)
				{
					assetsfound = new ArrayList();
					messageidwithassets.put(messageid, assetsfound);
				}
				assetsfound.add(asset);
			}
		}

		inReq.putPageValue("messageidwithassets", messageidwithassets);
		return messageidwithassets;
	}

	public void attachAssetsToMessage(WebPageRequest inReq)
	{

		MediaArchive archive = getMediaArchive(inReq);
		
		Collection savedassets = (Collection) inReq.getPageValue("savedassets");
		
		String messageid = inReq.getRequestParameter("messageid");
		
		Data message = archive.getData("chatterbox", messageid);
		Boolean broadcast = false;
		if (message == null)
		{
			//Attachment only
			if (savedassets != null && !savedassets.isEmpty())
			{
				//Create message as the user
				String channel = inReq.getRequestParameter("channel");
				
				Date now = new Date();
				Searcher chatterboxsearcher = archive.getSearcher("chatterbox");
				message = chatterboxsearcher.createNewData();
				
				message.setValue("user", inReq.getUserName());
				message.setValue("date", now);
				message.setValue("channel", channel);
				String entityid = inReq.getRequestParameter("entityid");
				message.setValue("entityid", entityid);
				
				chatterboxsearcher.saveData(message);
	    		broadcast= true;
	    		//inReq.putPageValue("messageid", message.getId());
	    		inReq.putPageValue("chat", message);
			}
		}
		if (message == null)
		{
			return;
		}
		
		ArrayList tosave = new ArrayList();
		
		if(savedassets != null)
		{
			for (Iterator iterator = savedassets.iterator(); iterator.hasNext();)
			{
				Data hit = (Data) iterator.next();
				Asset asset = archive.getCachedAsset(hit.getId());
	
				asset.addValue("attachedtomessageid", message.getId());
				tosave.add(asset);
			}
		}

		//When is getting here?
		String[] assetids = inReq.getRequestParameters("assetid");
		if (assetids != null)
		{

			for (int i = 0; i < assetids.length; i++)
			{
				String assetid = assetids[i];
				Asset asset = archive.getCachedAsset(assetid);
				asset.addValue("attachedtomessageid", message.getId());
				tosave.add(asset);
			}

		}
		
		archive.saveAssets(tosave);
		
		archive.fireSharedMediaEvent("importing/assetscreated");  //Kicks off an async saving
		
		if (broadcast)
		{
			ChatServer server = (ChatServer) archive.getBean("chatServer");
			server.broadcastMessage(archive.getCatalogId(), message);
		}
	}
	
	
	public void removeAssetsFromMessage(WebPageRequest inReq)
	{

		MediaArchive archive = getMediaArchive(inReq);
		
		Collection savedassets = (Collection) inReq.getPageValue("savedassets");
		
		String messageid = inReq.getRequestParameter("messageid");
		String assetid = inReq.getRequestParameter("assetid");
		
		Data message = archive.getData("chatterbox", messageid);
		Boolean broadcast = false;

		if (message == null || assetid == null)
		{
			return;
		}
		
		Asset asset = archive.getCachedAsset(assetid);
		if (asset != null)
		{
			asset.removeValue("attachedtomessageid", messageid);
			archive.saveAsset(asset);
		}
		
		Searcher chatterboxsearcher = archive.getSearcher("chatterbox");
		
		Boolean messageotherassets = false;
		
		Collection otherassets = getMediaArchive(inReq).query("asset").exact("attachedtomessageid", messageid).search();
		for (Iterator iterator = otherassets.iterator(); iterator.hasNext();)
		{
			Data asset2 = (Data) iterator.next();
			Collection messageids = asset2.getValues("attachedtomessageid");
			for (Iterator iterator2 = messageids.iterator(); iterator2.hasNext();)
			{
				String assetmessageid = (String) iterator2.next();
				if (assetmessageid.equals(messageid))
				{
					messageotherassets = true;
				}
			}
		}
		
		if (!messageotherassets) 
		{
			//Safe to delete message
			ChatServer server = (ChatServer) archive.getBean("chatServer");
			server.broadcastRemovedMessage(archive.getCatalogId(), message);
		
			chatterboxsearcher.delete(message, null);
			inReq.putSessionValue("chat", null);
			
		}

	}
	
	

	public void loadChatChannel( WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String channel = inReq.findValue("channel");
		if (channel == null)
		{
			channel = inReq.getRequestParameter("channel");
		}
		Data currentchannel = archive.getCachedData("collectiveproject", channel);
		
		Searcher topicsearcher = archive.getSearcher("collectiveproject");
		
		Data librarycol  = (Data) inReq.getPageValue("librarycol");
		String module = inReq.findValue("module");
		
		if (librarycol != null && currentchannel == null) {
			currentchannel = topicsearcher.query().match("parentcollectionid",librarycol.getId()).sort("name").searchOne();
		}
		if (currentchannel == null) {
			currentchannel = topicsearcher.createNewData();
			currentchannel.setValue("moduleid", module);
			if(librarycol != null) 
			{				
				currentchannel.setValue("parentcollectionid", librarycol.getId() );
			}
			currentchannel.setName("General");
			topicsearcher.saveData(currentchannel);
		}
		
		inReq.putPageValue("currentchannel", currentchannel);
	}
	
	
	public void loadProjectLikes(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String collectionid = inReq.getRequestParameter("collectionid");
		
		HitTracker collectionlikes = archive.query("librarycollectionlikes").exact("collectionid", collectionid).exact("enabled", true).search(inReq);
		inReq.putPageValue("collikes", collectionlikes);
		
	}
	
	
	public void loadCollectiveIncome(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String collectionid = inReq.getRequestParameter("collectionid");
		
		String searchtype = "collectiveincome";
		HitTracker results = null;
		SearchQuery query = archive.getSearcher(searchtype).addStandardSearchTerms(inReq);
		if( query == null)
		{
			query = archive.getSearcher(searchtype).createSearchQuery();
			query.addExact("collectionid", collectionid);
			
			String sortby = inReq.getRequestParameter(searchtype+"sortby");
			if(sortby == null)
			{
				sortby = "dateDown";
			}
			query.addSortBy(sortby);
		}
		results = archive.getSearcher(searchtype).loadPageOfSearch(inReq);
		
		
		if( results == null)
		{
			results = archive.getSearcher(searchtype).cachedSearch(inReq, query);	
		}

		
		//Get Totals by Currency
		if (results != null)
		{
			inReq.putPageValue("income", results);
			HashMap<String, Double> totals = new HashMap<String,Double>();
			for (Iterator iterator = results.iterator(); iterator.hasNext();)
			{
				MultiValued row = (MultiValued) iterator.next();
				String currency = row.get("currencytype");
				Double total = row.getDouble("total");
				if (total != null)
				{
					totals.put(currency, totals.getOrDefault(currency, 0.0) + total);
				}
			}
			inReq.putPageValue("totalsbycurrency", totals);
		}
		
	}
	
	
	public void loadCollectiveExpenses(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String collectionid = inReq.getRequestParameter("collectionid");
		
		String searchtype = "collectiveexpense";
		
		SearchQuery query = archive.getSearcher(searchtype).addStandardSearchTerms(inReq);
		if (query == null)
		{
			query = archive.getSearcher(searchtype).createSearchQuery();
			query.addExact("collectionid", collectionid);
			
			String sortby = inReq.getRequestParameter(searchtype+"sortby");
			if(sortby == null)
			{
				sortby = "dateDown";
			}
			query.addSortBy(sortby);
		}
		
		HitTracker results = archive.getSearcher(searchtype).loadPageOfSearch(inReq);
		
		if( results == null)
		{
			results = archive.getSearcher(searchtype).cachedSearch(inReq, query);	
		}
		inReq.putPageValue("expenses", results);
		
		//Get Totals by Currency
		if (results != null)
		{
			HashMap<String, Double> totals = new HashMap<String,Double>();
			for (Iterator iterator = results.iterator(); iterator.hasNext();)
			{
				MultiValued row = (MultiValued) iterator.next();
				String currency = row.get("currencytype");
				Double total = row.getDouble("total");
				if (total != null)
				{
					totals.put(currency, totals.getOrDefault(currency, 0.0) + total);
				}
			}
			inReq.putPageValue("totalsbycurrency", totals);
		}
		
	}
	


}
