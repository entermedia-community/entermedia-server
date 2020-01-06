package utils;

import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.WebPageRequest
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.users.User




public void init()
{
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");

	Searcher users = archive.getUserManager().getUserSearcher();
	
	HashMap dependants = new HashMap();
	dependants.put("asset", "owner");
	dependants.put("librarycollection", "owner");
	dependants.put("userprofile", "id");
	
	
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
			
			dependants.keySet().each { 
				String table = it;
				String field = dependants.get(it);
				update(table, field, id, newid);
				
			}
			
		}
		
		
	}
	
}
init();



public void update(String inTable, String inField,String oldvalue,  String newValue) {
	
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");
	Searcher searcher = archive.getSearcher(inTable);
	
	HitTracker toupdate = searcher.fieldSearch(inField, oldvalue);
	toupdate.each { 
		Data hit = searcher.loadData(it);
		hit.setValue(inField, newValue);
		searcher.saveData(hit);
		
		
	}
	
	
	
	
	
}