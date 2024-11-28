package org.entermediadb.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.util.Replacer;

import com.google.gson.JsonParser;

public class RestTest extends BaseEnterMediaTest {

	private static final Log log = LogFactory.getLog(RestTest.class);

	public void testAPI(){


		MediaArchive archive = getMediaArchive("media/catalogs/public");

		Searcher endpoints = archive.getSearcher("endpoint");
		HitTracker apicalls = endpoints.getAllHits();
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HashMap map = new HashMap();
		map.put("applicationid", "mediadb");
		Replacer replacer = new Replacer();
		 
		
		for (Iterator iterator = apicalls.iterator(); iterator.hasNext();) {
		
			
		
		
			try {
				Data endpoint = (Data) iterator.next();
				String samplerequest = endpoint.get("samplerequest");
				String url = "http://localhost:8080" + endpoint.get("url");
				String method = endpoint.get("httpmethod");
				
				url = replacer.replace(url, map);
				if("POST".equalsIgnoreCase(method)){
					HttpPost postRequest = new HttpPost(
							url);
						
					StringEntity input = new StringEntity(samplerequest);
					input.setContentType("application/json");
					postRequest.setEntity(input);

					log.info("testing " + endpoint.getName());
					
					HttpResponse response = httpClient.execute(postRequest);
					assertTrue(verifyResponseIsJson(response));
					
					assertEquals(200, response.getStatusLine().getStatusCode());
					
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new OpenEditException(e);
			}
		
	}}


	
	
	public boolean verifyResponseIsJson(HttpResponse response) throws Exception{
		BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		
		StringBuffer result = new StringBuffer();
		String line = "";
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		try{
			String responsestring = result.toString();
			new JsonParser().parse(responsestring);
			
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
