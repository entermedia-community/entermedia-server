package assets

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.search.AssetSearcher
import org.openedit.Data
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery

public void checkforTasks()
{
	mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");//Search for all files looking for videos
	
	AssetSearcher searcher = mediaarchive.getAssetSearcher();
	//recordmodificationdate
	//editstatus:7
	
	log.info("checking for deleted assets older than 30 days");
	Calendar now = Calendar.getInstance();
	now.add(Calendar.MONTH, -1);
	SearchQuery query = searcher.createSearchQuery();
	
	query.addMatches("editstatus", "7");
	query.addBefore("recordmodificationdate", now.getTime())
	
	HitTracker newitems = searcher.search(query);
	newitems.enableBulkOperations();

	log.info("Searching for ${query} found ${newitems.size()}");
	
	for (Data hit in newitems)
	{	
		Asset realitem = searcher.searchById(hit.getId());
		
		
		if (realitem != null)
		{
			mediaarchive.removeGeneratedImages(realitem);
			//mediaarchive.removeOriginals(realitem);
			searcher.delete(realitem, context.getUser());
		}
		else
		{
			log.info("Can't find task object with id '${hit.getId()}'. Index out of date?")
		}
	}
}
checkforTasks();