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
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregations;
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
	protected int SCROLL_CACHE_TIME = 60000; //millis
	protected long fieldLastPullTime = -1;
	protected String fieldLastScrollId;
	protected Client fieldElasticClient;
	protected int fieldLastPageLoaded;
	
	public int getLastPageLoaded()
	{
		return fieldLastPageLoaded;
	}

	public void setLastPageLoaded(int inLastPageLoaded)
	{
		fieldLastPageLoaded = inLastPageLoaded;
	}

	public ElasticHitTracker()
	{

	}

	public ElasticHitTracker(Client inClient, SearchRequestBuilder builder, QueryBuilder inTerms)
	{
		setElasticClient(inClient);
		setTerms(inTerms);
		setSearcheRequestBuilder(builder);
		setHitsPerPage(50);
	}


	public Client getElasticClient()
	{
		return fieldElasticClient;
	}

	public void setElasticClient(Client inElasticClient)
	{
		fieldElasticClient = inElasticClient;
	}

	public String getLastScrollId()
	{
		return fieldLastScrollId;
	}

	public void setLastScrollId(String inLastScrollId)
	{
		fieldLastScrollId = inLastScrollId;
	}

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

	public void setShowOnlySelected(boolean inShowOnlySelected)
	{
		fieldShowOnlySelected = inShowOnlySelected;
		
		getChunks().clear();
	}
	
	@Override
	public void clear()
	{
		getChunks().clear();  //This is called form cachedSearch
		fieldLastPullTime = -1;
		setLastPageLoaded(-100);
		fieldCurrentPage = null;
		fieldLastScrollId = null;
	}
	
	@Override
	public void refresh()
	{
		super.refresh();
		getChunks().clear();  //This is called form cachedSearch
		fieldLastPullTime = -1;
		setLastPageLoaded(-100);
		fieldCurrentPage = null;
		//log.info(getSearcher().getSearchType() + hashCode() + " clear chunks");
	}
	
	public SearchResponse getSearchResponse(int inChunk)
	{
		Integer chunk = Integer.valueOf(inChunk);
		SearchResponse response = (SearchResponse) getChunks().get(chunk);
		if (response == null)
		{
			synchronized (getChunks())
			{
				response = (SearchResponse) getChunks().get(chunk);
				if (response == null)
				{
					int start = (inChunk) * getHitsPerPage();
					//int size = (inChunk + 1) * getHitsPerPage();
					int size = getHitsPerPage();
					long now = System.currentTimeMillis();
					//If its not the next one in the list, or it has expired then run it again
					if( isUseServerCursor() && chunk > 1 &&  now > fieldLastPullTime + SCROLL_CACHE_TIME )
					{
						String error = getSearcher().getSearchType()+ hashCode() + "expired " + now + ">" + fieldLastPullTime + "+SCROLL_CACHE_TIME";
						log.info(error);
						throw new OpenEditException(error);
					}	
					if( !isUseServerCursor() || fieldLastScrollId == null || chunk == 0 ) //todo: Allow scrolling for iterators
					{
						if( fieldLastPullTime == -1)
						{
							refreshFilters(); //This seems like it should only be done once?
						}
						getSearcheRequestBuilder().setFrom(start).setSize(size).setExplain(false);
						if( isUseServerCursor() )
						{
							getSearcheRequestBuilder().setScroll(new TimeValue(SCROLL_CACHE_TIME) );
						}
						response = getSearcheRequestBuilder().execute().actionGet();
						setLastScrollId(response.getScrollId());
						//log.info(getSearcher().getSearchType() + hashCode() + " search chunk: " + inChunk + " start from:" +  start );
					}
					else
					{
						//Only call this if we are moving forward in the scroll
						//scroll to the right place if within timeout 
						log.info(getSearcher().getSearchType() + " hash:" + hashCode() + " scrolling to chunk " + inChunk + " " + getLastScrollId());
						response = getElasticClient().prepareSearchScroll(getLastScrollId()).setScroll(new TimeValue(SCROLL_CACHE_TIME)).execute().actionGet();
					}
					setLastPageLoaded(inChunk);
					fieldLastPullTime = now;
		
					if (getChunks().size() > 30)
					{
						SearchResponse first = getChunks().get(0);
						getChunks().clear();
						getChunks().put(0, first);
					}
					getChunks().put(chunk, response);
				}	
			}	
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
		int chunk = inCount / getHitsPerPage();

		// 50 - (1 * 40) = 10 relative
		int indexlocation = inCount - (chunk * getHitsPerPage());

		// get the chunk 1
		SearchResponse searchResponse = getSearchResponse(chunk);
		SearchHit[] hits = searchResponse.getHits().getHits();
		if (indexlocation >= hits.length)
		{
			// we dont support getting results beyond what we have loaded.
			// User should call setPage(page+1) first
			throw new OpenEditException("row request falls beyond one page of results " + indexlocation + ">=" + hits.length);
		}
		SearchHit hit = hits[indexlocation];
		SearchHitData data = new SearchHitData(hit, getSearcher().getPropertyDetails() );
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
						
					//	org.elasticsearch.search.aggregations.bucket.terms.StringTerms.Bucket entry = (org.elasticsearch.search.aggregations.bucket.terms.StringTerms.Bucket) iterator2.next();
						org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket entry = (org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket) iterator2.next();
						
						
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
	public void refreshFilters()
	{
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
			BoolQueryBuilder bool = QueryBuilders.boolQuery();
//			for (int i = 0; i < fieldSelected.length; i++)
//			{
//				bool.filter(QueryBuilders.termQuery("_id",fieldSelected[i]));
//			}
			QueryBuilder ids = QueryBuilders.termsQuery("_id", fieldSelections);
//			QueryBuilder built = QueryBuilders.idsQuery(fieldSelected);
//			
//			FilterBuilder filter = ;//FilterBuilders.idsFilter().ids(fieldSelected);
//			andFilter.add(filter);
			getSearcheRequestBuilder().setPostFilter(ids);
		}
		else
		{
			getSearcheRequestBuilder().setPostFilter((QueryBuilder)null);
		}

	}
}
