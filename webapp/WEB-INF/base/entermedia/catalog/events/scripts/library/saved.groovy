package library

import org.entermediadb.asset.Category
import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker

public void init() {
	String id = context.getRequestParameter("id");

	Data library = context.getPageValue("data");
	MediaArchive archive = (MediaArchive)context.getPageValue("mediaarchive");
	
	if(library == null){
		if( id == null) {
			id = context.getRequestParameter("id.value");
		}
		if( id == null) {
			return;
		}
		library = archive.getSearcher("library").searchById(id);
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
		archive.getSearcher("library").saveData(library, null);
	}
	
	if(library.getValue("autocreatecollections") == true){
		String categoryid = library.categoryid;
		Category cat = archive.getCategory(categoryid);
		Searcher collectionsearcher = mediaarchive.getSearcher("librarycollection");
		
		if(cat != null){
			cat.getChildren().each{
				Category target = it;
				Data collection = collectionsearcher.searchByField("rootcategory", target.getId());
				if(collection == null){
					collection = collectionsearcher.createNewData();
					collection.setValue("rootcategory", target.getId());
					collection.setValue("library", library.getId());
					
					collection.setName(target.getName());
					collectionsearcher.saveData(collection);
					

				}
				
			}
		}
		
	}
	
	String divid = library.getValue("division");
	
	
	if(divid){
		Category cat = archive.getCategory(categoryid);
		Searcher collectionsearcher = mediaarchive.getSearcher("librarycollection");
		
		HitTracker cols = collectionsearcher.query().exact("library", library.getId());
		cols.each{
			if(!divid.equals(it.division)){
				Data real = collectionsearcher.loadData(it);
				real.setValue("division", divid);
				collectionsearcher.saveData(real);
			}
		}
		
	}
	
	
	
	
	
}

init();

