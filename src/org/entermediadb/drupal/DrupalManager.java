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
import org.elasticsearch.search.aggregations.metrics.InternalNumericMetricsAggregation.MultiValue;
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
		
		HitTracker sources = getSources();
		if (sources.isEmpty())
		{
			log.info("No Drupal sources configured");
			inLog.info("No Drupal sources configured");
			return;
		}
		inLog.info("Importing Drupal " +sources.size()+ " sources" );
		for (Iterator iterator1= getSources().iterator(); iterator1.hasNext();)
		{
			
			MultiValued source = (MultiValued) iterator1.next();
			String nextpageurl = source.get("remoteroot") + 
								"/" + source.get("remoteapipath") + 
								"/" + source.get("structure") +
								"/" + source.get("drupalcontenttype"); 
			// Filters:
			// inSource.get(startfrom)
			String sortby = source.get("sortfield");
			if (sortby != null)
			{
				nextpageurl += "?sort=" + sortby;
			}
			
			Integer datapage = 1;
			Integer datacount = 0;
			do
			{
				
				JSONObject sourcepage = getSourcePage(inLog, source, nextpageurl);
				if (sourcepage == null)
				{
					break;
				}
				//List all content by source/type ej: node/news_story
				Collection<JSONArray> sourcecontents = getSourceContents((JSONArray) sourcepage.get("data")); 
				if (!sourcecontents.isEmpty()) {
					//Loop content
					for (Iterator iterator = sourcecontents.iterator(); iterator.hasNext();)
					{
						JSONObject contentitem = (JSONObject) iterator.next();
						
						//Loop content items
						if (createContent(source, contentitem)) 
						{
						datacount++;
						};
						
					}
				}
				if (datacount > 0)
				{
					log.info("Imported page: " + datapage + " / " + datacount  + " items total from Drupal source: " + source.getName());
					inLog.info("Imported page: " + datapage + " / " + datacount  + " items total from Drupal source: " + source.getName());
				}
				
				JSONObject sourcelinks = (JSONObject)sourcepage.get("links");
				if (sourcelinks != null)
				{
					JSONObject nextpage = (JSONObject)sourcelinks.get("next");
					nextpageurl = (String)nextpage.get("href");
					if (datapage >1)
					{
						
						nextpageurl = null;
					}
				}
				datapage++;
			}while(nextpageurl != null);
			log.info("Imported Drupal finished");
		}
		
		
	}
	
	public JSONObject getSourcePage(ScriptLogger inLog, MultiValued inSource, String inUrl) {
		Collection results = new ArrayList();
		
		if (getAccessToken() != null)
		{
			getConnection().putSharedHeader("Authorization", "Bearer " + getAccessToken());
		}
		log.info("Getting source page: " + inUrl);
		JSONObject sourcepage = getConnection().getJson(inUrl);
		
		return sourcepage;
		
	}
	
	public Collection getSourceContents(JSONArray inData) {
		Collection results = new ArrayList();

		for (Iterator iterator = inData.iterator(); iterator.hasNext();)
		{
			JSONObject object = (JSONObject) iterator.next();
			String id = (String)object.get("id");
			JSONObject item = new JSONObject();
			item.put("id", id);
			item.put("data", object);
			//JSONObject links = (JSONObject) object.get("links");
			//item.put("links", links);
			results.add(item);
		}
		
		
		return results;
	}
	
	
	public JSONObject getSubContent(MultiValued inSource, String inContentType, String inContentId)
	{
		String subSource = inContentType.substring(0, inContentType.indexOf("--"));
		String subContentId = inContentType.substring(inContentType.indexOf("--") + 2);
		String url = inSource.get("remoteroot") + 
							"/" + inSource.get("remoteapipath") +
							"/" + subSource + 
							"/" + subContentId +
							"/" + inContentId; 

		if (getAccessToken() != null)
		{
			getConnection().putSharedHeader("Authorization", "Bearer " + getAccessToken());
		}
		JSONObject response = getConnection().getJson(url);
		if (response != null)
		{
			return (JSONObject) response.get("data"); 
			
		}
		log.info("Subcontent doesnt exist: " +subSource +"/" + subContentId +"/" + inContentId);
		return null;
	}
	
	
	
	public Boolean createContent(MultiValued inSource, JSONObject inContent)
	{
		//The moduleid we are saving to
		String contentSearchtype = inSource.get("moduleid");
		Searcher contentSearcher = getMediaArchive().getSearcher(contentSearchtype); //entity
		
		//search if exists
		String contentid = inContent.get("id").toString();  //TODO: used different id field
		MultiValued contentData = (MultiValued) contentSearcher.searchById(contentid);
		if (contentData != null) {
			//already exists, update?
			return false;
		}
		
		contentData = (MultiValued) contentSearcher.createNewData();
		contentData.setValue("entitysourcetype", contentSearchtype);
		contentData.setValue("id", contentid );
		
		JSONObject contentdata = (JSONObject) inContent.get("data");

		//Drupal internal nid
		Long drupalinternalid = (Long)contentdata.get("drupal_internal__nid");
		contentData.setValue("drupalinternalid",  drupalinternalid);
		//news_story fields:
		String title = (String)contentdata.get("title");
		if (title == null)
		{
			title = inSource.get("drupalcontenttype") + " " + drupalinternalid.toString();
		}
		
		contentData.setValue("name", title);
		contentData.setValue("contentdate", contentdata.get("field_news_date"));  //created || changed
		
		//path
		JSONObject contentpath = (JSONObject)contentdata.get("path");
		if (contentpath != null)
		{
			contentData.setValue("contentlink", inSource.get("remoteroot") + contentpath.get("alias"));
		}
		
		contentSearcher.saveData(contentData);
		
		
		//Save contents to SmartCreator
		Searcher sectionsearcher = getMediaArchive().getSearcher("componentsection");
		Searcher contentsearcher = getMediaArchive().getSearcher("componentcontent");
		
		Data section = sectionsearcher.query()
									.exact("playbackentityid", contentData.getId())
									.exact("playbackentitymoduleid", contentSearchtype).searchOne();
		
		if (section == null) 
		{
			//Create new
			section = sectionsearcher.createNewData();
			section.setValue("playbackentityid", contentid);
			section.setValue("playbackentitymoduleid", contentSearchtype);
			section.setValue("creationdate", new Date());
		}
		if (section != null) 
		{
			//remove section ??
			//sectionsearcher.delete(exists, null);
			
			//remove old contents ??
			//HitTracker oldcontents = contentsearcher.query().exact("componentsectionid", exists.getId()).search();
			//contentsearcher.deleteAll(oldcontents, null);
			
		}
		//rewrite section title	
		section.setValue("name", title);
		sectionsearcher.saveData(section);
		
		//Heading
		//createOrUpdateComponent(contentsearcher, section, "heading", title, 1);
		
		JSONObject  field_news_story_lead = (JSONObject) contentdata.get("field_news_story_lead");
		if (field_news_story_lead != null)
		{
			String storylead = (String)field_news_story_lead.get("processed");
			//contentData.setValue("storylead", storylead);
			createOrUpdateComponent(contentsearcher, section, "storylead", storylead, 1);
		}

		JSONArray field_news_story = (JSONArray) contentdata.get("field_news_story");
		if (!field_news_story.isEmpty())
		{
			for (Iterator iterator = field_news_story.iterator(); iterator.hasNext();)
			{
				JSONObject object = (JSONObject) iterator.next();
				String subcontenttype = (String)object.get("type");
				String subcontentid = (String)object.get("id");
				JSONObject paragraph = getSubContent(inSource, subcontenttype, subcontentid);
				if (paragraph == null)
				{
					continue;
				}
				JSONObject textfield = (JSONObject)paragraph.get("field_text_column");  //TODO: use drupalstructure table to map fields
				if (textfield == null)
				{
					continue;
				}
				String body = (String)textfield.get("processed");
				if (body != null)
				{
					//contentData.setValue("longcaption", body);
					createOrUpdateComponent(contentsearcher, section, "body", body, 3);
				}
			}
		}
		
		
		//Primary Asset
		JSONObject field_image = (JSONObject)contentdata.get("field_image");
		if (field_image != null)
		{
			String imagetype = (String)field_image.get("type");
			if (imagetype != null && imagetype.equals("media--entermedia_image"))
			{
				String imageid = (String)field_image.get("id");
				JSONObject drupalimage = getSubContent(inSource, imagetype, imageid);
				if (drupalimage != null)
				{
					JSONObject imagefield = (JSONObject) drupalimage.get("field_media_entermedia_image");
					if (imagefield != null)
					{
						String assetid = (String)imagefield.get("eid");
						if (assetid != null)
						{
							contentData.setValue("primarymedia", assetid);
							contentSearcher.saveData(contentData);
							
							createOrUpdateComponent(contentsearcher, section, "primarymedia", assetid, 2);
						}
					}
				}
			}
			
		}
		
		List entityassets = new ArrayList();	
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
		
		
		return true;
	}

	protected void createOrUpdateComponent(Searcher contentsearcher, Data inComponentSection, String inCommponentGroup, String inContent, int inOrdering )
	{
		
		Data component = contentsearcher.query().exact("componentgroup", inCommponentGroup)
												.exact("componentsectionid", inComponentSection)
												.searchOne();
		if (component == null)
		{
			component = contentsearcher.createNewData();
			component.setValue("componentgroup", inCommponentGroup);
			//component.setValue("componenttype", inCommponentType);
			component.setValue("componentsectionid", inComponentSection);
			component.setValue("creationdate", new Date());
		}
		if (inCommponentGroup.equals("primarymedia"))
		{
			component.setValue("assetid", inContent);
			component.setValue("componenttype", "asset");
		}
		else if (inCommponentGroup.equals("body"))
		{
			component.setValue("content", inContent);
			component.setValue("componenttype", "paragraph");
		}  
		else 
		{
			component.setValue("componentgroup", inCommponentGroup);
			component.setValue("componenttype", "paragraph"); 
			component.setValue("content", inContent);
		 }
		component.setValue("ordering", inOrdering);
		component.setValue("modificationdate", new Date());
		contentsearcher.saveData(component);
		
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
			

			JSONObject json = getConnection().parseMap(resp);
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
			
	
			JSONObject json = getConnection().parseMap(resp);
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
