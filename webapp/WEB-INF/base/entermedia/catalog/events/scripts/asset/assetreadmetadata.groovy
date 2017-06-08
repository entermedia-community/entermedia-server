package importing

import org.entermediadb.asset.Asset
import org.entermediadb.asset.CompositeAsset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.scanner.MetaDataReader
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.page.Page

public void init()
{
		MediaArchive archive = context.getPageValue("mediaarchive");
		String assetid = context.findValue("assetid");
		
		log.info("Reading metadata for asset $assetid");
		Searcher searcher = archive.getAssetSearcher();
		
		Asset asset = archive.getAsset(assetid,context);
		if( asset == null)
		{
			log.error("Could not load asset" + assetid);
			return;
		}
		MetaDataReader reader = moduleManager.getBean("metaDataReader");
		
		if( asset instanceof CompositeAsset)
		{
			CompositeAsset multi = (CompositeAsset)asset;
			for (Iterator iterator = multi.getSelectedResults().iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				Asset entry = searcher.loadData(data);
				readAsset(archive,reader,entry);
			}
		}
		else
		{
			readAsset(archive,reader,asset);
		}
}

public void readAsset(MediaArchive archive, MetaDataReader reader, Asset asset)
{
	Page content = archive.getOriginalDocument( asset );
	reader.populateAsset(archive, content.getContentItem(), asset);
	archive.saveAsset(asset, null);
	log.info("metadata reading complete");

}
init();