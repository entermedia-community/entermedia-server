package importing

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.scanner.MetaDataReader
import org.openedit.data.Searcher
import org.openedit.page.Page

public void init()
{
		MediaArchive archive = context.getPageValue("mediaarchive");
		String assetid = context.findValue("assetid");
		log.info("Reading metadata for asset $assetid");
		Searcher searcher = archive.getAssetSearcher();
		
		Asset asset = archive.getAsset("$assetid");
		if (asset!=null)
		{
			MetaDataReader reader = moduleManager.getBean("metaDataReader");
			Page content = archive.getOriginalDocument( asset );
			reader.populateAsset(archive, content.getContentItem(), asset);
			archive.saveAsset(asset, null);
			log.info("metadata reading complete");
		} 
		else 
		{
			log.info("unable to find $assetid, aborting");
		}
}
init();