package publishing;

import org.entermediadb.asset.MediaArchive
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker

public void init() {
MediaArchive archive = context.getPageValue("mediaarchive");
Searcher queue = archive.getSearcher("publishqueue");
HitTracker tracker = queue.query().match("status", "pending").search();
context.putPageValue("hits", tracker);
context.putPageValue("searcher", queue);
	
}
init();