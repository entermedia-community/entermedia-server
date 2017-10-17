package org.entermedia.elasticsearch;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.search.BaseAssetSearcher;
import org.entermediadb.elasticsearch.searchers.ElasticListSearcher;
import org.entermediadb.elasticsearch.searchers.ElasticXmlFileSearcher;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;


public class SearcherTest extends BaseEnterMediaTest
{

//public void testBasicRead() throws Exception
//{
//		// on startup
//		//Node node = NodeBuilder.nodeBuilder().client(true).node();
////    Settings settings = Settings.settingsBuilder()
////    		.put("cluster.name", "entermedia")
////            .build();
//	
//    Settings.Builder nb = Settings.builder();
//	nb.put("cluster.name", "entermedia");
//	nb.put("path.home", ".");
//    
////	Settings.Builder settings = ImmutableSettings.settingsBuilder().put("cluster.name", "entermedia");
////	Client client = new TransportClient(settings)
////	        .addTransportAddress(new InetSocketTransportAddress("localhost", 9300));
//
//	Client client = nb.build().c
//	
//	//	AdminClient admin = client.admin();
//	//	ActionFuture<CreateIndexResponse> indexresponse = admin.indices().create(new CreateIndexRequest("test"));
//	//	log(indexresponse.isDone() + " done ");
//		
//		IndexRequestBuilder builder = client.prepareIndex("media_catalogs_video", "asset", "102");
//		IndexResponse response = builder.setSource(XContentFactory.jsonBuilder()
//	                .startObject()
//	                    .field("user", "simon2")
//	                    .field("postDate", new Date())
//	                    .field("message", "saved to database")
//	                .endObject()
//	              )
//	    .setRefresh(true)
//	    .execute()
//	    .actionGet();
//		// on shutdown
//		SearchResponse results = client.prepareSearch("media_catalogs_video")
//		        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
//		        .setQuery(QueryBuilders.termQuery("user", "simon2"))
//		        .setFrom(0).setSize(60).setExplain(true)
////		        .addField("_all")
//		        .execute()
//		        .actionGet();
//		SearchHit hit = results.getHits().iterator().next();
//		
//		//client.g
//		Object message = hit.getSource().get("message");
//		assertEquals("saved to database",String.valueOf(message) );
//		client.close();
//	}
	
	public void testAssetConnectorSearcher()
	{
		BaseAssetSearcher searcher = (BaseAssetSearcher)getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "asset" );
		Asset asset = (Asset)searcher.createNewData();
		asset.setId("99");
		asset.setProperty("caption", "tester1");
		asset.setName("aname");
		asset.addCategory(getMediaArchive().getCategory("index"));
		asset.setSourcePath("users/aname");
		searcher.saveData(asset, null);
		
		Asset found = (Asset)searcher.searchById("99");
		assertNotNull(found);
		
		SearchQuery q = searcher.createSearchQuery();
		q.addMatches("caption","tester1");
		q.addOrsGroup("category","none other hope index");
		
		HitTracker tracker = searcher.search(q);
		assertEquals( 1, tracker.size() );
		
		Searcher approvalsearch = getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "approvals");
		Data approval = approvalsearch.createNewData();
		approval.setId("123");
		approval.setProperty("notes", "A note");
		approval.setProperty("assetid", "99");
		approval.setSourcePath(found.getSourcePath());
		approvalsearch.saveData(approval, null);
		
	}
	
	
	public void XXtestSearcher()
	{
		ElasticXmlFileSearcher searcher = (ElasticXmlFileSearcher)getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "states" );

		Data asset = searcher.createNewData();
		asset.setName("Bermuda");
		asset.setId("102");
		asset.setSourcePath("states/102");
		searcher.saveData(asset, null);
		
		SearchQuery q = searcher.createSearchQuery();
		q.addExact("name", "Bermuda");
		HitTracker tracker = searcher.search(q);
		assertTrue(tracker.size() > 0);
	}
	
	
	
	public void testListSearcher()
	{
		ElasticListSearcher searcher = (ElasticListSearcher)getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "somerandom" );

		assertTrue( searcher.getPropertyDetails().size() > 0 ); 	//Make sure webapp/entermedia/catalogs/testcatalog/_site.xconf has fallback 
		
		Data asset = searcher.createNewData();
		asset.setName("Bermuda");
		asset.setId("102");
		asset.setSourcePath("states/102");
		searcher.saveData(asset, null);

		Data hit = (Data)searcher.searchById("102");
		assertNotNull(hit);
		
		searcher.getAllHits();
		
		SearchQuery q = searcher.createSearchQuery();
		q.addExact("name", "Bermuda");
		HitTracker tracker = searcher.search(q);
		assertTrue(tracker.size() > 0);
	}
	
	
	
}
