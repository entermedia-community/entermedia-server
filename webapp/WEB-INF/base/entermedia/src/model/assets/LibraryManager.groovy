package model.assets

import org.openedit.Data
import org.openedit.MultiValued
import org.openedit.data.BaseData
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive

import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery

public class LibraryManager extends EnterMediaObject
{
	protected Map fieldLibraryFolders = null;
	protected Object NULL = new BaseData();
	
	public void assignLibraries(MediaArchive mediaarchive, HitTracker assets)
	{
		
		Searcher searcher = mediaarchive.getAssetSearcher();
		Searcher librarySearcher = mediaarchive.getSearcher("library")
		
		List tosave = new ArrayList();
		int savedsofar = 0;
		for (MultiValued hit in assets)
		{
			def sourcepath = hit.getSourcePath();
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
				sofar = "${sofar}${split[i]}";
				String libraryid = getLibraryIdForFolder(librarySearcher,sofar);
				sofar = "${sofar}/";
		
				if( libraryid != null )
				{
					Collection existing = hit.getValues("libraries");
					if( existing == null || !existing.contains(libraryid))
					{
						if( loaded == null)
						{
							loaded = mediaarchive.getAssetBySourcePath(sourcepath);
							tosave.add(loaded);
							savedsofar++;
						}
						loaded.addLibrary(libraryid);
						//log.info("found ${sofar}" );
					}
				}
			}
			if(tosave.size() == 100)
			{
				searcher.saveAllData(tosave, null);
				savedsofar = tosave.size() + savedsofar;
				log.info("assets added to library: ${savedsofar} " );
				tosave.clear();
			}
		}
		searcher.saveAllData(tosave, null);
		savedsofar = tosave.size() + savedsofar;
		log.info("completedlibraryadd added : ${savedsofar} " );
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
			for (Data hit in alllibraries)
			{
				String folder = hit.get("folder");
				if( folder != null)
				{
					fieldLibraryFolders.put( folder, hit.getId());
				}
			}
		}
		String libraryid = fieldLibraryFolders.get(inFolder);
		return libraryid;

	}
}
