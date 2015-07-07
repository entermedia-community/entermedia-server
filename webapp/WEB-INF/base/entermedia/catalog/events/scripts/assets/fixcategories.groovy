import org.openedit.Data
import org.openedit.entermedia.Asset
import org.openedit.entermedia.Category;
import org.openedit.entermedia.MediaArchive

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.util.PathUtilities



public void init(){
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");
	
	HitTracker hits = archive.getAssetSearcher().query().match("id", "*").sort("id").search();
	hits.setHitsPerPage(200);
	List tosave = new ArrayList();
	int savedsofar = 0;
	hits.each{
		Data hit = it;
		if( hit.get("category-exact") == null)
		{
			String path = PathUtilities.extractDirectoryPath(hit.getSourcePath());
			org.openedit.entermedia.Category catparent = archive.getCategoryArchive().createCategoryTree(path);
			Asset found = archive.getAssetBySourcePath(hit.getSourcePath());
			found.addCategory(catparent);
			tosave.add(found);
			savedsofar++;
			if( tosave.size() == 200)
			{
				archive.saveAssets(tosave, null);
				tosave.clear();
				log.info("saved assets ${savedsofar}");
			}
		}
	}
	archive.saveAssets(tosave, null);
	log.info("Finished fixcategories saved: ${savedsofar}");
}

init();