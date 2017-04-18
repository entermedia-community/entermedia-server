package model.assets;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.projects.ProjectManager;
import org.entermediadb.scripts.EnterMediaObject;
import org.openedit.Data;
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
	
	public void assignLibraries(MediaArchive mediaarchive, Collection assets)
	{
		Searcher searcher = mediaarchive.getAssetSearcher();
		Searcher librarySearcher = mediaarchive.getSearcher("library");
		Searcher librarycollectionSearcher = mediaarchive.getSearcher("librarycollection");
		
		ProjectManager proj = mediaarchive.getProjectManager();
		Map modifiedcollections = new HashMap();
		
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
								//Fire event, existing
								modifiedcollections.put(collection.getId(), collection);
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
							modifiedcollections.put(collection.getId(), collection);

						}	
					}
				}
			}
		}
		for (Iterator iterator = modifiedcollections.keySet().iterator(); iterator.hasNext();)
		{
			String id = (String) iterator.next();
			Data collection  = (Data)modifiedcollections.get(id);
			Object dirty = collection.getValue("lastassetaddeddatedirty");
			if( dirty == null || Boolean.valueOf( dirty.toString() ) == false)
			{
				collection.setValue("lastmodifieddate", new Date());
				collection.setValue("lastmodifieddatedirty",true);
			}	
			librarycollectionSearcher.saveData(collection);  //TODO: Lock this collection
			
		}
		mediaarchive.fireSharedMediaEvent("librarycollection/notifyassetscountchange");
	}
}
