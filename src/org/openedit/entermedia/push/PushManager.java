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
import org.openedit.repository.ContentItem;
import org.openedit.util.DateStorageUtil;

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
	
	//TODO: Put a 5 minute timeout on this connection. This way we will reconnect
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
			login(inCatalogId); //TODO: Track the server we last logged into in case they edit the server
		}
		return fieldClient;
	}
	public void processPushQueue(MediaArchive archive, User inUser)
	{
		processPushQueue(archive,null,inUser);
	}
	/**
	 * This will just mark assets as error?
	 * @param archive
	 * @param inUser
	 */
	public void processPushQueue(MediaArchive archive, String inAssetIds, User inUser)
	{
		//Searcher hot = archive.getSearcherManager().getSearcher(archive.getCatalogId(), "hotfolder");
		Searcher searcher = archive.getAssetSearcher();
		SearchQuery query = searcher.createSearchQuery();
		if( inAssetIds == null )
		{
			query.addMatches("category","index");
			query.addMatches("importstatus","complete");
			query.addNot("pushstatus","complete");
			query.addNot("pushstatus","nogenerated");
			query.addNot("pushstatus","error");
		}
		else
		{
			String assetids = inAssetIds.replace(","," ");
			query.addOrsGroup( "id", assetids );
		}
		query.addSortBy("assetmodificationdate");
		HitTracker hits = searcher.search(query);
		hits.setHitsPerPage(100);
		if( hits.size() == 0 )
		{
			log.info("No new assets to push");
			return;
		}
		log.info("processing " + hits.size() + " assets to push");
		List savequeue = new ArrayList();
		int noasset = 0;
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{			
			Data hit = (Data) iterator.next();
			Asset asset = (Asset) archive.getAssetBySourcePath(hit.getSourcePath());
			if( asset != null )
			{
				savequeue.add(asset);
				if( savequeue.size() > 100 )
				{
					pushAssets(archive, savequeue);
					savequeue.clear();
				}
			}
			else
			{
				noasset++;
			}
		}
		log.info("Could not load " + noasset + " assets");
		if( savequeue.size() > 0 )
		{
			pushAssets(archive, savequeue);
			savequeue.clear();
		}
	}


	public void uploadGenerated(MediaArchive archive, User inUser, Asset target, List savequeue)
	{
		Searcher searcher = archive.getAssetSearcher();
		
		List paths = archive.getPageManager().getChildrenPaths("/WEB-INF/data/" + archive.getCatalogId() + "/generated/" + target.getSourcePath() );
		List<File> filestosend = new ArrayList<File>(paths.size());

		for (Iterator iterator = paths.iterator(); iterator.hasNext();)
		{
			String path = (String) iterator.next();
			ContentItem item = archive.getPageManager().getRepository().get(path);
			filestosend.add(new File( item.getAbsolutePath() ) );
		}
//			}
//			else
//			{
//				//Try again to run the tasks
//				archive.fireMediaEvent("importing/queueconversions", null, target);	//This will run right now, conflict?			
//				archive.fireMediaEvent("conversions/runconversion", null, target);	//This will run right now, conflict?			
//				tosend = findInputPage(archive, target, preset);
//				if (tosend.exists())
//				{
//					File file = new File(tosend.getContentItem().getAbsolutePath());
//					filestosend.add(file);
//				}
//				else
//				{
//					break;
//				}
//			}
//		}
		if( filestosend.size() > 0 )
		{
			try
			{
				upload(target, archive, "generated", filestosend);
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
		else
		{
			//upload(target, archive, "generated", filestosend);
			saveAssetStatus(searcher, savequeue, target, "nogenerated", inUser);
		}
	}


	protected void 	saveAssetStatus(Searcher searcher, List savequeue, Asset target, String inNewStatus, User inUser)
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
		http://demo.entermediasoftware.com
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
	
	public Map<String, String> upload(Asset inAsset, MediaArchive inArchive, String inUploadType, List<File> inFiles)
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
			
			parts.add(new StringPart("uploadtype", inUploadType)); 
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
			log.info("Sent " + server + "/" + inUploadType + "/" + inAsset.getSourcePath() + " with " + inFiles.size() + " generated files");
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
			log.debug("Checking 		String server = inArchive.getCatalogSettingValue("push_server_url");
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
				FilePart part = new FilePart("file." + count, file.getName(),upload file);
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
			{upload(target, archive, filestosend);
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
asset: " + asset);
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

		int size = 0;
		do
		{
			hits.setHitsPerPage(1000);
			size = hits.size();
			for (Iterator iterator = hits.getPageOfHits().iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				Asset asset = inArchive.getAssetBySourcePath(data.getSourcePath());
				asset.setProperty("pushstatus", inNewStatus);
				savequeue.add(asset);
				if( savequeue.size() == 1000 )
				{
					assetSearcher.saveAllData(savequeue, null);
					savequeue.clear();
				}
			}
			assetSearcher.saveAllData(savequeue, null);
			hits = assetSearcher.fieldSearch("pushstatus", oldStatus);
		} while( size > hits.size() );
		
		
	}
	
	public Collection getCompletedAssets(MediaArchive inArchive)
	{
		HitTracker hits = inArchive.getAssetSearcher().fieldSearch("pushstatus", "complete");
		return hits;
	}

	public Collection getProcessedAssets(MediaArchive inArchive)
	{
		SearchQuery q = inArchive.getAssetSearcher().createSearchQuery();
		q.setAndTogether(false);
		q.append("pushstatus", "nogenerated");
		q.append("pushstatus", "error");
		q.append("pushstatus", "complete");
		
		HitTracker hits = inArchive.getAssetSearcher().search(q);
		return hits;
	}


	public Collection getNoGenerated(MediaArchive inArchive)
	{
		HitTracker hits = inArchive.getAssetSearcher().fieldSearch("pushstatus", "nogenerated");
		return hits;
	}



	public Collection getErrorAssets(MediaArchive inArchive)
	{
		HitTracker hits = inArchive.getAssetSearcher().fieldSearch("pushstatus", "error");
		return hits;
	}

	public Collection getImportCompleteAssets(MediaArchive inArchive)
	{
		HitTracker hits = inArchive.getAssetSearcher().fieldSearch("importstatus", "complete");
		return hits;
	}

	public Collection getImportedAssets(MediaArchive inArchive)
	{
		HitTracker hits = inArchive.getAssetSearcher().fieldSearch("importstatus", "imported");
		return hits;
	}


	public void pushAssets(MediaArchive inArchive, List<Asset> inAssetsSaved)
	{
		String enabled = inArchive.getCatalogSettingValue("push_masterswitch");
		if( "false".equals(enabled) )
		{
			log.info("Push is paused");
			return;
		}

		List tosave = new ArrayList();
		//convert then save
		for (Iterator iterator = inAssetsSaved.iterator(); iterator.hasNext();)
		{
			Asset asset = (Asset) iterator.next();
			uploadGenerated(inArchive, null, asset, tosave);
		}
		inArchive.getAssetSearcher().saveAllData(tosave, null);

		
	}


	public void pollRemotePublish(MediaArchive inArchive)
	{
		fieldClient = null;
		
		String server = inArchive.getCatalogSettingValue("push_server_url");
		String targetcatalogid = inArchive.getCatalogSettingValue("push_target_catalogid");

		String url = server + "/media/services/rest/searchpendingpublish.xml?catalogid=" + targetcatalogid;
		url = url + "&field=remotempublishstatus&remotempublishstatus.value=new&operation=exact";
		PostMethod method = new PostMethod(url);

		try
		{
			Element root = execute(inArchive.getCatalogId(), method);
			if( root.elements().size() > 0 )
			{
				log.info("polled " + root.elements().size() + " children" );
			}
			for (Object row : root.elements("hit"))
			{
				Element hit = (Element)row;
				String sourcepath = hit.attributeValue("assetsourcepath");
				Asset asset = inArchive.getAssetBySourcePath(sourcepath);
				String publishtaskid = hit.attributeValue("id");
				String saveurl = server + "/media/services/rest/savedata.xml?save=true&catalogid=" + targetcatalogid + "&searchtype=publishqueue&id=" + publishtaskid;
				if( asset == null )
				{
					log.info("Asset not found: " + sourcepath);
					saveurl = saveurl + "&field=status&status.value=error";
					saveurl = saveurl + "&field=errordetails&errordetails.value=original_asset_not_found";
					PostMethod savemethod = new PostMethod(saveurl);
					Element saveroot = execute(inArchive.getCatalogId(), savemethod);
				}
				else
				{

					String presetid = hit.attributeValue("presetid");
					String destinationid = hit.attributeValue("publishdestination");
					
					Data preset = getSearcherManager().getData(inArchive.getCatalogId(), "convertpreset", presetid);

					Data publishedtask = convertAndPublish(inArchive, asset, publishtaskid, preset, destinationid);

					Page inputpage = null;
					String type = null;
					if( !"original".equals(preset.get("type")))
					{
						String input= "/WEB-INF/data/" + inArchive.getCatalogId() +  "/generated/" + asset.getSourcePath() + "/" + preset.get("outputfile");
						inputpage= inArchive.getPageManager().getPage(input);
						type = "generated";
					}
					else
					{
						inputpage = inArchive.getOriginalDocument(asset);
						type = "originals";
					}
					if( inputpage.length() == 0 )
					{
						saveurl = saveurl + "&field=status&status.value=error";
						saveurl = saveurl + "&field=remotempublishstatus&remotempublishstatus.value=error";
						saveurl = saveurl + "&field=errordetails&errordetails.value=output_not_found";
						PostMethod savemethod = new PostMethod(saveurl);
						Element saveroot = execute(inArchive.getCatalogId(), savemethod);
						continue;
					}

					String status = publishedtask.get("status");
	
					saveurl = saveurl + "&field=remotempublishstatus&remotempublishstatus.value=" +  status;
					saveurl = saveurl + "&field=status&status.value=" + status;
					if( status.equals("error") )
					{
						String errordetails = publishedtask.get("errordetails");
						if( errordetails != null )
						{
							saveurl = saveurl + "&field=errordetails&errordetails.value=" + errordetails;
						}
	
					} 
					else	if( destinationid.equals("0") )
					{
						//If this is a browser download then we need to upload the file
						List<File> filestosend = new ArrayList<File>(1);

						filestosend.add(new File( inputpage.getContentItem().getAbsolutePath() ) );
						upload(asset, inArchive, type, filestosend);
					}

					
					PostMethod savemethod = new PostMethod(saveurl);
					Element saveroot = execute(inArchive.getCatalogId(), savemethod);
				}
			}
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}

	}
	
	protected Data convertAndPublish(MediaArchive inArchive, Asset inAsset, String publishqueueid, Data preset, String destinationid) throws Exception
	{
		boolean needstobecreated = true;
		String outputfile = preset.get("outputfile");

		//Make sure preset does not already exists?
		if( needstobecreated && "original".equals( preset.get("type") ) )
		{
			needstobecreated = false;
		}			
		if( needstobecreated && inArchive.doesAttachmentExist(outputfile, inAsset) )
		{
			needstobecreated = false;
		}
		String assetid = inAsset.getId();
		if (needstobecreated)
		{
			Searcher taskSearcher = getSearcherManager().getSearcher(inArchive.getCatalogId(), "conversiontask");
			//TODO: Make sure it is not already in here
			SearchQuery q = taskSearcher.createSearchQuery().append("assetid", assetid).append("presetid", preset.getId());
			HitTracker hits = taskSearcher.search(q);
			if( hits.size() == 0 )
			{
				Data newTask = taskSearcher.createNewData();
				newTask.setSourcePath(inAsset.getSourcePath());
				newTask.setProperty("status", "new");
				newTask.setProperty("assetid", assetid);
				newTask.setProperty("presetid", preset.getId());
				taskSearcher.saveData(newTask, null);
			}
			//TODO: Make sure it finished?
			inArchive.fireMediaEvent("conversions/runconversion", null, inAsset);
		}
		
		//Add a publish task to the publish queue
		Searcher publishQueueSearcher = getSearcherManager().getSearcher(inArchive.getCatalogId(), "publishqueue");
		Data publishqeuerow =  (Data)publishQueueSearcher.searchById("remote" + publishqueueid);
		if( publishqeuerow == null )
		{
			publishqeuerow = publishQueueSearcher.createNewData();
			publishqeuerow.setId("remote" + publishqueueid);
			publishqeuerow.setProperty("status", "new");
			publishqeuerow.setProperty("assetid", assetid);
			publishqeuerow.setProperty("publishdestination", destinationid);
			publishqeuerow.setProperty("presetid", preset.getId() );
			String exportname = inArchive.asExportFileName(inAsset, preset);
			publishqeuerow.setProperty("exportname", exportname);
			publishqeuerow.setSourcePath(inAsset.getSourcePath());
			publishqeuerow.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
			publishQueueSearcher.saveData(publishqeuerow, null);
		}
		inArchive.fireMediaEvent("publishing/publishasset", null, inAsset);
		
		publishqeuerow =  (Data)publishQueueSearcher.searchById("remote" + publishqueueid);
		return publishqeuerow;
	}



}
