package asset;

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.WebPageRequest
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker




public void init()
{
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");
	Searcher assets = archive.getAssetSearcher();
	HitTracker hits = archive.getAssetSearcher().query().all().search();
	hits.enableBulkOperations();
	List tosave = new ArrayList();
	hits.each{
		Data hit = it;		
		if( hit.get("mastereditclusterid") == null)
		{
			Asset asset = assets.loadData(hit);
			asset.setValue("mastereditclusterid", archive.getNodeManager().getLocalClusterId() );
			
			tosave.add(asset);			
			if( tosave.size() == 1000)
			{
				assets.saveAllData(tosave, null);
				tosave.clear();
				log.info("Saved 1000");
			}
		}
	}
	archive.saveAssets(tosave, null);
	log.info("Done");
}

init();



