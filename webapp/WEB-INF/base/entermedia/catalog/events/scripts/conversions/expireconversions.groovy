package conversions

import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker

public void init()
{
		MediaArchive archive = context.getPageValue("mediaarchive");//Search for all files looking for videos
		Searcher tasksearcher = archive.getSearcherManager().getSearcher(archive.getCatalogId(), "conversiontask");
		String expirytime = archive.getCatalogSettingValue("conversionexpirytime");
		int expire = 7;
		if(expirytime != null){
			expire = Integer.parseInt(expirytime);			
		}
		log.info("expire old conversions");
		
		//HitTracker tasks=tasksearcher.query().orgroup("status","new submitted retry missinginput").since("submitteddate", expire ).search(context);
		HitTracker tasks=tasksearcher.query().before("submitteddate", expire ).search(context);
		
		tasks.enableBulkOperations();
			
				
		ArrayList tasklist = new ArrayList();
		
		
		for (Data hit in tasks)
		{
			Data realtask = tasksearcher.searchById(hit.getId());
			realtask.setProperty("status","expired");		
			tasklist.add(realtask);
			if(tasklist.size() > 1000){
				tasksearcher.saveAllData(tasklist, null);
				tasklist.clear();
			}
		}
		tasksearcher.saveAllData(tasklist, null);
		
		
}

init();