package asset;

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.elasticsearch.categories.ElasticCategory
import org.openedit.WebPageRequest
import org.openedit.hittracker.HitTracker

public void init(){

	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");
	HitTracker assets = archive.getAssetSearcher().getAllHits();
	ArrayList catstodelete = new ArrayList();
	ArrayList assetstosave = new ArrayList();
	assets.enableBulkOperations();
	assets.each{
		Asset asset = archive.getAsset(it.id);
		ArrayList toremove = new ArrayList();
		if(asset != null){
			asset.getCategories().each{
				ElasticCategory cat = it;

				if(cat.getName() != null && asset.getName()!= null && cat.getName().equals(asset.getName())){
					log.info("here");
					catstodelete.add(cat);
					toremove.add(cat);
					asset.addCategory(cat.getParentCategory());
					assetstosave.add(asset);
				}
			}
			toremove.each{
				asset.removeCategory(it);
			}
		}
	}
	archive.getCategorySearcher().deleteAll(catstodelete, null);
	archive.getAssetSearcher().saveAllData(assetstosave, null);
}
init();