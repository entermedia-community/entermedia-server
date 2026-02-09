package org.entermediadb.elasticsearch.searchers;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsRequest;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.flush.FlushResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.geo.GeoDistance;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.GeoDistanceQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.PrefixQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.avg.AvgBuilder;
import org.elasticsearch.search.aggregations.metrics.sum.SumBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.RemoteTransportException;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.cluster.IdManager;
import org.entermediadb.asset.xmldb.CategorySearcher;
import org.entermediadb.data.FullTextLoader;
import org.entermediadb.elasticsearch.ElasticHitTracker;
import org.entermediadb.elasticsearch.ElasticNodeManager;
import org.entermediadb.elasticsearch.ElasticSearchQuery;
import org.entermediadb.elasticsearch.SearchHitData;
import org.entermediadb.location.Position;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.BaseSearcher;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.SearchData;
import org.openedit.data.Searcher;
import org.openedit.hittracker.ChildFilter;
import org.openedit.hittracker.GeoFilter;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.hittracker.Term;
import org.openedit.modules.translations.LanguageMap;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.IntCounter;
import org.openedit.util.JSONParser;
import org.openedit.util.OutputFiller;
import org.openedit.util.Replacer;
import org.openedit.xml.XmlSearcher;

import groovy.json.JsonOutput;
import groovy.json.JsonSlurper;

public class BaseElasticSearcher extends BaseSearcher implements FullTextLoader
{

	private static final Log log = LogFactory.getLog(BaseElasticSearcher.class);
	public static final Pattern VALUEDELMITER = Pattern.compile("\\s*\\|\\s*");
	protected static final Pattern operators = Pattern.compile("(\\sAND\\s|\\sOR\\s|\\sNOT\\s)");
	protected static final Pattern andoperators = Pattern.compile("(\\sAND\\s)");
	public static final Pattern TOKENS = Pattern.compile("[^a-zA-Z\\d\\s]");

	protected static final Pattern separatorchars = Pattern.compile("([0-9a-zA-Z]+)");
	protected static final Pattern orpattern = Pattern.compile("(.*?)\\b(AND|OR|NOT)\\b\\s+", Pattern.CASE_INSENSITIVE);
	protected static final Pattern spacepattern = Pattern.compile("([\\s]+)");

	protected ElasticNodeManager fieldElasticNodeManager;
	// protected IntCounter fieldIntCounter;
	// protected PageManager fieldPageManager;
	// protected LockManager fieldLockManager;
	protected boolean fieldAutoIncrementId;
	protected boolean fieldReIndexing;
	protected boolean fieldCheckVersions;
	protected boolean fieldRefreshSaves = true;
	protected boolean fieldCheckLegacy = true;
	protected long fieldIndexId = System.currentTimeMillis();
	protected ArrayList<String> fieldSearchTypes;
	protected boolean fieldIncludeFullText = true;
	protected OutputFiller fieldFiller;
	protected PageManager fieldPageManager;
	protected Replacer fieldReplacer;

	
	
	public boolean isCheckLegacy()
	{
		return fieldCheckLegacy;
	}

	public void setCheckLegacy(boolean inCheckLegacy)
	{
		fieldCheckLegacy = inCheckLegacy;
	}

	protected Replacer getReplacer()
	{
		if (fieldReplacer == null)
		{
			fieldReplacer = (Replacer) getModuleManager().getBean(getCatalogId(), "replacer");
		}

		return fieldReplacer;
	}

	protected void setReplacer(Replacer inReplacer)
	{
		fieldReplacer = inReplacer;
	}

	protected boolean fieldOptimizeReindex = false;

	public boolean isOptimizeReindex()
	{
		return fieldOptimizeReindex;
	}

	public void setOptimizeReindex(boolean inOptimizeReindex)
	{
		fieldOptimizeReindex = inOptimizeReindex;
	}

	public PageManager getPageManager()
	{
		if (fieldPageManager == null)
		{
			fieldPageManager = (PageManager) getModuleManager().getBean("pageManager");
		}
		return fieldPageManager;
	}

	public void setPageManager(PageManager pageManager)
	{
		fieldPageManager = pageManager;
	}

	protected OutputFiller getFiller()
	{
		if (fieldFiller == null)
		{
			fieldFiller = new OutputFiller();
		}
		return fieldFiller;
	}

	protected int fieldFullTextCap = 25000;

	public int getFullTextCap()
	{
		return fieldFullTextCap;
	}

	public void setFullTextCap(int inFullTextCap)
	{
		fieldFullTextCap = inFullTextCap;
	}

	public boolean isIncludeFullText()
	{
		return fieldIncludeFullText;
	}

	public void setIncludeFullText(boolean inIncludeFullText)
	{
		fieldIncludeFullText = inIncludeFullText;
	}

	public ArrayList<String> getSearchTypes()
	{
		if (fieldSearchTypes == null)
		{
			fieldSearchTypes = new ArrayList();

		}

		return fieldSearchTypes;
	}

	public void setSearchTypes(ArrayList<String> inSearchTypes)
	{
		fieldSearchTypes = inSearchTypes;
	}

	public boolean isRefreshSaves()
	{
		return fieldRefreshSaves;
	}

	public void setRefreshSaves(boolean inRefreshSaves)
	{
		fieldRefreshSaves = inRefreshSaves;
	}

	public ElasticNodeManager getElasticNodeManager()
	{
		return fieldElasticNodeManager;
	}

	public void setElasticNodeManager(ElasticNodeManager inElasticNodeManager)
	{
		fieldElasticNodeManager = inElasticNodeManager;
	}

	public boolean isCheckVersions()
	{
		return fieldCheckVersions;
	}

	public void setCheckVersions(boolean inCheckVersions)
	{
		fieldCheckVersions = inCheckVersions;
	}

	public boolean isReIndexing()
	{
		return fieldReIndexing;
	}

	public void setReIndexing(boolean inReIndexing)
	{
		fieldReIndexing = inReIndexing;
	}

	/**
	 * @deprecated not used
	 * @return
	 */
	public boolean isAutoIncrementId()
	{
		return fieldAutoIncrementId;
	}

	public void setAutoIncrementId(boolean inAutoIncrementId)
	{
		fieldAutoIncrementId = inAutoIncrementId;
	}

	public SearchQuery createSearchQuery()
	{
		SearchQuery query = new ElasticSearchQuery();
		query.setPropertyDetails(getPropertyDetails());
		query.setCatalogId(getCatalogId());
		query.setResultType(getSearchType()); // a default
		query.setSearcherManager(getSearcherManager());
		return query;
	}

	protected Client getClient()
	{
		return getElasticNodeManager().getClient();
	}

	protected String toId(String inId)
	{
		String id = inId.replace('/', '_');
		return id;
	}

	public HitTracker search(SearchQuery inQuery)
	{
		// if (isReIndexing())
		// {
		// int timeout = 0;
		// while (isReIndexing())
		// {
		// try
		// {
		// Thread.sleep(250);
		// }
		// catch (InterruptedException ex)
		// {
		// log.error(ex);
		// }
		// timeout++;
		// if (timeout > 100)
		// {
		// throw new OpenEditException("timeout on search while reindexing" +
		// getSearchType());
		// }
		// }
		// }
		// Think this is lucene junk
		String json = null;
		try
		{
			if (!(inQuery instanceof ElasticSearchQuery))
			{
				throw new OpenEditException("Elastic search requires elastic query");
			}
			
			HitTracker lowlevel = checkForJson(inQuery);
			if( lowlevel != null)
			{
				return lowlevel;
			}
				
			
			long start = System.currentTimeMillis();
			boolean showSearchLogs = getSearcherManager().getShowSearchLogs(getCatalogId());

			SearchRequestBuilder search = getClient().prepareSearch(toId(getCatalogId()));
			search.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
			if (showSearchLogs)
			{
				search.setExplain(true);
				search.setRequestCache(false);

			}
			//search.setExplain(true); //Really?

			if (getPropertyDetails().getSearchTypes() != null)
			{
				search.setTypes(getPropertyDetails().getSearchTypes().split(","));
			}
			else
			{
				search.setTypes(getSearchType());
			}

			if (isCheckVersions())
			{
				search.setVersion(true);
			}

			BoolQueryBuilder terms = buildTerms(inQuery);

			//function_score
			
			
			if (!inQuery.isIncludeDeleted())
			{
				TermQueryBuilder deleted = QueryBuilders.termQuery("emrecordstatus.recorddeleted", true);
				terms.mustNot(deleted);
			}

			search.setQuery(terms);
			addSorts(inQuery, search);
			addFacets(inQuery, search);

			addSearcherTerms(inQuery, search);
			addHighlights(inQuery, search);
			search.setRequestCache(true);

			if (inQuery.getIncludeOnly() != null || inQuery.getExcludeFields() != null)
			{
				String[] includes = null;
				String[] excludescludes = null;
				if( inQuery.getIncludeOnly() != null && !inQuery.getIncludeOnly().isEmpty())
				{
					includes = (String[])inQuery.getIncludeOnly().toArray( new String[inQuery.getIncludeOnly().size()]);
				} 
				if (inQuery.getExcludeFields() != null && !inQuery.getExcludeFields().isEmpty())
				{
					excludescludes = (String[])inQuery.getExcludeFields().toArray( new String[inQuery.getExcludeFields().size()]);
				}
				search.setFetchSource(includes, excludescludes);
			} 
			else  if( !inQuery.isIncludeDescription() )
			{
				search.setFetchSource(null,"description");
			}
			ElasticHitTracker hits = new ElasticHitTracker(getClient(), search, terms, inQuery.getHitsPerPage());
			hits.setSearcherManager(getSearcherManager());
			hits.setIndexId(getIndexId());
			hits.setSearcher(this);
			hits.setSearchQuery(inQuery);
			if (showSearchLogs)
			{
				long size = hits.size(); // order is important
				json = search.toString();
				long end = System.currentTimeMillis() - start;
				log.info(toId(getCatalogId()) + "/" + getSearchType() + "/_search' -d '" + json + "' \n" + size + " hits in: " + (double) end / 1000D + " seconds]");
			}
			return hits;
		}
		catch (Exception ex)
		{
			if (json != null)
			{
				log.error("Could not query: " + toId(getCatalogId()) + "/" + getSearchType() + "/_search' -d '" + json + "' sort by " + inQuery.getSorts(), ex);
			}

			if (ex instanceof OpenEditException)
			{
				throw (OpenEditException) ex;
			}
			throw new OpenEditException(ex);
		}
	}

	private HitTracker checkForJson(SearchQuery inQuery)
	{
		for (Iterator iterator = inQuery.getTerms().iterator(); iterator.hasNext();)
		{
			Term term = (Term) iterator.next();
			if( term.getDetail() != null && term.getDetail().isDataType("stringvector") )
			{
				double[] d = (double[])term.getParameters().getValue("value");
				if( d != null)
				{
					JSONObject query = new JSONObject();
					
					JSONObject function_score = new JSONObject();
					function_score.put("boost_mode", "replace");
					Double min = (Double)term.getParameters().getValue("min_score");
					function_score.put("min_score", min);  
					
					query.put("function_score",function_score);
					
					//.setMinScore(minScore)
					
					JSONObject script_score = new JSONObject();
					script_score.put("lang", "knn");
					script_score.put("script", "binary_vector_score");
					function_score.put("script_score", script_score);
					
					JSONObject params = new JSONObject();
					params.put("cosine", true);
					params.put("field",  term.getDetail().getId() );
					//double[] d = { -0.09217305481433868d, 0.010635560378432274d, -0.02878434956073761d, 0.06988169997930527d};
					List<Double> list = Arrays.stream(d).boxed().collect(Collectors.toList());
					JSONArray vector = new JSONArray();
					vector.addAll(list);
					params.put("vector",vector);
					script_score.put("params",params);
					//log.info("req: " + response);
					JSONObject root = new JSONObject();
					root.put("query",query);
					String source =  root.toJSONString();
					
					//log.info(source);
					
					//SearchResponse searchResponse = getClient().prepareSearch(toId(getCatalogId())).setSize(3).setTypes(getSearchType()).setSource(source).get();
					SearchResponse searchResponse = getClient().prepareSearch(toId(getCatalogId())).setTypes(getSearchType()).setSource(source).get();
					//log.info("req: " + searchResponse);
					
					SearchHit[] hits = searchResponse.getHits().getHits();
					ListHitTracker tracker = new ListHitTracker();
					tracker.setSearchQuery(inQuery);
					for (int i = 0; i < hits.length; i++)
					{
						SearchHit hit = hits[i];
						SearchHitData data = new SearchHitData(hit, this);
						tracker.add(data);
					}
					
					return tracker;
					
//					SearchRequest searchRequest = new SearchRequest(toId(getCatalogId()) );
		//
//					//String testquery = "{'bool': {'must': [{'match_phrase': {'countryName': 'Spain'}}], 'must_not': [], 'should': []}}".replace("'","\"");
//					//QueryBuilder qb = QueryBuilders.wrapperQuery(testquery);
		//
//					// Create your base query
//					QueryBuilder baseQuery = QueryBuilders.matchQuery("field", "value");
		//
//					// Create a function score query
//					
//					ScriptScoreFunctionBuilder scriptbuilder = ScoreFunctionBuilders.scriptFunction("binary_vector_score");
//					scriptbuilder.
//					FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.functionScoreQuery(
//					    baseQuery,
//					    scriptbuilder // Example: multiply score by 2
//					).boostMode( "replace");
//					
//					
//					// Use the functionScoreQuery in your search request
//					SearchResponse response = getClient().prepareSearch(toId(getCatalogId()))
//						.setTypes(getSearchType())
//					    .setQuery(functionScoreQuery)
//					    .get();
//					log.info("req: " + response);
//					FunctionScoreQueryBuilder(matchQuery("party_id", "12"))
//					.add(termsFilter("course_cd",
//					
//					SearchSourceBuilder searchSourceBuilder1 = SearchSourceBuilder.searchSource();
//					searchSourceBuilder1.query(source);
//					
//					.source(SearchSourceBuilder.searchSsearcherource()
//		                    .query(new QueryStringQueryBuilder("foo").field("query")));
//					
//					searchRequest.source(searchSourceBuilder1);
//					searchRequest.types(getSearchType());
//					//SearchRequestBuilder search = getClient().prepareSearch(toId(getCatalogId()));
//					ActionFuture<SearchResponse> res = getClient().search(searchRequest);
//					SearchResponse scrollResp1 = res.actionGet();

			//		log.info("req: " + scrollResp1);
					//String query = "{"bool": {"must": [{"match_phrase": {"countryName": "Spain"}}], "must_not": [], "should": []}}";
//					QueryBuilder qb = QueryBuilders.wrapperQuery(source);
//					SearchSourceBuilder searchSourceBuilder1 = new SearchSourceBuilder();
//					searchSourceBuilder1.query(qb);
//					SearchRequest searchRequest = new SearchRequest("index_name");
//					searchRequest.source(searchSourceBuilder1);
//					SearchResponse scrollResp1 = client.search(searchRequest, RequestOptions.DEFAULT);
//					System.out.println(scrollResp1);
					
//					MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("nationality", "italian");
//					  SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
//					  searchSourceBuilder.query(matchQueryBuilder);
		//
//					  search.setSource(searchSourceBuilder);
//					  
					
//					SearchModule searchModule= new SearchModule(Settings.EMPTY, false, Collections.emptyList());
//					try (XContentParser parser = XContentFactory.xContent(XContentType.JSON).createParser(new NamedXContentRegistry(searchModule
//					            .getNamedXContents()), source)) {
//					    searchSourceBuilder.parseXContent(parser);
//					}
//					
//					log.info("searchby: " + search.toString());
					
				}
			}
		}
		
		

		/**


"function_score": {
      "boost_mode": "replace",
      "script_score": {
        "lang": "knn",
        "params": {
          "cosine": false,
          "field": "facedata",
          "vector": [
               -0.09217305481433868, 0.010635560378432274, -0.02878434956073761, 0.06988169997930527
             ]
        },
        "script": "binary_vector_score"
      }
    }

		 
		//https://stackoverflow.com/questions/36589645/how-to-use-elasticsearch-functionscore-query-using-java-api
		 final FunctionScoreQueryBuilder queryBuilder = new FunctionScoreQueryBuilder(inTerms);
		 
		 Script script = new Script();
		 
		 
	        final ScoreFunctionBuilder scoreFunctionBuilder = new ScriptScoreFunctionBuilder(script);
	        queryBuilder.add(scoreFunctionBuilder);
	        return queryBuilder;
	        
		ScriptScoreFunctionBuilder scoreFunction = ScoreFunctionBuilders
			    .scriptFunction("_score * doc['calc_feild'].value");
		
		scoreFunction.setWeight(fieldFullTextCap);
		//new FunctionScoreQueryBuilder(search).add(scoreFunction).boostMode("replace")

		FunctionScoreQueryBuilder fqBuilder = new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                ScoreFunctionBuilders.scriptFunction(format("doc['%s'].value", FIELD_COUNT))
        );

//		 QueryBuilders.functionScoreQuery(inTerms);
//		    fqBuilder.boostMode("replace");
//		    fqBuilder.scoreMode("script_score");
		    
		    
		    ScriptScoreFunctionBuilder scoreFunction = ScoreFunctionBuilders
		    	    .scriptFunction("binary_vector_score");
		    
		    new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                    inTerms,
                    ScoreFunctionBuilders.weightFactorFunction(3)
            );
		    /*
		    FunctionScoreQueryBuilder fqBuilder = new FunctionScoreQueryBuilder(inTerms, scoreFunction).boostMode("replace"));
		    
		    QueryBuilders.functionScoreQuery(
		    		inTerms,
                    new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                            new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                    QueryBuilders.matchQuery(FIELD_TYPE, fieldType),
                                    ScoreFunctionBuilders.weightFactorFunction(3.0F)
                            )
                    }), ScoreMode.None));
		    
		    FunctionScoreQueryBuilder.FilterFunctionBuilder[] filterFunctionBuilders = new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
	                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
	                        QueryBuilders.matchQuery(FIELD_TYPE, fieldType),
	                        ScoreFunctionBuilders.weightFactorFunction(3)
	                ),
	                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
	                        ScoreFunctionBuilders.scriptFunction(format("doc['%s'].value", FIELD_COUNT))
	                )
	        };
		    fqBuilder.add(sfb2);
		
		return inTerms;
		*/
		return null;
	}

	public void addHighlights(SearchQuery inQuery, SearchRequestBuilder search)
	{
		for (Iterator iterator = getPropertyDetails().iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();
			if (detail.isHighlight())
			{
				search.addHighlightedField(detail.getId(), 180);

			}
		}

	}

	/**
	 * This is the main way to enable agregations and added to the query They
	 * are then run in the hit tracker
	 * 
	 * @param inQuery
	 * @param inSearch
	 * @return
	 */

	public boolean addFacets(SearchQuery inQuery, SearchRequestBuilder inSearch)
	{
		Collection facets = inQuery.getFacets();
//		if( getSearchType().equals("modulesearch") )
//		{
//			log.info( getSearchType() + " Adding Facets for " + inQuery.toQuery() + " with "  + facets);
//		}
		if (facets == null || facets.isEmpty()) //We might want the real facets just in case
		{
			boolean added = false;
			if (inQuery.getAggregation() != null)
			{
				inSearch.addAggregation((AbstractAggregationBuilder) inQuery.getAggregation());
				added = true;
			}
			ElasticSearchQuery q = (ElasticSearchQuery) inQuery;
			if (q.getAggregationJson() != null)
			{
				inSearch.setAggregations(q.getAggregationJson().getBytes());
				added = true;
			}
			return added;
		}
		List added = new ArrayList();


		for (Iterator iterator = facets.iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();
			if (added.contains(detail.getId()))
			{
				continue;
			}
			if (detail.isDate())
			{
				//TODO: Is this slow? seems kinda like a waste of CPU Use Groovy
				//				DateHistogramBuilder builder = new DateHistogramBuilder(detail.getId() + "_breakdown_day");
				//				builder.field(detail.getId());
				//				builder.interval(DateHistogramInterval.DAY);
				//				builder.order(Order.KEY_DESC);
				//				//	String timezone = TimeZone.getDefault().getID();
				//				//		builder.timeZone(timezone);
				//				inSearch.addAggregation(builder);
				//
				//				builder = new DateHistogramBuilder(detail.getId() + "_breakdown_week");
				//				builder.field(detail.getId());
				//				//	builder.timeZone(timezone);
				//
				//				builder.interval(DateHistogramInterval.WEEK);
				//				builder.order(Order.COUNT_DESC);
				//
				//				inSearch.addAggregation(builder);
				continue;
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
				AggregationBuilder b = null;
				//				if (detail.isViewType("tageditor"))
				//				{
				//					//b = AggregationBuilders.terms(detail.getId()).field(detail.getId() + ".exact").size(100);
				//					b = AggregationBuilders.terms(detail.getId()).field(detail.getId()).size(100);
				//				}
				//				else
				//				{
				//
				b = AggregationBuilders.terms(detail.getId()).field(detail.getId()).size(50);
				//				}
				inSearch.addAggregation(b);
			}
			else
			{
				AggregationBuilder b = AggregationBuilders.terms(detail.getId()).field(detail.getId() + ".exact").size(50);
				inSearch.addAggregation(b);
			}
			added.add(detail.getId());

		}

		// For reports, we can pass in a custom aggregation from a script or
		// somewhere

		if (inQuery.getAggregation() != null)
		{
			
			AbstractAggregationBuilder builder = (AbstractAggregationBuilder) inQuery.getAggregation();
			inSearch.addAggregation(builder);
		}
		ElasticSearchQuery q = (ElasticSearchQuery) inQuery;
		if (q.getAggregationJson() != null)
		{
			inSearch.setAggregations(q.getAggregationJson().getBytes());
		}
		return true;
	}

	protected void addSearcherTerms(SearchQuery inQuery, SearchRequestBuilder inSearch)
	{
		//For custom overries

	}

	// protected void addQueryFilters(SearchQuery inQuery, QueryBuilder inTerms)
	// {
	//
	// BoolQueryBuilder andFilter = inTerms.bo
	//
	// for (Iterator iterator = inQuery.getFilters().iterator();
	// iterator.hasNext();)
	// {
	// FilterNode node = (FilterNode) iterator.next();
	//
	// QueryBuilder filter = QueryBuilders.termQuery(node.getId(),
	// node.get("value"));
	// andFilter.must(filter);
	// }
	// .
	// //return andFilter;
	//
	//
	// }
	//
	//

	@SuppressWarnings("rawtypes")

	@Override
	public boolean initialize()
	{
		try
		{
			boolean alreadyin = getClient().admin().indices().typesExists(new TypesExistsRequest(new String[] { getElasticIndexId() }, getSearchType())).actionGet().isExists();
			if (!alreadyin)
			{
				log.info("initi mapping " + getCatalogId() + "/" + getSearchType());
				putMappings();
			}
		}
		catch (Exception ex)
		{
			log.error("index could not be created ", ex);
			return false;
		}
		return true;
	}

	protected String getElasticIndexId()
	{
		String indexid = getAlternativeIndex();

		if (indexid == null)
		{
			indexid = toId(getCatalogId());
		}
		return indexid;
	}

	//
	// protected void deleteOldMapping()
	// {
	// log.info("Does not work");
	// }

	// AdminClient admin = getElasticNodeManager().getClient().admin();
	// String indexid = toId(getCatalogId());
	// //XContentBuilder source = buildMapping();
	//
	// //DeleteMappingRequest dreq =
	// Requests.deleteMappingRequest(indexid).types(getSearchType());
	// try
	// {
	// DeleteMappingResponse dpres =
	// admin.indices().deleteMapping(dreq).actionGet();
	// if (dpres.isAcknowledged())
	// {
	// log.info("Cleared out the mapping " + getSearchType() );
	// getClient().admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();
	// }
	// }
	// catch (Throwable ex)
	// {
	// log.error(ex);
	// }
	// }
	public boolean putMappings()
	{
		AdminClient admin = getElasticNodeManager().getClient().admin();

		String indexid = getElasticIndexId();

		List<PropertyDetails> dependson = getPropertyDetailsArchive().findChildTables();
		for (Iterator iterator = dependson.iterator(); iterator.hasNext();)
		{
			PropertyDetails details = (PropertyDetails) iterator.next();
			PropertyDetail parent = details.getDetail("_parent");
			if (parent.getListId().equals(getSearchType()))
			{
				Searcher child = getSearcherManager().getSearcher(getCatalogId(), details.getId());
				child.setAlternativeIndex(getAlternativeIndex());
				child.reloadSettings();
				child.setAlternativeIndex(null);
			}
		}

		XContentBuilder source = buildMapping();
		try
		{
			log.info(indexid + "/" + getSearchType() + "/_mapping' -d '" + source.string() + "'");
		}
		catch (IOException ex)
		{
			log.error(ex);
		}
		// GetMappingsRequest find = new
		// GetMappingsRequest().types(getSearchType());
		// GetMappingsResponse found =
		// admin.indices().getMappings(find).actionGet();
		// if( !found.isContextEmpty())
		try
		{
			putMapping(admin, indexid, source);
			admin.cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();

			// Remove error warning
			getElasticNodeManager().removeMappingError(getSearchType());
		}
		catch (Exception ex)
		{
			// https://www.elastic.co/guide/en/elasticsearch/guide/current/scan-scroll.html
			// https://github.com/jprante/elasticsearch-knapsack
			log.info("Could not put mapping over existing mapping on catalog: " + getCatalogId() + " Searchtype: " + getSearchType(), ex);
			getElasticNodeManager().addMappingError(getSearchType(), ex.getMessage());
			//throw new OpenEditException("Mapping was not saved " + getSearchType(),ex);
			return false;
			// you will need to export data");
		}
		return true;
		// try
		// {
		// //Save existing index values
		// HitTracker all = getAllHits();
		// //Export to csv file?
		//
		// DeleteMappingRequest dreq =
		// Requests.deleteMappingRequest(indexid).types(getSearchType());
		// DeleteMappingResponse dpres =
		// admin.indices().deleteMapping(dreq).actionGet();
		// if (dpres.isAcknowledged())
		// {
		// log.info("Cleared out the mapping " + getSearchType() );
		// getClient().admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();
		// putMapping(admin, indexid, source);
		// }
		// //save it all back
		// }
		// catch( Throwable ex)
		// {
		// log.info("failed to clear mapping before reloading ",ex);
		// }
	}

	public void putMapping(AdminClient admin, String indexid, XContentBuilder source)
	{
		PutMappingRequest req = Requests.putMappingRequest(indexid).updateAllTypes(true).type(getSearchType());
		req = req.source(source);

		req.validate();
		PutMappingResponse pres = admin.indices().putMapping(req).actionGet();

		if (pres.isAcknowledged())
		{
			// log.info("mapping applied " + getSearchType());
			//	admin.cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();
		}

	}

	public XContentBuilder buildMapping()
	{
		try
		{
			XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
			XContentBuilder jsonproperties = jsonBuilder.startObject().startObject(getSearchType());
			jsonproperties.field("date_detection", "false");

			//"_all" : {"enabled" : false},
			jsonproperties.startObject("_all").field("enabled", "false").endObject();

			jsonproperties = jsonproperties.startObject("properties");

			List props = getPropertyDetails().findIndexProperties();
			//List objectarrays = new ArrayList();
			if (props.size() == 0)
			{
				log.error("No fields defined for " + getSearchType());
			}
			// https://github.com/elasticsearch/elasticsearch/pull/606
			// https://gist.github.com/870714
			/*
			 * index.analysis.analyzer.lowercase_keyword.type=custom
			 * index.analysis.analyzer.lowercase_keyword.filter.0=lowercase
			 * index.analysis.analyzer.lowercase_keyword.tokenizer=keyword
			 */

			// Add in namesorted
			// if (getPropertyDetails().contains("name") &&
			// !getPropertyDetails().contains("namesorted"))
			// {
			// props = new ArrayList(props);
			// PropertyDetail detail = new PropertyDetail();
			// detail.setId("namesorted");
			// props.add(detail);
			// }

			//			jsonproperties = jsonproperties.startObject("mastereditclusterid");
			//			jsonproperties = jsonproperties.field("type", "string");
			//			jsonproperties = jsonproperties.field("index", "not_analyzed");
			//			jsonproperties = jsonproperties.field("include_in_all", "false");
			//			jsonproperties = jsonproperties.field("store", "false");
			//
			//			jsonproperties = jsonproperties.endObject();
			//
			//			jsonproperties = jsonproperties.startObject("recordmodificationdate");
			//			jsonproperties = jsonproperties.field("include_in_all", "false");
			//			jsonproperties = jsonproperties.field("type", "date");
			//			jsonproperties = jsonproperties.field("store", "true");
			//			jsonproperties = jsonproperties.endObject();

			jsonproperties = buildClusterSyncMappings(jsonproperties);

			for (Iterator i = props.iterator(); i.hasNext();)
			{
				PropertyDetail detail = (PropertyDetail) i.next();
				if (detail.isDeleted())
				{
					continue;
				}
				if (detail.getId() == null || "_id".equals(detail.getId()) || "id".equals(detail.getId()))
				{
					// jsonproperties = jsonproperties.startObject("_id");
					// jsonproperties = jsonproperties.field("index",
					// "not_analyzed");
					// jsonproperties = jsonproperties.field("type", "_id");
					// jsonproperties = jsonproperties.endObject();
					continue;
				}
				if ("_parent".equals(detail.getId()) || detail.getId().contains(".") || "emrecordstatus".equals(detail.getId()) || "recordmodificationdate".equals(detail.getId()) || "mastereditclusterid".equals(detail.getId()) || detail.getId().startsWith("_")) //TODO: Check search type instead?
				{
					continue;
				}
				
				
				
				if (detail.isMultiLanguage())
				{
					jsonproperties = jsonproperties.startObject(detail.getId() + "_int");
					jsonproperties = jsonproperties.field("type", "object");

					jsonproperties.startObject("properties");
					HitTracker languages = getSearcherManager().getList(getCatalogId(), "locale");
					for (Iterator iterator = languages.iterator(); iterator.hasNext();)
					{
						Data locale = (Data) iterator.next();
						String id = locale.getId();

						jsonproperties.startObject(id);
						String analyzer = locale.get("analyzer");
						jsonproperties.field("type", "string");
						if (detail.isAnalyzed())
						{
							jsonproperties = createExactEnabledField(detail, jsonproperties);

						}

						if (analyzer != null)
						{
							jsonproperties.field("analyzer", analyzer);
						}
						else
						{
							jsonproperties.field("analyzer", "lowersnowball");

						}
						jsonproperties = jsonproperties.field("index", "analyzed");
						jsonproperties.endObject();
					}
					jsonproperties.endObject();
					jsonproperties.endObject();

					jsonproperties = jsonproperties.startObject(detail.getId());
					jsonproperties = jsonproperties.field("type", "string");
					jsonproperties = createExactEnabledField(detail, jsonproperties);
					jsonproperties = jsonproperties.field("include_in_all", "false");
					jsonproperties = jsonproperties.endObject();

					continue;
				}

				jsonproperties = jsonproperties.startObject(detail.getId());
				configureDetail(detail, jsonproperties);
				jsonproperties = jsonproperties.endObject();
			}
			jsonproperties = jsonproperties.endObject();
			PropertyDetail _parent = getPropertyDetails().getDetail("_parent");
			if (_parent != null)
			{
				jsonproperties = jsonproperties.startObject("_parent");
				jsonproperties = jsonproperties.field("type", _parent.getListId());
				jsonproperties = jsonproperties.endObject();
			}
			jsonBuilder = jsonproperties.endObject();
			String content = jsonproperties.string();
			//	log.info(getSearchType() + " " + content);
			return jsonproperties;
		}
		catch (Throwable ex)
		{
			ex.printStackTrace();
			throw new OpenEditException(ex);
		}

	}

	private XContentBuilder buildClusterSyncMappings(XContentBuilder jsonproperties) throws Exception
	{

		//		What about an object called asset.recordstatus.deleted = true
		//				asset.recordstatus.lastmodified and asset.recordstatus.lastmodifiedclusterid and asset.recordstatus.masternodeid and asset.recordstatus.masterlastmodified (edited) 

		jsonproperties.startObject("emrecordstatus").field("type", "object");
		jsonproperties.startObject("properties");

		jsonproperties.startObject("mastereditclusterid").field("type", "string").field("index", "not_analyzed").field("include_in_all", "false").field("store", "false").endObject();
		jsonproperties.startObject("masterrecordmodificationdate").field("include_in_all", "false").field("type", "date").field("store", "true").endObject();

		jsonproperties.startObject("lastmodifiedclusterid").field("type", "string").field("index", "not_analyzed").field("include_in_all", "false").field("store", "false").endObject();
		jsonproperties.startObject("recordmodificationdate").field("include_in_all", "false").field("type", "date").field("store", "true").endObject();
		jsonproperties.startObject("recorddeleted").field("include_in_all", "false").field("type", "boolean").field("store", "false").endObject();

		jsonproperties.endObject();
		jsonproperties.endObject();

		return jsonproperties;
	}

	protected void configureDetail(PropertyDetail detail, XContentBuilder jsonproperties) throws Exception
	{

		if ("description".equals(detail.getId()))
		{
			String analyzer = "lowersnowball";
			jsonproperties = jsonproperties.field("analyzer", analyzer);
			jsonproperties = jsonproperties.field("type", "string");
			jsonproperties = jsonproperties.field("index", "analyzed");
			jsonproperties = jsonproperties.field("include_in_all", "false");
			return;
		}

		//CHECK TIMECODE
		if (detail.isDataType("objectarray") || detail.isDataType("object"))
		{
			jsonproperties = jsonproperties.field("type", "object");
			//"type": "nested",

			jsonproperties.startObject("properties");
			for (Iterator iterator = detail.getObjectDetails().iterator(); iterator.hasNext();)
			{
				PropertyDetail child = (PropertyDetail) iterator.next();
				jsonproperties = jsonproperties.startObject(child.getId());
				configureDetail(child, jsonproperties);
				jsonproperties = jsonproperties.endObject();
			}
			jsonproperties.endObject();

			return;

		}
		
		else if (detail.isDataType("nested"))
		{
			jsonproperties = jsonproperties.field("type", "nested");
			jsonproperties.startObject("properties");
			for (Iterator iterator = detail.getObjectDetails().iterator(); iterator.hasNext();)
			{
				PropertyDetail child = (PropertyDetail) iterator.next();
				jsonproperties = jsonproperties.startObject(child.getId());
				configureDetail(child, jsonproperties);
				jsonproperties = jsonproperties.endObject();
			}
			jsonproperties.endObject();

			return;

		}
		else if (detail.isDataType("stringvector"))
		{
			// "index" : "not_analyzed"
			jsonproperties = jsonproperties.field("type", "binary");
			jsonproperties = jsonproperties.field("doc_values", true);
			return;
		}

		// First determine type
		if (detail.isDate())
		{
			jsonproperties = jsonproperties.field("type", "date");
			jsonproperties = jsonproperties.field("store", "true");

			// "date_detection" : 0
			// jsonproperties = jsonproperties.field("format",
			// "yyyy-MM-dd HH:mm:ss Z");
		}
		else if (detail.isBoolean())
		{
			jsonproperties = jsonproperties.field("type", "boolean");
		}
		else if (detail.isDataType("number") || detail.isDataType("long"))
		{
			jsonproperties = jsonproperties.field("type", "long");
		}
		else if (detail.isDataType("float"))
		{
			jsonproperties = jsonproperties.field("type", "float");
		}
		else if (detail.isDataType("double"))
		{
			jsonproperties = jsonproperties.field("type", "double");
		}
		else if (detail.isDataType("geo_point"))
		{
			jsonproperties = jsonproperties.field("type", "geo_point");
		}

		else if (detail.isList()) // Or multi valued?
		{
			if (Boolean.parseBoolean(detail.get("nested")))
			{
				jsonproperties = jsonproperties.field("type", "nested");
			}
			else
			{
				jsonproperties = jsonproperties.field("type", "string");
			}
			//TODO: enable sort on list fields. if exact field is sortable add sort subfield with the actual lookedup name value?
		}
		else
		{
			jsonproperties = jsonproperties.field("type", "string");
			if (detail.isAnalyzed())
			{
				jsonproperties = createExactEnabledField(detail, jsonproperties);
			}

		}

		// Now determine index
		String indextype = detail.get("indextype");

		if (indextype == null)
		{
			if (!detail.isAnalyzed())
			{
				indextype = "not_analyzed";
			}
		}
		if (indextype != null)
		{
			jsonproperties = jsonproperties.field("index", indextype);
		}

		jsonproperties = jsonproperties.field("include_in_all", "false"); // Do
																			// not
																			// use.
																			// Use
																			// _description

		String analyzer = detail.get("analyzer");
		if (analyzer != null)
		{
			jsonproperties.field("analyzer", analyzer);
		}
		else
		{
			//			if (detail.isAnalyzed()) //&& !("name".equals(detail.getId()))) 
			//			{
			//				jsonproperties.field("analyzer", "lowersnowball");
			//			}
		}
	}

	protected XContentBuilder createExactEnabledField(PropertyDetail detail, XContentBuilder jsonproperties) throws IOException
	{
		jsonproperties.startObject("fields");
		jsonproperties.startObject("exact");
		jsonproperties = jsonproperties.field("type", "string");
		jsonproperties = jsonproperties.field("index", "not_analyzed");
		if (!detail.getId().contains("path"))
		{
			jsonproperties = jsonproperties.field("ignore_above", 256);
		}
		jsonproperties.endObject();

		jsonproperties.startObject("sort");
		jsonproperties = jsonproperties.field("type", "string");
		jsonproperties = jsonproperties.field("index", "analyzed");
		jsonproperties = jsonproperties.field("analyzer", "tags");
		jsonproperties = jsonproperties.field("ignore_above", 256);
		jsonproperties.endObject();

		jsonproperties.endObject();
		return jsonproperties;
	}

	public BoolQueryBuilder buildTerms(SearchQuery inQuery)
	{

		// if (inQuery.getTerms().size() == 1 && inQuery.getChildren().size() ==
		// 0 ) //Shortcut for common cases
		// {
		// Term term = (Term) inQuery.getTerms().iterator().next();
		//
		// if ("orgroup".equals(term.getOperation()) ||
		// "orsGroup".equals(term.getOperation())) //orsGroup?
		// {
		// return addOrsGroup(term);
		// }
		//
		// String value = term.getValue();
		//
		// if (value != null && value.equals("*"))
		// {
		// return QueryBuilders.matchAllQuery();
		// }
		// QueryBuilder find = buildTerm(term.getDetail(), term, value);
		// return find;
		// }
		BoolQueryBuilder bool = QueryBuilders.boolQuery();

		buildBoolTerm(inQuery, bool, inQuery.isAndTogether());

		// if( inQuery.isEndUserSearch() )
		// {
		// Collection properties =
		// getPropertyDetails().findAutoIncludeProperties();
		// for (Iterator iterator = properties.iterator(); iterator.hasNext();)
		// {
		// PropertyDetail detail = (PropertyDetail) iterator.next();
		// if( inQuery.getDetail(detail.getId()) == null ) //Not already
		// included
		// {
		// //QueryBuilder find = buildTerm(detail, term, value);
		//
		// bool.must(find);
		// }
		// }
		// }

		if (inQuery.getChildren().size() > 0)
		{
			for (Iterator iterator = inQuery.getChildren().iterator(); iterator.hasNext();)
			{
				SearchQuery query = (SearchQuery) iterator.next();
				QueryBuilder builder = buildTerms(query);
				if (inQuery.isAndTogether())
				{
					bool.must(builder);
				}
				else
				{
					bool.should(builder);
				}
			}
		}
		return bool;

	}

	protected void buildBoolTerm(SearchQuery inQuery, BoolQueryBuilder bool, boolean inAnd)
	{
		for (Iterator iterator = inQuery.getTerms().iterator(); iterator.hasNext();)
		{
			Term term = (Term) iterator.next();
			PropertyDetail detail = term.getDetail();
			//We handle joins with SearchQueryFilter.java
			String ignoretypes = inQuery.get("ignoresearchttype");
			if (ignoretypes == null || !Boolean.parseBoolean(ignoretypes))
			{
				if (detail.getSearchType() != null && !getSearchType().equals(detail.getSearchType()))
				{
					continue;
				}
			}
			Object value = term.getValue();
			if (value == null)
			{
				value = term.getValues();
			}

			QueryBuilder find = buildTerm(inQuery, detail, term, value);
			if (find != null)
			{
				if (inAnd)
				{
					bool.must(find);
				}
				else
				{
					bool.should(find);
				}
			}
		}
		// }
	}

	// protected BoolQueryBuilder addOrsGroup(Term term)
	// {
	// if (term.getValues() != null)
	// {
	// BoolQueryBuilder or = QueryBuilders.boolQuery();
	// for (int i = 0; i < term.getValues().length; i++)
	// {
	// Object val = term.getValues()[i];
	// if (val != null && !val.equals(""))
	// {
	// QueryBuilder aterm = buildTerm(term.getDetail(), term, val);
	// if (aterm != null)
	// {
	// or.should(aterm);
	// }
	// }
	// }
	// return or;
	// }
	// return null;
	// }

	protected QueryBuilder buildTerm(SearchQuery inQuery, PropertyDetail inDetail, Term inTerm, Object inValue)
	{

		QueryBuilder find = buildNewTerm(inQuery, inDetail, inTerm, inValue);

		if ("not".equals(inTerm.getOperation()) || "notgroup".equals(inTerm.getOperation()))
		{
			BoolQueryBuilder or = QueryBuilders.boolQuery();
			or.mustNot(find);
			return or;
		}
		else if (inDetail.getId().contains("."))
		{
			String[] ids = inDetail.getId().split("\\.");
			PropertyDetail parent = getDetail(ids[0]);
			if (parent != null && "nested".equals(parent.getDataType()))
			{
				find = QueryBuilders.nestedQuery(ids[0], find);
			}
			/*
			 * "nested": { "path": "faceprofiles", "query": { "bool": { "must":
			 * [ { "term": {
			 * "faceprofiles.faceprofilegroup":"AXM5Gn6zvm9C1jY32Xy5" } } ] } }
			 * }
			 */
		}

		return find;
	}

	protected QueryBuilder buildNewTerm(SearchQuery inQuery, PropertyDetail inDetail, Term inTerm, Object inValue)
	{
		// Check for quick date object
		QueryBuilder find = null;
		String valueof = null;
		Date valuedate = null;
		if (inValue instanceof Date)
		{
			valuedate = (Date) inValue;
			valueof = DateStorageUtil.getStorageUtil().formatForStorage((Date) inValue);
		}
		else
		{
			valueof = String.valueOf(inValue); //Value is never null
		}

		String fieldid = inDetail.getId();
		if (inDetail.isMultiLanguage())
		{
			if (!fieldid.endsWith("_int"))
			{
				fieldid = fieldid + "_int.en";//default to search the english
			}
		}

		if ("searchjoin".equals(inDetail.getDataType()))
		{
			// contact.state
			String fieldname = fieldid.substring(0, fieldid.indexOf(".")); // contact
			String path = fieldid.substring(fieldid.indexOf(".") + 1); // state
			// TermsLookupQueryBuilder joinquery =
			// QueryBuilders.termsLookupQuery(fieldname);
			// joinquery.lookupId(inTerm.getValue());
			// joinquery.lookupType(inDetail.getListId());
			// joinquery.lookupIndex(toId( inDetail.getListCatalogId()));
			// joinquery.lookupPath(path);
			// return joinquery;
			org.openedit.data.QueryBuilder builder = getSearcherManager().getSearcher(inDetail.getListCatalogId(), inDetail.getListId()).query();
			HitTracker hits = builder.match(path, valueof).search();

			hits.setHitsPerPage(1000);
			Collection ids = new ArrayList(hits.size());
			for (Iterator iterator = hits.iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				ids.add(data.getId());
			}
			if (ids.size() > 0)
			{
				find = QueryBuilders.termsQuery(fieldname, ids);
				return find;
			}
			else
			{
				return null;
			}
		}
		else if ("childfilter".equals(inTerm.getOperation()))
		{
			ChildFilter filter = (ChildFilter) inTerm;
			QueryBuilder parent = QueryBuilders.termQuery(filter.getChildColumn(), filter.getValue());
			QueryBuilder haschild = QueryBuilders.hasChildQuery(filter.getChildTable(), parent);
			return haschild;
		}

		// if( fieldid.equals("description"))
		// {
		// //fieldid = "_all";
		// //valueof = valueof.toLowerCase();
		// find = QueryBuilders.textQuery(fieldid, valueof);
		// return find;
		// }
		if (fieldid != null && fieldid.equals("id"))
		{
			// valueof = valueof.toLowerCase();
			if (valueof.equals("*"))
			{
				find = QueryBuilders.matchAllQuery();
			}
			else if (!"orgroup".equals(inTerm.getOperation()))
			{
				find = QueryBuilders.termQuery("_id", valueof);
			}
			else if (inTerm.getValues() != null)
			{
				find = QueryBuilders.termsQuery("_id", inTerm.getValues());
			}
			if (find != null)
			{
				return find;
			}
		}

		if (valueof.equals("*"))
		{
			//find = QueryBuilders.wildcardQuery(fieldid, "*");
			find = QueryBuilders.matchAllQuery();
			// ExistsFilterBuilder filter =
			// FilterBuilders.existsFilter(fieldid);
			// find = QueryBuilders.filteredQuery(all, filter);

		}

		else if ("contains".equals(inTerm.getOperation()))
		{
			// MatchQueryBuilder text = QueryBuilders.matchPhraseQuery(fieldid,
			// valueof);
			// QueryBuilder text = QueryBuilders.queryString("*" + valueof +
			// "*").field(fieldid);
			String wildcard = valueof;

			if (!wildcard.startsWith("*"))
			{
				wildcard = "*" + wildcard;
			}
			if (!wildcard.endsWith("*"))
			{
				wildcard = wildcard + "*";
			}
			wildcard = wildcard.toLowerCase(); // Some reason wildcard searches
												// are not run by the analyser
												// MatchQueryBuilder text = QueryBuilders.matchPhraseQuery(fieldid,
												// valueof);
			String altid = null;
			if (inDetail.isAnalyzed() && !inDetail.getId().equals("description"))
			{
				altid = fieldid + ".sort";
			}
			else
			{
				altid = fieldid;
			}

			WildcardQueryBuilder text = QueryBuilders.wildcardQuery(altid, wildcard);

			BoolQueryBuilder or = QueryBuilders.boolQuery();
			or.should(text);

			valueof = valueof.replace("*", "");
			MatchQueryBuilder phrase = QueryBuilders.matchPhrasePrefixQuery(altid, valueof);
			phrase.maxExpansions(75);
			or.should(phrase);
			find = or;
		}
		else if ("missing".equals(inTerm.getOperation()))
		{
			find = QueryBuilders.missingQuery(inTerm.getId());
		}
		else if ("exists".equals(inTerm.getOperation()))
		{
			find = QueryBuilders.existsQuery(inTerm.getId());
		}

		else if ("startswith".equals(inTerm.getOperation()))
		{
			//TODO: Should startswith be exact or analysed phrases? 
			//find = QueryBuilders.prefixQuery(fieldid, valueof);
			//Left this in for now...

			if (inDetail.isAnalyzed())
			{

				MatchQueryBuilder text = QueryBuilders.matchPhrasePrefixQuery(fieldid, valueof);
				text.maxExpansions(10);
				find = text;
			}
			else
			{
				PrefixQueryBuilder text = QueryBuilders.prefixQuery(fieldid, valueof);
				find = text;
			}
		}
//		else if ("freeform".equals(inTerm.getOperation()))
//		{
//			List fields = getKeywordProperties();
//			for (Iterator iterator = fields.iterator(); iterator.hasNext();)
//			{
//				PropertyDetail detail = (PropertyDetail) iterator.next();
//				
//			}
//		}
		else if ("freeform".equals(inTerm.getOperation()))
		{
			//Pattern pattern = Pattern.compile("(?<=\\s)\\w(?=\\s)");

			if ((valueof.startsWith("\"") && valueof.endsWith("\""))) //This seems wrong
			{
				Pattern pattern = Pattern.compile("(?<=\\s)[^a-zA-Z\\\\d\\\\s](?=\\s)");
				Matcher matcher = pattern.matcher(valueof);
				//String oldvalueof = valueof;
				valueof = matcher.replaceAll("");

				valueof = valueof.replace("\"", "");
				valueof = QueryParser.escape(valueof);
				String query = "+(" + valueof + ")";
				MatchQueryBuilder text = QueryBuilders.matchPhraseQuery(inTerm.getId(), query);
				text.analyzer("lowersnowball");
				find = text;
			}
			else
			{
				//String uppercase = valueof.replace(" and ", " AND ").replace(" And ", " AND ").replace(" Or ", " OR ").replace(" or ", " OR ").replace(" not ", " NOT ").replace(" to ", " TO ");//.replace(", ", " AND "); //Babson uses lots of commas
				//We no longer allow + or - notation
				// Parse by Operator
				// Add wildcards
				// Look for Quotes

				//				Matcher customlogic = operators.matcher(uppercase);
				//				if (!customlogic.find()) //This somehow ignores things in " " .. ie. "Some things" Cool
				//				{
				//					uppercase = uppercase.replaceAll(" ", " AND "); //All spaces
				//				}
				// tom and nancy == *tom* AND *nancy*
				// tom or nancy == *tom* OR *nancy*
				// tom nancy => *tom* AND *nancy*
				// tom*nancy => tom*nancy
				// tom AND "Nancy Druew" => *tom* AND "Nancy Druew"
				// "Big Deal" => "Big Deal"
				//valueof = valueof.replace(" and ", " AND ").replace(" or ", " OR ").replace(" not ", " NOT ").replace(" to ", " TO "); // Why do this again?

				
				//..protected static final Pattern orpattern = Pattern.compile("(.*?)\\s+(OR?|AND?|NOT?)+");
				
				
				Matcher andors = orpattern.matcher(valueof);

				Collection searchpairs = new ArrayList();

				//String regex = "(.*?)\\s+(AND|OR)\\s)+";
				BoolQueryBuilder booleans = QueryBuilders.boolQuery();
				int lastterm = 0;
				while (andors.find())
				{
					// Get the matched character
					Map pair = new HashMap();
					pair.put("word", andors.group(1));
					pair.put("operator", andors.group(2));
					searchpairs.add(pair);
					lastterm = andors.end();
				}
				Map lastpair = new HashMap();
				lastpair.put("word", valueof.substring(lastterm));
				searchpairs.add(lastpair);

				String currentoperator = null;

				for (Iterator iterator = searchpairs.iterator(); iterator.hasNext();)
				{
					Map<String, String> pair = (Map) iterator.next();
					String orword = pair.get("word");
					//If there are tokens then treat a one word with quotes
					//Check for quotes..
					// Create a Matcher object
					Matcher matcher = separatorchars.matcher(orword);
					currentoperator = pair.get("operator");
					String previousoperator = null;

					//Bill_Clinton_official.jpg  break it down =  Bill_Clinton_official AND jpg
					while (matcher.find()) //sEPARATE ON spaces and weird characters
					{
						// Get the matched character
						String partialword = matcher.group();
						previousoperator = currentoperator;
						boolean lastword = orword.endsWith(partialword);
						String nextoperator = addSearchTerms(inTerm, lastword, partialword, previousoperator, currentoperator, booleans);
						currentoperator = nextoperator;
					}
				}

				find = booleans;
			}
		}
		else if (valueof.endsWith("*"))
		{
			valueof = valueof.substring(0, valueof.length() - 1);

			MatchQueryBuilder text = QueryBuilders.matchPhrasePrefixQuery(fieldid, valueof);
			text.maxExpansions(10);
			find = text;
		}
		else if (valueof.contains("*"))
		{
			if (inDetail.isAnalyzed())
			{
				find = QueryBuilders.wildcardQuery(fieldid + ".sort", valueof);
			}
			else
			{
				find = QueryBuilders.wildcardQuery(fieldid, valueof);
			}
		}
		else if (inDetail.isBoolean())
		{
			find = QueryBuilders.termQuery(fieldid, Boolean.parseBoolean(valueof));
		}

		else if (inDetail.isDate())
		{
			if ("beforedate".equals(inTerm.getOperation()))
			{
				// Date after = new Date(0);
				Date before = DateStorageUtil.getStorageUtil().parseFromStorage(valueof);
				Calendar c = new GregorianCalendar();

				c.setTime(before);
				c.set(Calendar.HOUR_OF_DAY, 23);
				c.set(Calendar.MINUTE, 59);
				c.set(Calendar.SECOND, 59);
				c.set(Calendar.MILLISECOND, 999);
				before = c.getTime();

				find = QueryBuilders.rangeQuery(inDetail.getId()).to(before);

			}
			else if ("afterdate".equals(inTerm.getOperation()))
			{
				Date after = DateStorageUtil.getStorageUtil().parseFromStorage(valueof);
				find = QueryBuilders.rangeQuery(fieldid).from(after);// .to(before);
			}
			else if ("betweendates".equals(inTerm.getOperation()))
			{
				// String end =
				// DateStorageUtil.getStorageUtil().formatForStorage(new
				// Date(Long.MAX_VALUE));
				Date before = (Date) inTerm.getValue("beforeDate");
				Date after = (Date) inTerm.getValue("afterDate");

				// inTerm.getParameter("beforeDate");

				// String before
				//TODO: Use gte ?
				find = QueryBuilders.rangeQuery(fieldid).from(after).to(before).includeUpper(true).includeLower(true);
			}
			else if ("ondate".equals(inTerm.getOperation()))
			{
				Date target = DateStorageUtil.getStorageUtil().parseFromStorage(valueof);

				Calendar c = new GregorianCalendar();
				c.setTime(target);
				c.set(Calendar.HOUR_OF_DAY, 0);
				c.set(Calendar.MINUTE, 0);
				c.set(Calendar.SECOND, 0);
				c.set(Calendar.MILLISECOND, 0);
				Date fromtime = c.getTime();

				c.set(Calendar.HOUR_OF_DAY, 23);
				c.set(Calendar.MINUTE, 59);
				c.set(Calendar.SECOND, 59);
				c.set(Calendar.MILLISECOND, 999);

				// inTerm.getParameter("beforeDate");

				// String before
				find = QueryBuilders.rangeQuery(fieldid).includeLower(true).includeLower(true).from(fromtime).to(c.getTime()).includeUpper(true).includeLower(true);
			}
			else
			{
				// Think this doesn't ever run. I think we use betweendates.
				Date target = DateStorageUtil.getStorageUtil().parseFromStorage(valueof);
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(target);
				int year = calendar.get(Calendar.YEAR);
				int month = calendar.get(Calendar.MONTH);
				int day = calendar.get(Calendar.DATE);
				calendar.set(Calendar.MILLISECOND, 0);
				calendar.set(year, month, day, 0, 0, 0);

				Date after = calendar.getTime();
				calendar.set(year, month, day, 23, 59, 59);
				calendar.set(Calendar.MILLISECOND, 999);

				Date before = calendar.getTime();

				find = QueryBuilders.rangeQuery(fieldid).from(after).to(before);

				// find = QueryBuilders.termQuery(fieldid, valueof); //TODO make
				// it a range query? from 0-24 hours
			}
			RangeQueryBuilder finalquery = (RangeQueryBuilder) find;
			if (inQuery.getTimeZone() != null)
			{
				finalquery.timeZone(inQuery.getTimeZone());
			}

		}
		else if (inDetail.isNumber())
		{
			if ("betweennumbers".equals(inTerm.getOperation()))
			{

				if (inDetail.isDataType("double"))
				{
					Double lowval = (Double) inTerm.getValue("lowval");
					Double highval = (Double) inTerm.getValue("highval");
					find = QueryBuilders.rangeQuery(fieldid).from(lowval).to(highval);
				}
				if (inDetail.isDataType("long") || inDetail.isDataType("number"))
				{
					Long lowval = (Long) inTerm.getValue("lowval");
					Long highval = (Long) inTerm.getValue("highval");
					find = QueryBuilders.rangeQuery(fieldid).from(lowval).to(highval);
				}
			}

			else if ("lessthannumber".equals(inTerm.getOperation()))
			{
				if (inDetail.isDataType("double"))
				{
					Double val = Double.valueOf(inTerm.getValue());
					find = QueryBuilders.rangeQuery(fieldid).lt(val);
				}
				if (inDetail.isDataType("long") || inDetail.isDataType("number"))
				{
					Long val = Long.valueOf(inTerm.getValue());
					find = QueryBuilders.rangeQuery(fieldid).lt(val);
				}

			}

			else if ("greaterthannumber".equals(inTerm.getOperation()))
			{
				if (inDetail.isDataType("double"))
				{
					Double val = Double.valueOf(inTerm.getValue());
					find = QueryBuilders.rangeQuery(fieldid).gt(val);
				}
				if (inDetail.isDataType("long") || inDetail.isDataType("number"))
				{
					Long val = Long.valueOf(inTerm.getValue());
					find = QueryBuilders.rangeQuery(fieldid).gt(val);
				}

			}

			else
			{
				if (inDetail.isDataType("double"))
				{
					find = QueryBuilders.termQuery(fieldid, Double.parseDouble(valueof));

				}
				else if (inDetail.isDataType("float"))
				{
					find = QueryBuilders.termQuery(fieldid, Float.parseFloat(valueof));
				}
				else
				{
					find = QueryBuilders.termQuery(fieldid, Long.parseLong(valueof));
				}
			}

		}
		else if (inDetail.isGeoPoint())
		{
			GeoFilter filter = (GeoFilter) inTerm;
			if (filter.getLatitude() == 0)
			{
				find = QueryBuilders.termQuery("id", "-" + System.currentTimeMillis());
			}
			else
			{
				GeoDistanceQueryBuilder geoDistanceFilterBuilder = new GeoDistanceQueryBuilder(inDetail.getId());
				geoDistanceFilterBuilder.point(filter.getLatitude(), filter.getLongitude());
				geoDistanceFilterBuilder.distance(String.valueOf(filter.getDistance()));
				geoDistanceFilterBuilder.optimizeBbox("memory"); // Can be also "indexed" or "none"
				geoDistanceFilterBuilder.geoDistance(GeoDistance.ARC); // Or GeoDistance.PLANE
				find = geoDistanceFilterBuilder;
			}
		}
		// DO not use _all use _description
		// else if (fieldid.equals("description"))
		// {
		// // valueof = valueof.substring(0,valueof.length()-1);
		//
		// MatchQueryBuilder text = QueryBuilders.matchPhrasePrefixQuery("_all",
		// valueof);
		// text.analyzer("lowersnowball");
		// text.maxExpansions(10);
		// find = text;
		// }
		else
		{
			if ("exact".equals(inTerm.getOperation()))
			{
				if (inDetail.isAnalyzed())
				{
					find = QueryBuilders.termQuery(fieldid + ".exact", valueof);
				}
				else
				{
					find = QueryBuilders.termQuery(fieldid, valueof);
				}
			}
			else if ("orgroup".equals(inTerm.getOperation()) || "notgroup".equals(inTerm.getOperation()))
			{
				if (inDetail.isList() || !inDetail.isAnalyzed())
				{
					find = QueryBuilders.termsQuery(fieldid, inTerm.getValues()); //This is an OR
				}
				else
				{
					String altid = fieldid + ".exact";

					find = QueryBuilders.termsQuery(altid, inTerm.getValues()); //This is an OR
					//find = createMatchQuery(fieldid, inTerm.getValues()); //This is an OR
				}
				//				BoolQueryBuilder or  = QueryBuilders.boolQuery();
				//				Object[] values = inTerm.getValues();
				//				for (int i = 0; i < values.length; i++)
				//				{
				//					Object val = values[i];
				//
				//					TermQueryBuilder item = QueryBuilders.termQuery(fieldid, val);
				//					if("notgroup".equals(inTerm.getOperation()))
				//					{
				//						or.mustNot(item);
				//					}
				//					else
				//					{
				//						or.should(item);						
				//					}
				//				}
				//				find = or;
			}
			else if ("andgroup".equals(inTerm.getOperation()))
			{
				Object[] values = inTerm.getValues();
				BoolQueryBuilder or = QueryBuilders.boolQuery();

				for (int i = 0; i < values.length; i++)
				{
					Object val = values[i];
					if (inDetail.isAnalyzed() || "keywords".equals(fieldid))
					{
						MatchQueryBuilder item = QueryBuilders.matchQuery(fieldid, val);
						or.must(item);
					}
					else
					{
						TermQueryBuilder item = QueryBuilders.termQuery(fieldid, val);
						or.must(item);

					}
				}
				find = or;
			}
			else if ("matches".equals(inTerm.getOperation()))
			{
				find = createMatchQuery(inDetail, fieldid, valueof);
			}
			else if ("contains".equals(inTerm.getOperation()))
			{
				find = createMatchQuery(inDetail, fieldid, valueof);
			}
			else if (inDetail.isList())
			{
				find = QueryBuilders.termQuery(fieldid, valueof);
			}
			else
			{
				find = createMatchQuery(inDetail, fieldid, valueof);
			}
		}
		return find;
	}

	/**
	 * This searches for partial words in the description and other text
	 * But for other keyword fields it searches for exact matches
	 * 
	 */
	protected String addSearchTerms(Term inDescriptionTerm, boolean lastword, String partofword, String previousoperator, String currentoperator, BoolQueryBuilder booleans)
	{
		//For freeform we want to have pairs of words. lowersnoball does not work well with prefix phrase queries

		//Bill_Clinton_official.jpg
		//So MYFILE.JPG will search for MYFILE and JPG and AND toghether the results?
		String escaped = QueryParser.escape(partofword);	
	
		MatchQueryBuilder oneword = null; //Desription always included
		oneword = QueryBuilders.matchPhrasePrefixQuery(inDescriptionTerm.getDetail().getId(), escaped);
		oneword.analyzer("lowersnowball");
		//booleans.must(oneword);

		//CB: I removed this because instead I just collected all the special characters and put them at the end of description field so they will be ANDed in
		
		//This does not apply for description that should be handled above
		//The other text fields can be searched directly
		
//		for (Iterator iterator = getKeywordProperties().iterator(); iterator.hasNext();)
//		{
//			PropertyDetail detail = (PropertyDetail) iterator.next();
//			if( detail.isList() || detail.isDate() || detail.isMultiLanguage() || detail.isDataType("objectarray") || detail.isDataType("nested") || detail.getId().equals("description") )   //				else if (det.isDataType("objectarray") || det.isDataType("nested"))
//			{
//				continue;
//			}
//			String altid = detail.getId();
//			if (detail.isAnalyzed())
//			{
//				altid = altid + ".sort";
//			}
//			
//			if (lastword)
//			{
//				oneword = QueryBuilders.matchPhraseQuery(altid, escaped);
//			}
//			else
//			{
//				oneword = QueryBuilders.matchPhrasePrefixQuery(altid, escaped);
//			}
//			either.should(oneword);
//		}
//		
		if (currentoperator == null && (previousoperator != null && previousoperator.equals("OR")))  //Start using OR operator
		{
			currentoperator = "OR";
		}
		else if (currentoperator == null)
		{
			currentoperator = "AND";
		}

		if (currentoperator.equals("NOT"))
		{
			booleans.mustNot(oneword);
		}
		else if (currentoperator.equals("OR"))
		{
			booleans.should(oneword);
		}
		else
		{
			booleans.must(oneword);
		}
		return currentoperator;
	}

	protected QueryBuilder createMatchQuery(PropertyDetail inDetail, String fieldid, String valueof)
	{
		QueryBuilder find;
		if (inDetail.isAnalyzed() && !inDetail.getId().equals("description"))
		{
			find = QueryBuilders.matchQuery(fieldid + ".sort", valueof);
		}
		else
		{
			find = QueryBuilders.matchQuery(fieldid, valueof);
		}
		return find;
	}

	private void wildcard(StringBuffer output, String word)
	{
		String escaped = QueryParser.escape(word);

		output.append("*");
		output.append(escaped);
		output.append("*");
	}

	protected void addSorts(SearchQuery inQuery, SearchRequestBuilder search)
	{
		if (inQuery.getSorts() == null)
		{
			return;
		}
		for (Iterator iterator = inQuery.getSorts().iterator(); iterator.hasNext();)
		{
			String field = (String) iterator.next();
			boolean direction = false;
			if (field.endsWith("Down"))
			{
				direction = true;
				field = field.substring(0, field.length() - 4);
			}
			else if (field.endsWith("Up"))
			{
				direction = false;
				field = field.substring(0, field.length() - 2);
			}
			PropertyDetail detail = getDetail(field);
			FieldSortBuilder sort = null;

			if (detail != null)
			{
				if (detail.isMultiLanguage())
				{
					if (detail.isAnalyzed())
					{
						sort = SortBuilders.fieldSort(field + "_int." + inQuery.getSortLanguage() + ".sort");
					}
					else
					{
						sort = SortBuilders.fieldSort(field + "_int." + inQuery.getSortLanguage());
					}
				}
				/*
				 * "_geo_distance": { "coords": { "lat": -27.87, "lon": -54.43
				 * }, "order": "asc", "unit": "km", "mode": "min",
				 * "distance_type": "arc", "ignore_unmapped": true }
				 */

				else if (detail.isDataType("objectarray") && detail.getObjectDetails() != null && !detail.getObjectDetails().isEmpty())
				{
					PropertyDetail first = (PropertyDetail) detail.getObjectDetails().iterator().next();
					if (first.isAnalyzed())
					{
						sort = SortBuilders.fieldSort(field + "." + first.getId() + ".sort");
					}
					else
					{
						sort = SortBuilders.fieldSort(field + "." + first.getId());
					}
				}
				else if (detail.isAnalyzed())
				{
					sort = SortBuilders.fieldSort(field + ".sort");
				}
				else
				{
					sort = SortBuilders.fieldSort(field);
				}
			}

			if (sort == null)
			{
				sort = SortBuilders.fieldSort(field);

			}

			sort.ignoreUnmapped(true);
			if (direction)
			{
				sort.order(SortOrder.DESC);
			}
			else
			{
				sort.order(SortOrder.ASC);
			}
			search.addSort(sort);

		}
	}

	public String getIndexId()
	{
		if (fieldIndexId == -1)
		{
			fieldIndexId = System.currentTimeMillis();
		}
		return String.valueOf(fieldIndexId);
	}

	public void clearIndex()
	{
		fieldIndexId = -1;
	}

	public void saveData(Data inData, User inUser)
	{
		// update the index
		// List<Data> list = new ArrayList(1);
		// list.add((Data) inData);
		// saveAllData(list, inUser);
		PropertyDetails details = getPropertyDetailsArchive().getPropertyDetailsCached(getSearchType());
		saveToElasticSearch(details, inData, false, inUser);
		clearIndex();
	}

	/*
	 * protected void bulkUpdateIndex(Collection<Data> inBuffer, User inUser) {
	 * try { String catid = toId(getCatalogId());
	 * 
	 * // BulkRequestBuilder brb = getClient().prepareBulk(); //
	 * brb.add(Requests.indexRequest(indexName).type(getIndexType()).id(id).
	 * source(source)); // } // if (brb.numberOfActions() > 0)
	 * brb.execute().actionGet(); PropertyDetails details =
	 * getPropertyDetailsArchive().getPropertyDetailsCached(getSearchType());
	 * 
	 * BulkRequestBuilder bulkRequest = getClient().prepareBulk();
	 * 
	 * for (Data data : inBuffer) { XContentBuilder content =
	 * XContentFactory.jsonBuilder().startObject();
	 * 
	 * updateIndex(content, data, details);
	 * 
	 * content.endObject(); if (data.getId() == null) { IndexRequestBuilder
	 * builder = getClient().prepareIndex(catid,
	 * getSearchType()).setSource(content); updateVersion(data, builder);
	 * bulkRequest.add(builder); } else { IndexRequestBuilder builder =
	 * getClient().prepareIndex(catid, getSearchType(),
	 * data.getId()).setSource(content); updateVersion(data, builder);
	 * 
	 * bulkRequest.add(builder); } }
	 * 
	 * BulkResponse bulkResponse =
	 * bulkRequest.setRefresh(true).execute().actionGet(); if
	 * (bulkResponse.hasFailures()) { log.info("Failures detected!"); throw new
	 * OpenEditException("failure during batch update");
	 * 
	 * } log.info("Saved " + inBuffer.size() + " records into " + catid + "/" +
	 * getSearchType()); } catch (Exception e) { throw new OpenEditException(e);
	 * } }
	 */
	public void updateIndex(Collection<Data> inBuffer, User inUser)
	{
		if( inBuffer.isEmpty() )
		{
			return;
		}
		
		if (inBuffer.size() > 2 || fieldForceBulk) // 100 was too low - caused shard exceptions
		// due to thread pool size on large
		// ingests..
		{
			updateInBatch(inBuffer, inUser); // This is asynchronous
		}
		else
		{
			PropertyDetails details = getPropertyDetailsArchive().getPropertyDetailsCached(getSearchType());
			for (Data data : inBuffer)
			{
				if (data == null)
				{
					throw new OpenEditException("Data was null!");
				}
				saveToElasticSearch(details, data, false, inUser);
			}
		}
		clearIndex();

		// inBuffer.clear();
	}

	public void updateInBatch(Collection<Data> inBuffer, User inUser)
	{
		String catid = getElasticIndexId();
		long start = new Date().getTime();
		// We cant use this for normal updates since we do not get back the id
		// or the version for new data object

		// final Map<String, Data> toversion = new HashMap(inBuffer.size());
		final List<Data> toprocess = new ArrayList(inBuffer);
		final List errors = new ArrayList();
		// Make this not return till it is finished?
		int currentordering = -1;

		BulkProcessor bulkProcessor = BulkProcessor.builder(getClient(), new BulkProcessor.Listener()
		{
			@Override
			public void beforeBulk(long executionId, BulkRequest request)
			{

			}

			@Override
			public void afterBulk(long executionId, BulkRequest request, BulkResponse response)
			{
				for (int i = 0; i < response.getItems().length; i++)
				{
					// request.getFromContext(key)
					BulkItemResponse res = response.getItems()[i];
					if (res.isFailed())
					{
						log.info(res.getFailureMessage());
						errors.add(res.getFailureMessage());

					}
					// Data toupdate = toversion.get(res.getId());
					Data toupdate = toprocess.get(res.getItemId());
					if (toupdate == null)
					{
						errors.add("Data [" + i + "] was null: " + res.getItemId());
					}
					else
					{
						if (isCheckVersions())
						{
							toupdate.setProperty(".version", String.valueOf(res.getVersion()));
						}
						toupdate.setId(res.getId());
						getCacheManager().remove("data" + getSearchType(), res.getId());
					}
				}
				//	request.refresh(true);
			}

			@Override
			public void afterBulk(long executionId, BulkRequest request, Throwable failure)
			{
				log.info(failure);
				errors.add(failure);
			}
		}).setBulkActions(-1).setBulkSize(new ByteSizeValue(10, ByteSizeUnit.MB)).setFlushInterval(TimeValue.timeValueMinutes(4)).setConcurrentRequests(1).setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueMillis(100), 10)).build();

		//setConcurrentRequests = 1 sets concurrentRequests to 1, which means an asynchronous execution of the flush operation.

		PropertyDetails details = getPropertyDetailsArchive().getPropertyDetailsCached(getSearchType());

		PropertyDetail ordering = details.getDetail("ordering");
		boolean fixordering = false;
		if (ordering != null && ordering.isAutoIncrement() && ordering.isIndex())
		{
			fixordering = true;
		}

		for (Iterator iterator = inBuffer.iterator(); iterator.hasNext();)
		{
			try
			{
				Data data2 = (Data) iterator.next();
				if (fixordering)
				{
					Object order = data2.getValue("ordering");
					if (order != null)
					{
						if (Long.parseLong(order.toString()) == 0)
						{
							order = null;
						}
					}
					if (order == null)
					{
						if (currentordering == -1)
						{
							IdManager manager = (IdManager) getModuleManager().getBean(getCatalogId(), "idManager");
							currentordering = manager.nextNumber(getSearchType() + "_ordering").intValue();
						}
						else
						{
							currentordering = currentordering + 10;
						}
						data2.setValue("ordering", currentordering);
					}
				}

				XContentBuilder content = XContentFactory.jsonBuilder().startObject();
				presave(details, data2, content, false);
				updateIndex(content, data2, details, inUser);
				content.endObject();
				IndexRequest req = Requests.indexRequest(catid).type(getSearchType());
				PropertyDetail parent = details.getDetail("_parent");
				if (parent != null)
				{
					// String _parent = data.get(parent.getListId());
					String _parent = data2.get(parent.getId());
					if (_parent != null)
					{
						req.parent(_parent);
					}
				}
				if (data2.getId() != null)
				{
					req = req.id(data2.getId());

				}
				req = req.source(content);
				// if( isRefreshSaves() )
				// {
				// req = req.refresh(true);
				// }
				//				try
				//				{
				bulkProcessor.add(req);
				//				}
				//				catch( RemoteTransportException ex)
				//				{
				//					if( ex.getCause() instanceof EsRejectedExecutionException)
				//					{
				//						
				//					}
				//				}
			}
			catch (Throwable ex)
			{
				if (ex instanceof OpenEditException)
				{
					throw (OpenEditException) ex;
				}
				throw new OpenEditException(ex);

			}
		}

		//		bulkProcessor.close();
		try
		{
			bulkProcessor.flush();
			bulkProcessor.awaitClose(5, TimeUnit.MINUTES);

			//This is in memory only flush
			RefreshResponse actionGet = getClient().admin().indices().prepareRefresh(catid).execute().actionGet();

		}
		catch (InterruptedException e)
		{
			throw new OpenEditException(e);
		}

		if (errors.size() > 0)
		{
			throw new OpenEditException((String) errors.get(0).toString());

		}
		long end = new Date().getTime();
		double total = (end - start) / 1000.0;
		log.info("processed bulk save  " + inBuffer.size() + " records in " + total + " seconds (" + getSearchType() + ")");

		if (currentordering != -1)
		{
			IdManager manager = (IdManager) getModuleManager().getBean(getCatalogId(), "idManager");
			manager.setNumber(getSearchType() + "_ordering", currentordering);
		}

		// ConcurrentModificationException
		// builder = builder.setSource(content).setRefresh(true);
		// BulkRequestBuilder brb = getClient().prepareBulk();
		//
		// brb.add(Requests.indexRequest(indexName).type(getIndexType()).id(id).source(source));
		// }
		// if (brb.numberOfActions() > 0) brb.execute().actionGet();

		//getClient().admin().cluster().prepareHealth().setWaitForGreenStatus().execute().actionGet();

	}

	protected void presave(PropertyDetails details, Data inData, XContentBuilder content, boolean delete)
	{
		try
		{
			if(!isTrackEdits())
			{
				return;
			}

			Map status = (Map) inData.getValue("emrecordstatus");
			/*
			 * if(status == null) { status = new HashMap(); } if
			 * (isReIndexing()) { content.field("emrecordstatus", status);
			 * return; }
			 */
			String localClusterId = getElasticNodeManager().getLocalClusterId();
			if (isReIndexing())
			{
				if (status != null )
				{
					if (getElasticNodeManager().isForceSaveMasterCluster())
					{
						String oldClusterId = (String) status.get("mastereditclusterid");
						String lastModifiedClusterId = (String) status.get("lastmodifiedclusterid");
						if (oldClusterId != null && oldClusterId.equals(lastModifiedClusterId))
						{
							status.put("lastmodifiedclusterid", localClusterId);
						}
						
						status.put("mastereditclusterid", localClusterId);
						
					}
					content.field("emrecordstatus", status);
					return;				
				}
				
				if (isOptimizeReindex())
				{
					return; //Dont worry if its not created already
				}
			}

			if (status == null)
			{
				status = new HashMap();
			}

			
			String currentid = null;
			
			
			if (status != null)
			{
				currentid = (String) status.get("mastereditclusterid");
			}
			if (currentid == null)
			{
				currentid = localClusterId;
			}
			

			status.put("recorddeleted", delete);
			status.put("mastereditclusterid", currentid);
			if (isReIndexing() && status.get("lastmodifiedclusterid") != null)
			{
				//Do nothing. Will copy old value
			}
			else
			{
				status.put("lastmodifiedclusterid", localClusterId); //reset				
			}

			Object currentmod = status.get("recordmodificationdate");
			if (currentmod instanceof String)
			{
				currentmod = DateStorageUtil.getStorageUtil().parseFromStorage((String) currentmod);
			}

			currentmod = new Date();

			status.put("recordmodificationdate", currentmod);

			Object currentmastermod = null;
			if (status != null)
			{

				currentmastermod = status.get("masterrecordmodificationdate");
			}
			if (currentmastermod instanceof String)
			{
				currentmastermod = DateStorageUtil.getStorageUtil().parseFromStorage((String) currentmastermod);
			}
			if (currentid.equals(localClusterId))
			{
				currentmastermod = currentmod;
			}
			status.put("masterrecordmodificationdate", currentmastermod);

			content.field("emrecordstatus", status);

		}
		catch (Exception ex)
		{
			throw new OpenEditException(ex);
		}
	}

	protected boolean isTrackEdits()
	{
		return true;
	}

	public void deleteAll(Collection inBuffer, User inUser)
	{
		if (inBuffer instanceof HitTracker)
		{
			HitTracker htracker = (HitTracker) inBuffer;
			htracker.enableBulkOperations();

		}
		String catid = getElasticIndexId();

		if (inBuffer.size() < 99) // 100 was too low - caused shard exceptions
		// due to thread pool size on large
		// ingests..
		{
			for (Iterator iterator = inBuffer.iterator(); iterator.hasNext();)
			{
				Data object = (Data) iterator.next();
				try
				{
					delete(object, inUser);
				}
				catch (Exception ex)
				{
					log.error("Could not delete " + object, ex);
				}
			}
			return;
		}

		final List errors = new ArrayList();
		// Make this not return till it is finished?
		BulkProcessor bulkProcessor = BulkProcessor.builder(getClient(), new BulkProcessor.Listener()
		{
			@Override
			public void beforeBulk(long executionId, BulkRequest request)
			{

			}

			@Override
			public void afterBulk(long executionId, BulkRequest request, BulkResponse response)
			{
				for (int i = 0; i < response.getItems().length; i++)
				{
					// request.getFromContext(key)
					BulkItemResponse res = response.getItems()[i];
					if (res.isFailed())
					{
						log.info(res.getFailureMessage());
						errors.add(res.getFailureMessage());

					}
				}
			}

			@Override
			public void afterBulk(long executionId, BulkRequest request, Throwable failure)
			{
				log.info(failure);
				errors.add(failure);
			}
		}).setBulkActions(inBuffer.size()).setBulkSize(new ByteSizeValue(1, ByteSizeUnit.GB)).setFlushInterval(TimeValue.timeValueSeconds(5)).setConcurrentRequests(2).build();

		for (Iterator iterator = inBuffer.iterator(); iterator.hasNext();)
		{
			try
			{
				Data data2 = (Data) iterator.next();

				DeleteRequest req = Requests.deleteRequest(catid).type(getSearchType());

				if (data2.getId() != null)
				{
					req = req.id(data2.getId());

				}
				bulkProcessor.add(req);
			}
			catch (Exception ex)
			{
				log.error(ex);
			}
		}
		try
		{
			bulkProcessor.flush();
			bulkProcessor.awaitClose(5, TimeUnit.MINUTES);
			clearIndex();
			//This is in memory only flush
			RefreshResponse actionGet = getClient().admin().indices().prepareRefresh(catid).execute().actionGet();

		}
		catch (InterruptedException e)
		{
			throw new OpenEditException(e);
		}

		if (errors.size() > 0)
		{
			log.error("Bulk delete errors" + errors);
			//TODO: Throw exception?
		}
		clearIndex();
	}

	protected void saveToElasticSearch(PropertyDetails details, Data data, boolean delete, User inUser)
	{
		try
		{
			String catid = getElasticIndexId();
			XContentBuilder content = XContentFactory.jsonBuilder().startObject();

			IndexRequestBuilder builder = null;
			if (data.getId() == null)
			{
				builder = getClient().prepareIndex(catid, getSearchType()); //Should we preface the id?
			}
			else
			{
				builder = getClient().prepareIndex(catid, getSearchType(), data.getId());
			}

			PropertyDetail parent = details.getDetail("_parent");
			if (parent != null)
			{
				// String _parent = data.get(parent.getListId());
				String _parent = data.get(parent.getId());
				if (_parent != null)
				{
					builder = builder.setParent(_parent);
				}
				else
				{
					return; // Can't save data that doesn't have a parent!
				}
			}
			presave(details, data, content, delete);
			updateIndex(content, data, details, inUser);
			content.endObject();
			if (log.isDebugEnabled())
			{
				log.info("Saving " + getSearchType() + " " + data.getId() + " = " + content.string());
			}

			builder = builder.setSource(content);
			//log.info("Saving " + getSearchType() + " " + data.getId() + " = " + content.string());

			if (isRefreshSaves())
			{
				builder = builder.setRefresh(true);
			}
			if (isCheckVersions())
			{
				updateVersion(data, builder);
			}
			IndexResponse response = null;

			response = builder.execute().actionGet();
			if (response.getId() != null)
			{
				data.setId(response.getId());
			}
			data.setValue(".version", response.getVersion());
		}
		catch (RemoteTransportException ex)
		{
			if (ex.getCause() instanceof VersionConflictEngineException)
			{
				throw new ConcurrentModificationException(ex.getMessage());
			}
		}
		catch (VersionConflictEngineException ex)
		{
			throw new ConcurrentModificationException(ex.getMessage());
		}
		catch (Exception ex)
		{
			log.error("Problem saving data in " + getCatalogId() + " " + getSearchType() + " " + data.getId());
			if (ex instanceof OpenEditException)
			{
				throw (OpenEditException) ex;
			}
			throw new OpenEditException(ex);
		}

		getCacheManager().remove("data" + getSearchType(), data.getId());
	}

	public void setIndexId(long inIndexId)
	{
		fieldIndexId = inIndexId;
	}

	private void updateVersion(Data data, IndexRequestBuilder builder)
	{
		if (isCheckVersions())
		{
			Long version = (Long) data.getValue(".version");
			if (version != null)
			{
				if (version.longValue() > -1)
				{
					builder.setVersion(version);
				}
			}
		}
	}

	protected void updateIndex(XContentBuilder inContent, Data inData, PropertyDetails inDetails)
	{
		updateIndex(inContent, inData, inDetails, null);
	}

	protected void updateIndex(XContentBuilder inContent, Data inData, PropertyDetails inDetails, User inUser)
	{
		if (inData == null)
		{
			log.error("Null Data");
			return;
		}
		try
		{
			//Map props = inData.getProperties();
			
			HashSet allprops = new HashSet();
			
			if( inDetails.isAllowDynamicFields() )
			{
				allprops.addAll(inData.getProperties().keySet()); //Needed for legacy field handling below
			} 
			else if( isCheckLegacy() )
			{
				for (Iterator iterator = inDetails.iterator(); iterator.hasNext();)
				{
					PropertyDetail detail = (PropertyDetail) iterator.next();
					String legacyfield = detail.get("legacy");
					if (legacyfield != null )
					{
						allprops.add(legacyfield); //We need to make a copy anyways
					}
				}
			}
			//allprops.addAll(props.keySet());
			for (Iterator iterator = inDetails.iterator(); iterator.hasNext();)
			{
				PropertyDetail detail = (PropertyDetail) iterator.next();
				if (!detail.isDeleted()) //TODO: Dont pass in deleted to begin with
				{
					allprops.add(detail.getId()); //We need to make a copy anyways
				}
			}
			if (!allprops.contains("description"))
			{
				allprops.add("description");
			}
			if (!allprops.contains("id"))
			{
				allprops.add("id");
			}
			if (!allprops.contains("entitysourcetype"))
			{
				if (getDetail("entitysourcetype") != null)
				{
					allprops.add("entitysourcetype");
				}
			}
			List badges = new ArrayList();

			for (Iterator iterator = allprops.iterator(); iterator.hasNext();)
			{
				String propid = (String) iterator.next();
				if (propid == null)
				{
					continue;
				}
				if (propid.contains("."))
				{
					continue;
				}
				if (propid.equals("entitysourcetype"))
				{
					inContent.field(propid, getSearchType()); //Cheap workaround for emfinder
					continue;
				}
				if (propid.equals("recordmodificationdate") || propid.equals("mastereditclusterid") || propid.equals("masterrecordmodificationdate") || propid.equals("emrecordstatus"))
				{
					continue;
				}
				if (propid.contains("recorddeleted"))
				{
					continue;
				}

				PropertyDetail detail = (PropertyDetail) inDetails.getDetail(propid);
				if (detail == null)
				{
					detail = inDetails.getLegacyDetail(propid);
				}
				if (detail != null && detail.isDeleted())
				{
					if( !inDetails.isAllowDynamicFields() )
					{
						continue;
					}
				}
				if (detail != null && detail.get("stored") != null && "false".equals(detail.get("stored")))
				{
					continue;
				}
				
				
				if (detail == null && !propid.equals("description") && !propid.contains("_int") && !propid.equals("emrecordstatus") && !propid.equals("recordmodificationdate") && !propid.equals("mastereditclusterid"))
				{
					if (isReIndexing())
					{
						continue;
					}
					detail = getPropertyDetailsArchive().createDetail(getSearchType(), propid, propid);
					detail.setDeleted(false);
					//setType(detail);
					getPropertyDetailsArchive().savePropertyDetail(detail, getSearchType(), null);
					inDetails.addDetail(detail);

					if (!putMappings())
					{
						throw new OpenEditException(getSearchType() + " could not put mapping on data " + propid + " rowid=" + inData.getId());
					}
					else
					{
						log.info("Added new detail " + propid + " to " + getSearchType() + " as " + detail.getDataType());
					}
				}
				if (detail == null || !detail.isIndex()) //&& !propid.contains("sourcepath")
				{
					continue;
				}
				String key = detail.getId();
				if (key == null)
				{
					continue;
				}
				if (shoudSkipField(key))
				{
					continue;
				}
				if (propid.equals("description")) //This field must be defined by user first. or it will continue above
				{
					Object value = inData.getValue(propid);
					if (value == null || !isReIndexing() || !isOptimizeReindex()) 
					{
						StringBuffer desc = new StringBuffer();
						populateKeywords(desc, inData, inDetails);
						//populateFullText(inData, desc);

						if (desc.length() > 0)
						{
							value = fixSpecialCharacters(desc);
							//value = desc.toString();
						}
					}
					//?inData.setValue("description",value);
					inContent.field(propid, value);
					continue;
				}
				Object value = null;
				//				String mask = detail.get("rendermask");
				//				if( mask != null && Boolean.parseBoolean(detail.get("index")) )
				//				{
				//					value = getReplacer().replace(mask, inData);
				//				}
				value = inData.getValue(key);
				if (value != null)
				{
					if (value instanceof String && ((String) value).isEmpty()) //Standarize
					{
						value = null;
					}
				}
				else
				{
					if (!isReIndexing() && detail.isAutoIncrement())
					{
						IdManager manager = (IdManager) getModuleManager().getBean(getCatalogId(), "idManager");
						value = manager.nextNumber(getSearchType() + "_" + detail.getId());
					}
				}
				//				if( isReIndexing() ) //When reindexing dont mess with this data
				//				{
				//					if (key.equals("recordmodificationdate"))
				//					{
				//						inContent.field(key, value);
				//						continue;
				//					}
				//					if (key.equals("mastereditclusterid"))
				//					{
				//						inContent.field(key, value); //Copy over existing values
				//						continue;
				//					}
				//				}	

				if (detail.isBadge() && value != null)
				{
					badges.add(getSearchType() + "_" + detail.getId() + "_" + value);
				}

				if (value != null && (detail.isDataType("object")))
				{
					 if (value instanceof String)
					    {
					        // parse JSON string → Map
					        value = new JsonSlurper().parseText((String) value);
					    }
					    else if (!(value instanceof Map))
					    {
					        throw new OpenEditException(inData.getId() + " / " + detail.getId()
					                + " Data was not a Map or JSON string " + value.getClass());
					    }
					    inContent.field(key, value); // accept single object
				}
				

				if (value != null && (detail.isDataType("objectarray") || detail.isDataType("nested")))
				{
					if (!(value instanceof Collection))
					{
						if (value instanceof String)
						{
							String[] values = MultiValued.VALUEDELMITER.split((String) value);
							Collection objects = new ArrayList(values.length);
							//JsonSlurper slurper = new JsonSlurper();
							for (int i = 0; i < values.length; i++)
							{
								//{cliplabel=New Clip, timecodelength=114863, timecodestart=108276}
								String text = values[i];
								if (text.length() < 2)
								{
									continue;
								}
								text = text.substring(1, text.length() - 1);
								String[] parts = text.split(",");
								Map chunk = new HashMap();
								for (int j = 0; j < parts.length; j++)
								{
									String ptext = parts[j];
									int eq = ptext.indexOf("=");
									if (eq > 0)
									{
										String id = ptext.substring(0, eq);
										String valtext = ptext.substring(eq + 1, ptext.length());
										chunk.put(id.trim(), valtext.trim());
									}
								}
								objects.add(chunk);
							}
							value = objects;
						}
						else
						{
							throw new OpenEditException(inData.getId() + " / " + detail.getId() + " Data was not a collection or a string " + value.getClass());
						}
					}
					inContent.field(key, value); //This seems to map Long data types to Integer when they are read again
				}
				
				else if (detail.isDate())
				{
					if (value != null)
					{
						Date date = null;
						if (value instanceof Date)
						{
							date = (Date) value;
						}
						else if (value instanceof String)
						{
							date = DateStorageUtil.getStorageUtil().parseFromStorage((String) value);
						}
						if (date != null)
						{
							inContent.field(key, date);
						}
					}
				} 
				else if (detail.isCategory())
				{
					//Lets assume this detail is the EXACT one, not the set.  the matching detail should have 
					//TODO:  add this optimization
//					if (isOptimizeReindex() && !(inData instanceof Asset)) //Low level performance fix
//					{
//						MultiValued values = (MultiValued) inData;
//						saveArray(inContent, "category", values.getValues("category"));
//						saveArray(inContent, "category-exact", values.getValues("category-exact"));
//						String desc = values.get("description");
//						inContent.field("description", desc);
//						setFolderPath(inData, inContent);
//						super.updateIndex(inContent, inData, inDetails, inUser);
//
//						return;
//					}
					
					if (value != null)
					{

						String fullsetdetail = detail.get("categorypath");
						List categories = null;
						Set fulltree = null;
						String searchtype = detail.getListId();
						String catalog = detail.getListCatalogId();

						CategorySearcher catsearcher = (CategorySearcher) getSearcherManager().getSearcher(catalog, searchtype);
						if (value instanceof Collection)
						{
							categories = new ArrayList();
							Collection vals = (Collection) value;
							for (Iterator iterator2 = vals.iterator(); iterator2.hasNext();)
							{
								String catid = (String) iterator2.next();
								Category cat = catsearcher.getCategory(catid);
								if (cat != null)
								{
									categories.add(cat);
								}
							}
						}
						else
						{
							categories = new ArrayList();
							Category cat = catsearcher.getCategory((String) value);
							if (cat != null)
							{
								categories.add(cat);
							}
						}

						fulltree = catsearcher.buildCategorySet(categories);

						String[] catids = new String[categories.size()];
						int i = 0;
						for (Iterator iterator3 = categories.iterator(); iterator3.hasNext();)
						{
							Category cat = (Category) iterator3.next();
							catids[i++] = cat.getId();
						}
						if (i > 0)
						{
							inContent.field(detail.getId(), catids);
						}

						List ids = new ArrayList(fulltree.size());
						for (Iterator iterator2 = fulltree.iterator(); iterator2.hasNext();)
						{
							Object object = iterator2.next();
							String id = null;
							if (object instanceof Data)
							{
								id = ((Data) object).getId();
							}
							else
							{
								id = String.valueOf(object);
							}
							ids.add(id);
						}
						if (ids.size() > 0)
						{
							String[] array = new String[ids.size()];
							Object oa = ids.toArray(array);
							inContent.field(fullsetdetail, oa);
						}
					}
					else
					{
						inContent.field(key, value);

					}

				}
				else if (detail.isBoolean())
				{
					boolean val = false;
					if (value instanceof Boolean)
					{
						val = (Boolean) value;
					}
					else if (value != null)
					{
						val = Boolean.valueOf((String) value);
					}
					inContent.field(key, val);
				}
				else if (detail.isDataType("double"))
				{
					if( detail.isMultiValue() )
					{
						if (value instanceof double[] )
						{
							double[] values = (double[]) value;
							inContent.field(key, values);
							continue;
						}
						if (value instanceof List )
						{
							List<Double> values = (List<Double>) value;
							inContent.field(key, values);
							continue;
						}
					}
					Double val = null;

					if (value instanceof Double)
					{
						val = (Double) value;
					}
					else if (value instanceof Integer)
					{
						val = Double.valueOf((int) value);
					}
					else if (value instanceof Long)
					{
						val = Double.valueOf((long) value);
					}
					else if (value != null)
					{
						try
						{
							val = Double.valueOf((String) value);
						}
						catch (Exception ef)
						{
							log.error("Cant format " + getSearchType() + " " + detail.getId() + " " + value, ef);
							continue;
						}

					}
					inContent.field(key, val);
				}
				else if (detail.isDataType("float"))
				{
					if( detail.isMultiValue() )
					{
						if (value instanceof float[] )
						{
							float[] values = (float[]) value;
							inContent.field(key, values);
							continue;
						}
						if (value instanceof List )
						{
							List<Float> values = (List<Float>) value;
							inContent.field(key, values);
							continue;
						}
					}
					Float val = null;

					if (value instanceof Float)
					{
						val = (Float) value;
					}
					else if (value instanceof Integer)
					{
						val = Float.valueOf((int) value);
					}
					else if (value instanceof Long)
					{
						val = Float.valueOf((long) value);
					}
					else if (value != null)
					{
						try
						{
							val = Float.valueOf((String) value);
						}
						catch (Exception ef)
						{
							log.error("Cant format " + getSearchType() + " " + detail.getId() + " " + value, ef);
							continue;
						}

					}
					inContent.field(key, val);
				}
				else if (detail.isDataType("long"))
				{
					Long val = null;
					if (value instanceof Double && detail.getId().contains("timecode"))
					{
						Double d = (Double) value;
						val = Math.round(d * 1000d);
					}
					else if (value instanceof Double)
					{
						val = Math.round((Double) value); //Throw exception?
					}
					else if (value instanceof Integer)
					{
						val = ((Integer) value).longValue();
					}
					else if (value != null)
					{
						val = Long.valueOf(value.toString());
					}
					inContent.field(key, val);
				}
				else if (detail.isDataType("number"))
				{
					Object val = 0;

					if (value instanceof Collection)
					{
						val = value;
					}
					else if (value instanceof Number)
					{
						val = (Number) value;
					}
					else if (value instanceof Integer)
					{
						val = (Integer) value;
					}
					else if (value != null)
					{
						try
						{
							val = Long.valueOf((String) value);
						}
						catch (Exception e)
						{
							//						throw new OpenEditException("Bad Value for Number:  " + val + " trying to set: " + key);
							log.info("Bad Value for Number:  " + val + " trying to set: " + key);

						}
					}
					inContent.field(key, val);
				}
				else if (detail.isMultiValue() || detail.isList())
				{
					if (value != null)
					{
						if (value instanceof Data)
						{
							String id = ((Data) value).getId();
							inContent.field(key, id);
						}
						else if (value instanceof Collection)
						{
							Collection values = (Collection) value;
							Collection ids = new ArrayList(values.size());
							for (Iterator iterator2 = values.iterator(); iterator2.hasNext();)
							{
								Object object = (Object) iterator2.next();
								if (object instanceof Data)
								{
									ids.add(((Data) object).getId());
								}
								else
								{
									ids.add(String.valueOf(object));
								}
							}
							inContent.field(key, ids);
						}
						else if (detail.isMultiValue() && value instanceof String)
						{
							String vs = (String) value;
							String[] vals = VALUEDELMITER.split(vs);
							Collection values = Arrays.asList(vals);
							inContent.field(key, values);
						}
						else
						{
							inContent.field(key, value);
						}
					}
				}
				else if (value != null && detail.isGeoPoint())
				{
					//Saved it as two fields?
					if (value instanceof Position)
					{
						Position pos = (Position) value;
						GeoPoint point = new GeoPoint(pos.getLatitude(), pos.getLongitude());
						inContent.field(key, point);
					}
					else if (value instanceof String)
					{
						String geopoint = (String) value;
						if( geopoint.startsWith("{") )
						{
							if( !geopoint.contains("\""))
							{
								geopoint = geopoint.substring(6, geopoint.length() - 1);
								geopoint = geopoint.replace("lng: ", "");
							}
							else
							{
								Map points = new JSONParser().parse(geopoint);
								geopoint = points.get("lat") + "," + points.get("lng");	
							}
							
						}
						GeoPoint point = new GeoPoint(geopoint);
						inContent.field(key, point); 
						Position position = new Position(point.getLat(), point.getLon());
						 
						inData.setValue(key, position); //For next time?
					}
					else if (value instanceof GeoPoint)
					{
						GeoPoint point = (GeoPoint) value;
						inContent.field(key, point);
						Position position = new Position(point.getLat(), point.getLon());
						inData.setValue(key, position); //For next time?
					}
				}
				else if (detail.isMultiLanguage())
				{
					// This is a nested document
					if (value == null)
					{
						continue;
					}
					key = key + "_int";
					XContentBuilder lanobj = inContent.startObject(key); // start
																			// first
																			// detail
																			// object

					HitTracker locales = getSearcherManager().getList(getCatalogId(), "locale");

					if (value instanceof String)
					{
						String target = (String) value;
						LanguageMap map = new LanguageMap();
						map.setText("en", target);
						value = map;
					}
					if (value instanceof LanguageMap)
					{
						// all good
					}
					else if (value instanceof Map)
					{
						value = new LanguageMap((Map) value);
					}
					if (!(value instanceof LanguageMap))
					{
						throw new OpenEditException("Unexpexted value for MultiLanguage enabled field : " + value + " detail: " + detail.getId() + "Data Was: " + inData.getId() + " searchtype " + getSearchType());
					}
					LanguageMap map = (LanguageMap) value;
					for (Iterator iterator2 = locales.iterator(); iterator2.hasNext();)
					{
						Data locale = (Data) iterator2.next();
						String id = locale.getId();
						String localeval = map.getText(id); // get value
						if (localeval != null)
						{
							lanobj.field(id, localeval);
						}
					}
					lanobj.endObject();
				}
				else
				{
					if (value == null)
					{
						// log.info( getSearchType() + "Had null value " + key);
					}
					else
					{
						if (value instanceof LanguageMap)
						{
							value = ((LanguageMap) value).toString();
						}
						else if (!(value instanceof String))
						{
							String svalue = String.valueOf(value);
							if (svalue.isEmpty())
							{
								value = null;
							}
						}
						if (value != null)
						{
							inContent.field(key, value);
						}
					}
				}
				// log.info("Saved" + key + "=" + value );
			}
			if (!badges.isEmpty())
			{
				inContent.field("badge", badges);
			}

			addSecurity(inContent, inData);

			addCustomFields(inContent, inData);
		}

		catch (Exception ex)
		{
			if (ex instanceof OpenEditException)
			{
				throw (OpenEditException) ex;
			}
			throw new OpenEditException(ex);
		}

	}

	protected String fixSpecialCharacters(StringBuffer inDesc)
	{
		String[] vals = MultiValued.VALUEDELMITER.split(inDesc.toString());
		StringBuffer out = new StringBuffer();
		String pipe = "|";
		Set allwords = new HashSet();
		
		Set extras = new HashSet();
		
		for (int i = 0; i < vals.length; i++)
		{
			String chunk = vals[i].trim();
			out.append(chunk);
			if( i < vals.length)
			{
				out.append(pipe);
			}
			String[] spaces = spacepattern.split(chunk);
			for (int j = 0; j < spaces.length; j++)
			{
				String word = spaces[j];
				allwords.add(word);
				Matcher matcher = separatorchars.matcher(word);
				//Matcher matcher = Pattern.compile("([a-zA-Z0-9]+)").matcher(spaces[j]);
				//matcher.find(); //Skip first one, this is ok in the main text
				while (matcher.find())
				{
					// Get the matched character
					String partialword = matcher.group();
					if(!allwords.contains(partialword) && partialword.length() > 1)
					{
						extras.add(partialword);
					}
				}
			}
		}

		StringBuffer special = new StringBuffer();
		for (Iterator iterator = extras.iterator(); iterator.hasNext();)
		{
			String word = (String) iterator.next();
			special.append(word);
			special.append(pipe);
		}
		String finalout = out.toString() + special;
		
		return finalout;
	}

	protected void addSecurity(XContentBuilder inContent, Data inData) throws Exception
	{
		//Check for security
		PropertyDetail detail = getDetail("securityenabled");

		if (detail == null)
		{
			return;
		}
		
		PropertyDetail alwaysvisible = getDetail("securityalwaysvisible");

		if (alwaysvisible != null)
		{
			if(  Boolean.parseBoolean( inData.get("securityalwaysvisible") ) )
			{
				inContent.field("securityenabled", false); //Everyone
				return;
			}
		}
		
		Collection combinedusers = new HashSet();
		Collection combinedgroups = new HashSet();
		Collection combinedroles = new HashSet();
		
		boolean securityenabled = false;
				
		String securityfield = (String) detail.getValue("securityfield");
		
		PropertyDetail securefield = getDetail(securityfield);;
		
		if (securefield == null)
		{
			combinedusers = inData.getValues("viewusers");
			combinedgroups = inData.getValues("viewgroups");
			combinedroles = inData.getValues("viewroles");

		}
		else
		{
			
			String fieldid = securefield.getId();
			String categorysearchertype = securefield.getListId();//category 
			CategorySearcher searcher = (CategorySearcher) getSearcherManager().getSearcher(getCatalogId(), categorysearchertype);

			if ("category".equals(securefield.getViewType()))  //View type can be rootcategory OR category-exact
			{
				Collection exact = inData.getValues(securityfield); 
				if (exact != null)
				{
						
					
					for (Iterator iterator = exact.iterator(); iterator.hasNext();)
					{
						Object obj = iterator.next();
						Category c = null;
						if( obj instanceof Category )
						{
							c = (Category)obj;
						}
						else
						{
							c= (Category) searcher.getCategory((String)obj);
						}
						if (c == null)
						{
							log.info("Category missing: "+ obj + " Searchtype: " + getSearchType() + " Data: " + inData);
							continue;
						}
						
						Collection moreusers = c.collectValues("viewerusers"); //These are already combined from customusers
						Collection moregroups = c.collectValues("viewergroups");
						Collection moreroles = c.collectValues("viewerroles");
						
						if (moreusers != null)
						{
							combinedusers.addAll(moreusers);
						}
						if (moregroups != null)
						{
							combinedgroups.addAll(moregroups);
						}
						if (moreroles != null)
						{
							combinedroles.addAll(moreroles);
						}

					}
				}

			}
		}
		if (combinedusers != null && !combinedusers.isEmpty())
		{
			inContent.field("viewusers", combinedusers);
			securityenabled = true;
		}
		if (combinedgroups != null && !combinedgroups.isEmpty())
		{
			inContent.field("viewgroups", combinedgroups);
			securityenabled = true;
		}
		if (combinedroles != null && !combinedroles.isEmpty())
		{
			inContent.field("viewroles", combinedroles);
			securityenabled = true;
		}
		
		inContent.field("securityenabled", securityenabled);

	}

	public void addCustomFields(XContentBuilder inContent, Data inData)
	{
		// TODO Auto-generated method stub  Override this for custom searchers

	}

	//	private void setType(PropertyDetail detail) {
	//	
	//		
	//		
	//		String catid = getElasticIndexId();
	//		GetFieldMappingsRequest	 req = new GetFieldMappingsRequest().indices(catid).fields(detail.getId());
	//		GetFieldMappingsResponse resp = getClient().admin().indices().getFieldMappings(req).actionGet();
	//		Map data = resp.mappings();
	//		Object mappings = data.get(catid);
	//		if(mappings != null){
	//			Map types = (Map) data.get("blah");
	//		}
	//	}

	private void checkMapping(String inKey) throws Exception
	{
		String catid = getElasticIndexId();

		GetMappingsRequest req = new GetMappingsRequest().indices(catid).types(getSearchType());
		GetMappingsResponse resp = getClient().admin().indices().getMappings(req).actionGet();
		String indexname = getElasticNodeManager().getIndexNameFromAliasName(catid);
		if (indexname != null)
		{
			ImmutableOpenMap typeMappings = resp.getMappings().get(indexname);
			MappingMetaData mapping = (MappingMetaData) typeMappings.get(getSearchType());

			LinkedHashMap data = (LinkedHashMap) mapping.getSourceAsMap();
			Map properties = (Map) data.get("properties");
			Object prop = properties.get(inKey);

			if (prop == null)
			{

				XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
				jsonBuilder.startObject("properties");
				jsonBuilder.startObject(inKey);
				jsonBuilder.endObject();
				jsonBuilder.endObject();
				PutMappingRequest putreq = new PutMappingRequest().indices(new String[] { catid }).type(getSearchType()).source(jsonBuilder);
				getClient().admin().indices().putMapping(putreq);
			}
		}
	}

	public boolean shoudSkipField(String inKey)
	{
		//skip description?
		if ("_id".equals(inKey) || "_parent".equals(inKey) || "_all".equals(inKey) || inKey.contains(".") || inKey.contains("viewusers") || inKey.contains("viewgroups") || inKey.contains("viewroles") || inKey.contains("securityenabled"))
		{
			return true;
		}
		return false;
	}

	public void deleteAll(User inUser)
	{

		// https://github.com/elastic/elasticsearch/blob/master/plugins/delete-by-query/src/main/java/org/elasticsearch/action/deletebyquery/TransportDeleteByQueryAction.java#L104

		if (inUser != null)
		{
			log.info("Deleted all records database " + getSearchType() + " by user:" + inUser.getId());
		}
		//		 DeleteByQueryRequestBuilder delete =
		//		 getClient().prepareDeleteByQuery(toId(getCatalogId()));
		//		 delete.setTypes(getSearchType());
		//		 delete.setQuery(new MatchAllQueryBuilder()).execute().actionGet();

		org.openedit.data.QueryBuilder q = query().all();

		q.getQuery().setIncludeDeleted(true);

		HitTracker all = q.search();
		all.enableBulkOperations();

		deleteAll(all, null);

	}

	public void delete(Data inData, User inUser)
	{
		PropertyDetails details = getPropertyDetailsArchive().getPropertyDetailsCached(getSearchType());

		Map recordstatus = (Map) inData.getValue("emrecordstatus");

		if (recordstatus != null)
		{
			if (inUser != null)
			{
				saveToElasticSearch(details, inData, true, inUser);
				clearIndex();
				return;
			}
		}
		//We should not do this as much for some tables
		String id = inData.getId();
		//log.info(id.length());
		DeleteRequestBuilder delete = getClient().prepareDelete(toId(getCatalogId()), getSearchType(), id);
		if (inData.get("_parent") != null)
		{
			delete.setParent(inData.get("_parent"));
		}
		delete.setRefresh(true).execute().actionGet();
		clearIndex();

	}

	// Base class only updated the index in bulk
	public void saveAllData(Collection<Data> inAll, User inUser)
	{
		updateIndex(inAll, inUser);
	}

	public synchronized String nextId()
	{
		// Lock lock = getLockManager().lock(getCatalogId(), loadCounterPath(),
		// "admin");
		// try
		// {
		// return String.valueOf(getIntCounter().incrementCount());
		// }
		// finally
		// {
		// getLockManager().release(getCatalogId(), lock);
		// }
		throw new OpenEditException("Should not call next ID");
	}

	protected IntCounter getIntCounter()
	{
		// if (fieldIntCounter == null)
		// {
		// fieldIntCounter = new IntCounter();
		// // fieldIntCounter.setLabelName(getSearchType() + "IdCount");
		// Page prop = getPageManager().getPage(loadCounterPath());
		// File file = new File(prop.getContentItem().getAbsolutePath());
		// file.getParentFile().mkdirs();
		// fieldIntCounter.setCounterFile(file);
		// }
		// return fieldIntCounter;
		throw new OpenEditException("Cant load int counters from elasticsearch");
	}

	/** TODO: Update this location to match the new standard location */
	protected String loadCounterPath()
	{
		return "/WEB-INF/data/" + getCatalogId() + "/" + getSearchType() + "s/idcounter.properties";
	}

	// public boolean hasChanged(HitTracker inTracker)
	// {
	// //We will add a refresh() to the tracker and call it with cachedSearch
	// //We will scroll forward using the scroll
	// //We will scroll backwards using a previous chunck or new search
	// return false;
	// }

	// public HitTracker checkCurrent(WebPageRequest inReq, HitTracker
	// inTracker) throws OpenEditException
	// {
	// return inTracker;
	// }

	protected boolean flushChanges()
	{
		FlushRequest req = Requests.flushRequest(toId(getCatalogId())); //To The disk drive
		FlushResponse res = getClient().admin().indices().flush(req).actionGet();
		if (res.getSuccessfulShards() > 0)
		{
			return true;
		}
		return false;
	}

	public Object searchByField(String inField, String inValue)
	{
		if (inField.equals("id") || inField.equals("_id"))
		{
			if (getPropertyDetails().getDetail("_parent") == null) //? what is this for? routing?
			{
				GetResponse response = getClient().prepareGet(toId(getCatalogId()), getSearchType(), inValue).execute().actionGet();
				if (response.isExists())
				{
					Map source = response.getSource();
					if (isDeleted(source))
					{
						return null;
					}

					Data data = null;
					if (getNewDataName() != null)
					{
						data = createNewData();
						// copyData(data, typed);
						updateData(source, data);
					}
					else
					{
						SearchHitData sdata = new SearchHitData(this);
						sdata.setSearchData(source);
						data = sdata;
						// data.setProperties(response.getSource());
						//updateData(response.getSource(), data);
					}
					// log.info(response.getSourceAsString());
					data.setId(inValue);
					// data.setName(data.getName());
					// data.setSourcePath(data.getSourcePath());

					if (response.getVersion() > -1)
					{
						data.setValue(".version", response.getVersion());
					}
					return loadData(data);
					//return data;
				}
				return null;
			}
		}
		return super.searchByField(inField, inValue);
	}

	protected boolean isDeleted(Map source)
	{
		Map status = (Map) source.get("emrecordstatus");
		if (status != null)
		{
			Object deleted = status.get("recorddeleted");
			if (deleted != null && Boolean.parseBoolean(String.valueOf(deleted)))
			{
				return true;
			}
		}
		return false;
	}

	protected void copyData(Data data, Data typed)
	{
		typed.setId(data.getId());
		typed.setName(data.getName());
		typed.setSourcePath(data.getSourcePath());
		Map<String, Object> props = data.getProperties();
		updateData(props, typed);
		// for (Iterator iterator = props.keySet().iterator();
		// iterator.hasNext();)
		// {
		// String key = (String) iterator.next();
		// Object obj = props.get(key);
		// typed.setProperty(key, String.valueOf(obj)); //TODO: use setValue ?
		// }
	}

	
	protected void populateKeywords(StringBuffer inFullDesc, Data inData, PropertyDetails inDetails)
	{
		Collection keywordFields = inDetails.findKeywordProperties();
		for (Iterator iter = keywordFields.iterator(); iter.hasNext();)
		{
			PropertyDetail det = (PropertyDetail) iter.next();
			if (det.isList())
			{
				Object prop = inData.getValue(det.getId());
				if (prop != null)
				{
					if (prop instanceof Collection)
					{
						Collection values = (Collection) prop;
						for (Iterator iterator = values.iterator(); iterator.hasNext();)
						{
							Object object = (Object) iterator.next();
							if (object instanceof String)
							{
								Data data = (Data) getSearcherManager().getCachedData(det.getListCatalogId(), det.getListId(), (String) object);
								if (data != null && data.getName() != null)
								{
									inFullDesc.append(data.getName());
								}
							}
							else
							{
								inFullDesc.append(String.valueOf(object));
							}
							inFullDesc.append("|");
						}
					}
					else if (prop instanceof String)
					{
						Data data = (Data) getSearcherManager().getCachedData(det.getListCatalogId(), det.getListId(), (String) prop);
						if (data != null && data.getName() != null)
						{
							inFullDesc.append(data.getName());
							inFullDesc.append("|");
						}
					}
				}
			}
			else
			{
				if (det.isMultiLanguage())
				{

					Object value = inData.getValue(det.getId());
					if (value instanceof String)
					{
						String target = (String) value;
						LanguageMap map = new LanguageMap();
						map.setText("en", target);
						value = map;

					}
					LanguageMap map = (LanguageMap) value;

					if (map != null)
					{
						HitTracker locales = getSearcherManager().getList(getCatalogId(), "locale");

						for (Iterator iterator2 = locales.iterator(); iterator2.hasNext();)
						{
							Data locale = (Data) iterator2.next();
							String id = locale.getId();
							String localeval = map.getText(id); // get a
																// location
																// specific
																// value
							if (localeval != null)
							{

								inFullDesc.append(localeval);
								inFullDesc.append("|");
							}

						}
					}
				}
				
				else if (det.isDataType("object")) {
					Object values = inData.getValue(det.getId());
					if (values != null && values instanceof String)
					{
						
					}
				}	
					
				
				else if (det.isDataType("objectarray") || det.isDataType("nested"))
				{
					Object values = inData.getValue(det.getId());
					if (values != null && values instanceof String)
					{
						//Spreadsheet import
						inFullDesc.append(values);
						inFullDesc.append("|");
						return;
					}

					if (values != null && det.getObjectDetails() != null)
					{
						Collection maps = (Collection) values;
						for (Iterator iterator = maps.iterator(); iterator.hasNext();)
						{
							Map map = (Map) iterator.next();
							for (Iterator miterator = det.getObjectDetails().iterator(); miterator.hasNext();)
							{
								PropertyDetail detal = (PropertyDetail) miterator.next();
								if (detal.isKeyword())
								{
									Object val = map.get(detal.getId());
									if (val != null)
									{
										if (detal.isMultiValue())
										{
											Collection colvalues = null;
											if (val instanceof Collection)
											{
												colvalues = (Collection) val;
											}
											else
											{
												colvalues = new ArrayList();
												colvalues.add(val);
											}
											//Could  be an collection
											for (Iterator iterator2 = colvalues.iterator(); iterator2.hasNext();)
											{
												String string = (String) String.valueOf(iterator2.next());

												if (detal.isList())
												{
													Data data = (Data) getSearcherManager().getCachedData(detal.getListCatalogId(), detal.getListId(), (String) string);
													if (data != null && data.getName() != null)
													{
														inFullDesc.append(data.getName());
														inFullDesc.append(' ');
													}
												}
												else
												{
													inFullDesc.append(string);
													inFullDesc.append(' ');
												}
											}
										}
										else
										{
											inFullDesc.append(String.valueOf(val));
											inFullDesc.append(' ');
										}
									}
								}

							}
						}
					}
				}
				else if (det.isMultiValue()) //But not a list
				{
					Collection values = inData.getValues(det.getId());
					if (values != null && !values.isEmpty())
					{
						for (Iterator iterator = values.iterator(); iterator.hasNext();)
						{
							String oneval = (String) iterator.next();
							inFullDesc.append(oneval);
							inFullDesc.append("|");
						}
					}
				}
				else
				{
					//Skip dates and lists? if( detail.isList() || detail.isDate() || detail.isMultiLanguage() || detail.isDataType("objectarray") || detail.isDataType("nested") || detail.getId().equals("description") )   //				else if (det.isDataType("objectarray") || det.isDataType("nested"))
					
					String val = inData.get(det.getId());
					if (val != null)
					{
						inFullDesc.append(val);
						inFullDesc.append("|");
					}
				}
			}
		}
	}
	public void reIndexAll() throws OpenEditException
	{
		// there is not reindex step since it is only in memory
		if (isReIndexing())
		{
			return;
		}
		synchronized (this)
		{
			try
			{
				setReIndexing(true);
				setOptimizeReindex(false);

				// putMappings(); //We can only try to put mapping. If this
				// failes then they will
				
				//TODO:  This is really bad code - this should be a ElasticListSearcher if it needs this, or we need some kind of
				//loadDefaults - I feel like if I specifically called my searcher a dataSearcher it would still potentially index old junk
				
				HitTracker allhits = (ElasticHitTracker) getAllHits();
				if (allhits.isEmpty())
				{
					//get them from XML as a backup
					XmlSearcher fieldXmlSearcher = (XmlSearcher) getModuleManager().getBean(getCatalogId(), "xmlSearcher");
					fieldXmlSearcher.setCatalogId(getCatalogId());
					fieldXmlSearcher.setSearchType(getSearchType());
					fieldXmlSearcher.setPropertyDetailsArchive(getPropertyDetailsArchive());
					allhits = fieldXmlSearcher.getAllHits();

				}
				allhits.enableBulkOperations();
				ArrayList tosave = new ArrayList();
				for (Iterator iterator2 = allhits.iterator(); iterator2.hasNext();)
				{
					Data hit = (Data) iterator2.next();
					if (hit.getId() == null || hit.getId().trim().isEmpty())
					{
						continue;
					}
					Data real = (Data) loadData(hit);

					tosave.add(real);
					if (tosave.size() > 1000)
					{
						updateIndex(tosave, null);
						tosave.clear();
					}
				}
				updateIndex(tosave, null);
				if (allhits instanceof ElasticHitTracker)
				{
					//Save memory
					ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
					clearScrollRequest.addScrollId(((ElasticHitTracker) allhits).getLastScrollId());
					getClient().clearScroll(clearScrollRequest).actionGet();
				}
				//System.gc();

			}
			finally
			{
				setReIndexing(false);
				setOptimizeReindex(true);
			}
		}
	}

	@Override
	public void restoreSettings()
	{
		getPropertyDetailsArchive().clearCustomSettings(getSearchType());
		// deleteOldMapping(); //you will lose your data!
		// reIndexAll();
		putMappings();
	}

	@Override
	public void reloadSettings()
	{
		// deleteOldMapping(); //you will lose your data!
		// reIndexAll();
		putMappings();

	}

	public void updateData(Map inSource, Data inData)
	{
		if (inData instanceof SearchData)
		{
			SearchData data = (SearchData) inData;
			data.setSearchData(inSource);
		}
		else
		{

			for (Iterator iterator = inSource.keySet().iterator(); iterator.hasNext();)
			{
				String key = (String) iterator.next();
				Object object = inSource.get(key);
				if (key.equals("name_int") && object != null && object instanceof HashMap)
				{
					object = new LanguageMap((Map) object);
				}
				inData.setValue(key, object);

			}
		}
	}

	public boolean tableExists()
	{
		boolean used = getClient().admin().indices().typesExists(new TypesExistsRequest(new String[] { toId(getCatalogId()) }, getSearchType())).actionGet().isExists();
		return used;

	}

	@Override
	public void reindexInternal() throws OpenEditException
	{
		//Manual Reindex
		
		HitTracker allhits = getAllIndexed();
		setReIndexing(true);
		try
		{
			int SIZE = 3000;

			allhits.enableBulkOperations();
			allhits.setHitsPerPage(SIZE);
			ArrayList tosave = new ArrayList();
			for (Iterator iterator2 = allhits.iterator(); iterator2.hasNext();)
			{
				Data hit = (Data) iterator2.next();
				if (hit.getId() == null || hit.getId().trim().isEmpty())
				{
					continue;
				}
				tosave.add(hit);
				if (tosave.size() > SIZE)
				{
					updateInBatch(tosave, null);

					tosave.clear();
				}
			}
			updateInBatch(tosave, null);
		}
		finally
		{
			setReIndexing(false);
		}

	}

	/**
	 * @override
	 */
	public String getConfigValue(String inKey)
	{
		MediaArchive archive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		return archive.getCatalogSettingValue(inKey);
	}

	public String getExistingMapping()
	{
		String cat = getCatalogId().replace("/", "_");
		String indexid = getElasticNodeManager().getIndexNameFromAliasName(cat);

		GetMappingsResponse getMappingsResponse = getElasticNodeManager().getClient().admin().indices().getMappings(new GetMappingsRequest().indices(indexid)).actionGet();
		ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> indexToMappings = getMappingsResponse.getMappings();

		MappingMetaData actualMapping = indexToMappings.get(indexid).get(getSearchType());
		if (actualMapping != null)
		{
			String jsonString;
			try
			{
				jsonString = actualMapping.source().string();
				//				JSONObject config = (JSONObject) new JSONParser().parse(returned);
				jsonString = JsonOutput.prettyPrint(jsonString);
				//				JSONObject json = new JSONObject(jsonString); // Convert text to object
				//				SjsonString = json.toString(4);
				return jsonString;
			}
			catch (IOException e)
			{
				new OpenEditException(e);
			}

		}
		return null;
	}

	protected Map checkTypes(Map inData)
	{
		for (Iterator iterator = inData.keySet().iterator(); iterator.hasNext();)
		{
			String type = (String) iterator.next();
			PropertyDetail detail = getDetail(type);
			if (detail != null)
			{
				if (detail.isDataType("objectarray") || detail.isDataType("nested"))
				{
					Object childdata = inData.get(type);
					if (childdata instanceof List)
					{
						Collection childdatalist = (List) childdata;
						for (Iterator iterator2 = childdatalist.iterator(); iterator2.hasNext();)
						{
							Map map = (Map) iterator2.next();
							for (Iterator iterator3 = detail.getObjectDetails().iterator(); iterator3.hasNext();)
							{
								PropertyDetail childdetail = (PropertyDetail) iterator3.next();
								fixTypes(map, childdetail);
							}
						}
					}
				}
				else
				{
					fixTypes(inData, detail);
				}
			}
		}
		return inData;
	}

	protected void fixTypes(Map inFields, PropertyDetail detail)
	{
		if (detail.isDataType("long"))
		{
			Object num = inFields.get(detail.getId());
			if (num != null)
			{
				if (num instanceof String)
				{
					num = Long.parseLong((String) num);
				}
				if (num instanceof Integer)
				{
					num = ((Integer) num).longValue();
				}
			}
			inFields.put(detail.getId(), num);
		}
	}

	public void saveJson(Collection inJsonArray)
	{
		BulkProcessor processor = getElasticNodeManager().getBulkProcessor();

		try
		{
			for (Iterator iterator = inJsonArray.iterator(); iterator.hasNext();)
			{
				JSONObject json = (JSONObject) iterator.next();

				IndexRequest req = Requests.indexRequest(getElasticIndexId()).type(getSearchType());
				req.source(json.toJSONString());
				//log.info("savinng " + json);
				//Parse the json and save it with id

				String id = (String) json.get("id");
				if (id != null)
				{
					req.id(id);
				}
				processor.add(req);
			}
			//processor.awaitClose(5, TimeUnit.MINUTES);  do in flushBulk
		}
		catch (Exception e)
		{
			throw new OpenEditException("Errors saving bulk data ", e);
		}
		finally
		{
			getElasticNodeManager().flushBulk();
		}
	}

	public void saveJson(String inID, JSONObject json)
	{

		BulkProcessor processor = getElasticNodeManager().getBulkProcessor();
		
		try
		{
			IndexRequest req = Requests.indexRequest(getElasticIndexId()).type(getSearchType());
			req.source(json.toJSONString());
			req.id(inID);

			processor.add(req);
			//processor.awaitClose(5, TimeUnit.MINUTES);  do in flushBulk
		}
		catch (Exception e)
		{
			throw new OpenEditException("Errors saving bulk data ", e);
		}
		finally
		{
			getElasticNodeManager().flushBulk();
			
		}
		
		getCacheManager().remove("data" + getSearchType(), inID);

	}

	public HitTracker getAllIndexed()
	{
		SearchRequestBuilder search = getClient().prepareSearch(toId(getCatalogId()));
		search.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
		search.setTypes(getSearchType());
		search.setRequestCache(true);
		QueryBuilder findall = QueryBuilders.matchAllQuery();

		//TODO: Dont include deleted...
		if (getDetail("emrecordstatus") == null)
		{
			search.setQuery(findall);
		}
		else
		{
			BoolQueryBuilder bool = QueryBuilders.boolQuery();
			TermQueryBuilder deleted = QueryBuilders.termQuery("emrecordstatus.recorddeleted", true);
			bool.must(findall);
			bool.mustNot(deleted);
			search.setQuery(bool);
		}

		ElasticHitTracker hits = new ElasticHitTracker(getClient(), search, findall, 1000);
		hits.enableBulkOperations();
		hits.setSearcherManager(getSearcherManager());
		//String inIndexId = toId(getCatalogId());
		hits.setIndexId(getIndexId());
		hits.setCatalogId(getCatalogId());
		return hits;
	}

	@Override
	public String getFulltext(Data inSearchHitData)
	{
		return getFulltext(inSearchHitData, getSearchType());
	}

	public String getFulltext(Data inSearchHitData, String type)
	{
		StringBuffer out = new StringBuffer();
		populateFullText(inSearchHitData, type, out);
		return out.toString();
	}

	protected void populateFullText(Data data, String datatype, StringBuffer fullDesc)
	{
		if (isIncludeFullText() && Boolean.parseBoolean(data.get("hasfulltext")))
		{
			//Legacy support 
			if (datatype.equals("asset"))
			{
				datatype = "assets"; //TODO: Move everyone over
			}

			String path = "/WEB-INF/data/" + getCatalogId() + "/" + datatype + "/" + data.getSourcePath() + "/fulltext.txt";
			ContentItem item = getPageManager().getRepository().getStub(path);
			if (item.exists())
			{
				Reader input = null;
				try
				{
					input = new InputStreamReader(item.getInputStream(), "UTF-8");
					StringWriter output = new StringWriter();
					getFiller().setMaxSize(getFullTextCap());
					getFiller().fill(input, output);
					fullDesc.append(output.toString());
				}
				catch (IOException ex)
				{
					log.error(ex);
				}
				finally
				{
					getFiller().close(input);
				}
			}
		}
	}

	protected void addAggregations(WebPageRequest inPageRequest, SearchQuery inSearch)
	{

		String aggs = inPageRequest.findValue("aggs");
		if (aggs == null)
		{
			aggs = (String) inPageRequest.getPageValue("aggs");
		}
		if (aggs != null)
		{
			ElasticSearchQuery search = (ElasticSearchQuery) inSearch;
			search.setAggregationJson(aggs);
		}
	}

	//	protected void assignCategoryPermissions(Set inCategories, Data inAsset) {
	//	   //Search 
	//		
	//		HashSet viewusers = new HashSet();
	//	    HashSet viewgroups = new HashSet();
	//	    HashSet viewroles = new HashSet();
	//
	//	    
	//	    
	//	    for (Iterator iterator = inAsset.getCategories().iterator(); iterator.hasNext();) {
	//	        Category cat = (Category) iterator.next();
	//
	//	        viewusers.addAll(cat.findValues("viewusers") != null ? cat.findValues("viewusers") : Collections.emptySet());
	//	        viewgroups.addAll(cat.findValues("viewgroups") != null ? cat.findValues("viewgroups") : Collections.emptySet());
	//	        viewroles.addAll(cat.findValues("viewroles") != null ? cat.findValues("viewroles") : Collections.emptySet());
	//	    }
	//
	//	    inAsset.setValue("viewusers", viewusers);
	//	    inAsset.setValue("viewgroups", viewgroups);
	//	    inAsset.setValue("viewroles", viewroles);
	//	}

}
