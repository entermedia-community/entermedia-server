import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.Searcher

public void init()
{
		MediaArchive archive = context.getPageValue("mediaarchive");//Search for all files looking for videos
		Searcher searcher = archive.getAssetSearcher();
		Collection assets = searcher.getAllHits();
		List assetsToSave = new ArrayList();
		for (Data hit in assets)
		{
			Asset asset = archive.getAssetBySourcePath(hit.get("sourcepath"));
			archive.removeGeneratedImages(asset);
			asset.setProperty("importstatus", "imported");
			assetsToSave.add(asset);
			if(assetsToSave.size() == 100)
			{
				archive.saveAssets( assetsToSave );
				assetsToSave.clear();
			}
		}
		archive.saveAssets assetsToSave;
		log.info("now kick off import event");
		archive.fireMediaEvent("importing/assetsimported", user);
}

init();