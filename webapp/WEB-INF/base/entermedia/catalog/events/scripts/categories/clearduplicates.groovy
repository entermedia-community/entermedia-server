package categories

import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.xmldb.CategorySearcher
import org.openedit.Data
import org.openedit.hittracker.HitTracker

public void init()
{
	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");
	CategorySearcher searcher = mediaarchive.getCategorySearcher();
	
	HitTracker categories = searcher.getAllHits();
	categories.enableBulkOperations();
	HashSet duplicates = new HashSet();
	for (Data cat in categories)
	{
	
		String categorypath = cat.categorypath;
		HitTracker hits = searcher.query().exact("categorypath", categorypath).search();
		if(hits.size() > 0){
			duplicates.add(categorypath);
			log.info(categorypath);
		}
			
	}
	
}

log.info("Clear duplicate categories");
init();
log.info("Found ${duplicates.size()} categorypaths with extra categories");
