package org.openedit.logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventListener;

import com.openedit.OpenEditException;
import com.openedit.Shutdownable;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.modules.BaseModule;
import com.openedit.page.Page;

public class LogSearchModule extends BaseModule implements Shutdownable
{
	private static final Log log = LogFactory.getLog(LogSearchModule.class);
	protected SearcherManager getSearcherManager()
	{
		return (SearcherManager) getBeanFactory().getBean("searcherManager");
	}

	public LuceneLogSearcher getLogSearcher(WebPageRequest inReq)
	{
		String foldername = inReq.findValue("foldername");
		if( foldername == null)
		{
			return null;
		}
		if( foldername.startsWith("/"))
		{
			foldername = foldername.substring(1);
		}
		
		
		String[] info = foldername.split("/");
		String type = info[info.length-1];
		String catalogid = foldername.substring(0, foldername.length() - type.length() -1);
		
		LuceneLogSearcher seacher =  (LuceneLogSearcher) getSearcherManager().getSearcher(catalogid, type); //"logSearcher"
		inReq.putPageValue("searcher", seacher);
		return seacher;
	}

	public void fieldSearch(WebPageRequest inPageRequest) throws Exception
	{

		LuceneLogSearcher search = getLogSearcher(inPageRequest);
		inPageRequest.putPageValue("searcher", search);
		search.fieldSearch(inPageRequest);

	}

	public void recentSearch(WebPageRequest inPageRequest) throws Exception
	{
		LuceneLogSearcher search = getLogSearcher(inPageRequest);
		inPageRequest.putPageValue("searcher", search);
		if (inPageRequest.getRequestParameter("page") == null)
		{
			search.recentSearch(inPageRequest);
		}
	}

	public HitTracker loadHits(WebPageRequest inReq) throws OpenEditException
	{
		String hitsname = inReq.findValue("hitsname");
		if (hitsname == null)
		{
			hitsname = "loghits";
		}

		return getLogSearcher(inReq).loadHits(inReq, hitsname);
	}

	public HitTracker loadPageOfSearch(WebPageRequest inReq) throws Exception
	{

		return getLogSearcher(inReq).loadPageOfSearch(inReq);

	}

	protected void findLogs(List pagelist, String currentPath)
	{
		List<String> paths = getPageManager().getChildrenPaths(currentPath);
		for (String path : paths) 
		{
			Page page = getPageManager().getPage(path);
			if( page.isFolder())
			{
				if(path.endsWith("Log"))
				{
					String temp = path.substring("/WEB-INF/logs".length());
					pagelist.add(temp);
				}
				else
				{
					findLogs(pagelist, path);
				}
			}
		}
	}
	
	public List loadLogList(WebPageRequest inReq)
	{
		List<String> pages = new ArrayList();
		findLogs(pages, "/WEB-INF/logs");
		Collections.sort(pages);
		inReq.putPageValue("logs", pages);
		return pages;
	}

	public void reindexLogs(WebPageRequest inContext) throws OpenEditException
	{
		LuceneLogSearcher search = getLogSearcher(inContext);

		search.reIndexAll();
	}

	public void addEventLog(WebPageRequest inReq)
	{
		WebEvent inEvent = (WebEvent)inReq.getPageValue("webevent");
		if ( inEvent == null)
		{
			//for some reason we do not have any event calling this. Maybe a user event?
			log.error("No actual webevent found " + inReq.getPath());
			return;
		}
		Searcher found = null;
		if( inEvent.getSearchType().endsWith("Log"))
		{
			found = getSearcherManager().getSearcher(inEvent.getCatalogId(), inEvent.getSearchType() );			
		}
		else
		{
			found = getSearcherManager().getSearcher(inEvent.getCatalogId(), inEvent.getSearchType() + inEvent.getOperation() + "Log");
		}
		if( found instanceof WebEventListener)
		{
			WebEventListener lucenelogsearcher= (WebEventListener)found;
			lucenelogsearcher.eventFired(inEvent);
		}
	}
}
