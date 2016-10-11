package org.entermediadb.elasticsearch.searchers;

import java.util.Iterator;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.openedit.Data;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventListener;
import org.openedit.hittracker.SearchQuery;
import org.openedit.util.DateStorageUtil;

public class ElasticLogSearcher extends BaseElasticSearcher   implements WebEventListener {

	
	public void eventFired(WebEvent inEvent) {
		Data entry = createNewData();
		entry.setProperty("operation", inEvent.getOperation());
		entry.setProperty("user", inEvent.getUsername());
		for (Iterator iterator = inEvent.keySet().iterator(); iterator
				.hasNext();) {
			String key = (String) iterator.next();
			entry.setProperty(key, inEvent.get(key));
		}
		entry.setProperty("date", DateStorageUtil.getStorageUtil()
				.formatForStorage(inEvent.getDate()));
		saveData(entry, null);
	}
	
	
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
