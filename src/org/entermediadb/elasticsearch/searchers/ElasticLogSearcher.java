package org.entermediadb.elasticsearch.searchers;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.openedit.hittracker.SearchQuery;

public class ElasticLogSearcher extends BaseElasticSearcher  {

	
	
	@Override
	protected void addFacets(SearchQuery inQuery, SearchRequestBuilder inSearch)
	{
		// TODO Auto-generated method stub
		super.addFacets(inQuery, inSearch);
		DateHistogramAggregationBuilder builder = new DateHistogramAggregationBuilder("event_breakdown_day");
		builder.field("date");
		builder.dateHistogramInterval(DateHistogramInterval.DAY);
		
//		DateHistogramBuilder builder = new DateHistogramBuilder("event_breakdown");
//		builder.interval(DateHistogramInterval.DAY);
		
		inSearch.addAggregation(builder);
		
		 builder = new DateHistogramAggregationBuilder("event_breakdown_week");
		builder.field("date");
		builder.dateHistogramInterval(DateHistogramInterval.WEEK);
		
//		DateHistogramBuilder builder = new DateHistogramBuilder("event_breakdown");
//		builder.interval(DateHistogramInterval.DAY);
		
		inSearch.addAggregation(builder);
		
		
		
	}
	
	
	

}
