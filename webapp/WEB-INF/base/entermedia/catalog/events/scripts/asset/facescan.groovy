package asset

import org.entermediadb.asset.*
import org.entermediadb.asset.facedetect.FaceDetectManager
import org.json.simple.JSONArray
import org.openedit.Data
import org.openedit.hittracker.HitTracker
import org.openedit.locks.Lock

public void init()
{
	MediaArchive archive = context.getPageValue("mediaarchive");//Search for all files looking for videos

	Lock lock = archive.getLockManager().lockIfPossible("facescanning", "admin");
	
	if( lock == null)
	{
		log.info("("+archive.getCatalogId()+") Face scanning already in progress.");
		return;
	}

	try
	{	
		HitTracker hits = archive.query("asset").exact("facescancomplete", "false").exact("importstatus","complete").search();
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
				log.info("Facescan assets saved: " + saved);
				archive.saveAssets(tosave);
				tosave.clear();
			}
		}
		if(tosave.size() > 0) {
			archive.saveAssets(tosave);
			saved = saved +  tosave.size();
			log.info("Facescan assets saved: " + saved);
		}
		if( saved > 0)
		{
			archive.fireMediaEvent("facecompare", context.getUser());
		}
	}
	finally
	{
		archive.getLockManager().release(lock);
	}	
	
}


init();
