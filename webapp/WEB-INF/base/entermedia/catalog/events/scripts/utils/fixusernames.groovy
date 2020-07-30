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
	
	dependants.put("annotation", "user");
	dependants.put("asset", "owner");
	dependants.put("assetapprovedLog", "user");
	dependants.put("assetdeletedLog", "user");
	dependants.put("assetdownloadLog", "user");
	dependants.put("asseteditLog", "user");
	dependants.put("assetnotifications", "userid");
	dependants.put("assetpreviewLog", "user");
	dependants.put("assetrejectedLog", "user");
	dependants.put("assetsearchLog", "user");
	dependants.put("assetusercropLog", "user");
	dependants.put("assetuserlikesLog", "user");
	dependants.put("assetvotes", "user");
	dependants.put("attachment", "creator");
	dependants.put("category", "owner");
	dependants.put("chatterbox", "user");
	dependants.put("chattopiclastchecked", "userid");
	dependants.put("chattopiclastmodified", "userid");
	dependants.put("clienthours", "user");
	dependants.put("clientinvoice", "user");
	dependants.put("collectioninvoice", "owner");
	dependants.put("collectiveproduct", "owner");
	dependants.put("collectiveproject", "owner");
	dependants.put("defaultLog", "user");
	dependants.put("entermedia_clients", "userid");
	dependants.put("entermedia_instances", "owner");
	dependants.put("goaltask", "owner");
	dependants.put("library", "owner");
	dependants.put("librarycollection", "owner");
	dependants.put("librarycollectionhistory", "owner");
	dependants.put("librarycollectionlikes", "followeruser");
	dependants.put("librarycollectionsaved", "userid");
	dependants.put("librarycollectionshares", "followeruser");
	dependants.put("librarycollectionsnapshot", "owner");
	dependants.put("librarycollectionuploads", "owner");
	dependants.put("librarycollectionusers", "followeruser");
	dependants.put("libraryusers", "userid");
	//dependants.put("module", "viewusers");
	dependants.put("order", "userid");
	dependants.put("orderitem", "userid");
	dependants.put("paymentplan", "userid");
	dependants.put("postcomments", "userid");
	//dependants.put("postdata", "viewusers");
	dependants.put("projectgoal", "owner");
	dependants.put("publishdestination", "usename");  //textbox typo
	dependants.put("publishqueue", "user");
	dependants.put("savedquery", "userid");
	dependants.put("settingsmodulepermissionsasseteditLog", "user");
	dependants.put("site_services", "usename");  //textbox
	dependants.put("sitesubscribers", "followeruser");
	dependants.put("statuschanges", "userid");
	dependants.put("transaction", "userid");
	dependants.put("transactionLog", "user");
	dependants.put("usagehistory", "user");
	dependants.put("userauthenticationLog", "userid");
	dependants.put("userprofile", "userid");
	dependants.put("statuschanges", "userid");
	dependants.put("userprofileeditLog", "user");
	dependants.put("userupload", "owner");
	dependants.put("videotrack", "owner");
	dependants.put("userupload", "owner");
	
	
	
	
	
	
	
	
	HitTracker hits = users.getAllHits();
	int count = 0;
	hits.each { 
		String id = it.id;
		String newid = id.toLowerCase();
		
		if (id.indexOf('@')>1) {
			//contains @
			newid = id.substring(0, id.indexOf('@')) + "1"
			User userexists = archive.getUser(newid);
			if (userexists != null) {
				newid = newid + (int)((Math.random() * ((999 - 2) + 1)) + 2);
			}
		}
		if(!newid.equals(id)) {
			log.info("Fixing User: "+ id + " -> "+newid);
			User user = archive.getUser(id);
			user.setId(newid);
			users.saveData(user);
			archive.clearCaches();
			archive.clearAll();
			user = archive.getUser(id);
			users.delete(user, null);
			count = count+1;
			dependants.keySet().each { 
				String table = it;
				String field = dependants.get(it);
				update(table, field, id, newid);
				
			}
			
		}
		
		
	}
	log.info("Fixed: "+count+" users.")
	
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