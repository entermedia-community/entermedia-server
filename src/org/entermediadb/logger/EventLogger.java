package org.entermediadb.logger;

import java.util.Iterator;

import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventListener;

public class EventLogger implements WebEventListener
{
	protected SearcherManager fieldSearcherManager;
	
	public void eventFired(WebEvent inEvent) 
	{
		Searcher searcher = getSearcherManager().getSearcher(inEvent.getCatalogId(), inEvent.getSearchType() + inEvent.getOperation() + "Log");
		Data entry = searcher.createNewData();
		entry.setValue("operation", inEvent.getOperation());
		entry.setValue("user", inEvent.getUsername());
		for (Iterator iterator = inEvent.keySet().iterator(); iterator
				.hasNext();) {
			String key = (String) iterator.next();
			entry.setValue(key, inEvent.get(key));
		}
		entry.setValue("date", inEvent.getDate());
		searcher.saveData(entry, null);
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
