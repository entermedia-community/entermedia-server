package importing

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.scanner.Md5MetadataExtractor
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.repository.ContentItem

public void init() {
	MediaArchive archive = context.getPageValue("mediaarchive");//Search for all files looking for videos
	Searcher searcher = archive.getAssetSearcher();
	//HitTracker assets = searcher.getAllHits();
	HitTracker assets = searcher.query().match("id","*").not("editstatus","7").search();
	assets.enableBulkOperations();


	List assetsToSave = new ArrayList();
	Md5MetadataExtractor reader = moduleManager.getBean("md5MetadataExtractor");
	for (Data hit in assets)
	{
		if(hit.md5hex == null){
			Asset asset = archive.getAssetBySourcePath(hit.get("sourcepath"));

			if( asset != null)
			{
				ContentItem content = archive.getOriginalContent( asset );
				if(content.exists()){
					reader.extractData(archive, content, asset);
					assetsToSave.add(asset);
				}
				if(assetsToSave.size() == 1000)
				{
					archive.saveAssets( assetsToSave );
					assetsToSave.clear();
					log.info("saved 1000 metadata readings");
				}
			}
		}
	}
	archive.saveAssets assetsToSave;
	log.info("metadata reading complete");
}

init();