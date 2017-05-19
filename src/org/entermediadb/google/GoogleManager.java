package org.entermediadb.google;

import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.common.OAuthProviderType;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.entermedia.util.EmTokenResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GoogleManager
{

	private static final Log log = LogFactory.getLog(GoogleManager.class);

	
	public String listDriveFile(Data authinfo, List filelist, String startkey) throws Exception
	{
		String fileurl = "https://www.googleapis.com/drive/v3/files?fields=*";
		if(startkey != null){
			fileurl = fileurl + "&pageToken=" + URLEncoder.encode(startkey, "UTF-8");;
		}
		
	
		try
		{
			
			JsonElement elem = get(fileurl, "get", authinfo);
		    JsonObject	array = elem.getAsJsonObject();
			JsonElement pagekey = array.get("nextPageToken");
			if(pagekey != null){
				startkey = pagekey.getAsString();
			} else{
				startkey = null;
			}

		    JsonArray files = array.getAsJsonArray("files");
		    for (Iterator iterator = files.iterator(); iterator.hasNext();)
			{
				JsonObject object = (JsonObject) iterator.next();
				String name = object.get("name").getAsString();
				String id = object.get("id").getAsString();
				filelist.add(object);

				
			}
		    

			
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		finally{

		}
		
		return startkey;
		
		
	}

	private JsonElement get(String inFileurl, String method,  Data authinfo) throws Exception
	{
		
		CloseableHttpClient httpclient;
		httpclient = HttpClients.createDefault();
		String accesstoken = getAccessToken(authinfo);
		HttpRequestBase httpmethod = null;
		if("get".equals(method)){
		httpmethod = new HttpGet(inFileurl);
		httpmethod.addHeader("authorization", "Bearer " + accesstoken);
		}
	
		
		HttpResponse resp = httpclient.execute(httpmethod);
		
		if( resp.getStatusLine().getStatusCode() != 200 )
		{
			log.info("Google Server error returned " + resp.getStatusLine().getStatusCode());
		}

		HttpEntity entity = resp.getEntity();
		String content = IOUtils.toString(entity.getContent());
		JsonParser parser = new JsonParser();
		JsonElement elem = parser.parse(content);
		//log.info(content);
		return elem;
		
	}

	private String getAccessToken(Data authinfo) throws Exception
	{
		OAuthClientRequest request = OAuthClientRequest.tokenProvider(OAuthProviderType.GOOGLE).setGrantType(GrantType.REFRESH_TOKEN).setRefreshToken(authinfo.get("refreshtoken")).setClientId(authinfo.get("clientid")).setClientSecret(authinfo.get("clientsecret")).buildBodyMessage();
		OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
		//Facebook is not fully compatible with OAuth 2.0 draft 10, access token response is
		//application/x-www-form-urlencded, not json encoded so we use dedicated response class for that
		//Own response class is an easy way to deal with oauth providers that introduce modifications to
		//OAuth specification
		EmTokenResponse oAuthResponse = oAuthClient.accessToken(request, EmTokenResponse.class);
		// final OAuthAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request, "POST");
		// final OAuthAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request);
		String accessToken = oAuthResponse.getAccessToken();
		return accessToken;
	}
	
	
	
}
