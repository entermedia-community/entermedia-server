package org.entermediadb.projects;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;

public class ProjectModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(ProjectModule.class);
	
	public void loadCollections(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.findValue("catalogid");
		ProjectManager manager = (ProjectManager)getModuleManager().getBean(catalogid,"projectManager");
		manager.loadCollections(inReq, getMediaArchive(inReq));
	}

	public void loadOpenCollections(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.findValue("catalogid");
		ProjectManager manager = (ProjectManager)getModuleManager().getBean(catalogid,"projectManager");
		manager.loadOpenCollections(inReq,getMediaArchive(inReq));
	}
	
	
	public void loadSelectedLibrary(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.findValue("catalogid");
		ProjectManager manager = (ProjectManager)getModuleManager().getBean(catalogid,"projectManager");
		Data selected = manager.getCurrentLibrary(inReq.getUserProfile());
		inReq.putPageValue("selectedlibrary", selected);

	}

//	public void savedCollection(WebPageRequest inReq)
//	{
//		MediaArchive archive = getMediaArchive(inReq);
//		Data collection = (Data)inReq.getPageValue("data");
//		if( collection != null)
//		{
//			ProjectManager manager = getProjectManager(inReq);	
//			manager.savedCollection(archive,collection,inReq.getUser());
//		}
//	}
	
	/*
	public void addAssetToLibrary(WebPageRequest inReq)
	{
		//TODO: Support multiple selections
		MediaArchive archive = getMediaArchive(inReq);
		String libraryid = inReq.getRequestParameter("libraryid");
		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		ProjectManager manager = getProjectManager(inReq);

		if( hitssessionid != null )
		{
			HitTracker tracker = (HitTracker)inReq.getSessionValue(hitssessionid);
			if( tracker != null )
			{
				tracker = tracker.getSelectedHitracker();
				if( tracker != null && tracker.size() > 0 )
				{
					manager.addAssetToLibrary(archive, libraryid, tracker);
					inReq.putPageValue("added" , String.valueOf( tracker.size() ) );
					return;
				}
			}
		}

		String assetid = inReq.getRequestParameter("assetid");
		manager.addAssetToLibrary(archive, libraryid, assetid);
		inReq.putPageValue("added" , "1" );
		
	}
	public void removeFromLibrary(WebPageRequest inReq)
	{
		//TODO: Support multiple selections
		MediaArchive archive = getMediaArchive(inReq);
		String libraryid = inReq.getRequestParameter("libraryid");
		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		ProjectManager manager = getProjectManager(inReq);

		if( hitssessionid != null )
		{
			HitTracker tracker = (HitTracker)inReq.getSessionValue(hitssessionid);
			if( tracker != null )
			{
				tracker = tracker.getSelectedHitracker();
				if( tracker != null && tracker.size() > 0 )
				{
					manager.removeAssetFromLibrary(archive, libraryid, tracker);
					inReq.putPageValue("count" , String.valueOf( tracker.size() ) );
					return;
				}
			}
		}
	}
	*/
	public void addAssetToCollection(WebPageRequest inReq)
	{
		//TODO: Support multiple selections
		MediaArchive archive = getMediaArchive(inReq);
		
		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		//String libraryid = inReq.getRequestParameter("libraryid");
		String librarycollection = inReq.getRequestParameter("collectionid");
		if( librarycollection == null)
		{
			log.error("librarycollection not found");
			return;
		}
		ProjectManager manager = getProjectManager(inReq);
		if( hitssessionid != null )
		{
			HitTracker tracker = (HitTracker)inReq.getSessionValue(hitssessionid);
			if( tracker != null )
			{
				tracker = tracker.getSelectedHitracker();
			}
			if( tracker != null && tracker.size() > 0 )
			{
				manager.addAssetToCollection(archive, librarycollection, tracker);
				inReq.putPageValue("added" , String.valueOf( tracker.size() ) );
				return;
			}
		}
		String assetid = inReq.getRequestParameter("assetid");
		if( assetid != null)
		{
			manager.addAssetToCollection(archive, librarycollection, assetid);
			inReq.putPageValue("added" , "1" );
		}	
	}
	public void removeAssetFromCollection(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		String collectionid = loadCollectionId(inReq);
		
		ProjectManager manager = getProjectManager(inReq);
		if( hitssessionid != null )
		{
			HitTracker tracker = (HitTracker)inReq.getSessionValue(hitssessionid);
			if( tracker != null )
			{
				tracker = tracker.getSelectedHitracker();
			}
			if( tracker != null && tracker.size() > 0 )
			{
				manager.removeAssetFromCollection(archive, collectionid, tracker);
				inReq.putPageValue("count" , String.valueOf( tracker.size() ) );
				return;
			}
		}
	}
//	public void searchForAssetsInLibrary(WebPageRequest inReq)
//	{
//		MediaArchive archive = getMediaArchive(inReq);
//		ProjectManager manager = getProjectManager(inReq);
//		Data library = manager.getCurrentLibrary(inReq.getUserProfile());
//		if( library != null)
//		{
//			HitTracker hits = manager.loadAssetsInLibrary(library,archive,inReq);
//			inReq.putPageValue("hits", hits);
//		}
//	}
	public void searchForAssetsOnCollection(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String collectionid = loadCollectionId(inReq);
		if( collectionid == null)
		{
			return;
		}		
		ProjectManager manager = getProjectManager(inReq);
		
		
		
		
		
		HitTracker all = manager.loadAssetsInCollection(inReq, archive, collectionid);
		//String hitsname = inReq.findValue("hitsname");
		inReq.putPageValue("hits", all);
		String sessionId = all.getSessionId();
		inReq.putSessionValue(sessionId,all);
	}
	protected String loadCollectionId(WebPageRequest inReq)
	{
		String collectionid = inReq.getRequestParameter("collectionid");
		if( collectionid == null)
		{
			collectionid = inReq.getRequestParameter("librarycollection");
			if(collectionid == null)
			{
				collectionid = inReq.getRequestParameter("id");
				if( collectionid == null)
				{
					Data coll = (Data)inReq.getPageValue("librarycol");
					if( coll != null)
					{
						collectionid = coll.getId();
					}
				}	
			}
		}
		if(collectionid == null){
			String page = inReq.getPage().getName();
			page = page.replace(".html", "");
			collectionid = page;
		}
		return collectionid;
	}	

	public boolean checkLibraryPermission(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		
		User user = inReq.getUser();
			
		if(  user != null && user.isInGroup("administrators"))
		{
			//dont filter since its the admin
			return true;
		}
		String collectionid = loadCollectionId(inReq);
		Data data = archive.getData("librarycollection", collectionid);
		if( data != null)
		{
			String libraryid  = data.get("library");
			UserProfile profile = inReq.getUserProfile();
			if( profile != null)
			{
				boolean ok = profile.getViewCategories().contains(libraryid);
				return ok;
			}
		}
		return false;
	}
	
	public void savedLibrary(WebPageRequest inReq)
	{
		Data saved = (Data)inReq.getPageValue("data");
		if( saved != null)
		{
			inReq.setRequestParameter("profilepreference","last_selected_library" );
			inReq.setRequestParameter("profilepreference.value", saved.getId() );
		}
//		//Make sure I am in the list of users for the library
//		MediaArchive archive = getMediaArchive(inReq);
//		ProjectManager manager = getProjectManager(inReq);
//		if( manager.addUserToLibrary(archive,saved,inReq.getUser()) )
//		{
//			//reload profile?
//			UserProfile profile = inReq.getUserProfile();
//			profile.getViewCategories().add(saved.getId());
//		}
	}
	public void createCollection(WebPageRequest inReq)
	{
		createUserLibrary(inReq);
		Data saved = (Data)inReq.getPageValue("data");
		saved.setValue("creationdate", new Date());
		saved.setValue("owner",inReq.getUserName() );
		saved.setValue("library",inReq.getUserName() );
		
		getMediaArchive(inReq).getSearcher("librarycollection").saveData(saved,null);
		inReq.setRequestParameter("librarycollection", saved.getId());
		inReq.setRequestParameter("collectionid", saved.getId());
	}
	
	public Data createUserLibrary(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		UserProfile profile = inReq.getUserProfile();
		ProjectManager manager = getProjectManager(inReq);
		Data userlibrary = manager.loadUserLibrary(archive, profile);
		inReq.setRequestParameter("profilepreference","last_selected_library" );
		inReq.setRequestParameter("profilepreference.value", userlibrary.getId() );
		return userlibrary;
	}

	
	public void addOpenCollection(WebPageRequest inReq)
	{
		UserProfile profile = inReq.getUserProfile();
		Collection cols = profile.getValues("opencollections");
		
		Data collection = (Data)inReq.getPageValue("data");
		if( collection != null)
		{
			String collectionid = collection.getId();
			if( cols == null || !cols.contains(collectionid))
			{
				profile.addValue("opencollections", collectionid);
			}
			profile.setProperty("selectedcollection", collectionid);
		}
	}
	
	public void addCollectionTab(WebPageRequest inReq)
	{
		UserProfile profile = inReq.getUserProfile();
		String collectionid = null;
		Data collection = (Data)inReq.getPageValue("librarycol");
		if( collection != null)
		{
			collectionid = collection.getId();
		}
		else
		{
			collectionid = inReq.getRequestParameter("collectionid");
		}
		if( collectionid == null)
		{
			String newcollection = inReq.getRequestParameter("collectionname.value");
			if( newcollection != null)
			{
				MediaArchive archive = getMediaArchive(inReq);
				Searcher searcher = archive.getSearcher("librarycollection");
				Data newcol = searcher.createNewData();
				String libraryid = inReq.getRequestParameter("library.value");
				if(libraryid == null)
				{
					Searcher lsearcher = archive.getSearcher("library");
					
					Data lib = (Data)searcher.searchById(inReq.getUserName());
					if( lib == null)
					{
						lib = lsearcher.createNewData();
						lib.setId(inReq.getUserName());
						lib.setName(inReq.getUser().getShortDescription());
						lsearcher.saveData(lib, inReq.getUser());
					}
					libraryid = lib.getId();
					inReq.getUserProfile().setProperty("last_selected_library",libraryid);
				}
				newcol.setProperty("library", libraryid);
				newcol.setName(newcollection);
				//searcher.updateData(inReq, fields, data);
				
				searcher.saveData(newcol, inReq.getUser());
				inReq.setRequestParameter("collectionid",newcol.getId());
				collectionid = newcol.getId();
			}
		}

		Collection cols = profile.getValues("opencollections");
		if( cols == null || !cols.contains(collectionid))
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
		if( collectionid.equals(selcol))
		{
			profile.setProperty("selectedcollection", null);
		}
		profile.save(inReq.getUser());
	}
	
	public ProjectManager getProjectManager(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = (ProjectManager)getModuleManager().getBean(archive.getCatalogId(),"projectManager");
		inReq.putPageValue("projectmanager",manager);
		return manager;
	}
	
	public void addCategoryToCollection(WebPageRequest inReq)
	{
		String categoryid = inReq.getRequestParameter("categoryid");
		if( categoryid != null)
		{
			MediaArchive archive = getMediaArchive(inReq);
			ProjectManager manager = getProjectManager(inReq);
			String collectionid = inReq.getRequestParameter("collectionid");
			Data collection = manager.addCategoryToCollection(inReq.getUser(),archive, collectionid, categoryid);
			inReq.putPageValue("librarycol",collection);
		}	
	}
	
	
	
//	public void loadCategoriesOnCollection(WebPageRequest inReq)
//	{
//		MediaArchive archive = getMediaArchive(inReq);
//		ProjectManager manager = getProjectManager(inReq);
//		String collectionid = loadCollectionId(inReq);
//		if( collectionid == null)
//		{
//			return;
//		}		
//		HitTracker categories = manager.loadCategoriesOnCollection(archive,collectionid);
//		//log.info("Found: " + categories.size());
//		inReq.putPageValue("collectioncategories", categories);
//		//inReq.putSessionValue(all.getSessionId(),all);
//	}
	public void importCollection(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);
		String collectionid = loadCollectionId(inReq);
		LibraryCollection collection = (LibraryCollection)archive.getData("librarycollection", collectionid);

		User user = inReq.getUser();
		String outfolder = "/WEB-INF/data/" + archive.getCatalogId() + "/workingfolders/"+ user.getId() + "/" + collection.getName() +"/";
	
		
		
		
		List paths = archive.getPageManager().getChildrenPathsSorted(outfolder);
		if( paths.isEmpty() ) {
			log.info("No import folders found ");
			return;
		}
		Collections.reverse(paths);
		String latest = (String)paths.iterator().next();
		latest = latest + "/";
		//Need to check if this is unique - increment a counter?
		String note = inReq.getRequestParameter("note.value");
		if(note == null){
			note = "Auto Created Revision on Import";
		}
		manager.importCollection(inReq, inReq.getUser(), archive, collectionid, latest, note);
		inReq.putPageValue("importstatus", "completed");
		
		
	}	
	
	
	public void createSnapshot(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);
		String collectionid = loadCollectionId(inReq);

		User user = inReq.getUser();

		String note = inReq.getRequestParameter("note.value");
		if(note == null){
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
		if(note == null){
			note = "Snapshot Restored from " + revision;
		}
		manager.restoreSnapshot(inReq, user, archive, collectionid, revision,note);
		
		
		

	
		
		
	}	
	
	
	
	
	
	
	
		
	public void exportCollection(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);
		String collectionid = loadCollectionId(inReq);
		User user = inReq.getUser();
		Data collection = archive.getData("librarycollection", collectionid);

		
		//The trailing slash is needed for the recursive algorithm.  Don't delete.
		String infolder = "/WEB-INF/data/" + archive.getCatalogId() + "/workingfolders/"+ user.getId()+"/" + collection.getName();  
		
		Date now = new Date();
		
		String stamp = DateStorageUtil.getStorageUtil().formatDateObj(now, "yyyy-MM-dd-HH-mm-ss");
		
		infolder = infolder + "/" + stamp + "/";
		
		manager.exportCollection(archive, collectionid, infolder);
		
		inReq.putPageValue("exportpath", infolder );
		

//		if(getWebEventListener() != null)
//		{
//			WebEvent event = new WebEvent();
//			event.setSearchType(searcher.getSearchType());
//			event.setCatalogId(searcher.getCatalogId());
//			event.setOperation(searcher.getSearchType() + "/saved");
//			event.setProperty("dataid", data.getId());
//			event.setProperty("id", data.getId());
//
//			event.setProperty("applicationid", inReq.findValue("applicationid"));
//
//			getWebEventListener().eventFired(event);
//		}
		
		
		
		
//		Searcher librarycollectiondownloads = archive.getSearcher("librarycollectiondownloads");
//
//		Data history  = librarycollectiondownloads.createNewData();
//	
//		history.setValue("owner", inReq.getUserName());
//		history.setValue("librarycollection", collectionid);
//		history.setValue("date", new Date());
//		history.setValue("revision", collection.get("revisions"));
//		
//		String fields[] = inReq.getRequestParameters("field");
//		librarycollectiondownloads.updateData(inReq, fields, history);
//		librarycollectiondownloads.saveData(history);

	}	
	
	
//	
//	public void loadCategoriesOnCollections(WebPageRequest inReq)
//	{
//		MediaArchive archive = getMediaArchive(inReq);
//		ProjectManager manager = getProjectManager(inReq);
//		Collection<LibraryCollection> collections = manager.loadOpenCollections(inReq,archive);
//		
//		manager.loadCategoriesOnCollections(inReq, archive, collections);
//		
//	}
}
