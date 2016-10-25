package library;

import model.assets.LibraryManager

import org.entermediadb.asset.MediaArchive
import org.openedit.data.Searcher
import org.openedit.hittracker.*


public void readProjectData()
{
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	Searcher assetsearcher = mediaArchive.getAssetSearcher();
	HitTracker hits = assetsearcher.getAllHits();
	hits.enableBulkOperations();

	
	//Look for collections and libraries
	LibraryManager librarymanager = new LibraryManager();
	librarymanager.log = log;
	librarymanager.assignLibraries(mediaArchive, hits);

}

readProjectData();

