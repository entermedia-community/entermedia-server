import org.openedit.Data;
import com.openedit.hittracker.HitTracker; 
import org.openedit.data.*; 
import org.openedit.entermedia.orders.OrderManager;
import com.openedit.hittracker.SearchQuery;
import java.util.*;


OrderManager manager = moduleManager.getBean("orderManager");
Collection hits = manager.findOrdersForUser(mediaarchive.getCatalogId(), user);

if( hits.size() == 0)
{
	return;
}
Searcher orderitemssearcher = searcherManager.getSearcher(mediaarchive.getCatalogId(),"orderitem");
SearchQuery iquery = orderitemssearcher.createSearchQuery();

StringBuffer orderids = new StringBuffer();
for(Data hit: hits)
{
	orderids.append(hit.getId() );
	orderids.append(" ");
}

iquery.addOrsGroup("orderid",orderids.toString());
HitTracker items = orderitemssearcher.search(iquery);

StringBuffer assetids = new StringBuffer();
for(Data hit : items)
{
	assetids.append(hit.get("assetid") );
	assetids.append(" ");
}

SearchQuery query = mediaarchive.getAssetSearcher().createSearchQuery();
if( assetids.length() == 0)
{
	query.addExact("id","none");
	mediaarchive.getAssetSearcher().cachedSearch(context,query);
	return;
}

query.addOrsGroup("id",assetids.toString());

mediaarchive.getAssetSearcher().cachedSearch(context,query);
