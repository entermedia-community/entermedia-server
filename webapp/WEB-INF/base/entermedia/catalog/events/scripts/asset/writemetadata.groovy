package asset;


import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.search.AssetSearcher
import org.entermediadb.asset.xmp.XmpWriter
import org.openedit.Data
import org.openedit.hittracker.HitTracker



public void init()
{

	MediaArchive archive = context.getPageValue("mediaarchive");
	AssetSearcher searcher = archive.getAssetSearcher();
	
	HitTracker hits = searcher.getAllHits();
	hits.enableBulkOperations();
	
	
	XmpWriter writer = (XmpWriter) archive.getModuleManager().getBean("xmpWriter");
	
	
	hits.each {
		Data hit = it;
		Asset asset = searcher.loadData(hit);
		if( archive.isTagSync(asset.getFileFormat() ) )
			{
				boolean didSave = writer.saveMetadata(archive, asset);
				if(!didSave){
					log.info("Failed to write metadata for asset " + asset.getId());
				
				}
			}
	}
	
		
}

init();