package org.openedit.entermedia.modules;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.entermedia.savedqueries.SavedQueryManager;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;

public class SavedQueryModule extends BaseMediaModule
{
	protected SavedQueryManager fieldSavedQueryManager;
	private static final Log log = LogFactory.getLog(SavedQueryModule.class);

	public SavedQueryManager getSavedQueryManager()
	{
		return fieldSavedQueryManager;
	}

	public void setSavedQueryManager(SavedQueryManager inSavedQueryManager)
	{
		fieldSavedQueryManager = inSavedQueryManager;
	}

	public void loadSavedQueryList(WebPageRequest inReq) throws Exception
	{
//		HitTracker hits = new ListHitTracker();
		getSavedQueryManager().loadSavedQueryList(inReq);
//		for (Iterator iterator = queries.iterator(); iterator.hasNext();)
//		{
//			SearchQuery query = (SearchQuery) iterator.next();
//			if (!query.getId().contains("recent"))
//			{
//				// log.info(query.getId());
//				hits.add(query);
//			}
//		}

//		String hitsname = inReq.findValue("hitsname");
//		if (hitsname == null)
//		{
//			hitsname = "hits";
//		}
//		inReq.putPageValue(hitsname, hits);
	}

	public SearchQuery loadSearchQuery(WebPageRequest inReq) throws Exception
	{
		String queryid = inReq.getRequestParameter("queryid");
		if (queryid == null)
		{
			queryid = inReq.findValue("queryid"); // "mostrecent" ?
		}
		String catalogid = inReq.findValue("catalogid");
		Data query = getSavedQueryManager().loadSavedQuery(catalogid, queryid, inReq.getUser());
		//String sessionid = catalogid + "_" + queryid + "_query";
		// inReq.putSessionValue(queryid + "_query", query);
		SearchQuery searchquery = getSavedQueryManager().loadSearchQuery(catalogid,query,inReq.getUser());
		
		inReq.putSessionValue("currentquery", searchquery);
		//inReq.putSessionValue(sessionid, searchquery);
		inReq.putPageValue("query", searchquery);
		return searchquery;
	}

	public Data createNewSavedQuery(WebPageRequest inReq) throws Exception
	{
		//String id = inReq.getRequestParameter("queryid");
		String name = inReq.getRequestParameter("name");
		String description = inReq.getRequestParameter("description");
		SearchQuery query = loadCurrentQuery(inReq);
		query.setName(name);
		query.setProperty("caption", description);

		String catalogid = inReq.findValue("catalogid");
		getSavedQueryManager().saveQuery(catalogid, query,inReq.getUser());

		inReq.putSessionValue("currentquery", query);
		inReq.putPageValue("query", query);

		return query;
	}

	public void loadLastQuery(WebPageRequest inReq) throws Exception
	{
		loadCurrentQuery(inReq);
	}

	public SearchQuery loadCurrentQuery(WebPageRequest inReq) throws Exception
	{
		//String applicationid = inReq.findValue("applicationid");
		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		SearchQuery query = null;

		if (hitssessionid != null)
		{
			HitTracker hits = (HitTracker) inReq.getSessionValue(hitssessionid);
			query = hits.getSearchQuery();

		}

		if (query == null)
		{
			query = (SearchQuery) inReq.getSessionValue("currentquery");
		}

//		if (query == null)
//		{
//			query = getSearcherManager().getSearcher(applicationid, "luceneComposite").createSearchQuery();
//			query.setDescription("Last edited query for user " + inReq.getUserName());
//			query.setId(inReq.getUserName() + "-mostrecent");
//			query.setName("Current query");
//		}
		if (query != null)
		{
			inReq.putSessionValue("currentquery", query);
			inReq.putPageValue("query", query);
		}
		return query;
	}

	public HitTracker runSavedQuery(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.findValue("catalogid");
		String queryid = inReq.findValue("queryid");
		Data data = getSavedQueryManager().loadSavedQuery(catalogid, queryid, inReq.getUser());
		if(data == null)
		{
			throw new OpenEditException("saved query not found " + queryid);
		}
		SearchQuery query = getSavedQueryManager().loadSearchQuery(catalogid, data,inReq.getUser());
//		String searchType = inReq.getRequestParameter("searchtype");
//		String[] catalogs = new String[query.getCatalogs().size()];
//		for (int i = 0; i < catalogs.length; i++)
//		{
//			catalogs[i] = String.valueOf(query.getCatalogs().get(i));
//		}
//		inReq.setRequestParameter("catalogid", catalogs);
				
				
		Searcher searcher = getSearcherManager().getSearcher(catalogid, "asset");
		HitTracker hittracker = searcher.cachedSearch(inReq, query);
		return hittracker;
	}

}
