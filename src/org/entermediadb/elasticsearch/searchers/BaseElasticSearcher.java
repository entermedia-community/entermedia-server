package org.entermediadb.elasticsearch.searchers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsRequest;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.flush.FlushResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.RemoteTransportException;
import org.entermediadb.elasticsearch.ElasticHitTracker;
import org.entermediadb.elasticsearch.ElasticNodeManager;
import org.entermediadb.elasticsearch.ElasticSearchQuery;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.BaseData;
import org.openedit.data.BaseSearcher;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.Join;
import org.openedit.hittracker.SearchQuery;
import org.openedit.hittracker.Term;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.IntCounter;

public class BaseElasticSearcher extends BaseSearcher
{

	private static final Log log = LogFactory.getLog(BaseElasticSearcher.class);
	protected ElasticNodeManager fieldElasticNodeManager;
	//protected IntCounter fieldIntCounter;
	//protected PageManager fieldPageManager;
	//protected LockManager fieldLockManager;
	protected boolean fieldAutoIncrementId;
	protected boolean fieldReIndexing;
	protected boolean fieldCheckVersions;
	protected boolean fieldRefreshSaves = true;
	
	public boolean isRefreshSaves()
	{
		return fieldRefreshSaves;
	}

	public void setRefreshSaves(boolean inRefreshSaves)
	{
		fieldRefreshSaves = inRefreshSaves;
	}

	public static final Pattern VALUEDELMITER = Pattern.compile("\\s*\\|\\s*");

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
		ElasticSearchQuery query = new ElasticSearchQuery();
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
		if (isReIndexing())
		{
			int timeout = 0;
			while (isReIndexing())
			{
				try
				{
					Thread.sleep(250);
				}
				catch (InterruptedException ex)
				{
					log.error(ex);
				}
				timeout++;
				if (timeout > 100)
				{
					throw new OpenEditException("timeout on search while reindexing" + getSearchType());
				}
			}
		}
		String json = null;
		try
		{
			if (!(inQuery instanceof ElasticSearchQuery))
			{
				throw new OpenEditException("Elastic search requires elastic query");
			}
			long start = System.currentTimeMillis();
			SearchRequestBuilder search = getClient().prepareSearch(toId(getCatalogId()));
			search.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
			search.setTypes(getSearchType());
			if (isCheckVersions())
			{
				search.setVersion(true);
			}
			QueryBuilder terms = buildTerms(inQuery);

			search.setQuery(terms);
			// search.
			addSorts(inQuery, search);
			addFacets(inQuery, search);

			ElasticHitTracker hits = new ElasticHitTracker(search, terms);

			hits.setIndexId(getIndexId());
			hits.setSearcher(this);
			hits.setSearchQuery(inQuery);

			//Infinite loop check
			if( getSearcherManager().getShowSearchLogs(getCatalogId()) )
			{
				//if (log.isDebugEnabled())
				if( true )
				{
					long size = hits.size(); //order is important
					json = search.toString();
					long end = System.currentTimeMillis() - start;
					log.info(toId(getCatalogId()) + "/" + getSearchType() + "/_search' -d '" + json + "' \n" + size + " hits in: " + (double) end / 1000D + " seconds]");
				}
				else
				{
					log.info(toId(getCatalogId()) + "/" + getSearchType() + " "  + hits.size() + " hits q=" + inQuery.toQuery() + " sort by " + inQuery.getSorts() );
				}
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
	protected void addFacets(SearchQuery inQuery, SearchRequestBuilder inSearch)
	{
		for (Iterator iterator = getPropertyDetails().iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();
			if (detail.isFilter())
			{
				AggregationBuilder b = AggregationBuilders.terms(detail.getId()).field(detail.getId()).size(10);
				inSearch.addAggregation(b);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	
	
	
	@Override
	public boolean initialize()
	{
		try
		{			
			if( getAllHits().size() == 0 )
			{
				reIndexAll();
//			} else{
//				putMappings();
			}
			log.info(getCatalogId() + " Node is ready for " + getSearchType());
		}
		catch (Exception ex)
		{
			log.error("index could not be created ", ex);
			return false;
		}
		return true;
	}


	
//	
//	protected void deleteOldMapping()
//	{
//		log.info("Does not work");
//	}
	
//		AdminClient admin = getElasticNodeManager().getClient().admin();
//		String indexid = toId(getCatalogId());
//		//XContentBuilder source = buildMapping();
//
//		//DeleteMappingRequest dreq = Requests.deleteMappingRequest(indexid).types(getSearchType());
//		try
//		{
//			DeleteMappingResponse dpres = admin.indices().deleteMapping(dreq).actionGet();
//			if (dpres.isAcknowledged())
//			{
//				log.info("Cleared out the mapping " + getSearchType() );
//				getClient().admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();
//			}
//		}	
//		catch (Throwable ex)
//		{
//			log.error(ex);
//		}
//	}
	public void putMappings(String indexid)
	{
		AdminClient admin = getElasticNodeManager().getClient().admin();
		
		boolean alreadyin = admin.indices().typesExists(new TypesExistsRequest(new String[] { indexid }, getSearchType())).actionGet().isExists();
		if(alreadyin){
			return;
		}
		if(indexid == null){
			 indexid = toId(getCatalogId());
		}
		List dependson = getPropertyDetails().getDependsOn();
		for (Iterator iterator = dependson.iterator(); iterator.hasNext();)
		{
			String searchtype = (String) iterator.next();
			Searcher child = getSearcherManager().getSearcher(getCatalogId(), searchtype);
			//child.reloadSettings();
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
		//		GetMappingsRequest find = new GetMappingsRequest().types(getSearchType()); 
		//		GetMappingsResponse found = admin.indices().getMappings(find).actionGet();
		//		if( !found.isContextEmpty())
		try
		{
			putMapping(admin, indexid, source);
			admin.cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();
			return;
		}
		catch( Exception ex)
		{
			//https://www.elastic.co/guide/en/elasticsearch/guide/current/scan-scroll.html
			//https://github.com/jprante/elasticsearch-knapsack
			log.info("Could not put mapping over existing mapping. Please use restoreDefaults",ex);
			getElasticNodeManager().addMappingError(getSearchType(),ex.getMessage());
			//throw new OpenEditException("Mapping was not able to be merged, you will need to export data");
		}
//		try
//		{
//			//Save existing index values
//			HitTracker all = getAllHits();
//			//Export to csv file?
//			
//			DeleteMappingRequest dreq = Requests.deleteMappingRequest(indexid).types(getSearchType());
//			DeleteMappingResponse dpres = admin.indices().deleteMapping(dreq).actionGet();
//			if (dpres.isAcknowledged())
//			{
//				log.info("Cleared out the mapping " + getSearchType() );
//				getClient().admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();
//				putMapping(admin, indexid, source);
//			}
//			//save it all back
//		}
//		catch( Throwable ex)
//		{
//			log.info("failed to clear mapping before reloading ",ex);
//		}
	}

	protected void putMapping(AdminClient admin, String indexid, XContentBuilder source)
	{
		PutMappingRequest req = Requests.putMappingRequest(indexid).updateAllTypes(true).type(getSearchType());
		req = req.source(source);
	
		req.validate();
		PutMappingResponse pres = admin.indices().putMapping(req).actionGet();
		
		if (pres.isAcknowledged())
		{
			//log.info("mapping applied " + getSearchType());
			admin.cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();
		}
		
	}

	protected XContentBuilder buildMapping()
	{
		try
		{
			XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
			XContentBuilder jsonproperties = jsonBuilder.startObject().startObject(getSearchType());
			jsonproperties.field("date_detection", "false");
			jsonproperties = jsonproperties.startObject("properties");

			List props = getPropertyDetails().findIndexProperties();
			if (props.size() == 0)
			{
				throw new OpenEditException("No fields defined for " + getSearchType());
			}
			// https://github.com/elasticsearch/elasticsearch/pull/606
			// https://gist.github.com/870714
			/*
			 * index.analysis.analyzer.lowercase_keyword.type=custom
			 * index.analysis.analyzer.lowercase_keyword.filter.0=lowercase
			 * index.analysis.analyzer.lowercase_keyword.tokenizer=keyword
			 */

			jsonproperties = jsonproperties.startObject("_all");
			jsonproperties = jsonproperties.field("store", "false");
			jsonproperties = jsonproperties.field("analyzer", "lowersnowball");
			//jsonproperties = jsonproperties.field("index_analyzer", "lowersnowball");
			//jsonproperties = jsonproperties.field("search_analyzer", "lowersnowball"); // lower
																						// case
																						// does
																						// not
																						// seem
																						// to
																						// work
			jsonproperties = jsonproperties.field("index", "analyzed");
			jsonproperties = jsonproperties.field("type", "string");
			jsonproperties = jsonproperties.endObject();

			// Add in namesorted
			if (getPropertyDetails().contains("name") && !getPropertyDetails().contains("namesorted"))
			{
				props = new ArrayList(props);
				PropertyDetail detail = new PropertyDetail();
				detail.setId("namesorted");
				props.add(detail);
			}

			for (Iterator i = props.iterator(); i.hasNext();)
			{
				PropertyDetail detail = (PropertyDetail) i.next();

				if ("_id".equals(detail.getId()) || "id".equals(detail.getId()))
				{
//					jsonproperties = jsonproperties.startObject("_id");
//					jsonproperties = jsonproperties.field("index", "not_analyzed");
//					jsonproperties = jsonproperties.field("type", "_id");
//					jsonproperties = jsonproperties.endObject();
					continue;
				}
				if ("_parent".equals(detail.getId()))
				{
					continue;
				}
				jsonproperties = jsonproperties.startObject(detail.getId());
				if ("description".equals(detail.getId()))
				{
					String analyzer = "lowersnowball";
					jsonproperties = jsonproperties.field("analyzer", analyzer);
					jsonproperties = jsonproperties.field("type", "string");
					jsonproperties = jsonproperties.field("index", "analyzed");
					jsonproperties = jsonproperties.field("store", "no");
					jsonproperties = jsonproperties.field("include_in_all", "false");
					jsonproperties = jsonproperties.endObject();

					continue;
				}
				if (detail.isDate())
				{
					jsonproperties = jsonproperties.field("type", "date");
					//"date_detection" : 0
					// jsonproperties = jsonproperties.field("format",
					// "yyyy-MM-dd HH:mm:ss Z");
				}
				else if (detail.isBoolean())
				{
					jsonproperties = jsonproperties.field("type", "boolean");
				}
				else if (detail.isDataType("number"))
				{
					jsonproperties = jsonproperties.field("type", "long");
				}
				else if (detail.isList())  //Or multi valued?
				{
					//if( detail.isMultiValue() )
					jsonproperties = jsonproperties.field("index", "not_analyzed");
					if (Boolean.parseBoolean(detail.get("nested")))
					{
						jsonproperties = jsonproperties.field("type", "nested");
					}
					else
					{
						jsonproperties = jsonproperties.field("type", "string");
					}
				}
				else
				{
					String indextype = detail.get("indextype");
					if( indextype == null )
					{
						if( detail.getId().endsWith("id") || detail.getId().contains("sourcepath"))
						{
							indextype = "not_analyzed";
						}
					}
					if (indextype != null )
					{
						jsonproperties = jsonproperties.field("index", indextype);
					}
					jsonproperties = jsonproperties.field("type", "string");
				}

				if (detail.isStored())
				{
					jsonproperties = jsonproperties.field("store", "yes");
				}
				else
				{
					jsonproperties = jsonproperties.field("store", "no");
				}
				// this does not work yet
				// if( detail.isKeyword())
				// {
				// jsonproperties = jsonproperties.field("include_in_all", "true");
				// }
				// else
				// {
				jsonproperties = jsonproperties.field("include_in_all", "false");

				// }

				jsonproperties = jsonproperties.endObject();
				
			}
			jsonproperties = jsonproperties.endObject();
			PropertyDetail _parent = getPropertyDetails().getDetail("_parent");
			if( _parent != null)
			{
				jsonproperties = jsonproperties.startObject("_parent");
				jsonproperties = jsonproperties.field("type", _parent.getListId());
				jsonproperties = jsonproperties.endObject();
			}	
			jsonBuilder = jsonproperties.endObject();
			return jsonproperties;
		}
		catch (Throwable ex)
		{
			throw new OpenEditException(ex);
		}

	}

	protected QueryBuilder buildTerms(SearchQuery inQuery)
	{

		if (inQuery.getTerms().size() == 1 && inQuery.getChildren().size() == 0 && inQuery.getParentJoins() == null)  //Shortcut for common cases
		{
			Term term = (Term) inQuery.getTerms().iterator().next();

			if ("orgroup".equals(term.getOperation()) || "orsGroup".equals(term.getOperation())) //orsGroup? 
			{
				return addOrsGroup(term);
			}

			String value = term.getValue();

			if (value != null && value.equals("*"))
			{
				return QueryBuilders.matchAllQuery();
			}
			QueryBuilder find = buildTerm(term.getDetail(), term, value);
			return find;
		}

		BoolQueryBuilder bool = QueryBuilders.boolQuery();

		if (inQuery.isAndTogether())
		{
			// TODO: Deal with subqueries, or, and, not
			for (Iterator iterator = inQuery.getTerms().iterator(); iterator.hasNext();)
			{
				Term term = (Term) iterator.next();
				if ("orgroup".equals(term.getOperation()) || "orsGroup".equals(term.getOperation()))
				{
					BoolQueryBuilder or = addOrsGroup(term);
					bool.must(or);
				}
				else
				{
					String value = term.getValue();
					QueryBuilder find = buildTerm(term.getDetail(), term, value);
					if (find != null)
					{
						bool.must(find);
					}
				}
			}
		}
		else
		{
			for (Iterator iterator = inQuery.getTerms().iterator(); iterator.hasNext();)
			{
				Term term = (Term) iterator.next();
				if ("orgroup".equals(term.getOperation()) || "orsGroup".equals(term.getOperation())) //orsGroup?
				{
					BoolQueryBuilder or = addOrsGroup(term);
					bool.should(or);
				}
				else
				{
					String value = term.getValue();
					QueryBuilder find = buildTerm(term.getDetail(), term, value);
					if (find != null)
					{
						bool.should(find);
					}
				}
			}
		}

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
		if (inQuery.getParentJoins() != null)
		{
			for (Iterator iterator = inQuery.getParentJoins().iterator(); iterator.hasNext();)
			{
				Join join = (Join) iterator.next();
				QueryBuilder parent = QueryBuilders.termQuery(join.getChildColumn(), join.getEqualsValue() );
				QueryBuilder haschild = QueryBuilders.hasChildQuery(join.getChildTable(), parent);
				bool.must(haschild);
			}
		}
		return bool;

	}

	protected BoolQueryBuilder addOrsGroup(Term term)
	{
		if (term.getValues() != null)
		{
			BoolQueryBuilder or = QueryBuilders.boolQuery();
			for (int i = 0; i < term.getValues().length; i++)
			{
				Object val = term.getValues()[i];
				QueryBuilder aterm = buildTerm(term.getDetail(), term, val);
				if (aterm != null)
				{
					or.should(aterm);
				}
			}
			return or;
		}
		return null;
	}

	protected QueryBuilder buildTerm(PropertyDetail inDetail, Term inTerm, Object inValue)
	{
		QueryBuilder find = buildNewTerm(inDetail, inTerm, inValue);
		if ("not".equals(inTerm.getOperation()))
		{
			BoolQueryBuilder or = QueryBuilders.boolQuery();
			or.mustNot(find);
			return or;
		}
		return find;
	}

	protected QueryBuilder buildNewTerm(PropertyDetail inDetail, Term inTerm, Object inValue)
	{
		// Check for quick date object
		QueryBuilder find = null;
		String valueof = null;

		if (inValue instanceof Date)
		{
			valueof = DateStorageUtil.getStorageUtil().formatForStorage((Date) inValue);
		}
		else
		{
			valueof = String.valueOf(inValue);
		}

		String fieldid = inDetail.getId();
		// if( fieldid.equals("description"))
		// {
		// //fieldid = "_all";
		// //valueof = valueof.toLowerCase();
		// find = QueryBuilders.textQuery(fieldid, valueof);
		// return find;
		// }
		if (fieldid.equals("id"))
		{
			// valueof = valueof.toLowerCase();
			if (valueof.equals("*"))
			{
				find = QueryBuilders.matchAllQuery();
			}
			else
			{
				find = QueryBuilders.termQuery("_id", valueof);
			}
			return find;
		}

		if (valueof.equals("*"))
		{
			find = QueryBuilders.matchAllQuery();
			//ExistsFilterBuilder filter = FilterBuilders.existsFilter(fieldid);
			//find = QueryBuilders.filteredQuery(all, filter);

		}
		else if ("contains".equals(inTerm.getOperation()))
		{
			//MatchQueryBuilder text = QueryBuilders.matchPhraseQuery(fieldid, valueof);
			//QueryBuilder text = QueryBuilders.queryString("*" + valueof + "*").field(fieldid);
			if(!valueof.startsWith("*") )
			{
				valueof = "*"  + valueof;
			}
			if(!valueof.endsWith("*") )
			{
				valueof = valueof + "*";
			}
				
			WildcardQueryBuilder text = QueryBuilders.wildcardQuery(fieldid, valueof);
			//text.maxExpansions(10);
			find = text;
		}
		else if ("startswith".equals(inTerm.getOperation()))
		{
			MatchQueryBuilder text = QueryBuilders.matchPhrasePrefixQuery(fieldid, valueof);
			text.maxExpansions(10);
			find = text;
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
			find = QueryBuilders.wildcardQuery(fieldid, valueof);
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
				find = QueryBuilders.rangeQuery(inDetail.getId()).to(before);
			}
			else if ("afterdate".equals(inTerm.getOperation()))
			{
				Date before = new Date(Long.MAX_VALUE);
				Date after = DateStorageUtil.getStorageUtil().parseFromStorage(valueof);
				find = QueryBuilders.rangeQuery(fieldid).from(after);// .to(before);
			}
			else if ("betweendates".equals(inTerm.getOperation()))
			{
				// String end =
				// DateStorageUtil.getStorageUtil().formatForStorage(new
				// Date(Long.MAX_VALUE));
				Date after = DateStorageUtil.getStorageUtil().parseFromStorage(inTerm.getParameter("afterDate"));
				Date before = DateStorageUtil.getStorageUtil().parseFromStorage(inTerm.getParameter("beforeDate"));

				// inTerm.getParameter("beforeDate");

				// String before
				find = QueryBuilders.rangeQuery(fieldid).from(after).to(before);
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
		}
		else if (inDetail.isDataType("number"))
		{
			find = QueryBuilders.termQuery(fieldid, Long.parseLong(valueof));
		}
		else if (fieldid.equals("description"))
		{
			// valueof = valueof.substring(0,valueof.length()-1);

			MatchQueryBuilder text = QueryBuilders.matchPhrasePrefixQuery("_all", valueof);
			text.maxExpansions(10);
			find = text;
		}
		else
		{
			if ("matches".equals(inTerm.getOperation()))
			{
				find = QueryBuilders.matchQuery(fieldid, valueof); //this is analyzed
				//find = QueryBuilders.termQuery(fieldid, valueof);
			}
			else if ("contains".equals(inTerm.getOperation()))
			{
				find = QueryBuilders.matchQuery(fieldid, valueof);
			}
			else
			{
				find = QueryBuilders.matchQuery(fieldid, valueof); //This is not analyzed termQuery
				//find = QueryBuilders.termQuery(fieldid, valueof);
			}
		}
		// QueryBuilders.idsQuery(types)
		return find;
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
			FieldSortBuilder sort = SortBuilders.fieldSort(field);
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

		return "singleton";
	}

	public void clearIndex()
	{

	}

	public void saveData(Data inData, User inUser)
	{
		// update the index
		//List<Data> list = new ArrayList(1);
		//list.add((Data) inData);
		//saveAllData(list, inUser);
		PropertyDetails details = getPropertyDetailsArchive().getPropertyDetailsCached(getSearchType());
		updateElasticIndex(details, inData);
 
	}
/*
	protected void bulkUpdateIndex(Collection<Data> inBuffer, User inUser)
	{
		try
		{
			String catid = toId(getCatalogId());

			// BulkRequestBuilder brb = getClient().prepareBulk();
			// brb.add(Requests.indexRequest(indexName).type(getIndexType()).id(id).source(source));
			// }
			// if (brb.numberOfActions() > 0) brb.execute().actionGet();
			PropertyDetails details = getPropertyDetailsArchive().getPropertyDetailsCached(getSearchType());

			BulkRequestBuilder bulkRequest = getClient().prepareBulk();

			for (Data data : inBuffer)
			{
				XContentBuilder content = XContentFactory.jsonBuilder().startObject();

				updateIndex(content, data, details);

				content.endObject();
				if (data.getId() == null)
				{
					IndexRequestBuilder builder = getClient().prepareIndex(catid, getSearchType()).setSource(content);
					updateVersion(data, builder);
					bulkRequest.add(builder);
				}
				else
				{
					IndexRequestBuilder builder = getClient().prepareIndex(catid, getSearchType(), data.getId()).setSource(content);
					updateVersion(data, builder);

					bulkRequest.add(builder);
				}
			}

			BulkResponse bulkResponse = bulkRequest.setRefresh(true).execute().actionGet();
			if (bulkResponse.hasFailures())
			{
				log.info("Failures detected!");
				throw new OpenEditException("failure during batch update");

			}
			log.info("Saved " + inBuffer.size() + " records into " + catid + "/" + getSearchType());
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
	}
*/
	public void updateIndex(Collection<Data> inBuffer, User inUser)
	{
		if( inBuffer.size() > 99 )
		{
			updateInBatch( inBuffer, inUser);  //This is asynchronous
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
				updateElasticIndex(details, data);
			}
		}
		//inBuffer.clear();
	}

	public void updateInBatch(Collection<Data> inBuffer, User inUser)
	{
		String catid = toId(getCatalogId());

		//We cant use this for normal updates since we do not get back the id or the version for new data object
		
		//final Map<String, Data> toversion = new HashMap(inBuffer.size());
		final List<Data> toprocess = new ArrayList(inBuffer);
		final List errors = new ArrayList();
		//Make this not return till it is finished?
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
					//request.getFromContext(key)
					BulkItemResponse res = response.getItems()[i];
					//Data toupdate = toversion.get(res.getId());
					Data toupdate = toprocess.get(res.getItemId());
					if( toupdate != null)
					{
						if (isCheckVersions())
						{
							toupdate.setProperty(".version", String.valueOf(res.getVersion()));
						}
						toupdate.setId(res.getId());
					}
				}
			}

			@Override
			public void afterBulk(long executionId, BulkRequest request, Throwable failure)
			{
				log.error(failure);
				errors.add( failure );
			}
		}).setBulkActions(inBuffer.size()).setBulkSize(new ByteSizeValue(1, ByteSizeUnit.GB)).setFlushInterval(TimeValue.timeValueSeconds(5)).setConcurrentRequests(2).build();

		PropertyDetails details = getPropertyDetailsArchive().getPropertyDetailsCached(getSearchType());

		for (Iterator iterator = inBuffer.iterator(); iterator.hasNext();)
		{
			try
			{
				Data data2 = (Data) iterator.next();
				XContentBuilder content = XContentFactory.jsonBuilder().startObject();
				updateIndex(content, data2, details);
				content.endObject();
				IndexRequest req = Requests.indexRequest(catid).type(getSearchType());
				if( data2.getId() != null)
				{
					req = req.id(data2.getId());
				}
				req = req.source(content);
				if( isRefreshSaves() )
				{
					req = req.refresh(true);
				}
				bulkProcessor.add(req);
			}
			catch (Exception ex)
			{
				log.error(ex);
			}
		}
		bulkProcessor.close();

		if( errors.size() > 0)
		{
			throw new OpenEditException((Throwable)errors.get(0));
		}
		// ConcurrentModificationException
		//builder = builder.setSource(content).setRefresh(true);
		// BulkRequestBuilder brb = getClient().prepareBulk();
		//
		// brb.add(Requests.indexRequest(indexName).type(getIndexType()).id(id).source(source));
		// }
		// if (brb.numberOfActions() > 0) brb.execute().actionGet();
	}

	protected void updateElasticIndex(PropertyDetails details, Data data)
	{
		try
		{
			String catid = toId(getCatalogId());
			IndexRequestBuilder builder = null;
			if (data.getId() == null)
			{
				builder = getClient().prepareIndex(catid, getSearchType());
			}
			else
			{
				builder = getClient().prepareIndex(catid, getSearchType(), data.getId());
			}
			PropertyDetail parent = details.getDetail("_parent");
			if( parent != null)
			{
				String _parent = data.get(parent.getListId());
				if( _parent != null)
				{
					builder = builder.setParent(_parent);
				}
			}
			
			XContentBuilder content = XContentFactory.jsonBuilder().startObject();
			
			
			updateIndex(content, data, details);
			content.endObject();
			if( log.isDebugEnabled() )
			{
				log.debug("Saving " + getSearchType() + " " + data.getId() + " = " + content.string());
			}

			builder = builder.setSource(content);
			if( isRefreshSaves() )
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
			data.setProperty(".version", String.valueOf(response.getVersion()));
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
			if (ex instanceof OpenEditException)
			{
				throw (OpenEditException) ex;
			}
			throw new OpenEditException(ex);
		}
	}

	private void updateVersion(Data data, IndexRequestBuilder builder)
	{
		if (isCheckVersions())
		{
			String version = data.get(".version");
			if (version != null)
			{
				long val = Long.parseLong(version);
				if (val > -1)
				{
					builder.setVersion(val);
				}
			}
		}
	}

	protected void updateIndex(XContentBuilder inContent, Data inData, PropertyDetails inDetails)
	{
		if (inData == null)
		{
			log.info("Null Data");
		}
		populateDoc(inContent, inData, inDetails);
	}
	
	protected void populateDoc(XContentBuilder inContent, Data inData, PropertyDetails inDetails){
		
		
		
//		Map props = inData.getProperties();
//		HashSet everything = new HashSet(props.keySet());
//		everything.add("id");
//		everything.add("name");
//		everything.add("sourcepath");
//		for (Iterator iterator = inDetails.iterator(); iterator.hasNext();)
//		{
//			PropertyDetail detail = (PropertyDetail) iterator.next();
//			everything.add(detail.getId());// We need this to handle booleans
//											// and potentially other things.
//
//		}
//		everything.remove(".version"); // is this correct?
		for (Iterator iterator = inDetails.getDetails().iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();
			if (!detail.isIndex())
			{
				continue;
			}

			String key = detail.getId();
			String value = inData.get(key);
			
//			if(key.contains(".")){
//				log.info("Warning - data id : " + inData.getId() + " of type " + getSearchType() + " contained a . in field " + key);
//				key = key.replace(".", "_");
//			}
			
			if (value != null && value.trim().length() == 0)
			{
				value = null;
			}
			try
			{
				if ("_id".equals(key) || "_parent".equals(key))
				{
					// if( value != null)
					// {
					// inContent.field("_id", value);
					// continue;
					// }
					continue;
				}

				if (detail != null && detail.isDate())
				{
					if (value != null)
					{
						// ie date =
						// DateStorageUtil.getStorageUtil().parseFromStorage(value);
						Date date = DateStorageUtil.getStorageUtil().parseFromStorage(value);
						if (date != null)
						{
							inContent.field(key, date);
						}

					}
				}
				else if (detail != null && detail.isBoolean())
				{
					if (value == null)
					{
						inContent.field(key, Boolean.FALSE);
					}
					else
					{
						inContent.field(key, Boolean.valueOf(value));
					}
				}
				else if (detail != null && detail.isDataType("number"))
				{
					if (value == null)
					{
						inContent.field(key, Long.valueOf(0));
					}
					else
					{
						inContent.field(key, Long.valueOf(value));
					}
				}

				else if (detail != null && detail.isList())
				{

					ArrayList values = new ArrayList();
					if (value != null && value.contains("|"))
					{
						String[] vals = VALUEDELMITER.split(value);

						inContent.field(key, vals);
					}
					else
					{
						inContent.field(key, value);
					}
				}

				else if (key.equals("description"))
				{
					StringBuffer desc = new StringBuffer();
					populateKeywords(desc, inData, inDetails);
					if (desc.length() > 0)
					{
						inContent.field(key, desc.toString());
					}
				}
				else if (key.equals("name"))
				{
					// This matches how we do it on Lucene
					if (value != null)
					{
						inContent.field(key, value);
						inContent.field(key + "sorted", value);
					}
				}
				else
				{
					if (value == null)
					{
						// inContent.field(key, ""); // this ok?
					}
					else
					{
						inContent.field(key, value);
					}
				}
				// log.info("Saved" + key + "=" + value );
			}
			catch (Exception ex)
			{
				throw new OpenEditException(ex);
			}
		}
	}

	public void deleteAll(User inUser)
	{
		
		
		//https://github.com/elastic/elasticsearch/blob/master/plugins/delete-by-query/src/main/java/org/elasticsearch/action/deletebyquery/TransportDeleteByQueryAction.java#L104
		
		log.info("Deleted all records database " + getSearchType());
//		DeleteByQueryRequestBuilder delete = getClient().prepareDeleteByQuery(toId(getCatalogId()));
//		delete.setTypes(getSearchType());
//		delete.setQuery(new MatchAllQueryBuilder()).execute().actionGet();
		for (Iterator iterator = getAllHits().iterator(); iterator.hasNext();)
		{
			Data row = (Data) iterator.next();
			delete(row,null);
		}
		
	}

	public void delete(Data inData, User inUser)
	{
		String id = inData.getId();

		DeleteRequestBuilder delete = getClient().prepareDelete(toId(getCatalogId()), getSearchType(), id);
		delete.setRefresh(true).execute().actionGet();

	}

	// Base class only updated the index in bulk
	public void saveAllData(Collection<Data> inAll, User inUser)
	{
		updateIndex(inAll, inUser);
	}

	public synchronized String nextId()
	{
		//		Lock lock = getLockManager().lock(getCatalogId(), loadCounterPath(), "admin");
		//		try
		//		{
		//			return String.valueOf(getIntCounter().incrementCount());
		//		}
		//		finally
		//		{
		//			getLockManager().release(getCatalogId(), lock);
		//		}
		throw new OpenEditException("Should not call next ID");
	}

	protected IntCounter getIntCounter()
	{
		//		if (fieldIntCounter == null)
		//		{
		//			fieldIntCounter = new IntCounter();
		//			// fieldIntCounter.setLabelName(getSearchType() + "IdCount");
		//			Page prop = getPageManager().getPage(loadCounterPath());
		//			File file = new File(prop.getContentItem().getAbsolutePath());
		//			file.getParentFile().mkdirs();
		//			fieldIntCounter.setCounterFile(file);
		//		}
		//		return fieldIntCounter;
		throw new OpenEditException("Cant load int counters from elasticsearch");
	}

	/** TODO: Update this location to match the new standard location */
	protected String loadCounterPath()
	{
		return "/WEB-INF/data/" + getCatalogId() + "/" + getSearchType() + "s/idcounter.properties";
	}

	public boolean hasChanged(HitTracker inTracker)
	{
		//We dont cache results because another node might have edited a record
		//We could cache by a timer? risky
		return true;
	}

	public HitTracker checkCurrent(WebPageRequest inReq, HitTracker inTracker) throws OpenEditException
	{
		return inTracker;
	}

	protected boolean flushChanges()
	{
		FlushRequest req = Requests.flushRequest(toId(getCatalogId()));
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
			GetResponse response = getClient().prepareGet(toId(getCatalogId()), getSearchType(), inValue).execute().actionGet();
			if (response.isExists())
			{
				Data data = new BaseData(response.getSource());
				if( getNewDataName() != null )
				{
					Data typed = createNewData();		
					copyData(data, typed);
					data = typed;
				}	
				
				data.setId(inValue);
				if (response.getVersion() > -1)
				{
					data.setProperty(".version", String.valueOf(response.getVersion()));
				}
				return data;
			}
			return null;
		}
		return super.searchByField(inField, inValue);
	}

	protected void copyData(Data data, Data typed)
	{
		typed.setId(data.getId());
		typed.setName(data.getName());
		typed.setSourcePath(data.getSourcePath());
		Map<String,Object> props = data.getProperties();
		for (Iterator iterator = props.keySet().iterator(); iterator.hasNext();)
		{
			String	key = (String) iterator.next();
			Object obj = props.get(key);
			typed.setProperty(key,String.valueOf(obj));
		}
	}


	protected void populateKeywords(StringBuffer inFullDesc, Data inData, PropertyDetails inDetails)
	{
		for (Iterator iter = inDetails.findKeywordProperties().iterator(); iter.hasNext();)
		{
			PropertyDetail det = (PropertyDetail) iter.next();
			if (det.isList())
			{
				String prop = inData.get(det.getId());
				if (prop != null)
				{
					Data data = (Data) getSearcherManager().getData(det.getListCatalogId(), det.getListId(), prop);
					if (data != null && data.getName() != null)
					{
						inFullDesc.append(data.getName());
						inFullDesc.append(' ');
					}
				}
			}
			else
			{
				String val = inData.get(det.getId());
				if (val != null)
				{
					inFullDesc.append(val);
					inFullDesc.append(' ');
				}
			}
		}
	}

	public void reIndexAll() throws OpenEditException
	{
		//there is not reindex step since it is only in memory
		if (isReIndexing())
		{
			return;
		}
		try
		{
			setReIndexing(true);
			putMappings(toId(getCatalogId())); //We can only try to put mapping. If this failes then they will
			//need to export their data and factory reset the fields 
			//deleteAll(null); //This only deleted the index
		}
		finally
		{
			setReIndexing(false);
		}
	}
	
	@Override
	public void restoreSettings()
	{
		getPropertyDetailsArchive().clearCustomSettings(getSearchType());
		//deleteOldMapping();  //you will lose your data!
		//reIndexAll();
		putMappings(toId(getCatalogId()));
	}
	
	@Override
	public void reloadSettings()
	{
		//deleteOldMapping();  //you will lose your data!
		//reIndexAll();
		putMappings(toId(getCatalogId()));
		
		
	}
	
	public HitTracker loadHits(WebPageRequest inReq)
	{
		String id = inReq.findValue("hitssessionid");
		if (id != null)
		{
			HitTracker tracker = (HitTracker) inReq.getSessionValue(id);
			boolean runsearch = false;
			String clear = inReq.getRequestParameter(getSearchType() + "clearselection");
			if( clear != null)
			{
				runsearch = true;
			}
			else
			{
				String showonly = inReq.getRequestParameter(getSearchType() + "showonlyselections");
				if(showonly != null)
				{
					runsearch = true;
				}
			}
			if (tracker != null)
			{
				if( runsearch )
				{
					tracker = cachedSearch(inReq, tracker.getSearchQuery()); //only run search when using cachedSearch
				}
				String hitsname = inReq.findValue("hitsname");
				if( hitsname == null)
				{
					hitsname = tracker.getHitsName();
				}
				inReq.putPageValue(hitsname, tracker);
			}
			return tracker;
		}
		return null;
	}

	
	
	
	
	public void updateData(Data inData, Map inSource){
		for (Iterator iterator = inSource.keySet().iterator(); iterator.hasNext();)
		{
			String key = (String) iterator.next();
			Object object = inSource.get(key);
			if("category-exact".equals(key)){
				continue;
			}
			String val = null;
			if (object instanceof String) {
				val= (String) object;
			}
			if (object instanceof Date) {
				val= String.valueOf((Date) object);
			}
			if (object instanceof Boolean) {
				val= String.valueOf((Boolean) object);
			}
			if (object instanceof Integer) {
				val= String.valueOf((Integer) object);
			}
			if (object instanceof Float) {
				val= String.valueOf((Float) object);
			}
			if (object instanceof Collection) {
				continue;
//				Collection values = (Collection) object;
//				inData.setValues(key, (Collection<String>) object);
			}
			else if(val != null)
			{
				inData.setProperty(key, val);
			}
		}
	}
	//This is good code, don't delete:

	
}
