package asset;

import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.openedit.hittracker.HitTracker

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

import groovy.json.JsonSlurper;

//{
//	"apikey": "58ihLBajjxDsfojGG7x2P8E1L1uU_kW6xZAzh2KVukAK",
//	"iam_apikey_description": "Auto generated apikey during resource-key operation for Instance - crn:v1:bluemix:public:watson-vision-combined:us-south:a/df4ae162ca2c4aea9f6a3860168258bc:e14115d8-3228-45f3-907e-5dcc09806653::",
//	"iam_apikey_name": "auto-generated-apikey-5f60d603-9783-424d-874b-51c10ece7045",
//	"iam_role_crn": "crn:v1:bluemix:public:iam::::serviceRole:Manager",
//	"iam_serviceid_crn": "crn:v1:bluemix:public:iam-identity::a/df4ae162ca2c4aea9f6a3860168258bc::serviceid:ServiceId-5a4fd783-cc38-4a41-9040-517a9e791509",
//	"url": "https://gateway.watsonplatform.net/visual-recognition/api"
//  }


//https://www.ibm.com/watson/developercloud/visual-recognition/api/v3/curl.html?curl#authentication

public void init() {
	MediaArchive archive = context.getPageValue("mediaarchive");
	HitTracker assets = archive.getAssetSearcher().getAllHits();
	String apikey = archive.getCatalogSettingValue("watsonkey");
	
	//curl -u "apikey:{apikey}" "https://gateway.watsonplatform.net/visual-recognition/api/v3/classify?url=https://watson-developer-cloud.github.io/doc-tutorial-downloads/visual-recognition/fruitbowl.jpg&version=2018-03-19"
	assets.each{
		Asset asset = archive.getAsset(it.id);
		//http://localhost:8080/mediadb/services/module/asset/downloads/preset/2018/06/5f/bef/Screenshot+from+2017-09-18+10-10-34.png/image1024x768.jpg
		String siteroot = context.findValue("siteroot");
		String mediadb = context.findValue("mediadbappid");		
		//String imageurl = "${siteroot}/${mediadb}/services/module/asset/downloads/preset/${it.sourcepath}/image1024x768.jpg";
		String imageurl = "https://watson-developer-cloud.github.io/doc-tutorial-downloads/visual-recognition/fruitbowl.jpg&version=2018-03-19";
		
		
		String finalurl = "https://gateway.watsonplatform.net/visual-recognition/api/v3/classify?url=${imageurl}";
		CloseableHttpClient httpclient;
		httpclient = HttpClients.createDefault();
		HttpRequestBase httpmethod = null;
		httpmethod = new HttpGet(finalurl);


		String enc = "apikey" + ":" + apikey;
		byte[] encodedBytes = Base64.encodeBase64(enc.getBytes());
		String authString = new String(encodedBytes);
		httpmethod.setHeader("Accept", "application/json");
		httpmethod.setHeader("Content-type", "application/json");
		httpmethod.setHeader("Authorization", "Basic " + authString);
		HttpResponse resp = httpclient.execute(httpmethod);
		
		if (resp.getStatusLine().getStatusCode() != 200) {
			log.info("Google Server error returned " + resp.getStatusLine().getStatusCode());
		}

		HttpEntity entity = resp.getEntity();
		String content = IOUtils.toString(entity.getContent());
		JsonParser parser = new JsonParser();
		JsonElement elem = parser.parse(content);
		// log.info(content);
		JsonObject json = elem.getAsJsonObject();
		JsonSlurper slurper = new JsonSlurper();
		ArrayList tags = new ArrayList();
		
		def data = slurper.parseText(content);
		data.images.each{
			it.classifiers.each{
				it.classes.each{
					String myclass = it.class;
					tags.add(myclass);
					String cats = it.type_hierarchy;
					Category root = archive.createCategoryPath(cats);
					asset.addCatergory(root);
					
				}
			}	
		
		}
		asset.setValue("watsontags", tags);
		archive.saveAsset(asset);
	}
}


init();
