package org.entermediadb.elasticsearch.searchers;

import java.util.Iterator;

import org.openedit.Data;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventListener;
import org.openedit.util.DateStorageUtil;

public class ElasticLogSearcher extends BaseElasticSearcher   implements WebEventListener {

	
	public void eventFired(WebEvent inEvent) {
		Data entry = createNewData();
		entry.setProperty("operation", inEvent.getOperation());
		entry.setProperty("user", inEvent.getUsername());
		for (Iterator iterator = inEvent.getProperties().keySet().iterator(); iterator
				.hasNext();) {
			String key = (String) iterator.next();
			entry.setProperty(key, inEvent.get(key));
		}
		entry.setProperty("date", DateStorageUtil.getStorageUtil()
				.formatForStorage(inEvent.getDate()));
		saveData(entry, null);
	}

}
