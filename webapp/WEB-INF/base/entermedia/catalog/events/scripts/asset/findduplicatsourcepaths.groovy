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
		
	AggregationBuilder b = AggregationBuilders.terms("sourcepath-terms").minDocCount(2).field("sourcepath").size(1000);
	query.setAggregation(b);
	HitTracker hits = searcher.search(query);
	hits.enableBulkOperations();
	FilterNode node = hits.findFilterNode("sourcepath-terms");
	
	
	
	ArrayList assets = new ArrayList();
	node.getChildren().each{
		FilterNode child = it;
		//println child.getId();
		HitTracker duplicates = searcher.fieldSearch("sourcepath", child.getId());
		duplicates.enableBulkOperations();
		duplicates.each{
			Data hit = it;
			Asset asset = searcher.loadData(hit);
			asset.setValue("duplicatesourcepath", true);
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
	query.addMatches("duplicatesourcepath", "true");
	HitTracker hits = searcher.search(query);
	hits.enableBulkOperations();
	ArrayList assets = new ArrayList();
	hits.each{
		Data hit = it;
		Asset asset = searcher.loadData(hit);
		asset.setValue("duplicatesourcepath", false);
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
