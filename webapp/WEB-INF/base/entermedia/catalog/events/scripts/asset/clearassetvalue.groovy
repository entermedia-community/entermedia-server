package asset

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.page.manage.PageManager
import org.openedit.repository.ContentItem


public init()
{
	
	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");
	PageManager pageManager = mediaarchive.getPageManager();
	
	Searcher searcher = mediaarchive.getSearcher("asset");
	HitTracker hits = searcher.query().exact("emidwritten", "true").not("editstatus","deleted").not("fileformat","json").not("fileformat","xml").not("fileformat","samp").not("fileformat","mp3").not("fileformat","idml").not("fileformat","epub").not("fileformat","txt").not("fileformat","docx").not("fileformat","xlsx").not("fileformat","wav").not("fileformat","avi").not("fileformat","xls").not("fileformat","doc").not("fileformat","bmp").not("fileformat","aiff").search();
	hits.enableBulkOperations();
	ArrayList tosave = new ArrayList();
	log.info("processing " + hits.size() + " assets");
	hits.each
	{
		Data data = (Data)(it);
		data.setValue("emidwritten", "false");
		data.setValue("emiderror", "false");
		data.setValue("xmperror", "false");
		
		tosave.add(data);
		if( tosave.size() > 1000)
		{
			mediaarchive.saveAssets(tosave);
			log.info("saved " + tosave.size() + " assets");
			tosave.clear();
		}
	}
	mediaarchive.saveAssets(tosave);
}

init();
