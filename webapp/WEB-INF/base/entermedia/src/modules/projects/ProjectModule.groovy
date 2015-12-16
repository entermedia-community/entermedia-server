package modules.projects;

import model.projects.ProjectManager

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.modules.BaseMediaModule
import org.openedit.profile.UserProfile

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.users.*

public class ProjectModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(ProjectModule.class);
	
	
	public void loadCollections(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.findValue("catalogid");
		ProjectManager manager = (ProjectManager)getModuleManager().getBean(catalogid,"projectManager");
		manager.loadCollections(inReq);
	}
	
	public void savedCollection(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Data collection = (Data)inReq.getPageValue("data");
		if( collection != null)
		{
			ProjectManager manager = (ProjectManager)getModuleManager().getBean(archive.getCatalogId(),"projectManager");	
			manager.savedCollection(archive,collection,inReq.getUser());
		}
	}
	
	public void addAssetToLibrary(WebPageRequest inReq)
	{
		//TODO: Support multiple selections
		MediaArchive archive = getMediaArchive(inReq);
		String libraryid = inReq.getRequestParameter("libraryid");
		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		ProjectManager manager = (ProjectManager)getModuleManager().getBean(archive.getCatalogId(),"projectManager");

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
		ProjectManager manager = (ProjectManager)getModuleManager().getBean(archive.getCatalogId(),"projectManager");

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
		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		String libraryid = inReq.getRequestParameter("libraryid");
		String librarycollection = inReq.getRequestParameter("librarycollection");
		ProjectManager manager = (ProjectManager)getModuleManager().getBean(archive.getCatalogId(),"projectManager");
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
		
		manager.addAssetToCollection(archive, libraryid, assetid);
		inReq.putPageValue("added" , "1" );
	}
	public void removeAssetFromCollection(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		String collectionid = loadCollectionId(inReq);
		
		ProjectManager manager = (ProjectManager)getModuleManager().getBean(archive.getCatalogId(),"projectManager");
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
		ProjectManager manager = (ProjectManager)getModuleManager().getBean(archive.getCatalogId(),"projectManager");
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
		ProjectManager manager = (ProjectManager)getModuleManager().getBean(archive.getCatalogId(),"projectManager");
		
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
		ProjectManager manager = (ProjectManager)getModuleManager().getBean(archive.getCatalogId(),"projectManager");
		if( manager.addUserToLibrary(archive,saved,inReq.getUser()) )
		{
			//reload profile?
			UserProfile profile = inReq.getUserProfile();
			profile.getCombinedLibraries().add(saved.getId());
		}
	}
}
