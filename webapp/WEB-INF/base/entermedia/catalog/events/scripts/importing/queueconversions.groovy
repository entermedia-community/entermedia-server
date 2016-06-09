
package importing;

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.scanner.PresetCreator
import org.openedit.Data
import org.openedit.MultiValued
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery
import org.openedit.page.Page
import org.openedit.util.DateStorageUtil

public void createTasksForUpload() throws Exception {

	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");//Search for all files looking for videos
	PresetCreator presets = mediaarchive.getPresetManager();
	
	Searcher tasksearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "conversiontask");
	Searcher presetsearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "convertpreset");
	Searcher destinationsearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "publishdestination");

	Searcher publishqueuesearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "publishqueue");

	MediaArchive mediaArchive = context.getPageValue("mediaarchive");//Search for all files looking for videos
	Searcher assetsearcher = mediaArchive.getAssetSearcher();

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
	
		hits = new ArrayList(assetsearcher.search(q) );
	}
	if( hits.size() == 0 )
	{
		log.error("Problem with import, no asset found");
	}
	boolean foundsome = false;
	List tosave = new ArrayList();
	List assetsave = new ArrayList();
	hits.each
	{
		//Lock lock = null;
		try{
			//lock = mediaArchive.lock("assetconversions/" + it.id, "queueconversions.createTasksForUpload");
			Asset asset = assetsearcher.loadData(it);
			//Data asset =  it;
			int counted = mediaarchive.getPresetManager().retryConversions(mediaarchive,tasksearcher,asset);
			if( counted > 0)
			{
				String rendertype = mediaarchive.getMediaRenderType(asset);
				if( foundsome )
				{
					asset.setProperty("importstatus","imported");
					if( asset.get("previewstatus") == null)
					{
						asset.setProperty("previewstatus","converting");
					}
					assetsave.add(asset);
				}
				else
				{
					if( asset.get("importstatus") != "needsdownload" )
					{
						asset.setProperty("importstatus","complete");
						if(asset.getProperty("fileformat") == "embedded")
						{
							asset.setProperty("previewstatus","2");
						}
						else
						{
							asset.setProperty("previewstatus","mime");
						}
						assetsave.add(asset);
					}
				}
			}	
		}
		catch( Throwable ex)
		{
			log.error(it.id,ex);
			//asset.setProperty("importstatus","error");
		}
//		finally
//		{
//			if( lock != null)
//			{
//				mediaArchive.releaseLock(lock);
//			}
//		}
	}

	mediaarchive.saveAssets( assetsave,user);
	tasksearcher.saveAllData(tosave, user);

	if( foundsome )
	{
		//PathEventManager pemanager = (PathEventManager)moduleManager.getBean(mediaarchive.getCatalogId(), "pathEventManager");
		//pemanager.runPathEvent("/${mediaarchive.getCatalogId()}/events/conversions/runconversions.html",context);
		mediaarchive.fireSharedMediaEvent("importing/importcomplete");
	}
	else
	{
		log.info("No assets found");
	}

}

private saveAutoPublishTasks(Searcher publishqueuesearcher, Searcher destinationsearcher, Searcher presetsearcher, Asset asset, MediaArchive mediaArchive) {
	SearchQuery autopublish = destinationsearcher.createSearchQuery();
	autopublish.addMatches("onimport", "true");

	HitTracker destinations = destinationsearcher.search(autopublish);

	destinations.each
	{
		MultiValued destination = it;
		Collection destpresets = destination.getValues("convertpreset");

		destpresets.each
		{
			String destpresetid = it;
			Data destpreset = presetsearcher.searchById(destpresetid);
			Data publishrequest = publishqueuesearcher.createNewData();
			publishrequest.setSourcePath(asset.getSourcePath());
			publishrequest.setProperty("status", "pending"); //pending on the convert to work
			publishrequest.setProperty("assetid", asset.id);
			publishrequest.setProperty("presetid", destpresetid);
			String nowdate = DateStorageUtil.getStorageUtil().formatForStorage(new Date() );
			publishrequest.setProperty("date", nowdate);

			publishrequest.setProperty("publishdestination", destination.id);
			String exportName=null;

			if( destpreset.get("type") != "original")
			{
				exportName = mediaArchive.asExportFileName( asset, destpreset);
			}
			if( exportName == null)
			{
				Page inputpage = mediaArchive.getOriginalDocument(asset);
				exportName = inputpage.getName();
			}
			publishrequest.setProperty ("exportname", exportName);

			publishqueuesearcher.saveData(publishrequest, context.getUser());
		}
	}
}


createTasksForUpload();

