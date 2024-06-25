package searchcategory;

import org.entermediadb.asset.MediaArchive
import org.entermediadb.find.EntityManager
import org.entermediadb.projects.LibraryCollection
import org.entermediadb.projects.ProjectManager
import org.openedit.Data
import org.openedit.MultiValued
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker


public void init()
{

	MediaArchive archive = context.getPageValue("mediaarchive");
	Searcher collectionsearcher = archive.getSearcher("librarycollection");
	Searcher categorysearcher = archive.getSearcher("searchcategory");
	Searcher mediacapturesearcher = archive.getSearcher("mediacapture");
	
	
	//Load all categories
	HashMap allcategories = new HashMap();
	for( cat in categorysearcher.getAllHits() )
	{
		allcategories.put(cat.getName(), cat);
		
	}
	
	HitTracker hits = mediacapturesearcher.query().exact("collection_name", "*").search();
	hits.enableBulkOperations();
	
	
	//Blow away existing Source Folders first from other import script
	
	EntityManager manager = archive.getEntityManager();
	
	int fixed = 0;
	Collection tosave = new ArrayList();
	for(folder in hits)
	{
		String label = folder.get("collection_name");
		MultiValued newcopy  = manager.copyEntity(null,label, "librarycollection",folder); //This will add a field called mediacapture to each newcopy
		fixed++;
		
		String curatedlabel = folder.get("curated");
		if( curatedlabel != null)
		{
			Data found = allcategories.get(curatedlabel);
			if( found == null)
			{
				found = categorysearcher.createNewData();
				found.setName(curatedlabel);
				categorysearcher.saveData(found);
				allcategories.put(found.getName(), found);
			}
			newcopy.addValue("searchcategory", found.getId());
		}
		
		tosave.add(newcopy);
	}
	collectionsearcher.saveAllData(tosave, null);
	
	//TODO: Do a new search and reverse link all the folders
	
	log.info("added collections " + fixed);
}


init();

