package importing;

import com.openedit.page.Page
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.*;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.*;
import org.openedit.entermedia.scanner.MetaDataReader;

public void init()
{
		MediaArchive archive = context.getPageValue("mediaarchive");//Search for all files looking for videos
		Searcher searcher = archive.getAssetSearcher();
		Collection assets = searcher.getAllHits();
		List assetsToSave = new ArrayList();
		MetaDataReader reader = moduleManager.getBean("metaDataReader");
		for (Data hit in assets)
		{
			Asset asset = archive.getAssetBySourcePath(hit.get("sourcepath"));

			Page content = archive.getOriginalDocument( asset );

			reader.populateAsset(archive, content.getContentItem(), asset);
			assetsToSave.add(asset);
			if(assetsToSave.size() == 100)
			{
				archive.saveAssets( assetsToSave );
				assetsToSave.clear();
				log.info("saved 100 metadata readings");
			}
		}
		archive.saveAssets assetsToSave;
		log.info("metadata reading complete");
}

init();