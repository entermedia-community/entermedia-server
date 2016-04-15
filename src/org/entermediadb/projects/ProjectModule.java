package org.entermediadb.projects;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

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

public class ProjectModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(ProjectModule.class);
	
	
	public void loadCollections(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.findValue("catalogid");
		ProjectManager manager = (ProjectManager)getModuleManager().getBean(catalogid,"projectManager");
		manager.loadCollections(inReq);
	}

	public void loadOpenCollections(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.findValue("catalogid");
		ProjectManager manager = (ProjectManager)getModuleManager().getBean(catalogid,"projectManager");
		manager.loadOpenCollections(inReq);
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

	public void addAssetToCollection(WebPageRequest inReq)
	{
		//TODO: Support multiple selections
		MediaArchive archive = getMediaArchive(inReq);
		
		String catid = inReq.getRequestParameter("categoryid");
		if( catid != null)
		{
			return;
		}

		
		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		String libraryid = inReq.getRequestParameter("libraryid");
		String librarycollection = inReq.getRequestParameter("librarycollection");
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
				manager.addAssetToCollection(archive, libraryid,  librarycollection, tracker);
				inReq.putPageValue("added" , String.valueOf( tracker.size() ) );
				return;
			}
		}
		String assetid = inReq.getRequestParameter("assetid");
		
		manager.addAssetToCollection(archive, librarycollection, assetid);
		inReq.putPageValue("added" , "1" );
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
	public void searchForAssetsInLibrary(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);
		Data library = manager.getCurrentLibrary(inReq.getUserProfile());
		if( library != null)
		{
			HitTracker hits = manager.loadAssetsInLibrary(library,archive,inReq);
			inReq.putPageValue("hits", hits);
		}
	}
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
		inReq.putSessionValue(all.getSessionId(),all);
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
				boolean ok = profile.getCombinedLibraries().contains(libraryid);
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
		//Make sure I am in the list of users for the library
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);
		if( manager.addUserToLibrary(archive,saved,inReq.getUser()) )
		{
			//reload profile?
			UserProfile profile = inReq.getUserProfile();
			profile.getCombinedLibraries().add(saved.getId());
		}
	}
	public void createCollection(WebPageRequest inReq)
	{
		createUserLibrary(inReq);
		Data saved = (Data)inReq.getPageValue("data");
		saved.setValue("creationdate", new Date());
		saved.setValue("owner",inReq.getUserName() );
		getMediaArchive(inReq).getSearcher("librarycollection").saveData(saved,null);
		
	}
	public void createUserLibrary(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Data userlibrary = archive.getData("library", inReq.getUserName());
		if( userlibrary != null)
		{
			return;
		}
		
		userlibrary = archive.getSearcher("library").createNewData();
		userlibrary.setId(inReq.getUserName());
		userlibrary.setName(inReq.getUser().getScreenName());
		archive.getSearcher("library").saveData(userlibrary, null);
		inReq.setRequestParameter("profilepreference","last_selected_library" );
		inReq.setRequestParameter("profilepreference.value", userlibrary.getId() );
		//Make sure I am in the list of users for the library
		ProjectManager manager = getProjectManager(inReq);
		if( manager.addUserToLibrary(archive,userlibrary,inReq.getUser()) )
		{
			//reload profile?
			UserProfile profile = inReq.getUserProfile();
			profile.getCombinedLibraries().add(userlibrary.getId());
		}
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
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);
		String collectionid = loadCollectionId(inReq);
		String categoryid = inReq.getRequestParameter("categoryid");
		manager.addCategoryToCollection(archive, collectionid, categoryid);
	}
	
	public void removeCategoryFromCollection(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);
		String collectionid = loadCollectionId(inReq);
		String categoryid = inReq.getRequestParameter("categoryid");
		manager.removeCategoryFromCollection(archive, collectionid, categoryid);
	}
	
	public void loadCategoriesOnCollection(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);
		String collectionid = loadCollectionId(inReq);
		if( collectionid == null)
		{
			return;
		}		
		HitTracker categories = manager.loadCategoriesOnCollection(archive,collectionid);
		//log.info("Found: " + categories.size());
		inReq.putPageValue("collectioncategories", categories);
		//inReq.putSessionValue(all.getSessionId(),all);
	}
	public void moveCollection(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);
		String collectionid = loadCollectionId(inReq);
		String libraryid = inReq.getRequestParameter("targetlibraryid");
		manager.moveCollectionTo(inReq,archive,collectionid,libraryid);
		inReq.putPageValue("movestatus", "completed");
	}	
	
	public void loadFileSizes(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);
		String collectionid = loadCollectionId(inReq);
		Map sizes = manager.loadFileSizes(inReq, archive, collectionid);
		inReq.putPageValue("filesizes", sizes);
	}
	
	public void loadCategoriesOnCollections(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);
		Collection<UserCollection> collections = manager.loadOpenCollections(inReq);
		
		manager.loadCategoriesOnCollections(archive, collections);
		
	}
}
