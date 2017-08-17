package library

import org.entermediadb.asset.Category
import org.entermediadb.asset.MediaArchive
import org.entermediadb.projects.ProjectManager
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker

public void migratePermissions() {
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	Searcher libraries = mediaArchive.getSearcher("library");
	libs = libraries.getAllHits();
	ProjectManager projectmanager = (ProjectManager)moduleManager.getBean(catalogid,"projectManager");
	ArrayList tosave =  new ArrayList();

	libs.each {

		String catid = it.categoryid;
		if(catid != null){

			Category cat = mediaArchive.getData("category", catid);
			if(cat){
				List users = it.getValues("viewusers");
				List groups = it.getValues("viewgroups");
				List roles = it.getValues("viewroles");


				users.each {
					cat.addValue("viewusers",it);
				}

				groups.each {
					cat.addValue("viewgroups",it);
				}

				roles.each {
					cat.addValue("viewroles",it);
				}

				tosave.add(cat);
			}
		}
	}




	mediaArchive.getCategorySearcher().saveAllData(tosave,null);
}


public void convertLibrariesToCollections(){
	
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	Searcher libraries = mediaArchive.getSearcher("library");
	Searcher collections = mediaArchive.getSearcher("librarycollection");
	
	libs = libraries.getAllHits();
	
	libs.each {
		
		HitTracker hits = collections.fieldSearch("library", it.id);
		
		if(hits.size() == 0){
//			Data newcol = collections.createNewData();
//			newcol.setId("lib" + it.id);
//			newcol.setProperty("division", it.division);
//			collections.saveData(newcol);
			
			
		}
		
	}
	
}




public void assignDivisions(){
	
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	Searcher collections = mediaArchive.getSearcher("librarycollection");
	
	cols = collections.getAllHits();
	
	cols.each {
		
		String libraryid = it.library;
		if(libraryid){
			Data lib = mediaArchive.getData("library", libraryid);
			if(lib.division){
				Data col = mediaArchive.getData("librarycollection", it.id);
				col.setValue("division", lib.division);
				collections.saveData(col);
			}
			
		}
		
	}
	
}


public void createProjects(){
	
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	Searcher libraries = mediaArchive.getSearcher("library");
	Searcher collections = mediaArchive.getSearcher("librarycollection");
	Searcher assets = mediaArchive.getSearcher("asset");
	ProjectManager manager = mediaArchive.getProjectManager();
	
	libs = libraries.getAllHits();
	
	libs.each {
		
		HitTracker hits = collections.fieldSearch("library", it.id);
		Data lib = it;
		if(hits.size() > 0){
			
			
			hits.each{
				HitTracker libraryassets = assets.fieldSearch("libraries", it.id);
				if(libraryassets.size() > 0){				
					Data newcollection = collections.searchById("subcol-${lib.id}");
					if(newcollection == null){
						newcollection = collections.createNewData();
						newcollection.setId("subcol-${lib.id}");
						newcollection.setName(lib.getName());
						collections.saveData(newcollection);
					}
					newcollection.setProperty("division", lib.division);
					manager.addAssetToCollection(mediaArchive, "subcol-${lib.id}", libraryassets);					
					
					
				}
			}
			
			
			
			
			
			
		}
		
	}
	
}





//- If there is a library with assets that has no child collections, it becomes a collection   DONE
//- Assign Divisions to Collections directly if the parent library has a division   
//- If a library has child collections and is empty, do nothing 
//- If a library has child collections and itself isn't empty, create a project and a collection with the same anme and move assets from the library to the collection with the same name





convertLibrariesToCollections();
assignDivisions();
createProjects();
migratePermissions();