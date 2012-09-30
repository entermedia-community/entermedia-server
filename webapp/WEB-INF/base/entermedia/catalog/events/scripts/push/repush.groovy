package push;
import java.text.SimpleDateFormat

import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArch	ive
import org.openedit.*;

import com.openedit.hittracker.*;

public void init()
{
		MediaArchive mediaArchive = context.getPageValue("mediaarchive");//Search for all files looking for videos
		Searcher targetsearcher = mediaArchive.getAssetSearcher();
		SearchQuery q = targetsearcher.createSearchQuery();
		q.addMatches("pushstatus", "complete");
		q.addBefore("pushdate", new SimpleDateFormat("MM/dd/yyyy").parse("7/16/2012") );
		q.addMatches("importstatus", "complete");
		q.addSortBy("id");
		HitTracker assets = targetsearcher.search(q);

		assets.setHitsPerPage(100000);

		int count = 0;
		log.info("Starting ${assets.size()}"); 
		List assetsToSave = new ArrayList();
		assets.each
		{
			Data hit =  it;
			count++;
			Asset asset = mediaArchive.getAssetBySourcePath(hit.getSourcePath());
			if( asset != null )
			{
				asset.setProperty("pushstatus", "resend");
				assetsToSave.add(asset)
				if(assetsToSave.size() == 100)
				{
						mediaArchive.saveAssets assetsToSave;
						assetsToSave.clear();
						log.info("checked ${count} records." );
				}
			}
		}

		mediaArchive.saveAssets assetsToSave;
		log.info("checked ${count} records." );

}

init();