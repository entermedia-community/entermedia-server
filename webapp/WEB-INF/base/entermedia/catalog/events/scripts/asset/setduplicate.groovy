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
	
	HitTracker all = searcher.query().all().sort("md5hex").sort("assetaddeddateUp").search();
	all.enableBulkOperations();
	HashSet tosave = new HashSet();

	Asset currentasset = null;
	for( Data duplicate : all)
	{
		if( currentasset == null)
		{
			currentasset = searcher.loadData( duplicate );
		}
		else
		{
			String hex1 = currentasset.get("md5hex");
			String hex2 = duplicate.get("md5hex");
			if(hex1.equals(hex2))
			{
				if( !Boolean.parseBoolean( duplicate.get("duplicate") ))
				{
					Asset newer = searcher.loadData(duplicate);
					newer.setValue("duplicate",true);
					tosave.add(newer); //hashset
				}
				if( !Boolean.parseBoolean( currentasset.get("duplicate") ))
				{
					currentasset.setValue("duplicate",true);
					tosave.add(currentasset); //hashset
				}
			}
			else
			{
				//Load Next
				currentasset = searcher.loadData( duplicate );
			}
		}
	}
	log.info("Found duplicates " + tosave.size());
	searcher.saveAllData(tosave, null);
}

init();
