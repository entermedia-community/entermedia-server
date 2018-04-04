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
	
	HitTracker all = searcher.query().exact("duplicate", "true").sort("md5hex").sort("assetaddeddate").search();
	all.enableBulkOperations();
	HashSet tosave = new HashSet();

	Collection todelete = new ArrayList();
		
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
				Asset older = searcher.loadData(duplicate);
				for( Category cat : older.getCategories() )
				{
					currentasset.addCategory(cat);
				}
				todelete.add(older);
				currentasset.setValue("duplicate",false);
				tosave.add(currentasset);
			}
			else
			{
				currentasset = searcher.loadData( duplicate );
			}
		}
	}
	log.info("Keeping " + tosave.size());
	searcher.saveAllData(tosave, null);
	log.info("Deleting " + todelete.size());
	for(Asset oldjunk : todelete)
	{
		ContentItem item = archive.getOriginalContent(oldjunk);
		archive.getPageManager().getRepository().remove(item);
		log.info("Deduplication deleted " + item.getAbsolutePath());
		archive.removeGeneratedImages(oldjunk);
	}	
	searcher.deleteAll(todelete, null)
		
}

init();
