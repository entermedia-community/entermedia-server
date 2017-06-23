package library

import org.entermediadb.asset.Category
import org.entermediadb.asset.MediaArchive
import org.openedit.Data

public void init() {
	String id = context.getRequestParameter("id");

	Data library = context.getPageValue("data");
	MediaArchive mediaArchive = (MediaArchive)context.getPageValue("mediaarchive");
	if(library == null){
		if( id == null) {
			id = context.getRequestParameter("id.value");
		}
		if( id == null) {
			return;
		}
		library = mediaArchive.getSearcher("library").searchById(id);
	}

	if( library != null ) 
	{
		
		String username = context.getUserName();
		String owner = library.get("owner");
		if(owner == null)
		{
			library.setProperty("owner", username);
		}	
		boolean isprivate = false;
		if( library.getValue("viewusers") != null ||  library.getValue("viewroles") != null ||  library.getValue("viewgroups") != null)
		{
			isprivate  = true;
		}
		library.setValue("privatelibrary", isprivate);
		//library.setProperty("ownerprofile",context.getUserProfile().getId()); 
		log.info("saving library $library");
		mediaArchive.getSearcher("library").saveData(library, null);
	}
}

init();

