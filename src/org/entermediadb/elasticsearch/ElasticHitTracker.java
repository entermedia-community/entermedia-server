package org.entermediadb.elasticsearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.filters.Filters.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.PropertyDetail;
import org.openedit.hittracker.FilterNode;
import org.openedit.hittracker.HitTracker;

public class ElasticHitTracker extends HitTracker
{
	private static final Log log = LogFactory.getLog(ElasticHitTracker.class);

	protected SearchRequestBuilder fieldSearcheRequestBuilder;
	protected Map fieldChunks;
	protected int fieldHitsPerChunk = 40;
	protected QueryBuilder terms;
	//protected List fieldFilterOptions;

	public QueryBuilder getTerms()
	{
		return terms;
	}

	public void setTerms(QueryBuilder inTerms)
	{
		terms = inTerms;
	}

	public SearchRequestBuilder getSearcheRequestBuilder()
	{
		return fieldSearcheRequestBuilder;
	}

	public void setSearcheRequestBuilder(SearchRequestBuilder inSearcheRequestBuilder)
	{
		fieldSearcheRequestBuilder = inSearcheRequestBuilder;
	}

	public ElasticHitTracker()
	{

	}

	public ElasticHitTracker(SearchRequestBuilder builder, QueryBuilder inTerms)
	{
		setTerms(inTerms);
		setSearcheRequestBuilder(builder);
	}

	public void setShowOnlySelected(boolean inShowOnlySelected)
	{
		fieldShowOnlySelected = inShowOnlySelected;
		
		getChunks().clear();
	}

	public SearchResponse getSearchResponse(int inChunk)
	{
		Integer chunk = Integer.valueOf(inChunk);
		SearchResponse response = (SearchResponse) getChunks().get(chunk);
		if (response == null)
		{
			int start = (inChunk) * fieldHitsPerChunk;
			int end = (inChunk + 1) * fieldHitsPerChunk;

			getSearcheRequestBuilder().setFrom(start).setSize(end).setExplain(false);
			if(getSearchQuery().hasFilters())
			{
				BoolQueryBuilder bool = QueryBuilders.boolQuery();
				bool.must(getTerms());
				for (Iterator iterator = getSearchQuery().getFilters().iterator(); iterator.hasNext();)
				{
					FilterNode node = (FilterNode) iterator.next();
					QueryBuilder term = QueryBuilders.matchQuery(node.getId(), node.get("value"));
					bool.must(term);
				}
				getSearcheRequestBuilder().setQuery(bool);
			}
			
			if (isShowOnlySelected() && fieldSelections != null && fieldSelections.size() > 0)
			{
				String[] fieldSelected = (String[])fieldSelections.toArray(new String[fieldSelections.size()]);
				QueryBuilder built = QueryBuilders.idsQuery(fieldSelected);
				//FilterBuilder filter = FilterBuilders.idsFilter().ids(fieldSelected);
				//andFilter.add(filter);
				getSearcheRequestBuilder().setPostFilter(built);
			}
			else
			{
				getSearcheRequestBuilder().setPostFilter((QueryBuilder)null);
			}
			
			response = getSearcheRequestBuilder().execute().actionGet();
			
			if (getChunks().size() > 30)
			{
				SearchResponse first = getChunks().get(0);
				getChunks().clear();
				getChunks().put(0, first);
				log.info("Reloaded chunks " + start + " to " + end);
			}
			getChunks().put(chunk, response);
		}
		return response;
	}

	protected Map<Integer, SearchResponse> getChunks()
	{
		if (fieldChunks == null)
		{
			fieldChunks = new HashMap();
		}
		return fieldChunks;
	}

	@Override
	public Data get(int inCount)
	{
		// Get the relative location based on the page we are on

		// ie 50 / 40 = 1
		int chunk = inCount / fieldHitsPerChunk;

		// 50 - (1 * 40) = 10 relative
		int indexlocation = inCount - (chunk * fieldHitsPerChunk);

		// get the chunk 1
		SearchResponse searchResponse = getSearchResponse(chunk);
		SearchHit[] hits = searchResponse.getHits().getHits();
		if (indexlocation >= hits.length)
		{
			// we dont support getting results beyond what we have loaded.
			// User should call setPage(page+1) first
			throw new OpenEditException("row request falls beyond one page of results");
		}
		SearchHit hit = hits[indexlocation];
		SearchHitData data = new SearchHitData(hit);
//		if (searchResponse.getVersion() > -1)
//		{
//			data.setProperty(".version", String.valueOf(searchResponse.getVersion()));
//		}

		return data;
	}

	@Override
	public Iterator iterator()
	{
		return new ElasticHitIterator(this);
	}

	@Override
	public boolean contains(Object inHit)
	{
		throw new OpenEditException("Not implemented");
	}

	public int size()
	{

		if (!isAllSelected() && isShowOnlySelected() && (fieldSelections == null || fieldSelections.size() == 0))
		{
			return 0;
		}

		return (int) getSearchResponse(0).getHits().getTotalHits();
	}

	@Override
	protected List loadFacetsFromResults()  //parse em
	{
		List topfacets = new ArrayList(); 
		SearchResponse response = getSearchResponse(0);
		//TODO: Should save the response and only load it if someone needs the data
		if (response.getAggregations() != null )
		{
			Aggregations facets = response.getAggregations();
			//log.info(facets);
			for (Iterator iterator = facets.iterator(); iterator.hasNext();)
			{
				Terms f = (Terms) iterator.next();
				
//				Collection<Terms.Bucket> buckets = terms.getBuckets();
//				assertThat(buckets.size(), equalTo(3));
				
				
				if (f.getBuckets().size() > 0)
				{
					FilterNode parent = new FilterNode();
					parent.setId(f.getName());
					parent.setName(f.getName());
					PropertyDetail detail = getSearcher().getDetail(f.getName());
					if (detail != null)
					{
						parent.setName(detail.getText());
					}
					for (Iterator iterator2 = f.getBuckets().iterator(); iterator2.hasNext();)
					{
						Bucket entry = (Bucket) iterator2.next();
						long count = entry.getDocCount();
						String term = entry.getKeyAsString();
						FilterNode child = new FilterNode();
						child.setId(term);
						if (detail.isList())
						{
							Data data = getSearcher().getSearcherManager().getData(getCatalogId(), detail.getListId(), term);
							if(data != null)
							{
								child.setName(data.getName());
							} else{
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
		return topfacets;

	}	
	
	public void invalidate()
	{
		setIndexId(getIndexId() + 1);
		fieldFilterOptions = null;		
	}
}
