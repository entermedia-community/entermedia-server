package org.entermediadb.elasticsearch;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryAction;
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryRequestBuilder;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotAction;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotRequestBuilder;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsAction;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsRequestBuilder;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsResponse;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotAction;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotRequestBuilder;
import org.elasticsearch.action.admin.indices.close.CloseIndexAction;
import org.elasticsearch.action.admin.indices.close.CloseIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.snapshots.SnapshotInfo;
import org.entermediadb.asset.cluster.NodeManager;
import org.openedit.OpenEditException;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.locks.Lock;
import org.openedit.locks.LockManager;
import org.openedit.page.Page;
import org.openedit.util.PathUtilities;
import org.openedit.util.Replacer;
import org.xbib.elasticsearch.action.knapsack.exp.KnapsackExportRequestBuilder;
import org.xbib.elasticsearch.action.knapsack.exp.KnapsackExportResponse;
import org.xbib.elasticsearch.action.knapsack.imp.KnapsackImportRequestBuilder;
import org.xbib.elasticsearch.action.knapsack.imp.KnapsackImportResponse;
import org.xbib.elasticsearch.action.knapsack.state.KnapsackStateRequestBuilder;
import org.xbib.elasticsearch.action.knapsack.state.KnapsackStateResponse;

public class ElasticNodeManager extends NodeManager
{
	protected Log log = LogFactory.getLog(ElasticNodeManager.class);

	protected Client fieldClient;
	protected boolean fieldShutdown = false;

	
	public Client getClient()
	{
		if( fieldShutdown == false && fieldClient == null)
		{
			synchronized (this)
			{
				if( fieldClient != null)
				{
					return fieldClient;
				}
				NodeBuilder nb = NodeBuilder.nodeBuilder();//.client(client)local(true);
				
				Page config = getPageManager().getPage("/WEB-INF/node.xml"); //Legacy DO Not use REMOVE sometime
				if( !config.exists() )
				{
					//throw new OpenEditException("Missing " + config.getPath());
					config = getPageManager().getPage("/system/configuration/node.xml");
				}
				
				for (Iterator iterator = getLocalNode().getElement().elementIterator("property"); iterator.hasNext();)
				{
					Element	prop = (Element) iterator.next();
					String key = prop.attributeValue("id");
					String val = prop.getTextTrim();

					val = getSetting(key);
					
					nb.settings().put(key, val);
				}
				//extras
	            //nb.settings().put("index.store.type", "mmapfs");
	            //nb.settings().put("index.store.fs.mmapfs.enabled", "true");
	            //nb.settings().put("index.merge.policy.merge_factor", "20");
	           // nb.settings().put("discovery.zen.ping.unicast.hosts", "localhost:9300");
	           // nb.settings().put("discovery.zen.ping.unicast.hosts", elasticSearchHostsList);
	
	            fieldClient = nb.node().client();   //when this line executes, I get the error in the other node 
			}
		}
		return fieldClient;
	}
	
	protected String getSetting(String inId) 
	{
		Page config = getPageManager().getPage("/WEB-INF/node.xml");		
		String abs = config.getContentItem().getAbsolutePath();
		File parent = new File(abs);
		Map params = new HashMap();
		params.put("webroot", parent.getParentFile().getParentFile().getAbsolutePath());
		params.put("nodeid", getLocalNodeId());
		Replacer replace = new Replacer();
		
		String value = getLocalNode().get(inId);
		if( value == null)
		{
			return null;
		}
		if( value.startsWith("."))
		{
			value = PathUtilities.resolveRelativePath(value, abs );
		}
		
		return replace.replace(value, params);
	}

	//called from the lock manager
	public void shutdown()
	{
		if(!fieldShutdown)
		{
			if(fieldClient != null){
				fieldClient.close();
			}
		}
		fieldShutdown = true;
		log.info("Elastic Shutdown called");
	}


	protected String toId(String inId)
	{
		String id = inId.replace('/', '_');
		return id;
	}
	public String createSnapShot(String inCatalogId)
	{
		Lock lock  = null;
		try
		{
			lock = getLockManager(inCatalogId).lock("snapshot", "elasticNodeManager");
			return createSnapShot(inCatalogId,lock);
		}
		finally
		{
			getLockManager(inCatalogId).release(lock);
		}
	}
	protected LockManager getLockManager(String inCatalogId)
	{
		return getSearcherManager().getLockManager(inCatalogId);
	}

	public String createSnapShot(String inCatalogId, Lock inLock)
	{
		String indexid = toId(inCatalogId);
		String path = getSetting("repo.root.location") + "/" + indexid; //Store it someplace unique so we can be isolated?
	
	//log.info("Deleted nodeid=" + id + " records database " + getSearchType() );
	
	    Settings settings = Settings.settingsBuilder()
	            .put("location", path)
	            .build();
	    PutRepositoryRequestBuilder putRepo = 
	    		new PutRepositoryRequestBuilder(getClient().admin().cluster(),PutRepositoryAction.INSTANCE);
	    putRepo.setName(indexid)
	            .setType("fs")
	            .setSettings(settings) //With Unique location saved for each catalog
	            .execute().actionGet();

	    
//	    PutRepositoryRequestBuilder putRepo = 
//	    		new PutRepositoryRequestBuilder(getClient().admin().cluster());
//	    putRepo.setName(indexid)
//	            .setType("fs")
//	            .setSettings(settings) //With Unique location saved for each catalog
//	            .execute().actionGet();

	    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	  //  CreateSnapshotRequestBuilder(elasticSearchClient, CreateSnapshotAction.INSTANCE)
	    
	    CreateSnapshotRequestBuilder builder = new CreateSnapshotRequestBuilder(getClient(),CreateSnapshotAction.INSTANCE);
	    String snapshotid =  format.format(new Date());
	    builder.setRepository(indexid)
	            .setIndices(indexid)
	            .setWaitForCompletion(true)
	            .setSnapshot(snapshotid);
	    builder.execute().actionGet();
	
	    return snapshotid;
	}	
	public String createDailySnapShot(String inCatalogId)
	{		
		Lock lock  = null;
		
		try
		{
			lock = getLockManager(inCatalogId).lock("snapshot", "elasticNodeManager");
	
			List list = listSnapShots(inCatalogId);
			if( list.size() > 0)
			{
				SnapshotInfo recent = (SnapshotInfo)list.iterator().next();
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
				Date date = format.parse(recent.name());
				Calendar yesterday = new GregorianCalendar();
				yesterday.add(Calendar.DAY_OF_YEAR, -1);
				if( date.after(yesterday.getTime()))
				{
					return recent.name();
				}
			}
			return createSnapShot(inCatalogId, lock);
		}
		catch( Throwable ex)
		{
			throw new OpenEditException(ex);
		}
		finally
		{
			getLockManager(inCatalogId).release(lock);
		}
	}
		
	public List listSnapShots(String inCatalogId)
	{
		String indexid = toId(inCatalogId);
	    
	    String path = getSetting("repo.root.location") + "/" + indexid;
	    
	    if (!new File(path).exists()) {
	    	return Collections.emptyList();
	    }
	    Settings settings = Settings.settingsBuilder()
	            .put("location", path)
	            .build();
	    
	    PutRepositoryRequestBuilder putRepo = 
	    		new PutRepositoryRequestBuilder(getClient().admin().cluster(),PutRepositoryAction.INSTANCE);
	    putRepo.setName(indexid)
	            .setType("fs")
	            .setSettings(settings) //With Unique location saved for each catalog
	            .execute().actionGet();
	    
		GetSnapshotsRequestBuilder builder = 
		            new GetSnapshotsRequestBuilder(getClient(),GetSnapshotsAction.INSTANCE);
	    builder.setRepository(indexid);
	    
	    GetSnapshotsResponse getSnapshotsResponse = builder.execute().actionGet();
	    List results =  new ArrayList(getSnapshotsResponse.getSnapshots());
	
	    Collections.sort(results, new Comparator<SnapshotInfo>()
		{
	    	@Override
	    	public int compare(SnapshotInfo inO1, SnapshotInfo inO2)
	    	{
	    		return inO1.name().toLowerCase().compareTo(inO2.name().toLowerCase());
	    	}
		});
	    Collections.reverse(results);
	    return results;
	}
	
	public void restoreSnapShot(String inCatalogId, String inSnapShotId)
	{
		String indexid = toId(inCatalogId);
	   // String reponame = indexid + "_repo";

		   // Obtain the snapshot and check the indices that are in the snapshot
	    AdminClient admin = getClient().admin();
	
	    //TODO: Close index!!
	    
	    try
	    {
		    CloseIndexRequestBuilder closeIndexRequestBuilder =
		            new CloseIndexRequestBuilder(getClient(),CloseIndexAction.INSTANCE);
		    closeIndexRequestBuilder.setIndices(indexid);
		    closeIndexRequestBuilder.execute().actionGet();
	
		    try
		    {
				ClusterHealthResponse health = admin.cluster().prepareHealth(indexid).setWaitForYellowStatus().execute().actionGet(); 
		    }
		    catch( Exception ex)
		    {
		    	log.error(ex);
		    }
		    
		    // Now execute the actual restore action
		    RestoreSnapshotRequestBuilder restoreBuilder = new RestoreSnapshotRequestBuilder(getClient(),RestoreSnapshotAction.INSTANCE);
		    restoreBuilder.setRepository(indexid).setSnapshot(inSnapShotId);
		    restoreBuilder.execute().actionGet();

		    try
		    {
				ClusterHealthResponse health = admin.cluster().prepareHealth(indexid).setWaitForYellowStatus().execute().actionGet(); 
		    }
		    catch( Exception ex)
		    {
		    	log.error(ex);
		    }
}
	    catch( Throwable ex)
	    {
	    	log.error(ex);
	    }
	    admin.indices().open(new OpenIndexRequest(indexid));
	    try
	    {
			ClusterHealthResponse health = admin.cluster().prepareHealth(indexid).setWaitForYellowStatus().execute().actionGet(); 
	    }
	    catch( Exception ex)
	    {
	    	log.info(ex);
	    }
	}
	
	
	public void exportKnapsack(String inCatalogId)
	{		
		Lock lock  = null;
		
		try
		{
			lock = getLockManager(inCatalogId).lock("snapshot", "elasticNodeManager");
			Client client = getClient();
			Date date = new Date();
			Page target = getPageManager().getPage("/WEB-INF/snapshots/knapsack-bulk-" + date.getTime() + ".bulk.gz"  );
			Page folder = getPageManager().getPage(target.getParentPath());
			File file = new File(folder.getContentItem().getAbsolutePath());
			file.mkdirs();
		        Path exportPath = Paths.get(URI.create("file:" + target.getContentItem().getAbsolutePath()));
		        KnapsackExportRequestBuilder requestBuilder = new KnapsackExportRequestBuilder(client.admin().indices())
		                .setArchivePath(exportPath)
		                .setOverwriteAllowed(true);
		        KnapsackExportResponse knapsackExportResponse = requestBuilder.execute().actionGet();
		       
		        KnapsackStateRequestBuilder knapsackStateRequestBuilder =
		               new KnapsackStateRequestBuilder(client.admin().indices());
		        KnapsackStateResponse knapsackStateResponse = knapsackStateRequestBuilder.execute().actionGet();
		        knapsackStateResponse.isExportActive(exportPath);
		        Thread.sleep(1000L);
		        // delete index
//		        client("1").admin().indices().delete(new DeleteIndexRequest("index1")).actionGet();
//		        KnapsackImportRequestBuilder knapsackImportRequestBuilder = new KnapsackImportRequestBuilder(client("1").admin().indices())
//		                .setPath(exportPath);
//		        KnapsackImportResponse knapsackImportResponse = knapsackImportRequestBuilder.execute().actionGet();
//		        if (!knapsackImportResponse.isRunning()) {
//		            logger.error(knapsackImportResponse.getReason());
//		        }
//		        assertTrue(knapsackImportResponse.isRunning());
//		        Thread.sleep(1000L);
//		        // count
//		        long count = client("1").prepareCount("index1").setQuery(QueryBuilders.matchAllQuery()).execute().actionGet().getCount();
//		        assertEquals(1L, count);
		}
		catch( Throwable ex)
		{
			throw new OpenEditException(ex);
		}
		finally
		{
			getLockManager(inCatalogId).release(lock);
		}
	}
	
	
	public HitTracker listKnapsacks(){
		List snaps = getPageManager().getChildrenPathsSorted("/WEB-INF/snapshots/");
		ListHitTracker tracker = new ListHitTracker();
		for (Iterator iterator = snaps.iterator(); iterator.hasNext();)
		{
			String path = (String) iterator.next();
			Page page = getPageManager().getPage(path);
			tracker.add(page);
		}
		return tracker;
	}
	
	public void importKnapsack(String inCatalogId, String inFile)
	{		
		Lock lock  = null;
		
		try
		{
			lock = getLockManager(inCatalogId).lock("snapshot", "elasticNodeManager");

			Page target = getPageManager().getPage("/WEB-INF/snapshots/" + inFile  );

			Client client = getClient();
			   File exportFile = new File(target.getContentItem().getAbsolutePath());
		        Path exportPath = Paths.get(URI.create("file:" + exportFile.getAbsolutePath()));
		        KnapsackImportRequestBuilder knapsackImportRequestBuilder = new KnapsackImportRequestBuilder(client.admin().indices())
		                .setArchivePath(exportPath);
		        KnapsackImportResponse knapsackImportResponse = knapsackImportRequestBuilder.execute().actionGet();
		
		}
		catch( Throwable ex)
		{
			throw new OpenEditException(ex);
		}
		finally
		{
			getLockManager(inCatalogId).release(lock);
		}
	}
	
	
	
}
