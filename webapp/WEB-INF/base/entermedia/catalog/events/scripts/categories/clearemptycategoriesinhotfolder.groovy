package categories

import org.entermediadb.asset.Category
import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery

public void init()
{
	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");
	
	HitTracker categories = mediaarchive.getCategorySearcher().getAllHits();
	categories.enableBulkOperations();
	for (Data data in categories)
	{
		SearchQuery q = mediaarchive.getAssetSearcher().createSearchQuery();
		q.addExact("category", data.getId() );
		q.addNot("editstatus","7");
		Data oneasset = mediaarchive.getAssetSearcher().searchByQuery(q);
		if( oneasset == null )
		{
			Category cat = mediaarchive.getCategorySearcher().loadData(data);
			mediaarchive.getCategorySearcher().deleteCategoryTree(cat);
			log.info("removed ${cat}" );
		}
	}
}

log.info("Clear empty categories");
init();
log.info("Clear empty categories complete");
