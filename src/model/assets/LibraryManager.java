package model.assets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.projects.ProjectManager;
import org.entermediadb.scripts.EnterMediaObject;
import org.openedit.Data;
import org.openedit.data.BaseData;
import org.openedit.data.Searcher;

/**
 * This class is not needed anymore
 * @author shanti
 *
 */
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
		
		List tosave = new ArrayList();
		//int savedsofar = 0;
		
		ProjectManager proj = mediaarchive.getProjectManager();
		
		for (Iterator iterator = assets.iterator(); iterator.hasNext();) 
		{
			Asset asset = (Asset)searcher.loadData((Data)iterator.next());
			for (Iterator iterator2 = asset.getCategories().iterator(); iterator2.hasNext();)
			{
				Category child = (Category) iterator2.next();
				//Make sure it's not part of a library
				Data library = (Data)librarySearcher.query().orgroup("categoryid", child.getParentCategories()).searchOne();
				if( library != null)
				{
					Data collection = (Data)librarycollectionSearcher.query().exact("library",library.getId()).orgroup("rootcategory", child.getParentCategories()).searchOne();
					if( collection == null)
					{
						String librarycatid = library.get("categoryid");
						Category collectioncat = null;
						for (Iterator iterator3 = child.getParentCategories().iterator(); iterator3.hasNext();)
						{
							Category parent = (Category) iterator3.next();
							if( parent.getId().equals(librarycatid) && iterator3.hasNext())
							{
								collectioncat = (Category)iterator3.next();
								break;
							}
							
						}
						if( collectioncat != null)
						{
							collection = (Data)librarycollectionSearcher.createNewData();
							collection.setName(collectioncat.getName());
							collection.setProperty("library", library.getId());
							collection.setProperty("rootcategory", collectioncat.getId());
							librarycollectionSearcher.saveData(collection);
						}	
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
//		searcher.saveAllData(tosave, null);
//		savedsofar = tosave.size() + savedsofar;
//		log.debug("completedlibraryadd added: " + savedsofar );
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
