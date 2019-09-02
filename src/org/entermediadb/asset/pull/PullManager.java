package org.entermediadb.asset.pull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.elasticsearch.ElasticNodeManager;
import org.entermediadb.elasticsearch.SearchHitData;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.locks.Lock;
import org.openedit.node.NodeManager;
import org.openedit.repository.ContentItem;
import org.openedit.repository.filesystem.FileItem;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.FileUtils;
import org.openedit.util.HttpRequestBuilder;
import org.openedit.util.HttpSharedConnection;
import org.openedit.util.OutputFiller;
import org.openedit.util.PathUtilities;
import org.openedit.util.URLUtilities;

public class PullManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(PullManager.class);
	protected SearcherManager fieldSearcherManager;
	protected NodeManager fieldNodeManager;
	protected String fieldCatalogId;
	OutputFiller filler = new OutputFiller();

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public NodeManager getNodeManager()
	{
		return fieldNodeManager;
	}

	public void setNodeManager(NodeManager inNodeManager)
	{
		fieldNodeManager = inNodeManager;
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public HitTracker listRecentChanges(String inType, Date startingfrom)
	{
		Searcher searcher = getSearcherManager().getSearcher(getCatalogId(), inType);
		MediaArchive archive = (MediaArchive) getSearcherManager().getModuleManager().getBean(getCatalogId(), "mediaArchive");
		QueryBuilder builder = null;
		if(inType.equals("asset")) {
			if(archive.isCatalogSettingTrue("syncalways")) {
				builder = searcher.query().exact("mastereditclusterid", getNodeManager().getLocalClusterId());

			} else {
				builder = searcher.query().exact("importstatus", "complete").exact("mastereditclusterid", getNodeManager().getLocalClusterId());

			}
		} else {
			 builder = searcher.query().exact("mastereditclusterid", getNodeManager().getLocalClusterId());

		}
		//TODO:  support this on all tables
		if (startingfrom != null )
		{
			builder.after("recordmodificationdate", startingfrom);
		} 
		builder.sort("recordmodificationdateDown"); //newer first
		builder.includeDescription();
		HitTracker hits = builder.search();
		if (!hits.isEmpty())
		{
			hits.enableBulkOperations();
			log.info("Found changes " + hits.size());
		}
		return hits;

	}

	@Deprecated
	public void processAssetPullQueue(MediaArchive inArchive, ScriptLogger inLog)
	{
		
		//Connect to all the nodes
		//Run a search based on las time I pulled it down
		Data node = null;
		Collection nodes = getNodeManager().getRemoteEditClusters(inArchive.getCatalogId());
		for (Iterator iterator = nodes.iterator(); iterator.hasNext();)
		{
			node = (Data) iterator.next();
			String url = node.get("baseurl");
			if (url == null)
			{
				continue;
			}
			if( !Boolean.parseBoolean( node.get("enabled") ) )
			{
				inLog.info(node.getName() + " disabled. Skipping ");
				continue;
			}
			
			Date now = new Date();
			HttpSharedConnection connection = new HttpSharedConnection();
			Map<String,String> params = new HashMap();
			try
			{

				if (node.get("entermediakey") != null)
				{
					params.put("entermedia.key", node.get("entermediakey"));
				}
				else
				{
					log.error("entermediakey is required");
					continue;
				}
				Object dateob = node.getValue("lastpulldate");
				if( dateob == null)
				{
					throw new OpenEditException("lastpulldate must be set on " + node.getName());
				}
				Date pulldate = null;
				if (dateob instanceof String)
				{
					pulldate = DateStorageUtil.getStorageUtil().parseFromStorage((String) dateob);
				}
				else
				{
					pulldate = (Date) node.getValue("lastpulldate");
				}

				if (pulldate.getTime() + (1000L*30L) > now.getTime() )
				{
					log.info(node.getName() + " We just ran a pull within last 30 seconds. Trying again later");
					inLog.info(node.getName() + " We just ran a pull within last 30 seconds. Trying again later");
					continue;
				}
				long ago = now.getTime() - pulldate.getTime();
				params.put("lastpullago", String.valueOf( ago ) ); 
				
				Collection pulltypes = inArchive.getCatalogSettingValues("nodepulltypes");
				boolean foundsomething = false;
				for (Iterator iteratort = pulltypes.iterator(); iteratort.hasNext();)
				{
					String inSearchType = (String) iteratort.next();
					params.put("searchtype", inSearchType); //Loop over all of the types
					long totalcount = downloadPullData(inArchive, connection, node, params, inSearchType);
					inLog.info("imported " + totalcount + " " + inSearchType);
					if( totalcount > 0)
					{
						foundsomething = true;
					}
				}
				if( foundsomething )
				{
					node.setValue("lastpulldate", now);
					node.setValue("lasterrormessage", null);
					node.setValue("lasterrordate",null);
					getSearcherManager().getSearcher(inArchive.getCatalogId(), "editingcluster").saveData(node);
				}	
			}
			catch (Throwable ex)
			{
				log.error("Could not process sync files ", ex);
				inLog.error("Could not process sync files " +  ex);
				if( node != null)
				{
					node.setProperty("lasterrormessage", "Could not process sync files " + ex);
					node.setValue("lasterrordate", new Date());
					getSearcherManager().getSearcher(inArchive.getCatalogId(), "editingcluster").saveData(node);
				}
				throw new OpenEditException(ex);
			}
		}
	}
	@Deprecated
	protected long downloadPullData(MediaArchive inArchive, HttpSharedConnection connection, Data node, Map<String,String> params, String inSearchType) throws Exception
	{
		String baseurl = node.get("baseurl");
		//add origiginal support
		String url = baseurl + "/mediadb/services/cluster/listchanges.json";
		StringBuffer debugurl = new StringBuffer();
		debugurl.append("?");
		debugurl.append("entermedia.key=");
		debugurl.append(params.get("entermedia.key"));
		debugurl.append("&lastpullago=");
		if (params.get("lastpullago") != null)
		{
			String last = params.get("lastpullago");
			debugurl.append(last);
		}
		
		debugurl.append("&searchtype=");
		debugurl.append(params.get("searchtype"));
		
		String encoded = url + debugurl;
		log.info("Checking: " + URLUtilities.urlEscape(encoded));
		HttpResponse response2 = connection.sharedPost(url, params);
		StatusLine sl = response2.getStatusLine();
		if (sl.getStatusCode() != 200)
		{
			node.setProperty("lasterrormessage", "Could not download " + sl.getStatusCode() + " " + sl.getReasonPhrase());
			node.setValue("lasterrordate", new Date());
			getSearcherManager().getSearcher(inArchive.getCatalogId(), "editingcluster").saveData(node);
			throw new OpenEditException("Initial data server error " + sl + " on " + encoded);
		}
		String returned = EntityUtils.toString(response2.getEntity());
		//log.info("returned:" + returned);
		Map parsed = (Map) new JSONParser().parse(returned);
		boolean skipgenerated = (boolean) Boolean.parseBoolean(node.get("skipgenerated"));
		boolean skiporiginal = (boolean) Boolean.parseBoolean(node.get("skiporiginal"));

		long assetcount = 0;
		Map response = (Map) parsed.get("response");
		String ok = (String) response.get("status");
		if (ok != null && ok.equals("ok"))
		{
			Collection saved = importChanges(inArchive, returned, parsed,inSearchType);
			assetcount = assetcount + saved.size();
			if("asset".equals(inSearchType)) 
			{
				downloadGeneratedFiles(inArchive, connection, node, params, parsed, skipgenerated, skiporiginal);
			}
			if("category".equals(inSearchType)) 
			{
				if( !saved.isEmpty() )
				{
					inArchive.getCategorySearcher().clearIndex();
					inArchive.getCategoryArchive().clearCategories();
				}	
			}	

			//Now loop over pages
			int pages = Integer.parseInt(response.get("pages").toString());
			String hitssessionid = (String) response.get("hitssessionid");
			params.put("hitssessionid", hitssessionid);
			for (int count = 2; count <= pages; count++)
			{
				url = baseurl + "/mediadb/services/cluster/nextpage.json";

				params.put("page", String.valueOf(count));

				log.info("next page: " + url + debugurl + "&page=" + count + "&hitssessionid=" + hitssessionid);
				response2 = connection.sharedPost(url, params);
				sl = response2.getStatusLine();
				if (sl.getStatusCode() != 200)
				{
					node.setProperty("lasterrormessage", sl.getStatusCode() + " " + sl.getReasonPhrase());
					node.setValue("lasterrordate", new Date());

					getSearcherManager().getSearcher(inArchive.getCatalogId(), "editingcluster").saveData(node);
					log.error("Page server error " + sl);
					return -1;
				}
				returned = EntityUtils.toString(response2.getEntity());
				//log.info("Got page of json: " + returned);
				parsed = (Map) new JSONParser().parse(returned);
				response = (Map) parsed.get("response");
				ok = (String) response.get("status");
				if (ok != null && !ok.equals("ok"))
				{
					log.error("Page could not be loaded " + returned);
					return -1;
				}
				log.info("Downloading page " + count + " of " + pages + " pages. assets count:" + assetcount);
				saved = importChanges(inArchive, returned, parsed,inSearchType);
				assetcount = assetcount + saved.size();
				if("asset".equals(inSearchType)) 
				{
					downloadGeneratedFiles(inArchive, connection, node, params, parsed, skipgenerated, skiporiginal);
				}
			}
			return assetcount;
		}
		else if (ok != null && ok.equals("empty"))
		{
			//No changes found
			return 0;
		}
		else
		{
			log.error("Initial data could not be loaded " + returned);
			return -1;
		}
	}
	
	//Used by both pulls
	protected void downloadGeneratedFiles(MediaArchive inArchive, HttpSharedConnection inConnection, Data node, Map inParams, Map parsed, boolean skipgenerated, boolean skiporiginal)
	{
		String url = node.get("baseurl");
		try
		{
			Map response = (Map)parsed.get("response");

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
							String lastmodified = (String) filelisting.get("lastmodified");
							long datetime = Long.parseLong(lastmodified);
							String genpath = (String) filelisting.get("path"); //TODO: Support multiple catalog ids

							String remotecatalogid = (String)response.get("catalogid");
							String generatefolder = remotecatalogid + "/generated";
							String endpath = genpath.substring(genpath.indexOf(  generatefolder ) + generatefolder.length() ) ;
							String savepath = "/WEB-INF/data/" + inArchive.getCatalogId() + "/generated" + endpath;
							ContentItem found = inArchive.getContent(savepath);
							
							
							if (!found.exists() || !FileUtils.isSameDate(found.getLastModified() , datetime ) )
							{
								log.info("Found change: " + found.getLastModified() + " !=" + datetime + " on " + found.getAbsolutePath());
								
								//http://em9dev.entermediadb.org/openinstitute/mediadb/services/module/asset/downloads/preset/Collections/Cincinnati%20-%20Flying%20Pigs/Flying%20Pig%20Marathon/Business%20Pig.jpg/image1024x768.jpg?cache=false
								//String fullURL = url + "/mediadb/services/module/asset/downloads/generated/" + sourcepath + "/" + filename + "/" + filename;
								String tmpfilename = PathUtilities.extractFileName(endpath);
								String path = url + URLUtilities.encode("/mediadb/services/module/asset/downloads/generatedpreview" + endpath + "/" + tmpfilename);
								HttpResponse genfile = inConnection.sharedPost(path, inParams);
								StatusLine filestatus = genfile.getStatusLine();
								if (filestatus.getStatusCode() != 200)
								{
									log.error("Could not download generated " + filestatus + " " + path);
									throw new OpenEditException("Could not download generated " + filestatus + " " + path);
								}

								//Save to local file
								log.info("Saving :" + endpath + " URL:" + path);
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
						}
					}
				}

				if (!skiporiginal)
				{
					String assetid = (String)changed.get("id");
					Asset asset = inArchive.getAsset(assetid);
					if( asset == null)
					{
						log.error("Could not find asset. Canceling original download for assetid " + assetid + " sourcepath:" + sourcepath);
						continue;
					}
					else
					{
						File file = getFile(asset);
						downloadOriginal(inArchive, asset, file, true);
					}
				}
			}
		}
		catch (Exception ex)
		{
			log.error("Could not download files " + url, ex);
			if( ex instanceof OpenEditException)
			{
				throw (OpenEditException)ex;
			}
			throw new OpenEditException(ex);
		}
	}
	
	@Deprecated
	protected Collection importChanges(MediaArchive inArchive, String returned, Map parsed, String inSearchType)
	{
		//I dont want to edit the json in any way, so using original
		try
		{
			//array = new JsonUtil().parseArray("results", returned);
			JSONParser parser = new JSONParser();
			JSONObject everything = (JSONObject) parser.parse(returned);

			JSONArray jsonarray = (JSONArray) everything.get("results");

			inArchive.getSearcher(inSearchType).saveJson(jsonarray);
			log.info("saved " + jsonarray.size() + " changed " + inSearchType );
			return jsonarray;
		}
		catch (Exception e)
		{
			log.info("Error parsing following content: " + returned);
			throw new OpenEditException(e);
		}
	}

	/**
	 * Should this be in realtime? Maybe we should have as database journal to
	 * track local edits and push them out slowly...yes!
	 * 
	 * @param inType
	 * @param inAssetIds
	 */
	public void pushLocalChangesToMaster(String inType, Collection<String> inAssetIds)
	{
		Searcher searcher = getSearcherManager().getSearcher(getCatalogId(), inType);
		Collection nodes = getNodeManager().getRemoteEditClusters(getCatalogId());
		HttpRequestBuilder builder = new HttpRequestBuilder();
		try
		{
			for (Iterator iterator = nodes.iterator(); iterator.hasNext();)
			{
				Data node = (Data) iterator.next();
				HitTracker hits = searcher.query().ids(inAssetIds).exact("mastereditclusterid", node.getId()).search();
				if (!hits.isEmpty())
				{
					String url = node.get("baseurl");
					if (url != null)
					{
						Map params = new HashMap();
						if (node.get("entermediadkey") != null)
						{
							params.put("entermediadkey", node.get("entermediadkey"));
						}
						if (node.get("lastpulldate") != null)
						{
							params.put("lastpulldate", node.get("lastpulldate"));
						}
						//TODO: Add the json data
						StringBuffer jsonbody = new StringBuffer();
						jsonbody.append("[");
						for (Iterator iterator2 = hits.iterator(); iterator2.hasNext();)
						{
							SearchHitData data = (SearchHitData) iterator2.next();
							jsonbody.append(data.toJsonString());
							if (iterator2.hasNext())
							{
								jsonbody.append(",");
							}
						}
						jsonbody.append("]");
						params.put("changes", jsonbody.toString());

						HttpResponse response2 = builder.post(url + "/mediadb/services/cluster/savechanges.json", params);
						StatusLine sl = response2.getStatusLine();
						if (sl.getStatusCode() != 200)
						{
							node.setProperty("lasterrormessage", "Could not push changes " + sl.getStatusCode() + " " + sl.getReasonPhrase());
							getSearcherManager().getSearcher(getCatalogId(), "editingcluster").saveData(node);
							log.error("Could not save changes to remote server " + url + " " + sl.getStatusCode() + " " + sl.getReasonPhrase());
							continue;
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

	public ContentItem downloadOriginal(MediaArchive inArchive, Asset inAsset, File inFile, boolean ifneeded)
	{

		Data node = (Data) inArchive.getSearcher("editingcluster").searchByField("clustername", inAsset.get("mastereditclusterid"));
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
				HttpSharedConnection connection = new HttpSharedConnection();
				Map params = new HashMap();
				if (node.get("entermediakey") != null)
				{
					params.put("entermedia.key", node.get("entermediakey"));
					finalurl = finalurl.concat("?entermedia.key=" + node.get("entermediakey"));
				}

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
		try
		{
			Data node = (Data) inArchive.getSearcher("editingcluser").searchByField("clustername", inAsset.get("mastednodeid"));
			String url = node.get("baseurl");
			String finalurl = url + URLUtilities.encode(inArchive.asLinkToOriginal(inAsset));
			HttpSharedConnection connection = new HttpSharedConnection();
			Map params = new HashMap();
			if (node.get("entermediakey") != null)
			{
				params.put("entermedia.key", node.get("entermediakey"));
			}

			HttpResponse genfile = connection.sharedPost(finalurl, params);
			return genfile.getEntity().getContent();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			throw new OpenEditException(e);
		}

	}

	protected File getFile(Asset inAsset)
	{
		String path = "/WEB-INF/data" + inAsset.getMediaArchive().getCatalogHome() + "/originals/";
		path = path + inAsset.getSourcePath(); //Check archived?

		String primaryname = inAsset.getPrimaryFile();
		if (primaryname != null && inAsset.isFolder())
		{
			path = path + "/" + primaryname;
		}
		return new File(inAsset.getMediaArchive().getPageManager().getPage(path).getContentItem().getAbsolutePath());

	}

	public void processAllData(MediaArchive inArchive, ScriptLogger inLog)
	{
		
		//TODO: Only call this every 30 seconds not more
		Lock lock = inArchive.getLockManager().lockIfPossible("processAllPull", "processAllPull");
		if( lock == null)
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
		for (Iterator iterator = nodes.iterator(); iterator.hasNext();)
		{
			try
			{
				node = (Data) iterator.next();
				String url = node.get("baseurl");
				if (url == null)
				{
					log.error("No url, skipping");
					continue;
				}
				Date now = new Date();

				HttpSharedConnection connection = new HttpSharedConnection();
				Map params = new HashMap();
				if (node.get("entermediakey") == null)
				{
					log.error("entermediakey is required");
					continue;
				}
				params.put("entermedia.key", node.get("entermediakey"));
				if (node.get("lastpulldate") != null)
				{
					Object dateob = node.getValue("lastpulldate");
					Date pulldate = null;
					if (dateob instanceof String)
					{
						pulldate = DateStorageUtil.getStorageUtil().parseFromStorage((String) dateob);
					}
					else
					{
						pulldate = (Date) node.getValue("lastpulldate");
					}

					if (pulldate.getTime() + (1000L*20L) > System.currentTimeMillis() )
					{
						log.info(node.getName() + " We just ran a pull within last 20 seconds. Trying again later");
						inLog.info(node.getName() + " We just ran a pull within last 20 seconds. Trying again later");
						continue;
					}
					long ago = now.getTime() - pulldate.getTime();
					params.put("lastpullago", String.valueOf( ago ) ); 
				}

				long totalcount = downloadAllData(inArchive, connection, node, params);
				if( totalcount > 0  || node.getValue("lasterrordate") != null)
				{
					node.setValue("lastpulldate", now);
					node.setValue("lasterrormessage", null);
					node.setValue("lasterrordate",null);
					getSearcherManager().getSearcher(inArchive.getCatalogId(), "editingcluster").saveData(node);
				}	
			}
			catch (Throwable ex)
			{
				log.error("Could not process sync files ", ex);
				inLog.error("Could not process sync files " +  ex);
				if( node != null)
				{
					node.setProperty("lasterrormessage", "Could not process sync files " + ex);
					node.setValue("lasterrordate", new Date());
					getSearcherManager().getSearcher(inArchive.getCatalogId(), "editingcluster").saveData(node);
				}	
			}

		}		
	}

	protected long downloadAllData(MediaArchive inArchive, HttpSharedConnection connection, Data node, Map<String,String> params)
	{
			String baseurl = node.get("baseurl");
			//add origiginal support
			String url = baseurl + "/mediadb/services/cluster/listalldocs.json";
			StringBuffer debugurl = new StringBuffer();
			debugurl.append("?");
			debugurl.append("entermedia.key=");
			debugurl.append(params.get("entermedia.key"));
			debugurl.append("&lastpullago=");
			if (params.get("lastpullago") != null)
			{
				String last = params.get("lastpullago");
				debugurl.append(last);
			}
			
			debugurl.append("&searchtype=");
			debugurl.append(params.get("searchtype"));

			String encoded = url + debugurl;
			log.info("Checking: " + URLUtilities.urlEscape(encoded));
			
			HttpResponse response2 = connection.sharedPost(url, params);
			StatusLine sl = response2.getStatusLine();
			if (sl.getStatusCode() != 200)
			{
				node.setProperty("lasterrormessage", "Could not download " + sl.getStatusCode() + " " + sl.getReasonPhrase());
				node.setValue("lasterrordate", new Date());
				getSearcherManager().getSearcher(inArchive.getCatalogId(), "editingcluster").saveData(node);
				log.error("Initial data server error " + sl);
			}
			String returned = null;
			Map parsed = null;
			try
			{
				returned = EntityUtils.toString(response2.getEntity());
				parsed = (Map) new JSONParser().parse(returned);
			}
			catch (Exception e)
			{
				throw new OpenEditException(e);
			}
			long datacounted = 0;
			Map response = (Map) parsed.get("response");
			String ok = (String) response.get("status");
			if (ok != null && ok.equals("ok"))
			{
				boolean skipgenerated = (boolean) Boolean.parseBoolean(node.get("skipgenerated"));
				boolean skiporiginal = (boolean) Boolean.parseBoolean(node.get("skiporiginal"));

				Collection saved = importChanges(inArchive, returned, parsed);
				//pull in generated 	
				downloadGeneratedFiles(inArchive, connection, node, params, parsed, skipgenerated, skiporiginal);

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
					try
					{
						returned = EntityUtils.toString(response2.getEntity());
						parsed = (Map) new JSONParser().parse(returned);
					}
					catch (Exception e)
					{
						throw new OpenEditException(e);
					}
					response = (Map) parsed.get("response");
					ok = (String) response.get("status");
					if (ok != null && !ok.equals("ok"))
					{
						throw new OpenEditException("Page could not be loaded " + returned);
					}
					log.info("Downloading page " + page + " of " + pages + " pages. data count:" + datacounted);
					saved = importChanges(inArchive, returned, parsed);
					//pull in generated 	
					downloadGeneratedFiles(inArchive, connection, node, params, parsed, skipgenerated, skiporiginal);

					datacounted = datacounted + saved.size();
				}
				return datacounted;
			}
			else 
			{
				throw new OpenEditException("Initial data could not be loaded " + returned);
			}
	}

	protected Collection importChanges( MediaArchive inArchive, String inReturned, Map inParsed)
	{
		try
		{
			JSONParser parser = new JSONParser();
			Set searchers = new HashSet();
			JSONObject everything = (JSONObject) parser.parse(inReturned);
			JSONArray jsonarray = (JSONArray) everything.get("results");
			for (Iterator iterator = jsonarray.iterator(); iterator.hasNext();)
			{
				JSONObject object = (JSONObject) iterator.next();
				String catalogid = (String) object.get("catalog");
				catalogid = catalogid.replace("_", "/");
				String searchtype = (String) object.get("searchtype");
				
				Searcher searcher = getSearcherManager().getSearcher(catalogid, searchtype);
				searchers.add(searcher);
				String id = (String) object.get("id");
				JSONObject source = (JSONObject) object.get("source");
				searcher.saveJson(id, source);
				
			}
			
			for (Iterator iterator = searchers.iterator(); iterator.hasNext();)
			{
				Searcher searcher = (Searcher) iterator.next();
				searcher.clearIndex();
				//IF categorty clear cache
			}
			return jsonarray;
		}
		catch (Exception e)
		{
			log.info("Error parsing following content: " + inReturned);
			throw new OpenEditException(e);
		}
		finally 
		{
			ElasticNodeManager manager = (ElasticNodeManager) getNodeManager();
			manager.flushBulk();
		}
	}

	@Deprecated
	public void processPull(MediaArchive inArchive,ScriptLogger inLog)
	{
		//TODO: Only call this every 30 seconds not more
		Lock lock = inArchive.getLockManager().lockIfPossible("processAllPull", "processAllPull");
		if( lock == null)
		{
			log.info("Pull is already locked");
			inLog.info("Pull is locked or already running");
			return;
		}
		try
		{
				processAssetPullQueue(inArchive, inLog);
		}
		finally
		{
			inArchive.releaseLock(lock);
		}
		
		
	}

}
