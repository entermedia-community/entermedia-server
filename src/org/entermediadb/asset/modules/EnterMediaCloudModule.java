package org.entermediadb.asset.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.mediadb.BaseJsonModule;
import org.entermediadb.authenticate.BaseAutoLogin;
import org.entermediadb.net.HttpSharedConnection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.page.Page;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;
import org.openedit.users.UserManager;
import org.openedit.users.authenticate.PasswordGenerator;

public class EnterMediaCloudModule extends BaseJsonModule
{
	private static final Log log = LogFactory.getLog(EnterMediaCloudModule.class);
	
	protected HttpSharedConnection fieldConnection;
	protected String fieldEndpointurl;

	public String getEndpointurl() {
		return fieldEndpointurl;
	}

	public void setEndpointurl(String endpointurl) {
		this.fieldEndpointurl = endpointurl;
	}

	protected HttpSharedConnection getConnection(MediaArchive archive)
	{
			
		if (fieldConnection == null ) {
			fieldConnection = createConnection(archive);
		}
		
		return fieldConnection;
	}
	
	protected HttpSharedConnection createConnection(MediaArchive archive)
	{
		String remotekey = archive.getCatalogSettingValue("remote-assetsearch-endpoint-key");
		HttpSharedConnection connection = new HttpSharedConnection();
		connection.addSharedHeader("X-token", remotekey);
		connection.addSharedHeader("X-tokentype", "entermedia");
		return connection;
	}

	//Give it a temporary key that is generated by the server and will be authenticated by the remote server as well
	public void loginUserWithExpiringKey(WebPageRequest inReq)
	{
		//createConnection
		//The adminkey will be defaulted for new sites. Once this is  used once we will want to change the login
		if(inReq.getUser() != null)
		{
			log.info("Already logged in as " + inReq.getUserName());
			inReq.putPageValue("status","Already logged in");	
			return;
			
		}
		
		boolean isoptions = inReq.getRequest().getMethod().equals("OPTIONS");
		if( isoptions)
		{
			//Dont redirect. Wait for CORS to run
			return;
		}
		
		//TODO: Make this expire
		String userkey = inReq.getRequestParameter("entermediacloudkey");
		if(userkey == null)
		{
			log.info("No key found " + userkey);
			inReq.putPageValue("status","No key found on request");	
			return;
		}
		String userid = userkey.substring(0,userkey.indexOf("md5"));
		
		//TODO: Make this configurable
		
		log.info("Validate user key: " +userkey + " | user: "+userid);
		
		
		JSONObject params = new JSONObject();
		params.put("accountname",userid);
		params.put("entermedia.key",userkey); //TODO: Some problem parsing this in NGINX/TOmacat without the .
		params.put("entermediakey",userkey);
		//TODO: Make sure this user is part of this collection 
		String collectionid = inReq.getRequestParameter("collectionid");
		
		MediaArchive archive = getMediaArchive(inReq);
		String workspaceid = archive.getCatalogSettingValue("workspace-id");
		if( workspaceid == null)
		{
			archive.setCatalogSettingValue("workspace-id", workspaceid);
		}
		else if( !workspaceid.equals(collectionid) )
		{
			inReq.putPageValue("status","Workspace ID does not match previous collection id");	
			return;
		}
		params.put("collectionid",collectionid);
		
		String base = archive.getCatalogSettingValue("workspace-provider-mediadb");//"https://entermediadb.org/entermediadb/mediadb";
		//String base = "http://localhost:8080/entermediadb/mediadb";
		String url = base + "/services/authentication/validateuser.json";
		CloseableHttpResponse resp = getConnection(archive).sharedPostWithJson(url, params);
		StatusLine filestatus = resp.getStatusLine();
		if (filestatus.getStatusCode() != 200)
		{
			//Problem
			log.info( filestatus.getStatusCode() + " URL issue " + " " + url + " with " + userkey);
			inReq.setCancelActions(true);
			return;
		}
		JSONObject data = getConnection(archive).parseJson(resp);
		String status = (String)data.get("status");
		if( "ok".equals(status))
		{
			UserManager userManager = (UserManager) getModuleManager().getBean("system", "userManager");
			String email = (String)data.get("email");
			
			User user = userManager.getUserByEmail(email);
			if( user == null)
			{
				user = userManager.createUser("em" + userid, null);
				user.setEmail(email);
				user.setEnabled(true);
			}
			user.setFirstName((String)data.get("firstname"));
			user.setLastName((String)data.get("lastname"));
			user.setPassword((String)data.get("password"));
			userManager.saveUser(user);

			String catalogid = inReq.findPathValue("catalogid");

			Searcher profilesearcher = getSearcherManager().getSearcher(catalogid, "userprofile");
			
			UserProfile userprofile = (UserProfile)profilesearcher.query().exact("userid",user.getId()).searchOne();
			if( userprofile == null)
			{
				userprofile = (UserProfile) profilesearcher.createNewData();
				userprofile.setId(user.getId());
				userprofile.setProperty("settingsgroup", "administrator"); //dependant on what what we get back from our site. On Team?
			}
			userprofile.setValue("entermediacloudkey",userkey);
			profilesearcher.saveData(userprofile);
			
			inReq.putSessionValue("systemuser", user);
			inReq.putSessionValue(catalogid + "user", user);
			inReq.putPageValue("user", user);
			
			BaseAutoLogin autologin = (BaseAutoLogin)getModuleManager().getBean(archive.getCatalogId(),"autoLoginWithCookie");
			autologin.saveCookieForUser(inReq, user);
			
		}
		else
		{
			log.info( status + " Could not login user " + userkey);
			inReq.setCancelActions(true);
		}
		inReq.putPageValue("status",status);		
	}

	public void getAdminKey(WebPageRequest inReq)
	{
			UserManager userManager = (UserManager) getModuleManager().getBean("system", "userManager");
			
			User user = userManager.getUser("admin");
			String password = user.getPassword();
			if( password.equals("admin") || password.equals("DES:2JPGMLB8Y60=") )
			{
				//reset their password to something better
				String tmppassword = new PasswordGenerator().generate();
				user.setPassword(tmppassword);				
			}
			
			String key = userManager.getStringEncryption().getEnterMediaKey(user);
			inReq.putPageValue("adminkey",key);
	}
	
	public boolean validateCloudKey(WebPageRequest inReq) {
		inReq.getJsonRequest();
		String userkey = inReq.getRequestParameter("entermediacloudkey");
		if(userkey == null)
		{
			log.info("No key found " + userkey);
			inReq.putPageValue("status","No key found on request");
			inReq.setCancelActions(true);
			return false;
		}
		String collectionid = inReq.getRequestParameter("collectionid");
		
		MediaArchive archive = getMediaArchive(inReq);
		String workspaceid = archive.getCatalogSettingValue("workspace-id");
		if( workspaceid == null)
		{
			archive.setCatalogSettingValue("workspace-id", collectionid);
		}
		else if( !workspaceid.equals(collectionid) )
		{
			inReq.putPageValue("status","Workspace ID does not match previous collection id");
			inReq.setCancelActions(true);
			return false;
		}

		JSONObject params = new JSONObject();
		params.put("entermedia.key",userkey);
		params.put("entermediakey",userkey);
		params.put("collectionid",collectionid);
		
		String base = archive.getCatalogSettingValue("workspace-provider-mediadb");
		
		if( base == null)
		{
			inReq.putPageValue("status","workspace-provider-mediadb is not set in catalog settings");
			inReq.setCancelActions(true);
			return false;
		}
		String url = base + "/services/authentication/validateuser.json";
		 
		CloseableHttpResponse resp = getConnection(archive).sharedPostWithJson(url, params);
		StatusLine filestatus = resp.getStatusLine();
		if (filestatus.getStatusCode() != 200)
		{
			//Problem
			log.info( filestatus.getStatusCode() + " URL issue " + " " + url + " with " + userkey);
			inReq.setCancelActions(true);
			return false;
		}
		JSONObject data = getConnection(archive).parseJson(resp);
		String status = (String)data.get("status");
		inReq.putPageValue("status",status);
		if( "ok".equals(status))
		{			
			return true;
		}
		inReq.setCancelActions(true);
		return false;
	}
	
	public void logInAsAdmin(WebPageRequest inReq) {
		UserManager userManager = (UserManager) getModuleManager().getBean("system", "userManager");
		
		User user = userManager.getUser("admin");
		inReq.putPageValue("user", user);
	}
	
	
	
	
	public void searchEnterMediaAssets(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		setEndpointurl(archive.getCatalogSettingValue("remote-assetsearch-mediadb-endpoint"));
		
		if( getEndpointurl() == null)
		{
			return;
		}
		String input = inReq.getRequestParameter("id");
		if(input == null) {
			return;
		}

		//Search category and asset and combine results
		String [] splits = input.split("-");
		String searchstring = splits[splits.length -1];
		
		Collection assets = searchRemoteAssets(archive, searchstring);
		inReq.putPageValue("assets", assets);

		Collection datacategories = searchRemoteCategories(archive, searchstring);
		inReq.putPageValue("foundcategories", datacategories);

	}

	protected Collection searchRemoteAssets(MediaArchive archive, 	String searchstring) {
		JSONArray termsarray = new JSONArray();
			JSONObject terms = new JSONObject();
			terms.put("field", "description");
			terms.put("value", searchstring);
			terms.put("operation", "matches");
			termsarray.add(terms);
		
		JSONObject search = new JSONObject();
		search.put("terms", termsarray);
		
		JSONObject params = new JSONObject();
		params.put("query", search);
		
		String url = 	getEndpointurl() + "/services/module/asset/search.json";  
		CloseableHttpResponse resp = getConnection(archive).sharedPostWithJson(url, params);
		log.info("Searching Remote: " + url + " Params: " + params.toString());
		StatusLine filestatus = resp.getStatusLine();
		if (filestatus.getStatusCode() != 200)
		{
			//Problem
			log.info( filestatus.getStatusCode() + " URL issue " + " " + url);
			return null;
		}
		JSONObject data = getConnection(archive).parseJson(resp);
		Collection jsonarray = (Collection)data.get("results");
		return jsonarray;
	}
	
	protected Collection searchRemoteCategories(MediaArchive archive, 	String searchstring) {

		JSONArray termsarray = new JSONArray();
		JSONObject terms = new JSONObject();
		terms.put("field", "categorypath");
		terms.put("value", searchstring);
		terms.put("operation", "matches");
		
		termsarray.add(terms);
		
		JSONObject search = new JSONObject();
		search.put("terms", termsarray);

		JSONObject params = new JSONObject();
		params.put("query", search);
		
		String url = getEndpointurl() + "/services/module/category/search.json";  
		log.info("Searching Remote: " + url + " Params: " + params.toString());
		CloseableHttpResponse resp = getConnection(archive).sharedPostWithJson(url, params);
		StatusLine filestatus = resp.getStatusLine();
		if (filestatus.getStatusCode() != 200)
		{
			//Problem
			log.info( filestatus.getStatusCode() + " URL issue " + " " + url);
			return null;
		}
		JSONObject data = getConnection(archive).parseJson(resp);
		Collection jsonarray = (Collection)data.get("results");
		return jsonarray;
	}
	
	
	public void importRemote(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		setEndpointurl(archive.getCatalogSettingValue("remote-assetsearch-mediadb-endpoint"));
		
		if( getEndpointurl() == null)
		{
			return;
		}
		
		String targetcategoryid = inReq.getRequestParameter("targetcategoryid");
		Category targetcategory = archive.getCategory(targetcategoryid);
		
		String[] assetids = inReq.getRequestParameters("assetid");
		List tosave = new ArrayList();
		if(assetids != null) {
			//download assets selected
			tosave = createAssetFromIds(inReq, archive, assetids);
			downloadAssets(inReq, archive, targetcategory, tosave);
		}

		String[] categoryids = inReq.getRequestParameters("categoryid");
		String[] categorynames = inReq.getRequestParameters("categoryname");
		List tosavemore = new ArrayList();
		if( categoryids != null)
		{
			//download assets in cateogires selected
			for (int i = 0; i < categoryids.length; i++) 
			{
				Collection assets = findAssetsInRemoteCategory(archive, categoryids[i]);
				if(assets != null) {
					tosavemore = createAssetFromResponse(inReq, archive, assets);
					if(tosavemore.size() > 0) 
					{
						String newcat = targetcategory.getCategoryPath() + "/imported/" +categorynames[i];
						Category newchild = archive.getCategorySearcher().createCategoryPath(newcat);
						downloadAssets(inReq, archive, newchild, tosavemore);
						archive.getAssetSearcher().saveAllData(tosavemore,inReq.getUser());
					}
				}
			}
		}
		tosave.addAll(tosavemore);
		inReq.putPageValue("downloaded", tosave.size());
		archive.fireSharedMediaEvent("importing/assetsreadmetadata");

		
	}

	private Collection findAssetsInRemoteCategory(MediaArchive archive, String searchterm) {

		JSONArray termsarray = new JSONArray();
		JSONObject terms = new JSONObject();
		terms.put("field", "category");
		terms.put("value", searchterm);
		terms.put("operation", "matches");
		
		termsarray.add(terms);
		
		JSONObject search = new JSONObject();
		search.put("terms", termsarray);

		JSONObject params = new JSONObject();
		params.put("query", search);
		String url = getEndpointurl() + "/services/module/asset/search.json";  
		log.info("Searching Remote: " + url + " Params: " + params.toString());
		CloseableHttpResponse resp = getConnection(archive).sharedPostWithJson(url, params);
		StatusLine filestatus = resp.getStatusLine();
		if (filestatus.getStatusCode() != 200)
		{
			//Problem
			log.info( filestatus.getStatusCode() + " URL issue " + " " + url);
			return null;
		}
		JSONObject data = getConnection(archive).parseJson(resp);
		Collection jsonarray = (Collection)data.get("results");
		return jsonarray;
	}

	protected List createAssetFromIds(WebPageRequest inReq, MediaArchive archive, String[] assetids) {
		String url = getEndpointurl() + "/services/module/asset/data/";
		List tosave = new ArrayList();
		for (int i = 0; i < assetids.length; i++) {
			//assetdata
			String path = url+assetids[i];
			CloseableHttpResponse resp = getConnection(archive).sharedGet(path);
			StatusLine filestatus = resp.getStatusLine();
			if (filestatus.getStatusCode() != 200)
			{
				//Problem
				throw new OpenEditException( filestatus.getStatusCode() + " URL issue " + " " + path);
			}
			JSONObject data = getConnection(archive).parseJson(resp);
			Data newasset = createAssetRecord(archive, assetids[i], (Map)data.get("data"));
			tosave.add(newasset);
		}
		return tosave;
	}
	
	protected List createAssetFromResponse(WebPageRequest inReq, MediaArchive archive, Collection assets) {
		List tosave = new ArrayList();
		for (Iterator iterator = assets.iterator(); iterator.hasNext();) {
			JSONObject data = (JSONObject) iterator.next();
			Data newasset = createAssetRecord(archive, data.get("id").toString(), (Map)data);
			tosave.add(newasset);
		}
		
		return tosave;
	}

	protected Data createAssetRecord(MediaArchive archive, String assetid, Map data) {
		String newid = "rm"+assetid;
		Data newasset = archive.getAsset(newid);
		if( newasset == null)
		{
			newasset = archive.getAssetSearcher().createNewData();
		}
		//Map newassetdata = data.get("data"); 
		data.put("emrecordstatus", null);
		
		populateJsonData(data, archive.getAssetSearcher(), newasset);
		newasset.setId(newid);
		return newasset;
	}

	protected void downloadAssets(WebPageRequest inReq, MediaArchive archive, Category category, List tosave) {

		String savepath = category.getCategoryPath() + "/imported/";
		Page savefolder = getPageManager().getPage("/WEB-INF/data/" + archive.getCatalogId() + "/originals/" + savepath);
		
		for (Iterator iterator = tosave.iterator(); iterator.hasNext();) {
			Asset newasset = (Asset) iterator.next();
			newasset.addCategory(category);
			newasset.setValue("importstatus", "needsmetadata");
			String download  = getEndpointurl() + "/services/module/asset/downloads/originals/" + archive.asLinkToOriginal(newasset);
			log.info("Downloading original: " + download);
			CloseableHttpResponse resp = getConnection(archive).sharedGet(download);
			String abspath = savefolder.getContentItem().getAbsolutePath() +  "" + newasset.getName();
			
			getConnection(archive).parseFile(resp, abspath);
			newasset.setSourcePath(savepath +  newasset.getName());
			
		}
		archive.getAssetSearcher().saveAllData(tosave,inReq.getUser());
	}
	

	
	
}
