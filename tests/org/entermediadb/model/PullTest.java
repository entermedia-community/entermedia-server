package org.entermediadb.model;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.SyncModule;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.util.HttpMimeBuilder;

public class PullTest extends BaseEnterMediaTest
{
	public PullTest(String inName)
	{
		super(inName);
	}

	//	public void testParse() throws Exception
	//	{
	//		String json =  "results: [{\"shutterspeed\":\"1/40\",\"previewstatus\":\"2\",\"iso\":\"50\",\"md5hex\":\"6a725b00bf58ba41ce66add97406bb85\",\"hasfulltext\":false,\"filesize\":404649,\"sourcepath\":\"Collections/General/Checkin/Sub1/20180420_113751.jpg\",\"editstatus\":\"1\",\"assetviews\":1,\"geo_point\":{\"lon\":-99.0819444444444,\"lat\":19.4355555555556},\"recordmodificationdate\":\"2018-05-15T04:14:34.555Z\",\"pages\":0,\"datatype\":\"original\",\"mastereditclusterid\":\"testcluster\",\"headline_int\":{},\"id\":\"AWQT2LQLHJI6cjAsq6NC\",\"previewtatus\":\"0\",\"assetcreationdate\":\"2018-04-20T15:37:51.000Z\",\"assettype\":\"photo\",\"height\":1458,\"imageorientation\":\"3\",\"owner\":\"admin\",\"focallength\":\"2.1 mm\",\"assetmodificationdate\":\"2018-04-20T19:56:02.000Z\",\"importstatus\":\"complete\",\"length\":null,\"pushstatus\":\"resend\",\"assetvotes\":0,\"publisheds3\":false,\"duplicate\":false,\"assetaddeddate\":\"2018-05-04T21:22:28.000Z\",\"fromviz\":false,\"badge\":[\"asset_editstatus_1\",\"asset_duplicate_false\"],\"aperture\":\"1.7\",\"colorspace\":\"1\",\"bitspersample\":\"8\",\"width\":2592,\"name\":\"20180420_113751.jpg\",\"fileformat\":\"jpg\",\"category\":[\"index\"],\"assettitle\":\"sdsd\",\"isfolder\":false,\"collectionid\":\"AWMs_9ZG_WtfuuJ3hwPJ\"}]";
	//
	//		Collection array = new JsonUtil().parseArray("results", json);
	//		getMediaArchive().getAssetSearcher().saveJson(array);
	//
	//		Asset asset = getMediaArchive().getAsset("AWQT2LQLHJI6cjAsq6NC");
	//		assertNotNull(asset);
	//		
	//		
	//	}

	public void testDataPull() throws Exception
	{

		HttpMimeBuilder builder = new HttpMimeBuilder();

		HttpPost postMethod = null;
		try
		{
			String url = "https://em9dev.entermediadb.org/assets/mediadb/services/lists/create/purpose?entermedia.key=adminmd5421c0af185908a6c0c40d50fd5e3f16760d5580bc";

			String payload = "{\"id\": \"purpose\", \"name\": \"I have a purpose\" }";

			CloseableHttpClient httpClient = HttpClients.createDefault();
			HttpPost postRequest = new HttpPost(url);

			StringEntity input = new StringEntity(payload);
			input.setContentType("application/json");
			postRequest.setEntity(input);

			HttpResponse response = httpClient.execute(postRequest);

			assertEquals(200, response.getStatusLine().getStatusCode());

			MediaArchive archive = getMediaArchive();
			
			
				//Create the purpose entry on the demo
			Searcher editingcluster = archive.getSearcher("editingcluster");

			Data nodeinfo = (Data) editingcluster.searchById("demo");

			if (nodeinfo == null)
			{
				nodeinfo = editingcluster.createNewData();
				nodeinfo.setId("demo");
				nodeinfo.setValue("entermediakey", "adminmd5421c0af185908a6c0c40d50fd5e3f16760d5580bc");
				nodeinfo.setValue("baseurl", "https://em9dev.entermediadb.org/assets/");

			}
			nodeinfo.setValue("lastpulldate", null);
			editingcluster.saveData(nodeinfo);

			SyncModule module = (SyncModule) getFixture().getModuleManager().getModule("SyncModule");

			WebPageRequest req = getFixture().createPageRequest("/assets/mediadb");
			req.setRequestParameter("catalogid", archive.getCatalogId());
			
			//See if we already have a "purpose" entry locally.  If we do delete it.
			
			Data purpose = archive.getData("purpose", "purpose");
			if (purpose != null)
			{
				archive.getSearcher("purpose").delete(purpose, null);
			}
			
			
			//Sync the data from the remote demo server
			
			module.processDataQueue(req);
			
			//Validate that Purpose now exists locally.
			
			
			purpose = archive.getData("purpose", "purpose");
			assertNotNull(purpose);

		}
		catch (Exception e)
		{

		}
	}

}
