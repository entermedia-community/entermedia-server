package asset;

import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.search.AssetSearcher
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.util.DateStorageUtil

public void init(){
	
	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");
	
	
	Searcher autocompletesearcher = mediaarchive.getSearcher("assetAutoComplete");
	
	AssetSearcher searcher = mediaarchive.getAssetSearcher();
	
	HitTracker hits = autocompletesearcher.getAllHits();
	hits.each{
		Data word = autocompletesearcher.loadData(it);
		
		def search = word.get("synonyms");
		
		HitTracker assets = searcher.query().freeform("description", search).search();
		
		if(assets.size() == 0){
			autocompletesearcher.delete(word, null)
		} else{
			word.setProperty("hitcount", String.valueOf( assets.size() ) );
			word.setProperty("timestamp", DateStorageUtil.getStorageUtil().formatForStorage(new Date()) );
			autocompletesearcher.saveData(word);
		}
		
		
		
		
	}
	
	
	
}





init();