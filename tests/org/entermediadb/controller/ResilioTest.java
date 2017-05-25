package org.entermediadb.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.resilio.ResilioFolder;
import org.entermediadb.resilio.ResilioManager;

import com.google.gson.JsonParser;

public class ResilioTest extends BaseEnterMediaTest {

	private static final Log log = LogFactory.getLog(ResilioTest.class);

	public void testAPI(){


		MediaArchive archive = getMediaArchive("media/catalogs/public");
	//	ResilioManager manager = (ResilioManager) archive.getSearcherManager().getModuleManager().getBean(archive.getCatalogId(),"resilioManager");
		ResilioManager manager = new ResilioManager();
	
		
		Collection <ResilioFolder>folders = manager.getFolders(archive);
		assertNotNull(folders);
		
		
		
		
	}


	
	
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
