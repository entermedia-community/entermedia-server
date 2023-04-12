package org.entermediadb.asset.modules;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.savedqueries.SavedQueryManager;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.hittracker.Term;

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
	public Data loadSavedQuery(WebPageRequest inReq) throws Exception
	{
		String queryid = inReq.getRequestParameter("queryid");
		if (queryid == null)
		{
			queryid = inReq.findValue("queryid"); // "mostrecent" ?
		}
		String catalogid = inReq.findPathValue("catalogid");
		Data query = getSavedQueryManager().loadSavedQuery(catalogid, queryid, inReq.getUser());
		inReq.putPageValue("savedquery", query);
		return query;
	}
	
	public SearchQuery loadSearchQuery(WebPageRequest inReq) throws Exception
	{
		String queryid = inReq.getRequestParameter("queryid");
		if (queryid == null)
		{
			queryid = inReq.findValue("queryid"); // "mostrecent" ?
		}
		if (queryid == null)
		{
			return null;
		}
		String catalogid = inReq.findPathValue("catalogid");
		Data query = getSavedQueryManager().loadSavedQuery(catalogid, queryid, inReq.getUser());
		//String sessionid = catalogid + "_" + queryid + "_query";
		// inReq.putSessionValue(queryid + "_query", query);
		if( query == null)
		{
			log.error("No such query " + queryid);
			return null;
		}
		SearchQuery searchquery = getSavedQueryManager().loadSearchQuery(catalogid,query,inReq.getUser());
		
		inReq.putSessionValue("currentquery", searchquery);
		//inReq.putSessionValue(sessionid, searchquery);
		inReq.putPageValue("query", searchquery);
		return searchquery;
	}

	public void saveNew(WebPageRequest inReq) throws Exception
	{
		SearchQuery query = new SearchQuery();
		query.setName("New");
		inReq.putSessionValue("currentquery",query);
		inReq.putPageValue("query", query);
	}
	public Data saveFromQuery(WebPageRequest inReq) throws Exception
	{
		//String id = inReq.getRequestParameter("queryid");
		String name = inReq.getRequestParameter("name");
		String description = inReq.getRequestParameter("description");
		
		SearchQuery query = loadCurrentQuery(inReq);
		query.setName(name);
		query.setProperty("caption", description);
		boolean usersaved = Boolean.valueOf( inReq.findValue("usersaved") );
		query.setProperty("usersaved", String.valueOf(usersaved));

		String catalogid = inReq.findPathValue("catalogid");
		getSavedQueryManager().saveQuery(catalogid, query,inReq.getUser());

		inReq.putSessionValue("currentquery", query);
		inReq.putPageValue("query", query);

		return query;
	}

	public Data addPreviousSearch(WebPageRequest inReq) throws Exception
	{
		if( inReq.getUser() == null)
		{
			return null;
		}
		SearchQuery query = loadCurrentQuery(inReq);
		//query.setProperty("caption", query.getName() );
		query.setProperty("usersaved", "false");
		
		String catalogid = inReq.findPathValue("catalogid");
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
			HitTracker hits = (HitTracker)inReq.getPageValue("hits");
			if( hits != null)
			{
				query = hits.getSearchQuery();
			}
		}

		if (query == null)
		{
			query = (SearchQuery) inReq.getSessionValue("currentquery");
		}

		
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
		if(query.getName() == null){
			query.setName("currentquery");
		}
		return query;
	}

	public void deletedSavedQuery(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.findPathValue("catalogid");
		String queryid = inReq.findValue("queryid");
		getSavedQueryManager().deleteQuery(catalogid, queryid, inReq.getUser());
	}
	public HitTracker runSavedQuery(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.findPathValue("catalogid");
		String queryid = inReq.findValue("queryid");
		Data data = getSavedQueryManager().loadSavedQuery(catalogid, queryid, inReq.getUser());
		if(data != null)
		{
			//throw new OpenEditException("saved query not found " + queryid);
			SearchQuery query = getSavedQueryManager().loadSearchQuery(catalogid, data,true,inReq.getUser());
			Searcher searcher = getSearcherManager().getSearcher(catalogid, "asset");
			HitTracker hittracker = searcher.cachedSearch(inReq, query);
			return hittracker;
		}
		return null;
	}
	
	public void saveTerm(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.findPathValue("catalogid");
		String queryid = inReq.findValue("queryid");
		Data data = getSavedQueryManager().loadSavedQuery(catalogid, queryid, inReq.getUser());
		if(data == null)
		{
			throw new OpenEditException("saved query not found " + queryid);
		}
		
		//Construct it
		SearchQuery query = getSavedQueryManager().loadSearchQuery(catalogid, data,inReq.getUser());

		//Edit it
		String detailid = inReq.findValue("detailid");
		
		//TODO: Use this to create a new term:	
		SearchQuery tmpquery = getMediaArchive(inReq).getAssetSearcher().addStandardSearchTerms(inReq);
		Term newterm = tmpquery.getTermByDetailId(detailid);
		if( newterm == null)
		{
			log.info("Could not replace term");
			return;
		}
		String termid = inReq.getRequestParameter("termid");
		query.removeTerm(termid);
		query.addTerm(newterm);
		//query.getT
		//Term term = query.getTermByTermId(termid);
//		String value = inReq.getRequestParameter(term.getDetail().getId() + ".value");
//		term.setValue(value);
		
		//Save it back
		Data saved = getSavedQueryManager().saveQuery(catalogid, query,inReq.getUser());

		//TODO: Add any other properties
	}

	
	public void addTerm(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.findPathValue("catalogid");
		SearchQuery tmpquery = getMediaArchive(inReq).getAssetSearcher().addStandardSearchTerms(inReq);
		//Edit it
		String detailid = inReq.getRequestParameter("detailid");
		String queryid = inReq.findValue("queryid");

		Term term = tmpquery.getTermByDetailId(detailid);
		
		Data data = getSavedQueryManager().loadSavedQuery(catalogid, queryid, inReq.getUser());
		if(data == null)
		{
			throw new OpenEditException("saved query not found " + queryid);
		}
		
		//Construct it
		SearchQuery query = getSavedQueryManager().loadSearchQuery(catalogid, data,inReq.getUser());

		query.addTerm(term);
		//Save it back
		Data saved = getSavedQueryManager().saveQuery(catalogid, query,inReq.getUser());

	}

	public void removeTerm(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.findPathValue("catalogid");
		String queryid = inReq.findValue("queryid");
		
		Data data = getSavedQueryManager().loadSavedQuery(catalogid, queryid, inReq.getUser());
		if(data == null)
		{
			throw new OpenEditException("saved query not found " + queryid);
		}
		
		//Construct it
		SearchQuery query = getSavedQueryManager().loadSearchQuery(catalogid, data,inReq.getUser());

		String termid = inReq.getRequestParameter("termid");
		query.removeTerm(termid);
		//Save it back
		Data saved = getSavedQueryManager().saveQuery(catalogid, query,inReq.getUser());

	}

}
