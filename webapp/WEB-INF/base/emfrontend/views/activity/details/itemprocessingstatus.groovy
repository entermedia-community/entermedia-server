import java.util.Calendar
import java.util.GregorianCalendar

import org.openedit.Data
import org.openedit.data.Searcher

import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.ListHitTracker;
import com.openedit.hittracker.SearchQuery
import org.openedit.entermedia.util.AssetSorter

// Find all asset ids viewed in the last month

String orderitemid = context.getRequestParameter("orderitemid");
String catalogId = mediaarchive.getCatalogId();
Data orderitem = searcherManager.getData(catalogId,"orderitem",orderitemid);

boolean isConversionComplete = false;

String conversiontaskid = orderitem.get("conversiontaskid");
if( conversiontaskid != null)
{
	Data conversiontask = searcherManager.getData(catalogId, "conversiontask", conversiontaskid);
	context.putPageValue("conversiontask", conversiontask);
	Searcher eventsearcher = searcherManager.getSearcher(catalogId,"conversioneventLog");
	SearchQuery iquery = eventsearcher.createSearchQuery();
	iquery.addExact("conversiontaskid", conversiontaskid);
	
	HitTracker convertevents = eventsearcher.search(iquery);
	
	for (Data convertevent: convertevents)
	{
		String type = convertevent.operation;
		int slash = type.indexOf("/");
		if( slash > -1)
		{
			type = type.substring(slash+1);
		}
		context.putPageValue(type,convertevent.date);
		if (type=="conversioncomplete")
		{
			isConversionComplete = true;
		}
	}
}
String publishqueueid = orderitem.get("publishqueueid");
if( publishqueueid != null)
{
	Data publishqueue = searcherManager.getData(catalogid, "publishqueue", publishqueueid);
	context.putPageValue("publishqueue", publishqueue);

	if (conversiontaskid == null || isConversionComplete)
	{				
		Searcher eventsearcher = searcherManager.getSearcher(catalogId,"publisheventLog");
		SearchQuery iquery = eventsearcher.createSearchQuery();
		iquery.addExact("publishqueueid", publishqueueid);
		
		HitTracker events = eventsearcher.search(iquery);
		
		for (Data event: events)
		{
			String type = event.operation;
			int slash = type.indexOf("/");
			if( slash > -1)
			{
				type = type.substring(slash+1);
			}
			context.putPageValue(type, event.date);
		}
	}
}
