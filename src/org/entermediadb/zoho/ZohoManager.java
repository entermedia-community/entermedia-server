package org.entermediadb.zoho;

import java.io.File;
import java.io.FileOutputStream;
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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
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
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.ExecutorManager;
import org.openedit.util.OutputFiller;
import org.openedit.util.XmlUtil;



public class ZohoManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(ZohoManager.class);
	protected String fieldCatalogId;
	protected MediaArchive fieldMediaArchive;
	protected ModuleManager fieldModuleManager;
	protected OutputFiller filler = new OutputFiller();
	protected XmlUtil fieldXmlUtil;
	protected Date fieldTokenTime;
	protected HttpSharedConnection connection;

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
	
	
	
	public int syncAssets(String inAccessToken, String inRoot, boolean savenow)
	{
		Integer assetcount = 0;
		try
		{

			Collection<JSONObject> projects = listProjects(inAccessToken, inRoot);

			for (Iterator iterator = projects.iterator(); iterator.hasNext();) {
				JSONObject project = (JSONObject) iterator.next();
				
				JSONObject projectsettings = getProjectSettings(inAccessToken, inRoot, project);
				
				Collection<JSONObject> projectFolders = listProjectFolders(inAccessToken, projectsettings);
			
				for (Iterator iterator2 = projectFolders.iterator(); iterator2.hasNext();) {
					JSONObject projectFolder = (JSONObject) iterator2.next();
					String categoyPath = "Zoho/" + project.get("name") +"/" + projectFolder.get("name");
					assetcount = assetcount + processProjectFilesAndFolders(inAccessToken, categoyPath, projectFolder, savenow);
				}
				
			}
			getMediaArchive().fireSharedMediaEvent("conversions/runconversions"); // this will save the asset as// imported
		}
		catch (Exception ex)
		{
			log.info("Access Token: " + inAccessToken);
			throw new OpenEditException(ex);
		}
		return assetcount;

	}
	
	

	public Collection listProjects(String inAccessToken, String portalID) throws Exception
	{
		Collection results = new ArrayList();
		//https://projectsapi.zoho.com/restapi/portal/orrenpickellbuildinggroup/projects/

		String url = "https://projectsapi.zoho.com/restapi/portal/"+portalID+"/projects/"; 
		getConnection().putSharedHeader("Authorization", "Bearer " + inAccessToken);
		JSONObject json = getConnection().getJson(url);
		if( json != null)
		{
			JSONArray data = (JSONArray)json.get("projects");
			for (Iterator iterator = data.iterator(); iterator.hasNext();)
			{
				JSONObject object = (JSONObject) iterator.next();
				Long id = (Long)object.get("id");
				JSONObject item = new JSONObject();
				item.put("id", id.toString());
				item.put("name", (String)object.get("name"));
				results.add(item);
			}
		
		}
		return results;
	}

	
	public JSONObject getProjectSettings(String inAccessToken, String portalId, JSONObject project) throws Exception
	{
		JSONObject projectsettings = new JSONObject();

		//Doc: https://www.zoho.com/projects/help/rest-api/documents-api.html
		
		//TODO: alternative options to read folders? 
		//https://help.zoho.com/portal/en-gb/community/topic/how-to-get-a-list-of-team-folders-associated-to-a-project-from-the-api
		
		//https://projectsapi.zoho.com/restapi/portal/orrenpickellbuildinggroup/projects/PROJECTID/documents/settings
		String projectId = (String)project.get("id");
		String url = "https://projectsapi.zoho.com/restapi/portal/"+portalId+"/projects/"+ projectId +"/documents/settings/"; //tested with and without trailing slash
		getConnection().putSharedHeader("Authorization", "Bearer " + inAccessToken);
		JSONObject json = getConnection().getJson(url);
		if( json != null)
		{
			JSONObject data = (JSONObject)json.get("document_details");
			JSONObject settings = (JSONObject)data.get("thirdparty_settings");
			JSONArray team_folders = (JSONArray)settings.get("team_folders");
			if(team_folders != null) {
				for (Iterator iterator = team_folders.iterator(); iterator.hasNext();) //Loop Team Folders 
				{
					JSONObject teamfolder = (JSONObject) iterator.next();
					String id = (String)teamfolder.get("thirdparty_folder_id");
					projectsettings.put("id", id);
					projectsettings.put("name", (String)teamfolder.get("name"));
				}
			}
		
		}
		return projectsettings;
	}
	
	
	

	
	public Collection listProjectFolders(String inAccessToken, JSONObject project) {
		Collection folders = new ArrayList();
		String workspaceid = (String)project.get("id");
		if(workspaceid == null) {
			return folders;
		}
		String folderLink = "https://workdrive.zoho.com/api/v1/workspaces/"+workspaceid+"/folders";
		
		getConnection().putSharedHeader("Authorization", "Bearer " + inAccessToken);
		JSONObject json = getConnection().getJson(folderLink);
		if( json != null)
		{
			JSONArray data = (JSONArray)json.get("data");
			for (Iterator iterator = data.iterator(); iterator.hasNext();)
			{
				JSONObject object = (JSONObject) iterator.next();
				String id = (String)object.get("id");
				JSONObject attributes = (JSONObject) object.get("attributes");
				String type = (String)attributes.get("type");
				if (type.equals("folder")) {
					JSONObject relationships = (JSONObject) object.get("relationships");
					JSONObject item = new JSONObject();
					item.put("id", id);
					item.put("name", (String)attributes.get("name"));
					folders.add(item);
				}
				
			}
		
		}
		return folders;
	}
	
	public Collection listProjectFilesOrSubfolders(String inAccessToken, JSONObject projectFolder) {
		Collection files = new ArrayList();
		String folderid = (String)projectFolder.get("id");
		String filesLink = "https://workdrive.zoho.com/api/v1/workspaces/"+folderid+"/files";
		getConnection().putSharedHeader("Authorization", "Bearer " + inAccessToken);
		JSONObject json = getConnection().getJson(filesLink);
		if( json != null)
		{
			JSONArray data = (JSONArray)json.get("data");
			for (Iterator iterator = data.iterator(); iterator.hasNext();)
			{
				//
				
				JSONObject object = (JSONObject) iterator.next();
				String id = (String)object.get("id");
				
				JSONObject attributes = (JSONObject) object.get("attributes");
				String type = (String)attributes.get("type");
				String name = (String)attributes.get("name");
				if(name.endsWith(".zip")) {
					continue;
				}
				Boolean isfolder = (Boolean)attributes.get("is_folder");
				if(isfolder == false) 
				{
					JSONObject item = new JSONObject();
					item.put("id", id);
					item.put("name", name);
					item.put("download_url", (String)attributes.get("download_url"));
					item.put("permalink", (String)attributes.get("permalink"));
					files.add(item);
				}
				else {
					//loop over subfolders
				}
			}
		
		}
		return files;
	}
	
	
	
	//TODO: We need the Project first
	public Results listTeamFolders(String inAccessToken, String teamId) throws Exception
	{
		//Zoho scope WorkDrive.teamfolders.ALL
//		https://www.zohoapis.com/workdrive/api/v1/teams/pwyo7952f7a1662bf4960825ea0a72a771c3e/teamfolders?page%5Blimit%5D=50&page%5Boffset%5D=0
		
		
		String url = "https://www.zohoapis.com/workdrive/api/v1/teams/"+ teamId + "/teamfolders";
		String search = "page%5Blimit%5D=50&page%5Boffset%5D=0";
		url = url + "?" + search;
		// TODO: Add date query from the last time we imported
		log.info("Zoho Team Folders: "+url);

		HttpSharedConnection connection = getConnection(); 
		connection.putSharedHeader("Authorization", "Bearer " + inAccessToken);
		JSONObject json = getConnection().getJson(url);
		if( json != null)
		{
			JSONArray data = (JSONArray)json.get("data");
			for (Iterator iterator = data.iterator(); iterator.hasNext();)
			{
				JSONObject object = (JSONObject) iterator.next();
				String id = (String)object.get("id");

				JSONObject attributes = (JSONObject) object.get("attributes");
				String name = (String)attributes.get("name");
				
			}
		
		}
		
		Results results = new Results();
		return results;
		
		
	}
	
	
	
	
	
	//TODO: Validate this token before running any API. Cache results
	//Create user if not exists

	public String getAccessToken(Data hotfolderConfig) throws OpenEditException
	{
		
		Data authinfo = getMediaArchive().getData("oauthprovider", "zohoselfclient");
		String accesstoken = authinfo.get("httprequesttoken");
		return accesstoken;
	}
	
	
	
	public void refreshToken() throws OpenEditException
	{
		
		Data authinfo = getMediaArchive().getData("oauthprovider", "zohoselfclient");
		String accesstoken = authinfo.get("httprequesttoken");
		String clientid = null;
		String clientsecret = null;
		String granttoken = null;
		clientid = authinfo.get("clientid");
		clientsecret = authinfo.get("clientsecret");				
		granttoken = authinfo.get("granttoken");
		String accountsUrl = "https://accounts.zoho.com/oauth/v2/token";
		String token = authinfo.get("refreshtoken");

		if(token == null) {
			Map params = new HashMap();
			params.put("client_id", clientid);
			params.put("client_secret", clientsecret);
			params.put("code", granttoken);
			params.put("grant_type", "authorization_code");
			
			CloseableHttpResponse resp = getConnection().sharedPost(accountsUrl, params);

			if (resp.getStatusLine().getStatusCode() != 200)
			{
				log.info("Zoho Server error returned " + resp.getStatusLine().getStatusCode());
			}
			

			JSONObject json = getConnection().parseJson(resp);
			if(json.get("access_token") != null) {
				accesstoken = (String)json.get("access_token"); 
				authinfo.setValue("httprequesttoken", json.get("access_token"));
				authinfo.setValue("refreshtoken", json.get("refresh_token"));
				authinfo.setValue("accesstokentime", new Date());
				authinfo.setValue("expiresin", json.get("expires_in"));
				getMediaArchive().getSearcher("oauthprovider").saveData(authinfo);
				log.info("Refresh token granted");
			}
			else {
				log.info(json);
				throw new OpenEditException("Token Expired, manually provide new oauthproviders granttoken: https://api-console.zoho.com - Required SCOPES: ZohoProjects.projects.ALL,ZohoProjects.documents.ALL,ZohoPC.files.ALL,WorkDrive.teamfolders.ALL,WorkDrive.team.ALL,WorkDrive.files.ALL,ZohoFiles.files.READ");
			}
			
		}
		else {
			Map params = new HashMap();
			params.put("client_id", clientid);
			params.put("client_secret", clientsecret);
			params.put("refresh_token", token);
			params.put("grant_type", "refresh_token");
			
			CloseableHttpResponse resp = getConnection().sharedPost(accountsUrl, params);
	
			if (resp.getStatusLine().getStatusCode() != 200)
			{
				log.info("Zoho Server error returned " + resp.getStatusLine().getStatusCode());
			}
			
	
			JSONObject json = getConnection().parseJson(resp);
			if(json.get("access_token") != null) {
				accesstoken = (String)json.get("access_token"); 
				authinfo.setValue("httprequesttoken", json.get("access_token"));
				authinfo.setValue("accesstokentime", new Date());
				authinfo.setValue("expiresin", json.get("expires_in"));
				getMediaArchive().getSearcher("oauthprovider").saveData(authinfo);
			}

		}
		
	}
	

	

	protected int processProjectFilesAndFolders(String inAccessToken, String inCategoryPath, JSONObject projectFolder, boolean savenow) throws Exception
	{
		Collection<JSONObject> folderFiles = listProjectFilesOrSubfolders(inAccessToken, projectFolder);
		return createAssets(inAccessToken, inCategoryPath, folderFiles, savenow);

	}

	protected int createAssets(String inAccessToken, String categoryPath, Collection inFiles, boolean savenow) throws Exception
	{
		if (inFiles == null)
		{
			return 0;
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
		Integer assetcount = 0;
		Map onepage = new HashMap();
		for (Iterator iterator = inFiles.iterator(); iterator.hasNext();)
		{
			JSONObject object = (JSONObject) iterator.next();
			String id = (String)object.get("id");
			onepage.put(id, object);
			String fs = (String)object.get("size");
			if (fs != null)
			{
				leftkb = leftkb - (Long.parseLong(fs) / 1000);
				if (leftkb < Long.parseLong(free))
				{
					log.info("Not enough disk space left to download more " + leftkb + "<" + free);
					return assetcount;
				}
			}

			if (onepage.size() == 30)
			{
				createAssetsIfNeeded(inAccessToken, onepage, category, savenow);
				onepage.clear();
			}
			assetcount = assetcount +1;
		}
		createAssetsIfNeeded(inAccessToken, onepage, category, savenow);
		return assetcount;
	}

	private void createAssetsIfNeeded(String inAccessToken, Map<String, JSONObject> inOnepage, Category category, boolean savenow) throws Exception
	{
		if (inOnepage.isEmpty())
		{
			log.info("empty map");
			return;
		}
		Collection tosave = new ArrayList();
		Map existingAssets = new  HashMap();

		HitTracker existingassets = getMediaArchive().getAssetSearcher().query().orgroup("zohoid", inOnepage.keySet()).search();
		log.info("checking " + existingassets.size() + " assets ");
		// Update category
		for (Iterator iterator = existingassets.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			Asset existing = (Asset) getMediaArchive().getAssetSearcher().loadData(data);
			// Remove existing assets
			existingAssets.put(existing.get("zohoid"), existing);
			if(existing.get("importstatus").equals("error") ) {
				continue;
			}
			inOnepage.remove(existing.get("zohoid"));
			// existing.clearCategories();
			if (!existing.isInCategory(category))
			{
				// Clear old Drive categorties
				Category root = getMediaArchive().createCategoryPath("Zoho");
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
			JSONObject object = (JSONObject) inOnepage.get(id);

			// log.info(object.get("kind"));// "kind": "drive#file",
			// String md5 = object.get("md5Checksum").getAsString();
			Asset newasset = (Asset)existingAssets.get(id);
			if( newasset == null)
			{
				newasset = (Asset) getMediaArchive().getAssetSearcher().createNewData();
			}
			String filename = (String)object.get("name");
			filename = filename.trim();
			// JsonElement webcontentelem = object.get("webContentLink");

			newasset.setSourcePath(category.getCategoryPath() + "/" + filename);
			newasset.setFolder(false);
			newasset.setValue("zohoid", id);
			newasset.setValue("assetaddeddate", new Date());
			//newasset.setValue("retentionpolicy", "deleteoriginal"); // Default
			newasset.setValue("importstatus", "uploading");
			String downloadurl = (String)object.get("download_url");
			if (downloadurl != null)
			{
				newasset.setValue("zohodownloadurl", downloadurl);
			}

			// TODO: Add dates here

			newasset.setName(filename);
			String weblink  = (String)object.get("permalink");
			if (weblink != null)
			{
				newasset.setValue("linkurl", weblink);
			}
			// inArchive.getAssetSearcher().saveData(newasset);
			tosave.add(newasset);
		}
		if (!tosave.isEmpty())
		{
			for (Iterator iterator = tosave.iterator(); iterator.hasNext();)
			{
				Asset asset = (Asset) iterator.next();
				saveFile(inAccessToken, asset);
			}

			log.info("Saving new assets " + tosave.size());
			getMediaArchive().saveAssets(tosave);
			//getMediaArchive().fireMediaEvent("importing/importassets", null, tosave); // Will launch events for "importstatus=created" assets
			getMediaArchive().fireSharedMediaEvent("importing/assetscreated"); 
		}
		
		
		/*
		
		//retry Download Errors
		HitTracker existingerror = getMediaArchive().getAssetSearcher().query().orgroup("importstatus", "error imported").orgroup("zohoid", inOnepage.keySet()).search();
		if(!existingerror.isEmpty()) {
			log.info("Eror or Pending found: " + existingerror.size());
			for (Iterator iterator = existingerror.iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				Asset existing = (Asset) getMediaArchive().getAssetSearcher().loadData(data);
				log.info("Retring Download: " + existing);
				saveFile(inAccessToken, existing);
			}
		}
		*/
	}
	
	public void saveFile(String inAccessToken, Asset inAsset) throws Exception
	{

		String url =  inAsset.get("zohodownloadurl");
		if(url != null) {
			HttpRequestBase httpmethod = new HttpGet(url);
			HttpSharedConnection connection = getConnection();
			connection.putSharedHeader("Authorization", "Bearer " + inAccessToken);
			CloseableHttpResponse resp = connection.sharedExecute(httpmethod);
			try
			{
				if (resp.getStatusLine().getStatusCode() != 200)
				{
					log.info("Zoho Server error returned " + resp.getStatusLine().getStatusCode());
					log.info("Zoho Server error returned " + resp.getStatusLine());
					inAsset.setProperty("importstatus", "error");
					return;
					//throw new OpenEditException("Could not save: " + inAsset.getName());
				}
		
				HttpEntity entity = resp.getEntity();
		
				ContentItem item = getMediaArchive().getOriginalContent(inAsset);
		
				File output = new File(item.getAbsolutePath());
				output.getParentFile().mkdirs();
				log.info("Zoho Manager Downloading " + item.getPath());
				filler.fill(entity.getContent(), new FileOutputStream(output), false);
			
				// getMediaArchive().getAssetImporter().reImportAsset(getMediaArchive(),
				// inAsset);
				// ContentItem itemFile = getMediaArchive().getOriginalContent(inAsset);
				inAsset.setProperty("importstatus", "created");
				//getMediaArchive().getAssetImporter().getAssetUtilities().getMetaDataReader().updateAsset(getMediaArchive(), item, inAsset);
				//inAsset.setProperty("previewstatus", "converting");
				//getMediaArchive().saveAsset(inAsset);
				//getMediaArchive().fireMediaEvent("assetimported", null, inAsset); // Run custom scripts?
			}
			finally
			{
				connection.release(resp);
			}
		}
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


	public ExecutorManager getExecutorManager()
	{
		ExecutorManager queue = (ExecutorManager) getModuleManager().getBean(getMediaArchive().getCatalogId(), "executorManager");
		return queue;
	}


	protected HttpSharedConnection getConnection() 
	{
		if( connection == null)
		{
			connection = new HttpSharedConnection();
			connection.putSharedHeader("Accept", "application/vnd.api+json");
		}
		return connection;
	}
	


	
}
