package org.entermediadb.asset.pull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.elasticsearch.ElasticNodeManager;
import org.entermediadb.elasticsearch.SearchHitData;
import org.entermediadb.net.HttpSharedConnection;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.locks.Lock;
import org.openedit.repository.ContentItem;
import org.openedit.repository.filesystem.FileItem;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.FileUtils;
import org.openedit.util.OutputFiller;
import org.openedit.util.PathUtilities;
import org.openedit.util.URLUtilities;

public class OriginalPuller extends BasePuller implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(OriginalPuller.class);

	public ContentItem downloadOriginal(MediaArchive inArchive, Asset inAsset, File inFile, boolean ifneeded)
	{
		Data node = loadMasterDataForAsset(inArchive, inAsset);
		if( node == null)
		{
			return null;
		}
		HttpSharedConnection connection = createConnection(node);
		
		ContentItem item = downloadOriginal(inArchive,connection,node,inAsset,inFile,ifneeded);
		return item;

	}

	public ContentItem downloadOriginal(MediaArchive inArchive, HttpSharedConnection connection, Data node, Asset inAsset, File inFile, boolean ifneeded)
	{

		String url = node.get("baseurl");
		inFile.getParentFile().mkdirs();
		FileItem item = new FileItem(inFile);

		String path = "/WEB-INF/data" + inArchive.getCatalogHome() + "/originals/";
		path = path + inAsset.getSourcePath(); //Check archived?

		String primaryname = inAsset.getPrimaryFile();
		if (primaryname != null && inAsset.isFolder())
		{
			path = path + "/" + primaryname;
		}
		item.setPath(path);
		if (ifneeded)
		{
			//Check it exists and it matches
			long size = inAsset.getLong("filesize");
			if (item.getLength() != size)
			{
				String finalurl = url + "/mediadb/services/module/asset/downloads/originals/" + URLUtilities.encode(inArchive.asLinkToOriginal(inAsset));
				Map params = new HashMap();
				finalurl = finalurl.concat("?entermedia.key=" + node.get("entermediakey"));

				HttpResponse genfile = connection.sharedPost(finalurl, params);
				StatusLine filestatus = genfile.getStatusLine();
				if (filestatus.getStatusCode() != 200)
				{
					log.error("Could not download original " + filestatus + " " + path + "Full URL: " + finalurl);
					return null;
				}

				//Save to local file
				try
				{
					log.info("Saving :" + inAsset.getSourcePath() + "/" + inAsset.getName() + " URL:" + path);
					InputStream stream = genfile.getEntity().getContent();

					inFile.getParentFile().mkdirs();
					FileOutputStream fos = new FileOutputStream(inFile);
					filler.fill(stream, fos);
					filler.close(stream);
					filler.close(fos);
					//inFile.setLastModified(datetime);
				}
				catch (Exception e)
				{
					// TODO Auto-generated catch block
					throw new OpenEditException(e);
				}

			}
		}

		return item;

	}
	public InputStream getOriginalDocumentStream(MediaArchive inArchive, Asset inAsset)
	{
		Data masterData = loadMasterDataForAsset(inArchive, inAsset);
		if( masterData == null)
		{
			return null;
		}
		HttpSharedConnection connection = createConnection(masterData);
		InputStream stream = getOriginalDocumentStream(inArchive, connection,masterData, inAsset);
		return stream;

	}
	protected InputStream getOriginalDocumentStream(MediaArchive inArchive, HttpSharedConnection connection, Data inMasterData, Asset inAsset)
	{
		try
		{
			String url = inMasterData.get("baseurl");
			String finalurl = url + URLUtilities.encode(inArchive.asLinkToOriginal(inAsset));
			Map params = new HashMap();
			HttpResponse genfile = connection.sharedPost(finalurl, params);
			return genfile.getEntity().getContent();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			throw new OpenEditException(e);
		}

	}

	public void pull(MediaArchive inArchive, ScriptLogger inLog)
	{

		Lock lock = inArchive.getLockManager().lockIfPossible("processPull", this.getClass().getCanonicalName() );
		if (lock == null)
		{
			log.info("Pull is already running");
			inLog.info("Pull is already running");
			return;
		}
		try
		{
			pullOriginalsQueue(inArchive, inLog);
		}
		finally
		{
			inArchive.releaseLock(lock);
		}

	}

	//Send in pages
	
	protected void pullOriginalsQueue(MediaArchive inArchive, ScriptLogger inLog)
	{

		Collection nodes = getNodeManager().getRemoteEditClusters(inArchive.getCatalogId());
		Data node = null;
		for (Iterator iterator = nodes.iterator(); iterator.hasNext();)
		{
			try
			{
				node = (Data) iterator.next();

				if (node.get("entermediakey") == null)
				{
					log.error("entermediakey is required");
					continue;
				}
				String url = node.get("baseurl");
				if (url == null || !Boolean.parseBoolean( node.get("enabled") ) )
				{
					log.error(node + " not enabled " + node);
					continue;
				}
				Date now = new Date();
				
				Object dateob = node.getValue("lastpulldateoriginals");
				Date pulldate = null;

				if (dateob instanceof String)
				{
					pulldate = DateStorageUtil.getStorageUtil().parseFromStorage((String) dateob);
				}
				else
				{
					pulldate = (Date) dateob;
				}
				if (dateob == null)
				{
					pulldate = DateStorageUtil.getStorageUtil().substractDaysToDate(new Date(), 7);
				}

				if (pulldate.getTime() + (1000L * 20L) > System.currentTimeMillis())
				{
					inLog.info(node.getName() + " Orignals pulled within 20 seconds. Trying again later");
					continue;
				}
				node.setValue("lasterrordateoriginals",null);
				node.setValue("lasterrormessageoriginals", null);

				long ago = now.getTime() - pulldate.getTime();
				Map<String,String> params = new HashMap();
				params.put("lastpullago", String.valueOf(ago));
				params.put("sincedate", DateStorageUtil.getStorageUtil().formatForStorage(pulldate));
				inLog.info(node.getName() + " checking originals since " + pulldate);

				HttpSharedConnection connection = createConnection(node);

				if( Boolean.parseBoolean( node.get("pulloriginals") ) )
				{
					long totalcount = downloadOriginals(inArchive, connection, node,params,inLog);
					
					if( node.getValue("lasterrormessageoriginals") != null )
					{
						inLog.info(node.getName() + " originals download error " + node.getValue("lasterrormessageoriginals"));
						continue;
					}
					else
					{
						inLog.info(node.getName() + " originals downloaded: " + totalcount );
					}
				}
				
				//Upload origianls
				if( Boolean.parseBoolean( node.get("pushoriginals") ) )
				{
						params.remove("hitssessionid");
						params.remove("page");
						
						syncUpLocalOriginals(inArchive,connection,node,pulldate,params,inLog);
						
						if( node.getValue("lasterrormessageoriginals") != null )
						{
							inLog.info(node.getName() + " error " + node.getValue("lasterrormessageoriginals"));
							continue;
						}
				}
				//save the date
				node.setValue("lastpulldateoriginals", now);
				getSearcherManager().getSearcher(inArchive.getCatalogId(), "editingcluster").saveData(node);

			}
			catch (Throwable ex)
			{
				log.error("Could not originals process sync files ", ex);
				inLog.error("Could not originals process sync files " + ex);
				if (node != null)
				{
					node.setValue("lasterrormessageoriginals", "Could not process originals files " + ex);
					node.setValue("lasterrordateoriginals", new Date());
					getSearcherManager().getSearcher(inArchive.getCatalogId(), "editingcluster").saveData(node);
				}
			}

		}
	}


	protected long downloadOriginals(MediaArchive inArchive, HttpSharedConnection connection, Data node, Map<String,String> params, ScriptLogger inLog)
	{
		String baseurl = node.get("baseurl");
		String url = baseurl + "/mediadb/services/cluster/pullrecentuploads.json";
		StringBuffer debugurl = new StringBuffer();
		debugurl.append("?");
		debugurl.append("lastpullago=");
		String last = params.get("lastpullago");

		if (params.get("lastpullago") != null)
		{
			debugurl.append(last);
		}

		String encoded = url + debugurl;
		log.info("Checking Originals: " + URLUtilities.urlEscape(encoded));
		
		CloseableHttpResponse response2 = connection.sharedPost(url, params);
		
		StatusLine sl = response2.getStatusLine();
		if (sl.getStatusCode() != 200)
		{
			node.setValue("lasterrormessageoriginals", "Could not list originals " + sl.getStatusCode() + " " + sl.getReasonPhrase());
			node.setValue("lasterrordateoriginals", new Date());
			getSearcherManager().getSearcher(inArchive.getCatalogId(), "editingcluster").saveData(node);
			log.error("Initial originals server error " + sl);
			return -1;
		}
		JSONObject	remotechanges = connection.parseJson(response2);
		long counted = 0;
		Map response = (Map) remotechanges.get("response");
		String ok = (String) response.get("status");
		if (ok != null && ok.equals("ok"))
		{
			
			String removecatalogid = (String)response.get("catalogid");
			
			JSONArray jsonarray = (JSONArray) remotechanges.get("results");

			counted = counted + downloadOriginalFiles(inArchive, connection, node,  params,removecatalogid,jsonarray);

			int pages = Integer.parseInt(response.get("pages").toString());
			//loop over pages
			String hitssessionid = (String) response.get("hitssessionid");
			params.put("hitssessionid", hitssessionid);
			for (int page = 2; page <= pages; page++)
			{
				url = baseurl + "/mediadb/services/cluster/nextalloriginals.json";

				params.put("page", String.valueOf(page));
				log.info("next page: " + encoded + "&page=" + page + "&hitssessionid=" + hitssessionid);
				response2 = connection.sharedPost(url, params);
				
				sl = response2.getStatusLine();
				if (sl.getStatusCode() != 200)
				{
					connection.release(response2);
					throw new OpenEditException("Could not load page of data " + sl.getStatusCode() + " " + sl.getReasonPhrase());
				}
				remotechanges = connection.parseJson(response2);
				response = (Map) remotechanges.get("response");
				ok = (String) response.get("status");
				if (ok != null && !ok.equals("ok"))
				{
					throw new OpenEditException("Page could not be loaded " + remotechanges.toJSONString());
				}

				//JSONArray results = (JSONArray)remotechanges.get("results"); //records?
				
				counted = counted + downloadOriginalFiles(inArchive, connection, node, params,removecatalogid,jsonarray);
			}
			return counted;
		}
		else if (ok.equals("empty"))
		{
			log.info("No changes found");
			return 0;
		}
		else
		{
			throw new OpenEditException("Initial data could not be loaded " +  remotechanges.toJSONString());
		}
	}

	protected int downloadOriginalFiles(MediaArchive inArchive, HttpSharedConnection inConnection, Data node, Map<String,String> params, String removecatalogid, JSONArray inJsonarray)
	{
		int downloads = 0;
		String url = node.get("baseurl");
		try
		{
			for (Iterator iterator2 = inJsonarray.iterator(); iterator2.hasNext();)
			{
				Map filelisting = (Map) iterator2.next();
				//List generated media and compare it
								
				//Compare timestamps
				//String lastmodified = (String) filelisting.get("lastmodified");
				long longedited = Long.parseLong((String)filelisting.get("lastmodified"));
				String originalpath = (String) filelisting.get("path");
				
				String savepath  = originalpath.replace(removecatalogid, inArchive.getCatalogId());
				ContentItem found = inArchive.getContent(savepath);

				if (!found.exists() || !FileUtils.isSameDate(found.getLastModified(), longedited))
				{
					//log.info("Found change: " + found.getLastModified() + " !=" + longedited + " on " + found.getAbsolutePath());
					//http://em9dev.entermediadb.org/openinstitute/mediadb/services/module/asset/downloads/preset/Collections/Cincinnati%20-%20Flying%20Pigs/Flying%20Pig%20Marathon/Business%20Pig.jpg/image1024x768.jpg?cache=false
					//String fullURL = url + "/mediadb/services/module/asset/downloads/generated/" + sourcepath + "/" + filename + "/" + filename;
					String sourcepath = (String) filelisting.get("sourcepath");
					
					//TODO: This does not seem right for folder based assets 
					String path = url + URLUtilities.urlEscape("/mediadb/services/module/asset/downloads/originals/" + sourcepath + "/" + found.getName());
					HttpResponse genfile = inConnection.sharedGet(path);
					StatusLine filestatus = genfile.getStatusLine();
					if (filestatus.getStatusCode() != 200)
					{
						log.error("Could not download generated " + filestatus + " " + path);
						throw new OpenEditException("Could not download generated " + filestatus + " " + path);
					}
					downloads++;
					//Save to local file
					log.info("Saving :" + found.getAbsolutePath());
					InputStream stream = genfile.getEntity().getContent();
					//Change the timestamp to match
					File tosave = new File(found.getAbsolutePath());
					tosave.getParentFile().mkdirs();
					FileOutputStream fos = new FileOutputStream(tosave);
					filler.fill(stream, fos);
					filler.close(stream);
					filler.close(fos);
					tosave.setLastModified(longedited);
				}
			}
		}
		catch (Exception ex)
		{
			log.error("Could not download files " + url, ex);
			if (ex instanceof OpenEditException)
			{
				throw (OpenEditException) ex;
			}
			throw new OpenEditException(ex);
		}
		return downloads;
	}
	
	protected void syncUpLocalOriginals(MediaArchive inArchive, HttpSharedConnection inConnection, Data inRemoteNode, Date inPulldate, Map<String, String> inParams, ScriptLogger inLog)
	{
		//Post a list of local orginals (less ones I have that belong to the server)

		String mastereditid = inArchive.getNodeManager().getLocalClusterId();
		//Search for uploads
		
		//Only upload our own files. Because otherwise it might try and download stuff
		
		HitTracker recentuploads = inArchive.query("asset").after("assetmodificationdate", inPulldate).
				exact("emrecordstatus.mastereditclusterid", mastereditid).not("emrecordstatus.deleted", "true").sort("sourcepath").search();
		
		recentuploads.enableBulkOperations();
		recentuploads.setHitsPerPage(100);  //No timeouts
		
		log.info("Uploading " + recentuploads.size() + " locally edited files");
		if (recentuploads.isEmpty())
		{
			return;
		}

		try
		{
			JSONObject finaldata = new JSONObject();

			JSONObject response = new JSONObject();
			response.put("status", "ok");
			response.put("totalhits", recentuploads.size());
			response.put("hitsperpage", recentuploads.getHitsPerPage());
			response.put("pages", recentuploads.getTotalPages());
			response.put("catalogid", inArchive.getCatalogId());
			
			//TODO: On remote server use the ids to find local assets
			response.put("sincedate", DateStorageUtil.getStorageUtil().formatForStorage(inPulldate));
			finaldata.put("response", response);
			
			for (int i = 0; i < recentuploads.getTotalPages(); i++)
			{
				recentuploads.setPage(i+1); //1 based
				response.put("page", recentuploads.getPage());

				JSONArray results = new JSONArray();
				Collection apage = recentuploads.getPageOfHits();
				for (Iterator iterator2 = apage.iterator(); iterator2.hasNext();)
				{
					Data data = (Data) iterator2.next();
					Asset asset = inArchive.getAsset(data.getId());
					JSONObject details = new JSONObject();
					
					details.put("id",asset.getId());
					details.put("sourcepath",asset.getSourcePath());
					details.put("isfolder",asset.isFolder());
					if( asset.isFolder() )
					{
						details.put("parentsourcepath",asset.getPath());
					}
					else
					{
						String parentsourcepath = PathUtilities.extractDirectoryPath(asset.getPath());
						details.put("parentsourcepath",asset.getPath());
					}

					//Make an array of original files
					JSONArray array = new JSONArray();	
					details.put("files",array);
					if( asset.isFolder() )
					{
						Collection items = inArchive.listOriginalFiles(asset.getPath());
						for (Iterator iterator = items.iterator(); iterator.hasNext();)
						{
							ContentItem item = (ContentItem) iterator.next();
							addFileToArray(inArchive, array, item);
						}
					}
					else
					{
						ContentItem item = inArchive.getOriginalContent(asset);
						addFileToArray(inArchive, array, item);
					}
					results.add(details);
				}
				finaldata.put("results", results);
				
				String url = inRemoteNode.get("baseurl");
				CloseableHttpResponse response2 = inConnection.sharedPostWithJson(url + "/mediadb/services/cluster/receive/receiveoriginalschanges.json", finaldata);
				StatusLine sl = response2.getStatusLine();
				if (sl.getStatusCode() != 200)
				{
					inRemoteNode.setValue("lasterrormessageoriginals", "Could not get originals list " + sl.getStatusCode() + " " + sl.getReasonPhrase());
					inRemoteNode.setValue("lasterrordateoriginals", new Date());
					getSearcherManager().getSearcher(getCatalogId(), "editingcluster").saveData(inRemoteNode);
					log.error("Could not save changes to remote server " + url + "/mediadb/services/cluster/receive/receiveoriginalschanges.json " + sl.getStatusCode() + " " + sl.getReasonPhrase());
					inConnection.release(response2);
					return;
				}
				//The server will return a list of files it needs
				JSONObject json = inConnection.parseJson(response2);
				
				Map responseheader = (Map)json.get("response");  //These are files they want us to send them
				
				String status = (String)responseheader.get("status");
				if( status.equals("error"))
				{
					inRemoteNode.setValue("lasterrormessageoriginals", "Could not get list of originals from remote server");
					inRemoteNode.setValue("lasterrordateoriginals", new Date());
					getSearcherManager().getSearcher(getCatalogId(), "editingcluster").saveData(inRemoteNode);
					log.error("Could not save changes to remote server " + url + "/mediadb/services/cluster/receive/receiveoriginalschanges.json " + sl.getStatusCode() + " " + sl.getReasonPhrase());
					return;
				}
				String remotecatalogid = (String)responseheader.get("catalogid");
				Collection toupload = (Collection)json.get("results");
				if( toupload != null)
				{
					//TODO: Use pagination to do a few at a time
					String urlpath = url + "/mediadb/services/module/asset/sync/uploadfile.json"; //TODO: This should also include asking for Originals
					for (Iterator iterator = toupload.iterator(); iterator.hasNext();)
					{
						JSONObject fileinfo = (JSONObject) iterator.next();
						
						String localpath = (String)fileinfo.get("path"); //Path on the remote machie
						
						String reallocalpath = localpath.replace(remotecatalogid, inArchive.getCatalogId());  //May not be needed
						ContentItem item = inArchive.getContent(reallocalpath);
						File tosend = new File(item.getAbsolutePath());

						JSONObject tosendparams = new JSONObject(fileinfo);
						tosendparams.put("catalogid", inArchive.getCatalogId());
						tosendparams.put("savepath", localpath);
						tosendparams.put("file.0", tosend);
													
						CloseableHttpResponse resp = null;
						try
						{
							resp = inConnection.sharedMimePost(urlpath,tosendparams);
	
							if (resp.getStatusLine().getStatusCode() != 200)
							{
								//error
								//reportError();
								throw new RuntimeException(resp.getStatusLine().getStatusCode() + " Could not upload: " + localpath + " Error: " + resp.getStatusLine().getReasonPhrase() );
							}
						}
						finally
						{
							inConnection.release(resp);
						}
					}	
				}
			}
		}
		catch (Exception ex)
		{
			throw new OpenEditException(ex);
		}		
		//It will return a list of files I need to upload
		
	}

	protected void addFileToArray(MediaArchive inArchive, JSONArray inArray, ContentItem item)
	{
		JSONObject detail = new JSONObject();
		detail.put("isfolder",item.isFolder());
		String originalspath = item.getPath();
		detail.put("path",item.getPath());
		detail.put("filename",item.getName());
		String starts = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals";
		originalspath = originalspath.substring(starts.length());
		detail.put("originalspath",originalspath);
		detail.put("filesize",String.valueOf(item.getLength()));
		detail.put("filedate",item.getLastModified());
		inArray.add(detail);
	}

	/**
	 * STEP 1 Send a list of files I have and see if you need em
	 * @param inArchive
	 * @param inJsonRequest
	 * @return
	 */
	public List receiveOriginalsChanges(MediaArchive inArchive, Map inJsonRequest)
	{
		
		//Look for any assets and compare all the thunbnails
		Map response = (Map)inJsonRequest.get("response");
		String remotecatalogid = (String)response.get("catalogid");

		Collection results = (Collection)inJsonRequest.get("results");

		List toupload = new ArrayList();
		
		for (Iterator iterator = results.iterator(); iterator.hasNext();)
		{
			JSONObject originaldata = (JSONObject) iterator.next();
			//String assetsouercepath = (String)originaldata.get("sourcepath");
			
			//String remotemasterclusterid = (String) originaldata.get("mastereditclusterid");
//			if( inArchive.getNodeManager().getLocalClusterId().equals(remotemasterclusterid))
//			{
//				log.info("Skipping originals download on non-master generated files?");
//				continue;
//			}  Wait we need these to work

			JSONArray files = (JSONArray)originaldata.get("files");
			
			//Make sure this is a folder. If its a file then move it
			String parentsourcepath = (String)originaldata.get("parentsourcepath");
			ContentItem folderfound = inArchive.getContent("/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/" + parentsourcepath);
			if( folderfound.exists() && !folderfound.isFolder() )
			{
				String name = PathUtilities.extractFileName(parentsourcepath);
				ContentItem dest = inArchive.getContent("/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/" + parentsourcepath + "/" + name + "/");
				inArchive.getPageManager().getRepository().move(folderfound, dest);
			}
			
			if(files != null)
			{
				for (Iterator iterator2 = files.iterator(); iterator2.hasNext();)
				{
					JSONObject contentfile = (JSONObject) iterator2.next();
					long size = Long.parseLong((String)contentfile.get("filesize"));
					String originalpath = (String) contentfile.get("path");
					
					String savepath  = originalpath.replace(remotecatalogid, inArchive.getCatalogId());
					ContentItem found = inArchive.getContent(savepath);
		
					if (!found.exists() || found.getLength() != size)  //Missing files
					{
						//download it
						JSONObject contentdetails = new JSONObject();
						contentdetails.put("path",originalpath);  //This is their path name
						toupload.add(contentdetails);
					}
				}
			}
		}

		//and send them back
		return toupload;	
	}

	
}