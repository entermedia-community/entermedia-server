package org.entermediadb.asset.modules;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.EnterMedia;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.CompositeSearcher;
import org.openedit.data.PropertyDetail;
import org.openedit.data.SearchQueryArchive;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventHandler;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.hittracker.Term;
import org.openedit.page.Page;
import org.openedit.page.PageProperty;
import org.openedit.profile.UserProfile;


/**
 * @deprecated API has been moved to SavedQueryModule
 * @author cburkey
 *
 */

public class MultiSearchModule extends BaseMediaModule
{
	protected SearcherManager fieldSearcherManager;
	protected SearchQueryArchive fieldSearchQueryArchive;
	protected WebEventHandler fieldWebEventHandler;
	
	protected WebEventHandler getWebEventHandler() {
		return fieldWebEventHandler;
	}

	public void setWebEventHandler(WebEventHandler inListener) {
		fieldWebEventHandler = inListener;
	}
	
	public SearchQueryArchive getSearchQueryArchive()
	{
		return fieldSearchQueryArchive;
	}

	public void setSearchQueryArchive(SearchQueryArchive inSearchQueryArchive)
	{
		fieldSearchQueryArchive = inSearchQueryArchive;
	}

	private static final Log log = LogFactory.getLog(MultiSearchModule.class);

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public String loadApplicationId(WebPageRequest inReq) throws Exception
	{
		String applicationid = inReq.findValue("applicationid");
		inReq.putPageValue("applicationid", applicationid);
		inReq.putPageValue("apphome", "/" + applicationid);

		String prefix = inReq.getContentProperty("themeprefix");
		inReq.putPageValue("themeprefix", prefix);
		return applicationid;
	}

	public SearchQuery loadQuery(WebPageRequest inReq) throws Exception
	{

		String queryid = inReq.getRequestParameter("queryid");
		if (queryid == null)
		{
			queryid = inReq.findValue("queryid"); // "mostrecent" ?
		}
		String applicationid = inReq.getRequestParameter("catalogid");
		if(applicationid == null){
				 applicationid = inReq.findValue("applicationid");
		}
		if (applicationid == null)
		{
			applicationid = inReq.findValue("catalogid");
		}
		SearchQuery query = getSearchQueryArchive().loadQuery(applicationid, "asset", queryid, inReq.getUser());
		if (query != null)
		{
			String sessionid = applicationid + "_" + queryid + "_query";
			// inReq.putSessionValue(queryid + "_query", query);
			inReq.putSessionValue("currentquery", query);
			inReq.putSessionValue(sessionid, query);
			inReq.putPageValue("query", query);
		}

		return query;
	}

	public SearchQuery saveQuery(WebPageRequest inReq) throws Exception
	{

		String id = inReq.getRequestParameter("queryid");
		String name = inReq.getRequestParameter("name");
		String description = inReq.getRequestParameter("description");
		if (id == null)
		{
			id = String.valueOf(new Date().getTime());
		}
		String applicationid = inReq.findValue("applicationid");
		if (applicationid == null)
		{
			applicationid = inReq.findValue("catalogid");
		}
		SearchQuery query = loadCurrentQuery(inReq);

		if (query != null)
		{
			query.setName(name);
			query.setDescription(description);
			getSearchQueryArchive().saveQuery(applicationid, query, id, inReq.getUser());

			inReq.putSessionValue("currentquery", query);
			inReq.putPageValue("query", query);
		}

		return query;
	}

	public void loadLastQuery(WebPageRequest inReq) throws Exception
	{
		loadCurrentQuery(inReq);
	}
/*
	public void addCatalog(WebPageRequest inReq) throws Exception
	{
		SearchQuery query = loadCurrentQuery(inReq);
		String addcatalog = inReq.getRequestParameter("addcatalogid");
		if (addcatalog != null)
		{
			if( "addall".equals(addcatalog))
			{
				//loop all catalogs
				UserProfile settings = inReq.getUserProfile();
				query.setCatalogs(new ArrayList());
				for (Iterator iterator = settings.getCatalogs().iterator(); iterator.hasNext();) 
				{
					Data cat = (Data) iterator.next();
					query.addCatalog(cat.getId());
				}
			}
			else
			{
				query.addCatalog(addcatalog);
			}
		}
		String removecatalog = inReq.getRequestParameter("removecatalogid");
		if (removecatalog != null)
		{
			if( "removeall".equals(removecatalog))
			{
				query.setCatalogs(new ArrayList());
			}
			else
			{
				query.removeCatalog(removecatalog);
			}
		}
		String appid = inReq.findValue("applicationid");
		getSearchQueryArchive().saveQuery(appid, query, inReq.getUserName() + "-mostrecent", inReq.getUser());

	}
*/
	public void addTerm(WebPageRequest inReq) throws Exception
	{
		SearchQuery query = loadCurrentQuery(inReq);

		String catalogid = inReq.getRequestParameter("catalogid");
		if (catalogid == null)
		{
			catalogid = inReq.findValue("catalogid");
		}
		String fieldid = inReq.getRequestParameter("fieldid");
		String viewname = inReq.getRequestParameter("view");

		String searchtype = "asset";
		
		Searcher searcher = getSearcherManager().getSearcher(catalogid, searchtype);
		PropertyDetail detail = searcher.getDetailForView(viewname, fieldid, inReq.getUser());
		if (detail != null)
		{
			// detail.setSearchType(searchtype); //This should not be needed
			query.addMatches(detail);
		}
		else
		{
			log.error("Term not found " + searchtype + "/" + fieldid);
		}
	}

	public void removeTerm(WebPageRequest inReq) throws Exception
	{
		SearchQuery query = loadCurrentQuery(inReq);

		String termid = inReq.getRequestParameter("termid");
		query.removeTerm(termid);
	}

	public void updateQuery(WebPageRequest inReq) throws Exception
	{
		String[] terms = inReq.getRequestParameters("termid");
		String[] operation = inReq.getRequestParameters("operation");
		SearchQuery query = loadCurrentQuery(inReq);

		if (query == null || terms == null)
		{
			return;
		}

		Map counters = new HashMap();
		for (int i = 0; i < terms.length; i++)
		{
			Term term = query.getTermByTermId(terms[i]);
			if (term == null)
			{
				continue;
			}
			PropertyDetail detail = term.getDetail();
			String[] values = inReq.getRequestParameters(detail.getId() + ".value");
			int index = -1;
			if (counters.get(detail.getId()) != null)
			{
				index = ((Integer) counters.get(detail.getId())).intValue();
			}
			index++;
			String val = null;
			if (values != null && values.length > index)
			{
				val = values[index];
			}
			// term.setValue(val);
			query.setProperty(term.getId(), val);
			term.addParameter("op", operation[i]);
			counters.put(detail.getId(), new Integer(index));

			if (operation[i].equals("is"))
			{
				String[] requestParams = inReq.getRequestParameters(detail.getId() + ".additionals");
				if (requestParams != null)
				{
					String[] additionalInputs = requestParams[index].split(",");
					for (int j = 0; j < additionalInputs.length; j++)
					{
						String paramid = detail.getId() + "." + additionalInputs[j];
						String inputid = term.getId() + "." + additionalInputs[j];
						String additional = inReq.getRequestParameters(paramid)[index];
						query.setProperty(inputid, additional);
					}
				}
			}
		}
		//log.info(query.toQuery());
	}

	public SearchQuery loadCurrentQuery(WebPageRequest inReq) throws Exception
	{
		String applicationid = inReq.findValue("applicationid");
		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		SearchQuery query = null;
		
		if(hitssessionid != null){
			HitTracker hits = (HitTracker) inReq.getSessionValue(hitssessionid);
			query = hits.getSearchQuery();
			
		}
	
		if(query == null){
		 query = (SearchQuery) inReq.getSessionValue("currentquery");
		}
		try
		{
			if (query == null)
			{
				log.info("No query in session - loading most recent");
				query = getSearchQueryArchive().loadQuery(applicationid, "asset", inReq.getUserName() + "-mostrecent", inReq.getUser());
			}
		}
		catch (Exception e)
		{
			log.error(e);
		}
		if (query == null)
		{
			query = getSearcherManager().getSearcher(applicationid, "asset").createSearchQuery();
			query.setDescription("Last edited query for user " + inReq.getUserName());
			query.setId(inReq.getUserName() + "-mostrecent");
			query.setName("Current query");
		}
		if (query != null)
		{
			inReq.putSessionValue("currentquery", query);
			inReq.putPageValue("query", query);
		}
		return query;
	}

	public HitTracker runCurrentQuery(WebPageRequest inReq) throws Exception
	{
		SearchQuery query = loadCurrentQuery(inReq);
		String catalogid = inReq.findValue("applicationid");
		String searchType = inReq.getRequestParameter("searchtype");
		String[] catalogs = new String[query.getCatalogs().size()];
		for (int i = 0; i < catalogs.length; i++)
		{
			catalogs[i] = String.valueOf(query.getCatalogs().get(i));
		}
		inReq.setRequestParameter("catalogid", catalogs);
		Searcher searcher = getSearcherManager().getSearcher(catalogid, searchType);
		HitTracker hittracker = searcher.cachedSearch(inReq, query);
		return hittracker;
	}

	public HitTracker loadHits(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.getRequestParameter("catalogid");
		if (catalogid != null)
		{// for a sub searcher
			String searchType = inReq.getRequestParameter("searchtype");
			Searcher searcher = getSearcherManager().getSearcher(catalogid, searchType);
			String hitsname = inReq.findValue("hitsname");
			return searcher.loadHits(inReq, hitsname);

		}

		catalogid = inReq.findValue("applicationid");

		String searchType = inReq.getRequestParameter("searchtype");
		Searcher searcher = getSearcherManager().getSearcher(catalogid, searchType);
		String hitsname = inReq.findValue("hitsname");

		if (hitsname == null)
		{
			hitsname = "hits";
		}
		HitTracker hittracker = searcher.loadHits(inReq, hitsname);
		inReq.putPageValue(hitsname + catalogid, hittracker);
		inReq.putPageValue("hits", hittracker);
		return hittracker;

	}
/*
	public void showAll(WebPageRequest inReq) throws Exception
	{
		String applicationid = inReq.findValue("applicationid");
		CompositeSearcher searcher = (CompositeSearcher)getSearcherManager().getSearcher(applicationid, "compositeLucene");
		SearchQuery q = searcher.createSearchQuery();
		q.addMatches("description","*");
		
		UserProfile settings = inReq.getUserProfile();
		if( settings != null )
		{
			String catid = null;
			if( settings.getLastCatalog() != null )
			{
				catid = settings.getLastCatalog().getId();	
			}
			else if( settings.getCatalogs().size() > 0)
			{
				Data cat = (Data)settings.getCatalogs().get(0);
				catid = cat.getId();
			}
			inReq.setRequestParameter("catalogid", catid);
		}
		HitTracker	hits = searcher.cachedSearch(inReq, q);
		// save off this as the most recent
		if (hits != null)
		{
			hits.setDataSource(applicationid + "/search/multicatsearch/multiresults");
			getSearchQueryArchive().saveQuery(applicationid, hits.getSearchQuery(), inReq.getUserName() + "-mostrecent", inReq.getUser());
			inReq.putSessionValue("currentquery", hits.getSearchQuery());
		}
		inReq.putPageValue("searcher", searcher);
	}
	public void multiSearch(WebPageRequest inReq) throws Exception
	{
		String applicationid = inReq.findValue("applicationid");

		CompositeSearcher searcher = (CompositeSearcher)getSearcherManager().getSearcher(applicationid, "compositeLucene");
		SearchQuery inQuery = searcher.addStandardSearchTerms(inReq);
		HitTracker hits = null;
		if( inQuery != null)
		{
			Term term = inQuery.getTermByDetailId("description");
			if( term != null && "*".equals(term.getValue()))
			{
				inQuery.removeTerm(term); //will be ignored by the search query
				inQuery.setProperty("description", "*");
				inQuery.addMatches("category", "index");
			}
			
			hits = searcher.cachedSearch(inReq,inQuery);
			//HitTracker hits = searcher.fieldSearch(inReq);
			if (hits == null)
			{
				if (Boolean.parseBoolean(inReq.getRequestParameter("reload")))
				{
					SearchQuery q = loadCurrentQuery(inReq);
					q.setCatalogId(applicationid);
					hits = searcher.cachedSearch(inReq, q);
				}
			}
		}
		if( hits == null)
		{
			hits = searcher.loadHits(inReq);
		}
		// save off this as the most recent
		if (hits != null)
		{
			hits.setDataSource(applicationid + "/search/multicatsearch/multiresults");
			getSearchQueryArchive().saveQuery(applicationid, hits.getSearchQuery(), inReq.getUserName() + "-mostrecent", inReq.getUser());
			inReq.putSessionValue("currentquery", hits.getSearchQuery());
		}
		inReq.putPageValue("searcher", searcher);
	}
*/
	public void search(WebPageRequest inReq) throws Exception
	{
		String applicationid = inReq.findValue("applicationid");

		CompositeSearcher searcher = (CompositeSearcher)getSearcherManager().getSearcher(applicationid, "compositeLucene");
		SearchQuery inQuery = searcher.addStandardSearchTerms(inReq);
		HitTracker hits = null;
		if( inQuery != null)
		{
			hits = searcher.search(inReq,inQuery);
		}
		inReq.putPageValue("searcher", searcher);
	}

	
	public void loadSavedQueryList(WebPageRequest inReq) throws Exception
	{
		// This is bad/slow/called all the time. TODO: We should just maintain
		// an XML file with the list of saved queries

		HitTracker hits = new ListHitTracker();
		String applicationid = inReq.findValue("applicationid");
		List queries = getSearchQueryArchive().loadSavedQueryList(applicationid, "compositeLucene", inReq.getUser());
		for (Iterator iterator = queries.iterator(); iterator.hasNext();)
		{
			SearchQuery query = (SearchQuery) iterator.next();
			if (!query.getId().contains("recent"))
			{
				// log.info(query.getId());
				hits.add(query);
			}
		}

		String hitsname = inReq.findValue("hitsname");
		if (hitsname == null)
		{
			hitsname = "hits";
		}
		inReq.putPageValue(hitsname, hits);
	}

	protected String getCatalogId(WebPageRequest inReq)
	{
		String catid = inReq.getContentProperty("catalogid");
		if (catid == null)
		{
			catid = inReq.getRequestParameter("catalogid");
		}
		if (catid == null)
		{
			catid = inReq.findValue("catalogid");
		}
		return catid;
	}

	public void clearCurrentQuery(WebPageRequest inReq) throws Exception
	{
		SearchQuery query = loadCurrentQuery(inReq);
		query.getTerms().clear();
	}

	public void loadAsset(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.getRequestParameter("catalogid");
		String assetid = inReq.getRequestParameter("assetid");
		EnterMedia matt = getEnterMedia(inReq);
		Asset prod = matt.getAsset(catalogid, assetid);
		inReq.putPageValue("asset", prod);
		inReq.putPageValue("matt", matt);

	}

	public void createNewCatalog(WebPageRequest inReq) throws Exception
	{
		String catname = inReq.getRequestParameter("name");
		String rootpath = inReq.getRequestParameter("rootpath");
		if( !rootpath.startsWith("/"))
		{
			rootpath = "/" + rootpath;
		}
		String subdomain = inReq.getRequestParameter("clientsubdomain");
//		String foldername = inReq.findValue("foldername");
//		String appfolder = null;
//		if(foldername == null)
//		{
//			appfolder= catname;
//	
//			appfolder = PathUtilities.extractId(appfolder, true);
//	
//			if (!appfolder.startsWith("/"))
//			{
//				appfolder = "/" + appfolder;
//			}
//			if (!appfolder.endsWith("/"))
//			{
//				appfolder = appfolder + "/";
//			}
//			appfolder = appfolder.toLowerCase();
//		}
//		else
//		{
//			appfolder = foldername;
//		}
//		if(!appfolder.endsWith("/"))
//		{
//			appfolder = appfolder + "/";
//		}
//		String prefix = inReq.findValue("appfolderprefix");
//		if (prefix == null)
//		{
//			prefix = "";
//		}
		String catalogid = rootpath + "/catalog";
		catalogid = catalogid.substring(1);
		catalogid = catalogid.toLowerCase();
		Page app = getPageManager().getPage("/" + catalogid + "/_site.xconf");
		PageProperty prop = new PageProperty("fallbackdirectory");
		prop.setValue(inReq.findValue("fallbackfolder"));
		app.getPageSettings().putProperty(prop);

		PageProperty catid = new PageProperty("catalogid");
//		String catalogid = appfolder;
//		catalogid = catalogid.replace("/", "");
//		String appid = inReq.findValue("applicationid");
//		catalogid = appid + "/catalogs/" + catalogid;
		catid.setValue(catalogid);
		app.getPageSettings().putProperty(catid);

		getPageManager().saveSettings(app);

		// Add to catalog
		Searcher searcher = getSearcherManager().getSearcher("system", "catalog");
		Data row = searcher.createNewData();
		row.setId(catalogid);
		row.setProperty("name", catname);
		row.setProperty("rootpath", rootpath);
		row.setProperty("clientsubdomain", subdomain);
		
		searcher.saveData(row, inReq.getUser());
		inReq.putPageValue("catalog", row);

//		String id = appid + "usersettings" + inReq.getUserName();
//		inReq.removeSessionValue(id);
		fireCatalogEvent("catalog/saved", catalogid);
	}

	protected void fireCatalogEvent(String inOperation, String newcatalogid) 
	{
		if (fieldWebEventHandler != null) {
			WebEvent event = new WebEvent();
			event.setOperation(inOperation);
			event.setSearchType("catalog");
			event.setSource(this);
			event.setProperty("newcatalogid", newcatalogid);
			event.setCatalogId("system");
			getWebEventHandler().eventFired(event);
		}
	}
	
	public Map loadMultiViews(WebPageRequest inReq)
	{
		String applicationid = inReq.findValue("applicationid");
		Searcher viewSearcher = getSearcherManager().getSearcher(applicationid, "views");
		String[] catalogids = inReq.getRequestParameters("catalogid");
		//Performance problem, only support the top 4 catalogs
		if( catalogids.length > 4)
		{
			String[] copies = new String[4];
			System.arraycopy(catalogids, 0, copies, 0, 4);
			catalogids = copies;
		}
		Map allViews = ListOrderedMap.decorate(new HashMap());
		HitTracker views = viewSearcher.getAllHits();

		for (Iterator iterator = views.iterator(); iterator.hasNext();)
		{
			Object view = (Object) iterator.next();
			String viewid = views.getValue(view, "id");
			Map alldetails = ListOrderedMap.decorate(new HashMap());
			for (int i = 0; i < catalogids.length; i++)
			{
				Searcher assetSearcher = getSearcherManager().getSearcher(catalogids[i], "asset");
				List details = assetSearcher.getDetailsForView(viewid, inReq.getUser());
				if( details != null)
				{
					for (Iterator iterator2 = details.iterator(); iterator2.hasNext();)
					{
						PropertyDetail detail = (PropertyDetail) iterator2.next();
						if (!alldetails.containsKey(detail.getId()))
						{
							alldetails.put(detail.getId(), detail);
						}
					}
				}
			}
			allViews.put(view, alldetails);
		}
		inReq.putPageValue("allviews", allViews);
		return allViews;
	}

	public void deleteCatalog(WebPageRequest inReq)
	{
		String applicationid = inReq.findValue("applicationid");
		String catalogid = inReq.getRequestParameter("deletecatalogid");
		if (catalogid == null)
		{
			log.warn("No catalog to remove");
			return;
		}

		Searcher catalogs = getSearcherManager().getSearcher(applicationid, "catalogs");
		Data element = (Data) catalogs.searchById(catalogid);
		if (element == null)
		{
			return;
		}
		catalogs.delete(element, inReq.getUser());

		Page storedir = getPageManager().getPage("/" + catalogid);
		if (storedir.exists())
		{
			getPageManager().removePage(storedir);
		}
		String id = applicationid + "usersettings" + inReq.getUserName();
		inReq.removeSessionValue(id);

	}
}
