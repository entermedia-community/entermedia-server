package model.assets;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.projects.ProjectManager;
import org.entermediadb.scripts.EnterMediaObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.BaseData;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;

public class LibraryManager extends EnterMediaObject
{
	//If a user picks a library and collection then we just need to add the category
	
	//If we are scanning a hot folder then we add categories anyways. So this is not needed
	
	
	protected Map fieldLibraryFolders = null;
	protected Map fieldLibraries = null;
	protected Object NULL = new BaseData();
	
	public void assignLibraries(MediaArchive mediaarchive, Collection assets)
	{
		Searcher searcher = mediaarchive.getAssetSearcher();
		Searcher librarySearcher = mediaarchive.getSearcher("library");
		Searcher librarycollectionSearcher = mediaarchive.getSearcher("librarycollection");
		
		//List tosave = new ArrayList();
		int savedsofar = 0;
		
		ProjectManager proj = mediaarchive.getProjectManager();
		
		for (Iterator iterator = assets.iterator(); iterator.hasNext();) {
			MultiValued hit = (MultiValued) iterator.next();
			
			String sourcepath = hit.getSourcePath();
			//log.info("try ${sourcepath}" );
			String[] split = sourcepath.split("/");
			String sofar = "";
			Asset loaded = null;
			for( int i=0;i<split.length - 1;i++)
			{
				if( i > 10 )
				{
					break;
				}
				sofar = sofar + split[i];
				String libraryid = getLibraryIdForFolder(librarySearcher,sofar);
				sofar = sofar + "/";
		
				if( libraryid != null )
				{
					Collection existing = hit.getValues("libraries");
					if( existing == null || !existing.contains(libraryid))
					{
//						if( loaded == null)
//						{
//							loaded = (Asset) searcher.loadData(hit);
//							tosave.add(loaded);
//							savedsofar++;
//						}
						//loaded.addLibrary(libraryid);
						Data li = (Data) fieldLibraries.get(libraryid);
						loaded.setProperty("project",li.get("project") );
						//Now check for collections that have the right name
						HitTracker hits = librarycollectionSearcher.query().match("library", libraryid).search();
						for (Iterator iterator2 = hits.iterator(); iterator2.hasNext();)
						{
							Data coll = (Data) iterator2.next();
							String toppath = li.get("folder") + "/" + coll.getName();
							if( loaded.getSourcePath().startsWith(toppath) )
							{
								proj.addAssetToCollection(mediaarchive, coll.getId(), loaded.getId());
								savedsofar++;
							}
							
						}
						//log.info("found ${sofar}" );
					}
				}
			}
//			if(tosave.size() == 100)
//			{
//				searcher.saveAllData(tosave, null);
//				savedsofar = tosave.size() + savedsofar;
//				log.info("assets added to library: " + savedsofar );
//				tosave.clear();
//			}
		}
		//searcher.saveAllData(tosave, null);
		//savedsofar = tosave.size() + savedsofar;
		log.debug("completedlibraryadd added: " + savedsofar );
		if( fieldLibraryFolders != null)
		{
			fieldLibraryFolders.clear();
		}
		
	}
	protected String getLibraryIdForFolder(Searcher librarySearcher, String inFolder)
	{
		if( fieldLibraryFolders == null)
		{
			//load up all the folder we have
			Collection alllibraries = librarySearcher.query().match("folder", "*").search();
			fieldLibraryFolders = new HashMap(alllibraries.size());
			fieldLibraries = new HashMap(alllibraries.size());
			for (Iterator iterator = alllibraries.iterator(); iterator
					.hasNext();) {
				Data hit = (Data) iterator.next();
				String folder = hit.get("folder");
				if( folder != null)
				{
					fieldLibraryFolders.put( folder, hit.getId());
					fieldLibraries.put(hit.getId(),hit);
				}
			}
		}
		String libraryid = (String) fieldLibraryFolders.get(inFolder);
		return libraryid;

	}
}
