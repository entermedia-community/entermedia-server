package org.entermediadb.elasticsearch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram.Order;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.avg.AvgBuilder;
import org.elasticsearch.search.aggregations.metrics.sum.SumBuilder;
import org.openedit.Data;
import org.openedit.cache.CacheManager;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.FilterNode;
import org.openedit.hittracker.SearchQuery;
import org.openedit.hittracker.UserFilters;
import org.openedit.profile.UserProfile;

public class ElasticUserFilters implements UserFilters
{

	protected UserProfile fieldUserProfile;
	protected CacheManager fieldCacheManager;

	public CacheManager getCacheManager()
	{
		return fieldCacheManager;
	}

	public void setCacheManager(CacheManager inCacheManager)
	{
		fieldCacheManager = inCacheManager;
	}

	protected PropertyDetailsArchive fieldPropertyDetailsArchive;
	protected SearcherManager fieldSearcherManager;

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public PropertyDetailsArchive getPropertyDetailsArchive()
	{
		if (fieldPropertyDetailsArchive == null)
		{
			fieldPropertyDetailsArchive = getSearcherManager().getPropertyDetailsArchive(getUserProfile().getCatalogId());

		}

		return fieldPropertyDetailsArchive;

	}

	public UserProfile getUserProfile()
	{
		return fieldUserProfile;
	}

	public void setUserProfile(UserProfile inUserProfile)
	{
		fieldUserProfile = inUserProfile;
	}


	
	
//	List facets = getPropertyDetailsArchive().getView(inSearchType, inSearchType + "/" + inSearchType + "facets", getUserProfile());
//
//	if (facets != null && facets.size() > 0)
//	{
//		for (Iterator iterator = facets.iterator(); iterator.hasNext();)
//		{
//			PropertyDetail detail = (PropertyDetail) iterator.next();
//			//	if (detail.isFilter())
//			//		{
//			allFilters.add(detail);
//			//	}
//
//		}
//
//	}

	
	
	
	

	

	public List<FilterNode> getFilterOptions(String inSearchType, SearchQuery inQuery)
	{
		if (inQuery.getMainInput() != null)
		{
			Object object = getCacheManager().get("facethits" + inSearchType, inQuery.getMainInput());
			return (List<FilterNode>) object;
		}
		else
		{
			return null;
		}

	}

	public void clear(String inSearchType)
	{
		getCacheManager().clear("facethits" + inSearchType);
	}

}
