package org.entermediadb.modules;

import java.util.Iterator;
import java.util.List;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.google.GoogleManager;
import org.openedit.Data;
import org.openedit.WebPageRequest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class GoogleModule extends BaseMediaModule
{

	protected GoogleManager fieldGoogleManager;
	
	public GoogleManager getGoogleManager()
	{
		return fieldGoogleManager;
	}

	public void setGoogleManager(GoogleManager inGoogleManager)
	{
		fieldGoogleManager = inGoogleManager;
	}

	public void syncAssets(WebPageRequest inReq) throws Exception{
		
		MediaArchive archive = getMediaArchive(inReq);
		Data authinfo = archive.getData("oauthprovider", "google");
		List files = getGoogleManager().listDriveFile(authinfo);
		for (Iterator iterator = files.iterator(); iterator.hasNext();)
		{
			JsonObject object = (JsonObject) iterator.next();
			String id = object.get("id").getAsString();
			
			String filename = object.get("name").getAsString();
			JsonElement webcontentelem = object.get("webContentLink");
	
			
			JsonElement jsonElement = object.get("webViewLink");
		
			
		//	String md5 = object.get("md5Checksum").getAsString();
			Category google = archive.createCategoryPath("/Google Drive/");//need to recreate folder structue still
			Data asset = (Asset) archive.getAssetSearcher().query().exact("googleid", id).searchOne();
			if(asset == null){
				Asset newasset = (Asset) archive.getAssetSearcher().createNewData();
				newasset.setValue("googleid", id);
				if(webcontentelem != null){

				newasset.setValue("fetchurl", webcontentelem.getAsString());
				}
				if(jsonElement != null){
					newasset.setValue("google-view-link", jsonElement.getAsString());

				}
//				newasset.setValue("md5hex", md5);
				newasset.addCategory(google);
				archive.getAssetSearcher().saveData(newasset);
			}
			
			
		}
		
		
		
	}
	
	
	
	
}
