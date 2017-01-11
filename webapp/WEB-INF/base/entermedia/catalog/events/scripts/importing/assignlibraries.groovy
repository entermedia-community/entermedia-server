package importing

import model.assets.LibraryManager

import org.entermediadb.asset.MediaArchive
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker

public void setupLibraries()
{
	MediaArchive mediaarchive = context.getPageValue("mediaarchive");
	Searcher assetsearcher = mediaarchive.getAssetSearcher();

	HitTracker assets = assetsearcher.getAllHits();
	assets.enableBulkOperations();
	LibraryManager librarymanager = new LibraryManager();
	librarymanager.log = log;
	librarymanager.assignLibraries(mediaarchive, assets);

}
setupLibraries();

