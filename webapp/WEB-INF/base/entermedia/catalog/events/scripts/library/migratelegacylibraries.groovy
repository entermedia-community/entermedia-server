package library

import org.entermediadb.asset.Category
import org.entermediadb.asset.MediaArchive
import org.entermediadb.projects.ProjectManager
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker




//public void moveAssetsToCollections(){
//	
//	
//	
//	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
//	Searcher colassets = mediaArchive.getSearcher("librarycollectionasset");
//	
//	ProjectManager projectmanager = (ProjectManager)moduleManager.getBean(catalogid,"projectManager");
//	
//	hits = colassets.getAllHits();
//	hits.enableBulkOperations();
//	hits.each{
//		String assetid = it.asset;
//		String collection = it.librarycollection;
//		Data col= mediaArchive.getData("librarycollection", collection);
//		if(col){
//			projectmanager.addAssetToCollection(mediaArchive, collection, assetid);
//		}else{
//			log.info("No Collection ${collection} was found, skipping");
//		}
//		
//		
//	}
//	
//	
//}


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
	Searcher assets = mediaArchive.getSearcher("asset");
	
	libs = libraries.getAllHits();
	
	libs.each {
		
		HitTracker hits = collections.fieldSearch("library", it.id);
		
		if(hits.size() == 0){
			Data newcol = collections.createNewData();
			newcol.setId("lib" + it.id);
			newcol.setProperty("division", it.division);
			collections.saveData(newcol);
			
			
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
			if(lib && lib.division){
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
	
	ArrayList libstodelete = new ArrayList();
	
	libs = libraries.getAllHits();
	libs.enableBulkOperations();
	log.info("Found ${libs.size()} libs")
	libs.each {
		HitTracker hits = collections.fieldSearch("library", it.id);
		log.info("Found ${hits.size()} collections")
		hits.enableBulkOperations();
		Data lib = it;
		HitTracker libraryassets = assets.fieldSearch("libraries", it.id);
		log.info("Found ${libraryassets.size()} assets")
		if(libraryassets.size() > 0){	
			libraryassets.enableBulkOperations();			
			Data newcollection = collections.searchById("subcol-${lib.id}");
			if(newcollection == null){
				newcollection = collections.createNewData();
				newcollection.setId("subcol-${lib.id}");
				newcollection.setName(lib.getName());
				collections.saveData(newcollection);
			}
			newcollection.setProperty("division", lib.division);
			if(hits.size() > 0){
				newcollection.setProperty("librarycollection", lib.id);				
			} else{
				libstodelete.add(it);
			}
			manager.addAssetToCollection(mediaArchive, "subcol-${lib.id}", libraryassets);					
		}		
	}
	libraries.deleteAll(libstodelete, null);
	
}



public void prepareCategories(){
	
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	Searcher libraries = mediaArchive.getSearcher("library");
	Searcher collections = mediaArchive.getSearcher("librarycollection");
	Searcher assets = mediaArchive.getSearcher("asset");
	ProjectManager manager = mediaArchive.getProjectManager();
	libs = libraries.getAllHits();
	libs.enableBulkOperations();
	log.info("Found ${libs.size()} libs")
	libs.each {
		HitTracker hits = collections.fieldSearch("library", it.id);
		log.info("Found ${hits.size()} collections")
		hits.enableBulkOperations();
		Data lib = it;
		HitTracker libraryassets = assets.fieldSearch("libraries", it.id);
		log.info("Found ${libraryassets.size()} assets")
		if(libraryassets.size() > 0){
			libraryassets.enableBulkOperations();
			Data newcollection = collections.searchById("subcol-${lib.id}");
			if(newcollection == null){
				newcollection = collections.createNewData();
				newcollection.setId("subcol-${lib.id}");
				newcollection.setName(lib.getName());
				collections.saveData(newcollection);
			}
			newcollection.setProperty("division", lib.division);
			if(hits.size() > 0){
				newcollection.setProperty("librarycollection", lib.id);
			} else{
				libstodelete.add(it);
			}
		}
	}
	libraries.deleteAll(libstodelete, null);
	
}










//- If there is a library with assets that has no child collections, it becomes a collection.  Library is Deleted   DONE
//- assignDivisions() Assign Divisions to Collections directly if the parent library has a division   
//- If a library has child collections and is empty, do nothing 
//  createProjects() - If a library has child collections and itself isn't empty, create a new collection underneath that library with the same name and move assets from the library to the collection with the same name




//moveAssetsToCollections();  Done with the importpermissions script


//convertLibrariesToCollections();
assignDivisions();
//createProjects();
//migratePermissions();