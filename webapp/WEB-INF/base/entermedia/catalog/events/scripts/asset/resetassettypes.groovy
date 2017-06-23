package asset;

import org.entermediadb.asset.MediaArchive
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import asset.model.*;

public void runit()
{
MediaArchive mediaArchive = context.getPageValue("mediaarchive");
Searcher assetsearcher = mediaArchive.getAssetSearcher();

log.info("Ran");
HitTracker assets = assetsearcher.getAllHits();
assets.enableBulkOperations();

AssetTypeManager manager = new AssetTypeManager();
manager.context = context;
manager.log = log;
manager.saveAssetTypes(assets);

}

runit();