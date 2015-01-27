package org.openedit.entermedia.controller

import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.json.simple.JSONObject
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.BaseEnterMediaTest
import org.openedit.entermedia.MediaArchive

import com.openedit.hittracker.HitTracker
import com.openedit.util.Replacer

class RestTest extends BaseEnterMediaTest {



	public void testAPI(){


		MediaArchive archive = getMediaArchive("media/catalogs/public");

		Searcher endpoints = archive.getSearcher("endpoint");
		HitTracker apicalls = endpoints.getAllHits();
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HashMap map = new HashMap();
		map.put("applicationid", "mediadb");
		Replacer replacer = new Replacer();
		
		apicalls.each{
			Data endpoint = it;
			String samplerequest = endpoint.samplerequest;
			String url = "http://localhost:8080/" + endpoint.url;
			String method = endpoint.httpmethod;
			
			url = replacer.replace(url, map);
			if("POST".equalsIgnoreCase(endpoint.httpmethod)){
				HttpPost postRequest = new HttpPost(
						url);
					
				StringEntity input = new StringEntity(samplerequest);
				input.setContentType("application/json");
				postRequest.setEntity(input);

				HttpResponse response = httpClient.execute(postRequest);
				assertTrue(verifyResponseIsJson(response));
				
				assertEquals(201, response.getStatusLine().getStatusCode());
				
			}
		}
	}


	
	
	public boolean verifyResponseIsJson(HttpResponse response){
		BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		
		StringBuffer result = new StringBuffer();
		String line = "";
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		try{
			String responsestring = result.toString();
			JSONObject o = new JSONObject(result.toString());
			return true;
			
		} catch(Exception e){
			return false;
		}
	}

	//	public void testCreateAsset(){
	//
	//		JsonSlurper slurper = new JsonSlurper();
	//		WebPageRequest req = getFixture().createPageRequest("/mediadb/services/json/asset/");
	//		req.setMethod("POST");
	//		Page page = getPage("/entermedia/catalogs/testcatalog/json/Asset_POST.txt");
	//		String content = page.getContent();
	//		req.putPageValue("jsondata", content );
	//		req.setRequestParameter("catalogid", "entermedia/catalogs/testcatalog");//header is also checked
	//		getFixture().getEngine().executePathActions(req);
	//		String response = req.getPageValue("json");
	//		assertNotNull(response);
	//		def data = slurper.parseText(response);
	//		String id = data.id;
	//		assertNotNull(data.id);
	//		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
	//		Asset asset = archive.getAsset(id);
	//		assertNotNull(asset);
	//		assertEquals(id, asset.getId());
	//		assertEquals(id, "jsondata")
	//		assertEquals(asset.contenttype, "article")
	//		assertEquals(asset.description, "Article about a squirrel who waterskis");
	//	}
	//
	//	public void testCreateFromLocalFile(){
	//
	//		JsonSlurper slurper = new JsonSlurper();
	//		WebPageRequest req = getFixture().createPageRequest("/entermedia/services/json/asset/");
	//		req.setMethod("POST");
	//		Page page = getPage("/entermedia/catalogs/testcatalog/json/Asset_POST_LOCAL.txt");
	//		String content = page.getContent();
	//		req.putPageValue("jsondata", content );
	//		req.setRequestParameter("catalogid", "entermedia/catalogs/testcatalog");//header is also checked
	//		getFixture().getEngine().executePathActions(req);
	//		String response = req.getPageValue("json");
	//		assertNotNull(response);
	//		def data = slurper.parseText(response);
	//		String id = data.id;
	//		assertNotNull(data.id);
	//		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
	//		Asset asset = archive.getAsset(id);
	//		assertNotNull(asset);
	//		assertEquals(id, asset.getId());
	//		assertEquals(id, "localfile")
	//		assertEquals(asset.contenttype, "article")
	//		assertEquals(asset.description, "Article about a squirrel who waterskis");
	//	}
	//
	//	public void testCreateFromUrl(){
	//
	//		JsonSlurper slurper = new JsonSlurper();
	//		WebPageRequest req = getFixture().createPageRequest("/entermedia/services/json/asset/");
	//		req.setMethod("POST");
	//		Page page = getPage("/entermedia/catalogs/testcatalog/json/Asset_POST_FETCH.txt");
	//		String content = page.getContent();
	//		req.putPageValue("jsondata", content );
	//		req.setRequestParameter("catalogid", "entermedia/catalogs/testcatalog");//header is also checked
	//		getFixture().getEngine().executePathActions(req);
	//		String response = req.getPageValue("json");
	//		assertNotNull(response);
	//		def data = slurper.parseText(response);
	//		String id = data.id;
	//		assertNotNull(data.id);
	//		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
	//		Asset asset = archive.getAsset(id);
	//		assertNotNull(asset);
	//		assertEquals(id, asset.getId());
	//		assertEquals(id, "fromentermedia")
	//		assertEquals(asset.contenttype, "article")
	//		assertEquals(asset.description, "Article about a squirrel who waterskis");
	//	}
	//
	//	public void testSearch(){
	//
	//
	//				JsonSlurper slurper = new JsonSlurper();
	//				WebPageRequest req = getFixture().createPageRequest("/entermedia/services/json/search/asset");
	//				req.setMethod("POST");
	//				Page page = getPage("/entermedia/catalogs/testcatalog/json/Asset_SEARCH.txt");
	//				String content = page.getContent();
	//				req.setRequestParameter("catalogid", "entermedia/catalogs/testcatalog");//header is also checked
	//
	//				req.putPageValue("jsondata", content );
	//				getFixture().getEngine().executePathActions(req);
	//				String response = req.getPageValue("json");
	//				assertNotNull(response);
	//				def data = slurper.parseText(response);
	//				println data;
	//
	//
	//
	//			}
	//
	//	public void testUpdateAndDelete(){
	//		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
	//		archive.getAssetSearcher().reIndexAll();
	//		JsonSlurper slurper = new JsonSlurper();
	//		WebPageRequest req = getFixture().createPageRequest("/entermedia/services/json/asset/jsondata");
	//		req.setMethod("PUT");
	//		req.setRequestParameter("catalogid", "entermedia/catalogs/testcatalog");//header is also checked
	//
	//		Page page = getPage("/entermedia/catalogs/testcatalog/json/Asset_PUT.txt");
	//		String content = page.getContent();
	//		req.putPageValue("jsondata", content );
	//		getFixture().getEngine().executePathActions(req);
	//		String response = req.getPageValue("json");
	//		assertNotNull(response);
	//		def data = slurper.parseText(response);
	//		String id = data.id;
	//		assertNotNull(data.id);
	//		Asset asset = archive.getAsset(id);
	//		assertNotNull(asset);
	//		assertEquals(id, asset.getId());
	//		assertEquals(asset.contenttype, "article")
	//		assertEquals(asset.description, "Image of a man wearing a jacket");
	//
	//		req = getFixture().createPageRequest("/entermedia/services/json/asset/jsondata");
	//		req.setMethod("DELETE");
	//		req.setRequestParameter("catalogid", "entermedia/catalogs/testcatalog");
	//		getFixture().getEngine().executePathActions(req);
	//		asset = archive.getAsset("jsondata");
	//		assertNull(asset);
	//	}
	//
	//
	//	public void testCreateData(){
	//
	//		JsonSlurper slurper = new JsonSlurper();
	//		WebPageRequest req = getFixture().createPageRequest("/entermedia/services/json/data/channel");
	//		req.setMethod("POST");
	//		Page page = getPage("/entermedia/catalogs/testcatalog/json/DATA_POST.txt");
	//		String content = page.getContent();
	//		req.setRequestParameter("catalogid", "entermedia/catalogs/testcatalog");//header is also checked
	//
	//		req.putPageValue("jsondata", content );
	//		getFixture().getEngine().executePathActions(req);
	//		String response = req.getPageValue("json");
	//		assertNotNull(response);
	//		def data = slurper.parseText(response);
	//		String id = data.id;
	//		assertNotNull(data.id);
	//		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
	//		Data target = archive.getData("channel", "somedata")
	//		assertNotNull(target);
	//		assertEquals(target.name, "Adam Gillaspie")
	//
	//	}
	//
	//	public void testSearchData(){
	//
	//				JsonSlurper slurper = new JsonSlurper();
	//				WebPageRequest req = getFixture().createPageRequest("/entermedia/services/json/datasearch/channel");
	//				req.setMethod("POST");
	//				Page page = getPage("/entermedia/catalogs/testcatalog/json/DATA_SEARCH.txt");
	//				String content = page.getContent();
	//				req.setRequestParameter("catalogid", "entermedia/catalogs/testcatalog");//header is also checked
	//
	//				req.putPageValue("jsondata", content );
	//				getFixture().getEngine().executePathActions(req);
	//				String response = req.getPageValue("json");
	//				assertNotNull(response);
	//				def data = slurper.parseText(response);
	//				String id = data.id;
	//				assertNotNull(data.id);
	//				MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
	//				Data target = archive.getData("channel", "somedata")
	//				assertNotNull(target);
	//				assertEquals(target.name, "Adam Gillaspie")
	//
	//	}
	//
	//
	//	public void testPreprocessor(){
	//
	//				JsonSlurper slurper = new JsonSlurper();
	//				WebPageRequest req = getFixture().createPageRequest("/entermedia/services/json/publish");
	//				req.setMethod("POST");
	//				Page page = getPage("/entermedia/catalogs/testcatalog/json/PUBLISH.txt");
	//				String content = page.getContent();
	//				req.setRequestParameter("catalogid", "entermedia/catalogs/testcatalog");//header is also checked
	//
	//				req.putPageValue("jsondata", content );
	//				getFixture().getEngine().executePathActions(req);
	//				String param = req.getRequestParameter("id")
	//				assertEquals("somedata", param);
	//
	//
	//	}



}
