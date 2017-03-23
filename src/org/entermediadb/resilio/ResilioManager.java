package org.entermediadb.resilio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.entermediadb.asset.MediaArchive;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.OpenEditException;
import org.openedit.page.Page;

public class ResilioManager
{
	private static final Log log = LogFactory.getLog(ResilioManager.class);

	
	
	public HttpClient getClient()
	{

		RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT).build();
		CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(globalConfig).build();

		return httpClient;
	}

	
	public Collection<ResilioFolder> getFolders(MediaArchive inArchive) throws OpenEditException
	{

		try
		{
			ArrayList folders = new ArrayList();
			String authString = getAuthString(inArchive);

			HttpGet method = null;
			String fullpath = inArchive.getCatalogSettingValue("resilio_url") + "/api/v2/folders";
			method = new HttpGet(fullpath);
			method.setHeader("Authorization", "Basic " + authString);

			HttpClient client = getClient();

			HttpResponse response = client.execute(method);

			StatusLine sl = response.getStatusLine();
			int status = sl.getStatusCode();
			if (status >= 400)
			{
				throw new OpenEditException("error from server " + status + "  " + sl.getReasonPhrase());
			}
			String val = EntityUtils.toString(response.getEntity());

			JSONObject config = (JSONObject) new JSONParser().parse(val);
			log.info("Got : " + val);
			JSONObject data = (JSONObject) config.get("data");
			JSONArray folderlist = (JSONArray) data.get("folders");
			for (Iterator iterator = folderlist.iterator(); iterator.hasNext();)
			{
				JSONObject folderinfo = (JSONObject) iterator.next();
				ResilioFolder folder = new ResilioFolder();
				folder.setId((String)folderinfo.get("id"));
				folder.setValue("secretkey", folderinfo.get("secretkey"));
				folder.setValue("folderpath", folderinfo.get("path"));
				refreshFolder(inArchive, folder);
				folders.add(folder);

			}
			return folders;
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);

		}

	}

	private String getAuthString(MediaArchive inArchive)
	{
		String enc = inArchive.getCatalogSettingValue("resilio_username") + ":" + inArchive.getCatalogSettingValue("resilio_password");
		byte[] encodedBytes = Base64.encodeBase64(enc.getBytes());
		String authString = new String(encodedBytes);
		return authString;
	}

	public  String refreshFolder(MediaArchive inArchive, ResilioFolder inFolder) throws OpenEditException
	{

		try
		{
			String authString = getAuthString(inArchive);

			HttpGet method = null;
			String fullpath = inArchive.getCatalogSettingValue("resilio_url") + "/api/v2/folders/"+inFolder.getId()+"/activity";
			method = new HttpGet(fullpath);
			method.setHeader("Authorization", "Basic " + authString);

			HttpClient client = getClient();

			HttpResponse response = client.execute(method);

			StatusLine sl = response.getStatusLine();
			int status = sl.getStatusCode();
			if (status >= 400)
			{
				throw new OpenEditException("error from server " + status + "  " + sl.getReasonPhrase());
			}
			String val = EntityUtils.toString(response.getEntity());
			JSONObject config = (JSONObject) new JSONParser().parse(val);
			JSONObject data = (JSONObject) config.get("data");
			for (Iterator iterator = data.keySet().iterator(); iterator.hasNext();)
			{
				String key = (String) iterator.next();
				if("id".equals(key)){
					continue;
				}
				Object value = data.get(key);
				log.info("Key: " + key + "value: " + value );
				inFolder.setValue(key, value);
			}
			return val;
		}
		catch (Exception e)
		{
		throw new OpenEditException(e);
		}
		
		
		
	}
	
	public ResilioFolder getFolderByPath(MediaArchive inArchive, String inPath){
		Collection folders = getFolders(inArchive);
		inArchive.getPageManager().getPage(inPath).getContentItem().getAbsolutePath();
		for (Iterator iterator = folders.iterator(); iterator.hasNext();)
		{
			ResilioFolder folder = (ResilioFolder) iterator.next();
			if(inPath.equals(folder.get("folderpath"))){
				refreshFolder(inArchive, folder);
				return folder;
			}
		}
	return null;
	}
	
	public ResilioFolder getWorkingFolder(MediaArchive inArchive,  String inUserName){
		Collection folders = getFolders(inArchive);
		for (Iterator iterator = folders.iterator(); iterator.hasNext();)
		{
			ResilioFolder folder = (ResilioFolder) iterator.next();
			String folderpath = folder.get("folderpath");
			if(folderpath == null){
				continue;
			}
			if(folderpath.contains("workingfolders/" + inUserName )){
				refreshFolder(inArchive, folder);
				return folder;
			}
			
			
		
		}
	return null;
	}
	
	
	
	
	

}
