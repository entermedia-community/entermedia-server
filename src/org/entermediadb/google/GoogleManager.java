package org.entermediadb.google;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.common.OAuthProviderType;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.dom4j.Element;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.data.BaseData;
import org.openedit.entermedia.util.EmTokenResponse;
import org.openedit.hittracker.HitTracker;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.OutputFiller;
import org.openedit.util.XmlUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GoogleManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(GoogleManager.class);
	protected String fieldCatalogId;
	protected MediaArchive fieldMediaArchive;
	protected ModuleManager fieldModuleManager;	
	protected OutputFiller filler = new OutputFiller();
	protected XmlUtil fieldXmlUtil;
	
	
	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}
	protected MediaArchive getMediaArchive()
	{
		if (fieldMediaArchive == null)
		{
			fieldMediaArchive = (MediaArchive)getModuleManager().getBean(getCatalogId(),"mediaArchive");
		}
		return fieldMediaArchive;
	}
	public Results listDriveFiles(Data authinfo, String inParentId) throws Exception
	{
		//https://developers.google.com/drive/v3/reference/files/list
		//https://developers.google.com/drive/v3/web/search-parameters
		String url = "https://www.googleapis.com/drive/v3/files?orderBy="+ URLEncoder.encode("modifiedTime desc,name") + "&pageSize=1000&fields=*";

		String search = "'" + inParentId + "' in parents";
		url = url + "&q=" + URLEncoder.encode(search);
		
		//TODO: Add date query from the last time we imported

		Results results = new Results();

		//List one folders worth of files
		boolean keepgoing = false;
		do
		{
			keepgoing = populateMoreResults(authinfo, url, results);
		}
		while (keepgoing);

		return results;
	}

	protected boolean populateMoreResults(Data authinfo, String fileurl, Results results) throws Exception
	{
		if (results.getResultToken() != null)
		{
			fileurl = fileurl + "&pageToken=" + URLEncoder.encode(results.getResultToken(), "UTF-8");
		}
		JsonObject json = get(fileurl, authinfo);
		JsonElement pagekey = json.get("nextPageToken");
		if (pagekey != null)
		{
			results.setResultToken(pagekey.getAsString());
		}
		JsonArray files = json.getAsJsonArray("files");
		for (Iterator iterator = files.iterator(); iterator.hasNext();)
		{
			JsonObject object = (JsonObject) iterator.next();
			String name = object.get("name").getAsString();
			String id = object.get("id").getAsString();

			String mt = object.get("mimeType").getAsString();

			if (mt.equals("application/vnd.google-apps.folder"))
			{
				results.addFolder(object);
			}
			else
			{
				results.addFile(object);
			}
		}
		String keepgoing = json.get("incompleteSearch").getAsString();
		return Boolean.parseBoolean(keepgoing);
	}

	private JsonObject get(String inFileurl, Data authinfo) throws Exception
	{

		CloseableHttpClient httpclient;
		httpclient = HttpClients.createDefault();
		HttpRequestBase httpmethod = null;
		httpmethod = new HttpGet(inFileurl);
		String accesstoken = getAccessToken(authinfo);
		httpmethod.addHeader("authorization", "Bearer " + accesstoken);

		HttpResponse resp = httpclient.execute(httpmethod);

		if (resp.getStatusLine().getStatusCode() != 200)
		{
			log.info("Google Server error returned " + resp.getStatusLine().getStatusCode());
		}

		HttpEntity entity = resp.getEntity();
		String content = IOUtils.toString(entity.getContent());
		JsonParser parser = new JsonParser();
		JsonElement elem = parser.parse(content);
		//log.info(content);
		JsonObject json = elem.getAsJsonObject();
		if( json.get("error") != null) //Invalid Credentials
		{
			log.error("Could not connect API" + content);
			authinfo.setValue("httprequesttoken",null);
			getMediaArchive().getSearcher("oauthprovider").saveData(authinfo);
		}
		return json;

	}
	protected void saveFile(Data authinfo, Asset inAsset) throws Exception
	{
		CloseableHttpClient httpclient = HttpClients.createDefault();
		
//		GET https://www.googleapis.com/drive/v3/files/0B9jNhSvVjoIVM3dKcGRKRmVIOVU?alt=media
//		Authorization: Bearer <ACCESS_TOKEN>

		String url = "https://www.googleapis.com/drive/v3/files/" + inAsset.get("googleid") + "?alt=media";
		HttpRequestBase httpmethod = new HttpGet(url);
		String accesstoken = getAccessToken(authinfo);
		httpmethod.addHeader("authorization", "Bearer " + accesstoken);

		HttpResponse resp = httpclient.execute(httpmethod);

		if (resp.getStatusLine().getStatusCode() != 200)
		{
			log.info("Google Server error returned " + resp.getStatusLine().getStatusCode());
		}

		HttpEntity entity = resp.getEntity();
		
		ContentItem item = getMediaArchive().getOriginalContent(inAsset);
		
		File output = new File(item.getAbsolutePath());
		output.getParentFile().mkdirs();
		log.info("Google Manager Downloading " + item.getPath());
		filler.fill(entity.getContent(),new FileOutputStream(output),true);
		
		//getMediaArchive().getAssetImporter().reImportAsset(getMediaArchive(), inAsset);
		//ContentItem itemFile = getMediaArchive().getOriginalContent(inAsset);
		getMediaArchive().getAssetImporter().getAssetUtilities().getMetaDataReader().updateAsset(getMediaArchive(), item, inAsset);
		inAsset.setProperty("previewstatus", "converting");
		getMediaArchive().saveAsset(inAsset);
		getMediaArchive().fireMediaEvent( "assetimported", null, inAsset); //Run custom scripts?
		
//		if( assettype != null && assettype.equals("embedded") )
//		{
//			current.setValue("assettype","embedded");
//		}

		
	}	

	private String getAccessToken(Data authinfo) throws Exception
	{
		String accesstoken = authinfo.get("httprequesttoken"); //Expired in 14 days 
		if( accesstoken == null)
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
			accesstoken = oAuthResponse.getAccessToken();
			authinfo.setValue("httprequesttoken", accesstoken);
			getMediaArchive().getSearcher("oauthprovider").saveData(authinfo);
			
		}	
		return accesstoken;
	}

	
	
	private String getUserAccessToken(User user) throws Exception
	{
		String accesstoken = user.get("httprequesttoken"); //Expired in 14 days 
		Data authinfo  = getMediaArchive().getData("oauthprovider", "google");
		if( accesstoken == null)
		{
		
			OAuthClientRequest request = OAuthClientRequest.tokenProvider(OAuthProviderType.GOOGLE).setGrantType(GrantType.REFRESH_TOKEN).setRefreshToken(user.get("refreshtoken")).setClientId(authinfo.get("clientid")).setClientSecret(authinfo.get("clientsecret")).buildBodyMessage();
			OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
			//Facebook is not fully compatible with OAuth 2.0 draft 10, access token response is
			//application/x-www-form-urlencded, not json encoded so we use dedicated response class for that
			//Own response class is an easy way to deal with oauth providers that introduce modifications to
			//OAuth specification
			EmTokenResponse oAuthResponse = oAuthClient.accessToken(request, EmTokenResponse.class);
			// final OAuthAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request, "POST");
			// final OAuthAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request);
			accesstoken = oAuthResponse.getAccessToken();
			authinfo.setValue("httprequesttoken", accesstoken);
			getMediaArchive().getSearcher("user").saveData(user);
			
		}	
		return accesstoken;
	}
	
	
	public void syncAssets(Data inAuthinfo) 
	{
		try
		{
			Results results = listDriveFiles(inAuthinfo,"root");
			processResults(inAuthinfo,"Drive", results);
			getMediaArchive().fireSharedMediaEvent( "conversions/runconversions"); //this will save the asset as imported
		}
		catch ( Exception ex)
		{
			throw new OpenEditException(ex);
		}
		
	}

	protected void processResults(Data inAuthinfo,  String inCategoryPath, Results inResults) throws Exception
	{
 		if( createAssets(inAuthinfo, inCategoryPath,inResults.getFiles()) )
		{
			if( inResults.getFolders() != null)
			{
				for (Iterator iterator = inResults.getFolders().iterator(); iterator.hasNext();)
				{
					JsonObject folder = (JsonObject) iterator.next();
					String id = folder.get("id").getAsString();
					String foldername = folder.get("name").getAsString();
					foldername = foldername.trim();
					Results folderresults = listDriveFiles(inAuthinfo,id);
					String categorypath = inCategoryPath +  "/" + foldername;
					processResults(inAuthinfo,categorypath,folderresults);
				}
			}	
		}

	}

	protected boolean createAssets(Data authinfo, String categoryPath, Collection inFiles) throws Exception
	{
		if( inFiles == null)
		{
			return true;
		}
		Category category = getMediaArchive().createCategoryPath(categoryPath);

		ContentItem item = getMediaArchive().getContent("/WEB-INF/" + getMediaArchive() + "/originals/" + categoryPath);
		File realfile = new File(item.getAbsolutePath());
		realfile.mkdirs();
		long leftkb = realfile.getFreeSpace()  / 1000;
		//FileSystemUtils.freeSpaceKb(item.getAbsolutePath()); 
		String free = getMediaArchive().getCatalogSettingValue("min_free_space");
		if( free == null)
		{
			free = "3000000";
		}


		
		Map onepage = new HashMap();
		for (Iterator iterator = inFiles.iterator(); iterator.hasNext();)
		{
			JsonObject object = (JsonObject) iterator.next();
			String id = object.get("id").getAsString();
			onepage.put(id,object);
			JsonElement fs = object.get("size");
			if( fs != null)
			{
				String size = fs.getAsString();
				
				if( size != null)
				{
					leftkb = leftkb - (Long.parseLong( size ) / 1000);
					if( leftkb < Long.parseLong( free) ) 
					{
						log.info("Not enough disk space left to download more " + leftkb + "<" + free );
						return false;
					}
				}
			}
			
			if(onepage.size() == 100)
			{
				createAssetsIfNeeded(authinfo,onepage, category);
				onepage.clear();
			}
		}
		createAssetsIfNeeded(authinfo,onepage,category);
		return true;
	}
	
	private void createAssetsIfNeeded(Data authinfo, Map inOnepage, Category category) throws Exception
	{
		if( inOnepage.isEmpty() )
		{
			log.info("empty map"); 
			return;
		}
		Collection tosave = new ArrayList();

		HitTracker existingassets = getMediaArchive().getAssetSearcher().query().orgroup("googleid", inOnepage.keySet()).search();
		log.info("checking " + existingassets.size() + " assets ");
		//Update category
		for (Iterator iterator = existingassets.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			Asset existing = (Asset)getMediaArchive().getAssetSearcher().loadData(data);
			//Remove existing assets
			inOnepage.remove(existing.get("googleid"));
			//existing.clearCategories();
			if( !existing.isInCategory(category) )
			{
				//Clear old Drive categorties
				Category root = getMediaArchive().createCategoryPath("Drive");
				Collection existingcategories = new ArrayList( existing.getCategories());
				for (Iterator iterator2 = existingcategories.iterator(); iterator2.hasNext();)
				{
					Category drive = (Category ) iterator2.next();
					if( root.isAncestorOf(drive) )
					{
						existing.removeCategory(drive);
					}
				}
				existing.addCategory(category);
				getMediaArchive().saveAsset(existing);
				log.info("Asset moved categories " + existing );
			}
		}

		//new Assets
		for (Iterator iterator = inOnepage.keySet().iterator(); iterator.hasNext();)
		{
			String id = (String) iterator.next();
			JsonObject object = (JsonObject)inOnepage.get(id);
		
			//log.info(object.get("kind"));// "kind": "drive#file",
			//	String md5 = object.get("md5Checksum").getAsString();
			Asset newasset = (Asset) getMediaArchive().getAssetSearcher().createNewData();
			String filename = object.get("name").getAsString();
			filename = filename.trim();
			//JsonElement webcontentelem = object.get("webContentLink");

			newasset.setSourcePath(category.getCategoryPath() + "/" + filename);
			newasset.setFolder(false);
			newasset.setValue("googleid", id);
			newasset.setValue("assetaddeddate", new Date());
			newasset.setValue("retentionpolicy", "deleteoriginal");  //Default
			//TODO: Add dates here
			
			newasset.setName(filename);
			JsonElement jsonElement = object.get("webViewLink");
			if (jsonElement != null)
			{
				newasset.setValue("linkurl", jsonElement.getAsString());

			}
//				JsonElement thumbnailLink = object.get("thumbnailLink");
//				if (thumbnailLink != null)
//				{
//					newasset.setValue("fetchthumbnailurl", thumbnailLink.getAsString());
//				}

			
			//			newasset.setValue("md5hex", md5);
			newasset.addCategory(category);

			//inArchive.getAssetSearcher().saveData(newasset);
			tosave.add(newasset);
		}
		if( ! tosave.isEmpty() )
		{
			getMediaArchive().saveAssets(tosave);
			log.info("Saving new assets " + tosave.size() );
			for (Iterator iterator = tosave.iterator(); iterator.hasNext();)
			{
				Asset asset = (Asset) iterator.next();
				saveFile(authinfo, asset);
			}
		}
	}
	
	public ArrayList syncContacts(User inAuthinfo) 
	{
		try
		{
			return listContacts(inAuthinfo, null);
			
		}
		catch ( Exception ex)
		{
			throw new OpenEditException(ex);
		}
		
	}
	public ArrayList listContacts(User inAuthinfo, String inSearchTerm) throws Exception
	{
		String url = "https://www.google.com/m8/feeds/contacts/default/full?v=3.0";
		if(inSearchTerm != null){
			url = url + "&q=" +  URLEncoder.encode(inSearchTerm);
		}
		ArrayList quicklist = new ArrayList();
		
		CloseableHttpClient httpclient;
		httpclient = HttpClients.createDefault();
		HttpRequestBase httpmethod = null;
		httpmethod = new HttpGet(url);
		String accesstoken = getUserAccessToken(inAuthinfo);
		httpmethod.addHeader("authorization", "Bearer " + accesstoken);

		HttpResponse resp = httpclient.execute(httpmethod);

		if (resp.getStatusLine().getStatusCode() != 200)
		{
			log.info("Google Server error returned " + resp.getStatusLine().getStatusCode() + ":"  + resp.getStatusLine().getReasonPhrase());
			String returned = EntityUtils.toString(resp.getEntity());
			log.info(returned);

		}

		HttpEntity entity = resp.getEntity();
		Element root = getXmlUtil().getXml(entity.getContent(), "UTF-8");
		for (Iterator iterator = root.elementIterator("entry"); iterator.hasNext();)
		{
			Element type = (Element) iterator.next();
			try
			{
				BaseData googledata = new BaseData();
				String email = type.element("email").attributeValue("address");
				String display = type.element("title").getText();
				googledata.setName(display + "(" + email + ")");
				googledata.setId(email);
				quicklist.add(googledata);
			}
			catch (Exception e)
			{
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}
		
		return quicklist;
		
	}

	public XmlUtil getXmlUtil()
	{
		if (fieldXmlUtil == null)
		{
			fieldXmlUtil = new XmlUtil();
			
		}

		return fieldXmlUtil;
	}

	public void setXmlUtil(XmlUtil inXmlUtil)
	{
		fieldXmlUtil = inXmlUtil;
	}
	
	
	
}
