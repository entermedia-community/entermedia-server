import org.entermediadb.asset.MediaArchive
import org.entermediadb.location.Position
import org.entermediadb.projects.*
import org.openedit.Data

import org.openedit.data.BaseSearcher
import org.openedit.data.Searcher

public void init()
{
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");//Search for all files looking for videos
	BaseSearcher collectionsearcher = mediaArchive.getSearcher("librarycollection");
	String  id = data.getId();
	LibraryCollection collection = (LibraryCollection)collectionsearcher.searchById(id);
	Searcher librarysearcher = mediaArchive.getSearcher("library");
	log.info("User is: " + user.getId() );

	
	Data library = librarysearcher.searchById("collectives");
	if( library == null)
	{
		library = librarysearcher.createNewData();
		library.setId("collectives");
		library.setValue("owner", "admin");
		library.setName("Collectives");
		librarysearcher.saveData(library);
	}
	collection.setValue("library",library.getId());
	if( collection.get("owner") == null )
	{
		collection.setValue("owner",user.getId());
	}	

	collectionsearcher.saveData(collection);
	
	mediaArchive.getProjectManager().getRootCategory(mediaArchive,collection);
	
	BaseSearcher colectivesearcher = mediaArchive.getSearcher("collectiveproject");
	Data newproject = colectivesearcher.createNewData();
	newproject.setName("General");
	newproject.setValue("parentcollectionid",collection.getId());
	colectivesearcher.saveData( newproject );
	context.putPageValue("librarycol",collection);
}

init();