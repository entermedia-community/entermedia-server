/*
 * Created on Jul 19, 2006
 */
package org.entermediadb.elasticsearch;

import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.openedit.data.PropertyDetail;
import org.openedit.hittracker.SearchQuery;

public class ElasticSearchQuery extends SearchQuery
{	
	
	public AbstractAggregationBuilder fieldAggregationBuilder;
	protected String fieldAggregationJson;
	
	public String getAggregationJson()
	{
		return fieldAggregationJson;
	}

	public void setAggregationJson(String inAggregationJson)
	{
		fieldAggregationJson = inAggregationJson;
	}

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
	
	@Override
	public void addFunction(PropertyDetail inDetail, Object inValues)
	{
		// TODO Auto-generated method stub
		super.addFunction(inDetail, inValues);
	}

}
