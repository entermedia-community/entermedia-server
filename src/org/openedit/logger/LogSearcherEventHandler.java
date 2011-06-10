package org.openedit.logger;

import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventHandler;
import org.openedit.event.WebEventListener;

public class LogSearcherEventHandler extends WebEventHandler
{
	protected SearcherManager fieldSearcherManager;
	
	public void eventFired(WebEvent inEvent)
	{
		Searcher found = null;
			found = getSearcherManager().getSearcher(inEvent.getCatalogId(), inEvent.getSearchType() + inEvent.getOperation() + "Log");
		if( found instanceof WebEventListener)
		{
			WebEventListener lucenelogsearcher= (WebEventListener)found;
			lucenelogsearcher.eventFired(inEvent);
		}
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

}
