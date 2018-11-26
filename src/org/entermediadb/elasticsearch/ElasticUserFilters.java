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

	public boolean addFacets(String inSearchType, SearchQuery inQuery, SearchRequestBuilder inSearch)
	{
		if (!(inQuery.isEndUserSearch() || inQuery.isFilter()))
		{

			return false;
		}
		List<FilterNode> filters = (List<FilterNode>) getFilterOptions(inSearchType, inQuery);

		if (filters != null)
		{
			return false;
		}

		Set allFilters = new HashSet();

		if (getUserProfile() != null)
		{

			List facets = getPropertyDetailsArchive().getView(inSearchType, inSearchType + "/" + inSearchType + "facets", getUserProfile());

			if (facets != null && facets.size() > 0)
			{
				for (Iterator iterator = facets.iterator(); iterator.hasNext();)
				{
					PropertyDetail detail = (PropertyDetail) iterator.next();
					//	if (detail.isFilter())
					//		{
					allFilters.add(detail);
					//	}

				}

			}

			for (Iterator iterator = inQuery.getExtraFacets().iterator(); iterator.hasNext();)
			{
				String detail = (String) iterator.next();
				PropertyDetail facet = getPropertyDetailsArchive().getPropertyDetails(inSearchType).getDetail(detail);
				if (facet != null)
				{
					allFilters.add(facet);
				}

			}
		}
		for (Iterator iterator = allFilters.iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();

			if (detail.isDate())
			{
				DateHistogramBuilder builder = new DateHistogramBuilder(detail.getId() + "_breakdown_day");
				builder.field(detail.getId());
				builder.interval(DateHistogramInterval.DAY);
				builder.order(Order.KEY_DESC);
				//	String timezone = TimeZone.getDefault().getID();
				//		builder.timeZone(timezone);
				inSearch.addAggregation(builder);

				builder = new DateHistogramBuilder(detail.getId() + "_breakdown_week");
				builder.field(detail.getId());
				//	builder.timeZone(timezone);

				builder.interval(DateHistogramInterval.WEEK);
				builder.order(Order.COUNT_DESC);

				inSearch.addAggregation(builder);

			}

			else if (detail.isNumber())
			{
				SumBuilder b = new SumBuilder(detail.getId() + "_sum");
				b.field(detail.getId());
				inSearch.addAggregation(b);

				AvgBuilder avg = new AvgBuilder(detail.getId() + "_avg");
				avg.field(detail.getId());

			}
			else if (detail.isList() || detail.isBoolean() || detail.isMultiValue())
			{
				if (detail.isViewType("tageditor"))
				{
					AggregationBuilder b = AggregationBuilders.terms(detail.getId()).field(detail.getId() + ".exact").size(100);
					inSearch.addAggregation(b);
				}
				else
				{

					AggregationBuilder b = AggregationBuilders.terms(detail.getId()).field(detail.getId()).size(100);
					inSearch.addAggregation(b);

				}
			}
			else
			{
				AggregationBuilder b = AggregationBuilders.terms(detail.getId()).field(detail.getId() + ".exact").size(100);
				inSearch.addAggregation(b);
			}

		}

		// For reports, we can pass in a custom aggregation from a script or
		// somewhere

		if (inQuery.getAggregation() != null)
		{
			inSearch.addAggregation((AbstractAggregationBuilder) inQuery.getAggregation());

		}
		return true;
	}

	public List setFilterOptions(String inSearchType, SearchQuery inQuery, SearchResponse response) //parse em
	{
		List topfacets = new ArrayList();
		//TODO: Should save the response and only load it if someone needs the data
		if (response.getAggregations() != null)
		{

			Aggregations facets = response.getAggregations();

			for (Iterator iterator = facets.iterator(); iterator.hasNext();)
			{
				Object agg = iterator.next();
				if (agg instanceof Terms)
				{
					Terms f = (Terms) agg;

					//				Collection<Terms.Bucket> buckets = terms.getBuckets();
					//				assertThat(buckets.size(), equalTo(3));

					if (f.getBuckets().size() > 0)
					{
						FilterNode parent = new FilterNode();
						parent.setId(f.getName());
						parent.setName(f.getName());
						PropertyDetail detail = getPropertyDetailsArchive().getPropertyDetails(inSearchType).getDetail(f.getName());
						if (detail != null)
						{
							parent.setValue("name", detail.getElementData().getLanguageMap("name"));
						}
						for (Iterator iterator2 = f.getBuckets().iterator(); iterator2.hasNext();)
						{

							//	org.elasticsearch.search.aggregations.bucket.terms.StringTerms.Bucket entry = (org.elasticsearch.search.aggregations.bucket.terms.StringTerms.Bucket) iterator2.next();
							org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket entry = (org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket) iterator2.next();

							long count = entry.getDocCount();
							String term = entry.getKeyAsString();
							//log.info("term " + parent.getName()  + " \\" + term);
							FilterNode child = new FilterNode();
							child.setId(term);
							child.setPropertyDetail(detail);
							if (detail != null && detail.isList())
							{
								Data data = getSearcherManager().getData(detail.getListCatalogId(), detail.getListId(), term);
								if (data != null)
								{
									//child.setName(data.getName());

									child.setProperties(data.getProperties());
								}
								else
								{
									//child.setName(term);
									continue;
								}
							}
							else
							{
								child.setName(term);

							}

							child.setProperty("count", String.valueOf(count));
							parent.addChild(child);
						}
						topfacets.add(parent);
					}
				}
			}
		}
		getCacheManager().put("facethits" + inSearchType, inQuery.getMainInput(), topfacets);

		return topfacets;
	}

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
