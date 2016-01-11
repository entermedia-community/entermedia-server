package org.entermedia.elasticsearch.searchers;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.elasticsearch.searchers.ElasticAutoCompleteSearcher;
import org.junit.Test;
import org.openedit.Data;
import org.openedit.hittracker.HitTracker;

public class ElasticAutoCompleteTest extends BaseEnterMediaTest
{
	@Test
	public void testSearchAssetSuggest()
	{
		ElasticAutoCompleteSearcher searcher = (ElasticAutoCompleteSearcher) getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "assetAutoComplete");
		assertNotNull("user searcher is NULL!", searcher);
		assertTrue("user searcher is elastic", searcher instanceof ElasticAutoCompleteSearcher);
		
		String word = "Test";
		HitTracker hits = getMediaArchive().getAssetSearcher().query().match("description",word).search();
		
		int count = hits.size();
		
		searcher.updateHits(hits, word);
		
		Data suggestion = (Data)searcher.query().startsWith("synonyms",word).searchOne();
		assertEquals( suggestion.get("hitcount"), String.valueOf(count) );
		
	}

}
