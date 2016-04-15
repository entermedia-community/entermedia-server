package org.entermedia.elasticsearch.searchers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

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
		
		
//		convertSearcher.reindexInternal();
//		q.addMatches("_id",found.getId());
//		 hits  = convertSearcher.search(q);
//		assertTrue(hits.size() > 0);

	}
	
	
	public void testLargeIndex() throws Exception
	{
		
		BaseElasticSearcher convertSearcher = (BaseElasticSearcher) getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "conversiontask");
		List data = new ArrayList();
		for (int i = 0; i < 10000; i++)
		{
			Data newone = convertSearcher.createNewData();
			newone.setProperty("submitted", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
			newone.setProperty("status","new");
			newone.setSourcePath("testing/new");
			newone.setProperty("assetid","101");
			newone.setId(String.valueOf(i));
			data.add(newone);
			if(data.size() == 10000){
			convertSearcher.updateInBatch(data, null);
			data.clear();
			}

		}
	

		convertSearcher.updateInBatch(data, null);

				
		Data found = (Data)convertSearcher.searchById("127");
		assertNotNull(found);

		
		SearchQuery q = convertSearcher.createSearchQuery();
		q.addMatches("_id",found.getId());
		Collection hits  = convertSearcher.search(q);
		assertTrue(hits.size() > 0);
		
		Date start = new Date();
		convertSearcher.getElasticNodeManager().reindexInternal(getMediaArchive().getCatalogId());
		Date end = new Date();
		long total = end.getTime() - start.getTime();
		q.addMatches("_id",found.getId());
		 hits  = convertSearcher.search(q);
		assertTrue(hits.size() > 0);

	}
	
	
}
