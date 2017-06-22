/*
 * Created on Jul 19, 2006
 */
package org.entermediadb.elasticsearch;

import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.openedit.hittracker.SearchQuery;

public class ElasticSearchQuery extends SearchQuery
{	
	
	public AbstractAggregationBuilder fieldAggregationBuilder;
	
	public AbstractAggregationBuilder getAggregation()
	{
		return fieldAggregationBuilder;
	}

	public void setAggregation(Object inAggregationBuilder)
	{
		fieldAggregationBuilder = (AbstractAggregationBuilder) inAggregationBuilder;
		setEndUserSearch(true);
	}

	public ElasticSearchQuery()
	{
		// TODO Auto-generated constructor stub
	}


}
