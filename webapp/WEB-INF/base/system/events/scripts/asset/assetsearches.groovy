package asset;

import org.elasticsearch.search.aggregations.AggregationBuilder
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms
import org.openedit.WebPageRequest
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery

public void init(){
	log.info("Running aggregation search");
	WebPageRequest req = context;
	String catalogid = context.getPageValue("reportcatalogid");

	Searcher searcher = searcherManager.getSearcher(catalogid, "assetsearchLog");
//	searcher.putMappings();//just in case it's never been done.
	
	SearchQuery query = searcher.addStandardSearchTerms(context);
	if(query == null){
		query = searcher.createSearchQuery();
		query.addMatches("id", "*");
		
	}
	AggregationBuilder b = AggregationBuilders.terms("keywords").field("query");
	query.setAggregation(b);
	HitTracker hits =searcher.search(query);
	hits.enableBulkOperations();
	hits.getFilterOptions();
	
	context.putPageValue("breakdownhits", hits)
	StringTerms agginfo = hits.getAggregations().get("keywords");
	context.putPageValue("breakdownhits", hits)
	log.info(agginfo.getBuckets().size())
	log.info("hits" + hits.size());
	context.putPageValue("hits", hits)
	
	log.info("response was: " + hits.getSearchResponse(0).toString());
	//log.info(response.toString());
	
}

init();
	
