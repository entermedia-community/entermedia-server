package model.projects;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.Searcher
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
					manager.addAssetToLibrary(inReq, archive, libraryid, tracker);
					inReq.putPageValue("added" , String.valueOf( tracker.size() ) );
					return;
				}
			}
		}

		String assetid = inReq.getRequestParameter("assetid");
		manager.addAssetToLibrary(inReq, archive, libraryid, assetid);
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
					manager.removeAssetFromLibrary(inReq, archive, libraryid, tracker);
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
				manager.addAssetToCollection(inReq, archive, libraryid, tracker);
				inReq.putPageValue("added" , String.valueOf( tracker.size() ) );
				return;
			}
		}
		String assetid = inReq.getRequestParameter("assetid");
		
		manager.addAssetToCollection(inReq, archive, libraryid, assetid);
		inReq.putPageValue("added" , "1" );
	}
	public void removeAssetFromCollection(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		String collectionid = inReq.getRequestParameter("collectionid");
		
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
				manager.removeAssetFromCollection(inReq, archive, collectionid, tracker);
				inReq.putPageValue("count" , String.valueOf( tracker.size() ) );
				return;
			}
		}
	
	}
	
	public void searchForAssetsOnCollection(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String collectionid = inReq.getRequestParameter("id");
		if( collectionid == null)
		{
			collectionid = inReq.getRequestParameter("collectionid");
		}
		ProjectManager manager = (ProjectManager)getModuleManager().getBean(archive.getCatalogId(),"projectManager");
		
		Collection<String> ids = manager.loadAssetsInCollection(inReq, archive, collectionid );
		//Do an asset search with permissions, showing only the assets on this collection
		HitTracker all = archive.getAssetSearcher().query().match("id", "*").not("editstatus", "7").search();
		
		all.setSelections(ids);
		all.setShowOnlySelected(true);
		//log.info("Searching for assets " + all.size() + " ANND " + ids.size() );
		
		if( all.size() != ids.size() )
		{
			//Some assets got deleted, lets remove them from the collection
			Set extras = new HashSet(ids);
			for (Data hit in all)
			{
				extras.remove(hit.getId());
			}
			//log.info("remaining " + extras );
			Searcher collectionassetsearcher = archive.getSearcher("librarycollectionasset");
			for (String id in extras)
			{
				Data toremove = collectionassetsearcher.query().match("asset",id).match("librarycollection", collectionid).searchOne();
				if( toremove != null)
				{
					collectionassetsearcher.delete(toremove, null);
				}
			}
		}
		
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
		//all.setHitsPerPage(1000);
		all.getSearchQuery().setProperty("collectionid", collectionid);
		all.getSearchQuery().setHitsName("collectionassets");
		inReq.putPageValue("hits", all);
		inReq.putSessionValue(all.getSessionId(),all);
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
		String collectionid = inReq.getRequestParameter("id");
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
}
