package notifications

import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.search.AssetSearcher
import org.openedit.hittracker.HitTracker

public void init(){
	
	MediaArchive archive = context.getPageValue("mediaarchive");
	
	AssetSearcher searcher = archive.getAssetSearcher();
	HitTracker renewals = searcher.fieldSearch("renewalpolicy", "*");
	
	
	
}

init();