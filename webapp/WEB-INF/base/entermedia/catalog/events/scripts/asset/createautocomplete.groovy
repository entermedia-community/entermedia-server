package asset;

import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.search.AssetSearcher
import org.openedit.Data
import org.openedit.MultiValued
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.util.DateStorageUtil

public void init(){
	
	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");
	
	Searcher autocompletesearcher = mediaarchive.getSearcher("assetAutoComplete");
	
	AssetSearcher searcher = mediaarchive.getAssetSearcher();
	
	HitTracker hits = autocompletesearcher.getAllHits();
	List toDelete = new ArrayList();
	
	if (hits != null)
	{
		hits.enableBulkOperations();
	}
	hits.each{
		Data word = autocompletesearcher.loadData(it);
		
		
		if (word.getValue("autopopulated") == true)
		{
			toDelete.add(word);
		}		
		else {
			def search = word.get("synonyms");
			if(search){
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
	}
	autocompletesearcher.deleteAll(toDelete, user);
	
	Set<String> assetTags = new HashSet();
	HitTracker assets = searcher.getAllHits();
	assets.enableBulkOperations();
	List toSave= new ArrayList();
	int saves = 0;
	assets.each 
	{ 
		try
		{
			MultiValued asset = it;
			String text = asset.get("name");
			Collection tags = asset.getValues("keywords");
			
			if (tags != null ) {
				assetTags.addAll(tags);
			}
			//assetTags.add("\"" + asset.getName() + "\"");
			
			Data word = autocompletesearcher.createNewData();
			word.setId(text.toLowerCase());  //THIS makes them only save one copy
			word.setValue("synonyms", "\"" + text + "\"");
			word.setValue("timestamp", DateStorageUtil.getStorageUtil().formatForStorage(new Date()) );
			word.setValue("synonymsenc",text); //This can be removed if you change the search term to match		
			word.setValue("autopopulated",true);
			toSave.add(word);
	
	//		String parentfolder = org.openedit.util.PathUtilities.extractDirectoryPath(asset.getSourcePath());
	//		assetTags.add("\"" + parentfolder + "\"");
	
			if (toSave.size() > 3000)
			{
				autocompletesearcher.saveAllData(toSave, user);
				saves = saves + toSave.size();
				toSave.clear();
			}
		}
		catch ( Exception ex)
		{
			log.error("Could not save:" + it,ex);
		}	
	}
	
	autocompletesearcher.saveAllData(toSave, user);

	saves = saves + toSave.size();
	log.info("Added filenames " + saves);
	

	toSave.clear();
	
	log.info("Saving tags now " + assetTags.size());
	
	assetTags.each
	{
		String text = it;

		try
		{
			Data word = autocompletesearcher.createNewData();
			word.setId(text.toLowerCase());  //THIS makes them only save one copy
			word.setValue("synonyms", text);
			word.setValue("timestamp", DateStorageUtil.getStorageUtil().formatForStorage(new Date()) );
			word.setValue("synonymsenc",text); //This can be removed if you change the search term to match
			word.setValue("autopopulated",true);
			toSave.add(word);
			if (toSave.size() > 3000)
			{
				autocompletesearcher.saveAllData(toSave, user);
				toSave.clear();
			}
		}
		catch ( Exception ex)
		{
			log.error("Could not tag save:" + it,ex);
		}


	}
	autocompletesearcher.saveAllData(toSave, user);
	log.info("Compelte save" + assetTags.size());
	
}

init();