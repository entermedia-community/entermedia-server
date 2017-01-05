package asset;

import org.elasticsearch.search.aggregations.AggregationBuilder
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms
import org.elasticsearch.search.aggregations.metrics.sum.SumBuilder
import org.entermediadb.asset.MediaArchive
import org.openedit.WebPageRequest
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery

public void init(){
	log.info("Running aggregation search - disk space by file format");
	WebPageRequest req = context;
	String catalogid = context.getPageValue("reportcatalogid");
	if(catalogid == null){
		catalogid = context.getPageValue("catalogid");
	}
	
	Searcher searcher = searcherManager.getSearcher(catalogid, "asset");
	searcher.putMappings();//just in case it's never been done.
	
	SearchQuery query = searcher.addStandardSearchTerms(context);
	if(query == null){
		query = searcher.createSearchQuery();
	}
	AggregationBuilder b = AggregationBuilders.terms("fileformat_filesize").field("libraries");
	SumBuilder sum = new SumBuilder("filesize_sum");
	sum.field("filesize");
	b.subAggregation(sum);
	query.setAggregation(b);
	HitTracker hits =searcher.search(query);
	hits.enableBulkOperations();
	hits.getFilterOptions();
	//StringTerms agginfo = hits.getAggregations().get("fileformat_filesize");
	context.putPageValue("diskspacehits", hits)
	context.putPageValue("hits", hits)
	
	//log.info(agginfo.getBuckets().size())
	log.info("Disk Space Library hits" + hits.size());
	
}

init();
	
