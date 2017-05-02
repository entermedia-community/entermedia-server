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
		//Make sure the root folder is within the library root folder
		String rootcatid = collection.get("rootcategory");
		if( rootcatid == null)
		{
			Data library = collection.getLibrary();
			if( library == null)
			{
				throw new OpenEditException("Library cannot be null");
			}
			String librarycategoryid = library.get("categoryid");
			if( librarycategoryid == null)
			{
				String path = library.get("folder");
				if( path == null)
				{
					path = "Libraries/" + library.getName();
				}
				Category parentcategory = mediaArchive.createCategoryPath(path);
				librarycategoryid = parentcategory.getId();
				library.setValue("categoryid",  librarycategoryid);
				mediaArchive.getSearcher("library").saveData(library, null);
			}	
		}	
		//Make sure we have a root category
		String librarycategoryid = collection.getLibrary().get("categoryid");
		Category librarycategory = mediaArchive.getCategory(librarycategoryid);
		if( !collection.hasRootCategory() )
		{
			Category collectioncategory = mediaArchive.createCategoryPath(librarycategory.getCategoryPath() + "/" + collection.getName());
			String username = context.getUserName();
			collectioncategory.addValue("viewusers",username);
			mediaArchive.getCategorySearcher().saveData(collectioncategory);
			collection.setCatalogId(collectioncategory.getId());
			mediaArchive.getSearcher("librarycollection").saveData(collection, null);
			log.info("saving collection");
		}
		//Make sure the name still matches
		Category collectioncategory = collection.getCategory();
		if( collectioncategory != null && !collectioncategory.getName().equals(collection.getName()))
		{
			collectioncategory.setName(collection.getName());
			mediaArchive.getCategorySearcher().saveData(collectioncategory);
		}
		//Move the parents if needed
		if( !collectioncategory.hasParent(librarycategory.getId()))
		{
			//Move the child into the parent
			librarycategory.addChild(collectioncategory);
			mediaArchive.getCategorySearcher().saveData(collectioncategory);
		}

	}
}

init();

