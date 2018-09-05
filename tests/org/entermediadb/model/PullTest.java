package org.entermediadb.model;

import java.util.Collection;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.util.JsonUtil;


public class PullTest extends BaseEnterMediaTest 
{
	public PullTest(String inName) 
	{
		super(inName);
	}

	public void testParse() throws Exception
	{
		String json =  "results: [{\"shutterspeed\":\"1/40\",\"previewstatus\":\"2\",\"iso\":\"50\",\"md5hex\":\"6a725b00bf58ba41ce66add97406bb85\",\"hasfulltext\":false,\"filesize\":404649,\"sourcepath\":\"Collections/General/Checkin/Sub1/20180420_113751.jpg\",\"editstatus\":\"1\",\"assetviews\":1,\"geo_point\":{\"lon\":-99.0819444444444,\"lat\":19.4355555555556},\"recordmodificationdate\":\"2018-05-15T04:14:34.555Z\",\"pages\":0,\"datatype\":\"original\",\"mastereditclusterid\":\"testcluster\",\"headline_int\":{},\"id\":\"AWQT2LQLHJI6cjAsq6NC\",\"previewtatus\":\"0\",\"assetcreationdate\":\"2018-04-20T15:37:51.000Z\",\"assettype\":\"photo\",\"height\":1458,\"imageorientation\":\"3\",\"owner\":\"admin\",\"focallength\":\"2.1 mm\",\"assetmodificationdate\":\"2018-04-20T19:56:02.000Z\",\"importstatus\":\"complete\",\"length\":null,\"pushstatus\":\"resend\",\"assetvotes\":0,\"publisheds3\":false,\"duplicate\":false,\"assetaddeddate\":\"2018-05-04T21:22:28.000Z\",\"fromviz\":false,\"badge\":[\"asset_editstatus_1\",\"asset_duplicate_false\"],\"aperture\":\"1.7\",\"colorspace\":\"1\",\"bitspersample\":\"8\",\"width\":2592,\"name\":\"20180420_113751.jpg\",\"fileformat\":\"jpg\",\"category\":[\"index\"],\"assettitle\":\"sdsd\",\"isfolder\":false,\"collectionid\":\"AWMs_9ZG_WtfuuJ3hwPJ\"}]";

		Collection array = new JsonUtil().parseArray("results", json);
		getMediaArchive().getAssetSearcher().saveJson(array);

		Asset asset = getMediaArchive().getAsset("AWQT2LQLHJI6cjAsq6NC");
		assertNotNull(asset);
		
		
	}
}
