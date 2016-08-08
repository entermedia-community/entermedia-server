package org.entermedia.elasticsearch.searchers;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.search.BaseAssetSearcher;
import org.entermediadb.elasticsearch.searchers.BaseElasticSearcher;
import org.junit.Test;
import org.openedit.data.PropertyDetail;
import org.openedit.hittracker.HitTracker;
import org.openedit.modules.translations.LanguageMap;

public class ElasticLanguageTest extends BaseEnterMediaTest
{
	@Test
	public void testLanguages()
	{
		
		BaseAssetSearcher searcher = (BaseAssetSearcher) getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "asset");
		BaseElasticSearcher ser = (BaseElasticSearcher) searcher.getDataConnector();
		ser.getElasticNodeManager().reindexInternal("entermedia/catalogs/testcatalog");
		searcher.getSearcherManager().setShowSearchLogs("entermedia/catalogs/testcatalog", true);
		PropertyDetail detail = searcher.getDetail("assettitle");
		assertTrue(detail.isMultiLanguage());
		Asset asset = (Asset) searcher.searchById("languages");
		if(asset == null){
			asset = (Asset) searcher.createNewData();
			asset.setId("languages");
			asset.setSourcePath("languages");
		}
		
		assertNull(asset.get("assettitle"));
		
		LanguageMap map = new LanguageMap();
		map.setText("fr","I am French");
		map.setText("de","I am German");
		
		asset.setValue("assettitle",map);
		
		
		

		searcher.saveData(asset);
	
		asset = (Asset) searcher.searchById("languages");
		Object assettitle = asset.getValue("assettitle");
		
		assertTrue(assettitle instanceof LanguageMap);
	
		asset.setProperty("assetttile", "I forgot it's multilanguage");
			
		searcher.saveData(asset);
		asset = (Asset) searcher.searchById("languages");
			
		
		assertTrue(assettitle instanceof LanguageMap);

		
		
		
		HitTracker hits = searcher.query().startsWith("assettitle.fr", "I").search();// .contain("assetttitle.fr", "French").search();
		assertTrue(hits.size() >0);
		 hits = searcher.query().contains("assettitle.fr", "french").search();// .contain("assetttitle.fr", "French").search();
		assertTrue(hits.size() >0);
		
		hits = searcher.fieldSearch("assetttitle.fr", "German");
		assertTrue(hits.size() ==0);
		
		//hits = searcher.fieldSearch("assetttitle", "French"); // all languages
		//assertTrue(hits.size() >0);
		
		
		
		
		
	}

}
