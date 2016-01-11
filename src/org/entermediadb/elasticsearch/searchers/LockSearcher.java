package org.entermediadb.elasticsearch.searchers;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchService;
import org.openedit.Shutdownable;

public class LockSearcher extends BaseElasticSearcher implements Shutdownable
{
	private static final Log log = LogFactory.getLog(LockSearcher.class);

	protected boolean fieldClearIndexOnStart;

	public boolean isClearIndexOnStart()
	{
		return fieldClearIndexOnStart;
	}

	public void setClearIndexOnStart(boolean inClearIndexOnStart)
	{
		fieldClearIndexOnStart = inClearIndexOnStart;
	}
	
	@Override
	protected void connect()
	{
		if( !isConnected() )
		{
			super.connect();

			clearStaleLocks();

		}
		else
		{
			super.connect();
		}
		
	}

	//TODO: move this to the ClientPool shutdown ruitine
	public void clearStaleLocks()
	{
		String id = getElasticNodeManager().getLocalNodeId();
		
		TermQueryBuilder builder = QueryBuilders.termQuery("nodeid", id);
		   SearchResponse response = getClient().prepareSearch(toId(getCatalogId()))
		            .setSearchType(SearchType.QUERY_THEN_FETCH)
		            .setQuery(builder)
		            .setTypes(getSearchType())
		            .setSize(10000)//Investigate deleting other ways (plugin)
		           // .addFields("id")
		            .execute()
		            .actionGet();

		    for (SearchHit hit : response.getHits().hits()) 
		    {
				DeleteRequestBuilder delete = getClient().prepareDelete(toId(getCatalogId()), getSearchType(), hit.getId());
				delete.setRefresh(false).execute().actionGet();
		    }
		
		log.info("Deleted nodeid=" + id + " records database " + getSearchType() );
//		DeleteByQueryRequestBuilder delete = getClient().prepareDeleteByQuery(toId(getCatalogId()));
//		delete.setTypes(getSearchType());
//		TermQueryBuilder builder = QueryBuilders.termQuery("nodeid", id);
//		delete.setQuery(builder).execute().actionGet();
		
	}
	
	public void shutdown()
	{
		if( isConnected() )
		{
			clearStaleLocks();
		}
		if (fieldElasticNodeManager != null)
		{
			fieldElasticNodeManager.shutdown();
			fieldConnected = false;
			fieldElasticNodeManager = null;
		}
	}
}
