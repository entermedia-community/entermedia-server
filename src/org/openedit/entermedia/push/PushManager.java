package org.openedit.entermedia.push;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.search.AssetSearcher;
import org.openedit.entermedia.util.NaiveTrustManager;
import org.openedit.util.DateStorageUtil;
import org.openedit.xml.XmlSearcher;

import com.openedit.OpenEditException;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.Page;
import com.openedit.users.User;
import com.openedit.users.UserManager;

public class PushManager
{
	private static final Log log = LogFactory.getLog(PushManager.class);
	protected SearcherManager fieldSearcherManager;
	protected UserManager fieldUserManager;
	protected HttpClient fieldClient;
	private SAXReader reader = new SAXReader();
	public boolean login(String inCatalogId)
	{
		String server = getSearcherManager().getData(inCatalogId, "catalogsettings", "push_server_url").get("value");
		String account = getSearcherManager().getData(inCatalogId, "catalogsettings", "push_server_username").get("value");
		String password = getUserManager().decryptPassword(getUserManager().getUser(account));
		PostMethod method = new PostMethod(server + "/media/services/rest/login.xml");

		//TODO: Support a session key and ssl
		method.addParameter("accountname", account);
		method.addParameter("password", password);
		execute(inCatalogId, method);
		return true;
	}


	public UserManager getUserManager()
	{
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public HttpClient getClient(String inCatalogId)
	{
		if (fieldClient == null)
		{
			//http://stackoverflow.com/questions/2290570/pkix-path-building-failed-while-making-ssl-connection
			NaiveTrustManager.disableHttps();
			fieldClient = new HttpClient();
			login(inCatalogId);
		}
		return fieldClient;
	}

	public void processPushQueue(MediaArchive archive, User inUser)
	{
		//Searcher hot = archive.getSearcherManager().getSearcher(archive.getCatalogId(), "hotfolder");
		Searcher searcher = archive.getAssetSearcher();
		SearchQuery query = searcher.createSearchQuery();
		query.addMatches("category","index");
		query.addNot("pushstatus","complete");
		query.addNot("pushstatus","error");
		query.addSortBy("assetmodificationdate");
		HitTracker hits = searcher.search(query);

		Collection presets = archive.getCatalogSettingValues("push_convertpresets");
		List savequeue = new ArrayList();

		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{			
			String enabled = archive.getCatalogSettingValue("push_masterswitch");
			if( "false".equals(enabled) )
			{
				log.info("Push is paused");
				break;
			}
			
			Data hit = (Data) iterator.next();
			Asset target = (Asset) archive.getAssetBySourcePath(hit.getSourcePath());

			String mediatype = archive.getMediaRenderType(target.getFileFormat());

			List filestosend = new ArrayList();
			for (Iterator iterator2 = presets.iterator(); iterator2.hasNext();)
			{
				String presetid = (String) iterator2.next();
				Data preset = getSearcherManager().getData(archive.getCatalogId(), "convertpreset", presetid);
				String requiredtype = preset.get("inputtype");
				if( requiredtype != null && requiredtype.length() > 0)
				{
					if( !requiredtype.equals(mediatype))
					{
						continue;
					}
				}
						
				Page tosend = findInputPage(archive, target, preset);
				if (tosend.exists())
				{
					File file = new File(tosend.getContentItem().getAbsolutePath());
					filestosend.add(file);
				}
				else
				{
					//flag this as waiting
					saveAssetStatus(searcher, savequeue, target, "notallconverted", inUser);
					break;
				}
			}

			try
			{
				upload(target, archive, filestosend);
				target.setProperty("pusheddate", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
				saveAssetStatus(searcher, savequeue, target, "complete", inUser);

			}
			catch (Exception e)
			{
				target.setProperty("pusherrordetails", e.toString());
				saveAssetStatus(searcher, savequeue, target, "error", inUser);
				log.error("Could not push",e);
			}
		}
		searcher.saveAllData(savequeue, inUser);

	}


	protected void saveAssetStatus(Searcher searcher, List savequeue, Asset target, String inNewStatus, User inUser)
	{
		String oldstatus = target.get("pushstatus");
		if( oldstatus == null || !oldstatus.equals(inNewStatus))
		{
			target.setProperty("pushstatus", inNewStatus);
			savequeue.add(target);
			if( savequeue.size() == 100 )
			{
				searcher.saveAllData(savequeue, inUser);
				savequeue.clear();
			}
		}
	}

	protected Page findInputPage(MediaArchive mediaArchive, Asset asset, Data inPreset)
	{

		if (inPreset.get("type") == "original")
		{
			return mediaArchive.getOriginalDocument(asset);

		}
		String input = "/WEB-INF/data/" + mediaArchive.getCatalogId() + "/generated/" + asset.getSourcePath() + "/" + inPreset.get("outputfile");
		Page inputpage = mediaArchive.getPageManager().getPage(input);
		return inputpage;

	}
	//The client can only be used by one thread at a time
	protected synchronized Element execute(String inCatalogId, HttpMethod inMethod)
	{
		try
		{
			int status = getClient(inCatalogId).executeMethod(inMethod);
			if (status != 200)
			{
				throw new Exception("Request failed: status code " + status);
			}
			Element result = reader.read(inMethod.getResponseBodyAsStream()).getRootElement();
			return result;
		}
		catch (Exception e)
		{	

			throw new RuntimeException(e);
		}

	}
	
	public Map<String, String> upload(Asset inAsset, MediaArchive inArchive, List inFiles)
	{
		String server = inArchive.getCatalogSettingValue("push_server_url");
		//String account = inArchive.getCatalogSettingValue("push_server_username");
		String targetcatalogid = inArchive.getCatalogSettingValue("push_target_catalogid");
		//String password = getUserManager().decryptPassword(getUserManager().getUser(account));

		String url = server + "/media/services/rest/" + "handlesync.xml?catalogid=" + targetcatalogid;
		PostMethod method = new PostMethod(url);

		String prefix = inArchive.getCatalogSettingValue("push_asset_prefix");
		if( prefix == null)
		{
			prefix = "";
		}
		
		try
		{
			List<Part> parts = new ArrayList();
			int count = 0;
			for (Iterator iterator = inFiles.iterator(); iterator.hasNext();)
			{
				File file = (File) iterator.next();
				FilePart part = new FilePart("file." + count, file.getName(), file);
				parts.add(part);
				count++;
			}
//			parts.add(new StringPart("username", account));
//			parts.add(new StringPart("password", password));
			for (Iterator iterator = inAsset.getProperties().keySet().iterator(); iterator.hasNext();)
			{
				String key = (String) iterator.next();
				parts.add(new StringPart("field", key));
				parts.add(new StringPart(key+ ".value", inAsset.get(key)));
			}
			parts.add(new StringPart("sourcepath", inAsset.getSourcePath()));
			
			if(inAsset.getName() != null )
			{
				parts.add(new StringPart("original", inAsset.getName())); //What is this?
			}
			parts.add(new StringPart("id", prefix + inAsset.getId()));
			
//			StringBuffer buffer = new StringBuffer();
//			for (Iterator iterator = inAsset.getCategories().iterator(); iterator.hasNext();)
//			{
//				Category cat = (Category) iterator.next();
//				buffer.append( cat );
//				if( iterator.hasNext() )
//				{
//					buffer.append(' ');
//				}
//			}
//			parts.add(new StringPart("catgories", buffer.toString() ));
			
			Part[] arrayOfparts = parts.toArray(new Part[] {});

			method.setRequestEntity(new MultipartRequestEntity(arrayOfparts, method.getParams()));
			
			Element root = execute(inArchive.getCatalogId(), method);
			Map<String, String> result = new HashMap<String, String>();
			for (Object o : root.elements("asset"))
			{
				Element asset = (Element) o;
				result.put(asset.attributeValue("id"), asset.attributeValue("sourcepath"));
			}
			log.info("Sent " + server + "/" + inAsset.getSourcePath());
			return result;
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
	} 
	/*
	protected boolean checkPublish(MediaArchive archive, Searcher pushsearcher, String assetid, User inUser)
	{
		Data hit = (Data) pushsearcher.searchByField("assetid", assetid);
		String oldstatus = null;
		Asset asset = null;
		if (hit == null)
		{
			hit = pushsearcher.createNewData();
			hit.setProperty("assetid", assetid);
			oldstatus = "none";
			asset = archive.getAsset(assetid);
			hit.setSourcePath(asset.getSourcePath());
			hit.setProperty("assetname", asset.getName());
			hit.setProperty("assetfilesize", asset.get("filesize"));
		}
		else
		{
			oldstatus = hit.get("status");
			if( "1pushcomplete".equals( oldstatus ) )
			{
				return false;
			}
			asset = archive.getAssetBySourcePath(hit.getSourcePath());
		}
		if( log.isDebugEnabled() )
		{
			log.debug("Checking asset: " + asset);
		}
		
		if(asset == null)
		{
			return false;
		}
		String rendertype = archive.getMediaRenderType(asset.getFileFormat());
		if( rendertype == null )
		{
			rendertype = "document";
		}
		boolean readyforpush = true;
		Collection presets = archive.getCatalogSettingValues("push_convertpresets");
		for (Iterator iterator2 = presets.iterator(); iterator2.hasNext();)
		{
			String presetid = (String) iterator2.next();
			Data preset = archive.getSearcherManager().getData(archive.getCatalogId(), "convertpreset", presetid);
			if( rendertype.equals(preset.get("inputtype") ) )
			{
				Page tosend = findInputPage(archive, asset, preset);
				if (!tosend.exists())
				{
					if( log.isDebugEnabled() )
					{
						log.debug("Convert not ready for push " + tosend.getPath());
					}
					readyforpush = false;
					break;
				}
			}
		}
		String newstatus = null;
		if( readyforpush )
		{
			newstatus = "3readyforpush";
			hit.setProperty("percentage","0");
		}
		else
		{
			newstatus = "2converting";			
		}
		if( !newstatus.equals(oldstatus) )
		{
			hit.setProperty("status", newstatus);
			pushsearcher.saveData(hit, inUser);
		}
		return readyforpush;
	}
	*/
	public void resetPushStatus(MediaArchive inArchive, String oldStatus,String inNewStatus)
	{
		AssetSearcher assetSearcher = inArchive.getAssetSearcher();
		List savequeue = new ArrayList();
		HitTracker hits = assetSearcher.fieldSearch("pushstatus", oldStatus);
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			Asset asset = inArchive.getAssetBySourcePath(data.getSourcePath());
			asset.setProperty("pushstatus", inNewStatus);
			savequeue.add(asset);
			if( savequeue.size() == 100 )
			{
				assetSearcher.saveAllData(savequeue, null);
				savequeue.clear();
			}
		}
		assetSearcher.saveAllData(savequeue, null);
		
	}
	
	public Collection getCompletedAssets(MediaArchive inArchive)
	{
		HitTracker hits = inArchive.getAssetSearcher().fieldSearch("pushstatus", "complete");
		return hits;
	}


	public Collection getErrorAssets(MediaArchive inArchive)
	{
		HitTracker hits = inArchive.getAssetSearcher().fieldSearch("pushstatus", "error");
		return hits;
	}

	public Collection getNotConvertedAssets(MediaArchive inArchive)
	{
		HitTracker hits = inArchive.getAssetSearcher().fieldSearch("pushstatus", "notallconverted");
		return hits;
	}


}
