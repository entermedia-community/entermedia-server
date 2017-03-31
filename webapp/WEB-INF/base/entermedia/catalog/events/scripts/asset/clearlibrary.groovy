package assets

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery

public void init()
{
		MediaArchive mediaArchive = context.getPageValue("mediaarchive");//Search for all files looking for videos
		Searcher targetsearcher = mediaArchive.getAssetSearcher();
		SearchQuery q = targetsearcher.createSearchQuery();
		//q.addSortBy("id");
		q.addMatches("libraries", "printreadyfinal");
		HitTracker assets = targetsearcher.search(q);

		assets.enableBulkOperations();

		int count = 0;
		int edited = 0;
		log.info("Starting ${assets.size()}"); 
		List assetsToSave = new ArrayList();
		assets.each
		{
			Data hit =  it;
			count++;
			Asset asset = mediaArchive.getAssetBySourcePath(hit.getSourcePath());
			if( asset != null )
			{
				//q.addExact( "importstatus", "imported" );
				String libraries = asset.get("libraries");
				if( libraries != null && libraries.contains("printreadyfinal") )
				{
					edited++;
					libraries = libraries.replace("printreadyfinal", "");
					asset.setProperty("libraries", libraries);
					assetsToSave.add(asset)
					if(assetsToSave.size() == 100)
					{
							mediaArchive.saveAssets assetsToSave;
							assetsToSave.clear();
							log.info("checked " + count + " records. Edited " + edited);
					}
				}
			}
		}

		mediaArchive.saveAssets assetsToSave;
		log.info("checked " + count + " records. Edited " + edited);

}

init();