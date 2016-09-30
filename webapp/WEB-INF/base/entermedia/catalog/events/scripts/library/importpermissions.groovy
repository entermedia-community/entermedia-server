package users

import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker

public void init() 
{
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	Searcher libraries = mediaArchive.getSearcher("library");
	libs = libraries.getAllHits();
	libs.each {
		Data library =  it;
		if( library.get("categoryid") == null )
		{
			String path = library.get("folder");
			if( path == null)
			{
				path = library.getName();
			}
			Category node = mediaArchive.createCategoryTree(path);
			library.setValue("categoryid", node.getId() );
			libraries.saveData(library);
		}
		Category node = mediaArchive.getData("category",library.get("categoryid") );
		
		HitTracker users = mediaArchive.getSearcher("libraryusers").query().match("_parent",library.getId()).search();
		users.each {
			node.addValue("viewusers",it.userid);
		}	

		HitTracker groups = mediaArchive.getSearcher("librarygroups").query().match("libraryid",library.getId()).search();
		groups.each {
			node.addValue("viewgroups",it.groupid);
		}

		HitTracker roles = mediaArchive.getSearcher("libraryroles").query().match("libraryid",library.getId()).search();
		roles.each {
			node.addValue("viewroles",it.roleid);
		}
		mediaArchive.getCategorySearcher().saveData(node);

		log.info("saved  ${library.getName() }");
	}
	
}
	
init();