package org.entermediadb.projects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.asset.upload.FileUpload;
import org.entermediadb.asset.upload.FileUploadItem;
import org.entermediadb.asset.upload.UploadRequest;
import org.entermediadb.desktops.Desktop;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.profile.UserProfile;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.PathUtilities;

public class ProjectModule extends BaseMediaModule {
	private static final Log log = LogFactory.getLog(ProjectModule.class);

	public void loadCollections(WebPageRequest inReq) throws Exception {
		String catalogid = inReq.findValue("catalogid");
		ProjectManager manager = (ProjectManager) getModuleManager().getBean(catalogid, "projectManager");
		manager.loadCollections(inReq, getMediaArchive(inReq));
	}

	public void redirectToCollection(WebPageRequest inReq) throws Exception {
		String catalogid = inReq.findValue("catalogid");
		ProjectManager manager = (ProjectManager) getModuleManager().getBean(catalogid, "projectManager");
		String collectionid = loadCollectionId(inReq);
		String appid = inReq.findValue("applicationid");
		String finalpath = "/" + appid + "/views/modules/librarycollection/media/" + collectionid + ".html";
		inReq.redirect(finalpath);

	}

	public void loadOpenCollections(WebPageRequest inReq) throws Exception {
		String catalogid = inReq.findValue("catalogid");
		ProjectManager manager = (ProjectManager) getModuleManager().getBean(catalogid, "projectManager");
		manager.loadOpenCollections(inReq, getMediaArchive(inReq), 10);
	}

	public void loadMostRecentCollection(WebPageRequest inReq) throws Exception {
		String catalogid = inReq.findValue("catalogid");
		ProjectManager manager = (ProjectManager) getModuleManager().getBean(catalogid, "projectManager");
		Collection<LibraryCollection> list = manager.loadOpenCollections(inReq, getMediaArchive(inReq), 1);
		if (!list.isEmpty()) {
			inReq.putPageValue("librarycol", list.iterator().next());
		}
	}

	public void loadSelectedLibrary(WebPageRequest inReq) throws Exception {
		String catalogid = inReq.findValue("catalogid");
		ProjectManager manager = (ProjectManager) getModuleManager().getBean(catalogid, "projectManager");
		Data selected = manager.getCurrentLibrary(inReq.getUserProfile());
		inReq.putPageValue("selectedlibrary", selected);

	}

	public void setCurrentLibrary(WebPageRequest inReq) {
		String catalogid = inReq.findValue("catalogid");
		String libraryid = inReq.findValue("selectedlibrary");
		if (libraryid != null) {
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
	 * = inReq.getRequestParameter("hitssessionid"); ProjectManager manager =
	 * getProjectManager(inReq);
	 * 
	 * if( hitssessionid != null ) { HitTracker tracker =
	 * (HitTracker)inReq.getSessionValue(hitssessionid); if( tracker != null ) {
	 * tracker = tracker.getSelectedHitracker(); if( tracker != null &&
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
	 * = inReq.getRequestParameter("hitssessionid"); ProjectManager manager =
	 * getProjectManager(inReq);
	 * 
	 * if( hitssessionid != null ) { HitTracker tracker =
	 * (HitTracker)inReq.getSessionValue(hitssessionid); if( tracker != null ) {
	 * tracker = tracker.getSelectedHitracker(); if( tracker != null &&
	 * tracker.size() > 0 ) { manager.removeAssetFromLibrary(archive, libraryid,
	 * tracker); inReq.putPageValue("count" , String.valueOf( tracker.size() )
	 * ); return; } } } }
	 */
	public void addAssetToCollection(WebPageRequest inReq) {
		// TODO: Support multiple selections
		MediaArchive archive = getMediaArchive(inReq);

		// String libraryid = inReq.getRequestParameter("libraryid");
		String librarycollection = inReq.getRequestParameter("collectionid");
		if (librarycollection == null) {
			Data data = (Data) inReq.getPageValue("data");
			if (data != null) {
				librarycollection = data.getId();
			}
		}
		if (librarycollection == null) {
			log.error("librarycollection not found");
			return;
		}
		ProjectManager manager = getProjectManager(inReq);
		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		if (hitssessionid != null) {
			HitTracker tracker = (HitTracker) inReq.getSessionValue(hitssessionid);
			if (tracker != null) {
				tracker = tracker.getSelectedHitracker();
			}
			if (tracker != null && tracker.size() > 0) {
				manager.addAssetToCollection(archive, librarycollection, tracker);
				inReq.putPageValue("added", String.valueOf(tracker.size()));
				return;
			}
		}
		String assetid = inReq.getRequestParameter("assetid");
		if (assetid != null) {
			manager.addAssetToCollection(archive, librarycollection, assetid);
			inReq.putPageValue("added", "1");
		}
	}

	public void addAssetsToCollection(WebPageRequest inReq) {
		String[] assetids = inReq.getRequestParameters("assetid");
		MediaArchive archive = getMediaArchive(inReq);
		String librarycollection = inReq.getRequestParameter("collectionid");

		if (assetids != null) {

			ProjectManager manager = getProjectManager(inReq);
			for (int i = 0; i < assetids.length; i++) {
				String assetid = assetids[i];
				manager.addAssetToCollection(archive, librarycollection, assetid);

			}

		}

		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		ProjectManager manager = getProjectManager(inReq);

		if (hitssessionid != null) {
			HitTracker tracker = (HitTracker) inReq.getSessionValue(hitssessionid);
			if (tracker != null) {
				tracker = tracker.getSelectedHitracker();
			}
			if (tracker != null && tracker.size() > 0) {
				manager.addAssetToCollection(archive, librarycollection, tracker);
				inReq.putPageValue("count", String.valueOf(tracker.size()));
				return;
			}
		}

	}

	public void removeAssetFromCollection(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		String collectionid = loadCollectionId(inReq);

		ProjectManager manager = getProjectManager(inReq);
		if (hitssessionid != null) {
			HitTracker tracker = (HitTracker) inReq.getSessionValue(hitssessionid);
			if (tracker != null) {
				tracker = tracker.getSelectedHitracker();
			}
			if (tracker != null && tracker.size() > 0) {
				manager.removeAssetFromCollection(archive, collectionid, tracker);
				inReq.putPageValue("count", String.valueOf(tracker.size()));
				return;
			}
		}
		String assetid = inReq.findValue("assetid");
		if(assetid != null){
	
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
	public void searchForPendingAssetsOnCollection(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		String collectionid = loadCollectionId(inReq);
		if (collectionid == null) {
			return;
		}
		ProjectManager manager = getProjectManager(inReq);
		HitTracker all = manager.loadAssetsInCollection(inReq, archive, collectionid, "1|rejected");
		if (all == null) {
			return;
		}

		Object caneditdata = inReq.getPageValue("caneditdata");
		all.getSearchQuery().setValue("caneditdata", caneditdata);
		all.selectAll();
		// String hitsname = inReq.findValue("hitsname");
		inReq.putPageValue("hits", all);
		String sessionId = all.getSessionId();
		inReq.putSessionValue(sessionId, all);
	}

	public void searchForAssetsOnCollection(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		String collectionid = loadCollectionId(inReq);
		if (collectionid == null) {
			return;
		}
		ProjectManager manager = getProjectManager(inReq);

		boolean canedit = manager.canEditCollection(inReq, collectionid);
		String editstatus = "6";
		if (canedit) {
			editstatus = null;
		}
		HitTracker all = manager.loadAssetsInCollection(inReq, archive, collectionid, editstatus);
		if (all == null) {
			return;
		}
		if (Boolean.parseBoolean(inReq.findValue("alwaysresetpage"))) {
			all.setPage(1);
		}
		Object caneditdata = inReq.getPageValue("caneditdata");
		all.getSearchQuery().setValue("caneditdata", caneditdata);

		// String hitsname = inReq.findValue("hitsname");
		inReq.putPageValue("hits", all);
		String sessionId = all.getSessionId();
		inReq.putSessionValue(sessionId, all);
	}

	public Data loadCollection(WebPageRequest inReq) {
		String collectionid = loadCollectionId(inReq);
		if (collectionid != null) {
			Data collection = getProjectManager(inReq).getLibraryCollection(getMediaArchive(inReq), collectionid);
			inReq.putPageValue("librarycol", collection);
			return collection;
		}
		return null;
	}

	protected String loadCollectionId(WebPageRequest inReq) {
		String collectionid = inReq.getRequestParameter("collectionid");
		if (collectionid == null) {
			collectionid = inReq.getRequestParameter("librarycollection");
			if (collectionid == null) {
				collectionid = inReq.getRequestParameter("id");
				if (collectionid == null) {
					Data coll = (Data) inReq.getPageValue("librarycol");
					if (coll != null) {
						collectionid = coll.getId();
					}
				}
			}
		}
		if (collectionid == null) {
			LibraryCollection coll = loadCollectionFromFolder(inReq);
			if (coll != null) {
				collectionid = coll.getId();
			} else {
				String page = inReq.getPage().getName();
				page = page.replace(".html", "").replace(".zip", "");
				collectionid = page;
			}
		}
		return collectionid;
	}

	public void savedLibrary(WebPageRequest inReq) {
		Data saved = (Data) inReq.getPageValue("data");
		if (saved != null) {
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
		Data saved = librarysearcher.createNewData();
		librarysearcher.updateData(inReq, inReq.getRequestParameters("field"), saved);
		saved.setValue("creationdate", new Date());
		saved.setValue("owner", inReq.getUserName());
		//saved.setValue("library", inReq.getRequestParameter("library.value"));  //optional
		Data userlibrary = manager.loadUserLibrary(mediaArchive, inReq.getUserProfile());
		saved.setValue("library", userlibrary.getId());
		librarysearcher.saveData(saved, null); //this fires event ProjectManager.configureCollection

		/*
		Category cat = manager.getRootCategory(mediaArchive, (LibraryCollection) saved); //This creates the library
		((MultiValued) cat).addValue("viewusers", inReq.getUserName());
		mediaArchive.getCategorySearcher().saveData(cat);
		 */
		inReq.setRequestParameter("librarycollection", saved.getId());
		inReq.setRequestParameter("collectionid", saved.getId());

	}
/*
	public Data createUserLibrary(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		UserProfile profile = inReq.getUserProfile();
		ProjectManager manager = getProjectManager(inReq);
		Data userlibrary = manager.loadUserLibrary(archive, profile);
		inReq.setRequestParameter("profilepreference", "last_selected_library");
		inReq.setRequestParameter("profilepreference.value", userlibrary.getId());
		return userlibrary;
	}
*/
	public void addOpenCollection(WebPageRequest inReq) {
		UserProfile profile = inReq.getUserProfile();
		Collection cols = profile.getValues("opencollections");

		Data collection = (Data) inReq.getPageValue("librarycol");
		if (collection == null) {
			collection = (Data) inReq.getPageValue("data");
		}
		if (collection != null) {
			String collectionid = collection.getId();
			if (cols == null || !cols.contains(collectionid)) 
			{
				profile.addValue("opencollections", collectionid);
				cols = profile.getValues("opencollections");
				if (cols.size() > 10) {
					List cut = new ArrayList(cols);
					cut = cut.subList(cols.size() - 10, cols.size());
					profile.setValues("opencollections", cut);
				}
			}
			profile.setProperty("selectedcollection", collectionid);
			profile.save(inReq.getUser());
		}
	}

	// Not used anymore
	public void addCollectionTab(WebPageRequest inReq) {
		UserProfile profile = inReq.getUserProfile();
		String collectionid = null;
		Data collection = (Data) inReq.getPageValue("librarycol");
		if (collection != null) {
			collectionid = collection.getId();
		} else {
			collectionid = inReq.getRequestParameter("collectionid");
		}
		if (collectionid == null) {
			String newcollection = inReq.getRequestParameter("collectionname.value");
			if (newcollection != null) {
				MediaArchive archive = getMediaArchive(inReq);
				Searcher searcher = archive.getSearcher("librarycollection");
				Data newcol = searcher.createNewData();
				String libraryid = inReq.getRequestParameter("library.value");
				if (libraryid == null) {
					Searcher lsearcher = archive.getSearcher("library");

					Data lib = (Data) searcher.searchById(inReq.getUserName());
					if (lib == null) {
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
		if (cols == null || !cols.contains(collectionid)) {
			profile.addValue("opencollections", collectionid);
			profile.save(inReq.getUser());
		}
		profile.setProperty("selectedcollection", collectionid);
	}

	public void closeCollectionTab(WebPageRequest inReq) {
		UserProfile profile = inReq.getUserProfile();
		String collectionid = inReq.getRequestParameter("collectionid");
		profile.removeValue("opencollections", collectionid);
		String selcol = profile.get("selectedcollection");
		if (collectionid.equals(selcol)) {
			profile.setProperty("selectedcollection", null);
		}
		profile.save(inReq.getUser());
	}

	public ProjectManager getProjectManager(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = (ProjectManager) getModuleManager().getBean(archive.getCatalogId(), "projectManager");
		inReq.putPageValue("projectmanager", manager);
		return manager;
	}

	public void addCategoryToCollection(WebPageRequest inReq) {
		String[] categoryid = inReq.getRequestParameters("categoryid");
		// if( categoryid == null)
		// {
		// String vals = inReq.getRequestParameter("category.values");
		// if( vals != null)
		// {
		// categoryid = vals.replace(" ","").trim().split(" +");
		// }
		// }
		if (categoryid != null) {
			MediaArchive archive = getMediaArchive(inReq);
			ProjectManager manager = getProjectManager(inReq);
			String collectionid = inReq.getRequestParameter("collectionid");
			Data collection = null;
			for (int i = 0; i < categoryid.length; i++) {
				collection = manager.addCategoryToCollection(inReq.getUser(), archive, collectionid, categoryid[i]);
			}
			inReq.putPageValue("librarycol", collection);
		}
	}

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

	public void copyCollection(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);
		String collectionid = loadCollectionId(inReq);
		LibraryCollection collection = (LibraryCollection) archive.getData("librarycollection", collectionid);

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

	public void createSnapshot(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);
		String collectionid = loadCollectionId(inReq);

		User user = inReq.getUser();

		String note = inReq.getRequestParameter("note.value");
		if (note == null) {
			note = "Snapshot Created";
		}
		manager.snapshotCollection(inReq, user, archive, collectionid, note);

	}

	public void restoreSnapshot(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);
		String collectionid = loadCollectionId(inReq);
		String revision = inReq.getRequestParameter("revision");

		User user = inReq.getUser();

		String note = inReq.getRequestParameter("note.value");
		if (note == null) {
			note = "Auto Snapshot on Restore from " + revision;
		}
		manager.restoreSnapshot(inReq, user, archive, collectionid, revision, note);

	}

	public void listConnectedDesktop(WebPageRequest inReq)
	{
		ProjectManager manager = getProjectManager(inReq);
		Desktop desktop = manager.getDesktopManager().getDesktop(inReq.getUser());
		inReq.putPageValue("desktop",desktop);
	}
	
	public void exportCollection(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);
		
		Desktop desktop = manager.getDesktopManager().getDesktop(inReq.getUserName());
		if( desktop.isBusy())
		{
			log.info("Desktop still busy");
			return;
		}
		String collectionid = loadCollectionId(inReq);
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

	
	
	
	public void importCollection(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);
		Desktop desktop = manager.getDesktopManager().getDesktop(inReq.getUserName());
		if( desktop.isBusy())
		{
			log.info("Desktop still busy");
			return;
		}

		String collectionid = loadCollectionId(inReq);
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

		manager.retrieveFilesFromClient(inReq, archive, collectionid);

		
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

	
	
	
	public LibraryCollection loadCollectionFromFolder(WebPageRequest inReq) {
		String colid = PathUtilities.extractDirectoryName(inReq.getPath());
		ProjectManager manager = getProjectManager(inReq);
		LibraryCollection col = manager.getLibraryCollection(getMediaArchive(inReq), colid);
		inReq.putPageValue("librarycol", col);
		return col;

	}

	public boolean checkViewCollection(WebPageRequest inReq) {
		ProjectManager manager = getProjectManager(inReq);
		String collectionid = loadCollectionId(inReq);
		return manager.canViewCollection(inReq, collectionid);

	}

	public Boolean canEditCollection(WebPageRequest inReq) {
		ProjectManager manager = getProjectManager(inReq);
		String collectionid = loadCollectionId(inReq);
		return manager.canEditCollection(inReq, collectionid);
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


	public void approveSelection(WebPageRequest inReq) {
		HitTracker hits = (HitTracker) inReq.getPageValue("hits");
		ProjectManager manager = getProjectManager(inReq);
		String collectionid = loadCollectionId(inReq);
		String comment = inReq.getRequestParameter("comment");
		int count = manager.approveSelection(inReq, hits, collectionid, inReq.getUser(), comment);
		inReq.putPageValue("approved", count);

	}

	public void rejectSelection(WebPageRequest inReq) {
		HitTracker hits = (HitTracker) inReq.getPageValue("hits");
		ProjectManager manager = getProjectManager(inReq);
		String collectionid = loadCollectionId(inReq);
		String comment = inReq.getRequestParameter("comment");
		int count = manager.rejectSelection(inReq, hits, collectionid, inReq.getUser(), comment);
		inReq.putPageValue("rejected", count);

	}

	public void createQuickGallery(WebPageRequest inReq) {

		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		HitTracker tracker = null;
		if (hitssessionid != null) {
			tracker = (HitTracker) inReq.getSessionValue(hitssessionid);

		}

		MediaArchive archive = getMediaArchive(inReq);
		Searcher assetsearcher = archive.getAssetSearcher();
		Searcher collections = archive.getSearcher("librarycollection");
		LibraryCollection collection = (LibraryCollection) collections.createNewData();
		String[] fields = inReq.getRequestParameters("field");
		collections.updateData(inReq, fields, collection);

		// collection.setValue("visibility", "hidden");

		Searcher categories = archive.getSearcher("category");

		String collectionroot = archive.getCatalogSettingValue("gallery_root");
		if (collectionroot == null) {
			collectionroot = "Collections";
		}

		Category newcat = archive
				.createCategoryPath(collectionroot + "/Galleries/" + inReq.getUserName() + "/" + collection.getName());

		// newcat.setValue("visibility", "hidden");
		newcat.setName(collection.getName());

		categories.saveData(newcat);

		collection.setValue("rootcategory", newcat.getId());
		collection.setValue("creationdate", new Date());
		collection.setValue("owner", inReq.getUserName());
		collection.setValue("visibility", "3");
		ArrayList assets = new ArrayList();

		for (Iterator iterator = tracker.getSelectedHitracker().iterator(); iterator.hasNext();) {
			Data hit = (Data) iterator.next();
			Asset asset = (Asset) assetsearcher.loadData(hit);
			asset.addCategory(newcat);
			assets.add(asset);
			if (assets.size() > 1000) {
				assetsearcher.saveAllData(assets, null);
				assets.clear();
			}

		}
		assetsearcher.saveAllData(assets, null);
		collections.saveData(collection);

		inReq.putPageValue("librarycol", collection);
	}

	public void loadUserUpload(WebPageRequest inReq) throws Exception {
		String page = inReq.getPage().getName();
		MediaArchive archive = getMediaArchive(inReq);
		Searcher userupload = archive.getSearcher("userupload");
		Data upload = userupload.query().exact("uploadcategory", PathUtilities.extractPageName(page)).searchOne();
		inReq.putPageValue("userupload", upload);
	}
	public void downloadRemoteFolder(WebPageRequest inReq)
	{
		Map params = inReq.getJsonRequest();
			
		String catalogid = (String)params.get("catalogid");
		MediaArchive archive = getMediaArchive(catalogid);
		String categoryid = (String)params.get("categoryid");
		String collectionid = (String)params.get("collectionid");
		String server = (String)params.get("server");
	
		LibraryCollection collection = archive.getProjectManager().getLibraryCollection(archive,collectionid);
		
		Category cat = archive.getCategory(categoryid);
		
		Map assets = archive.getProjectManager().listAssetMap(server, archive, collection, cat);
		inReq.putPageValue("assetmap", new JSONObject(assets));
		
	}
	public void uploadRemoteFolder(WebPageRequest inReq)
	{
		
		Map params = inReq.getJsonRequest();
		
		String catalogid = (String)params.get("catalogid");
		MediaArchive archive = getMediaArchive(catalogid);
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

	/*
	 *
Server ProjectManager.retrieveFilesFromClient
Server Desktop.importCollection
Client AppController.checkinFiles
Client EnterMediaModel.pushFolder
Server ProjectModule.syncRemoteFolder
Client EnterMediaModule.uploadFilesIntoCollection
Server ProjectModule.uploadFile

	 */
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
		String subfolder = (String)inReq.getRequestParameter("subfolder");
		String collectionid = (String)inReq.getRequestParameter("collectionid");
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
		if( subfolder != null)
		{
			sourcepath = sourcepath + subfolder + "/" + item.getFileItem().getName();
		}
		else
		{
			sourcepath = sourcepath + "/" + item.getFileItem().getName();
		}
		String savepath = "/WEB-INF/data/" + catalogid +"/originals/" + sourcepath;
		ContentItem contentitem = properties.saveFileAs(item, savepath, inReq.getUser());
		//set mod date assetmodificationdate
		Asset asset = null;
		if( assetid != null)
		{
			asset = archive.getAsset(assetid);
		}
		if( asset == null)
		{
			asset = (Asset)archive.getAssetSearcher().createNewData();
			asset.setSourcePath(sourcepath);
		}
		asset.setProperty("importstatus", "needsmetadata");
		asset.setValue("assetmodificationdate",contentitem.lastModified()); //This needs to be set or it will keep thinking it's changed
		asset.setProperty("editstatus", "1"); //pending

		//Category use ID?
		Category cat = null;
		if( subfolder == null )
		{
			cat = collection.getCategory();
		}
		else
		{
			cat = archive.createCategoryPath( collection.getCategory().getCategoryPath() + subfolder );
		}
		asset.addCategory(cat);
		archive.saveAsset(asset);
		//Fire event
		archive.fireSharedMediaEvent("importing/assetscreated");

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
		ProjectManager manager = getProjectManager(inReq);
		String collectionid = inReq.getRequestParameter("collectionid");
		LibraryCollection col = manager.getLibraryCollection(archive, collectionid);
		Desktop desktop = manager.getDesktopManager().getDesktop(inReq.getUserName());
		desktop.openRemoteFolder(archive, col);
		
	}
}
