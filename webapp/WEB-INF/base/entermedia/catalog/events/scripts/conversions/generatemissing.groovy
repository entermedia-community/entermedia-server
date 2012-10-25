import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.scanner.PresetCreator;
import org.openedit.*;
import com.openedit.hittracker.*;

public void init()
{
		MediaArchive mediaarchive = context.getPageValue("mediaarchive");//Search for all files looking for videos
		Searcher assetsearcher = mediaarchive.getAssetSearcher();
		Searcher tasksearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "conversiontask");
		PresetCreator presets = new PresetCreator();

		HitTracker assets = assetsearcher.getAllHits();
		
		//SearchQuery q = assetsearcher.createSearchQuery().append("importstatus", "imported");
		//q.addSortBy("id");
		//HitTracker assets =  assetsearcher.search(q);
		
		assets.setHitsPerPage(10000);
		
		//TODO: Only check importstatus of imported?
		log.info("Processing ${assets.size()}" );
		
		long added = 0;
		long checked  = 0;
		long completed = 0;
		List tosave = new ArrayList();
		for (Data hit in assets)
		{
			checked++;
			Asset asset = mediaarchive.getAssetBySourcePath(hit.get("sourcepath"));
			if( asset == null )
			{
				continue; //Bad index
			}

			int more = presets.createMissingOnImport(mediaarchive, tasksearcher, asset);
			added = added + more;
			
			if( more == 0 && !"complete".equals(asset.get("importstatus") ) )
			{
				//log.info("complete ${asset}");
				asset.setProperty("importstatus","complete");
				//mediaarchive.saveAsset(asset, null);
				tosave.add(asset);
				completed++;
			}
			if( tosave.size() == 1000 )
			{
				mediaarchive.saveAssets(tosave);
				tosave.clear();
				log.info("checked ${checked} assets. ${added} tasks queued, ${completed} completed. please run event again since index has changed order" );
				
			}
		}
		mediaarchive.saveAssets(tosave);
		log.info("checked ${checked} assets. ${added} tasks queued , ${completed} completed." );
}

init();