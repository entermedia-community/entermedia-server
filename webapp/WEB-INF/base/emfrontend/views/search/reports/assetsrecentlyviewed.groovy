import java.util.Calendar
import java.util.GregorianCalendar

import org.openedit.Data
import org.openedit.data.Searcher

import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.ListHitTracker;
import com.openedit.hittracker.SearchQuery
import org.openedit.entermedia.util.AssetSorter

// Find all asset ids viewed in the last month
Searcher assetpreviewsearcher = searcherManager.getSearcher(mediaarchive.getCatalogId(),"assetpreviewLog");
SearchQuery iquery = assetpreviewsearcher.createSearchQuery();
GregorianCalendar cal = new GregorianCalendar();
cal.add(Calendar.MONTH, -1);
iquery.addAfter("date", cal.getTime());
iquery.addSortBy("dateDown");
//find out current user
String name = context.getUserName()
//filter by user
iquery.addExact("user", name)
//sort desc (recent viewed on top)

HitTracker items = assetpreviewsearcher.search(iquery);

List assetids=new ArrayList();
//Map<String, String> assetids = new HashMap();
for (Data hit : items)
{
	String assetid=hit.get("assetid");
	String lastViewed=hit.get("date");
	if (assetid!=null) 
	{
		if (!assetids.contains(assetid))
		{
			assetids.add(assetid);
		}
	}
}

SearchQuery query = mediaarchive.getAssetSearcher().createSearchQuery();
if( assetids.size() == 0)
{
	query.addExact("id","none");
	mediaarchive.getAssetSearcher().cachedSearch(context,query);
	return;
}

// Build space delimited String from set of Asset ids
StringBuffer assetidsbuffer = new StringBuffer();
for (String assetid: assetids) {
		assetidsbuffer.append(assetid );
		assetidsbuffer.append(" ");
}

query.addOrsGroup("id",assetidsbuffer.toString());

HitTracker hits = mediaarchive.getAssetSearcher().cachedSearch(context,query);
List hitList = new ArrayList(hits);
//use custom asset sorter to sort by last viewed data desc
Collections.sort(hitList, new AssetSorter(assetids));
ListHitTracker hitTracker = new ListHitTracker(hitList)
context.putPageValue(hits.getHitsName(), hitTracker);
context.putSessionValue(hits.getSessionId(), hitTracker);


