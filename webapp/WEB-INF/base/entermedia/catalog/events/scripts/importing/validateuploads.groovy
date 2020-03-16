package importing

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.scanner.MetaDataReader
import org.entermediadb.scripts.ScriptLogger
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.locks.Lock
import org.openedit.repository.ContentItem

import model.assets.AssetTypeManager

public void init()
{
		MediaArchive archive = context.getPageValue("mediaarchive");//Search for all files looking for videos
		
		Searcher searcher = archive.getAssetSearcher();
		//HitTracker assets = searcher.getAllHits();

		//Use this to check on  
		
		HitTracker assets = searcher.query().exact("detectedfileformat","win32 exe").not("fileformat","exe").search();
		assets.enableBulkOperations();
		assets.setHitsPerPage(2000);
		log.info("Running on " + assets.size());
		
		AssetTypeManager manager = new AssetTypeManager();
		manager.setContext(context);
		manager.setLog(log);
		manager.validateAssetTypes(archive,assets);
		log.info("checking complete");
			
		
}

init();