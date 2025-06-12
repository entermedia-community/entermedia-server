package org.entermediadb.drupal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.net.HttpSharedConnection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.BaseData;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.util.ExecutorManager;
import org.openedit.util.OutputFiller;
import org.openedit.util.XmlUtil;



public class DrupalManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(DrupalManager.class);
	protected String fieldCatalogId;
	protected MediaArchive fieldMediaArchive;
	protected ModuleManager fieldModuleManager;
	protected OutputFiller filler = new OutputFiller();
	protected XmlUtil fieldXmlUtil;
	protected Date fieldTokenTime;
	protected HttpSharedConnection connection;
	protected MultiValued fieldConfig;

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
	
	public MultiValued getConfig()
	{
		if (fieldConfig == null)
		{
			fieldConfig = new BaseData();
			fieldConfig.setValue("remoteroot", getMediaArchive().getCatalogSettingValue("drupal_remoteroot"));
			fieldConfig.setValue("contenttype", getMediaArchive().getCatalogSettingValue("drupal_contenttype"));
			fieldConfig.setValue("filter_created_startfrom", getMediaArchive().getCatalogSettingValue("drupal_startfrom"));
		}
		return fieldConfig;
	}

	public void setConfig(MultiValued inConfig)
	{
		fieldConfig = inConfig;
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
	
	
	
	public void syncContent()
	{
		Integer datacount = 0;
		
		Collection<JSONObject> content = listContent();
		if (content != null) {
			
			for (Iterator iterator = content.iterator(); iterator.hasNext();)
			{
				JSONObject object = (JSONObject) iterator.next();
				
				if (createContent(object)) 
					{
					datacount++;
					};
				
			}
		}
		if (datacount > 0)
		{
			log.info("Imported " + datacount  + " from Drupal.");
		}
	}
	
	public Collection listContent() {
		Collection results = new ArrayList();
		
		String url = getConfig().get("remoteroot") + "/jsonapi/node/" + getConfig().get("contenttype"); 
		if (getAccessToken() != null)
		{
			getConnection().putSharedHeader("Authorization", "Bearer " + getAccessToken());
		}
		JSONObject json = getConnection().getJson(url);
		if( json != null)
		{
			JSONArray data = (JSONArray)json.get("data");
			for (Iterator iterator = data.iterator(); iterator.hasNext();)
			{
				JSONObject object = (JSONObject) iterator.next();
				String id = (String)object.get("id");
				
				JSONObject item = new JSONObject();
				item.put("id", id);
				JSONObject attributes = (JSONObject) object.get("attributes");
				item.put("attributes", attributes);
				JSONObject links = (JSONObject) object.get("links");
				item.put("links", links);
				results.add(item);
			}
		
		}
		
		
		return results;
	}
	
	
	
	public Boolean createContent(JSONObject inContent)
	{
		Searcher contentSearcher = getMediaArchive().getSearcher("drupalcontent"); //entity
		//search if exists
		String id = inContent.get("id").toString();
		MultiValued contentData = (MultiValued) contentSearcher.searchById(id);
		if (contentData != null) {
			//already exists, update?
			return false;
		}
		
		contentData = (MultiValued) contentSearcher.createNewData();
		
		contentData.setValue("id", id );
		
		JSONObject links = (JSONObject) inContent.get("links");
		if (links != null)
		{
			String href = (String)((JSONObject) links.get("self")).get("href");
			if (href != null)
			{
				contentData.setValue("contentlink", href );
			}
		}
		
		JSONObject attributes = (JSONObject) inContent.get("attributes");
		
		contentData.setValue("name", attributes.get("title") );
		
		JSONObject body = (JSONObject) attributes.get("body");
		if (body != null)
		{
			contentData.setValue("longcaption", body.get("value"));
		}
		
		contentData.setValue("contentdate", attributes.get("changed"));  //created || changed
		
		
		//search asset fields
		
		for (Iterator iterator = contentSearcher.getPropertyDetails().iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();
			
			JSONObject field = (JSONObject)attributes.get(detail.getId());
			if (field != null)
			{
				String assetid = (String)field.get("asset_id");
				if (assetid != null)
				{
					contentData.setValue(detail.getId(), assetid);
				}
			}
			
		}
		
			
		contentSearcher.saveData(contentData);
		
		//get more data
		//contentData.setValue("name", attributes.get("title") );
		
		return false;
	}
	
	
	
	
	//TODO: Validate this token before running any API. Cache results
	public String getAccessToken() throws OpenEditException
	{
		String accesstoken = getConfig().get("httprequesttoken");
		return accesstoken;
	}
	
	
	
	public void refreshToken() throws OpenEditException
	{
		
		Data authinfo = getConfig();
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
