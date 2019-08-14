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
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
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
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.data.BaseData;
import org.openedit.entermedia.util.EmTokenResponse;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.HttpMimeBuilder;
import org.openedit.util.HttpRequestBuilder;
import org.openedit.util.HttpSharedConnection;
import org.openedit.util.OutputFiller;
import org.openedit.util.URLUtilities;
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
	protected Date fieldTokenTime;

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

	public Results listDriveFiles(String inAccessToken, String inParentId) throws Exception
	{
		// https://developers.google.com/drive/v3/reference/files/list
		// https://developers.google.com/drive/v3/web/search-parameters
		String url = "https://www.googleapis.com/drive/v3/files?orderBy=" + URLEncoder.encode("modifiedTime desc,name") + "&pageSize=1000&fields=*";

		String search = "'" + inParentId + "' in parents";
		url = url + "&q=" + URLEncoder.encode(search);

		// TODO: Add date query from the last time we imported

		Results results = new Results();

		// List one folders worth of files
		boolean keepgoing = false;
		do
		{
			keepgoing = populateMoreResults(inAccessToken, url, results);
		}
		while (keepgoing);

		return results;
	}

	protected boolean populateMoreResults(String inAccessToken, String fileurl, Results results) throws Exception
	{
		if (results.getResultToken() != null)
		{
			fileurl = fileurl + "&pageToken=" + URLEncoder.encode(results.getResultToken(), "UTF-8");
		}
		JsonObject json = get(fileurl, inAccessToken);
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

	private JsonObject get(String inFileurl, String inAccessToken) throws Exception
	{

		CloseableHttpClient httpclient;
		httpclient = HttpClients.createDefault();
		HttpRequestBase httpmethod = null;
		httpmethod = new HttpGet(inFileurl);
		httpmethod.addHeader("authorization", "Bearer " + inAccessToken);

		HttpResponse resp = httpclient.execute(httpmethod);

		if (resp.getStatusLine().getStatusCode() != 200)
		{
			log.info("Google Server error returned " + resp.getStatusLine().getStatusCode());
		}

		HttpEntity entity = resp.getEntity();
		String content = IOUtils.toString(entity.getContent());
		JsonParser parser = new JsonParser();
		JsonElement elem = parser.parse(content);
		// log.info(content);
		JsonObject json = elem.getAsJsonObject();
		if (json.get("error") != null) // Invalid Credentials
		{
			log.error("Could not connect API" + content);

		}
		return json;

	}

	public File saveFile(String inAccessToken, Asset inAsset) throws Exception
	{
		CloseableHttpClient httpclient = HttpClients.createDefault();

		// GET
		// https://www.googleapis.com/drive/v3/files/0B9jNhSvVjoIVM3dKcGRKRmVIOVU?alt=media
		// Authorization: Bearer <ACCESS_TOKEN>

		String url = "https://www.googleapis.com/drive/v3/files/" + inAsset.get("googleid") + "?alt=media";
		HttpRequestBase httpmethod = new HttpGet(url);
		httpmethod.addHeader("authorization", "Bearer " + inAccessToken);

		HttpResponse resp = httpclient.execute(httpmethod);

		if (resp.getStatusLine().getStatusCode() != 200)
		{
			log.info("Google Server error returned " + resp.getStatusLine().getStatusCode());
			log.info("Google Server error returned " + resp.getStatusLine());

		}

		HttpEntity entity = resp.getEntity();

		ContentItem item = getMediaArchive().getOriginalContent(inAsset);

		File output = new File(item.getAbsolutePath());
		output.getParentFile().mkdirs();
		log.info("Google Manager Downloading " + item.getPath());
		filler.fill(entity.getContent(), new FileOutputStream(output), true);

		// getMediaArchive().getAssetImporter().reImportAsset(getMediaArchive(),
		// inAsset);
		// ContentItem itemFile = getMediaArchive().getOriginalContent(inAsset);
		getMediaArchive().getAssetImporter().getAssetUtilities().getMetaDataReader().updateAsset(getMediaArchive(), item, inAsset);
		inAsset.setProperty("previewstatus", "converting");
		getMediaArchive().saveAsset(inAsset);
		getMediaArchive().fireMediaEvent("assetimported", null, inAsset); // Run custom scripts?
		return output;
		// if( assettype != null && assettype.equals("embedded") )
		// {
		// current.setValue("assettype","embedded");
		// }

	}

	public String getAccessToken(Data authinfo) throws OpenEditException
	{
		try
		{
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
				OAuthClientRequest request = OAuthClientRequest.tokenProvider(OAuthProviderType.GOOGLE).setGrantType(GrantType.REFRESH_TOKEN).setRefreshToken(authinfo.get("refreshtoken")).setClientId(authinfo.get("clientid")).setClientSecret(authinfo.get("clientsecret")).buildBodyMessage();
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
		catch (OAuthSystemException e)
		{
			throw new OpenEditException(e);
		}
		catch (OAuthProblemException e)
		{
			throw new OpenEditException(e);
		}
	}

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

			OAuthClientRequest request = OAuthClientRequest.tokenProvider(OAuthProviderType.GOOGLE).setGrantType(GrantType.REFRESH_TOKEN).setRefreshToken(config.get("refreshtoken")).setClientId(authinfo.get("clientid")).setClientSecret(authinfo.get("clientsecret")).buildBodyMessage();
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
			config.setValue("expiresin", oAuthResponse.getExpiresIn());

			getMediaArchive().getSearcher(inType).saveData(config);

		}
		return accesstoken;
	}

	public Results syncAssets(String inAccessToken, String inRoot, boolean savenow)
	{
		try
		{
			Results results = listDriveFiles(inAccessToken, "root");
			processResults(inAccessToken, inRoot, results, savenow);
			getMediaArchive().fireSharedMediaEvent("conversions/runconversions"); // this will save the asset as// imported
			return results;
		}
		catch (Exception ex)
		{
			throw new OpenEditException(ex);
		}

	}

	protected void processResults(String inAccessToken, String inCategoryPath, Results inResults, boolean savenow) throws Exception
	{
		if (createAssets(inAccessToken, inCategoryPath, inResults.getFiles(), savenow))
		{
			if (inResults.getFolders() != null)
			{
				for (Iterator iterator = inResults.getFolders().iterator(); iterator.hasNext();)
				{
					JsonObject folder = (JsonObject) iterator.next();
					String id = folder.get("id").getAsString();
					String foldername = folder.get("name").getAsString();
					foldername = foldername.trim();
					Results folderresults = listDriveFiles(inAccessToken, id);
					String categorypath = inCategoryPath + "/" + foldername;
					processResults(inAccessToken, categorypath, folderresults, savenow);
				}
			}
		}

	}

	protected boolean createAssets(String inAccessToken, String categoryPath, Collection inFiles, boolean savenow) throws Exception
	{
		if (inFiles == null)
		{
			return true;
		}
		Category category = getMediaArchive().createCategoryPath(categoryPath);

		ContentItem item = getMediaArchive().getContent("/WEB-INF/" + getMediaArchive() + "/originals/" + categoryPath);
		File realfile = new File(item.getAbsolutePath());
		realfile.mkdirs();
		long leftkb = realfile.getFreeSpace() / 1000;
		// FileSystemUtils.freeSpaceKb(item.getAbsolutePath());
		String free = getMediaArchive().getCatalogSettingValue("min_free_space");
		if (free == null)
		{
			free = "3000000";
		}

		Map onepage = new HashMap();
		for (Iterator iterator = inFiles.iterator(); iterator.hasNext();)
		{
			JsonObject object = (JsonObject) iterator.next();
			String id = object.get("id").getAsString();
			onepage.put(id, object);
			JsonElement fs = object.get("size");
			if (fs != null)
			{
				String size = fs.getAsString();

				if (size != null)
				{
					leftkb = leftkb - (Long.parseLong(size) / 1000);
					if (leftkb < Long.parseLong(free))
					{
						log.info("Not enough disk space left to download more " + leftkb + "<" + free);
						return false;
					}
				}
			}

			if (onepage.size() == 100)
			{
				createAssetsIfNeeded(inAccessToken, onepage, category, savenow);
				onepage.clear();
			}
		}
		createAssetsIfNeeded(inAccessToken, onepage, category, savenow);
		return true;
	}

	private void createAssetsIfNeeded(String inAccessToken, Map inOnepage, Category category, boolean savenow) throws Exception
	{
		if (inOnepage.isEmpty())
		{
			log.info("empty map");
			return;
		}
		Collection tosave = new ArrayList();

		HitTracker existingassets = getMediaArchive().getAssetSearcher().query().orgroup("googleid", inOnepage.keySet()).search();
		log.info("checking " + existingassets.size() + " assets ");
		// Update category
		for (Iterator iterator = existingassets.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			Asset existing = (Asset) getMediaArchive().getAssetSearcher().loadData(data);
			// Remove existing assets
			inOnepage.remove(existing.get("googleid"));
			// existing.clearCategories();
			if (!existing.isInCategory(category))
			{
				// Clear old Drive categorties
				Category root = getMediaArchive().createCategoryPath("Drive");
				Collection existingcategories = new ArrayList(existing.getCategories());
				for (Iterator iterator2 = existingcategories.iterator(); iterator2.hasNext();)
				{
					Category drive = (Category) iterator2.next();
					if (root.isAncestorOf(drive))
					{
						existing.removeCategory(drive);
					}
				}
				existing.addCategory(category);
				getMediaArchive().saveAsset(existing);
				log.info("Asset moved categories " + existing);
			}
		}

		// new Assets
		for (Iterator iterator = inOnepage.keySet().iterator(); iterator.hasNext();)
		{
			String id = (String) iterator.next();
			JsonObject object = (JsonObject) inOnepage.get(id);

			// log.info(object.get("kind"));// "kind": "drive#file",
			// String md5 = object.get("md5Checksum").getAsString();
			Asset newasset = (Asset) getMediaArchive().getAssetSearcher().createNewData();
			String filename = object.get("name").getAsString();
			filename = filename.trim();
			// JsonElement webcontentelem = object.get("webContentLink");

			newasset.setSourcePath(category.getCategoryPath() + "/" + filename);
			newasset.setFolder(false);
			newasset.setValue("googleid", id);
			newasset.setValue("assetaddeddate", new Date());
			newasset.setValue("retentionpolicy", "deleteoriginal"); // Default
			newasset.setValue("importstatus", "needsmetadata");
			JsonElement jsonElement = object.get("webContentLink");
			if (jsonElement != null)
			{
				newasset.setValue("googledownloadurl", jsonElement.getAsString());

			}

			// TODO: Add dates here

			newasset.setName(filename);
			jsonElement = object.get("webViewLink");
			if (jsonElement != null)
			{
				newasset.setValue("linkurl", jsonElement.getAsString());

			}
			// JsonElement thumbnailLink = object.get("thumbnailLink");
			// if (thumbnailLink != null)
			// {
			// newasset.setValue("fetchthumbnailurl", thumbnailLink.getAsString());
			// }

			// newasset.setValue("md5hex", md5);
			newasset.addCategory(category);

			// inArchive.getAssetSearcher().saveData(newasset);
			tosave.add(newasset);
		}
		if (!tosave.isEmpty())
		{

			getMediaArchive().saveAssets(tosave);

			log.info("Saving new assets " + tosave.size());
			if (savenow)
			{
				for (Iterator iterator = tosave.iterator(); iterator.hasNext();)
				{
					Asset asset = (Asset) iterator.next();
					saveFile(inAccessToken, asset);
				}
			}
		}
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

	public JsonObject processImage(HttpSharedConnection connection, Asset inAsset)
	{
		MediaArchive archive = getMediaArchive();

		String input = "/WEB-INF/data/" + archive.getCatalogId() + "/generated/" + inAsset.getSourcePath() + "/image1024x768.jpg";

		Page inputpage = archive.getPageManager().getPage(input);
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
	public JsonObject processImage(	HttpSharedConnection connection,ContentItem inItem)
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

			JsonObject request = new JsonObject();
			JsonArray requestlist = new JsonArray();
			
			JsonObject data = new JsonObject();
			requestlist.add(data);

			JsonObject image = new JsonObject();
			image.addProperty("content", new String(new String(encoded, StandardCharsets.US_ASCII)));
			data.add("image", image);

			JsonArray features = new JsonArray();
			data.add("features", features);

			JsonObject type = new JsonObject();
			type.addProperty("type", "LABEL_DETECTION");
			features.add(type);

			type = new JsonObject();
			type.addProperty("type", "OBJECT_LOCALIZATION");
			features.add(type);

			//type = new JsonObject();
			//type.addProperty("type", "LANDMARK_DETECTION");
			//features.add(type);
			
			request.add("requests", requestlist);

			HttpPost post = new HttpPost(url);
			
			post.setEntity(new StringEntity(request.toString(), "UTF-8"));
			post.addHeader("Content-Type", "application/json");
			post.setProtocolVersion(HttpVersion.HTTP_1_1);
			
			//System.setProperty("https.protocols", "TLSv1.2");
			
			//CloseableHttpClient httpclient = HttpClients.createSystem();
			CloseableHttpResponse resp = null;
   		 	try
   		 	{
   		 		resp = connection.sharedPost(post);
			
				if (resp.getStatusLine().getStatusCode() != 200)
				{
					log.info("Google Server error returned " + resp.getStatusLine().getStatusCode() + ":" + resp.getStatusLine().getReasonPhrase());
					String returned = EntityUtils.toString(resp.getEntity());
					log.info(returned);
					return null;
				}
	
				HttpEntity entity = resp.getEntity();
				String content = IOUtils.toString(entity.getContent());
				JsonParser parser = new JsonParser();
				JsonElement elem = parser.parse(content);
				// log.info(content);
				JsonObject json = elem.getAsJsonObject();
				return json;
	        }
   		 	finally 
   		 	{
	        	connection.release(resp);
	        }
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

	public JsonObject uploadToBucket(Data inAuthInfo, String bucket, ContentItem inItem, JsonObject inMetadata) throws Exception
	{
		//https://cloud.google.com/storage/docs/json_api/v1/how-tos/multipart-upload

		String filename = URLUtilities.encode(inMetadata.get("name").getAsString());
		String geturl = "https://www.googleapis.com/storage/v1/b/" + bucket + "/o/" + filename;

		File file = new File(inItem.getAbsolutePath());

		if (!file.exists())
		{
			throw new OpenEditException("Input file missing " + file.getPath());
		}

		HttpRequestBuilder builder = new HttpRequestBuilder();

		HttpGet getmethod = new HttpGet(geturl);
		getmethod.addHeader("authorization", "Bearer " + getAccessToken(inAuthInfo));

		CloseableHttpClient httpclient;
		httpclient = HttpClients.createDefault();

		HttpResponse getresp = httpclient.execute(getmethod);
		int code = getresp.getStatusLine().getStatusCode();
		if (code == 404)
		{
			//do nothing
		}
		else if (code != 200)
		{
			log.error("Google Server error returned " + getresp.getStatusLine().getStatusCode());

		}
		else
		{
			//chek the size of the file
			HttpEntity entity = getresp.getEntity();
			JsonParser parser = new JsonParser();
			String content = IOUtils.toString(entity.getContent(), entity.getContentEncoding().getValue());
			JsonElement elem = parser.parse(content);
			JsonObject json = elem.getAsJsonObject();
			long existingsize = json.get("size").getAsLong();
			if (file.length() == existingsize)
			{
				return json;
			}
		}

		String url = "https://www.googleapis.com/upload/storage/v1/b/" + bucket + "/o?uploadType=multipart";
		//TODO: Use HttpRequestBuilder.addPart()
		HttpPost method = new HttpPost(url);
		method.addHeader("authorization", "Bearer " + getAccessToken(inAuthInfo));

		//POST https://www.googleapis.com/upload/storage/v1/b/myBucket/o?uploadType=multipart
		builder.addPart("metadata", inMetadata.toString(), "application/json"); //What should this be called?

		builder.addPart("file", file);
		//long size = inMetadata.getBytes().length + file.getTotalSpace();

		//method.setHeader("Content-Length",String.valueOf(size));

		method.setEntity(builder.build());
		String contenttype = method.getEntity().getContentType().getValue();
		String boundary = contenttype.substring(contenttype.indexOf("boundary=") + 9, contenttype.length());
		method.setHeader("Content-Type", "multipart/related; boundary=" + boundary);

		HttpResponse resp = httpclient.execute(method);

		if (resp.getStatusLine().getStatusCode() != 200)
		{
			log.info("Google Server error returned " + resp.getStatusLine().getStatusCode() + ":" + resp.getStatusLine().getReasonPhrase());
			String returned = EntityUtils.toString(resp.getEntity());
			log.error(returned);
			return null;
		}

		HttpEntity entity = resp.getEntity();
		JsonParser parser = new JsonParser();
		String content = IOUtils.toString(entity.getContent());

		JsonElement elem = parser.parse(content);
		return elem.getAsJsonObject();

	}

	public JsonObject listFiles(String bucket) throws Exception
	{
		//https://cloud.google.com/storage/docs/json_api/v1/how-tos/multipart-upload	
		HttpRequestBuilder builder = new HttpRequestBuilder();
		String url = "https://www.googleapis.com/storage/v1/b/" + bucket + "/o/";
		//TODO: Use HttpRequestBuilder.addPart()
		HttpGet method = new HttpGet(url);
		method.addHeader("authorization", "Bearer " + getAccessToken(getMediaArchive().getData("oauthprovider", "google")));

		//POST https://www.googleapis.com/upload/storage/v1/b/myBucket/o?uploadType=multipart

		CloseableHttpClient httpclient;
		httpclient = HttpClients.createDefault();

		HttpResponse resp = httpclient.execute(method);

		if (resp.getStatusLine().getStatusCode() != 200)
		{
			log.info("Google Server error returned " + resp.getStatusLine().getStatusCode() + ":" + resp.getStatusLine().getReasonPhrase());
			String returned = EntityUtils.toString(resp.getEntity());
			log.info(returned);

		}

		HttpEntity entity = resp.getEntity();
		JsonParser parser = new JsonParser();
		String content = IOUtils.toString(entity.getContent());

		JsonElement elem = parser.parse(content);
		return elem.getAsJsonObject();
		//This needs to loop over to get more than 1000 results
	}

	public void saveCloudFile(Data authinfo, String inUrl, ContentItem inItem) throws Exception
	{
		CloseableHttpClient httpclient = HttpClients.createDefault();

		// GET
		// https://www.googleapis.com/drive/v3/files/0B9jNhSvVjoIVM3dKcGRKRmVIOVU?alt=media
		// Authorization: Bearer <ACCESS_TOKEN>

		HttpRequestBase httpmethod = new HttpGet(inUrl);
		String accesstoken = getAccessToken(authinfo);
		httpmethod.addHeader("authorization", "Bearer " + accesstoken);

		HttpResponse resp = httpclient.execute(httpmethod);

		if (resp.getStatusLine().getStatusCode() != 200)
		{
			log.info("Google Server error returned " + resp.getStatusLine().getStatusCode());
		}

		HttpEntity entity = resp.getEntity();

		File output = new File(inItem.getAbsolutePath());
		output.getParentFile().mkdirs();
		log.info("Google Manager Downloading " + inItem.getPath());
		filler.fill(entity.getContent(), new FileOutputStream(output), true);

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

			CloseableHttpClient httpclient;
			httpclient = HttpClients.createDefault();

			//HttpResponse getresp = httpclient.execute(method);
			JsonObject object = new JsonObject();
			object.addProperty("name", inAsset.getName());
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

			HttpResponse resp = httpclient.execute(method);

			if (resp.getStatusLine().getStatusCode() != 200)
			{
				log.info("Google Server error returned " + resp.getStatusLine().getStatusCode() + ":" + resp.getStatusLine().getReasonPhrase());
				String returned = EntityUtils.toString(resp.getEntity());
				log.error(returned);
				return;
			}

			HttpEntity entity = resp.getEntity();
			JsonParser parser = new JsonParser();
			String content = IOUtils.toString(entity.getContent());

			JsonElement elem = parser.parse(content);
			//return elem.getAsJsonObject();
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
	//		return elem.getAsJsonObject();
	//				
	//	}

}
