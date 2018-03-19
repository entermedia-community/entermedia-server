import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery

public void init()
{
		MediaArchive archive = context.getPageValue("mediaarchive");//Search for all files looking for videos
		Searcher tasksearcher = archive.getSearcherManager().getSearcher(archive.getCatalogId(), "conversiontask");
		
		log.info("clear errors");
		
		SearchQuery query = tasksearcher.createSearchQuery();
		query.addMatches("status", "error");
		
		HitTracker tasks = tasksearcher.search(query);
		tasks.enableBulkOperations();
		List all = new ArrayList(tasks);
		for (Data hit in all)
		{
			Data realtask = tasksearcher.searchById(hit.getId());
			realtask.setProperty("status","new");
			tasksearcher.saveData(realtask,null);
		}
}

init();