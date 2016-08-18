import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive

import com.openedit.hittracker.HitTracker



public void init(){
	
	MediaArchive archive = context.getPageValue("mediaarchive");
	List uploads = context.getSessionValue("recent-uploads");
	
	String searchtype = context.getRequestParameter("searchtype");
	log.info("searchtype was: " + searchtype);
	Searcher searcher = archive.getSearcher(searchtype);
	String externalid = context.getRequestParameter("fieldexternalid");
	String dataid = context.getRequestParameter("id");
	String assetfield = context.getRequestParameter("assetfield");
	if(!assetfield){
		assetfield = "asset";
		
	}
	for (hit in uploads) {
		Data newrow = searcher.createNewData();
		newrow.setProperty(externalid, dataid);
		newrow.setProperty(assetfield, hit);
		newrow.setId(searcher.nextId());
		
		searcher.saveData(newrow, context.getUser());
			
	}
	
}


init();