package importing

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.scanner.MetaDataReader
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.repository.ContentItem

import groovy.util.logging.Log

public void init()
{
		MediaArchive archive = context.getPageValue("mediaarchive");
		Searcher searcher = archive.getAssetSearcher();
		HitTracker assets = searcher.query().all().not("editstatus","7").sort("id").search();
		assets.enableBulkOperations();
		String ids = context.getRequestParameter("assetids");
		if( ids != null )
		{
			String[] assetids = ids.split(",");
			assets.setSelections(Arrays.asList( assetids) );
			assets.setShowOnlySelected(true);
		}

		List assetsToSave = new ArrayList();
		MetaDataReader reader = moduleManager.getBean("metaDataReader");
		for (Data hit in assets)
		{
			log.info("metadatareader loading asset: ${hit.id}")
			Asset asset = searcher.loadData(hit);

			if( asset != null)
			{
				ContentItem content = archive.getOriginalContent( asset );
				reader.populateAsset(archive, content, asset);
				assetsToSave.add(asset);
				if(assetsToSave.size() == 300)
				{
					archive.saveAssets( assetsToSave );
					assetsToSave.clear();
					log.info("saved 300 metadata readings");
				}
			}
		}
		archive.saveAssets assetsToSave;
		log.info("metadata reading complete");
}

init();