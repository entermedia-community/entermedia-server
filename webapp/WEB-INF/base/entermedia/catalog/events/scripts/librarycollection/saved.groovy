package librarycollection

import org.entermediadb.asset.Category
import org.entermediadb.asset.MediaArchive
import org.entermediadb.projects.LibraryCollection
import org.openedit.Data
import org.openedit.OpenEditException

public void init() {
	String id = context.getRequestParameter("id");
	MediaArchive mediaArchive = (MediaArchive)context.getPageValue("mediaarchive");
	
	LibraryCollection collection = mediaArchive.getSearcher("librarycollection").searchById(id);
	if( collection != null ) 
	{
		if(collection.getValue("creationdate") == null){
			collection.setValue("creationdate", new Date());
			mediaArchive.getSearcher("librarycollection").saveData(collection);
		}
		
		mediaArchive.getProjectManager().configureCollection( collection, context.getUserName());
	
		
			
	}
	
	
	
	
	
}

init();

