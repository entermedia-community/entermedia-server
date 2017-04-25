package org.entermediadb.elasticsearch.searchers;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.openedit.hittracker.SearchQuery;

public class ElasticLogSearcher extends BaseElasticSearcher  {

	
	
	@Override
	protected void addFacets(SearchQuery inQuery, SearchRequestBuilder inSearch)
	{
		// TODO Auto-generated method stub
		super.addFacets(inQuery, inSearch);
		DateHistogramBuilder builder = new DateHistogramBuilder("event_breakdown_day");
		builder.field("date");
		builder.interval(DateHistogramInterval.DAY);
		
//		DateHistogramBuilder builder = new DateHistogramBuilder("event_breakdown");
//		builder.interval(DateHistogramInterval.DAY);
		
		inSearch.addAggregation(builder);
		
		 builder = new DateHistogramBuilder("event_breakdown_week");
		builder.field("date");
		builder.interval(DateHistogramInterval.WEEK);
		
//		DateHistogramBuilder builder = new DateHistogramBuilder("event_breakdown");
//		builder.interval(DateHistogramInterval.DAY);
		
		inSearch.addAggregation(builder);
		
		
		
	}
	
	
	

}
