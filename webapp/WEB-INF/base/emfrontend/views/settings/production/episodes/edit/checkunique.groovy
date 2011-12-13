import org.openedit.data.Searcher
import org.openedit.data.SearcherManager

import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery

String title = context.getRequestParameter("title.value");
String name = context.getRequestParameter("name.value");
String catalogid = context.findValue("catalogid");
SearcherManager searcherManager = context.getPageValue("searcherManager");

Searcher searcher = searcherManager.getSearcher(catalogid, "episode");
SearchQuery query= searcher.createSearchQuery();
query.addMatches("title", title);
query.addMatches("name", name);
HitTracker hits = searcher.search(query);
if(hits.size() >0){
	String error_url = context.findValue("errorURL");
	context.putPageValue("notunique", true);
	context.setHasForwarded(true);
	context.setCancelActions(true);
	context.forward(error_url);
}
