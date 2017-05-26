import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.SearchQuery

public void init()
{
		MediaArchive archive = context.getPageValue("mediaarchive");//Search for all files looking for videos
		Searcher searcher = archive.getAssetSearcher();
		SearchQuery query = searcher.createSearchQuery();
		query.addMatches("fileformat", "mp3");
		query.addSortBy("id");
		Collection assets = searcher.search(query);
		List assetsToSave = new ArrayList();
		for (Data hit in assets)
		{
			Asset asset = archive.getAssetBySourcePath(hit.get("sourcepath"));
			archive.removeGeneratedImages(asset);
			asset.setProperty("importstatus", "imported");
			asset.setProperty("previewstatus", "converting");
			asset.setProperty("pushstatus", "notallconverted");
			assetsToSave.add(asset);
			if(assetsToSave.size() == 100)
			{
				archive.saveAssets( assetsToSave );
				assetsToSave.clear();
			}
		}
		archive.saveAssets assetsToSave;
		
		pageManager.clearCache();
		
		log.info("now kick off import event");
		archive.fireMediaEvent("importing","assetsimported", null,user);
}

init();