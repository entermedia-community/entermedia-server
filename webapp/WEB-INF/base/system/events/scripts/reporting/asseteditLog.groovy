package reporting;

import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramBuilder
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram.Order
import org.openedit.WebPageRequest
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery

public void init(){
	log.info("Running report");
	WebPageRequest req = context;
	Searcher searcher = searcherManager.getSearcher(catalogid, "asseteditLog");//assetsearchLog");
	//searcher.putMappings();//just in case it's never been done.
	
	SearchQuery query = searcher.addStandardSearchTerms(req);
	if(query == null){
		query = searcher.createSearchQuery();
		query.addMatches("id", "*");
	}
	DateHistogramBuilder builder = new DateHistogramBuilder("event_breakdown_day");
	builder.field("date");
	builder.interval(DateHistogramInterval.DAY);
	builder.order(Order.KEY_DESC);

	query.setAggregation(builder);
	query.addMatches("id", "*");
	HitTracker hits =searcher.search(query);
	hits.enableBulkOperations();
	req.putPageValue("hits", hits);
	//log.info(hits);
}

init();

