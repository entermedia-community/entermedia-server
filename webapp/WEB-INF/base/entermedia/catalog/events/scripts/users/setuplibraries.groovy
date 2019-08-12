package users

import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.page.Page
import org.openedit.users.User

public void init()
{
		MediaArchive mediaArchive = context.getPageValue("mediaarchive");//Search for all files looking for videos
		
		boolean auto = Boolean.parseBoolean(mediaArchive.getCatalogSettingValue("autocreatelibraries"));
		if(!auto){
			return;
		}
		
		//HotFolderManager manager =mediaArchive.getModuleManager().getBean(mediaArchive.getCatalogId(), "hotFolderManager");
		
		String catalogId = mediaArchive.getCatalogId();
		
		
		
		Searcher searcher = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "user");
		Searcher profilesearcher = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "userprofile");
		
				Searcher libraries = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "library");
		Searcher collectionsearcher = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "librarycollection");
		
		
		
		//Loop over every user profile and move the userid colum into the id column
		String id = context.getRequestParameter("id");
		HitTracker profiles = null;
		if(id != null){
			profiles = searcher.fieldSearch("id",id);
		} else{
		    profiles = searcher.getAllHits();
		}
		profiles.enableBulkOperations();
		int ok = 0;
		profiles.each
		{
		
			Data hit =  it;
		
			
			//Create a library
			
			Data userlibrary = libraries.searchById("${hit.id}");
			if(userlibrary == null){
				userlibrary = libraries.createNewData();
				userlibrary.setId("${hit.id}");
				
				
			}
			userlibrary.setName("${it.firstName}'s Library");
			
			userlibrary.setProperty("folder", "${hit.id}");
			
			libraries.saveData(userlibrary);
			User user = mediaArchive.getUserManager().getUser(hit.getId());
			if(user != null){
			mediaArchive.getProjectManager().addUserToLibrary(mediaArchive, userlibrary,user);
			} 
			// create some collections
			HashMap collections = new HashMap();
			collections.put("documents", "Documents");
			collections.put("audio", "Audio");
			collections.put("videos", "Videos");
			collections.put("photos", "Photos");
			
			collections.keySet().each {
				
				String collectionpath = "/WEB-INF/data/" + catalogId + "/originals/${hit.id}/${it}/";
				Page colfolder = mediaArchive.getPageManager().getPage(collectionpath);
				if(!colfolder.exists()){
					
					mediaArchive.getPageManager().putPage(colfolder);
					
				}
				
				Data collection = collectionsearcher.searchById("${hit.id}-${it}-collection");
				if(collection == null){
					collection = collectionsearcher.createNewData();
					collection.setId("${hit.id}-${it}-collection");
					collection.setName(collections.get(it));
					collection.setProperty("library", userlibrary.getId());
					collection.setProperty("owner", hit.getId());
					collectionsearcher.saveData(collection);	
					
				}
				
				
			}
			
			
			
			
		}
		
		
		
}

init();