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
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
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
import org.openedit.locks.Lock;
import org.openedit.repository.ContentItem;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.FileUtils;
import org.openedit.util.PathUtilities;
import org.openedit.util.URLUtilities;

public class DataPuller extends BasePuller implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(DataPuller.class);
	//Used by both pulls
	protected void downloadGeneratedFiles(MediaArchive inArchive, HttpSharedConnection inConnection, Data node, Map inParams, Map parsed, boolean skipgenerated)
	{
		String url = node.get("baseurl");
			Map response = (Map) parsed.get("response");

			Collection generated = (Collection) parsed.get("generated");
			if (generated == null || generated.isEmpty())
			{
				return;
			}
			for (Iterator iterator2 = generated.iterator(); iterator2.hasNext();)
			{
				Map changed = (Map) iterator2.next();
				String sourcepath = (String) changed.get("sourcepath");
				//List generated media and compare it
								
				if (!skipgenerated)
				{
					Collection files = (Collection) changed.get("files");
					if (files != null)
					{
						if (files.isEmpty())
						{
							log.debug("No thumbs :" + sourcepath + " on " + parsed.toString());
							return;
						}
						for (Iterator iterator3 = files.iterator(); iterator3.hasNext();)
						{
							Map filelisting = (Map) iterator3.next();
							//Compare timestamps
							//String lastmodified = (String) filelisting.get("lastmodified");
							downloadOneGeneratedFile(inArchive, inConnection, inParams, url, response, filelisting);

						}
					}
				}
			}
	}
	protected void downloadOneGeneratedFile(MediaArchive inArchive, HttpSharedConnection inConnection, Map inParams, String url, Map response, Map filelisting)
	{
		long datetime = (long) filelisting.get("lastmodified");
		String genpath = (String) filelisting.get("localpath");
		String remotecatalogid = (String) response.get("catalogid");
		String generatefolder = remotecatalogid + "/generated";
		String endpath = genpath.substring(genpath.indexOf(generatefolder) + generatefolder.length());
		String savepath = "/WEB-INF/data/" + inArchive.getCatalogId() + "/generated" + endpath;
		ContentItem found = inArchive.getContent(savepath);

		if (!found.exists() || !FileUtils.isSameDate(found.getLastModified(), datetime))
		{
			log.info("Found change: " + found.getLastModified() + " !=" + datetime + " on " + found.getAbsolutePath());

			//http://em9dev.entermediadb.org/openinstitute/mediadb/services/module/asset/downloads/preset/Collections/Cincinnati%20-%20Flying%20Pigs/Flying%20Pig%20Marathon/Business%20Pig.jpg/image1024x768.jpg?cache=false
			//String fullURL = url + "/mediadb/services/module/asset/downloads/generated/" + sourcepath + "/" + filename + "/" + filename;
			String tmpfilename = PathUtilities.extractFileName(endpath);
			String path = url + URLUtilities.urlEscape("/mediadb/services/module/asset/downloads/generatedpreview" + endpath + "/" + tmpfilename);
			CloseableHttpResponse genfile = null;
			try
			{
				genfile = inConnection.sharedPost(path, inParams);
				StatusLine filestatus = genfile.getStatusLine();
				if (filestatus.getStatusCode() != 200)
				{
					//Cant PULL asset status
					log.error("Could not download generated " + filestatus + " " + path);
					throw new OpenEditException("Could not download generated " + filestatus + " " + path);
				}
				//Save to local file
				log.info("Saving :" + endpath + " URL:" + path);
				try
				{
					InputStream stream = genfile.getEntity().getContent();
					//Change the timestamp to match
					File tosave = new File(found.getAbsolutePath());
					tosave.getParentFile().mkdirs();
					FileOutputStream fos = new FileOutputStream(tosave);
					filler.fill(stream, fos);
					filler.close(stream);
					filler.close(fos);
					tosave.setLastModified(datetime);
				}
				catch (Exception ex)
				{
					log.error("Could not download file " + url + " " + filelisting, ex);
				}
			}
			finally
			{
				inConnection.release(genfile);
			}
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
			processAllDataQueue(inArchive, inLog);
		}
		finally
		{
			inArchive.releaseLock(lock);
		}

	}

	protected void processAllDataQueue(MediaArchive inArchive, ScriptLogger inLog)
	{

		Collection nodes = getNodeManager().getRemoteEditClusters(inArchive.getCatalogId());
		Data node = null;
		
		inLog.info("Scanning " + nodes.size() + " nodes ");
		for (Iterator iterator = nodes.iterator(); iterator.hasNext();)
		{
			try
			{
				node = (Data) iterator.next();
				node.setValue("lasterrordate",null);
				node.setValue("lasterrormessage", null);

				String url = node.get("baseurl");
				if (url == null || !Boolean.parseBoolean( node.get("enabled") ) )
				{
					log.error("skipping " + node);
					continue;
				}
				if (node.get("entermediakey") == null)
				{
					log.error("entermediakey is required");
					continue;
				}

				HttpSharedConnection connection = createConnection(node);
				Object dateob = node.getValue("lastpulldate");
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
					log.info(node.getName() + " We just ran a pull within last 30 seconds. Trying again later");
					inLog.info(node.getName() + " We just ran a pull within last 30 seconds. Trying again later");
					continue;
				}
				Date now = new Date();
				long ago = now.getTime() - pulldate.getTime();
				Map params = new HashMap();
				params.put("lastpullago", String.valueOf(ago));
				params.put("sincedate", DateStorageUtil.getStorageUtil().formatForStorage(pulldate));

				inLog.info(node.getName() + " checking since " + pulldate);

				long totalcount = downloadAllData(inArchive, connection, node, params);

				inLog.info(node.getName() + " imported " + totalcount);

				//uploadChanges... 
				ElasticNodeManager manager = (ElasticNodeManager) inArchive.getNodeManager();
				HitTracker localchanges = manager.getEditedDocuments(getCatalogId(), pulldate);
				
				String remotemastereditid = node.get("clustername");
				HitTracker trimmed = removeRemotesMasterNodeEdits(remotemastereditid,localchanges);

				inLog.info(node.getName() + " syncup local changes" + localchanges.size());

				syncUpLocalDataChanges(inArchive,node,pulldate,trimmed, connection);
				
				inLog.info(node.getName() + " data downloaded " + totalcount + " and uploaded " + trimmed.size() );
				if( totalcount > -1)
				{
					node.setValue("lastpulldate", now);
					getSearcherManager().getSearcher(inArchive.getCatalogId(), "editingcluster").saveData(node);
				}

			}
			catch (Throwable ex)
			{
				log.error("Could not process sync files ", ex);
				inLog.error("Could not process sync files " + ex);
				if (node != null)
				{
					node.setProperty("lasterrormessage", "Could not process sync files " + ex);
					node.setValue("lasterrordate", new Date());
					getSearcherManager().getSearcher(inArchive.getCatalogId(), "editingcluster").saveData(node);
				}
			}

		}
	}

	protected long downloadAllData(MediaArchive inArchive, HttpSharedConnection connection, Data node, Map<String, String> params)
	{
		String baseurl = node.get("baseurl");
		//add origiginal support
		String url = baseurl + "/mediadb/services/cluster/listalldocs.json";
		StringBuffer debugurl = new StringBuffer();
		debugurl.append("?");
		debugurl.append("entermedia.key=");
		debugurl.append(node.get("entermediakey"));
		debugurl.append("&lastpullago=");
		String last = params.get("lastpullago");

		if (params.get("lastpullago") != null)
		{
			debugurl.append(last);
		}

		debugurl.append("&searchtype=");
		if (params.get("searchtype") != null)
		{
			debugurl.append(params.get("searchtype"));
		}

		String encoded = url + debugurl;
		log.info("Checking: " + URLUtilities.urlEscape(encoded));

		
		CloseableHttpResponse response2 = connection.sharedPost(url, params);
		
		StatusLine sl = response2.getStatusLine();
		if (sl.getStatusCode() != 200)
		{
			node.setValue("lasterrormessage", "Could not download " + sl.getStatusCode() + " " + sl.getReasonPhrase());
			node.setValue("lasterrordate", new Date());
			getSearcherManager().getSearcher(inArchive.getCatalogId(), "editingcluster").saveData(node);
			log.error("Initial data server error " + sl);
			return -1;
		}
		JSONObject	remotechanges = connection.parseJson(response2);
		long datacounted = 0;
		Map response = (Map) remotechanges.get("response");
		String ok = (String) response.get("status");
		if (ok != null && ok.equals("ok"))
		{
			boolean skipgenerated = (boolean) Boolean.parseBoolean(node.get("skipgenerated"));

			JSONArray jsonarray = (JSONArray) remotechanges.get("results");

			Collection saved = importDataChanges(inArchive, jsonarray);
			//pull in generated 	
			downloadGeneratedFiles(inArchive, connection, node, params, remotechanges, skipgenerated);

			datacounted = datacounted + saved.size();

			int pages = Integer.parseInt(response.get("pages").toString());
			//loop over pages
			String hitssessionid = (String) response.get("hitssessionid");
			params.put("hitssessionid", hitssessionid);
			for (int page = 2; page <= pages; page++)
			{
				url = baseurl + "/mediadb/services/cluster/nextall.json";

				params.put("page", String.valueOf(page));

				log.info("next page: " + encoded + "&page=" + page + "&hitssessionid=" + hitssessionid);
				
				response2 = connection.sharedPost(url, params);
				
				sl = response2.getStatusLine();
				if (sl.getStatusCode() != 200)
				{
					throw new OpenEditException("Could not load page of data " + sl.getStatusCode() + " " + sl.getReasonPhrase());
				}
				remotechanges = connection.parseJson(response2);
				response = (Map) remotechanges.get("response");
				ok = (String) response.get("status");
				if (ok != null && !ok.equals("ok"))
				{
					throw new OpenEditException("Page could not be loaded " + remotechanges.toJSONString());
				}
				log.info("Downloading page " + page + " of " + pages + " pages. data count:" + datacounted);
				JSONArray results = (JSONArray)remotechanges.get("results"); //records?
				
				saved = importDataChanges(inArchive, results);
				//pull in generated 	
				downloadGeneratedFiles(inArchive, connection, node, params, remotechanges, skipgenerated);

				datacounted = datacounted + saved.size();
			}
			return datacounted;
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

	protected Map buildLocalChanges(MediaArchive inArchive, Date sinceDate)
	{
		ElasticNodeManager manager = (ElasticNodeManager) inArchive.getNodeManager();
		HitTracker hits = manager.getEditedDocuments(getCatalogId(), sinceDate);
		return map(hits);
	}
	protected Map<String, SearchHitData> buildLocalChangesByIds(MediaArchive inArchive, Collection inArray)
	{
		ElasticNodeManager manager = (ElasticNodeManager) inArchive.getNodeManager();
		Collection<String> ids = new ArrayList<String>();
		for (Iterator iterator = inArray.iterator(); iterator.hasNext();)
		{
			Map objt = (Map) iterator.next();
			String id = (String)objt.get("id");
			ids.add(id);
		}
		HitTracker hits = manager.getDocumentsByIds(getCatalogId(), ids);
		return map(hits);
	}


	protected Map map(HitTracker hits)
	{
		HashMap map = new HashMap();
		hits.enableBulkOperations();
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			SearchHitData data = (SearchHitData) iterator.next();
			//JSONObject indiHit = new JSONObject();
			String searchtype = data.getSearchHit().getType();
			map.put(searchtype + data.getId(), data);
		}
		return map;
	}
	

	
	protected Collection importDataChanges(MediaArchive inArchive, Collection jsonarray)
	{
		Set searchers = new HashSet();
		try
		{
			Map<String,SearchHitData> localchanges = buildLocalChangesByIds(inArchive, jsonarray);
			String localClusterId = inArchive.getNodeManager().getLocalClusterId();

			for (Iterator iterator = jsonarray.iterator(); iterator.hasNext();)
			{
				JSONObject object = (JSONObject) iterator.next();
				String searchtype = (String) object.get("searchtype");

				Searcher searcher = inArchive.getSearcher(searchtype);
				searchers.add(searcher);
				String id = (String) object.get("id");
				JSONObject remotesource = (JSONObject) object.get("source");

				SearchHitData localchange = localchanges.get(searchtype + id);
				
				boolean oktosave = false;

				if (localchange != null && remotesource.get("emrecordstatus") != null)
				{
					JSONObject recordstatus = (JSONObject) remotesource.get("emrecordstatus");//What we got from the other server

					Map localrecordstatus = (Map) localchange.getSearchHit().getSource().get("emrecordstatus");
					if( localrecordstatus != null)
					{
						String remotemasterclusterid = (String) recordstatus.get("mastereditclusterid");
						String remotelastmodifiedclusterid = (String) recordstatus.get("lastmodifiedclusterid");
	
						if (remotemasterclusterid.equals(remotelastmodifiedclusterid))
						{
							if (remotemasterclusterid.equals(localClusterId))
							{
								continue; //This is an identical record to what we have, coming back to us.						
							}
						}
					}
					else
					{
						oktosave = true;
					}
					//We have a conflict, has been modified on both.
					if( !oktosave )
					{
						Date localmastermod = DateStorageUtil.getStorageUtil().parseFromStorage((String) localrecordstatus.get("masterrecordmodificationdate"));
						if (localmastermod == null)
						{
							oktosave = true;
						}
						else
						{
							Date remotemastermastermod = DateStorageUtil.getStorageUtil().parseFromStorage((String) recordstatus.get("masterrecordmodificationdate"));
	
							//log.info("Node " + localClusterId + " Pulled " + id + " Local Last Mod was " + localmastermod + " Remote last mod was " + remoterecordmod 
							if (remotemastermastermod.equals(localmastermod) || remotemastermastermod.after(localmastermod)) //In order to save, it MUST be equal or after the master we started with
							{
								Date remoterecordmod = DateStorageUtil.getStorageUtil().parseFromStorage((String) recordstatus.get("recordmodificationdate"));
								Date localrecordmod = DateStorageUtil.getStorageUtil().parseFromStorage((String) localrecordstatus.get("recordmodificationdate"));
								if (remoterecordmod.equals(localrecordmod))
								{
									continue; //we already have this change, it's just circling around	
								}
	
								if (remoterecordmod.after(localrecordmod))
								{
									//no conflict, remote was edited after local was edited.  We are taking the remote.
									oktosave = true;
								}
							}
						}
					}
					if (!oktosave)
					{
						Searcher conflictsearcher = inArchive.getSearcher("remotesaveconflictLog");
						Data conflict = (Data) conflictsearcher.createNewData();

						conflict.setValue("searchtype", searchtype);
						conflict.setValue("pulltime", new Date());
						conflict.setValue("dataid", id);

						if( recordstatus != null)
						{
							String remotemasterclusterid = (String) recordstatus.get("mastereditclusterid");
							String remotelastmodifiedclusterid = (String) recordstatus.get("lastmodifiedclusterid");
							conflict.setValue("masternodeid", remotemasterclusterid);
							conflict.setValue("remoterecordstatus", recordstatus.toJSONString());
							conflict.setValue("remotesource", remotesource.toJSONString());
						}	
						if( localrecordstatus != null)
						{
							conflict.setValue("localrecordstatus", localrecordstatus.toString());
							conflict.setValue("localsource", localchange.getSearchHit().getSource().toString());
						}
						conflictsearcher.saveData(conflict);
					}
				}
				else
				{
					oktosave = true;
				}
				if(oktosave)
				{
					searcher.saveJson(id, remotesource);
				}
			}
		}
		finally
		{
			ElasticNodeManager manager = (ElasticNodeManager) getNodeManager();
			manager.flushBulk();
		}
		for (Iterator iterator = searchers.iterator(); iterator.hasNext();)
		{
			Searcher searcher = (Searcher) iterator.next();
			searcher.clearIndex();
			//IF categorty clear cache
		}
		return jsonarray;

	}

	public JSONObject createJsonFromHits(MediaArchive archive, Date inSince, HitTracker hits)
	{
		ElasticNodeManager manager = (ElasticNodeManager) archive.getNodeManager();

		JSONObject finaldata = new JSONObject();

		JSONObject response = new JSONObject();
		if (hits.isEmpty())
		{
			response.put("status", "empty");
		}
		else
		{
			response.put("status", "ok");
		}
		//String sessionid = inReq.getRequestParameter("hitssessionid");

		response.put("totalhits", hits.size());
		response.put("hitsperpage", hits.getHitsPerPage());
		response.put("page", hits.getPage());
		response.put("pages", hits.getTotalPages());
		response.put("hitssessionid", hits.getSessionId());
		response.put("catalogid", archive.getCatalogId());
		response.put("sincedate", DateStorageUtil.getStorageUtil().formatForStorage(inSince));
		finaldata.put("response", response);
		JSONArray generated = new JSONArray();

		JSONArray results = new JSONArray();
		for (Iterator iterator = hits.iterator(); iterator.hasNext();) //TODO: Add page support
		{
			SearchHitData data = (SearchHitData) iterator.next();
			JSONObject indiHit = new JSONObject();
			String searchtype = data.getSearchHit().getType();
			indiHit.put("searchtype", searchtype);
			String index = data.getSearchHit().getIndex();
			indiHit.put("index", index);
			indiHit.put("catalog", manager.getAliasForIndex(index));
			indiHit.put("source", data.getSearchData());
			indiHit.put("id", data.getId());
			results.add(indiHit);

			if (searchtype.equals("asset")) ///Also add stuff to the generated list
			{
				JSONObject details = new JSONObject();
				details.put("id", data.getId());
				String sourcepath = (String) data.getSearchData().get("sourcepath");
				details.put("sourcepath", sourcepath);
				JSONArray files = new JSONArray();
				
				
				//Asset asset = archive.getAsset(data.getId());
				data.setPropertyDetails(archive.getPropertyDetailsArchive().getPropertyDetailsCached(searchtype));
				Map editstatus = data.getEmRecordStatus();
				String mastereditclusterid = (String)editstatus.get("mastereditclusterid");
				
				for (ContentItem item : archive.listGeneratedFiles(sourcepath))
				{
					JSONObject contentdetails = new JSONObject();
					contentdetails.put("filename", item.getName());
					contentdetails.put("localpath", item.getPath());
					contentdetails.put("size", String.valueOf( item.getLength()) );
					contentdetails.put("lastmodified", item.getLastModified());
					contentdetails.put("mastereditclusterid", mastereditclusterid);
					
					files.add(contentdetails);
				}
				details.put("files", files);
				generated.add(details);
			}
		}
		finaldata.put("results", results);
		finaldata.put("generated", generated);
		
		return finaldata;
	}

	//Send in pages
	
	public JSONArray receiveDataChanges(MediaArchive inArchive, Map inJsonRequest)
	{
		//sed by: java.lang.ClassCastException: class java.util.ArrayList cannot be cast to class org.json.simple.JSONArray (java.util.ArrayList is in module java.base of loader 'bootstrap'; 
		//org.json.simple.JSONArray is in unnamed module of loader 'app')

		
		//Look for any assets and compare all the thunbnails
		Map response = (Map)inJsonRequest.get("response");
		String removecatalogid = (String)response.get("catalogid");

		Collection jsonarray = (Collection)inJsonRequest.get("results");

		importDataChanges(inArchive,jsonarray);
		

		JSONArray toupload = new JSONArray();
		
		Collection generated = (Collection)inJsonRequest.get("generated");
		for (Iterator iterator = generated.iterator(); iterator.hasNext();)
		{
			JSONObject object = (JSONObject) iterator.next();
			String assetsouercepath = (String)object.get("sourcepath");
			
			String remotemasterclusterid = (String) object.get("mastereditclusterid");
//			if( inArchive.getNodeManager().getLocalClusterId().equals(remotemasterclusterid))
//			{
//				log.info("Dont download non-master generated files?");
//				continue;
//			}
			String basepath = "/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + assetsouercepath + "/";
			Collection files = (Collection)object.get("files");
			if( files != null)
			{
				for (Iterator iterator2 = files.iterator(); iterator2.hasNext();)
				{
					Map file = (Map) iterator2.next();
					String filename = (String)file.get("filename");
					String fullpath = basepath + filename;
					String size = (String)file.get("size");
					
					ContentItem item = inArchive.getContent(fullpath);
					if( item.getLength() != Long.parseLong(size))
					{
						//download it
						JSONObject contentdetails = new JSONObject();
						contentdetails.put("filename", item.getName());
						contentdetails.put("localpath", item.getPath());
						contentdetails.put("lastmodified", item.getLastModified());
						toupload.add(contentdetails);
					}
					
				}
			}
		}

		//and send them back
		return toupload;
	}

	/**
	 * Should this be in realtime? Maybe we should have as database journal to
	 * track local edits and push them out slowly...yes!
	 * 
	 * @param inType
	 * @param inAssetIds
	 */
	protected void syncUpLocalDataChanges(MediaArchive inArchive, Data inRemoteNode, Date inSince, HitTracker inLocalchanges, HttpSharedConnection inConnection)
	{
		//Push up any and all data changes with details on the files it has.
		
		if (inLocalchanges.isEmpty())
		{
			return;
		}
		try
		{
			String url = inRemoteNode.get("baseurl");
			if (url != null)
			{
				JSONObject params = createJsonFromHits(inArchive,inSince, inLocalchanges);
				log.info("Sending data changes to server " + url + "/mediadb/services/cluster/receive/uploadchanges.json" + params.toJSONString() );
				CloseableHttpResponse response2 = inConnection.sharedPostWithJson(url + "/mediadb/services/cluster/receive/uploadchanges.json", params);
				StatusLine sl = response2.getStatusLine();
				if (sl.getStatusCode() != 200)
				{
					inRemoteNode.setProperty("lasterrormessage", "Could not push changes " + sl.getStatusCode() + " " + sl.getReasonPhrase());
					getSearcherManager().getSearcher(getCatalogId(), "editingcluster").saveData(inRemoteNode);
					log.error("Could not save changes to remote server " + url + "/mediadb/services/cluster/receive/uploadchanges.json " + sl.getStatusCode() + " " + sl.getReasonPhrase());
					inConnection.release(response2);
					throw new OpenEditException("Could not handle remote exception condition " + "Could not push changes " + sl.getStatusCode() + " " + sl.getReasonPhrase()  );
				}
				//log.info("Got back this " + sl.getStatusCode());

				//The server will return a list of files it needs
				JSONObject json = inConnection.parseJson(response2);
				//log.info("Data returned:" + params.toJSONString() );
				
				String remotecatalogid = (String)json.get("catalogid");
				Collection toupload = (Collection)json.get("fileuploads");
				if( toupload != null)
				{
					//TODO: Use pagination to do a few at a time
					for (Iterator iterator = toupload.iterator(); iterator.hasNext();)
					{
						JSONObject fileinfo = (JSONObject) iterator.next();
						String urlpath = url + "/mediadb/services/module/asset/sync/uploadfile.json"; //TODO: This should also include asking for Originals
						
						String localpath = (String)fileinfo.get("localpath"); //On the remote machie
						
						String reallocalpath = localpath.replace(remotecatalogid, inArchive.getCatalogId());
						ContentItem item = inArchive.getContent(reallocalpath);
						File tosend = new File(item.getAbsolutePath());
						log.info("Sending this file:" + tosend.length() + " " + tosend.getAbsolutePath() );

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
								throw new OpenEditException(resp.getStatusLine().getStatusCode() + " Could not upload: " + localpath + " Error: " + resp.getStatusLine().getReasonPhrase() );
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
	}

	
}