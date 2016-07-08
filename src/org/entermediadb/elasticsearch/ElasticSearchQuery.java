/*
 * Created on Jul 19, 2006
 */
package org.entermediadb.elasticsearch;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.openedit.hittracker.SearchQuery;

public class ElasticSearchQuery extends SearchQuery
{	
	
	public AggregationBuilder fieldAggregationBuilder;
	
	public AggregationBuilder getAggregation()
	{
		return fieldAggregationBuilder;
	}

	public void setAggregation(AggregationBuilder inAggregationBuilder)
	{
		fieldAggregationBuilder = inAggregationBuilder;
	}

	public ElasticSearchQuery()
	{
		// TODO Auto-generated constructor stub
	}


}
