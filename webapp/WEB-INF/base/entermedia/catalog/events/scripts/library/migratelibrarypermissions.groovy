package library

import org.entermediadb.asset.Asset
import org.entermediadb.asset.Category
import org.entermediadb.asset.MediaArchive
import org.entermediadb.projects.LibraryCollection
import org.entermediadb.projects.ProjectManager
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker

public void init() {
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



init();