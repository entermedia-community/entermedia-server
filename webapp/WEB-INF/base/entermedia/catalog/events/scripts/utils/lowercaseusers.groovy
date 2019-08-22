package utils;

import org.entermediadb.asset.MediaArchive
import org.openedit.WebPageRequest
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.users.User




public void init()
{
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");

	Searcher users = archive.getUserManager().getUserSearcher();
	
	HitTracker hits = users.getAllHits();
	hits.each { 
		String id = it.id;
		String newid = id.toLowerCase();
		if(!newid.equals(id) ) {
			User user = archive.getUser(id);
			user.setId(newid);
			users.saveData(user);
			archive.clearCaches();
			archive.clearAll();
			user = archive.getUser(id);
			users.delete(user, null);
			
		}
		
		
	}
	
}
init();



