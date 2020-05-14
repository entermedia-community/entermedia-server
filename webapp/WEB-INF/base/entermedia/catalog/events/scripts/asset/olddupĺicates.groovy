package asset;


import org.entermediadb.asset.Asset
import org.entermediadb.asset.Category
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.search.AssetSearcher
import org.openedit.Data
import org.openedit.hittracker.HitTracker
import org.openedit.repository.ContentItem



public void init()
{

	MediaArchive archive = context.getPageValue("mediaarchive");
	AssetSearcher searcher = archive.getAssetSearcher();
	
	HitTracker all = searcher.query().exact("duplicate", "true").search();
	all.enableBulkOperations();
	HashSet tosave = new HashSet();
		
	Asset currentasset = null;
	for( Data duplicate : all)
	{
			currentasset = searcher.loadData( duplicate );
			String hex1 = currentasset.get("md5hex");
			HitTracker others = searcher.query().exact("duplicate", "true").match("md5hex", hex1).search();
			if (others.size() == 1) {
				currentasset.setValue("duplicate",false);
				tosave.add(currentasset);
			}
	}
	log.info("Old Duplicates  " + tosave.size());
	searcher.saveAllData(tosave, null);
		
}

init();
