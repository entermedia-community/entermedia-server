package org.entermediadb.logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Shutdownable;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventListener;
import org.openedit.modules.BaseModule;

public class LogSearchModule extends BaseModule implements Shutdownable
{
	private static final Log log = LogFactory.getLog(LogSearchModule.class);

	
	protected SearcherManager getSearcherManager()
	{
		return (SearcherManager) getModuleManager().getBean("searcherManager");
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
		String type = inEvent.getOperation().replace("/","");
		
		Searcher found = null;
			found = getSearcherManager().getSearcher(inEvent.getCatalogId(), type + "Log");
		if( found instanceof WebEventListener)
		{
			WebEventListener lucenelogsearcher= (WebEventListener)found;
			lucenelogsearcher.eventFired(inEvent);
		}
	}
}
