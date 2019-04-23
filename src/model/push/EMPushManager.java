package model.push;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.elasticsearch.http.HttpException;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.push.PushManager;
import org.entermediadb.modules.update.Downloader;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.HttpMimeBuilder;
import org.openedit.util.HttpRequestBuilder;
import org.openedit.util.PathUtilities;
public class EMPushManager extends BasePushManager implements PushManager
{
	static final Log log = LogFactory.getLog(EMPushManager.class);
	protected Downloader fielddownloader;
	protected ThreadLocal perThreadCache = new ThreadLocal();
	
	
	
	
	//TODO: Put a 5 minute timeout on this connection. This way we will reconnect
	/* (non-Javadoc)
	 * @see org.entermediadb.asset.push.PushManager#login(java.lang.String)
	 */
	public HttpClient login(String inCatalogId)
	{
//		System.getProperties().put("proxySet", "true");
//		System.getProperties().put("proxyHost", "localhost");
//		System.getProperties().put("proxyPort", "8082");
		
	  RequestConfig globalConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.DEFAULT)
                .build();
        HttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(globalConfig)
                .build();
		
		String server = getSearcherManager().getData(inCatalogId, "catalogsettings", "push_server_url").get("value");
		String account = getSearcherManager().getData(inCatalogId, "catalogsettings", "push_server_username").get("value");
		User user = getUserManager(inCatalogId).getUser(account);
		if( user == null)
		{
			log.info("No such user " + account);
			return null;
		}
		String password = getUserManager(inCatalogId).decryptPassword(user);
		HttpPost method = new HttpPost(server + "/media/services/rest/login.xml");

		List <org.apache.http.NameValuePair> nvps = new ArrayList <NameValuePair>();
		nvps.add(new BasicNameValuePair("accountname", account));
		nvps.add(new BasicNameValuePair("password", password));
		
		//add(new BasicNameValuePair(

		//TODO: Support a session key and ssl
		//execute(inCatalogId, method);
		
		try
		{
			method.setEntity(new UrlEncodedFormEntity(nvps));
			HttpResponse response2 = client.execute(method);
			StatusLine sl = response2.getStatusLine();           
			//int status = client.executeMethod(method);
			if (sl.getStatusCode() != 200)
			{
				throw new Exception(" ${method} Request failed: status code ${status}");
			}
		}
		catch ( Exception ex )
		{
			throw new OpenEditException(ex);
		}
		log.info("Login sucessful");
		return client;
	}


	public Downloader getDownloader(){
		if (fielddownloader == null){
			fielddownloader = new Downloader();
		}
		return fielddownloader;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.push.PushManager#getClient(java.lang.String)
	 */
	public HttpClient getClient(String inCatalogId)
	{
		HttpClient ref = (HttpClient) perThreadCache.get();
		if (ref == null)
		{
			if( ref == null)
			{
				ref = login(inCatalogId);
				// use weak reference to prevent cyclic reference during GC
				perThreadCache.set(ref);
			}
		}
		return ref;
	}
	/* (non-Javadoc)
	 * @see org.entermediadb.asset.push.PushManager#uploadGenerated(org.entermediadb.asset.MediaArchive, org.openedit.users.User, org.entermediadb.asset.Asset, java.util.List)
	 */
	public void uploadGenerated(MediaArchive archive, User inUser, Asset target, List savequeue)
	{
		Searcher searcher = archive.getAssetSearcher();
		

		List<ContentItem> filestosend = new ArrayList<ContentItem>();

		String path = "/WEB-INF/data/" + archive.getCatalogId() + "/generated/" + target.getPath();
		
		readFiles( archive.getPageManager(), path, path, filestosend );
		ContentItem item = archive.getPageManager().getRepository().getStub("/WEB-INF/data/" + archive.getCatalogId() +"/assets/" + target.getPath() + "/fulltext.txt");
		if( item.exists() )
		{
			filestosend.add(item);
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
				target.setValue("pusheddate", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
				log.info("pushed " + target.getId());
				saveAssetStatus(searcher, savequeue, target, "complete", inUser);

			}
			catch (Exception e)
			{
				target.setValue("pusherrordetails", e.toString());
				saveAssetStatus(searcher, savequeue, target, "error", inUser);
				log.error("Could not push",e);
			}
		}
		else
		{
			//upload(target, archive, "generated", filestosend);
			saveAssetStatus(searcher, savequeue, target, "nogenerated", inUser);
		}
		
		
		if("mp3".equalsIgnoreCase(target.getFileFormat())){
		
			ContentItem inputpage = archive.getOriginalContent(target);
			ArrayList mp3 = new ArrayList();
			
			mp3.add(inputpage);
			upload(target, archive, "original", mp3);
			
		}
		
		
	}


	private void readFiles(PageManager pageManager, String inRootPath,  String inPath, List<ContentItem> inFilestosend)
	{
		
		List paths = pageManager.getChildrenPaths(inPath);
		
		for (Iterator iterator = paths.iterator(); iterator.hasNext();)
		{
			String path = (String) iterator.next();
			ContentItem item = pageManager.getRepository().get(path);
			if( item.isFolder() )
			{
				readFiles(pageManager, inRootPath, path, inFilestosend);
			}
			else
			{
				inFilestosend.add( item );
			}
		}

	}
	public void toggle(String inCatalogId) {
		perThreadCache = new ThreadLocal();
	}
	
	
	
	public void pushAssets(MediaArchive inArchive, List<Asset> inAssetsSaved) {
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
			log.info("Pushing: " + asset.getId());
			uploadGenerated(inArchive, null, asset, tosave);
		}
		inArchive.getAssetSearcher().saveAllData(tosave, null);
		
	}

	
	public void processDeletedAssets(MediaArchive archive, User inUser) {
		//Searcher hot = archive.getSearcherManager().getSearcher(archive.getCatalogId(), "hotfolder");
		Searcher searcher = archive.getAssetSearcher();
		SearchQuery query = searcher.createSearchQuery();
		//query.addMatches("category","index");
		query.addOrsGroup("pushstatus","complete resend retry"); //retry is legacy
		query.addMatches("editstatus","7");
		query.addSortBy("id");
	
		//Push them and mark them as pushstatus deleted
		HitTracker hits = searcher.search(query);
		hits.setHitsPerPage(1000);
		if( hits.size() == 0 )
		{
			log.info("No new assets to delete");
			return;
		}
		long deleted = 0;
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			Asset asset = (Asset)searcher.loadData(data);
			if( asset == null )
			{
				log.error("Reindex assets" + data.getSourcePath() );
				continue;
			}
			
			upload(asset, archive, "delete",  (List<ContentItem>) Collections.EMPTY_LIST );
			asset.setValue("pushstatus", "deleted");
			archive.saveAsset(asset, null);
			deleted++;
		}
		log.info("Removed " + deleted);
	}
	
	public void pollRemotePublish(MediaArchive inArchive) {
		
		String enabled = inArchive.getCatalogSettingValue("push_masterswitch");
		if( enabled == null || "false".equals(enabled) )
		{
			//log.info("Push is paused");
			return;
		}
	
		
		String server = inArchive.getCatalogSettingValue("push_server_url");
		String targetcatalogid = inArchive.getCatalogSettingValue("push_target_catalogid");
	
		String url = server + "/media/services/rest/searchpendingpublish.xml?catalogid=" + targetcatalogid;
		//url = url + "&field=remotempublishstatus&remotempublishstatus.value=new&operation=exact";
		HttpPost method = new HttpPost(url);
		
		//loop over all the destinations we are monitoring
		Searcher dests = inArchive.getSearcher("publishdestination");
		Collection hits = dests.query().match("remotepublish", "true").search();
		if( hits.size() == 0 )
		{
			log.info("No remote publish destinations defined. Disable Pull Remote Event");
			return;
		}
		StringBuffer ors = new StringBuffer();
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			Data dest = (Data) iterator.next();
			ors.append(dest.getId());
			if( iterator.hasNext() )
			{
				ors.append(" ");
			}
		}
           		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();                                                                                                                                                 
		
		// nameValuePairs.add(new BasicNameValuePair("field", "publishdestination"));
	//	 nameValuePairs.add(new BasicNameValuePair("publishdestination.value", ors.toString()));
	//	 nameValuePairs.add(new BasicNameValuePair("operation", "orsgroup"));
		 nameValuePairs.add(new BasicNameValuePair("field", "status"));
		 nameValuePairs.add(new BasicNameValuePair("status.value", "complete"));
		 nameValuePairs.add(new BasicNameValuePair("operation", "not"));
		 nameValuePairs.add(new BasicNameValuePair("field", "status"));
		 nameValuePairs.add(new BasicNameValuePair("status.value", "error"));
		 nameValuePairs.add(new BasicNameValuePair("operation", "not"));
		
			Charset UTF8 = Charset.forName("UTF-8");

		
		
		method.setEntity(new UrlEncodedFormEntity(nameValuePairs, UTF8));
	
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
				try
				{
					runRemotePublish(inArchive, server, targetcatalogid, hit);
				}
				catch (Exception e)
				{
					log.error("Could not save publish " , e);
				}
			}
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
	
	}


	
/*
	protected Page findInputPage(MediaArchive mediaArchive, Asset asset, Data inPreset)
	{
//		http://demo.entermediasoftware.com
		if (inPreset.get("type") == "original")
		{
			return mediaArchive.getOriginalDocument(asset);

		}
		String input = "/WEB-INF/data/" + mediaArchive.getCatalogId() + "/generated/" + asset.getPath() + "/" + inPreset.get("generatedoutputfile");
		Page inputpage = mediaArchive.getPageManager().getPage(input);
		return inputpage;

	}
*/	
	protected Element execute(String inCatalogId, HttpPost inMethod)
	{
		try
		{
			return send(inCatalogId, inMethod);
		}
		catch (Exception e)
		{	
			log.error(e);
			//try logging in again?
			perThreadCache.remove();
		}
		try
		{
			return send(inCatalogId, inMethod);
		}
		catch (Exception e)
		{	
			throw new RuntimeException(e);
		}
	}
	protected Element send(String inCatalogId, HttpPost inMethod) throws IOException, HttpException, Exception, DocumentException
	{
		return send(getClient(inCatalogId),inCatalogId, inMethod);
	}
	protected Element send(HttpClient inClient, String inCatalogId, HttpPost inMethod) throws IOException, HttpException, Exception, DocumentException
	{
		//method.setEntity(new UrlEncodedFormEntity(nvps));
		HttpResponse response2 = inClient.execute(inMethod);
		StatusLine sl = response2.getStatusLine();           
		int status = sl.getStatusCode();
		if (status != 200)
		{
			throw new Exception( inMethod + " Request failed: status code " + status);
		}
		Element result = xmlUtil.getXml(response2.getEntity().getContent(),"UTF-8");
		inMethod.releaseConnection();
		return result;
	}
	
	public Map<String, String> upload(Asset inAsset, MediaArchive inArchive, String inUploadType, List<ContentItem> inFiles)
	{
		String server = inArchive.getCatalogSettingValue("push_server_url");
		//String account = inArchive.getCatalogSettingValue("push_server_username");
		String targetcatalogid = inArchive.getCatalogSettingValue("push_target_catalogid");
		//String password = getUserManager().decryptPassword(getUserManager().getUser(account));

		String url = server + "/media/services/rest/" + "handlesync.xml?dontclear=true&catalogid=" + targetcatalogid;
		HttpPost method = new HttpPost(url);
		//Request.setHeader(CoreProtocolPNames.HTTP_CONTENT_CHARSET, Consts.UTF_8);
		//method.setHeader(CoreProtocolPNames.HTTP_CONTENT_CHARSET, Consts.UTF_8);
		
		//method.setHeader("Content-Type", "multipart/form-data; charset=utf-8");
		
		//method.setHeader(HTTP.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=UTF-8");
		//method.setHeader(HTTP.CONTENT_TYPE, "multipart/form-data; charset=utf-8");
		
		//method.getParams().setContentCharset("utf-8"); //The line I added
		//method.setRequestHeader("Content-Type", "multipart/form-data; charset=utf-8");
		//method.getParams().setBooleanParameter(HttpMethodParams.USE_EXPECT_CONTINUE, true);
		
		String prefix = inArchive.getCatalogSettingValue("push_asset_prefix");
		if( prefix == null)
		{
			prefix = "";
		}
		
		try
		{
			HttpRequestBuilder builder = new HttpRequestBuilder();
			//builder.setCharset(Charset.forName("UTF-8"));
			
			int count = 0;
			for (Iterator iterator = inFiles.iterator(); iterator.hasNext();)
			{
				ContentItem file = (ContentItem) iterator.next();
				String name  =  PathUtilities.extractFileName( file.getPath() );
				String type = "file.";
				if( inUploadType.equals("originals") )
				{
					type = "originals." + type;
				}
				builder.addPart(type + count, new File(file.getAbsolutePath()), name);
				//FilePart part = new FilePart(type + count, name, new File( file.getAbsolutePath() ));
				count++;
			}

			//ContentType contentType = ContentType.create(HTTP.PLAIN_TEXT_TYPE, HTTP.UTF_8);
			//ContentType contentType = ContentType.create("text/plain", Charset.forName("UTF-8"));
			//Charset set = Charset.forName("UTF-8");
			
//			parts.add(new BasicNameValuePair("username", account));
//			parts.add(new BasicNameValuePair("password", password));
			for (Iterator iterator = inAsset.keySet().iterator(); iterator.hasNext();)
			{
				String key = (String) iterator.next();
				//TODO: handle geopoints
				if("category".equals(key) || "category-exact".equals(key) || "description".equals(key) || "geo_point".equals(key) || "position".equals(key)){
					continue; //we care creating this automatically from the sourcepath
				}

				if( !key.equals("libraries"))  //handled below
				{
					String value = inAsset.get(key);
					if( value != null)
					{
						builder.addPart("field",key);
						builder.addPart(key+ ".value",value);
//						log.info(inAsset.getName() + " " + key + " " + value);
					}
				}
				
			}
			builder.addPart("field", "name");
			builder.addPart("name.value",  inAsset.getName());
			builder.addPart("sourcepath",  inAsset.getSourcePath());
			builder.addPart("uploadtype",  inUploadType);
			builder.addPart("id",  prefix + inAsset.getId());
			
			if( inAsset.getKeywords().size() > 0 )
			{
				StringBuffer buffer = new StringBuffer();
				for (Iterator iterator = inAsset.getKeywords().iterator(); iterator.hasNext();)
				{
					String keyword = (String) iterator.next();
					buffer.append( keyword );
					if( iterator.hasNext() )
					{
						buffer.append('|');
					}
				}
				builder.addPart("keywords",buffer.toString());
			}
			
//TODO: Do we need to sync the category tree as well?
//			Collection libraries =  inAsset.getCategories();
//			if(  libraries != null && libraries.size() > 0 )
//			{
//				StringBuffer buffer = new StringBuffer();
//				for (Iterator iterator = inAsset.getLibraries().iterator(); iterator.hasNext();)
//				{
//					String keyword = (String) iterator.next();
//					buffer.append( keyword );
//					if( iterator.hasNext() )
//					{
//						buffer.append('|');
//					}
//				}
//				builder.add("libraries", buffer.toString() ));
//			}

//			Part[] arrayOfparts = builder.toArray(new Part[builder.size()]);
//
//			MultipartRequestEntity entity = new MultipartRequestEntity(arrayOfparts, method.getParams());
			//entity.
			method.setEntity(builder.build());
			
			Element root = execute(inArchive.getCatalogId(), method);
			Map<String, String> result = new HashMap<String, String>();
			for (Object o : root.elements("asset"))
			{
				Element asset = (Element) o;
				result.put(asset.attributeValue("id"), asset.attributeValue("sourcepath"));
			}
			log.info("Sent " + server + " Type:" + inUploadType + " Path" + inAsset.getSourcePath() + " with " + inFiles.size() + " generated files");
			return result;
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
	} 
	
	protected void runRemotePublish(MediaArchive inArchive, String server, String targetcatalogid, Element hit) throws Exception
	{
		String sourcepath = hit.attributeValue("assetsourcepath");
		String assetid = hit.attributeValue("assetid");
		Asset asset = null;
		if( assetid != null)
		{
			asset = inArchive.getAsset(assetid);
		}
		if( asset == null)
		{
			asset = inArchive.getAssetBySourcePath(sourcepath);
		}
		String publishtaskid = hit.attributeValue("id");
		String saveurl = server + "/media/services/rest/savedata.xml?save=true&catalogid=" + targetcatalogid + "&searchtype=publishqueue&id=" + publishtaskid;
		if( asset == null )
		{
			log.info("Asset not found: " + sourcepath);
			saveurl = saveurl + "&field=status&status.value=error";
			saveurl = saveurl + "&field=errordetails&errordetails.value=original_asset_not_found";
			HttpPost savemethod = new HttpPost(saveurl);
			Element saveroot = execute(inArchive.getCatalogId(), savemethod);
		}
		else
		{

			String presetid = hit.attributeValue("presetid");
			String destinationid = hit.attributeValue("publishdestination");
			
			
			//TODO: Use the standard browser download here
			
			Data preset = getSearcherManager().getData(inArchive.getCatalogId(), "convertpreset", presetid);

			String exportpath = hit.attributeValue("exportpath");

			Data publishedtask = convertAndPublish(inArchive, asset, publishtaskid, preset, destinationid, exportpath);

			ContentItem inputpage = null;
			String type = null;
			if( !"original".equals(preset.get("transcoderid")))
			{
				String input= "/WEB-INF/data/" + inArchive.getCatalogId() +  "/generated/" + asset.getPath() + "/" + preset.get("generatedoutputfile");
				inputpage= inArchive.getPageManager().getRepository().getStub(input);
				type = "generated";
			}
			else
			{
				inputpage = inArchive.getOriginalContent(asset);
				type = "originals";
			}
			if( inputpage.getLength() == 0 )
			{
				saveurl = saveurl + "&field=status&status.value=error";
				//saveurl = saveurl + "&field=remotempublishstatus&remotempublishstatus.value=error";
				saveurl = saveurl + "&field=errordetails&errordetails.value=output_not_found";
				HttpPost savemethod = new HttpPost(saveurl);
		
				Element saveroot = execute(inArchive.getCatalogId(), savemethod);
				
				return;
			}

			String status = publishedtask.get("status");

			//saveurl = saveurl + "&field=remotempublishstatus&remotempublishstatus.value=" +  status;
			saveurl = saveurl + "&field=status&status.value=" + status;
			if( status.equals("error") )
			{
				String errordetails = publishedtask.get("errordetails");
				if( errordetails != null )
				{
					saveurl = saveurl + "&field=errordetails&errordetails.value=" + URLEncoder.encode(errordetails,"UTF-8");
				}

			} 
			else if( destinationid.equals("0") ||  destinationid.equals("pushhttp"))
			{
				//If this is a browser download then we need to upload the file
				List<ContentItem> filestosend = new ArrayList<ContentItem>(1);

				filestosend.add(inputpage);

				//String 	rootpath = "/WEB-INF/data/" + inArchive.getCatalogId() +  "/originals/" + asset.getSourcePath();
			
				upload(asset, inArchive, type, filestosend);
			}

			
			HttpPost savemethod = new HttpPost(saveurl);
			Element saveroot = execute(inArchive.getCatalogId(), savemethod);					
		}
	}
	
	protected Data convertAndPublish(MediaArchive inArchive, Asset inAsset, String publishqueueid, Data preset, String destinationid, String exportpath) throws Exception
	{
		boolean needstobecreated = true;
		String outputfile = preset.get("generatedoutputfile");

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
				newTask.setValue("status", "new");
				newTask.setValue("assetid", assetid);
				newTask.setValue("presetid", preset.getId());
				taskSearcher.saveData(newTask, null);
			}
			//TODO: Make sure it finished?
			inArchive.fireMediaEvent("conversions","runconversion", null, inAsset);
		}
		
		//Add a publish task to the publish queue
		Searcher publishQueueSearcher = getSearcherManager().getSearcher(inArchive.getCatalogId(), "publishqueue");
		Data publishqeuerow =  (Data)publishQueueSearcher.searchById("remote" + publishqueueid);
		if( publishqeuerow == null )
		{
			publishqeuerow = publishQueueSearcher.createNewData();
			publishqeuerow.setId("remote" + publishqueueid);
			publishqeuerow.setValue("status", "new");
			publishqeuerow.setValue("assetid", assetid);
			publishqeuerow.setValue("publishdestination", destinationid);
			publishqeuerow.setValue("presetid", preset.getId() );
			//Why is this not being passed back to us?
			if( exportpath == null )
			{
				exportpath = inArchive.asExportFileName(inAsset, preset);
			}
			publishqeuerow.setValue("exportname", exportpath);
			publishqeuerow.setSourcePath(inAsset.getSourcePath());
			publishqeuerow.setValue("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
			publishQueueSearcher.saveData(publishqeuerow, null);
		}
		inArchive.fireMediaEvent("publishing","publishasset", null, inAsset);
		
		publishqeuerow =  (Data)publishQueueSearcher.searchById("remote" + publishqueueid);
		return publishqeuerow;
	}
}
