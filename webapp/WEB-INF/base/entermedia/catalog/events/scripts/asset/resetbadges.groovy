package asset;

import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.PropertyDetail
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker

import asset.model.*;

public void runit()
{
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	Searcher assetsearcher = mediaArchive.getAssetSearcher();
	
	HitTracker assets = assetsearcher.getAllHits();
	assets.enableBulkOperations();
	log.info("running on " + assets.size() + " assets");

	List badges = mediaArchive.getAssetSearcher().getPropertyDetails().findBadgesProperties();
	List tosave = new ArrayList();
	for (Data asset in assets) 
	{
		for (PropertyDetail detail in badges) 
		{
			Object value = asset.getValue(detail.getId());
			if( value != null)
			{
				asset = mediaArchive.getAssetSearcher().loadData(asset);
				tosave.add(asset);
				break;
			}
		}
		if( tosave.size() > 1000)
		{
			mediaArchive.saveAssets(tosave);
			log.info("saved " + tosave.size() + " assets");
			tosave.clear();
		}
	}
	mediaArchive.saveAssets(tosave);
	log.info("Finished saved " + tosave.size() + " assets");
}

runit();