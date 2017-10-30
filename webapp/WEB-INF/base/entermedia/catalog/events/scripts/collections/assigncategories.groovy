package collections;

import org.entermediadb.asset.MediaArchive
import org.entermediadb.projects.LibraryCollection
import org.entermediadb.projects.ProjectManager
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker


public void init(){

	MediaArchive archive = context.getPageValue("mediaarchive");
	Searcher collectionsearcher = archive.getSearcher("librarycollection");
	HitTracker collections = collectionsearcher.getAllHits();
	collections.enableBulkOperations();
	ProjectManager projects = archive.getProjectManager();
	int fixed = 0;
	collections.each
	{
		String catid = it.getValue("rootcategory");
			
		if( catid == null)
		{
			LibraryCollection collection = collectionsearcher.loadData(it);
			projects.getRootCategory(archive,collection);
			fixed++;
		}
	}
	log.info("Fixed collections " + fixed);
}


init();

