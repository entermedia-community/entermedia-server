package asset;


import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.search.AssetSearcher
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery



public void init()
{

	MediaArchive archive = context.getPageValue("mediaarchive");
	AssetSearcher searcher = archive.getAssetSearcher();
	Searcher pushsearcher = archive.getSearcher("pushassets");

	HitTracker hits = searcher.getAllHits();
	hits.enableBulkOperations();
	HashSet assetids = new HashSet();
	
	hits.each{
		
		String id = it.id;
		if(id.startsWith("h")) {
			id = id.substring(1, id.length());
		}
		Data pushasset = pushsearcher.searchById(id);
		if(pushasset != null) {
			
				assetids.add("h"+it.id);				
			
		}		else {
			log.info("Asset " + it.id + " Was not found in push export ");
		}
	}
	ArrayList tosave = new ArrayList();
	
	assetids.each { 
		Asset asset = searcher.searchById(it);
		asset.setValue("existsonpush", true);
		tosave.add(asset);
		if(tosave.size() > 1000) {
			searcher.saveAllData(tosave, null);
			tosave.clear();
		}
	}
	
	searcher.saveAllData(tosave, null);
	
		
}


public void clearFlag(){
	
	MediaArchive archive = context.getPageValue("mediaarchive");
	AssetSearcher searcher = archive.getAssetSearcher();
	
	SearchQuery query = searcher.createSearchQuery();
	query.addMatches("existsonpush", "true");
	HitTracker hits = searcher.search(query);
	hits.enableBulkOperations();
	ArrayList assets = new ArrayList();
	hits.each{
		Data hit = it;
		Asset asset = searcher.loadData(hit);
		asset.setValue("existsonpush", false);
		assets.add(asset);
		if(assets.size() > 1000){
			searcher.saveAllData(assets, null);
			assets.clear();
		}
	}
	searcher.saveAllData(assets, null);
	
}

clearFlag();
init();
