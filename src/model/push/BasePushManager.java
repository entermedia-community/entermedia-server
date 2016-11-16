package model.push;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.elasticsearch.http.HttpException;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.push.PushManager;
import org.entermediadb.asset.search.AssetSearcher;
import org.entermediadb.asset.upload.FileUpload;
import org.entermediadb.asset.upload.FileUploadItem;
import org.entermediadb.asset.upload.UploadRequest;
import org.entermediadb.data.ImmutableData;
import org.entermediadb.modules.update.Downloader;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.users.UserManager;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.PathUtilities;
import org.openedit.util.XmlUtil;
public class BasePushManager implements PushManager
{
	private static final Log log = LogFactory.getLog(PushManager.class);
	protected SearcherManager fieldSearcherManager;
	protected PageManager fieldPageManager;
	protected Downloader fielddownloader;
	protected XmlUtil xmlUtil = new XmlUtil();
	//protected HttpClient fieldClient;
	
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


	/* (non-Javadoc)
	 * @see org.entermediadb.asset.push.PushManager#getUserManager()
	 */
	public UserManager getUserManager(String inCatalogId)
	{
		return 	(UserManager)getSearcherManager().getModuleManager().getBean(inCatalogId,"userManager");
	}


	/* (non-Javadoc)
	 * @see org.entermediadb.asset.push.PushManager#getSearcherManager()
	 */
	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.push.PushManager#setSearcherManager(org.openedit.data.SearcherManager)
	 */
	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
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
	 * @see org.entermediadb.asset.push.PushManager#processPushQueue(org.entermediadb.asset.MediaArchive, org.openedit.users.User)
	 */
	public void processPushQueue(MediaArchive archive, User inUser)
	{
		processPushQueue(archive,null,inUser);
	}
	/* (non-Javadoc)
	 * @see org.entermediadb.asset.push.PushManager#processPushQueue(org.entermediadb.asset.MediaArchive, java.lang.String, org.openedit.users.User)
	 */
	public void processPushQueue(MediaArchive archive, String inAssetIds, User inUser)
	{
		
		
		//field=importstatus&importstatus.value=complete&operation=matches&field=pushstatus&pushstatus.value=complete&operation=not&field=pushstatus&pushstatus.value=nogenerated&operation=not&field=pushstatus&
		//pushstatus.value=error&operation=not&field=pushstatus&pushstatus.value=deleted&operation=not
		
		//Searcher hot = archive.getSearcherManager().getSearcher(archive.getCatalogId(), "hotfolder");
		Searcher searcher = archive.getAssetSearcher();
		SearchQuery query = searcher.createSearchQuery();
		if( inAssetIds == null )
		{
			//query.addMatches("category","index");
			query.addMatches("importstatus","complete");
			query.addNot("pushstatus","complete");
			query.addNot("pushstatus","nogenerated");
			query.addNot("pushstatus","error");
			query.addNot("pushstatus","deleted");
			query.addNot("editstatus","7");
		}
		else
		{
			String assetids = inAssetIds.replace(","," ");
			query.addOrsGroup( "id", assetids );
		}
		query.addSortBy("assetmodificationdate");
		HitTracker hits = searcher.search(query);
		hits.setHitsPerPage(1000);
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

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.push.PushManager#processDeletedAssets(org.entermediadb.asset.MediaArchive, org.openedit.users.User)
	 */
	public void processDeletedAssets(MediaArchive archive, User inUser)
	{
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
			Asset asset = archive.getAssetBySourcePath(data.getSourcePath());
			if( asset == null )
			{
				log.error("Reindex assets" + data.getSourcePath() );
				continue;
			}
			
			upload(asset, archive, "delete",  (List<ContentItem>) Collections.EMPTY_LIST );
			asset.setProperty("pushstatus", "deleted");
			archive.saveAsset(asset, null);
			deleted++;
		}
		log.info("Removed " + deleted);
	}


	/* (non-Javadoc)
	 * @see org.entermediadb.asset.push.PushManager#uploadGenerated(org.entermediadb.asset.MediaArchive, org.openedit.users.User, org.entermediadb.asset.Asset, java.util.List)
	 */
	public void uploadGenerated(MediaArchive archive, User inUser, Asset target, List savequeue)
	{
		Searcher searcher = archive.getAssetSearcher();
		

		List<ContentItem> filestosend = new ArrayList<ContentItem>();

		String path = "/WEB-INF/data/" + archive.getCatalogId() + "/generated/" + target.getSourcePath();
		
		readFiles( archive.getPageManager(), path, path, filestosend );
		ContentItem item = archive.getPageManager().getRepository().getStub("/WEB-INF/data/" + archive.getCatalogId() +"/assets/" + target.getSourcePath() + "/fulltext.txt");
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
//		http://demo.entermediasoftware.com
		if (inPreset.get("type") == "original")
		{
			return mediaArchive.getOriginalDocument(asset);

		}
		String input = "/WEB-INF/data/" + mediaArchive.getCatalogId() + "/generated/" + asset.getSourcePath() + "/" + inPreset.get("generatedoutputfile");
		Page inputpage = mediaArchive.getPageManager().getPage(input);
		return inputpage;

	}
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
		return result;
	}
	
	protected Map<String, String> upload(Asset inAsset, MediaArchive inArchive, String inUploadType, List<ContentItem> inFiles)
	{
		String server = inArchive.getCatalogSettingValue("push_server_url");
		//String account = inArchive.getCatalogSettingValue("push_server_username");
		String targetcatalogid = inArchive.getCatalogSettingValue("push_target_catalogid");
		//String password = getUserManager().decryptPassword(getUserManager().getUser(account));

		String url = server + "/media/services/rest/" + "handlesync.xml?catalogid=" + targetcatalogid;
		HttpPost method = new HttpPost(url);
		//Request.setHeader(CoreProtocolPNames.HTTP_CONTENT_CHARSET, Consts.UTF_8);
		//method.setHeader(CoreProtocolPNames.HTTP_CONTENT_CHARSET, Consts.UTF_8);
		//method.setHeader("Content-Type", "multipart/form-data; charset=utf-8");
		
		method.setHeader(HTTP.CONTENT_TYPE,
                "application/x-www-form-urlencoded;charset=UTF-8");
		
		
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
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			
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
				FileBody fileBody = new FileBody(new File( file.getAbsolutePath() ), name, "application/octect-stream","utf-8");
				 
				builder.addPart(type + count, fileBody);
				//FilePart part = new FilePart(type + count, name, new File( file.getAbsolutePath() ));
				count++;
			}
//			parts.add(new BasicNameValuePair("username", account));
//			parts.add(new BasicNameValuePair("password", password));
			for (Iterator iterator = inAsset.keySet().iterator(); iterator.hasNext();)
			{
				String key = (String) iterator.next();
				if( !key.equals("libraries"))  //handled below
				{
					String value = inAsset.get(key);
					if( value != null)
					{
						builder.addPart("field",new StringBody(key, ContentType.TEXT_PLAIN));
						builder.addPart(key+ ".value",new StringBody(value, ContentType.TEXT_PLAIN));
					}
				}
			}
			builder.addPart("field", new StringBody("name",ContentType.TEXT_PLAIN));
			builder.addPart("name.value", new StringBody( inAsset.getName(),ContentType.TEXT_PLAIN));
			builder.addPart("sourcepath", new StringBody( inAsset.getSourcePath(),ContentType.TEXT_PLAIN));
			builder.addPart("uploadtype", new StringBody( inUploadType,ContentType.TEXT_PLAIN));
			builder.addPart("id", new StringBody( prefix + inAsset.getId(),ContentType.TEXT_PLAIN));
			
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
				builder.addPart("keywords",new StringBody(buffer.toString(),ContentType.TEXT_PLAIN));
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
//				builder.add(new StringBody("libraries", buffer.toString() ));
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
	
	public void resetPushStatus(MediaArchive inArchive, String oldStatus,String inNewStatus)
	{
		AssetSearcher assetSearcher = inArchive.getAssetSearcher();
		List savequeue = new ArrayList();
		HitTracker hits = assetSearcher.fieldSearch("pushstatus", oldStatus);
		hits.setHitsPerPage(1000);

		int size = 0;
		while(true)
		{
			size = hits.size();
			for (Iterator iterator = hits.getPageOfHits().iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				Asset asset = inArchive.getAssetBySourcePath(data.getSourcePath());
				if( asset == null )
				{
					log.error("Missing asset" + data.getSourcePath());
					continue;
				}
				asset.setProperty("pushstatus", inNewStatus);
				savequeue.add(asset);
				if( savequeue.size() == 1000 )
				{
					assetSearcher.saveAllData(savequeue, null);
					savequeue.clear();
				}
			}
			assetSearcher.saveAllData(savequeue, null);
			savequeue.clear();
			hits = assetSearcher.fieldSearch("pushstatus", oldStatus);
			hits.setHitsPerPage(1000);
			log.info(hits.size() + " remaining status updates " + oldStatus );
			if( hits.size() == 0 || size > hits.size() )
			{
				break;
			}
		} 
		
		
	}
	
	/* (non-Javadoc)
	 * @see org.entermediadb.asset.push.PushManager#getCompletedAssets(org.entermediadb.asset.MediaArchive)
	 */
	public Collection getCompletedAssets(MediaArchive inArchive)
	{
		HitTracker hits = inArchive.getAssetSearcher().fieldSearch("pushstatus", "complete");
		return hits;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.push.PushManager#getPendingAssets(org.entermediadb.asset.MediaArchive)
	 */
	public Collection getPendingAssets(MediaArchive inArchive)
	{
		SearchQuery query = inArchive.getAssetSearcher().createSearchQuery();
		query.addMatches("importstatus","complete");
		query.addNot("pushstatus","complete");
		query.addNot("pushstatus","nogenerated");
		query.addNot("pushstatus","error");
		query.addNot("pushstatus","deleted");
		query.addNot("editstatus","7");


		HitTracker hits = inArchive.getAssetSearcher().search(query);
		return hits;
	}


	/* (non-Javadoc)
	 * @see org.entermediadb.asset.push.PushManager#getNoGenerated(org.entermediadb.asset.MediaArchive)
	 */
	public Collection getNoGenerated(MediaArchive inArchive)
	{
		HitTracker hits = inArchive.getAssetSearcher().fieldSearch("pushstatus", "nogenerated");
		return hits;
	}



	/* (non-Javadoc)
	 * @see org.entermediadb.asset.push.PushManager#getErrorAssets(org.entermediadb.asset.MediaArchive)
	 */
	public Collection getErrorAssets(MediaArchive inArchive)
	{
		HitTracker hits = inArchive.getAssetSearcher().fieldSearch("pushstatus", "error");
		return hits;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.push.PushManager#getImportCompleteAssets(org.entermediadb.asset.MediaArchive)
	 */
	public Collection getImportCompleteAssets(MediaArchive inArchive)
	{
		SearchQuery query = inArchive.getAssetSearcher().createSearchQuery();
		//query.addMatches("category","index");
		query.addMatches("importstatus","complete");
		query.addNot("editstatus","7");

		//Push them and mark them as pushstatus deleted
		HitTracker hits = inArchive.getAssetSearcher().search(query);
		return hits;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.push.PushManager#getImportPendingAssets(org.entermediadb.asset.MediaArchive)
	 */
	public Collection getImportPendingAssets(MediaArchive inArchive)
	{
		SearchQuery query = inArchive.getAssetSearcher().createSearchQuery();
		query.addMatches("importstatus","imported");
		query.addNot("editstatus","7");

		//Push them and mark them as pushstatus deleted
		HitTracker hits = inArchive.getAssetSearcher().search(query);
		return hits;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.push.PushManager#getImportErrorAssets(org.entermediadb.asset.MediaArchive)
	 */
	public Collection getImportErrorAssets(MediaArchive inArchive)
	{
		HitTracker hits = inArchive.getAssetSearcher().fieldSearch("importstatus", "error");
		return hits;
	}


	/* (non-Javadoc)
	 * @see org.entermediadb.asset.push.PushManager#pushAssets(org.entermediadb.asset.MediaArchive, java.util.List)
	 */
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


	/* (non-Javadoc)
	 * @see org.entermediadb.asset.push.PushManager#pollRemotePublish(org.entermediadb.asset.MediaArchive)
	 */
	public void pollRemotePublish(MediaArchive inArchive)
	{
		
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
		Collection hits = dests.fieldSearch("remotepublish","true");
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
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();

		builder.addPart("field", new StringBody("publishdestination", ContentType.TEXT_PLAIN));
		//builder.addPart(new BasicNameValuePair(("publishdestination.value", "pushhttp");
		builder.addPart("publishdestination.value", new StringBody(ors.toString(), ContentType.TEXT_PLAIN));
		builder.addPart("operation", new StringBody("orsgroup", ContentType.TEXT_PLAIN));

		builder.addPart("field", new StringBody("status", ContentType.TEXT_PLAIN));
		builder.addPart("status.value", new StringBody("complete", ContentType.TEXT_PLAIN));
		builder.addPart("operation", new StringBody("not", ContentType.TEXT_PLAIN));

		builder.addPart("field", new StringBody("status", ContentType.TEXT_PLAIN));
		builder.addPart("status.value", new StringBody("error", ContentType.TEXT_PLAIN));
		builder.addPart("operation", new StringBody("not", ContentType.TEXT_PLAIN));

		method.setEntity(builder.build());

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


	protected void runRemotePublish(MediaArchive inArchive, String server, String targetcatalogid, Element hit) throws Exception
	{
		String sourcepath = hit.attributeValue("assetsourcepath");
		Asset asset = inArchive.getAssetBySourcePath(sourcepath);
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

			Page inputpage = null;
			String type = null;
			if( !"original".equals(preset.get("transcoderid")))
			{
				String input= "/WEB-INF/data/" + inArchive.getCatalogId() +  "/generated/" + asset.getSourcePath() + "/" + preset.get("generatedoutputfile");
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

				filestosend.add(inputpage.getContentItem());

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
			//Why is this not being passed back to us?
			if( exportpath == null )
			{
				exportpath = inArchive.asExportFileName(inAsset, preset);
			}
			publishqeuerow.setProperty("exportname", exportpath);
			publishqeuerow.setSourcePath(inAsset.getSourcePath());
			publishqeuerow.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
			publishQueueSearcher.saveData(publishqeuerow, null);
		}
		inArchive.fireMediaEvent("publishing/publishasset", null, inAsset);
		
		publishqeuerow =  (Data)publishQueueSearcher.searchById("remote" + publishqueueid);
		return publishqeuerow;
	}


	/* (non-Javadoc)
	 * @see org.entermediadb.asset.push.PushManager#toggle(java.lang.String)
	 */
	public void toggle(String inCatalogId)
	{
		perThreadCache = new ThreadLocal();
	}
	
	@Override
	public void acceptPush(WebPageRequest inReq, MediaArchive archive)
	{
		FileUpload command = new FileUpload();
		command.setPageManager(archive.getPageManager());
		UploadRequest properties = command.parseArguments(inReq);

		String sourcepath = inReq.getRequestParameter("sourcepath");
		
		Asset target = archive.getAssetBySourcePath(sourcepath);
		if (target == null)
		{
			String id = inReq.getRequestParameter("id");
			target = (Asset) archive.getAssetSearcher().createNewData();
			target.setId(id);
			target.setSourcePath(sourcepath);
		}
		
//		String name = inReq.getRequestParameter("name");
//		if( name != null)
//		{
//			target.setName(name);
//		}
		
//		String categories = inReq.getRequestParameter("categories");
//		String[] vals = categories.split(";");
//		archive.c
//		target.setCategories(cats);
		String categorypath = PathUtilities.extractDirectoryPath(sourcepath);
		Category category = archive.getCategoryArchive().createCategoryTree(categorypath);
		target.addCategory(category);
		
		String[] fields = inReq.getRequestParameters("field");
		
		//Make sure we ADD libraries not replace them
		String editstatus = inReq.getRequestParameter("editstatus.value");
		String k4processed = inReq.getRequestParameter("k4processed.value");
		
		
		if( k4processed == "true" || editstatus == "override" || editstatus == "7") 
		{
			archive.getAssetSearcher().updateData(inReq, fields, target);
		}
		else
		{
			archive.getAssetSearcher().updateData(inReq, fields, new ImmutableData(target));
		}
		String keywords = inReq.getRequestParameter("keywords");
		if( keywords != null )
		{
			String[] keys =  keywords.split("\\|");
			for (int i = 0; i < keys.length; i++)
			{
				target.addKeyword(keys[i]);
			}
		}
		
		List<FileUploadItem> uploadFiles = properties.getUploadItems();


		String	generatedsaveroot = "/WEB-INF/data/" + archive.getCatalogId() + "/generated/" + sourcepath;
		String	originalsaveroot = "/WEB-INF/data/" + archive.getCatalogId() + "/originals/" + sourcepath;
		
		//String originalsroot = "/WEB-INF/data/" + archive.getCatalogId() + "/originals/" + sourcepath + "/";

		if (uploadFiles != null)
		{
			archive.removeGeneratedImages(target, true);
			Iterator<FileUploadItem> iter = uploadFiles.iterator();
			while (iter.hasNext())
			{
				FileUploadItem fileItem = iter.next();
				String inputName = fileItem.getFieldName();
				if( inputName.startsWith("original") )
				{
//					if( target.isFolder())
//					{
//						properties.saveFileAs(fileItem, originalsaveroot + "/" + target.getMediaName(), inReq.getUser());
//					}
//					else
//					{
						properties.saveFileAs(fileItem, originalsaveroot, inReq.getUser());
//					}
				}
				else if( fileItem.getName().equals( "fulltext.txt"))
				{
					properties.saveFileAs(fileItem, "/WEB-INF/data/" + archive.getCatalogId() + "/assets/" + sourcepath + "/fulltext.txt", inReq.getUser());
				}
				else
				{
					properties.saveFileAs(fileItem, generatedsaveroot + "/" + fileItem.getName(), inReq.getUser());
				}
			}
		}
		archive.saveAsset(target, inReq.getUser());
		archive.fireMediaEvent("importing/pushassetimported", inReq.getUser(), target);

	}
	/*
	public void pullApprovedAssets(WebPageRequest inReq, MediaArchive inArchive){
		log.info("pulling approved assets from remote server");
		Map<String,Properties> map = getApprovedAssets(inArchive);
		log.info("found the following files, $map");
		if (!map.isEmpty()){
			processApprovedAssets(inArchive,map);
			log.info("finished pull");
		} else{
			log.info("no files approved on remote server, returning");
		}
	}
	
	 * Gets the approved assets (that are not marked for deletion) from remote server
	 * @param inArchive
	 * @return
	protected HashMap<String,Properties> getApprovedAssets(MediaArchive inArchive) {
		log.info("getApprovedAssets starting");
		String server = inArchive.getCatalogSettingValue("push_server_url");
		String remotecatalogid = inArchive.getCatalogSettingValue("push_target_catalogid");
		log.info("push_server_url = $server, push_target_catalogid = $remotecatalogid");
		String [] inFields = {"approvalstatus", "editstatus"};
		String [] inValues = {"approved", "7"};
		String [] inOperations = {"matches", "not"};

		String url = server + "/media/services/rest/assetsearch.xml";
		HttpPost method = new HttpPost(url);
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		
		builder.addPart("catalogid", new StringBody("remotecatalogid", ContentType.TEXT_PLAIN));

		for(int i=0; i<inFields.length; i++){
			builder.addPart("field", new StringBody("inFields[i]", ContentType.TEXT_PLAIN));
			builder.addPart("operation", new StringBody(inOperations[i], ContentType.TEXT_PLAIN));
			builder.addPart(inFields[i] + ".value", new StringBody(inValues[i], ContentType.TEXT_PLAIN));
		}
		log.info("executing $remotecatalogid, $method");
		method.setEntity(builder.build());
		Element root = execute(remotecatalogid,method);
		method.releaseConnection();
		Element hits = (Element)root.elements().get(0);
		
		int pages = Integer.parseInt(hits.attributeValue("pages"));
		String sessionid = hits.attributeValue("sessionid");
		
		log.info("found $pages, $sessionid, $root");
		Map<String, Properties> map = new HashMap<String, Properties>();
		addHits(hits, map);
		
		url = server + "/media/services/rest/getpage.xml";
		for( int i = 2; i <= pages; i++ )
		{
			method = new HttpPost(url);
			builder = MultipartEntityBuilder.create();

			builder.addPart("catalogid", new StringBody(remotecatalogid, ContentType.TEXT_PLAIN));
			builder.addPart("hitssessionid", new StringBody(sessionid, ContentType.TEXT_PLAIN));
			builder.addPart("page", new StringBody(String.valueOf(i), ContentType.TEXT_PLAIN));
			root = execute(remotecatalogid,method);
			method.releaseConnection();
			hits = (Element)root.elements().get(0);
			addHits(hits, map);
		}
		return (HashMap<String,Properties>) map;
	}
	
	protected void addHits(Element inHits, Map<String, Properties> inResults){
		Iterator<?> hits = inHits.elements("hit").iterator();
		while (hits.hasNext()){
			Element e = (Element) hits.next();
			Properties props = new Properties();
			Iterator<Attribute> attributes = e.attributeIterator();
			while(attributes.hasNext()){
				Attribute attr = attributes.next();
				String n = attr.getName();
				String v = attr.getValue();
				if (n.equalsIgnoreCase("id")){
					inResults.put(v, props);
				} else {
					props.put(n,v);
				}
			}
			Iterator<Element> elements = e.elementIterator();
			while(elements.hasNext()){
				Element element = elements.next();
				String n = element.getName();
				String v = element.getText();
				props.put(n,v);
			}
		}
	}
	
	protected void processApprovedAssets(MediaArchive inArchive, Map<String,Properties> inMap){
		String catalogid = inArchive.getCatalogId();
		String server = inArchive.getCatalogSettingValue("push_server_url");
		String remotecatalogid = inArchive.getCatalogSettingValue("push_target_catalogid");
		String exportpath = inArchive.getCatalogSettingValue("push_download_exportpath");
		if (exportpath == null){
			exportpath = "/WEB-INF/data/${catalogid}/originals/";
		} else if (exportpath.startsWith("/")){
			exportpath = "/WEB-INF/data/${catalogid}/originals${exportpath}";
		} else {
			exportpath = "/WEB-INF/data/${catalogid}/originals/${exportpath}";
		}
		if (!exportpath.endsWith("/")){
			exportpath = "${exportpath}/";
		}
		Iterator<String> itr = inMap.keySet().iterator();
		while(itr.hasNext()){
			String key = itr.next();
			Properties prop = inMap.get(key);
			//1. query REST for metadata of particular asset
			Properties metadata = getAssetMetadata(inArchive,key);
			//2. download original to a specific location
			String url = prop.getProperty("original");
			String name = prop.getProperty("name");
			if (url == null || name == null){
				log.info("unable to process $key, name ($name) or url ($url) are null, skipping");
				continue;
			}
			Page page = getDownloadedAsset(inArchive,url,name,exportpath);
			if (!page.exists()){
				log.info("unable to download asset $name, skipping");
				continue;
			}
			//3. update sourcepath
			page = moveDownloadedAsset(inArchive,page,metadata);
			//4. copy metadata to new asset
			Asset asset = null;
			if ( (asset = createAsset(inArchive,page,metadata)) == null){
				log.info("unable to create asset skipping changing asset status to deleted");
				continue;
			}
			//5. query REST to set delete status of asset
			updateAssetEditStatus(inArchive,key);
			//6. fire event
			inArchive.fireMediaEvent("asset/finalizepull",null,asset);
		}
	}
	
	protected Page getDownloadedAsset(MediaArchive inArchive, String inUrl, String inName, String inExportPath){
		String server = inArchive.getCatalogSettingValue("push_server_url");
		String incomingPath = "${inExportPath}${inName}";
		Page page = inArchive.getPageManager().getPage(incomingPath);
		File fileOut = new File(page.getContentItem().getAbsolutePath());
		getDownloader().download(server+inUrl,fileOut);
		return page;
	}
	
	protected Properties getAssetMetadata(MediaArchive inArchive, String inAssetId){
		log.info("get asset metadata for $inAssetId");
		String server = inArchive.getCatalogSettingValue("push_server_url");
		String remotecatalogid = inArchive.getCatalogSettingValue("push_target_catalogid");
		String url = server + "/media/services/rest/assetdetails.xml";
		HttpPost method = new HttpPost(url);
		builder.addPart(new BasicNameValuePair(("catalogid", remotecatalogid);
		method.add(new BasicNameValuePair(("id", inAssetId);
		Element root = execute(remotecatalogid,method);
		method.releaseConnection();
		Properties props = new Properties();
		Iterator<Element> itr = root.elementIterator();
		while(itr.hasNext()){
			Element e = itr.next();
			if (e.getName()==null || !e.getName().equals("property") || e.attribute("id")==null || e.attribute("id").getValue().isEmpty()){
//				log.info("skipping ${inAssetId}: ${e}");
				continue;
			}
			String id = e.attribute("id").getValue();
			String valueid = e.attribute("valueid")!=null ? e.attribute("valueid").getValue() : null;
			String text = e.getText();
			if (valueid!=null && !valueid.isEmpty()){
				props.put(id,valueid);
			} else {
				props.put(id,text);
			}
		}
		return props;
	}
	
	protected Asset createAsset(MediaArchive inArchive, Page inPage, Properties inMetadata){
		AssetImporter importer = (AssetImporter) inArchive.getModuleManager().getBean("assetImporter");
		String catalogid = inArchive.getCatalogId();
		String exportpath = "/WEB-INF/data/" + catalogid + "/originals/";
		String path = inPage.getPath();
		int index;
		if ( (index = path.toLowerCase().indexOf(exportpath.toLowerCase())) !=-1 ){
			path = path.substring(index + exportpath.length());
		}
		Asset asset = (Asset) importer.createAssetFromExistingFile(inArchive,null,false,path);
		if (asset == null){
			log.info("unable to create asset, aborting");
			return null;
		}
		log.info("created $asset: ${asset.getId()}");
		Enumeration<?> keys = inMetadata.keys();
		while (keys.hasMoreElements()){
			String key = keys.nextElement().toString();
			String value = inMetadata.getProperty(key);
			asset.setProperty(key, value);
		}
		importer.saveAsset(inArchive, null, asset); //TODO: check if zip functionality works
		return asset;
	}
	
	protected Page moveDownloadedAsset(MediaArchive inArchive, Page inPage, Properties inMetadata){
		PropertyDetails props = inArchive.getAssetSearcher().getPropertyDetails();
		StringBuilder buf = new StringBuilder();
		//parser pattern specified in download sourcepath; look for keys in metadata and match fields in asset property definition
		//to determin datatype; otherwise just use exact string provided
		String pattern = inArchive.getCatalogSettingValue("push_download_sourcepath");
		if (pattern!=null && !pattern.isEmpty()){
            List<String> tokens = findKeys(pattern,"//");
            for (Iterator iterator = tokens.iterator(); iterator.hasNext();) {
				String token = (String) iterator.next();
                if (token.startsWith("\\$"))
                {	//metadata field
					//get field, parameter and value from metadata map
                    String field = token.substring(1);
                    String param = null;
                    int start = -1;
                    int end = -1;
                    if ( (start = field.indexOf("{"))!=-1 && (end = field.indexOf("}"))!=-1 && start < end){
                        param = field.substring(start+1,end).trim();//eg, YYYY, MM for dates
                        field = field.substring(0,start).trim();//eg, $owner, $assetcreationdate
                    }
					String value = inMetadata.getProperty(field,"").trim();
					//check if it's a date
					boolean isDate = false;
					if (props.contains(field)){
						PropertyDetail prop = props.getDetail(field);
						if (prop.isDate()){
							isDate = true;
						} else if (param!=null){//check if it's formatted as a date because of provided param
							//if this succeeds then we know there's a difference in configs between client and server
							isDate = DateStorageUtil.getStorageUtil().parseFromStorage(value) != null;
						}
					}
					if (value!=null && !value.isEmpty()){
						if (isDate){
							String cleaned = DateStorageUtil.getStorageUtil().checkFormat(value);
							Date date = DateStorageUtil.getStorageUtil().parseFromStorage(value);
							if (date == null){
								date = new Date();
								log.info("unable to parse Date value from remote server: field = $field, value = $value, defaulting to NOW");
							}
							String formatted = null;
							try{
								SimpleDateFormat format = new SimpleDateFormat(param.trim());
								formatted = format.format(date);
							}catch (Exception e){
								log.info("exception caught parsing $date using format \"${param.trim()}\", ${e.getMessage()}, defaulting to $value");
							}
							if (formatted!=null && !formatted.isEmpty()){
								buf.append(formatted).append("/");
							} else {
								buf.append(value).append("/");
							}
						} else {
							buf.append(value).append("/");
						}
					} else {
						log.info("skipping $field, unable to find in metadata obtained from server");
					}
                } else {
					buf.append(token.trim()).append("/");
                }
            }
			if (!buf.toString().isEmpty()){
				buf.append("${inPage.getName()}");
			}
        }
		if (buf.toString().isEmpty()){
			String user = inMetadata.getProperty("owner","admin").trim();//make the default "admin" if owner has not been specified
			Calendar cal = Calendar.getInstance();
			String month = String.valueOf(cal.get(Calendar.MONTH)+1);
			if (month.length() == 1)
			{
				month = "0${month}";
			}
			String day = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
			if (day.length() == 1)
			{
				day = "0${day}";
			}
			String year = String.valueOf(cal.get(Calendar.YEAR));
			buf.append("users/${user}/${year}/${month}/${day}/${inPage.getName()}");
		}
		String generatedpath = buf.toString();
		log.info("moving ${inPage.getName()} to generated path \"$generatedpath\"");
		String destinationpath = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/$generatedpath";
		Page destinationpage = inArchive.getPageManager().getPage(destinationpath);
		inArchive.getPageManager().movePage(inPage,destinationpage);
		return destinationpage;
	}
	
	protected ArrayList<String> findKeys(String Subject, String Delimiters)
	{
		StringTokenizer tok = new StringTokenizer(Subject, Delimiters);
		ArrayList<String> list = new ArrayList<String>(Subject.length());
		while(tok.hasMoreTokens()){
			list.add(tok.nextToken());
		}
		return list;
	}
	
	protected void updateAssetEditStatus(MediaArchive inArchive, String inAssetId){
		String server = inArchive.getCatalogSettingValue("push_server_url");
		String remotecatalogid = inArchive.getCatalogSettingValue("push_target_catalogid");
		String url = server + "/media/services/rest/saveassetdetails.xml";
		HttpPost method = new HttpPost(url);
		method.add(new BasicNameValuePair(("catalogid", remotecatalogid);
		method.add(new BasicNameValuePair(("id", inAssetId);
		method.add(new BasicNameValuePair(("field", "editstatus");
		method.add(new BasicNameValuePair(("editstatus.value", "7");
		Element root = execute(remotecatalogid,method);
		method.releaseConnection();
		String out = root.attributeValue("stat");
		if (!"ok".equalsIgnoreCase(out)){
			log.info("warning, could not update $inAssetId editstatus!!!");
		}
	}
	*/
}
