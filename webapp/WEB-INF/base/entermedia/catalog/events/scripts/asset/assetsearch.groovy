package asset;

import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.WebPageRequest
import org.openedit.hittracker.HitTracker




public void init()
{
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");

	HitTracker hits = archive.getAssetSearcher().query().exact("facescancomplete", "true").search();
	
	hits.enableBulkOperations();
	log.info("Processing " + hits.size());
	
	List tosave = new ArrayList();
	
	hits.each{
		Data asset = archive.getAsset(it.id);
		asset.setValue("facescancomplete", "false");
		asset.setValue("faceprofiles", null)
		tosave.add(asset);
		if(tosave > 1000) {
			archive.saveAssets(tosave);
			tosave.clear();
		}
	}
	
	
	archive.saveAssets(tosave);
	
	
	
}

