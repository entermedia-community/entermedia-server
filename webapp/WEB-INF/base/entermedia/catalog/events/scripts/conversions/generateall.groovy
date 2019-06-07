import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.scanner.PresetCreator
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker

public void init()
{
	 log.info("Clearing all generated media files " + user);
	
	 	MediaArchive archive = context.getPageValue("mediaarchive");//Search for all files looking for videos
		Searcher searcher = archive.getAssetSearcher();
		HitTracker assets = searcher.getAllHits();
		assets.enableBulkOperations();
		List assetsToSave = new ArrayList();
		PresetCreator presets = archive.getPresetManager();

		Searcher tasksearcher = archive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "conversiontask");
		tasksearcher.deleteAll(null);
		
		
		for (Data hit in assets)
		{
			Asset asset = archive.getAssetBySourcePath(hit.get("sourcepath"));
			archive.removeGeneratedImages(asset);
			asset.setProperty("importstatus", "imported");
			assetsToSave.add(asset);
			if(assetsToSave.size() == 1000)
			{
				archive.saveAssets( assetsToSave );
				assetsToSave.clear();
			}
			presets.createMissingOnImport(archive, tasksearcher, hit);
			
		}
		archive.saveAssets assetsToSave;

}

init();