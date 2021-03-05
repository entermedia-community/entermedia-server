package asset

import org.entermediadb.asset.*
import org.entermediadb.asset.search.AssetSearcher
import org.openedit.Data
import org.openedit.hittracker.HitTracker

public void init()
{
	//Delete assets with Sourcepat starting with:
	String initialsourcepath = "Archival Collections/Photographs";
		
	MediaArchive archive = context.getPageValue("mediaarchive");
	AssetSearcher searcher = mediaarchive.getAssetSearcher();
	HitTracker hits = archive.query("asset").startsWith("sourcepath", initialsourcepath).search();
	hits.enableBulkOperations();
		
	int saved = 0;
	//List tosave = new ArrayList();
	//Category root = archive.getCategorySearcher().getRootCategory();
	for(Data hit in hits)
	{
		Asset asset = archive.getAssetSearcher().loadData(hit);
		//String path = asset.getPath();
		//Category cat = archive.getCategorySearcher().createCategoryPath(path);
		//asset.removeCategory(root);
		//asset.addCategory(cat);
		log.info("Deleting: " + asset.getId());
		archive.deleteAsset(asset, false);
		saved = saved +  1;
	}
	log.info("deleted " + saved);
}

public void rename()
{
	//Delete assets with Sourcepat starting with:
	String initialsourcepath = "Photographs";
		
	MediaArchive archive = context.getPageValue("mediaarchive");
	AssetSearcher searcher = mediaarchive.getAssetSearcher();
	HitTracker hits = archive.query("asset").startsWith("sourcepath", initialsourcepath).search();
	hits.enableBulkOperations();
		
	int saved = 0;
	List tosave = new ArrayList();
	//Category root = archive.getCategorySearcher().getRootCategory();
	for(Data hit in hits)
	{
		Asset asset = archive.getAssetSearcher().loadData(hit);
		String path = asset.getPath();

		asset.setSourcePath("Archival Collections/"+path);
		tosave.add(asset);
		
		if( tosave.size() == 1000 )
		{
			saved = saved +  tosave.size();
			log.info("saved " + saved);
			archive.saveAssets(tosave);
			tosave.clear();
		}
	}
	archive.saveAssets(tosave);
	saved = saved +  tosave.size();
	log.info("saved " + saved);
}

init();
rename();


