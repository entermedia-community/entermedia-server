package org.entermediadb.dropbox;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.net.HttpSharedConnection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.entermedia.util.EmTokenResponse;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.OutputFiller;
import org.openedit.util.PathUtilities;

public class DropboxManager implements CatalogEnabled {

    private static final Log log = LogFactory.getLog(DropboxManager.class);

    protected String fieldCatalogId;
    protected MediaArchive fieldMediaArchive;
    protected ModuleManager fieldModuleManager;
    protected OutputFiller filler = new OutputFiller();
    protected HttpSharedConnection connection;
    protected DropboxAssetSource fieldAssetSource;

    public DropboxAssetSource getAssetSource() {
	return fieldAssetSource;
    }

    public void setAssetSource(DropboxAssetSource inAssetSource) {
	fieldAssetSource = inAssetSource;
    }

    public String getCatalogId() {
	return fieldCatalogId;
    }

    @Override
    public void setCatalogId(String inCatalogId) {
	fieldCatalogId = inCatalogId;
    }

    protected MediaArchive getMediaArchive() {
	if (fieldMediaArchive == null) {
	    fieldMediaArchive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
	}
	return fieldMediaArchive;
    }

    public ModuleManager getModuleManager() {
	return fieldModuleManager;
    }

    public void setModuleManager(ModuleManager inModuleManager) {
	fieldModuleManager = inModuleManager;
    }

    public OutputFiller getFiller() {
	return filler;
    }

    public void setFiller(OutputFiller inFiller) {
	filler = inFiller;
    }

    public HttpSharedConnection getConnection() {
	if (connection == null) {
	    connection = new HttpSharedConnection();
	}
	return connection;
    }

    public String getNamespace() {
	return getAssetSource().getConfig().get("dropboxnamespace");

    }

    public int syncAssets(String inRemoteRoot) {
	String folderroot = getAssetSource().getConfig().get("subfolder");
	ArrayList assets = new ArrayList();
	int total = 0;
	try {
	    Collection<JSONObject> entries = listEntries(inRemoteRoot);

	    MediaArchive archive = getMediaArchive();
	    for (JSONObject entry : entries) {
		String tag = (String) entry.get(".tag");

		if ("file".equals(tag)) {
		    // Extract necessary information
		    String path = (String) entry.get("path_display");
		    String name = (String) entry.get("name");
		    String id = (String) entry.get("id");
		    String categorypath = folderroot + path;
		    categorypath = PathUtilities.extractDirectoryPath(categorypath);
		    String sourcepath = categorypath + "/" + name;
		    Category cat = getMediaArchive().createCategoryPath(categorypath);
		    Asset existingAsset = archive.getAssetBySourcePath(sourcepath);

		    if (existingAsset == null) {
			String downloadpath = "/WEB-INF/data/" + getMediaArchive() + "/originals/" + categorypath + "/"
				+ name;
			;
			Page download = downloadFile(id, downloadpath);
			if (download != null) {
			    Asset newAsset = getMediaArchive().getAssetImporter().createAssetFromPage(archive, false,null, download,null);
			    newAsset.setValue("dropboxid", id);
			    newAsset.addCategory(cat);
			    assets.add(newAsset);
			    //Downloading is WAY slower than saving, just save so they appear as we go.
			    getMediaArchive().saveAsset(newAsset);
				archive.fireSharedMediaEvent("importing/assetscreated");

			}
		    }

		    // Increment the total count
		    total++;
		}
	    }
	    getMediaArchive().saveAssets(assets);
	    for (JSONObject entry : entries) {
		String tag = (String) entry.get(".tag");

		if ("folder".equals(tag)) {
		    String subfolderPath = (String) entry.get("path_display");
		    total += syncAssets(subfolderPath); // Recursive call
		}
	    }

	} catch (Exception e) {
	    throw new OpenEditException(e);
	}

	return total;
    }

    protected Collection<JSONObject> listEntries(String path) throws Exception {
	Collection<JSONObject> entries = new ArrayList<>();
	String url = "https://api.dropboxapi.com/2/files/list_folder";
	String urlContinue = "https://api.dropboxapi.com/2/files/list_folder/continue";

	JSONObject requestPayload = new JSONObject();
	requestPayload.put("path", path);

	String namespaceId = getNamespace();
	String accountid = getAccountID();

	HttpPost method = new HttpPost(url);
	method.addHeader("Authorization", "Bearer " + getAccessToken());
	method.addHeader("Dropbox-Api-Path-Root",
		"{\"namespace_id\": \"" + namespaceId + "\", \".tag\": \"namespace_id\"}");
	method.addHeader("Dropbox-API-Select-Admin", accountid);
	method.setHeader("Content-Type", "application/json");

	String payload = requestPayload.toJSONString();
	method.setEntity(new StringEntity(payload, "UTF-8"));

	CloseableHttpResponse resp = getConnection().sharedExecute(method);
	JSONObject json = getConnection().parseJson(resp);

	if (json != null) {
	    boolean hasMore = false;
	    JSONArray filesAndFolders;

	    do {
		filesAndFolders = (JSONArray) json.get("entries");
		if (filesAndFolders != null) {
		    for (Object obj : filesAndFolders) {
			JSONObject entry = (JSONObject) obj;
			entries.add(entry);
		    }
		}

		hasMore = json.containsKey("has_more") && (boolean) json.get("has_more");
		if (hasMore) {
		    String cursor = (String) json.get("cursor");

		    HttpPost continueMethod = new HttpPost(urlContinue);
		    continueMethod.addHeader("Authorization", "Bearer " + getAccessToken());
		    continueMethod.addHeader("Dropbox-Api-Path-Root",
			    "{\"namespace_id\": \"" + namespaceId + "\", \".tag\": \"namespace_id\"}");
		    continueMethod.addHeader("Dropbox-API-Select-Admin", accountid);
		    continueMethod.setHeader("Content-Type", "application/json");

		    JSONObject continuePayload = new JSONObject();
		    continuePayload.put("cursor", cursor);
		    continueMethod.setEntity(new StringEntity(continuePayload.toJSONString(), "UTF-8"));

		    resp = getConnection().sharedExecute(continueMethod);
		    json = getConnection().parseJson(resp);
		}
	    } while (hasMore);
	}

	return entries;
    }

    private String getAccountID() {
	return getAssetSource().getConfig().get("dropboxuser");
    }

    public String getAccessToken() throws OpenEditException {
	try {
	    Data authinfo = getMediaArchive().getData("oauthprovider", "dropbox");
	    String accesstoken = authinfo.get("accesstoken"); // Current access token
	    Object accesstokendate = authinfo.getValue("accesstokentime");
	    boolean forceRefresh = false;

	    // Check token expiration
	    if (accesstokendate instanceof Date) {
			Date tokenIssuedDate = (Date) accesstokendate;
	
			if (tokenIssuedDate != null) 
			{
			    Date now = new Date();
			    long tokenAgeInSeconds = (now.getTime() - tokenIssuedDate.getTime()) / 1000;
	
			    Object expiresIn = authinfo.getValue("expiresin");
			    if (expiresIn == null || (tokenAgeInSeconds > (Long.parseLong(expiresIn.toString()) - 100))) 
			    {
			    	forceRefresh = true;
			    	log.info("Token is expiring, refreshing...");
			    }
			}
	    } 
	    else 
	    {
			// No token issue date, force refresh
			forceRefresh = true;
	    }

	    // Refresh token if needed
	    if (accesstoken == null || forceRefresh) {
		OAuthClientRequest request = OAuthClientRequest.tokenLocation("https://api.dropbox.com/oauth2/token")
			.setGrantType(GrantType.REFRESH_TOKEN).setRefreshToken(authinfo.get("refreshtoken"))
			.setClientId(authinfo.get("clientid")).setClientSecret(authinfo.get("clientsecret"))
			.buildBodyMessage();

		OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
		EmTokenResponse oAuthResponse = oAuthClient.accessToken(request, EmTokenResponse.class);

		// Update the access token and related metadata
		accesstoken = oAuthResponse.getAccessToken();
		authinfo.setValue("accesstoken", accesstoken);
		authinfo.setValue("accesstokentime", new Date());
		Long expiresIn = oAuthResponse.getExpiresIn();
		authinfo.setValue("expiresin", expiresIn);

		// Save updated authinfo
		getMediaArchive().getSearcher("oauthprovider").saveData(authinfo);
	    }

	    return accesstoken;

	} catch (OAuthSystemException | OAuthProblemException e) {
	    throw new OpenEditException("Failed to retrieve or refresh access token", e);
	}
    }

  

    public Collection<JSONObject> listTeamMembers() throws Exception {
	try {
	    Collection<JSONObject> teamMembers = new ArrayList<>();
	    String url = "https://api.dropboxapi.com/2/team/members/list";

	    HttpPost method = new HttpPost(url);
	    method.addHeader("Authorization", "Bearer " + getAccessToken());
	    method.setHeader("Content-Type", "application/json");

	    // Add payload for pagination (if needed)
	    JSONObject requestPayload = new JSONObject();
	    requestPayload.put("limit", 100); // Fetch up to 100 members in one call
	    String payload = requestPayload.toJSONString();
	    method.setEntity(new StringEntity(payload, "UTF-8"));

	    CloseableHttpResponse resp = getConnection().sharedExecute(method);
	    JSONObject json = getConnection().parseJson(resp);

	    if (json != null) {
		JSONArray members = (JSONArray) json.get("members");
		for (Object memberObj : members) {
		    JSONObject member = (JSONObject) memberObj;
		    teamMembers.add(member);
		}

		// Handle pagination
		while (json.containsKey("has_more") && (boolean) json.get("has_more")) {
		    String cursor = (String) json.get("cursor");
		    requestPayload = new JSONObject();
		    requestPayload.put("cursor", cursor);
		    payload = requestPayload.toJSONString();
		    method.setEntity(new StringEntity(payload, "UTF-8"));

		    resp = getConnection().sharedExecute(method);
		    json = getConnection().parseJson(resp);

		    members = (JSONArray) json.get("members");
		    for (Object memberObj : members) {
			JSONObject member = (JSONObject) memberObj;
			teamMembers.add(member);
		    }
		}
	    }
	    log.info(teamMembers);
	    return teamMembers;
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return null;
    }

    public Collection<JSONObject> listNamespaces() throws Exception {
	try {
	    Collection<JSONObject> namespaces = new ArrayList<>();
	    String url = "https://api.dropboxapi.com/2/team/namespaces/list";

	    HttpPost method = new HttpPost(url);
	    method.addHeader("Authorization", "Bearer " + getAccessToken());
	    method.setHeader("Content-Type", "application/json");

	    // No payload needed for the initial request
	    JSONObject requestPayload = new JSONObject();
	    String payload = requestPayload.toJSONString();
	    method.setEntity(new StringEntity(payload, "UTF-8"));

	    CloseableHttpResponse resp = getConnection().sharedExecute(method);
	    JSONObject json = getConnection().parseJson(resp);

	    if (json != null) {
			JSONArray jsonNamespaces = (JSONArray) json.get("namespaces");
			for (Iterator iterator = jsonNamespaces.iterator(); iterator.hasNext();) {
			    JSONObject namespace = (JSONObject) iterator.next();
			    namespaces.add(namespace);
			}
	    }
	    log.info(namespaces);
	    return namespaces;
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return null;
    }

    public Page downloadFile(String fileId, String outputPath) throws Exception {
	String url = "https://content.dropboxapi.com/2/files/download";

	JSONObject apiArg = new JSONObject();
	apiArg.put("path", fileId);

	String namespaceId = getNamespace();
	HttpPost method = new HttpPost(url);
	method.addHeader("Authorization", "Bearer " + getAccessToken());
	method.addHeader("Dropbox-API-Path-Root",
		"{\"namespace_id\": \"" + namespaceId + "\", \".tag\": \"namespace_id\"}");
	method.addHeader("Dropbox-API-Arg", apiArg.toJSONString());
	method.addHeader("Dropbox-API-Select-Admin", getAccountID());

	CloseableHttpResponse resp = getConnection().sharedExecute(method);
	if (resp.getStatusLine().getStatusCode() == 200) {
	    // Ensure directories exist
	    Page outputpage = getMediaArchive().getPageManager().getPage(outputPath);

	    File output = new File(outputpage.getContentItem().getAbsolutePath());
	    output.getParentFile().mkdirs();

	    log.info("Dropbox Manager Downloading to " + outputPath);

	    // Use OutputFiller to save content to the file
	    filler.fill(resp.getEntity().getContent(), new FileOutputStream(output), true);
	    return outputpage;
	} else {
	    String error = IOUtils.toString(resp.getEntity().getContent());
	    log.error("Failed to download file. HTTP Status: " + error);
	    
	    return null;
	}

    }

}
