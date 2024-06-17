package asset;

import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;

import org.elasticsearch.search.aggregations.AggregationBuilder
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.metrics.sum.SumBuilder
import org.entermediadb.asset.MediaArchive
import org.openedit.WebPageRequest
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery
import java.util.List
import org.openedit.Data
import org.openedit.BaseWebPageRequest
import java.util.ArrayList
import org.entermediadb.elasticsearch.SearchHitData

public void init(){
        log.info("Running assetpublish search "+catalogid);
        WebPageRequest req = context;

        String[] ar = req.getRequestParameters("field")

        Searcher searcher = searcherManager.getSearcher(catalogid, "asset");
        SearchQuery query = searcher.addStandardSearchTerms(req);
        if(query == null){
                query = searcher.createSearchQuery();
                query.addMatches("id", "*");

        }
        AggregationBuilder b = AggregationBuilders.dateHistogram("event_breakdown_day")
			.field("assetaddeddate").interval(DateHistogramInterval.DAY);//"query"

        query.setAggregation(b);
        HitTracker hits = searcher.search(query);

        context.putPageValue("hits", hits)

}
init();
