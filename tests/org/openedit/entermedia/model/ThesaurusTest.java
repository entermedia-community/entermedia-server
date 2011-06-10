package org.openedit.entermedia.model;

import java.util.Map;

import org.openedit.data.lucene.LuceneHitTracker;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.modules.DataEditModule;
import org.openedit.entermedia.modules.RelatedKeywordModule;

import com.openedit.WebPageRequest;


public class ThesaurusTest extends BaseEnterMediaTest {

	public ThesaurusTest(String inName) {
		super(inName);
	}

	public void xtestSuggestions() throws Exception
	{
		WebPageRequest req = getFixture().createPageRequest("/media/catalogs/photo/search/index.html");
		MediaArchive testcatalog = getMediaArchive("media/catalogs/photo");
		Asset asset = createAsset(testcatalog);
		asset.addKeyword("tom");
		testcatalog.saveAsset(asset, req.getUser());
		asset = createAsset(testcatalog);
		asset.setName("tom");
		testcatalog.saveAsset(asset, req.getUser());
				
		req.setRequestParameter("applicationid", testcatalog.getCatalogId());
		req.setRequestParameter("searchtype", "asset");
		req.setRequestParameter("field",new String[] {"description"});
		req.setRequestParameter("operation",new String[] {"startswith"});
		req.setRequestParameter("description.value", "tom" );
		
		//this is new - can we use the DataEditModule for all of this
		DataEditModule storesearch = (DataEditModule) getFixture().getModuleManager().getModule("DataEditModule");
		storesearch.search(req);
		LuceneHitTracker hits = (LuceneHitTracker)req.getPageValue("hits");
	 	
	 	RelatedKeywordModule thesaurus = (RelatedKeywordModule) getFixture().getModuleManager().getModule("ThesaurusModule");
	 	thesaurus.getSuggestions(req);
	 	
	 	Map suggestions = hits.getSearchQuery().getSuggestedSearches();
	 	assertNotNull(suggestions);
	 	assertTrue(suggestions.size() > 0);
	 	assertTrue(suggestions.containsKey("tom"));
	 	
	 	
	 	
	 	
	 	
	}
	
	public void testLookupSynonyms() throws Exception
	{
		/*
		WebPageRequest req = getFixture().createPageRequest("/media/search/results.html");
		
		
			
		req.setRequestParameter("searchtype", "compositeLucene");
		req.setRequestParameter("field",new String[] {"description"});
		req.setRequestParameter("operation",new String[] {"startswith"});
		req.setRequestParameter("description.value", "tom" );
		MultiSearchModule searchmod = (MultiSearchModule) getFixture().getModuleManager().getModule("MultiSearchModule");
		searchmod.multiSearch(req);
		
		
		
	 	ThesaurusModule thesaurus = (ThesaurusModule) getFixture().getModuleManager().getModule("ThesaurusModule");
	 	thesaurus.getSuggestions(req);
	 	
	 	
	 	
//	 	req.setRequestParameter("field", "description");
//	 	req.setRequestParameter("description.value", "tom");
	 	
	 	thesaurus.searchSuggestions(req);
	 	
	 	
	 	LuceneHitTracker hits = (LuceneHitTracker)req.getPageValue("hits");
	 	
	 	
	 	
	 	
	 	assertNotNull(hits);
	 	assertTrue(hits.size() > 0);
	 	
	 	for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			Document hit = (Document) iterator.next();
			String synonym  = hit.get("synonyms");
			String hitcount  = hit.get("hits");
			hitcount = new NumberUtils().SortableStr2int(hitcount);
			assertNotNull("hitcount");
			assertNotNull("synonyms");
			
		}
		*/
	}
	
	
}
