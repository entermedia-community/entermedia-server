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
		long added = 0;
		long checked  = 0;
		
		for (Data hit in assets)
		{
			checked++;
			Asset asset = mediaarchive.getAssetBySourcePath(hit.get("sourcepath"));
			if( asset == null )
			{
				continue; //Bad index
			}

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
		log.info("checked ${checked} assets. ${added} tasks queued" );
}

init();