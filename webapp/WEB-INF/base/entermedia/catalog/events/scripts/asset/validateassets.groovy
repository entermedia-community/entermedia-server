package asset

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.page.manage.PageManager
import org.openedit.repository.ContentItem



public init()
{
	
	
	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");
	PageManager pageManager = mediaarchive.getPageManager();
	
	
	Searcher searcher = mediaarchive.getSearcher("validation");
	HitTracker hits = searcher.getAllHits();
	hits.enableBulkOperations();
	ArrayList tosave = new ArrayList();
	
	hits.each{
		Data data = searcher.loadData(it);
		String assetid = data.getAt("assetid");
		Asset asset = mediaarchive.getAsset(assetid);
		if(asset == null) {
			data.setValue("record", false);
			data.setValue("original", false);
			
		}
		if(asset != null) {
			data.setValue("record", true);
			ContentItem item = mediaarchive.getOriginalContent(asset);
			if(item.exists()) {
				data.setValue("original", true);
			} else {
				data.setValue("original", false)
			}
		}
		tosave.add(data);
	}
	searcher.saveAllData(tosave, null);
}

init();
