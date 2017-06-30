package categories

import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.Searcher

public void init()
{
	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");
	
	Category parent = mediaarchive.createCategoryPath("Libraries");
	Category destination = mediaarchive.createCategoryPath("Collections");
	for (Category cat in parent.getChildren())
	{
		Category existing = mediaarchive.createCategoryPath("Collections/" + cat.getName());
		if( existing == null)
		{
			destination.addChild(cat);
		}
		else
		{
			for (Category subcat in cat.getChildren())
			{
				existing.addChild(subcat);
			}
		}	
	}
	//TODO: Delete Libraries
}

init();
log.info("Moved over");
