package org.entermedia.elasticsearch;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.search.BaseAssetSearcher;
import org.entermediadb.elasticsearch.ElasticNodeManager;
import org.entermediadb.elasticsearch.searchers.ElasticAssetDataConnector;
import org.openedit.Data;


public class ReindexTest extends BaseEnterMediaTest
{


	
	public void testReindex()
	{
			
		
		BaseAssetSearcher searcher = (BaseAssetSearcher)getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "asset" );
		ElasticAssetDataConnector con = (ElasticAssetDataConnector) searcher.getDataConnector();
		ElasticNodeManager manager = con.getElasticNodeManager();
		
		
		
		
		Data asset = searcher.createNewData();
		asset.setName("Bermuda");
		asset.setId("102");
		asset.setSourcePath("states/102");
		searcher.saveData(asset, null);

		manager.reindexInternal("entermedia/catalogs/testcatalog");
		
		asset = null;
		
		asset = (Data) searcher.searchById("102");
		assertNotNull(asset);
		assertEquals("Bermuda", asset.getName());

	
		
		
		
	
	
	}
	
	
	
}
