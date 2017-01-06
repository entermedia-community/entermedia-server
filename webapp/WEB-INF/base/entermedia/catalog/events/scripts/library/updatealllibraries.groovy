package library

import org.entermediadb.asset.Category
import org.entermediadb.asset.MediaArchive
import org.openedit.Data

public void init() {
	MediaArchive mediaArchive = (MediaArchive)context.getPageValue("mediaarchive");
	Collection all = mediaArchive.getSearcher("library").getAllHits();

	all.each {
		Data library = it;
		Category parentcategory = null;
		if( library.get("categoryid") == null)
		{
			String path = library.get("folder");
			if( path == null)
			{
				path = "Libraries/" + library.getName();
				library.setValue("folder", path );
			}
			parentcategory = mediaArchive.createCategoryPath(path);
			library.setValue("categoryid", parentcategory.getId() );
			//String username = context.getUserName();
			//parentcategory.addValue("viewusers",username);
			//mediaArchive.getCategorySearcher().saveData(parentcategory);
		
//			String owner = library.get("owner");
//			if(owner == null){
//				library.setProperty("owner", "admin");
//			}	
			//library.setProperty("ownerprofile",context.getUserProfile().getId()); 
			mediaArchive.getSearcher("library").saveData(library, null);
			log.info("saving library $path");
		}
    }
}

init();

