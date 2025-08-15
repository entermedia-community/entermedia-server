package org.entermediadb.google;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.SSLException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.common.OAuthProviderType;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.dom4j.Element;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.sources.GoogleDriveAssetSource;
import org.entermediadb.location.GeoCoder;
import org.entermediadb.location.Position;
import org.entermediadb.net.HttpSharedConnection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.data.BaseData;
import org.openedit.entermedia.util.EmTokenResponse;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.users.UserSearcher;
import org.openedit.users.authenticate.PasswordGenerator;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.ExecutorManager;
import org.openedit.util.HttpMimeBuilder;
import org.openedit.util.HttpRequestBuilder;
import org.openedit.util.OutputFiller;
import org.openedit.util.URLUtilities;
import org.openedit.util.XmlUtil;



public class GoogleManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(GoogleManager.class);
	protected String fieldCatalogId;
	protected MediaArchive fieldMediaArchive;
	protected ModuleManager fieldModuleManager;
	protected OutputFiller filler = new OutputFiller();
	protected XmlUtil fieldXmlUtil;
	protected Date fieldTokenTime;
	protected HttpSharedConnection connection;
	
	protected GoogleDriveAssetSource fieldAssetSource;
	

	public GoogleDriveAssetSource getAssetSource()
	{
		return fieldAssetSource;
	}

	public void setAssetSource(GoogleDriveAssetSource inAssetSource)
	{
		fieldAssetSource = inAssetSource;
	}


	public Date getTokenTime()
	{
		return fieldTokenTime;
	}

	public void setTokenTime(Date inTokenTime)
	{
		fieldTokenTime = inTokenTime;
	}

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
			fieldMediaArchive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		}
		return fieldMediaArchive;
	}
	
	
	public Collection listDriveRootFolders() throws Exception
	{
		if (getAccessToken() == null)
		{
			return null;
		}
		// https://developers.google.com/drive/v3/reference/files/list
		// https://developers.google.com/drive/v3/web/search-parameters
		String url = "https://www.googleapis.com/drive/v3/files?orderBy=modifiedTime desc,name&pageSize=1000&fields=*"; //escaped later
		String search = "'root' in parents";
		url = url + "&q=" + search;
		// TODO: Add date query from the last time we imported
		log.info("Google Drive URL: "+url);

		Results results = new Results();

		// List one folders worth of files
		boolean keepgoing = false;
		//do
		{
			keepgoing = populateMoreResults(url, results, true);
		}
		//while (keepgoing);
		log.info("Finish listing Root Folders.");
		return results.getFolders();
	}
	
	

	public Results listDriveFiles( String inParentId) throws Exception
	{

		String rootParent = inParentId;
		//verify if inParentid is a Shortcut
		String url = "https://www.googleapis.com/drive/v3/files/" + inParentId + "?fields=*";
		url = URLUtilities.urlEscape(url);
		
		HttpSharedConnection connection = getConnection(); 
		connection.putSharedHeader("authorization", "Bearer " + getAccessToken());

		try
		{
			JSONObject json = connection.getJson(url);
			
			JSONObject shortcutDetails = (JSONObject)json.get("shortcutDetails");
			if (shortcutDetails  != null) {
				String targetid = (String)shortcutDetails.get("targetId");
				if (targetid != null)
				{
					rootParent = targetid;
				}
				
			}
			
			// https://developers.google.com/drive/v3/reference/files/list
			// https://developers.google.com/drive/v3/web/search-parameters
			url = "https://www.googleapis.com/drive/v3/files?orderBy=modifiedTime desc,name&pageSize=1000&fields=*"; //escaped later
			String search = "'" + rootParent + "' in parents";
			url = url + "&q=" + search;
			// TODO: Add date query from the last time we imported
			log.info("Google Drive URL: "+url);
	
			Results results = new Results();
	
			// List one folders worth of files
			boolean keepgoing = false;
			do
			{
				keepgoing = populateMoreResults(url, results, false);
			}
			while (keepgoing);
			
			return results;
		}
		catch (Throwable ex) {
			log.info("Error response: " + ex);
			return null;
			//throw new OpenEditException(ex);
		}
	}
	

	protected boolean populateMoreResults( String fileurl, Results results, Boolean foldersOnly) throws Exception
	{
		if (results.getResultToken() != null)
		{
			fileurl = fileurl + "&pageToken=" + results.getResultToken();
		}
		
		fileurl = URLUtilities.urlEscape(fileurl);
		HttpSharedConnection connection = getConnection(); 
		connection.putSharedHeader("authorization", "Bearer " + getAccessToken());
		
		try
		{
			JSONObject json = connection.getJson(fileurl);
			
			if (json == null)
			{
				return false;
			}
			String pagekey = (String)json.get("nextPageToken");
			if (pagekey != null)
			{
				results.setResultToken(pagekey);
			}
			JSONArray files = (JSONArray)json.get("files");
			log.info("Google Drive found: "+files.size()+" items.");
			for (Iterator iterator = files.iterator(); iterator.hasNext();)
			{
				JSONObject object = (JSONObject) iterator.next();
				String name = (String)object.get("name");
				String id = (String)object.get("id");
	
				String mt = (String)object.get("mimeType");
				
				log.info(name + " - File type: " + mt);
				
				if (mt.equals("application/vnd.google-apps.folder"))
				{
					results.addFolder(object);
				}
				else if (mt.equals("application/vnd.google-apps.shortcut")) {
					results.addFolder(object);
				}
				else if (!foldersOnly)
				{
					results.addFile(object);
				}
			}
			Boolean keepgoing = (Boolean) json.get("incompleteSearch");
			return keepgoing;
		}
		catch (Throwable ex) {
			log.info("Error response: " + ex);
			return false;
			//throw new OpenEditException(ex);
		}

	}


	public ContentItem loadFile( Asset inAsset, ContentItem inItem) throws Exception
	{

		if (inItem.exists()) 
		{
			return inItem;
		}
		
		String url = null;
		
		if(inAsset.getFileFormat() == null)
		{
			log.error("Invalid file format: " + inAsset);
			return null;
		}
		
		if( inAsset.getFileFormat().startsWith("gd"))
		{
			url = inAsset.get("thumbnaillink");
		}
		else
		{
			url = "https://www.googleapis.com/drive/v3/files/" + inAsset.get("embeddedid") + "?alt=media";

		}
		
		// GET
		// https://www.googleapis.com/drive/v3/files/0B9jNhSvVjoIVM3dKcGRKRmVIOVU?alt=media
		// Authorization: Bearer <ACCESS_TOKEN>
		HttpRequestBase httpmethod = new HttpGet(url);
		HttpSharedConnection connection = getConnection();
		connection.putSharedHeader("authorization", "Bearer " + getAccessToken());
		CloseableHttpResponse resp = connection.sharedExecute(httpmethod);
		try
		{
			if (resp.getStatusLine().getStatusCode() != 200)
			{
				log.info("Google Server error returned " + resp.getStatusLine().getStatusCode() + " " +  resp.getStatusLine() + " File: " + inAsset.getName());
				//throw new OpenEditException("Could not save: " + inAsset.getName());
				
				/*
				 * https://developers.google.com/workspace/drive/api/reference/rest/v3/files/get?apix_params=%7B%22fileId%22%3A%2216LawVTbF9eiC7sGghSNwBxRJwQ36das%22%2C%22fields%22%3A%22*%22%7D
				 * */
				
			}
	
			HttpEntity entity = resp.getEntity();
			
			File output = new File(inItem.getAbsolutePath());
			output.getParentFile().mkdirs();
			
			log.info("Google Manager Downloading " + inItem.getPath());
			filler.fill(entity.getContent(), new FileOutputStream(output), true);
		
			return inItem;
		}
		catch (Throwable ex) {
			log.info("Wrong format url: " + url);
			throw new OpenEditException(ex);
		}
		finally
		{
			connection.release(resp);
		}
		// if( assettype != null && assettype.equals("embedded") )
		// {
		// current.setValue("assettype","embedded");
		// }

	}

	public synchronized String getAccessToken() throws OpenEditException
	{
		try
		{
			Data authinfo = getMediaArchive().getData("oauthprovider", "google");
			
			String accesstoken = authinfo.get("httprequesttoken"); // Expired in 14 days
			Object accesstokendate = authinfo.getValue("accesstokentime");
			boolean force = false;

			if (accesstokendate instanceof Date)
			{
				Date ageoftoken = (Date) accesstokendate;

				if (ageoftoken != null)
				{
					Date now = new Date();
					long seconds = (now.getTime() - ageoftoken.getTime()) / 1000;
					Object expiresin = (Object) authinfo.getValue("expiresin");
					if (seconds > (Long.parseLong( expiresin.toString()) - 100))
					{
						force = true;
						log.info("Expiring token");
					}
				}
			}
			else
			{

				force = true;
			}

			if (accesstoken == null || force)
			{
				if (authinfo.get("clientid") == null || authinfo.get("clientsecret") == null)
				{
					log.error("oAuth Missing Client Id or Client Secret");
					return null;
				}
				
				OAuthClientRequest request = OAuthClientRequest.tokenProvider(OAuthProviderType.GOOGLE).
						setGrantType(GrantType.REFRESH_TOKEN).setRefreshToken(authinfo.get("refreshtoken")).
						setClientId(authinfo.get("clientid")).setClientSecret(authinfo.get("clientsecret")).buildBodyMessage();
				OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
				// Facebook is not fully compatible with OAuth 2.0 draft 10, access token
				// response is
				// application/x-www-form-urlencded, not json encoded so we use dedicated
				// response class for that
				// Own response class is an easy way to deal with oauth providers that introduce
				// modifications to
				// OAuth specification
				EmTokenResponse oAuthResponse = oAuthClient.accessToken(request, EmTokenResponse.class);
				// final OAuthAccessTokenResponse oAuthResponse =
				// oAuthClient.accessToken(request, "POST");
				// final OAuthAccessTokenResponse oAuthResponse =
				// oAuthClient.accessToken(request);
				accesstoken = oAuthResponse.getAccessToken();
				authinfo.setValue("httprequesttoken", accesstoken);
				authinfo.setValue("accesstokentime", new Date());
				Long expiresin = oAuthResponse.getExpiresIn();
				authinfo.setValue("expiresin", expiresin);
				getMediaArchive().getSearcher("oauthprovider").saveData(authinfo);

			}
			return accesstoken;
		}
		catch (OAuthSystemException | OAuthProblemException e)
		{
			return null;
			//throw new OpenEditException("Failed to retrieve or refresh access token", e);
		}
	}

	//TODO: Validate this token before running any API. Cache results
	//Create user if not exists
	//https://developers.google.com/identity/sign-in/web/backend-auth
	
	
	public String getUserAccessToken(Data config, String inType) throws Exception
	{
		String accesstoken = config.get("httprequesttoken"); // Expired in 14 days
		Data authinfo = getMediaArchive().getData("oauthprovider", "google");
		Object age = config.getValue("accesstokentime");
		Date ageoftoken = null;
		;
		if (age instanceof Date)
		{
			ageoftoken = (Date) age;
		}
		if (ageoftoken == null && age != null)
		{
			ageoftoken = DateStorageUtil.getStorageUtil().parseFromStorage((String) age);
		}
		boolean force = false;
		if (ageoftoken != null)
		{
			Date now = new Date();
			long seconds = (now.getTime() - ageoftoken.getTime()) / 1000;
			Long expiresin = Long.valueOf(config.get("expiresin"));
			if (seconds > (expiresin - 100))
			{
				force = true;
				log.info("Expiring token after expiry min");
			}
		}
		if (accesstoken == null || force)
		{
			String clientid = null;
			String clientsecret = null;
			
			String token = config.get("refreshtoken");
			
			if( inType.equals("hotfolder"))
			{
				clientid = config.get("accesskey");
				clientsecret = config.get("secretkey");
			}
			else
			{
				clientid = authinfo.get("clientid");
				clientsecret = authinfo.get("clientsecret");				
			}
			
			OAuthClientRequest request = OAuthClientRequest.tokenProvider(OAuthProviderType.GOOGLE).
					setGrantType(GrantType.REFRESH_TOKEN).setRefreshToken(token).
					setClientId(clientid).setClientSecret(clientsecret).
					setParameter("access_type", "offline").
					buildBodyMessage();
			OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
			// Facebook is not fully compatible with OAuth 2.0 draft 10, access token
			// response is
			// application/x-www-form-urlencded, not json encoded so we use dedicated
			// response class for that
			// Own response class is an easy way to deal with oauth providers that introduce
			// modifications to
			// OAuth specification
			EmTokenResponse oAuthResponse = oAuthClient.accessToken(request, EmTokenResponse.class);
			Long expiresin = oAuthResponse.getExpiresIn();
			// final OAuthAccessTokenResponse oAuthResponse =
			// oAuthClient.accessToken(request, "POST");
			// final OAuthAccessTokenResponse oAuthResponse =
			// oAuthClient.accessToken(request);
			accesstoken = oAuthResponse.getAccessToken();
			config.setValue("httprequesttoken", accesstoken);
			config.setValue("accesstokentime", new Date());
			//config.setValue("access_type", "offline");
			config.setValue("expiresin", oAuthResponse.getExpiresIn());

			getMediaArchive().getSearcher(inType).saveData(config);

		}
		return accesstoken;
	}


	

	public ArrayList syncContacts(User inAuthinfo)
	{
		try
		{
			return listContacts(inAuthinfo, null);

		}
		catch (Exception ex)
		{
			throw new OpenEditException(ex);
		}

	}

	public ArrayList listContacts(User inAuthinfo, String inSearchTerm) throws Exception
	{
		String url = "https://www.google.com/m8/feeds/contacts/default/full?v=3.0";
		if (inSearchTerm != null)
		{
			url = url + "&q=" + URLEncoder.encode(inSearchTerm);
		}
		ArrayList quicklist = new ArrayList();

		CloseableHttpClient httpclient;
		httpclient = HttpClients.createDefault();
		HttpRequestBase httpmethod = null;
		httpmethod = new HttpGet(url);
		String accesstoken = getUserAccessToken(inAuthinfo, "user");
		httpmethod.addHeader("authorization", "Bearer " + accesstoken);

		HttpResponse resp = httpclient.execute(httpmethod);

		if (resp.getStatusLine().getStatusCode() != 200)
		{
			log.info("Google Server error returned " + resp.getStatusLine().getStatusCode() + ":" + resp.getStatusLine().getReasonPhrase());
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
				// e.printStackTrace();
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

	public JSONObject processImage(HttpSharedConnection connection, Asset inAsset)
	{
		MediaArchive archive = getMediaArchive();

		String input = "/WEB-INF/data/" + archive.getCatalogId() + "/generated/" + inAsset.getSourcePath() + "/image3000x3000.jpg";

		Page inputpage = archive.getPageManager().getPage(input);
		if (!inputpage.exists())
		{
			input = "/WEB-INF/data/" + archive.getCatalogId() + "/generated/" + inAsset.getSourcePath() + "/image3000x3000.png";
			inputpage = archive.getPageManager().getPage(input);
		}
		if (!inputpage.exists())
		{
			input = "/WEB-INF/data/" + archive.getCatalogId() + "/generated/" + inAsset.getSourcePath() + "/image1500x1500.png";
			inputpage = archive.getPageManager().getPage(input);
		}
		if (!inputpage.exists())
		{
			input = "/WEB-INF/data/" + archive.getCatalogId() + "/generated/" + inAsset.getSourcePath() + "/image1500x1500.jpg";
			inputpage = archive.getPageManager().getPage(input);

		}
		if (!inputpage.exists())
		{
			input = "/WEB-INF/data/" + archive.getCatalogId() + "/generated/" + inAsset.getSourcePath() + "/image1024x768.jpg";
			inputpage = archive.getPageManager().getPage(input);

		}
		if (!inputpage.exists())
		{
			input = "/WEB-INF/data/" + archive.getCatalogId() + "/generated/" + inAsset.getSourcePath() + "/image1024x768.png";
			inputpage = archive.getPageManager().getPage(input);

		}
		if (!inputpage.exists())
		{
			log.info("Couldn't process asset as input didn't exist:" + input);
			
			return null;
		}
		
		return processImage(connection,  inputpage.getContentItem());
	}
	public JSONObject processImage(	HttpSharedConnection connection,ContentItem inItem)
	{
		//https://cloud.google.com/vision/docs/

		try
		{
			MediaArchive archive = getMediaArchive();

			String googleapikey = archive.getCatalogSettingValue("googleapikey");
			if(googleapikey == null || googleapikey.isEmpty()) {
				log.info("Must specify google api key");
				return null;
			}
			
			String url = "https://vision.googleapis.com/v1/images:annotate?key=" + googleapikey;

			File file = new File(inItem.getAbsolutePath());
			byte[] encoded = Base64.encodeBase64(FileUtils.readFileToByteArray(file));

			JSONObject request = new JSONObject();
			JSONArray requestlist = new JSONArray();
			
			JSONObject data = new JSONObject();
			requestlist.add(data);

			JSONObject image = new JSONObject();
			image.put("content", new String(new String(encoded, StandardCharsets.US_ASCII)));
			data.put("image", image);

			JSONArray features = new JSONArray();
			data.put("features", features);

			JSONObject type = new JSONObject();
			type.put("type", "LABEL_DETECTION");
			features.add(type);

			type = new JSONObject();
			type.put("type", "OBJECT_LOCALIZATION");
			features.add(type);

			//type = new JSONObject();
			//type.addProperty("type", "LANDMARK_DETECTION");
			//features.add(type);
			
			request.put("requests", requestlist);
						
			//System.setProperty("https.protocols", "TLSv1.2");
			
			//CloseableHttpClient httpclient = HttpClients.createSystem();
			CloseableHttpResponse resp = null;
	 		resp = getConnection().sharedPostWithJson(url,request);
			JSONObject json = getConnection().parseJson(resp);
			return json;
		}
		catch (SSLException e) {
        	throw new OpenEditException(e);
        } 
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			throw new OpenEditException(e);
		}

	}

	public JSONObject uploadToBucket(Data inAuthInfo, String bucket, ContentItem inItem, JSONObject inMetadata) throws Exception
	{
		//https://cloud.google.com/storage/docs/json_api/v1/how-tos/multipart-upload

		String filename = URLUtilities.encode((String)inMetadata.get("name"));
		String geturl = "https://www.googleapis.com/storage/v1/b/" + bucket + "/o/" + filename;

		File file = new File(inItem.getAbsolutePath());

		if (!file.exists())
		{
			throw new OpenEditException("Input file missing " + file.getPath());
		}
		
		String accesstoken = getAccessToken();
		
		Map headers = new HashMap(1);
		headers.put("authorization", "Bearer " + accesstoken);

		JSONObject json = getConnection().getJson(geturl,headers);
		if( json != null)
		{
			//chek the size of the file
			Object existingsize = json.get("size");
			if( existingsize == null)
			{
				
			}
			if (file.length() == Long.parseLong( existingsize.toString() ) )
			{
				return json;
			}
		}

		String url = "https://www.googleapis.com/upload/storage/v1/b/" + bucket + "/o?uploadType=multipart";
		//TODO: Use HttpRequestBuilder.addPart()
		HttpPost method = new HttpPost(url);
		method.addHeader("authorization", "Bearer " + accesstoken);

		HttpRequestBuilder builder = new HttpRequestBuilder();

		//POST https://www.googleapis.com/upload/storage/v1/b/myBucket/o?uploadType=multipart
		builder.addPart("metadata", inMetadata.toJSONString(), "application/json"); //What should this be called?

		builder.addPart("file", file);
		//long size = inMetadata.getBytes().length + file.getTotalSpace();

		//method.setHeader("Content-Length",String.valueOf(size));

		method.setEntity(builder.build());
		String contenttype = method.getEntity().getContentType().getValue();
		String boundary = contenttype.substring(contenttype.indexOf("boundary=") + 9, contenttype.length());
		method.setHeader("Content-Type", "multipart/related; boundary=" + boundary);

		CloseableHttpResponse resp = getConnection().sharedPost(method);
		JSONObject json2 = getConnection().parseJson(resp);
		return json2;

	}

	/**
	 * @deprecated
	 * @param bucket
	 * @return
	 * @throws Exception
	 */
	
	public JSONObject listFiles(String bucket) throws Exception
	{
		//https://cloud.google.com/storage/docs/json_api/v1/how-tos/multipart-upload	
		String url = "https://www.googleapis.com/storage/v1/b/" + bucket + "/o/";
		//TODO: Use HttpRequestBuilder.addPart()

		//POST https://www.googleapis.com/upload/storage/v1/b/myBucket/o?uploadType=multipart

		String accesstoken = getAccessToken(); //TODO: Cache this?
		
		Map headers = new HashMap(1);
		headers.put("authorization", "Bearer " + accesstoken);
		
		JSONObject json = getConnection().getJson(url, headers);
		return json;
		//This needs to loop over to get more than 1000 results
	}

	public void saveCloudFile(String inUrl, ContentItem inItem) throws Exception
	{

		// GET
		// https://www.googleapis.com/drive/v3/files/0B9jNhSvVjoIVM3dKcGRKRmVIOVU?alt=media
		// Authorization: Bearer <ACCESS_TOKEN>

		HttpRequestBase httpmethod = new HttpGet(inUrl);
		String accesstoken = getAccessToken();
		httpmethod.addHeader("authorization", "Bearer " + accesstoken);

		CloseableHttpResponse resp = getConnection().sharedExecute(httpmethod);

		if (resp.getStatusLine().getStatusCode() != 200)
		{
			log.info("Google Server error returned " + resp.getStatusLine().getStatusCode());
		}

		try
		{
			HttpEntity entity = resp.getEntity();
	
			File output = new File(inItem.getAbsolutePath());
			output.getParentFile().mkdirs();
			log.info("Google Manager Downloading " + inItem.getPath());
			filler.fill(entity.getContent(), new FileOutputStream(output), true);
		}
		finally
		{
			getConnection().release(resp);
		}
		// getMediaArchive().getAssetImporter().reImportAsset(getMediaArchive(),
		// inAsset);

		// if( assettype != null && assettype.equals("embedded") )
		// {
		// current.setValue("assettype","embedded");
		// }

	}

	public void uploadToDrive(String inAccessToken, Asset inAsset, File file)
	{
		//	POST https://www.googleapis.com/upload/drive/v3/files?uploadType=media

		String geturl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart";

		try
		{
			HttpMimeBuilder builder = new HttpMimeBuilder();

			HttpPost method = new HttpPost(geturl);
			method.addHeader("authorization", "Bearer " + inAccessToken);

			//HttpResponse getresp = httpclient.execute(method);
			JSONObject object = new JSONObject();
			object.put("name", inAsset.getName());
			String metadata = object.toString();
			//POST https://www.googleapis.com/upload/storage/v1/b/myBucket/o?uploadType=multipart
			builder.addPart("file", metadata, "application/json"); //What should this be called?
		//	builder.addPart("file", "", "imagetype/jpeg");
			Charset UTF8 = Charset.forName("UTF-8");

			builder.addPart("file", file, ContentType.create("image/jpeg",UTF8));

			method.setEntity(builder.build());
			String contenttype = method.getEntity().getContentType().getValue();
			String boundary = contenttype.substring(contenttype.indexOf("boundary=") + 9, contenttype.length());
			method.setHeader("Content-Type", "multipart/related; boundary=" + boundary);

			CloseableHttpResponse resp = getConnection().sharedExecute(method);
			JSONObject json = getConnection().parseJson(resp);
			
			//ok?
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}

	}

	//	public void publishToYoutube(Asset inAsset, ContentItem inContentItem)
	//	{
	//		CloseableHttpClient httpclient;
	//		httpclient = HttpClients.createDefault();
	//		HttpRequestBuilder builder = new HttpRequestBuilder();
	//
	//		String url = "https://www.googleapis.com/upload/youtube/v3/videos";
	//		//TODO: Use HttpRequestBuilder.addPart()
	//		HttpPost method = new HttpPost(url);
	//		method.addHeader("authorization", "Bearer " + getAccessToken(getMediaArchive().getData("oauthprovider", "google")));
	//
	//		File file = new File(inContentItem.getAbsolutePath());
	//
	//		builder.addPart("file", file);
	//		//long size = inMetadata.getBytes().length + file.getTotalSpace();
	//
	//		//method.setHeader("Content-Length",String.valueOf(size));
	//		
	//		
	//		method.setEntity(builder.build());
	//		String contenttype = method.getEntity().getContentType().getValue();
	//		String boundary = contenttype.substring(contenttype.indexOf("boundary=")+9, contenttype.length());
	//		method.setHeader("Content-Type","multipart/related; boundary=" + boundary);
	//
	//		HttpResponse resp = httpclient.execute(method);
	//
	//		if (resp.getStatusLine().getStatusCode() != 200) {
	//			log.info("Google Server error returned " + resp.getStatusLine().getStatusCode() + ":"
	//					+ resp.getStatusLine().getReasonPhrase());
	//			String returned = EntityUtils.toString(resp.getEntity());
	//			log.error(returned);
	//			return null;
	//		}
	//
	//		HttpEntity entity = resp.getEntity();
	//		JsonParser parser = new JsonParser();
	//		String content = IOUtils.toString(entity.getContent());
	//
	//		JsonElement elem = parser.parse(content);
	//		return elem.getAsJSONObject();
	//				
	//	}
	public ExecutorManager getExecutorManager()
	{
		ExecutorManager queue = (ExecutorManager) getModuleManager().getBean(getMediaArchive().getCatalogId(), "executorManager");
		return queue;
	}

	// For Push Notifications
	public void notifyTopic(final String inChannel,final  User inUser,final  String inSubject, final String inMessage, final Map inExtraData)
	{
		//TODO: Dont spam the channel. Send the first one. Then wait 20min for the rest
		getExecutorManager().execute( new Runnable() {
			
			@Override
			public void run() 
			{
				try
				{
					MediaArchive archive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
					final Data authinfo = archive.getData("oauthprovider", "google");
					String firebaseid = authinfo.get("remoteprojectid"); //entermediadb-177816
					if( firebaseid == null)
					{
						//throw new OpenEditException("remote project id missing");
						return;
					}
					String accesstoken = getAccessToken();
					FireBase base = new FireBase();
					base.notifyTopic(firebaseid,accesstoken, inChannel, inUser, inSubject, inMessage, inExtraData);
				}
				catch (Throwable ex)
				{
					log.error("Could not send push", ex);
				}
			}
		});
	}

	protected HttpSharedConnection getConnection() 
	{
		if( connection == null)
		{
			connection = new HttpSharedConnection();
		}
		return connection;
	}
	
	public Map<String,String> getTokenDetails(String token) {

		JSONObject resp = getConnection().getJson("https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=" + token);
		if( resp != null)
		{
			
			/**
			{
  "issued_to": "279466694094-jo658skoqembq8p6nd5fqsl2t2p15lj0.apps.googleusercontent.com",
  "audience": "279466694094-jo658skoqembq8p6nd5fqsl2t2p15lj0.apps.googleusercontent.com",
  "user_id": "101451826132682989401",
  "scope": "https://www.googleapis.com/auth/userinfo.email openid https://www.googleapis.com/auth/userinfo.profile",
  "expires_in": 1306,
  "email": "cburkey@openedit.org",
  "verified_email": true,
  "access_type": "online"
}
			 */
			return resp;
		}
			
		return null;
	}

	public User createUserIfNeeded(String email)
	{
		MediaArchive archive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		UserSearcher searcher = (UserSearcher) archive.getSearcher("user");

		User target = null;
		
		if( email != null) {
			target = searcher.getUserByEmail(email);
		}
		if( target != null)
		{
			return target;
		}
		Data authinfo = archive.getData("oauthprovider", "google");
		String allowed = authinfo.get("alloweddomains");
		if( allowed == null)
		{
			log.error("Not allowed to create user based on alloweddomains" );
			return null;
		}
		boolean ok = false;
		String domains = authinfo.get("alloweddomains");
		if( domains.equals("*"))
		{
			ok  = true;
		}
		else
		{
			String[] domainlist = domains.split(",");
			for (int i = 0; i < domainlist.length; i++)
			{
				String domain = domainlist[i];
				if (email.endsWith(domain))
				{
					ok = true;
				}
			}
		}
		if( !ok)
		{
			log.error("Domain not authorized " + email );
			return null;
		}
		
		if (target == null)
		{
			target = (User) searcher.createNewData();
			//target.setFirstName(firstname);
			//target.setLastName(lastname);
			target.setEmail(email);
			target.setEnabled(true);
			//target.setId(userid);
			searcher.saveData(target, null);
		}
		return target;
	}

	//https://firebase.google.com/docs/reference/rest/auth
	public void createFireBaseUser(User inUser) 
	{
		String apikey = getMediaArchive().getCatalogSettingValue("firebaseapikey");
		if( apikey == null)
		{
			throw new OpenEditException("Firebase apikey not set");
		}

//		String password = getMediaArchive().getUserManager().decryptPassword(inUser);

		String firebasepassword = inUser.get("firebasepassword");
		if( firebasepassword == null)
		{
			createFirebaseUser(apikey,inUser);
		}
		else
		{
			String token = logIntoFirebase(apikey,inUser,firebasepassword);
			if( token == null)
			{
				//maybe user is deleted?
				createFirebaseUser(apikey,inUser);
			}
//			else if( !password.equals(firebasepassword ) )
//			{
//				updatePasswordOn(apikey,inUser,token,password);
//		 		inUser.setValue("firebasepassword", password);
//		 		getMediaArchive().getUserManager().saveUser(inUser);
//			}
		}
	}

	protected String logIntoFirebase(String apikey, User inUser, String firebasepassword)
	{
		JSONObject createrequest = new JSONObject();		
		createrequest.put("email",inUser.getEmail());
		createrequest.put("password",firebasepassword);	
		createrequest.put("returnSecureToken",true)	;

		String createurl = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + apikey;
		CloseableHttpResponse createresp = getConnection().sharedPostWithJson(createurl,createrequest);
 		
 		if( createresp.getStatusLine().getStatusCode() != 200)
		{
 			getConnection().release(createresp);
 			log.error("Could not log into Firebase. Please delete user " + inUser.getEmail());
 			return null;
		}
 		JSONObject json  = getConnection().parseJson(createresp);
 		String idtoken = (String)json.get("idToken");
 		return idtoken;
	}

	protected void updatePasswordOn(String apikey, User inUser, String firebaseid, String password)
	{
		JSONObject createrequest = new JSONObject();		
		createrequest.put("idToken",firebaseid);
		createrequest.put("password",password);	
		createrequest.put("returnSecureToken",true);

		String createurl = "https://identitytoolkit.googleapis.com/v1/accounts:update?key=" + apikey;
		CloseableHttpResponse createresp = getConnection().sharedPostWithJson(createurl,createrequest);
 		
 		if( createresp.getStatusLine().getStatusCode() != 200)
		{
 			getConnection().release(createresp);
 			log.error("Could not update password in Firebase. Please delete user " + inUser.getEmail());
 			return;
		}
 		JSONObject json  = getConnection().parseJson(createresp);
 		String idtoken = (String)json.get("idToken");
	}
		
	protected void createFirebaseUser(String apikey, User inUser)
	{
		JSONObject createrequest = new JSONObject();		
		createrequest.put("email",inUser.getEmail());
		
		String firebasepassword = new PasswordGenerator().generate();
		createrequest.put("password",firebasepassword);	
		createrequest.put("returnSecureToken",true);
			
		String createurl = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + apikey;
		CloseableHttpResponse createresp = null;
 		createresp = getConnection().sharedPostWithJson(createurl,createrequest);
 		
 		if( createresp.getStatusLine().getStatusCode() != 200)
		{
			String returned = getConnection().parseText( createresp );

 			log.error("Could not create user in firebase. Please manually delete existing user " + inUser.getEmail() + " " + returned);
 			return;
		}
 		JSONObject json  = getConnection().parseJson(createresp);
 		
 		inUser.setProperty("firebasepassword", firebasepassword);
 		getMediaArchive().getUserManager().saveUser(inUser);
 		
// 		String idtoken = (String)json.get("idToken");
	}

//	public JSONObject validateLogin(String email, String password) 
//	{
//		JSONObject request = new JSONObject();		
//		request.put("email",email);
//		request.put("password",password);
//		
//		String url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=[API_KEY]";
//		CloseableHttpResponse resp = null;
//	 	try
//	 	{
//	 		resp = getConnection().sharedPostWithJson(url,request);
//			JSONObject json = getConnection().parseJson(resp);
//			return json;
//	 	}
//	 	catch( Throwable ex)
//	 	{
//	 		log.error("Network error " ,ex);
//	 		throw new OpenEditException(ex);
//	 
//	}
//	}

public GeoCoder getGeoCoder()
{
	GeoCoder coder = (GeoCoder) getModuleManager().getBean(getCatalogId(), "geoCoder");
	coder.setGoogleKey(getMediaArchive().getCatalogSettingValue("google-maps-key"));
	if (coder.getGoogleKey() == null)
	{
		log.error("No key set");
	}
	return coder;
}


	public Position searchForGeoLocation(String inAddress)
	{
		Position pos = getGeoCoder().findFirstPosition(inAddress);
		return pos;
	}	
/**
 * 
 
function getLatLngFromString(ll) {
    var latlng = ll.split(/, ?/)
    return new google.maps.LatLng(parseFloat(latlng[0]), parseFloat(latlng[1])); 
}

var geocoder;

initialize = function() {
var parentheight = $(".assetpanel-geomap").height();
if (typeof parentheight !== 'undefined') {
	var mapheight = (jQuery(".assetpanel-geomap").height()-110);
	jQuery('#geo_point_map').height(mapheight);
}
 var centerloc = getLatLngFromString('39.025818619038375,-95.03089171562499');
 var map = new google.maps.Map(jQuery('#geo_point_map')[0], {
    zoom: 2
,
    center:centerloc,
    mapTypeControl: true,
    mapTypeControlOptions: {
      style: google.maps.MapTypeControlStyle.DROPDOWN_MENU,
      mapTypeIds: ['roadmap', 'terrain']
    }
  });

  var marker = new google.maps.Marker({
		position: getLatLngFromString('$lat,$lng'),
		draggable: true, 
		bounds: true,
        map: map,
        title: '{"en":"Screenshot from 2023-06-22 15-53-59.png"}'
     });
     
     
   google.maps.event.addListener(marker, 'dragend', function(event)
	    {
	    	console.log(marker.getPosition());
            jQuery('#geo_point_latandlong').val(marker.getPosition().lat()+ "," + marker.getPosition().lng());
	});


	jQuery('#geo_point_search').click(dosearch);
	
	jQuery('#geo_point_address').keypress(function(e) {
		if (e.keyCode == 13) {
			dosearch()
		}
	});
	
	geocoder = new google.maps.Geocoder();
			
		
}

dosearch = function () {
	var address = jQuery('#geo_point_address').val();
	var postalCode = jQuery('#geo_point_postalCode').val();
	if (address!=null) {
		$('#geo_point_map').gmap('search', { 'address': address }, function(results, status) {
		    if ( status === 'OK' ) {
		    	$('#geo_point_map').gmap('clear', 'markers');
		    	$('#geo_point_map').gmap('addMarker', 
		          {
		    		'position': results[0].geometry.location, 
		    		'draggable': true, 
		    		'bounds': true
		    	  }).dragend(function(event) {
		              jQuery('#geo_point_latandlong').val(event.latLng.lat() + "," + event.latLng.lng());
	    	  		}
		    	  );
	
		    	$('#geo_point_map').gmap('get', 'map').panTo(results[0].geometry.location);
		        $('#geo_point_map').gmap('option', 'zoom', 12);
	            jQuery('#geo_point_latandlong').val(results[0].geometry.location.lat() + "," + results[0].geometry.location.lng());
	
		    }
		});
	}
}

initialize(); //Onload?
$(window).resize(function() {
	if (typeof map !== 'undefined') {
  		google.maps.event.trigger(map, "resize");
	}
});

**/
	}
	
