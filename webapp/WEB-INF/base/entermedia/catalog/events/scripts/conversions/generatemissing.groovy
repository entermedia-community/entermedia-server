import com.openedit.page.Page
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.*;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.*;
import org.openedit.util.DateStorageUtil;

public void init()
{
		MediaArchive mediaarchive = context.getPageValue("mediaarchive");//Search for all files looking for videos
		Searcher assetsearcher = mediaarchive.getAssetSearcher();
		Searcher presetsearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "convertpreset");
		Searcher tasksearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "conversiontask");
		
		HitTracker assets = assetsearcher.getAllHits();
		context.putSessionValue(assets.getSessionId(),assets);
		context.setRequestParameter("hitssessionid",assets.getSessionId() );
		List assetsToSave = new ArrayList();
		long added = 0;
		long checked  = 0;
		
		int numPages = assets.getTotalPages();
		log.info("Pages: " + numPages);
		for(int i = 0; i < numPages; i++)
		{
			context.setRequestParameter("page", String.valueOf(i + 1));
			HitTracker assetpage = assetsearcher.loadPageOfSearch(context);
			log.info("checked thumbs for ${checked} assets. ${added} tasks queued" );
			for (Data hit in assetpage)
			{
				checked++;
				Asset asset = mediaarchive.getAssetBySourcePath(hit.get("sourcepath"));
				
				//TODO: use a hash map for this?
				String rendertype = mediaarchive.getMediaRenderType(asset.getFileFormat());
				SearchQuery query = presetsearcher.createSearchQuery();
				query.addMatches("onimport", "true");
				query.addMatches("inputtype", rendertype);
	
				HitTracker hits = presetsearcher.search(context,query);
				hits.each
				{
					Data preset = (Data) presetsearcher.searchById(it.id);
					String outputfile = preset.get("outputfile");
	
					if (!mediaarchive.doesAttachmentExist(outputfile, asset))
					{
						added++;
						Data newTask = tasksearcher.createNewData();
						newTask.setSourcePath(asset.getSourcePath());
						newTask.setProperty("status", "new");
						newTask.setProperty("assetid", asset.id);
						newTask.setProperty("presetid", it.id);
						newTask.setProperty("ordering", it.get("ordering") );
						
						String nowdate = DateStorageUtil.getStorageUtil().formatForStorage(new Date() );
						newTask.setProperty("submitted", nowdate);
						tasksearcher.saveData(newTask, context.getUser());
					}
				}
			}
		}
		log.info("added ${added} tasks"); 
}

init();