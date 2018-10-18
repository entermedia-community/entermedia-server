package notifications

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.search.AssetSearcher
import org.entermediadb.asset.util.TimeParser
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker

public void init(){
	
	MediaArchive archive = context.getPageValue("mediaarchive");
	
	AssetSearcher searcher = archive.getAssetSearcher();
	Searcher notifications = archive.getSearcher("notification");
	HitTracker renewals = searcher.query().before("renewaldate", new Date()).search();;
	ArrayList tosave = new ArrayList();
	renewals.each{
		Asset asset = searcher.loadData(it);	
		if(asset.getValue("assetexpired") != true) {
			asset.setValue("assetexpired", true);
			tosave.add(asset);
		}
		
		
	}
	
	searcher.saveAllData(tosave, null);
	
}

init();