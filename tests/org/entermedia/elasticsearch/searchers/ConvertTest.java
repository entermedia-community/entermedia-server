package org.entermedia.elasticsearch.searchers;

import java.util.Collection;
import java.util.Date;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.elasticsearch.searchers.BaseElasticSearcher;
import org.openedit.Data;
import org.openedit.hittracker.SearchQuery;
import org.openedit.util.DateStorageUtil;

public class ConvertTest extends BaseEnterMediaTest
{

	public void testCreateAndSearch() throws Exception
	{
		
		BaseElasticSearcher convertSearcher = (BaseElasticSearcher) getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "conversiontask");

		Data newone = convertSearcher.createNewData();
		newone.setProperty("submitted", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
		newone.setProperty("status","new");
		newone.setSourcePath("testing/new");
		newone.setProperty("assetid","101");

		convertSearcher.saveData(newone,null);
		
				
		Data found = (Data)convertSearcher.searchById(newone.getId());
		assertNotNull(found);

		
		SearchQuery q = convertSearcher.createSearchQuery();
		q.addMatches("_id",found.getId());
		Collection hits  = convertSearcher.search(q);
		assertTrue(hits.size() > 0);

	}
}
