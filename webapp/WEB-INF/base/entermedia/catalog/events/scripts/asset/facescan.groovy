package asset

import org.entermediadb.asset.*
import org.entermediadb.asset.facedetect.FaceDetectManager
import org.openedit.Data
import org.openedit.hittracker.HitTracker

public void init()
{
	MediaArchive archive = context.getPageValue("mediaarchive");//Search for all files looking for videos

	HitTracker hits = archive.query("asset").exact("facescancomplete", "false").exact("assettype","photo").search();
	hits.enableBulkOperations();
		
	int saved = 0;
	List tosave = new ArrayList();
	FaceDetectManager manager = archive.getBean("faceDetectManager");
	int found = 0;
	for(Data hit in hits)
	{
		Asset asset = archive.getAssetSearcher().loadData(hit);
		if( manager.extractFaces(archive, asset) )
		{
			tosave.add(asset);
		}
		if( tosave.size() == 1000 )
		{
			saved = saved +  tosave.size();
			log.info("saved " + saved);
			archive.saveAssets(tosave);
			tosave.clear();
		}
	}
	archive.saveAssets(tosave);
	saved = saved +  tosave.size();
	log.info("saved " + saved);
	
}


init();
