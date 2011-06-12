package conversions;

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.event.WebEvent

import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery;
import org.openedit.events.PathEventManager;
import java.util.ArrayList;

public void createTasksForUpload() 
{
	mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");//Search for all files looking for videos

	Searcher tasksearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "conversiontask");
	Searcher presetsearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "convertpreset");
	Searcher destinationsearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "publishdestination");
	
	Searcher publishqueuesearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "publishqueue");

	MediaArchive mediaArchive = context.getPageValue("mediaarchive");//Search for all files looking for videos
	log.info("Queue conversions running on " + mediaArchive.getCatalogId() );
	Searcher assetsearcher = mediaArchive.getAssetSearcher();
	SearchQuery q = assetsearcher.createSearchQuery();
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
	//Did a search for assetids but I got back the wrong assets
	List assets = new ArrayList(assetsearcher.search(q) );

	boolean foundsome = false;
	log.info("Found ${assets.size()} assets");
	assets.each
	{
		Asset asset = mediaArchive.getAsset(it.id);

		log.info("Adding conversions for " + it.id + " asset is " + asset.getName() + " from " + ids );
		
		String rendertype = mediaarchive.getMediaRenderType(asset.getFileFormat());
		//video?
		SearchQuery query = presetsearcher.createSearchQuery();
		query.addMatches("onimport", "true");
		query.addMatches("inputtype", rendertype); //video

		HitTracker hits = presetsearcher.search(query);
		log.info("Found ${hits.size()} automatic presets");
		hits.each
		{
			Data hit = it;
			Data newconversion = tasksearcher.createNewData();

			Data preset = (Data) presetsearcher.searchById(hit.id);
			
			SearchQuery presetquery = destinationsearcher.createSearchQuery();
			presetquery.addMatches("onimport", "true");
			presetquery.addMatches("presetid", preset.id); //video
			
			HitTracker dest = destinationsearcher.search(presetquery);
						
			dest.each
			{
				Data destination = it;
				Data publishrequest = publishqueuesearcher.createNewData();
				publishrequest.setSourcePath(asset.getSourcePath());
				publishrequest.setProperty("status", "pending"); //pending on the convert to work
				publishrequest.setProperty("assetid", asset.id);
				publishrequest.setProperty("presetid", preset.id);
				publishrequest.setProperty("publishdestination", destination.id);
				String exportName=null;
				if( preset.get("type") != "original")
				{
					exportName = mediaArchive.asExportFileName( asset, preset);
				}
				if( exportName == null)
				{
					inputpage = mediaArchive.getOriginalDocument(asset);
					exportName = inputpage.getName();
				}
				publishrequest.setProperty ("filename", exportName);
				
				publishqueuesearcher.saveData(publishrequest, context.getUser());
				foundsome = true;
			}
			
			String outputfile = preset.get("outputfile");

			if (!mediaarchive.doesAttachmentExist(outputfile, asset))
			{
				Data newTask = tasksearcher.createNewData();
				newTask.setSourcePath(asset.getSourcePath());
				newTask.setProperty("status", "new");
				newTask.setProperty("assetid", asset.id);
				newTask.setProperty("presetid", it.id);
				tasksearcher.saveData(newTask, context.getUser());
				foundsome = true;
			}
		}
		asset.setProperty("importstatus","complete");
		mediaarchive.saveAsset( asset, user );
	}
	if( foundsome )
	{
		log.info("Running runconversions");
		PathEventManager pemanager = (PathEventManager)moduleManager.getBean(mediaarchive.getCatalogId(), "pathEventManager");
		pemanager.runPathEvent("/${mediaarchive.getCatalogId()}/events/conversions/runconversions.html",context);
	}
	else
	{
		log.info("Not running runconversions");
		
	}
	
}



createTasksForUpload();

