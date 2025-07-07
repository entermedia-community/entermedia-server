package org.entermediadb.drupal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.net.HttpSharedConnection;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.BaseData;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
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
	
	protected HitTracker sourcesConfig;
	
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
	
	public HitTracker getSources()
	{
		if (sourcesConfig == null)
		{
			sourcesConfig = getMediaArchive().getSearcher("drupalsource").query().exact("enabled", "true").search();
		}
		return sourcesConfig;
	}
	
	
	
	public MultiValued getAuthConfig()
	{
		if (fieldConfig == null)
		{
			fieldConfig = new BaseData();
			fieldConfig.setValue("remoteroot", getMediaArchive().getCatalogSettingValue("drupal_remoteroot"));
			fieldConfig.setValue("drupalcontenttype", getMediaArchive().getCatalogSettingValue("drupal_contenttype"));
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
	
	
	
	public void syncContent(WebPageRequest inReq)
	{
		ScriptLogger inLog = (ScriptLogger)inReq.getPageValue("log");
		
		for (Iterator iterator1= getSources().iterator(); iterator1.hasNext();)
		{
			Integer datacount = 0;
			MultiValued source = (MultiValued) iterator1.next();
			Collection<JSONObject> content = listContent(source);
			if (content != null) {
				
				for (Iterator iterator = content.iterator(); iterator.hasNext();)
				{
					JSONObject object = (JSONObject) iterator.next();
					
					if (createContent(source, object)) 
						{
						datacount++;
						};
					
				}
			}
			if (datacount > 0)
			{
				log.info("Imported " + datacount  + " items from Drupal source: " + source.getName());
				inLog.info("Imported " + datacount  + " items from Drupal source: " + source.getName());
			}
		}
		
		
	}
	
	public Collection listContent(MultiValued inSource) {
		Collection results = new ArrayList();
		
		String url = inSource.get("remoteroot") + "/jsonapi/"+ inSource.get("structure")+"/" + inSource.get("drupalcontenttype"); 
		// Filters:
		// inSource.get(startfrom)
		
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
	
	
	
	public Boolean createContent(MultiValued inSource, JSONObject inContent)
	{
		String contentSearchtype = inSource.get("moduleid");
		Searcher contentSearcher = getMediaArchive().getSearcher(contentSearchtype); //entity
		//search if exists
		String id = inContent.get("id").toString();
		MultiValued contentData = (MultiValued) contentSearcher.searchById(id);
		if (contentData != null) {
			//already exists, update?
			return false;
		}
		
		contentData = (MultiValued) contentSearcher.createNewData();
		
		contentData.setValue("entitysourcetype", contentSearchtype);
		
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

		
		//drupal_internal__nid for node drupal_internal__id for entities
		Long drupalinternalid = (Long)attributes.get("drupal_internal__id");
		if (drupalinternalid == null)
		{
			drupalinternalid = (Long)attributes.get("drupal_internal__nid");
		}
		contentData.setValue("drupalinternalid",  drupalinternalid);
		
		String title = (String)attributes.get("title");
		if (title == null)
		{
			title = inSource.get("drupalcontenttype") + " " + drupalinternalid.toString();
		}
		
		contentData.setValue("name", title);
		contentData.setValue("contentdate", attributes.get("created"));  //created || changed

		JSONObject body = (JSONObject) attributes.get("body");
		if (body != null)
		{
			contentData.setValue("longcaption", body.get("value"));
		}
		
		
		/*
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
		*/
		
		List entityassets = new ArrayList();
		
		//Dynamic create fields when asset_id exists
		for (Iterator iterator = attributes.keySet().iterator(); iterator.hasNext();)
		{
			String attributekey = (String)iterator.next();
			if (attributes.get(attributekey) instanceof JSONObject)
			{
				JSONObject attribute = (JSONObject) attributes.get(attributekey);
				String assetid = (String)attribute.get("asset_id");
				if (assetid != null)
				{
					entityassets.add(assetid);
					contentData.setValue(attributekey, assetid);
					
					if (attributekey.endsWith("primary_media"))
					{
						contentData.setValue("primarymedia", assetid);
					}
					
				}
			}
		}
				
		
		//1 to many
		String parenttype = (String)attributes.get("parent_type");
		String parentid = (String)attributes.get("parent_id");
		if (parenttype != null && parentid != null)
		{
			for (Iterator iterator1= getSources().iterator(); iterator1.hasNext();)
			{
				MultiValued source = (MultiValued) iterator1.next();
				if (source.get("structure").equals(parenttype))
				{
					//It is a entity?
					String moduleid = source.get("moduleid");
					if (moduleid != null)
					{				
						Data parent = getMediaArchive().getSearcher(moduleid).query().exact("drupalinternalid", parentid).searchOne() ;
						if (parent != null)
						{
							contentData.setValue(moduleid, parent.get("id"));
						}
					}
				}
			}
			
		}
		
			
		contentSearcher.saveData(contentData);
		
		if (entityassets.size()> 0)
		{
			//get entity Category
			Category contentcategory = getMediaArchive().getEntityManager().createDefaultFolder(contentData, null);
			for (Iterator iterator = entityassets.iterator(); iterator.hasNext();)
			{
				String assetid = (String) iterator.next();
				Asset asset = getMediaArchive().getAsset(assetid);
				if (asset != null)
				{
					asset.addCategory(contentcategory);
					getMediaArchive().saveAsset(asset);
				}
			}
		}
		
		//get more data
		//contentData.setValue("name", attributes.get("title") );
		
		return true;
	}
	
	
	
	
	//TODO: Validate this token before running any API. Cache results
	public String getAccessToken() throws OpenEditException
	{
		String accesstoken = getAuthConfig().get("httprequesttoken");
		return accesstoken;
	}
	
	
	
	public void refreshToken() throws OpenEditException
	{
		
		Data authinfo = getAuthConfig();
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
