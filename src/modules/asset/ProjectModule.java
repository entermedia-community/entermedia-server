package modules.asset;

import java.util.Iterator;

import model.projects.ProjectManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.modules.BaseMediaModule;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;

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
			
	}
	
		public void addAssetToLibrary(WebPageRequest inReq)
		{
			//TODO: Support multiple selections
			MediaArchive archive = getMediaArchive(inReq);
			String libraryid = inReq.getRequestParameter("libraryid");
			String hitssessionid = inReq.getRequestParameter("hitssessionid");
			if( hitssessionid != null )
			{
				HitTracker tracker = (HitTracker)inReq.getSessionValue(hitssessionid);
				if( tracker != null )
				{
					tracker = tracker.getSelectedHitracker();
				}
				if( tracker != null && tracker.size() > 0 )
				{
					for (Iterator iterator = tracker.iterator(); iterator.hasNext();)
					{
						Data data = (Data) iterator.next();
						addAssetToLibrary(inReq, archive, libraryid, data.getId());
					}
					inReq.putPageValue("added" , String.valueOf( tracker.size() ) );
					return;
				}
			}

			String assetid = inReq.getRequestParameter("assetid");
			addAssetToLibrary(inReq, archive, libraryid, assetid);
			inReq.putPageValue("added" , "1" );
			
		}

		protected void addAssetToLibrary(WebPageRequest inReq, MediaArchive archive, String libraryid, String assetid)
		{
			Asset asset = archive.getAsset(assetid);
			
			if(asset != null && !asset.getLibraries().contains(libraryid))
			{
				asset.addLibrary(libraryid);
				archive.saveAsset(asset, inReq.getUser());
			}
		}
		
		public void addAssetToCollection(WebPageRequest inReq)
		{
			//TODO: Support multiple selections
			MediaArchive archive = getMediaArchive(inReq);
			String hitssessionid = inReq.getRequestParameter("hitssessionid");
			String libraryid = inReq.getRequestParameter("libraryid");
			if( hitssessionid != null )
			{
				HitTracker tracker = (HitTracker)inReq.getSessionValue(hitssessionid);
				if( tracker != null )
				{
					tracker = tracker.getSelectedHitracker();
				}
				if( tracker != null && tracker.size() > 0 )
				{
					for (Iterator iterator = tracker.iterator(); iterator.hasNext();)
					{
						Data data = (Data) iterator.next();
						addAssetToCollection(inReq, archive, libraryid, data.getId());
					}
					inReq.putPageValue("added" , String.valueOf( tracker.size() ) );
					return;
				}
			}
			String assetid = inReq.getRequestParameter("assetid");
			
			addAssetToCollection(inReq, archive, libraryid, assetid);
			inReq.putPageValue("added" , "1" );
		
		}

		protected void addAssetToCollection(WebPageRequest inReq, MediaArchive archive, String libraryid, String assetid)
		{
			addAssetToLibrary(inReq, archive, libraryid, assetid);

			
			String librarycollection = inReq.getRequestParameter("librarycollection");
			Searcher librarycollectionassetSearcher = archive.getSearcher("librarycollectionasset");
			
			Data found = librarycollectionassetSearcher.query().match("librarycollection", librarycollection).match("asset", assetid).searchOne();
			
			if(found == null)
			{
				found = librarycollectionassetSearcher.createNewData();
				found.setSourcePath(libraryid + "/" + librarycollection);
				found.setProperty("librarycollection", librarycollection);
				found.setProperty("asset", assetid);
				librarycollectionassetSearcher.saveData(found, inReq.getUser());
				log.info("Saved " + found.getId());
			}
		}
	
}

