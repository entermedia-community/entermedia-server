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
	log.info("Running diskusage search");
	WebPageRequest req = context;
	String catalogid = context.getPageValue("reportcatalogid");

	Searcher searcher = searcherManager.getSearcher(catalogid, "asset");
	//searcher.putMappings();//just in case it's never been done.
	
	SearchQuery query = searcher.addStandardSearchTerms(context);
	if(query == null){
		query = searcher.query().all().getQuery();
	}
	AggregationBuilder b = AggregationBuilders.terms("assettype_filesize").field("assettype");
	SumBuilder sum = new SumBuilder("assettype_sum");
	sum.field("filesize");
	b.subAggregation(sum);
	query.setAggregation(b);
	//query.addMatches("id", "*");
	//query.setEndUserSearch(false);

	
	HitTracker hits =searcher.search(query);
	//log.info("query:" + query.hasFilters());
	if(hits != null) {
		hits.enableBulkOperations();
		hits.getActiveFilterValues();
		StringTerms agginfo = hits.getAggregations().get("assettype_filesize");
		context.putPageValue("breakdownhits", hits)
		context.putPageValue("hits", hits)
		
		log.info(agginfo.getBuckets().size())
	}
	/*
		#foreach($item in $breakdownhits.getAggregations().get("assettype_filesize").getBuckets())
				#foreach($subitem in $item.getAggregations())
					title="$item.key">$!sizer.inEnglish($subitem.getValue()) </span>
					#set( $data = $mediaarchive.getData("assettype",$item.key))
					$context.getText($data)
	
	*/
	//	<li class="list-group-item"><span class="badge">$!sizer.inEnglish($breakdownhits.getSum("filesize"))
	
	log.info("hits" + hits.size());
	
}

init();
	
