package org.openedit.entermedia.controller

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

import org.openedit.entermedia.Asset
import org.openedit.entermedia.BaseEnterMediaTest
import org.openedit.entermedia.MediaArchive

import com.openedit.WebPageRequest
import com.openedit.page.Page

class RestTest extends BaseEnterMediaTest {


	public void testCreateAsset(){

		JsonSlurper slurper = new JsonSlurper();
		WebPageRequest req = getFixture().createPageRequest("/entermedia/services/json/asset/");
		req.setMethod("POST");
		Page page = getPage("/WEB-INF/data/entermedia/catalogs/testcatalog/json/Asset_POST.txt");
		String content = page.getContent();
		req.putPageValue("jsondata", content );
		getFixture().getEngine().executePathActions(req);
		String response = req.getPageValue("json");
		assertNotNull(response);
		def data = slurper.parseText(response);
		String id = data.id;
		assertNotNull(data.id);
		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
		Asset asset = archive.getAsset(id);
		assertNotNull(asset);
		assertEquals(id, asset.getId());
		assertEquals(id, "jsondata")
		assertEquals(asset.contenttype, "article")
		assertEquals(asset.description, "Article about a squirrel who waterskis");
	}

	public void testUpdateAndDelete(){
		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
		archive.getAssetSearcher().reIndexAll();
		JsonSlurper slurper = new JsonSlurper();
		WebPageRequest req = getFixture().createPageRequest("/entermedia/services/json/asset/jsondata");
		req.setMethod("PUT");
		Page page = getPage("/WEB-INF/data/entermedia/catalogs/testcatalog/json/Asset_PUT.txt");
		String content = page.getContent();
		req.putPageValue("jsondata", content );
		getFixture().getEngine().executePathActions(req);
		String response = req.getPageValue("json");
		assertNotNull(response);
		def data = slurper.parseText(response);
		String id = data.id;
		assertNotNull(data.id);
		Asset asset = archive.getAsset(id);
		assertNotNull(asset);
		assertEquals(id, asset.getId());
		assertEquals(asset.contenttype, "article")
		assertEquals(asset.description, "Image of a man wearing a jacket");
		
		 req = getFixture().createPageRequest("/entermedia/services/json/asset/jsondata");
		req.setMethod("DELETE");
		req.setRequestParameter("catalogid", "entermedia/catalogs/testcatalog");
		getFixture().getEngine().executePathActions(req);
		 asset = archive.getAsset("jsondata");
		assertNull(asset);
	}
	
	
	

	public void testSearch(){

		WebPageRequest req = getFixture().createPageRequest("/entermedia/services/json/search/asset/");
		JsonBuilder builder = new JsonBuilder();
		def root = builder.query {
			catalogid "entermedia/catalogs/testcatalog"
			searchtype "asset"
		}

		req.putPageValue("jsondata", builder.toPrettyString());
		getFixture().getEngine().executePathActions(req);
	}
}
