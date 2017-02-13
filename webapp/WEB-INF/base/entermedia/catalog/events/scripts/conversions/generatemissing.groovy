package conversions;

import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.scanner.PresetCreator
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery

public void init()
{
		MediaArchive mediaarchive = context.getPageValue("mediaarchive");//Search for all files looking for videos
		Searcher assetsearcher = mediaarchive.getAssetSearcher();
		Searcher tasksearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "conversiontask");
		PresetCreator presets = mediaarchive.getPresetManager();

		//HitTracker assets = assetsearcher.getAllHits();
//		SearchQuery q = assetsearcher.createSearchQuery();
//		q.addOrsGroup("importstatus", "imported reimported");
		SearchQuery q = assetsearcher.createSearchQuery().append("id", "*");
		q.addNot("editstatus","7");
		q.addSortBy("id");
		HitTracker assets =  assetsearcher.search(q);
		assets.enableBulkOperations();
		
		log.info("Processing ${assets.size()}" + q	);
		
		long added = 0;
		long checked  = 0;
		long logcount  = 0;
		long completed = 0;
		List tosave = new ArrayList();
		for (Data hit in assets)
		{
			checked++;
			logcount++;
			
//			Asset asset = mediaarchive.getAssetBySourcePath(hit.getSourcePath());
//			if( asset == null )
//			{
//				log.info("Missing" + hit.getSourcePath() );
//				continue; //Bad index
//			}

			Collection more = presets.createMissingOnImport(mediaarchive, tasksearcher, hit);
			added = added + more.size();
			if( logcount == 1000 )
			{
				logcount = 0;
				log.info("Assets Checked: ${checked} Conversions Found: ${added}");
			}
//			if( more == 0 && !"converting".equals(asset.get("previewstatus") ) )
//			{
//				//log.info("complete ${asset}");
//				asset.setProperty("previewstatus","converting");
//				//mediaarchive.saveAsset(asset, null);
//				tosave.add(asset);
//				completed++;
//			}
//			if( tosave.size() == 500 )
//			{
//				mediaarchive.saveAssets(tosave);
//				tosave.clear();
//				log.info("checked ${checked} assets. ${added} tasks queued, ${completed} completed. please run event again since index has changed order" );
//				
//			}
		}
//		mediaarchive.saveAssets(tosave);
		log.info("Assets Checked: ${checked} Conversions Found: ${added}");

}

init();