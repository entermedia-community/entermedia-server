package model.assets;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.scripts.EnterMediaObject;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;

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
		String autocreate = mediaarchive.getCatalogSettingValue("auto-create-collections");
		if( Boolean.valueOf( autocreate ) == false )
		{
			return;
		}
		Searcher searcher = mediaarchive.getAssetSearcher();
		Searcher librarySearcher = mediaarchive.getSearcher("library");
		Searcher librarycollectionSearcher = mediaarchive.getSearcher("librarycollection");
		Searcher librarycollectioncreationSearcher = mediaarchive.getSearcher("librarycollectioncreation");
		Map modifiedcollections = new HashMap();
		
		for (Iterator iterator = assets.iterator(); iterator.hasNext();) 
		{
			Asset asset = (Asset)searcher.loadData((Data)iterator.next());
			for (Iterator iterator2 = asset.getCategories().iterator(); iterator2.hasNext();)
			{
				Category child = (Category) iterator2.next();
				//Make sure it's not part of a library
				Data collection = librarycollectionSearcher.query().orgroup("rootcategory", child.getParentCategories()).searchOne();
				if( collection == null)
				{
					HitTracker creates = librarycollectioncreationSearcher.query().orgroup("category", child.getParentCategories()).search();
					for( Object hit : creates)
					{
						Data create = (Data)hit;
						String libraryid = create.get("library");
						String categoryid = create.get("category");
						
						//Search to make sure it already exists at any level
						//Find the first category this asset is in under the 
						Category collcat = null; 
						for (Iterator iterator3 = child.getParentCategories().iterator(); iterator3.hasNext();)
						{
							Category parent = (Category) iterator3.next();
							if( parent.getId().equals(categoryid) && iterator3.hasNext())
							{
								collcat = (Category) iterator3.next();
								break;
							}
						}
						if( collcat == null)
						{
							log.error("No folder found to create collection " + child.getCategoryPath() );
						}
						else
						{
							collection = (Data)librarycollectionSearcher.createNewData();
							collection.setName(collcat.getName());
							collection.setProperty("library", libraryid);
							collection.setProperty("rootcategory", collcat.getId());
							librarycollectionSearcher.saveData(collection);  //TODO: Lock root index category so that duplicate collections are not made
						}
					}
				}
				
				if( collection != null)
				{
					modifiedcollections.put(collection.getId(), collection);
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
		if( !modifiedcollections.isEmpty() )
		{
			mediaarchive.fireSharedMediaEvent("librarycollection/notifyassetscountchange");
		}
	}
}
