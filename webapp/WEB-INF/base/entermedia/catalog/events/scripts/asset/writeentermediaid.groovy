package asset;


import org.entermediadb.asset.Asset
import org.entermediadb.asset.CompositeAsset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.search.AssetSearcher
import org.entermediadb.asset.xmp.XmpWriter
import org.openedit.Data
import org.openedit.hittracker.HitTracker



public void init()
{

	MediaArchive archive = context.getPageValue("mediaarchive");
	AssetSearcher searcher = archive.getAssetSearcher();
	
	HitTracker assets = searcher.query().exact("emidwritten", "false").match("fileformat", "jpg").search();
	log.info(assets.getSearchQuery());
	log.info("Running on " + assets.size() +" assets");
	ArrayList tosave = new ArrayList();
	XmpWriter writer = (XmpWriter) archive.getModuleManager().getBean("xmpWriter");
	int count = 0;
	for (Iterator iterator = assets.iterator(); iterator.hasNext();) {
		Data hit = iterator.next();
		
		Asset asset = archive.getAsset(hit.id);
		if(writeAsset(archive,writer,asset)) {
			log.info("Running on " + asset.getId());
			asset.setValue("emidwritten", "true");
			tosave.add(asset);
		}
		if(tosave.size() > 1000) {
			searcher.saveAllData(tosave, null);
			tosave.clear();
		}
		count ++;
		if(count > 5) {
			break;
		}
		
	}
	searcher.saveAllData(tosave, null);
	
	
}

public boolean writeAsset(MediaArchive archive,XmpWriter writer, Asset asset)
{
	if( archive.isTagSync(asset.getFileFormat() ) )
		{
			HashMap additionaldetail = new HashMap();
			boolean didSave = writer.saveMetadata(archive, asset, additionaldetail, true);
			if(!didSave){
				log.info("Failed to write metadata for asset " + asset.getId());
			
			}
			return didSave;
		}
		
	
}

init();