package org.entermediadb.asset.pull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.util.JsonUtil;
import org.entermediadb.elasticsearch.SearchHitData;
import org.json.simple.parser.JSONParser;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.node.NodeManager;
import org.openedit.repository.ContentItem;
import org.openedit.repository.InputStreamItem;
import org.openedit.repository.filesystem.FileItem;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.HttpRequestBuilder;
import org.openedit.util.OutputFiller;
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
	
	public HitTracker listRecentChanges(String inType, String inLastpulldate)
	{
			Searcher searcher = getSearcherManager().getSearcher(getCatalogId(), inType);
			Date startingfrom = null;
			QueryBuilder builder = searcher.query().exact("importstatus","complete").exact("masternodeid", getNodeManager().getLocalNodeId());
			if( inLastpulldate != null)
			{
				startingfrom = DateStorageUtil.getStorageUtil().parseFromStorage(inLastpulldate);
				builder.after("recordmodificationdate", startingfrom);
			}
			HitTracker hits = builder.search();
			if( !hits.isEmpty() )
			{
				hits.enableBulkOperations();
				log.info("Found changes " + hits.size());
			}
			return hits;

	}


	public void processPullQueue(MediaArchive inArchive)
	{
		//Connect to all the nodes
		//Run a search based on las time I pulled it down
		try
		{
			Collection nodes = getNodeManager().getRemoteNodeList(inArchive.getCatalogId());
			for (Iterator iterator = nodes.iterator(); iterator.hasNext();)
			{
				Data node = (Data) iterator.next();
				String url = node.get("baseurl");
				if( url != null)
				{
					HttpRequestBuilder connection = new HttpRequestBuilder();
					Map params = new HashMap();
					if( node.get("entermediadkey") != null)
					{
						params.put("entermediadkey", node.get("entermediadkey"));
					}
					if( node.get("lastpulldate") != null)
					{
						params.put("lastpulldate", node.get("lastpulldate"));
					}
					Date now = new Date();
					
					HttpResponse response2 = connection.post(url + "/mediadb/services/cluster/listchanges.json", params);
					StatusLine sl = response2.getStatusLine();           
					if (sl.getStatusCode() != 200)
					{
						node.setProperty("lasterrormessage", sl.getStatusCode() + " " + sl.getReasonPhrase());
						getSearcherManager().getSearcher(inArchive.getCatalogId(),"emnode").saveData(node);
						continue;
					}
					String returned = EntityUtils.toString(response2.getEntity());
					Map parsed = (Map)new JSONParser().parse(returned);

					importChanges(inArchive, returned, parsed);

					downloadGeneratedFiles(inArchive,connection,node,params,parsed);
					
					node.setValue("lastpulldate", now);
					getSearcherManager().getSearcher(inArchive.getCatalogId(),"emnode").saveData(node);
					
				}
				
			}
		}
		catch ( Exception ex )
		{
			throw new OpenEditException(ex);
		}

	}

	protected void downloadGeneratedFiles(MediaArchive inArchive, HttpRequestBuilder inConnection, Data node, Map inParams, Map parsed)
	{
		String url = node.get("baseurl");
		try
		{
			//How do I get the sourcepath list?
			Collection changes = (Collection)parsed.get("generated");
			
			for (Iterator iterator2 = changes.iterator(); iterator2.hasNext();)
			{
				Map changed = (Map) iterator2.next();
				String sourcepath = (String)changed.get("sourcepath");
				//List generated media and compare it
				
				Collection files = (Collection)changed.get("files");
				if( files != null)
				{
					for (Iterator iterator3 = files.iterator(); iterator3.hasNext();)
					{
						Map filelisting = (Map) iterator3.next();
						//Compare timestamps
						String filename = (String)filelisting.get("filename");
						String lastmodified = (String)filelisting.get("lastmodified");
						long datetime = Long.parseLong(lastmodified);
						ContentItem found = inArchive.getContent( "/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + sourcepath + "/" + filename);
						if( !found.exists() || found.getLastModified() != datetime)
						{
							//http://em9dev.entermediadb.org/openinstitute/mediadb/services/module/asset/downloads/preset/Collections/Cincinnati%20-%20Flying%20Pigs/Flying%20Pig%20Marathon/Business%20Pig.jpg/image1024x768.jpg?cache=false
							//String fullURL = url + "/mediadb/services/module/asset/downloads/generated/" + sourcepath + "/" + filename + "/" + filename;
							String path = url + URLUtilities.encode("/mediadb/services/module/asset/downloads/generated/" + sourcepath + "/" + filename + "/" + filename );
							HttpResponse genfile = inConnection.sharedPost(path, inParams);
							StatusLine filestatus = genfile.getStatusLine();           
							if (filestatus.getStatusCode() == 200)
							{
								//Save to local file
								log.info("Saving :" + sourcepath + "/" + filename + " URL:" + path);
								InputStream stream = genfile.getEntity().getContent();
//								InputStreamItem item  = new InputStreamItem();
//								item.setAbsolutePath(found.getAbsolutePath());
//								item.setInputStream(genfile.getEntity().getContent());
//								inArchive.getPageManager().getRepository().put(item);
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
			}
		}
		catch ( Exception ex)
		{
			log.error("Could not download files " + url , ex);
		}
	}

	protected Collection importChanges(MediaArchive inArchive, String returned, Map parsed)
	{
		//I dont want to edit the json in any way, so using original
		Collection array = new JsonUtil().parseArray("results", returned); //I wanted to use the data in raw form
		
		inArchive.getAssetSearcher().saveJson(array);
		log.info("saved " + array.size() + " changed asset ");
		return array;
	}

	/**
	 * Should this be in realtime? Maybe we should have as database journal to track local edits and push them out slowly...yes!
	 * @param inType
	 * @param inAssetIds
	 */
	public void pushLocalChangesToMaster(String inType, Collection<String> inAssetIds)
	{
		Searcher searcher = getSearcherManager().getSearcher(getCatalogId(), inType);
		Collection nodes = getNodeManager().getRemoteNodeList(getCatalogId());
		HttpRequestBuilder builder = new HttpRequestBuilder();
		try
		{
			for (Iterator iterator = nodes.iterator(); iterator.hasNext();)
			{
				Data node = (Data) iterator.next();
				HitTracker hits = searcher.query().ids(inAssetIds).exact("masternodeid", node.getId()).search();
				if( !hits.isEmpty())
				{
					String url = node.get("baseurl");
					if( url != null)
					{
						Map params = new HashMap();
						if( node.get("entermediadkey") != null)
						{
							params.put("entermediadkey", node.get("entermediadkey"));
						}
						if( node.get("lastpulldate") != null)
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
							if( iterator2.hasNext() )
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
							node.setProperty("lasterrormessage", sl.getStatusCode() + " " + sl.getReasonPhrase());
							getSearcherManager().getSearcher(getCatalogId(),"emnode").saveData(node);
							log.error("Could not save changes to remote server " + url + " " + sl.getStatusCode() + " " + sl.getReasonPhrase());
							continue;
						}
					}
				}
			}
		}
		catch ( Exception ex )
		{
			throw new OpenEditException(ex);
		}
	}



}
