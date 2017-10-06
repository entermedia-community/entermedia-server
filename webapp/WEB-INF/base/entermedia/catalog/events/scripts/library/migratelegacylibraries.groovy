package library

import org.entermediadb.asset.Category
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.search.AssetSearcher
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


	Data orphancollections = libraries.searchById("default");
	if(orphancollections == null){
		orphancollections = libraries.createNewData();
		orphancollections.setId("default");
		orphancollections.setName("General");
		libraries.saveData(orphancollections);
	}

	libs.each {
		HitTracker childcollections = collections.fieldSearch("library", it.id);
		childcollections.enableBulkOperations();
		Data lib = it;
		HitTracker libraryassets = assets.fieldSearch("libraries", it.id);
		libraryassets.enableBulkOperations();

		if(libraryassets.size() > 0){
			Data newcollection = collections.searchById("subcol-${lib.id}");
			if(newcollection == null){
				newcollection = collections.createNewData();
				newcollection.setId("subcol-${lib.id}");
				newcollection.setName(lib.getName());
			}
			Category node =null;
			Data library =  it;
			String path = library.get("folder");

			if(childcollections.size() > 0){ //The move to the child case

				if( path == null)
				{
					path = "Collections/" + library.getName();
				}

				//In this case, this was a library that has child collections, so we are making a parent collection
				node = mediaArchive.createCategoryPath(path);
				newcollection.setValue("library", lib.id);

			} else{

				if( path == null)
				{
					path = "Collections/General/" + newcollection.getName()
				}


				node = mediaArchive.createCategoryPath(path);
				//In this case, this was a library that we are converting to a collection, it didn't have any child collections at all.
				newcollection.setValue("library", "default");
				libstodelete.add(it);
			}
			newcollection.setValue("categoryid", node.getId());
			newcollection.setProperty("division", lib.division);
			collections.saveData(newcollection);
			manager.addAssetToCollection(mediaArchive, newcollection.getId(), libraryassets);


			HitTracker users = mediaArchive.getSearcher("libraryusers").query().match("libraryid",library.getId()).search();
			users.each {
				library.addValue("viewusers",it.userid);
				if(node != null) {
					node.addValue("viewusers",it.userid);
				}
			}


			HitTracker groups = mediaArchive.getSearcher("librarygroups").query().match("libraryid",library.getId()).search();
			groups.each {
				library.addValue("viewgroups",it.groupid);
				if(node != null) {

					node.addValue("viewgroups",it.groupid);
				}
			}

			HitTracker roles = mediaArchive.getSearcher("libraryroles").query().match("libraryid",library.getId()).search();
			roles.each {
				library.addValue("viewroles",it.roleid);
				if(node != null) {
					node.addValue("viewroles",it.roleid);
				}
			}

			libraries.saveData(library);
			if( node != null)
			{
				mediaArchive.getCategorySearcher().saveData(node);
			}





		}
	}
	libraries.deleteAll(libstodelete, null);

}


public void setupAssetPermissions(){

	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	Searcher libraries = mediaArchive.getSearcher("library");
	Searcher collections = mediaArchive.getSearcher("librarycollection");
	Searcher assets = mediaArchive.getSearcher("asset");
	ProjectManager manager = mediaArchive.getProjectManager();

	HitTracker groups = mediaArchive.getSearcher("group").getAllHits();
	AssetSearcher searcher = mediaArchive.getAssetSearcher();


	Data lib  = libraries.createNewData();
	lib.setName("Shared Assets");
	lib.setId("sharedassets");
	libraries.saveData(lib);


	groups.each{
		HitTracker hits = searcher.query().exact("viewgroups", it.id).search();
		if(hits.size() > 0){

			Data newcollection = collections.searchById("groupcol-${it.id}");
			if(newcollection == null){
				newcollection = collections.createNewData();
				newcollection.setId("groupcol-${it.id}");
				newcollection.setName(it.getName());
				newcollection.setProperty("library", "sharedassets");
			}
			String path  = "Shared Assets/Groups/" + it.getName();

			Category node = mediaArchive.createCategoryPath(path);
			newcollection.setValue("categoryid",node.getId());
			collections.saveData(newcollection);
			manager.addAssetToCollection(mediaArchive, newcollection.getId(), hits);

		}


	}


	HitTracker users = mediaArchive.getSearcher("user").getAllHits();
	users.each{
		HitTracker hits = searcher.query().exact("viewusers", it.id).search();
		if(hits.size() > 0){

			Data newcollection = collections.searchById("usercol-${it.id}");
			if(newcollection == null){
				newcollection = collections.createNewData();
				newcollection.setId("usercol-${it.id}");
				newcollection.setName(it.getName());
			}
			String path  = "Shared Assets/Users/" + it.getName();

			Category node = mediaArchive.createCategoryPath(path);
			newcollection.setValue("categoryid",node.getId());
			collections.saveData(newcollection);
			manager.addAssetToCollection(mediaArchive, newcollection.getId(), hits);

		}


	}






}








//- If there is a library with assets that has no child collections, it becomes a collection.  Library is Deleted   DONE
//- assignDivisions() Assign Divisions to Collections directly if the parent library has a division
//- If a library has child collections and is empty, do nothing
//  createProjects() - If a library has child collections and itself isn't empty, create a new collection underneath that library with the same name and move assets from the library to the collection with the same name




//moveAssetsToCollections();  Done with the importpermissions script


//convertLibrariesToCollections();
assignDivisions();
createProjects();
setupAssetPermissions();




//migratePermissions();