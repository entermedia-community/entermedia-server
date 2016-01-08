package categories

import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.hittracker.SearchQuery

public void init()
{
	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");
	
	List categories = mediaarchive.getCategoryArchive().listAllCategories();

	for (Category cat in categories)
	{
		SearchQuery q = mediaarchive.getAssetSearcher().createSearchQuery();
		q.addExact("category", cat.getId() );
		q.addNot("editstatus","7");
		Data data = mediaarchive.getAssetSearcher().searchByQuery(q);
		if( data == null )
		{
			mediaarchive.getCategoryArchive().deleteCategory(cat);
			log.info("removed ${cat}" );
		}
	}
}

log.info("Clear empty categories");
init();
log.info("Clear empty categories complete");
