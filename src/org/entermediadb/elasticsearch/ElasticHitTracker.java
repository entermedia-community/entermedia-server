package org.entermediadb.elasticsearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.InternalMultiBucketAggregation.InternalBucket;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.avg.Avg;
import org.elasticsearch.search.aggregations.metrics.sum.InternalSum;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;
import org.json.simple.parser.ParseException;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.FilterNode;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ElasticHitTracker extends HitTracker
{
	private static final Log log = LogFactory.getLog(ElasticHitTracker.class);
	protected SearchRequestBuilder fieldSearcheRequestBuilder;
	protected Map fieldChunks;
	protected int SCROLL_CACHE_TIME = 900000; //15 minutes
	protected long fieldLastPullTime = -1;
	protected String fieldLastScrollId;
	protected Client fieldElasticClient;
	protected int fieldLastPageLoaded;
	protected SearcherManager fieldSearcherManager;
	protected Aggregations fieldAggregations;

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}
	@Deprecated
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
	
	public ElasticHitTracker(Client inClient, SearchRequestBuilder builder, QueryBuilder inTerms, int inHitsPerPage)
	{
		setElasticClient(inClient);
		setTerms(inTerms);
		setSearcheRequestBuilder(builder);
		setHitsPerPage(inHitsPerPage);
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
		clear();
	}

	@Override
	public void clear()
	{
		getChunks().clear(); //This is called form cachedSearch
		fieldLastPullTime = -1;
		setLastPageLoaded(-100);
		fieldCurrentPage = null;
		fieldLastScrollId = null;
	}

	@Override
	public void refresh()
	{
		super.refresh();
		getChunks().clear(); //This is called form cachedSearch
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
					if (isUseServerCursor() && chunk > 1 && now > fieldLastPullTime + SCROLL_CACHE_TIME)
					{
						String error = getSearcher().getSearchType() + hashCode() + "expired " + now + ">" + fieldLastPullTime + "+SCROLL_CACHE_TIME";
						log.info(error);
						throw new OpenEditException(error);
					}
					
//					ElasticUserFilters filters = getUserFilters();
//					if(filters == null && (getSearchQuery().isFilter())) {
//						filters = (ElasticUserFilters) getSearcher().getSearcherManager().getModuleManager().getBean(getCatalogId(), "userFilters");
//						filters.addFacets(getSearchType(), getSearchQuery(), getSearcheRequestBuilder());
//						added = true;
//					
//					}
//					if(filters != null)
//					{
//						filters.addFacets(getSearchType(), getSearchQuery(), getSearcheRequestBuilder());
//						added = true;
//					}
					if (fieldLastPullTime == -1 && fieldActiveFilterValues == null)
					{
						applyFilters(); //this should only be done once
					}
					if (!isUseServerCursor() || fieldLastScrollId == null || chunk == 0) //todo: Allow scrolling for iterators
					{
						getSearcheRequestBuilder().setFrom(start).setSize(size).setExplain(false);
						
						if (isUseServerCursor())
						{
							getSearcheRequestBuilder().setScroll(new TimeValue(SCROLL_CACHE_TIME));
						}
						response = getSearcheRequestBuilder().execute().actionGet();
						setLastScrollId(response.getScrollId());
						//log.info(getSearcher().getSearchType() + hashCode() + " search chunk: " + inChunk + " start from:" +  start );
					}
					else
					{
						//Only call this if we are moving forward in the scroll
						//scroll to the right place if within timeout 
						//log.info(getSearchType() + " hash:" + hashCode() + " scrolling to chunk " + inChunk + " " + getHitsPerPage());
						response = getElasticClient().prepareSearchScroll(getLastScrollId()).setScroll(new TimeValue(SCROLL_CACHE_TIME)).execute().actionGet();
					}
					setLastPageLoaded(inChunk);
					fieldLastPullTime = now;
					
					if (getChunks().size() > 30)   //TODO: Keep the pages near us
					{
						SearchResponse first = getChunks().get(0);
						getChunks().clear();
						getChunks().put(0, first);
					}
					getChunks().put(chunk, response);
					
					if(fieldActiveFilterValues == null && response.getAggregations() != null ) 
					{
						setActiveFilterValues( loadValuesFromResults(response) ); //This will load the values
					    getSearcheRequestBuilder().setAggregations(new HashMap());  //this keeps is from loading the same values on page 2,3+ etc
					}
					else
					{
						if( fieldActiveFilterValues == null)
						{
							fieldActiveFilterValues = Collections.EMPTY_MAP;
						}
					    getSearcheRequestBuilder().setAggregations(new HashMap());
					}
				}
			}
		}
		return response;
	}
	
	
	public  Map<String,FilterNode> getActiveFilterValues()
	{
		if (fieldActiveFilterValues == null)
		{
			getSearchResponse(0);//This will cause the aggregations to be loaded if possible
		}
		if( fieldActiveFilterValues == Collections.EMPTY_MAP)
		{
			return null;
		}
		
		return fieldActiveFilterValues;
	}

	public  Map<String,FilterNode> loadValuesFromResults(SearchResponse response) //parse em
	{
		Map<String,FilterNode> options = new HashMap();
	
		//TODO: Should save the response and only load it if someone needs the data
		if (response.getAggregations() != null)
		{
			
			Aggregations facets = response.getAggregations();
			fieldAggregations = facets;
			for (Iterator iterator = facets.iterator(); iterator.hasNext();)
			{
				Object agg = iterator.next();
				if (agg instanceof Terms)
				{
					Terms f = (Terms) agg;

					//				Collection<Terms.Bucket> buckets = terms.getBuckets();
					//				assertThat(buckets.size(), equalTo(3));

					if (f.getBuckets().isEmpty())
					{
						continue;
					}
					FilterNode parent = new FilterNode();
					parent.setId(f.getName());
					parent.setName(f.getName());
					PropertyDetail detail = getSearcher().getDetail(f.getName());
					if (detail != null)
					{
						options.put(detail.getId(), parent);
						parent.setValue("name", detail.getElementData().getLanguageMap("name"));
						parent.setPropertyDetail(detail);
					}
					else
					{
					//	log.info("No such detail for " + f);
						continue;
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
				}
			}
		}
		return options;
	}
	
	@Override
	public int indexOfId(String inId)
	{
		if( inId == null || inId.startsWith("multiedit:") || inId.trim().isEmpty() )
		{
			return -1;
		}
		for (Iterator iterator = getChunks().keySet().iterator(); iterator.hasNext();)
		{
			Integer chunkindex = (Integer) iterator.next();
			
			int found = findIdOnPage(inId,chunkindex+1);
			if( found > -1)
			{
				return found;
			}
		}
	
		return super.indexOfId(inId);
		
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
		int chunkindex = inCount / getHitsPerPage();

		// 50 - (1 * 40) = 10 relative
		int indexlocation = inCount - (chunkindex * getHitsPerPage());

		// get the chunk 1
		SearchResponse searchResponse = getSearchResponse(chunkindex);
		SearchHit[] hits = searchResponse.getHits().getHits();
		if (indexlocation >= hits.length)
		{
			// we dont support getting results beyond what we have loaded.
			// User should call setPage(page+1) first
			throw new OpenEditException("row request falls beyond one page of results " + indexlocation + ">=" + hits.length);
		}
		SearchHit hit = hits[indexlocation];
		
		Searcher searcher = (Searcher)getSearcher();
		if( searcher == null && getSearcherManager() != null)
		{
			searcher = getSearcherManager().getSearcher(getCatalogId(), hit.getType());
		}
		SearchHitData data = new SearchHitData(hit, searcher);

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

	public double getSum(String inField, String inSummarizer)
	{
		double total = 0;
//		Map map = getAggregations().asMap();
//		map.get(inField);
		Terms terms = (Terms)getAggregations().get(inField);
		if( terms != null)
		{
			Collection<Terms.Bucket> buckets = terms.getBuckets(); 
		
			for(Terms.Bucket bucket : buckets)
			{
				Sum aggregation = bucket.getAggregations().get(inSummarizer);
				double count = aggregation.getValue();				
				total = total + count;
			}
		}
		return total;
	}
	

//	public double getAggregation(String inId)
//	{
//		SearchResponse response = getSearchResponse(0);
//		
//		Terms a = response.getAggregations().get(inId);
//		Collection <Terms.Bucket> a.getBuckets();
//		
//	}

	
	
	
	
	public double getAverage(String inField)
	{
		SearchResponse response = getSearchResponse(0);

		Avg aggregation = (Avg) response.getAggregations().get(inField + "_avg");
		return aggregation.getValue();
	}

	
	public List loadHistogram(String inField, boolean inReverse) //parse em
	{
		ArrayList topfacets = new ArrayList();
		Aggregations facets = getAggregations();
		
		//log.info(getSearchQuery().getFacets());
		//TODO: Should save the response and only load it if someone needs the data
		if (facets != null)
		{
			//log.info(response.toString());
				Object agg = facets.get(inField);
				if (agg instanceof Histogram)
				{
					Histogram f = (Histogram) agg;

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
							parent.setName(detail.getName());
						}
						for (Iterator iterator2 = f.getBuckets().iterator(); iterator2.hasNext();)
						{

							//	org.elasticsearch.search.aggregations.bucket.terms.StringTerms.Bucket entry = (org.elasticsearch.search.aggregations.bucket.terms.StringTerms.Bucket) iterator2.next();
							org.elasticsearch.search.aggregations.bucket.histogram.Histogram.Bucket entry = (org.elasticsearch.search.aggregations.bucket.histogram.Histogram.Bucket) iterator2.next();

							long count = entry.getDocCount();
							String term = entry.getKeyAsString();
							FilterNode child = new FilterNode();
							child.setId(term);
							child.setName(term);
							child.setProperty("count", String.valueOf(count));
							
							if(inReverse){
								parent.addChildToStart(child);
							} else{
								parent.addChild(child);
							}
						}
						topfacets.add(parent);				
					}
				}
			
		}
		
		return topfacets;

	}
	public List loadHistogram(String inField){
		return loadHistogram(inField, false);
	}
	
	/**
	 * This is a low level custom map of values. Not related to DataProperty fields
	 * @param inName
	 * @return
	 */
	public Map<String,Object> getAggregationMap(String inName){

		Map<String,Object> map = new HashMap();
		
		Aggregations agregations = getAggregations();
		if( agregations != null)
		{
			org.elasticsearch.search.aggregations.bucket.terms.Terms terms = agregations.get(inName);
			if( terms != null)
			{
				for (Iterator iterator = terms.getBuckets().iterator(); iterator.hasNext();)
				{
					InternalBucket bucket = (InternalBucket) iterator.next();
					//bucket.getAggregations()
					for (Iterator iterator2 = bucket.getAggregations().iterator(); iterator2.hasNext();)
					{
						Aggregation aggregation = (Aggregation) iterator2.next();
						
						//TODO: If there is one in there already, add a collection of values
						if( aggregation instanceof InternalSum)
						{
							map.put(bucket.getKeyAsString(),((InternalSum)aggregation).value());
						}
						else
						{
							//map.put(bucket.getKeyAsString(),aggregation.value());
						}
					}
				}
			}
		}
		
		return map;
	}
	public Aggregations getAggregations()
	{
		if( fieldAggregations == null)
		{
			SearchResponse response = getSearchResponse(0);
			fieldAggregations = response.getAggregations();
		}		
		return fieldAggregations;
	}
	
	public JsonObject getAggregationJson() throws ParseException {
		String json = getSearchResponse(0).toString();
		JsonParser parser = new JsonParser();
		JsonObject results = (JsonObject) parser.parse(json);

		JsonObject aggs = (JsonObject) results.get("aggregations");
		return aggs;
	}
	
		

	public void invalidate()
	{
		setIndexId(getIndexId() + 1);
		fieldActiveFilterValues = null;
	}

	public void applyFilters()
	{
		if (getSearchQuery().hasFilters())
		{
			BoolQueryBuilder bool = QueryBuilders.boolQuery();
			bool.must(getTerms());
			HashMap ors = new HashMap();
			Set<String> filterids = new HashSet();
			for (Iterator iterator = getSearchQuery().getFilters().iterator(); iterator.hasNext();)
			{
				FilterNode node = (FilterNode) iterator.next();
				filterids.add(node.getId());
			}
			
			for (Iterator iterator = filterids.iterator(); iterator.hasNext();)
			{
				String filterid = (String) iterator.next();
				ArrayList <FilterNode> nodes = getSearchQuery().getNodesForType(filterid);
				if(nodes.size() == 1) {
					FilterNode node = nodes.get(0);
					QueryBuilder term = QueryBuilders.matchQuery(filterid, node.get("value"));
					bool.must(term);
				} else if(nodes.size() >1) {
					List ids = new ArrayList();
					for (Iterator iterator2 = nodes.iterator(); iterator2.hasNext();)
					{
						FilterNode node = (FilterNode) iterator2.next();
						ids.add(node.get("value"));
					}
					QueryBuilder term =QueryBuilders.termsQuery(filterid, ids);
					bool.must(term);
				}
			}
			getSearcheRequestBuilder().setQuery(bool);
		}
		Collection<String> validids = new HashSet<String>();
		
		if (isShowOnlySelected() && fieldSelections != null && fieldSelections.size() > 0)
		{
			validids.addAll(fieldSelections);
			validids = findCommonIds(validids,getSearchQuery().getSecurityIds());
		}
		else
		{
			validids = getSearchQuery().getSecurityIds();
		}
		
		if ( validids != null && !validids.isEmpty())
		{
			QueryBuilder fids = QueryBuilders.termsQuery("_id", validids);
			//			QueryBuilder built = QueryBuilders.idsQuery(fieldSelected);
			//			FilterBuilder filter = ;//FilterBuilders.idsFilter().ids(fieldSelected);
			//			andFilter.add(filter);
			getSearcheRequestBuilder().setPostFilter(fids);
		}
		else
		{
			getSearcheRequestBuilder().setPostFilter((QueryBuilder) null);
		}

	}

	protected Collection<String> findCommonIds(Collection<String> validids, Collection<String> andids)
	{
		if( andids == null || andids.size() == 0)
		{
			return validids;
		}
		Collection<String> both = new HashSet<String>();
		for(String id : andids)
		{
			if( validids.contains(id))
			{
				both.add(id);
			}
		}
		return both;
	}
	
	
	
	@Override
	protected void finalize() throws Throwable {
		if(fieldLastScrollId != null){
			clearScroll(fieldLastScrollId);
		}
	}
	
	
	protected void clearScroll(String inScrollId){
		ClearScrollRequest request = new ClearScrollRequest();
		request.addScrollId(fieldLastScrollId);
			
		getElasticClient().clearScroll(request);
		
		
	}

	public HitTracker copy()
	{
		SearchQuery q = getSearchQuery().copy();
		ElasticHitTracker selecteddata = (ElasticHitTracker)getSearcher().search(q);
		//selecteddata.fieldChunks = fieldChunks;
		selecteddata.setPage( fieldPage );
		selecteddata.setHitsPerPage(getHitsPerPage());
		return selecteddata;
	}

//	public String highlight(Object inHit, String inField, int size)
//	{
//		if( inHit instanceof SearchHitData )
//		{
//			StringBuffer out = new StringBuffer();
//			SearchHitData data = (SearchHitData)inHit;
//			List rows = data.getHighlights(inField);
//			if( rows != null)
//			{
//				for (Iterator iterator = rows.iterator(); iterator.hasNext();)
//				{
//					String row = (String) iterator.next();
//					out.append(row);
//					out.append(" ");
//				}
//			}
//			return out.toString(); //TODO: Clean up
//		}
//}
	
}
