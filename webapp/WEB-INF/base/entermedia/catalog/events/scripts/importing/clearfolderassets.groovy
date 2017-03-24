package importing

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.page.Page

public void init()
{
	MediaArchive archive = context.getPageValue("mediaarchive");//Search for all files looking for videos
	
	Searcher assets = archive.getAssetSearcher();
	HitTracker folderassets = assets.query().match("isfolder", "true").search();
	folderassets.enableBulkOperations();
	folderassets.each{
		Asset asset = assets.loadData(it);
		if(asset.isFolder()){
			String path = "/WEB-INF/data/" + archive.getCatalogId() + "/generated/" + asset.getSourcePath() +"/";
			Page generatedfiles = archive.getPageManager().getPage(path);
			archive.getPageManager().removePage(generatedfiles);
			log.info("Would remove" + path);
			assets.delete(asset, null);
			
		}
	}
}


init();
