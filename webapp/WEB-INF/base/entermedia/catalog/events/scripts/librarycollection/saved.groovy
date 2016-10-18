package librarycollection

import org.entermediadb.asset.MediaArchive
import org.entermediadb.projects.LibraryCollection
import org.openedit.Data

public void init() {
	String id = context.getRequestParameter("id");

	Data data = context.getPageValue("data");
	MediaArchive mediaArchive = (MediaArchive)context.getPageValue("mediaarchive");
	if(data == null){
		if( id == null) {
			id = context.getRequestParameter("id.value");
		}
		if( id == null) {
			return;
		}
	}
	LibraryCollection collection = mediaArchive.getSearcher("librarycollection").searchById(id);
	if( data != null ) 
	{
		//Make sure the root folder is within the library root folder
		String rootcatid = data.get("rootcategory");
		String libraryid = data.get("library");
		mediaArchive.getData("library",libraryid);
		if( rootcatid == null)
		{
			String path = library.get("folder");
			if( path == null)
			{
				path = "Libraries/" + library.getName();
			}
			parentcategory = mediaArchive.createCategoryPath(path);
			library.setValue("categoryid", parentcategory.getId() );
			String username = context.getUserName();
			parentcategory.addValue("viewusers",username);
			mediaArchive.getCategorySearcher().saveData(parentcategory);
		
			String owner = library.get("owner");
			if(owner == null){
				library.setProperty("owner", username);
				//library.setProperty("ownerprofile",context.getUserProfile().getId()); 
				mediaArchive.getSearcher("library").saveData(library, null);
			}
			log.info("saving library $path");
		}	
	}
}

init();

