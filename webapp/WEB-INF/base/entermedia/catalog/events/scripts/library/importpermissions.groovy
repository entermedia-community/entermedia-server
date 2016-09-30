package users

import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.scanner.HotFolderManager
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.page.Page
import org.openedit.users.User

public void init() 
{
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	Searcher libraries = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "library");
	libs = libraries.getAllHits();
	libs.each {
		Data hit =  it;
		
		//Searcher searcher = getSearcherManager().getSearcher(inCatalogId, "libraryusers");
		
		log.info(hit);
	}
	
}
	
init();