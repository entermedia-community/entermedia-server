import org.entermediadb.asset.MediaArchive
import org.openedit.WebPageRequest
import org.openedit.data.PropertyDetail
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery
import org.openedit.hittracker.Term


public void init(){
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");
	Searcher searcher = archive.getSearcher(req.findValue("searchtype"));
	HitTracker hits = null;
	if (searcher != null)
	{
		SearchQuery search = searcher.addStandardSearchTerms(req);

		if (search == null) //this seems unexpected. Should it be a new API such as searchAll?
		{
			hits = searcher.getAllHits(req);
		}
		else
		{
			Term term = search.getTermByDetailId("description");
			if( term != null )
			{
				search.setAndTogether(false);
				search.addContains("description", term.getValue());
				/*
				//Add the name and every other detail
				Collection all = searcher.getPropertyDetails();
				for(PropertyDetail detail: all)
				{
			
					search.addContains(detail, term.getValue());
				}
				*/
			}
			hits = searcher.cachedSearch(req, search);
		}
		//log.info("Report ran " +  hits.getSearchType() + ": " + hits.getSearchQuery().toQuery() + " size:" + hits.size() );
		if (hits != null)
		{
			String name = req.findValue("hitsname");
			req.putPageValue(name, hits);
			req.putSessionValue(hits.getSessionId(), hits);
		}
	}
	req.putPageValue("searcher", searcher);

}

init();