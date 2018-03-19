package conversions;

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery

public void init()
{
		MediaArchive mediaarchive = context.getPageValue("mediaarchive");//Search for all files looking for videos
		Searcher assetsearcher = mediaarchive.getAssetSearcher();
		HitTracker assets =  assetsearcher.query().exact("importstatus", "complete").search();
		assets.enableBulkOperations();
		
	
		for (Data hit in assets)
		{
	
			Asset asset = assetsearcher.loadData(hit);
			mediaarchive.removeGeneratedPages(asset, "image1500x1500");

	
		}


}

init();