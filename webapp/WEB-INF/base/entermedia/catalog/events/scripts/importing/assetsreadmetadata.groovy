package importing

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.scanner.MetaDataReader
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.locks.Lock
import org.openedit.repository.ContentItem

public void init()
{
		MediaArchive archive = context.getPageValue("mediaarchive");//Search for all files looking for videos
		
			Searcher searcher = archive.getAssetSearcher();
			//HitTracker assets = searcher.getAllHits();
			HitTracker assets = searcher.query().exact("importstatus","needsmetadata").sort("sourcepath").search();
			assets.enableBulkOperations();
			assets.setHitsPerPage(100);
			List assetsToSave = new ArrayList();
			for (Data hit in assets)
			{
				Asset asset = searcher.loadData(hit);
				//log.info("${asset.getSourcePath()}");
				if( asset != null)
				{
					assetsToSave.add(asset);
					if(assetsToSave.size() == 100)
					{
						archive.readMetadata(assetsToSave);
						assetsToSave.clear();
					}
				}
			}
			archive.readMetadata( assetsToSave );
			archive.firePathEvent("importing/assetsimported",user,assetsToSave);
			//log.info("metadata reading complete");
			
		
}

init();