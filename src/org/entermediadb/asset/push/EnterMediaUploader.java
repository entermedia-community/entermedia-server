package org.entermediadb.asset.push;

import java.io.File;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.entermediadb.asset.*;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.net.HttpMimeBuilder;
import org.entermediadb.net.HttpSharedConnection;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.users.User;

public class EnterMediaUploader implements MediaUploader
{
	protected HttpSharedConnection fieldHttpSharedConnection;
	
	
	protected HttpSharedConnection getHttpSharedConnection()
	{
		return fieldHttpSharedConnection;
	}

	protected void setHttpSharedConnection(HttpSharedConnection inHttpSharedConnection)
	{
		fieldHttpSharedConnection = inHttpSharedConnection;
	}

	@Override
	public boolean uploadOriginal(MediaArchive inArchive, Asset inAsset, Data inPublishDestination, User inUser)
	{
			try
			{
				//String url = (String) inMap.get("uploadurl");
						
				String mediadbid = (String)inPublishDestination.get("mediadb");
				String catalogid = (String) inPublishDestination.get("catalogid");
				String serverurl = (String) inPublishDestination.get("server");
				String assetid = (String) inAsset.getId();
				String name = (String) inAsset.get("filename");
				//String filepath = getWorkFolder() + "/assets/" + catalogid + "/" + assetid + "/" + name ;
				
				String url = null;
					url = serverurl + "/" + mediadbid + "/services/module/asset/create";
				String abspath = inArchive.getOriginalContent(inAsset).getAbsolutePath();
				File file = new File(abspath);
				HttpMimeBuilder builder = new HttpMimeBuilder();

				HttpPost method = new HttpPost(url);
				
				JSONObject inMap = new JSONObject(inAsset.getProperties());
				inMap.put("id", "published" + inAsset.getId());
				builder.addPart("metadata", inMap.toJSONString(), "application/json"); //What should this be called?
				builder.addPart("file.0", file);
				//builder.addPart("path", url);
				method.setEntity(builder.build());

				CloseableHttpResponse resp = getHttpSharedConnection().sharedPost(method);

				if (resp.getStatusLine().getStatusCode() != 200)
				{
					String returned = EntityUtils.toString(resp.getEntity());
					getHttpSharedConnection().release(resp);

				}
				
				
			}
			catch (Exception e)
			{
				// TODO Auto-generated catch block
				throw new RuntimeException(e);
			}

			return true;
	}

	@Override
	public boolean uploadGenerated(MediaArchive inArchive, Asset inAsset, Data inPublishDestination, User inUser)
	{
		// TODO Auto-generated method stub
		return false;
	}

}
