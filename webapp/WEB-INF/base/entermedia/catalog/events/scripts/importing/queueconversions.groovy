package importing;

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.event.WebEvent

import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery;
import org.openedit.events.PathEventManager;
import java.util.ArrayList;
import java.util.Date;
import org.openedit.util.DateStorageUtil;

public void createTasksForUpload() 
{
	mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");//Search for all files looking for videos

	Searcher tasksearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "conversiontask");
	Searcher presetsearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "convertpreset");
	Searcher destinationsearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "publishdestination");
	
	Searcher publishqueuesearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "publishqueue");

	MediaArchive mediaArchive = context.getPageValue("mediaarchive");//Search for all files looking for videos
	Searcher targetsearcher = mediaArchive.getAssetSearcher();
	SearchQuery q = targetsearcher.createSearchQuery();
	String ids = context.getRequestParameter("assetids");
	if( ids == null)
	{
		
		//Do a search for importstatus of "added" -> "converted"
		q.addExact( "importstatus", "imported" );
	}
	else
	{
		String assetids = ids.replace(","," ");
		q.addOrsGroup( "id", assetids );
	}
	
	List assets = new ArrayList(targetsearcher.search(q) );

	boolean foundsome = false;
	log.debug("Found ${assets.size()} assets");
	assets.each
	{
		foundsome = false;
		Asset asset = mediaArchive.getAsset(it.id);
		
		String rendertype = mediaarchive.getMediaRenderType(asset.getFileFormat());
		SearchQuery query = presetsearcher.createSearchQuery();
		query.addMatches("onimport", "true");
		query.addMatches("inputtype", rendertype); //video

		HitTracker hits = presetsearcher.search(query);
		log.debug("Found ${hits.size()} automatic presets");
		hits.each
		{
			Data hit = it;
		//	Data newconversion = tasksearcher.createNewData();

			Data preset = (Data) presetsearcher.searchById(it.id);
			
			//TODO: Move this to a new script just for auto publishing
//			SearchQuery presetquery = destinationsearcher.createSearchQuery();
//			presetquery.addMatches("onimport", "true");
//		//	presetquery.addMatches("convertpreset", preset.id); //video
//			
//			HitTracker dest = destinationsearcher.search(presetquery);
//						
//			dest.each{
//				Data publishrequest = publishqueuesearcher.createNewData();
//				publishrequest.setSourcePath(asset.getSourcePath());
//				publishrequest.setProperty("status", "pending"); //pending on the convert to work
//				publishrequest.setProperty("assetid", asset.id);
//				publishrequest.setProperty("presetid", preset.id);
//				String nowdate = DateStorageUtil.getStorageUtil().formatForStorage(new Date() );
//				publishrequest.setProperty("date", nowdate);
//
//				publishrequest.setProperty("publishdestination", it.id);
//				String exportName=null;
//				if( preset.get("type") != "original")
//				{
//					exportName = mediaArchive.asExportFileName( asset, preset);
//				}
//				if( exportName == null)
//				{
//					inputpage = mediaArchive.getOriginalDocument(asset);
//					exportName = inputpage.getName();
//				}
//				publishrequest.setProperty ("exportname", exportName);
//				
//				publishqueuesearcher.saveData(publishrequest, context.getUser());
//				foundsome = true;
//			}
			
			String outputfile = preset.get("outputfile");

			if (!mediaarchive.doesAttachmentExist(outputfile, asset))
			{
				Data newTask = tasksearcher.createNewData();
				newTask.setSourcePath(asset.getSourcePath());
				newTask.setProperty("status", "new");
				newTask.setProperty("assetid", asset.id);
				newTask.setProperty("presetid", it.id);
				newTask.setProperty("ordering", it.get("ordering") );
				
				String nowdate = DateStorageUtil.getStorageUtil().formatForStorage(new Date() );
				newTask.setProperty("submitted", nowdate);
				tasksearcher.saveData(newTask, context.getUser());
				foundsome = true;
			}
		}
		if( foundsome )
		{
			asset.setProperty("importstatus","imported");
		}
		else
		{
			asset.setProperty("previewstatus","mime");
			asset.setProperty("importstatus","complete");
		}
		mediaarchive.saveAsset( asset, user );
	}
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



createTasksForUpload();

