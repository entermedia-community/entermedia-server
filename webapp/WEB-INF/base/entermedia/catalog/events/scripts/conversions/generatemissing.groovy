import com.openedit.page.Page
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.*;
import assets.model.PresetCreator;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.*;
import org.openedit.util.DateStorageUtil;

public void init()
{
		MediaArchive mediaarchive = context.getPageValue("mediaarchive");//Search for all files looking for videos
		Searcher assetsearcher = mediaarchive.getAssetSearcher();
		Searcher presetsearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "convertpreset");
		Searcher tasksearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "conversiontask");
		PresetCreator presets = new PresetCreator();
		
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

				if (!mediaarchive.doesAttachmentExist(asset,preset,0) )
				{
					presets.createPresetsForPage(tasksearcher, preset, asset,0);
				}
				String pages = asset.get("pages");
				if( pages != null )
				{
					int npages = Integer.parseInt(pages);
					if( npages > 1 )
					{
						for (int i = 1; i < npages; i++)
						{
							int pagenum = i + 1;
							if (!mediaarchive.doesAttachmentExist(asset,preset,pagenum) )
							{
								presets.createPresetsForPage(tasksearcher, preset, asset, pagenum);
							}
						}
					}
				}
		
					
//					Data found = tasksearcher.search(taskq).first();
//					if( found != null )
//					{
//						//If it is complete then the converter will mark it complete again
//						if( found.get("status") != "new")
//						{
//							found = (Data)tasksearcher.searchById(found.getId());
//							found.setProperty("status", "retry");
//							added++;
//							tasksearcher.saveData(found, context.getUser());
//						}
//					}
//					else
//					{
//						added++;
//						Data newTask = tasksearcher.createNewData();
//						newTask.setSourcePath(asset.getSourcePath());
//						newTask.setProperty("status", "new");
//						newTask.setProperty("assetid", asset.id);
//						newTask.setProperty("presetid", it.id);
//						newTask.setProperty("ordering", it.get("ordering") );
//						String nowdate = DateStorageUtil.getStorageUtil().formatForStorage(new Date() );
//						newTask.setProperty("submitted", nowdate);
//						tasksearcher.saveData(newTask, context.getUser());
//					}
			}
		}
		log.info("checked ${checked} assets. ${added} tasks queued" );
}

init();