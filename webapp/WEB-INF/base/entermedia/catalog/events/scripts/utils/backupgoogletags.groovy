package utils

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.search.AssetSearcher
import org.entermediadb.asset.util.TimeParser
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.hittracker.HitTracker

public void init(){
	
	MediaArchive archive = context.getPageValue("mediaarchive");
	SearcherManager sm = archive.getSearcherManager();
	Searcher backupgoogletags = sm.getSearcher(archive.getCatalogId(), "backupgoogletags");
	
	AssetSearcher searcher = archive.getAssetSearcher();
	HitTracker hits = searcher.query().match("googletagged","true").search();
	hits.enableBulkOperations();
	ArrayList tosave = new ArrayList();
	
	hits.each{
		Asset asset = searcher.loadData(it);
		Data bkptags = backupgoogletags.createNewData();
		bkptags.setValue("assetid", asset.getId());
		bkptags.setValue("googlekeywords", asset.getValue("googlekeywords"));
		tosave.add(bkptags);
	}
	
	backupgoogletags.saveAllData(tosave, null);
	
}

public void restore(){
	
	MediaArchive archive = context.getPageValue("mediaarchive");
	SearcherManager sm = archive.getSearcherManager();
	Searcher backupgoogletags = sm.getSearcher(archive.getCatalogId(), "backupgoogletags");
	HitTracker hits = backupgoogletags.query().search();
	hits.enableBulkOperations();
	
	AssetSearcher searcher = archive.getAssetSearcher();
	
	ArrayList tosave = new ArrayList();
	Integer assetcount = 0;
	hits.each{
		Asset asset = archive.getAsset(it.assetid);
		if (asset!=null) {
			asset.setValue("googletagged", "true");
			asset.setValue("googlekeywords", it.getValue("googlekeywords"));
			tosave.add(asset);
			assetcount++;
			if( tosave.size() == 200)
			{
				archive.saveAssets(tosave);
				tosave.clear();
				log.info(assetcount+" assets tagged by Google.");
			}
		}
		/*Data bkptags = backupgoogletags.createNewData();
		bkptags.setValue("assetid", asset.getId());
		bkptags.setValue("googlekeywords", asset.getValue("googlekeywords"));
		tosave.add(bkptags);*/
	}
	
	archive.saveAssets(tosave);
	
}

restore();