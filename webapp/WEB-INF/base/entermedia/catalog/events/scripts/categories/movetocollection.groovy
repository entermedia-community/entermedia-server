package categories

import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.Searcher

public void init()
{
	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");
	
	Category parent = mediaarchive.getCategory("1464281146024");
	
	Searcher searcher = mediaarchive.getSearcher("librarycollection")
	for (Category cat in parent.getChildren())
	{
		Data data = searcher.createNewData();
		data.setId(cat.getId());
		data.setName(cat.getName());
		data.setProperty("rootcategory",cat.getId());
		data.setProperty("library","AVj0tDTenPVib_aqW1RL");
		searcher.saveData(data);
	}
}

init();
log.info("Moved over");
