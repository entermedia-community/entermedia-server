package importing;

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery

public void createTasksForUpload() throws Exception 
{

	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");//Search for all files looking for videos
	Searcher assetsearcher = mediaarchive.getAssetSearcher();

	Collection hits = context.getPageValue("hits");
	if( hits == null)
	{
		//There is a chance that the index is out of date.
	
		SearchQuery q = assetsearcher.createSearchQuery();
		String ids = context.getRequestParameter("assetids");
		//log.info("Found ${ids} assets from context ${context}");
		log.info("Running queueconversions on ${ids}");
		
		if( ids == null)
		{
			//Do a search for importstatus of "added" -> "converted"
			q.addOrsGroup( "importstatus", "imported reimported" );
		}
		else
		{
			String assetids = ids.replace(","," ");
			q.addOrsGroup( "id", assetids );
		}
	
		HitTracker tracker = assetsearcher.search(q);
		tracker.enableBulkOperations();
		hits = tracker;
	}
	if( hits.size() == 0 )
	{
		log.error("Problem with import, no asset found");
	}
	Searcher tasksearcher = mediaarchive.getSearcher("conversiontask");
	hits.each
	{
		try
		{
			Asset asset = assetsearcher.loadData(it);
			mediaarchive.getPresetManager().queueConversions(mediaarchive,tasksearcher,asset);
		}
		catch( Throwable ex)
		{
			log.error(it.id,ex);
		}
	}

}


createTasksForUpload();

