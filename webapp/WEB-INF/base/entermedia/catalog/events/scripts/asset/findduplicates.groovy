package asset;


import org.elasticsearch.search.aggregations.AggregationBuilder
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.search.AssetSearcher
import org.openedit.Data
import org.openedit.hittracker.FilterNode
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery



public void init()
{

	MediaArchive archive = context.getPageValue("mediaarchive");
	AssetSearcher searcher = archive.getAssetSearcher();
	
	SearchQuery query = searcher.createSearchQuery();
	query.addNot("duplicate", "true");
		
	AggregationBuilder b = AggregationBuilders.terms("md5hex-detect").minDocCount(2).field("md5hex").size(1000);
	query.setAggregation(b);
	HitTracker hits = searcher.search(query);
	FilterNode node = hits.findFilterNode("md5hex-detect");
	
	
	
	ArrayList assets = new ArrayList();
	node.getChildren().each{
		FilterNode child = it;
		println child.getId();
		HitTracker duplicates = searcher.fieldSearch("md5hex", child.getId());
		duplicates.each{
			Data hit = it;
			Asset asset = searcher.loadData(hit);
			asset.setValue("duplicate", true);
			asset.setValue("deleted", true);
			assets.add(asset);
		}
		child.getMap();
		println child.getCount();
		
	}	
	searcher.saveAllData(assets, null);
	
	
		
}


public void clearDuplicateFlag(){
	
	MediaArchive archive = context.getPageValue("mediaarchive");
	AssetSearcher searcher = archive.getAssetSearcher();
	
	SearchQuery query = searcher.createSearchQuery();
	query.addMatches("duplicate", "true");
	HitTracker hits = searcher.search(query);
	
	ArrayList assets = new ArrayList();
	hits.each{
		Data hit = it;
		Asset asset = searcher.loadData(hit);
		asset.setValue("duplicate", false);
		assets.add(asset);
		if(assets.size() > 1000){
			searcher.saveAllData(assets, null);
			assets.clear();
		}
	}
	searcher.saveAllData(assets, null);
	
}

clearDuplicateFlag();
init();
