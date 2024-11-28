package org.entermedia.elasticsearch.searchers;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.data.ViewData;
import org.entermediadb.elasticsearch.searchers.ElasticViewSearcher;
import org.junit.Test;
import org.openedit.Data;

public class ElasticViewSearcherTest extends BaseEnterMediaTest
{
	@Test
	public void testVerifyConfiguration()
	{
		ElasticViewSearcher viewSearcher = (ElasticViewSearcher) getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "view");
		assertNotNull("user searcher is NULL!", viewSearcher);
		assertTrue("user searcher is elastic", viewSearcher instanceof ElasticViewSearcher);

		Data data = viewSearcher.createNewData();
		assertNotNull("data is NULL!", data);
		assertTrue(data instanceof ViewData);
		
		Data viewd = (Data)viewSearcher.searchById("entitypersongeneral");
		assertNotNull("Missing data", viewd);
		ViewData base = (ViewData)viewSearcher.loadData( viewd);
		
		assertNotNull("Show children", base.getChildren());

	}

	@Test
	public void testCreateNewData()
	{
		ElasticViewSearcher searcher = new ElasticViewSearcher();
		Data data = null;
	}

}
