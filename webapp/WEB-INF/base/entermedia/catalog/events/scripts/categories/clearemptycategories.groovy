package categories

import org.openedit.entermedia.Category;
import org.openedit.entermedia.MediaArchive;
import org.openedit.Data;
import com.openedit.page.Page;


public void init()
{
	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");
	
	List categories = mediaarchive.getCategoryArchive().listAllCategories();

	for (Category cat in categories)
	{
		Data data = mediaarchive.getAssetSearcher().searchByField("category", cat.getId() );
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
