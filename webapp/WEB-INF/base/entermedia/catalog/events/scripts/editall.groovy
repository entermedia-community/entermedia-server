import com.openedit.page.Page
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.*;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.*;

public void init()
{
		MediaArchive mediaArchive = context.getPageValue("mediaarchive");//Search for all files looking for videos
		Searcher targetsearcher = mediaArchive.getAssetSearcher();
		SearchQuery q = targetsearcher.createSearchQuery();
		q.addSortBy("assetaddeddateDown");
		q.addMatches("category", "index");
		HitTracker assets = targetsearcher.search(q);
		assets.setHitsPerPage(100);
		assets.setHitsName("hits");
		context.setRequestParameter("hitssessionid", assets.getSessionId());
		context.putSessionValue(assets.getSessionId(), assets);

		int count = 0;
		int edited = 0;

		int numPages = assets.getTotalPages();
		log.info("Pages: " + numPages);
		List assetsToSave = new ArrayList();
		for(int i = 0; i < numPages; i++)
		{
				context.setRequestParameter("page", String.valueOf(i + 1));
				HitTracker hits = targetsearcher.loadPageOfSearch(context);
				log.info("New page: " + i );
				hits.setPage(i+1);
				List page = hits.getPageOfHits();
				for(Iterator iterator = page.iterator(); iterator.hasNext();)
				{
						Data hit =  iterator.next();
						count++;
						Asset asset = mediaArchive.getAssetBySourcePath(hit.get("sourcepath"));
						if( asset != null && asset.get("previewstatus") != 0)
						{
								edited++;
								//q.addExact( "importstatus", "imported" );
								asset.setProperty("importstatus", "imported");
								//asset.setProperty("previewstatus", "0");
								assetsToSave.add(asset)
								if(assetsToSave.size() == 100)
								{
										mediaArchive.saveAssets assetsToSave;
										assetsToSave.clear();
								}
						}
				}
		}
		mediaArchive.saveAssets assetsToSave;
		log.info("checked " + count + " records. Edited " + edited);

}

init();