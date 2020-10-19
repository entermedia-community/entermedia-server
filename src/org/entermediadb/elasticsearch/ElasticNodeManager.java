package org.entermediadb.elasticsearch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryAction;
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryRequestBuilder;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotAction;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotRequestBuilder;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsAction;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsRequestBuilder;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsResponse;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotAction;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotRequestBuilder;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotResponse;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheRequest;
import org.elasticsearch.action.admin.indices.close.CloseIndexAction;
import org.elasticsearch.action.admin.indices.close.CloseIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkProcessor.Listener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.metadata.AliasOrIndex;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.discovery.zen.elect.ElectMasterService;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.snapshots.SnapshotInfo;
import org.elasticsearch.transport.RemoteTransportException;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.cluster.BaseNodeManager;
import org.entermediadb.elasticsearch.searchers.BaseElasticSearcher;
import org.entermediadb.elasticsearch.searchers.LockSearcher;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.Shutdownable;
import org.openedit.cache.CacheManager;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.locks.Lock;
import org.openedit.locks.LockManager;
import org.openedit.page.Page;
import org.openedit.util.FileUtils;
import org.openedit.util.Replacer;

import com.carrotsearch.hppc.ObjectLookupContainer;
import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.carrotsearch.hppc.cursors.ObjectObjectCursor;

import groovy.json.JsonOutput;

//ES5 class MyNode extends Node {
//    public MyNode(Settings preparedSettings, Collection<Class<? extends Plugin>> classpathPlugins) {
//        super(InternalSettingsPreparer.prepareEnvironment(preparedSettings, null), classpathPlugins);
//    }
//}

public class ElasticNodeManager extends BaseNodeManager implements Shutdownable
{
	protected Log log = LogFactory.getLog(ElasticNodeManager.class);

	protected Client fieldClient;
	protected boolean fieldShutdown = false;
	protected List fieldMappingErrors;
	protected Node fieldNode;
	protected Map fieldIndexSettings;
	protected boolean checkedNodeCount = false;
	protected CacheManager fieldCacheManager;
	protected BulkProcessor fieldBulkProcessor;
	protected ArrayList fieldBulkErrors = new ArrayList();

	public static String[] synctypes = new String[] { "library","category", "asset", "librarycollection"};
	public static Collection synctypesCol = Arrays.asList(synctypes);

	public CacheManager getCacheManager()
	{
		return fieldCacheManager;
	}

	public void setCacheManager(CacheManager inCacheManager)
	{
		fieldCacheManager = inCacheManager;
	}

	protected void loadSettings()
	{
		// TODO Auto-generated method stub
		//TODO: Move node.xml to system
		//TODO: add locking file for this node and remove it when done

		Page config = getPageManager().getPage("/WEB-INF/node.xml"); //Legacy DO Not use REMOVE sometime
		if (!config.exists())
		{
			//throw new OpenEditException("Missing " + config.getPath());
			config = getPageManager().getPage("/system/configuration/node.xml");
		}

		if (!config.exists())
		{
			throw new OpenEditException("WEB-INF/node.xml is not defined");
		}
		Element root = getXmlUtil().getXml(config.getInputStream(), "UTF-8");

		fieldLocalNode = new org.openedit.node.Node();
		String nodeid = getWebServer().getNodeId();
		if (nodeid == null)
		{
			nodeid = root.attributeValue("id");
		}
		if (nodeid == null)
		{
			for (Iterator iterator = root.elementIterator(); iterator.hasNext();)
			{
				Element ele = (Element) iterator.next();
				String key = ele.attributeValue("id");
				if (key.equals("node.name"))
				{
					nodeid = ele.getTextTrim();
					break;
				}
			}
		}
		getLocalNode().setId(nodeid);
		String abs = config.getContentItem().getAbsolutePath();
		File parent = new File(abs);
		Map params = new HashMap();

		String webroot = parent.getParentFile().getParentFile().getAbsolutePath();
		params.put("webroot", webroot);
		params.put("nodeid", getLocalNodeId());

		getLocalNode().setValue("path.plugins", webroot + "/WEB-INF/base/entermedia/elasticplugins");

		Replacer replace = new Replacer();

		Element basenode = getXmlUtil().getXml(getPageManager().getPage("/system/configuration/basenode.xml").getInputStream(), "UTF-8");
		for (Iterator iterator = basenode.elementIterator(); iterator.hasNext();)
		{
			Element ele = (Element) iterator.next();
			String key = ele.attributeValue("id");
			String val = ele.getTextTrim();
			val = replace.replace(val, params);
			getLocalNode().setValue(key, val);
		}

		for (Iterator iterator = root.elementIterator(); iterator.hasNext();)
		{
			Element ele = (Element) iterator.next();
			String key = ele.attributeValue("id");
			String val = ele.getTextTrim();
			//if( val.startsWith("."))
			//{
			val = replace.replace(val, params);
			//}
			getLocalNode().setValue(key, val);
		}
		getLocalNode().setValue("node.name", nodeid);

	}

	protected boolean reindexing = false;

	public List getMappingErrors()
	{
		if (fieldMappingErrors == null)
		{
			fieldMappingErrors = new ArrayList<>();
		}
		return fieldMappingErrors;
	}

	public void setMappingErrors(List inMappingErrors)
	{
		fieldMappingErrors = inMappingErrors;
	}

	public Client getClient()
	{
		if (fieldShutdown == false && fieldClient == null)
		{
			synchronized (this)
			{
				if (fieldClient != null)
				{
					return fieldClient;
				}
				NodeBuilder nb = NodeBuilder.nodeBuilder();
				//ES5: Settings.Builder preparedsettings = Settings.builder();

				for (Iterator iterator = getLocalNode().getProperties().keySet().iterator(); iterator.hasNext();)
				{
					String key = (String) iterator.next();
					if (!key.startsWith("index.") && !key.startsWith("entermedia.") && key.contains(".")) //Legacy
					{
						String val = getLocalNode().getSetting(key);
						//ES5: preparedsettings.put(key, val);
						nb.settings().put(key, val);
					}
				}
				fieldNode = nb.node();
				fieldClient = fieldNode.client(); //when this line executes, I get the error in the other node 

				//nb.settings().put("index.mapper.dynamic",false);

				//			     <property id="path.plugins">${webroot}/WEB-INF/base/entermedia/elasticplugins</property>

				//extras
				//nb.settings().put("index.store.type", "mmapfs");
				//nb.settings().put("index.store.fs.mmapfs.enabled", "true");
				//nb.settings().put("index.merge.policy.merge_factor", "20");
				// nb.settings().put("discovery.zen.ping.unicast.hosts", "localhost:9300");
				// nb.settings().put("discovery.zen.ping.unicast.hosts", elasticSearchHostsList);
				//fieldNode = nb.node();
				//fieldClient = fieldNode.client(); //when this line executes, I get the error in the other node 

			}
		}
		return fieldClient;
	}

	//called from the lock manager
	public void shutdown()
	{

		try
		{
			synchronized (this)
			{

				if (!fieldShutdown)
				{
					if (fieldClient != null)
					{
						try
						{
							//TODO: Should we call FlushRequest req = Requests.flushRequest(toId(getCatalogId()));  ? To The disk drive
							fieldClient.close();
						}
						finally
						{
							if (fieldNode != null)
							{
								fieldNode.close();
							}
							fieldNode.close();
						}
					}
					if (fieldNode != null)
					{
						fieldNode.close();
					}
				}
				fieldShutdown = true;
				System.out.println("Elastic shutdown complete");
			}
		}
		catch (Exception e)
		{
			System.out.println("Elastic shutdown failed");
			e.printStackTrace();
			throw new OpenEditException(e);
		}
	}

	public String toId(String inId)
	{
		String id = inId.replace('/', '_');
		return id;
	}

	protected LockManager getLockManager(String inCatalogId)
	{
		return getSearcherManager().getLockManager(inCatalogId);
	}

	public String createSnapShot(String inCatalogId, boolean wholecluster)
	{
		Lock lock = null;
		try
		{
			log.info("Creating snapshot. Locking table");
			lock = getLockManager(inCatalogId).lock("snapshot", "elasticNodeManager");
			return createSnapShot(inCatalogId, lock, wholecluster);
		}
		finally
		{
			getLockManager(inCatalogId).release(lock);
		}
	}

	public String createSnapShot(String inCatalogId, Lock inLock, boolean wholecluster)
	{
		String indexid = toId(inCatalogId);
		String path = getLocalNode().getSetting("path.repo") + "/" + indexid; //Store it someplace unique so we can be isolated?

		//log.info("Deleted nodeid=" + id + " records database " + getSearchType() );

		Settings settings = Settings.builder().put("location", path).build();
		PutRepositoryRequestBuilder putRepo = new PutRepositoryRequestBuilder(getClient().admin().cluster(), PutRepositoryAction.INSTANCE);
		putRepo.setName(indexid).setType("fs").setSettings(settings) //With Unique location saved for each catalog
				.execute().actionGet();

		//	    PutRepositoryRequestBuilder putRepo = 
		//	    		new PutRepositoryRequestBuilder(getClient().admin().cluster());
		//	    putRepo.setName(indexid)
		//	            .setType("fs")
		//	            .setSettings(settings) //With Unique location saved for each catalog
		//	            .execute().actionGet();

		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		//  CreateSnapshotRequestBuilder(elasticSearchClient, CreateSnapshotAction.INSTANCE)

		CreateSnapshotRequestBuilder builder = new CreateSnapshotRequestBuilder(getClient(), CreateSnapshotAction.INSTANCE);
		String snapshotid = format.format(new Date());
		if (!wholecluster)
		{
			builder.setRepository(indexid).setIndices(indexid).setWaitForCompletion(true).setSnapshot(snapshotid);
		}
		else
		{
			builder.setRepository(indexid).setWaitForCompletion(true).setSnapshot(snapshotid + "-full");
		}
		builder.execute().actionGet();

		return snapshotid;
	}

	public String createDailySnapShot(String inCatalogId)
	{
		return createDailySnapShot(inCatalogId, false);
	}

	public String createDailySnapShot(String inCatalogId, boolean wholedatabase)
	{
		Lock lock = null;

		try
		{
			lock = getLockManager(inCatalogId).lock("snapshot", "elasticNodeManager");
			if( lock != null)
			{
				List list = listSnapShots(inCatalogId);
				if (list.size() > 0)
				{
					SnapshotInfo recent = (SnapshotInfo) list.iterator().next();
					Date date = new Date(recent.startTime());
					Calendar yesterday = new GregorianCalendar();
					
					MediaArchive archive = (MediaArchive) getSearcherManager().getModuleManager().getBean(inCatalogId, "mediaArchive");
					int hours = 24;
					String setting  = archive.getCatalogSettingValue("snapshot_max_period_hours");
					if( setting != null )
					{
						hours = Integer.parseInt(setting);
					}
					yesterday.add(Calendar.HOUR_OF_DAY, 0 - hours); 
					yesterday.add(Calendar.MINUTE,15); //has it been 23 hours and 45 minutes
					if (date.after(yesterday.getTime()))
					{
						return String.valueOf(recent.startTime());
					}
				}
				return createSnapShot(inCatalogId, lock, wholedatabase);
			}
		}
		catch (Throwable ex)
		{
			throw new OpenEditException(ex);
		}
		finally
		{
			getLockManager(inCatalogId).release(lock);
		}
		return null;
	}

	public List listSnapShots(String inCatalogId)
	{
		String indexid = toId(inCatalogId);

		String path = getLocalNode().getSetting("path.repo") + "/" + indexid;

		if (!new File(path).exists())
		{
			return Collections.emptyList();
		}
		Settings settings = Settings.builder().put("location", path).build();

		PutRepositoryRequestBuilder putRepo = new PutRepositoryRequestBuilder(getClient().admin().cluster(), PutRepositoryAction.INSTANCE);
		putRepo.setName(indexid).setType("fs").setSettings(settings) //With Unique location saved for each catalog
				.execute().actionGet();

		GetSnapshotsRequestBuilder builder = new GetSnapshotsRequestBuilder(getClient(), GetSnapshotsAction.INSTANCE);
		builder.setRepository(indexid);

		GetSnapshotsResponse getSnapshotsResponse = builder.execute().actionGet();
		List results = new ArrayList(getSnapshotsResponse.getSnapshots());

		Collections.sort(results, new Comparator<SnapshotInfo>()
		{
			@Override
			public int compare(SnapshotInfo inO1, SnapshotInfo inO2)
			{
				return String.valueOf(inO1.startTime()).compareTo( String.valueOf(inO2.startTime()));
			}
		});
		Collections.reverse(results);
		return results;
	}

	public String restoreLatest(String inCatalogId, String lastrestored)
	{

		List snapshots = listSnapShots(inCatalogId);
		if (snapshots.size() == 0)
		{
			return null;
		}
		SnapshotInfo info = (SnapshotInfo) snapshots.get(0);
		if (lastrestored == null)
		{
			lastrestored = "";
		}

		if (lastrestored.equals(info.name()))
		{
			return null;
		}

		restoreSnapShot(inCatalogId, info.name());

		return info.name();

	}

	public void restoreSnapShot(String inCatalogId, String inSnapShotId)
	{
		String indexid = toId(inCatalogId);
		listSnapShots(indexid);

		// String reponame = indexid + "_repo";

		// Obtain the snapshot and check the indices that are in the snapshot
		AdminClient admin = getClient().admin();

		//TODO: Close index!!

		try
		{
			ClusterHealthResponse health = admin.cluster().prepareHealth(indexid).setWaitForYellowStatus().execute().actionGet();
		}
		catch (Exception ex)
		{
			log.error(ex);
			throw new OpenEditException(ex);
		}

		String currentindex = getIndexNameFromAliasName(indexid); //This is the current index that the alias 
		clearAlias(indexid);
		boolean undo = false;
		try
		{
			//Close it first
			CloseIndexRequestBuilder closeIndexRequestBuilder = new CloseIndexRequestBuilder(getClient(), CloseIndexAction.INSTANCE);
			closeIndexRequestBuilder.setIndices(currentindex);
			closeIndexRequestBuilder.execute().actionGet();
			// Now execute the actual restore action
			//Sleep a little?

			RestoreSnapshotRequestBuilder restoreBuilder = new RestoreSnapshotRequestBuilder(getClient(), RestoreSnapshotAction.INSTANCE);
			restoreBuilder.setRepository(indexid).setSnapshot(inSnapShotId).setWaitForCompletion(true);
			RestoreSnapshotResponse response = restoreBuilder.execute().actionGet();
			//Cant read index information on a closed index
			List<String> restored = response.getRestoreInfo().indices();
			if (restored.isEmpty())
			{
				loadIndex(indexid, currentindex, false);
				throw new OpenEditException("Cannot Restore Snapshot - restored" + restored + " indeces");
			}
			String loadedindexid = getIndexNameFromAliasName(indexid);
			if (!loadedindexid.equals(currentindex))
			{
				DeleteIndexResponse delete = getClient().admin().indices().delete(new DeleteIndexRequest(currentindex)).actionGet();
			}
			ClusterHealthResponse health = admin.cluster().prepareHealth(indexid).setWaitForYellowStatus().execute().actionGet();
		}
		catch (Exception ex)
		{
			undo = true;
			log.error("Could not restore", ex);
		}
		try
		{
			if (undo)
			{
				admin.indices().open(new OpenIndexRequest(currentindex));
			}
			else
			{
				admin.indices().open(new OpenIndexRequest(indexid));
			}

			ClusterHealthResponse health = admin.cluster().prepareHealth(indexid).setWaitForYellowStatus().execute().actionGet();
			MediaArchive archive = (MediaArchive) getSearcherManager().getModuleManager().getBean(inCatalogId, "mediaArchive");
			LockSearcher locks = (LockSearcher) archive.getSearcher("lock");
			locks.clearStaleLocks();
			archive.clearAll();

		}
		catch (Exception ex)
		{
			log.error("Could to finalize " + undo, ex);
			throw new OpenEditException(ex);
		}
	}

	//	public void exportKnapsack(String inCatalogId)
	//	{
	//		Lock lock = null;
	//
	//		try
	//		{
	//			String indexid = toId(inCatalogId);
	//
	//			lock = getLockManager(inCatalogId).lock("snapshot", "elasticNodeManager");
	//			Client client = getClient();
	//			Date date = new Date();
	//			Page target = getPageManager().getPage("/WEB-INF/data/" + inCatalogId + "/snapshots/knapsack-bulk-" + date.getTime() + ".bulk.gz");
	//			Page folder = getPageManager().getPage(target.getParentPath());
	//			File file = new File(folder.getContentItem().getAbsolutePath());
	//			file.mkdirs();
	//			Path exportPath = Paths.get(URI.create("file:" + target.getContentItem().getAbsolutePath()));
	//			KnapsackExportRequestBuilder requestBuilder = new KnapsackExportRequestBuilder(client.admin().indices()).setArchivePath(exportPath).setOverwriteAllowed(true).setIndex(indexid);
	//			KnapsackExportResponse knapsackExportResponse = requestBuilder.execute().actionGet();
	//
	//			KnapsackStateRequestBuilder knapsackStateRequestBuilder = new KnapsackStateRequestBuilder(client.admin().indices());
	//			KnapsackStateResponse knapsackStateResponse = knapsackStateRequestBuilder.execute().actionGet();
	//			knapsackStateResponse.isExportActive(exportPath);
	//			Thread.sleep(1000L);
	//			// delete index
	//			//		        client("1").admin().indices().delete(new DeleteIndexRequest("index1")).actionGet();
	//			//		        KnapsackImportRequestBuilder knapsackImportRequestBuilder = new KnapsackImportRequestBuilder(client("1").admin().indices())
	//			//		                .setPath(exportPath);
	//			//		        KnapsackImportResponse knapsackImportResponse = knapsackImportRequestBuilder.execute().actionGet();
	//			//		        if (!knapsackImportResponse.isRunning()) {
	//			//		            logger.error(knapsackImportResponse.getReason());
	//			//		        }
	//			//		        assertTrue(knapsackImportResponse.isRunning());
	//			//		        Thread.sleep(1000L);
	//			//		        // count
	//			//		        long count = client("1").prepareCount("index1").setQuery(QueryBuilders.matchAllQuery()).execute().actionGet().getCount();
	//			//		        assertEquals(1L, count);
	//		}
	//		catch (Throwable ex)
	//		{
	//			throw new OpenEditException(ex);
	//		}
	//		finally
	//		{
	//			getLockManager(inCatalogId).release(lock);
	//		}
	//	}
	//
	//	public HitTracker listKnapsacks(String inCatalogId)
	//	{
	//		List snaps = getPageManager().getChildrenPathsSorted("/WEB-INF/data/" + inCatalogId + "/snapshots/");
	//		ListHitTracker tracker = new ListHitTracker();
	//		for (Iterator iterator = snaps.iterator(); iterator.hasNext();)
	//		{
	//			String path = (String) iterator.next();
	//			Page page = getPageManager().getPage(path);
	//			tracker.add(page);
	//		}
	//		return tracker;
	//	}
	//
	//	public void importKnapsack(String inCatalogId, String inFile)
	//	{
	//		Lock lock = null;
	//
	//		try
	//		{
	//			lock = getLockManager(inCatalogId).lock("snapshot", "elasticNodeManager");
	//			String indexid = toId(inCatalogId);
	//
	//			Page target = getPageManager().getPage("/WEB-INF/data/" + inCatalogId + "/snapshots/" + inFile);
	//
	//			Client client = getClient();
	//			File exportFile = new File(target.getContentItem().getAbsolutePath());
	//			Path exportPath = Paths.get(URI.create("file:" + exportFile.getAbsolutePath()));
	//			KnapsackImportRequestBuilder knapsackImportRequestBuilder = new KnapsackImportRequestBuilder(client.admin().indices()).setArchivePath(exportPath).setIndex(indexid);
	//			KnapsackImportResponse knapsackImportResponse = knapsackImportRequestBuilder.execute().actionGet();
	//
	//		}
	//		catch (Throwable ex)
	//		{
	//			throw new OpenEditException(ex);
	//		}
	//		finally
	//		{
	//			getLockManager(inCatalogId).release(lock);
	//		}
	//	}

	public void addMappingError(String inSearchType, String inMessage)
	{
		MappingError error = new MappingError();
		error.setError(inMessage);
		error.setSearchType(inSearchType);
		if (inMessage.contains("Mapper for ["))
		{
			String guessdetail = inMessage.substring("Mapper for [".length(), inMessage.length());
			guessdetail = guessdetail.substring(0, guessdetail.indexOf("]"));
			error.setDetail(guessdetail);
		}

		if (inMessage.contains("cannot be changed"))
		{
			String guessdetail = inMessage.substring("mapper [".length(), inMessage.length());
			guessdetail = guessdetail.substring(0, guessdetail.indexOf("]"));
			error.setDetail(guessdetail);
		}

		getMappingErrors().add(error);

	}

	public boolean hasMappingErrors()
	{
		return !getMappingErrors().isEmpty();
	}

	public boolean containsCatalog(String inCatalogId)
	{
		if (getConnectedCatalogIds().containsKey(inCatalogId))
		{
			return true;
		}
		String index = toId(inCatalogId);
		IndicesExistsRequest existsreq = Requests.indicesExistsRequest(index);
		IndicesExistsResponse res = getClient().admin().indices().exists(existsreq).actionGet();
		return res.isExists();
	}

	public boolean connectCatalog(String inCatalogId)
	{
		if (!getConnectedCatalogIds().containsKey(inCatalogId))
		{
			synchronized (this)
			{
				if (!getConnectedCatalogIds().containsKey(inCatalogId))
				{
					String alias = toId(inCatalogId);

					AdminClient admin = getClient().admin();
					ClusterHealthResponse health = admin.cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();

					if (health.isTimedOut())
					{
						throw new OpenEditException("Could not get yellow status for " + alias);
					}
					String index = getIndexNameFromAliasName(alias);//see if we already have an index
					//see if an actual index exists

					if (index == null)
					{
						index = alias + "-0";
					}
					IndicesExistsRequest existsreq = Requests.indicesExistsRequest(index); //see if 
					IndicesExistsResponse res = admin.indices().exists(existsreq).actionGet();
					//			if (res.isExists() ){
					//				index = alias;
					//			}

					boolean createdIndex = prepareIndex(health, index);
					if (createdIndex)
					{
						if (!res.isExists())
						{
							admin.indices().prepareAliases().addAlias(index, alias).execute().actionGet();//This sets up an alias that the app uses so we can flip later.
						}
					}
					getConnectedCatalogIds().put(inCatalogId, index);
					//			PropertyDetailsArchive archive = getSearcherManager().getPropertyDetailsArchive(inCatalogId);
					//			List sorted = archive.listSearchTypes();
					//			for (Iterator iterator = sorted.iterator(); iterator.hasNext();)
					//			{
					//				String type = (String) iterator.next();
					//				Searcher searcher = getSearcherManager().getSearcher(inCatalogId, type);
					//				searcher.initialize();	
					//			}

					return true;
				}
			}
		}
		return true;
	}

	public boolean prepareIndex(String index)
	{
		return prepareIndex(null, index);
	}

	public boolean prepareIndex(ClusterHealthResponse health, String index)
	{

		AdminClient admin = getClient().admin();

		IndicesExistsRequest existsreq = Requests.indicesExistsRequest(index);
		IndicesExistsResponse res = admin.indices().exists(existsreq).actionGet();
		boolean createdIndex = false;
		if (!res.isExists())
		{
			InputStream in = null;
			try
			{
				Page yaml = getPageManager().getPage("/system/configuration/elasticindex.yaml");
				in = yaml.getInputStream();
				Builder settingsBuilder = Settings.builder().loadFromStream(yaml.getName(), in);

				for (Iterator iterator = getLocalNode().getProperties().keySet().iterator(); iterator.hasNext();)
				{
					String key = (String) iterator.next();
					if (key.startsWith("index.")) //Legacy
					{
						String val = getLocalNode().getSetting(key);
						settingsBuilder.put(key, val);
					}
				}

				CreateIndexResponse newindexres = admin.indices().prepareCreate(index).setSettings(settingsBuilder).execute().actionGet();

				/*
				 * XContentBuilder settingsBuilder =
				 * XContentFactory.jsonBuilder() .startObject()
				 * .startObject("analysis") .startObject("analyzer")
				 * .startObject("lowersnowball").field("tokenizer",
				 * "standard").startArray("filter").value("standard").value(
				 * "lowercase").value("stemfilter").endArray() .endObject()
				 * .endObject() .startObject("analyzer")
				 * .startObject("tags").field("type",
				 * "custom").field("tokenizer",
				 * "keyword").startArray("filter").value("lowercase").endArray()
				 * .endObject() .endObject() .startObject("filter")
				 * .startObject("stemfilter").field("type","snowball").field(
				 * "language","English") .endObject() .endObject() .endObject();
				 */

				if (newindexres.isAcknowledged())
				{
					log.info("index created " + index);
				}
				createdIndex = true;
			}
			catch (RemoteTransportException exists)
			{
				// silent error
				log.debug("Index already exists " + index);
			}
			catch (Exception e)
			{
				if (e instanceof RuntimeException)
				{
					throw (RuntimeException) e;
				}
				throw new OpenEditException(e);
			}
			finally
			{
				FileUtils.safeClose(in);
			}
		}
		else
		{
			RefreshRequest req = Requests.refreshRequest(index);
			RefreshResponse rres = admin.indices().refresh(req).actionGet();
			if (rres.getFailedShards() > 0)
			{
				log.error("Could not refresh shards");
			}
			//TODO: Make sure we are setting replicas and master nodes
			//			if (health != null && health.getNumberOfNodes() > 1)
			//			{
			//				settingsBuilder.put(ElectMasterService.DISCOVERY_ZEN_MINIMUM_MASTER_NODES, "2");
			//				settingsBuilder.put(ElectMasterService.DISCOVERY_ZEN_MINIMUM_MASTER_NODES, "2");
			//			}
		}

		//Check the number of nodes

		return createdIndex;
	}

	public String getIndexNameFromAliasName(final String aliasName)
	{
		AliasOrIndex indexToAliasesMap = getClient().admin().cluster().state(Requests.clusterStateRequest()).actionGet().getState().getMetaData().getAliasAndIndexLookup().get(aliasName);

		if (indexToAliasesMap == null)
		{
			return null;
		}

		if (indexToAliasesMap.isAlias() && indexToAliasesMap.getIndices().size() > 0)
		{
			//ES5 return indexToAliasesMap.getIndices().iterator().next().getIndex().getName();
			return indexToAliasesMap.getIndices().iterator().next().getIndex();
		}

		return null;
	}

	public String getAliasForIndex(final String inIndex)
	{

		String catalog = (String) getCacheManager().get("system", inIndex);
		if (catalog != null)
		{
			return catalog;
		}

		AliasOrIndex indexToAliasesMap = getClient().admin().cluster().state(Requests.clusterStateRequest()).actionGet().getState().getMetaData().getAliasAndIndexLookup().get(inIndex);

		if (indexToAliasesMap == null)
		{
			return null;
		}

		if (!indexToAliasesMap.isAlias())
		{
			//ES5 return indexToAliasesMap.getIndices().iterator().next().getIndex().getName();

			IndexMetaData index = indexToAliasesMap.getIndices().iterator().next();
			if (index.getAliases().size() > 0)
			{
				ImmutableOpenMap aliases = index.getAliases();
				ObjectLookupContainer bob = aliases.keys();
				ObjectCursor key = (ObjectCursor) bob.iterator().next();
				String catalogid = (String) key.value;
				getCacheManager().put("system", inIndex, catalogid);
				return catalogid;
			}

		}

		return null;

	}

	protected void clearAlias(final String aliasName)
	{

		AliasOrIndex indexToAliasesMap = getClient().admin().cluster().state(Requests.clusterStateRequest()).actionGet().getState().getMetaData().getAliasAndIndexLookup().get(aliasName);

		if (indexToAliasesMap == null)
		{
			return;
		}

		if (indexToAliasesMap.isAlias() && indexToAliasesMap.getIndices().size() > 0)
		{
			for (Iterator iterator = indexToAliasesMap.getIndices().iterator(); iterator.hasNext();)
			{
				IndexMetaData metadata = (IndexMetaData) iterator.next();
				//ES5 String indexid = metadata.getIndex().getName();
				String indexid = metadata.getIndex();
				getClient().admin().indices().prepareAliases().removeAlias(indexid, aliasName).execute().actionGet();

			}
		}

	}

	public boolean reindexInternal(String inCatalogId)
	{
		if (reindexing)
		{
			throw new OpenEditException("Already reindexing");
		}
		reindexing = true;
		try
		{
			createSnapShot(inCatalogId);
		}
		catch (Throwable ex)
		{
			log.error("Could not get snapshot", ex);
			reindexing = false;
		}
		Searcher reindexlogs = getSearcherManager().getSearcher("system", "reindexLog");
		Data reindexhistory = reindexlogs.createNewData();
		reindexhistory.setValue("date", new Date());
		reindexhistory.setValue("operation", "started");
		reindexhistory.setValue("catalogid", inCatalogId);
		reindexlogs.saveData(reindexhistory);
		String newindex = null;
		String searchtype = null;
		try
		{
			getPageManager().clearCache();
			getSearcherManager().getCacheManager().clearAll();

			String id = toId(inCatalogId);

			PropertyDetailsArchive archive = getSearcherManager().getPropertyDetailsArchive(inCatalogId);
			//List mappedtypes = archive.listSearchTypes();

			List searchers = new ArrayList();
			archive.clearCache();
			getPageManager().clearCache();
			getSearcherManager().getCacheManager().clearAll();

			//need to reset/create the mappings o
			List mappedtypes = getMappedTypes(inCatalogId); //from existing elastic database
			for (Iterator iterator = mappedtypes.iterator(); iterator.hasNext();)
			{
				String searchtypeid = (String) iterator.next();
				if (searchtypeid == null || searchtypeid.equals("null"))
				{
					continue;
				}
				Searcher searcher = getSearcherManager().loadSearcher(inCatalogId, searchtypeid, false);
				searchers.add(searcher);
				//Make sure all the errors are done loading from initilize() putMappings on the old index
			}

			newindex = prepareTemporaryIndex(inCatalogId, searchers); //TODO: Dont initilize/putMappings on all the searchers

			mappedtypes.remove("lock");

			for (Iterator iterator = searchers.iterator(); iterator.hasNext();)
			{
				Searcher searcher = (Searcher) iterator.next();
				searchtype = searcher.getSearchType();
				long start = System.currentTimeMillis();

				reindexhistory = reindexlogs.createNewData();
				reindexhistory.setValue("date", new Date());
				reindexhistory.setValue("operation", "progress");
				reindexhistory.setValue("catalogid", inCatalogId);
				reindexhistory.setValue("details", "Starting: " + searchtype);

				reindexlogs.saveData(reindexhistory);

				//This calls initialize that calls putMappings...
				if (searcher.getCatalogId().equals(inCatalogId))
				{
					searcher.setAlternativeIndex(newindex);//Should	not look at searchers from system etc
				}
				else
				{
					continue;
				}
				searcher.reindexInternal(); //This calls put mapping again
				searcher.setAlternativeIndex(null);

				reindexhistory = reindexlogs.createNewData();
				reindexhistory.setValue("date", new Date());
				reindexhistory.setValue("operation", "progress");
				reindexhistory.setValue("catalogid", inCatalogId);
				reindexhistory.setValue("details", "Finished: " + searchtype);

				reindexlogs.saveData(reindexhistory);
				long end = System.currentTimeMillis();
				log.info("Reindex of " + searchtype + " took " + (end - start) / 1000L);
			}

			loadIndex(id, newindex, true);
			getSearcherManager().clear();

		}
		catch (Throwable e)
		{
			reindexhistory = reindexlogs.createNewData();
			reindexhistory.setValue("date", new Date());
			reindexhistory.setValue("operation", "failed");
			reindexhistory.setValue("catalogid", inCatalogId);
			reindexhistory.setValue("details", "Errrored: " + e);

			reindexlogs.saveData(reindexhistory);
			log.error("Could not reindex " + searchtype, e);
			if (newindex != null)
			{
				DeleteIndexResponse delete = getClient().admin().indices().delete(new DeleteIndexRequest(newindex)).actionGet();
				if (!delete.isAcknowledged())
				{
					log.error("Index wasn't deleted " + newindex);
				}
			}
			if (e instanceof OpenEditException)
			{
				throw e;
			}

			throw new OpenEditException("Could not reindex " + searchtype, e);
		}
		finally
		{
			reindexing = false;
		}

		reindexhistory = reindexlogs.createNewData();
		reindexhistory.setValue("date", new Date());
		reindexhistory.setValue("operation", "completed");
		reindexhistory.setValue("catalogid", inCatalogId);

		return true;

	}

	public String prepareTemporaryIndex(String inCatalogId, List searchers)
	{
		Date date = new Date();
		String id = toId(inCatalogId);
		String tempindex = id + date.getTime();
		prepareIndex(null, tempindex);

		boolean ok = true;
		try
		{
			getMappingErrors().clear();

			for (Iterator iterator = searchers.iterator(); iterator.hasNext();)
			{
				Searcher searcher = (Searcher) iterator.next();
				if (!searcher.getCatalogId().equals(inCatalogId))
				{
					continue;
				}
				searcher.setAlternativeIndex(tempindex);//Should

				if (!searcher.putMappings())
				{
					ok = false;
					//get this mapping and show all mapping info
					if( searcher instanceof BaseElasticSearcher)
					{
						BaseElasticSearcher elasticsearcher = (BaseElasticSearcher) searcher;
						String error = "Could not map " + searcher.getSearchType();
						XContentBuilder source = elasticsearcher.buildMapping();
						try
						{
							String out = JsonOutput.prettyPrint(source.string());
							error = error + " with mapping of : <pre class='errordump'>" + out + "</pre><br>";
						}
						catch (IOException e)
						{
							log.error("Mapping ", e);
						}
						error = error + " existing mapping " + showAllExistingMapping(inCatalogId, tempindex);
						throw new OpenEditException(error); //real error
					}
				}
				searcher.setAlternativeIndex(null);
			}
		}
		finally
		{
			if (!ok)
			{
				DeleteIndexResponse delete = getClient().admin().indices().delete(new DeleteIndexRequest(tempindex)).actionGet();
				if (!delete.isAcknowledged())
				{
					log.error("Index wasn't deleted");
				}
			}
		}
		return tempindex;
	}

	public String listAllExistingMapping(String inCatalogId)
	{
		String indexid = toId(inCatalogId);
		String actualindex = getIndexNameFromAliasName(indexid);

		String mapping = showAllExistingMapping(inCatalogId, actualindex);
		return mapping;
	}

	protected String showAllExistingMapping(String inCatalogId, String tmpindexid)
	{

		GetMappingsResponse getMappingsResponse = getClient().admin().indices().getMappings(new GetMappingsRequest().indices(tmpindexid)).actionGet();

		ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> indexToMappings = getMappingsResponse.getMappings();

		StringBuffer all = new StringBuffer();

		ImmutableOpenMap<String, MappingMetaData> typeMappings = indexToMappings.get(tmpindexid);
		for (ObjectCursor<String> typeName : typeMappings.keys())
		{
			if (typeName.value != null && typeName.value.startsWith("$"))
			{
				continue;
			}
			MappingMetaData actualMapping = typeMappings.get(typeName.value);
			if (actualMapping != null)
			{
				try
				{
					String jsonString = actualMapping.source().string();
					jsonString = JsonOutput.prettyPrint(jsonString);
					all.append("<br>" + typeName.value + " : <pre>" + jsonString + "</pre>");
				}
				catch (IOException e)
				{
					new OpenEditException(e);
				}
			}
		}
		return all.toString();
	}

	public List getMappedTypes(String inCatalogId)
	{
		String indexid = toId(inCatalogId);
		String oldindex = getIndexNameFromAliasName(indexid);
		List types = new ArrayList();
		GetMappingsRequest req = new GetMappingsRequest().indices(indexid);
		GetMappingsResponse resp = getClient().admin().indices().getMappings(req).actionGet();

		ImmutableOpenMap<String, MappingMetaData> mapping = resp.getMappings().get(oldindex);
		for (ObjectObjectCursor<String, MappingMetaData> c : mapping)
		{
			// System.out.println(c.key+" = "+c.value.source());
			if (!c.key.startsWith("$"))
			{
				types.add(c.key);
			}
		}

		//		for (ObjectCursor<String> mappingIndexName : resp.getMappings().keys()) 
		//		{
		//			ImmutableOpenMap<String, MappingMetaData> typeMappings = resp.getMappings().get(mappingIndexName.value);
		//            for (ObjectCursor<String> typeName : typeMappings.keys()) 
		//            {
		//            	types.add(typeName.value);
		//                MappingMetaData typeMapping = typeMappings.get(typeName.value);
		//               // Map<String, Map<String, String>> properties = getPropertiesFromTypeMapping(typeMapping);
		//            }
		//		}

		types.remove("category");
		types.add(0, "category");
		return types;

	}

	public boolean checkAllMappings(String inCatalogId)
	{
		Date date = new Date();
		String id = toId(inCatalogId);

		String tempindex = id + date.getTime();
		prepareIndex(null, tempindex);
		//need to reset/creat the mappings here!
		getMappingErrors().clear();
		PropertyDetailsArchive archive = getSearcherManager().getPropertyDetailsArchive(inCatalogId);
		List withparents = archive.findChildTablesNames();

		for (Iterator iterator = withparents.iterator(); iterator.hasNext();)
		{
			String searchtype = (String) iterator.next();
			Searcher searcher = getSearcherManager().getSearcher(inCatalogId, searchtype);
			searcher.setAlternativeIndex(tempindex);//Should				
			searcher.putMappings();
			searcher.setAlternativeIndex(null);

		}

		List sorted = archive.listSearchTypes();
		for (Iterator iterator = sorted.iterator(); iterator.hasNext();)
		{

			String searchtype = (String) iterator.next();
			if (!withparents.contains(searchtype))
			{

				Searcher searcher = getSearcherManager().getSearcher(inCatalogId, searchtype);

				searcher.setAlternativeIndex(tempindex);//Should				
				searcher.putMappings();
				searcher.setAlternativeIndex(null);
			}

		}
		DeleteIndexResponse delete = getClient().admin().indices().delete(new DeleteIndexRequest(tempindex)).actionGet();
		if (!delete.isAcknowledged())
		{
			log.error("Index wasn't deleted");
		}
		if (getMappingErrors().size() == 0)
		{
			return true;
		}
		else
		{
			return false;
		}

	}

	public void loadIndex(String id, String inTarget, boolean dropold)
	{
		id = toId(id);

		String oldindex = getIndexNameFromAliasName(id);
		getClient().admin().indices().prepareAliases().removeAlias(oldindex, id).addAlias(inTarget, id).execute().actionGet();
		if (dropold)
		{

			DeleteIndexResponse response = getClient().admin().indices().delete(new DeleteIndexRequest(oldindex)).actionGet();
			log.info("Dropped: " + response.isAcknowledged());
		}

	}

	private Listener createLoggingBulkProcessorListener()
	{
		return new BulkProcessor.Listener()
		{
			@Override
			public void beforeBulk(long executionId, BulkRequest request)
			{
			}

			@Override
			public void afterBulk(long executionId, BulkRequest request, BulkResponse response)
			{
			}

			@Override
			public void afterBulk(long executionId, BulkRequest request, Throwable failure)
			{
			}
		};
	}

	public void removeMappingError(String inSearchType)
	{
		// TODO Auto-generated method stub
		getMappingErrors().remove(inSearchType);
	}

	@Override
	public void deleteCatalog(String inId)
	{
		DeleteIndexResponse delete = getClient().admin().indices().delete(new DeleteIndexRequest(toId(inId))).actionGet();
		if (!delete.isAcknowledged())
		{
			log.error("Index wasn't deleted");
		}

	}

	@Override
	public String createSnapShot(String inCatalogId)
	{
		return createSnapShot(inCatalogId, false);
	}

	public boolean tableExists(String indexid, String searchtype)
	{
		boolean used = getClient().admin().indices().typesExists(new TypesExistsRequest(new String[] { indexid }, searchtype)).actionGet().isExists();
		return used;

	}

	public void clear()
	{
		getClient().admin().indices().clearCache(new ClearIndicesCacheRequest()).actionGet();
	}

	public BulkProcessor getBulkProcessor()
	{

		if (fieldBulkProcessor == null)
		{

			fieldBulkProcessor = BulkProcessor.builder(getClient(), new BulkProcessor.Listener()
			{
				@Override
				public void beforeBulk(long executionId, BulkRequest request)
				{

				}

				@Override
				public void afterBulk(long executionId, BulkRequest request, BulkResponse response)
				{
					for (int i = 0; i < response.getItems().length; i++)
					{
						// request.getFromContext(key)
						BulkItemResponse res = response.getItems()[i];
						if (res.isFailed())
						{
							log.info(res.getFailureMessage());
							fieldBulkErrors.add(res.getFailureMessage());

						}
						// Data toupdate = toversion.get(res.getId());

					}
					//	request.refresh(true);
				}

				@Override
				public void afterBulk(long executionId, BulkRequest request, Throwable failure)
				{
					log.info(failure);
					fieldBulkErrors.add(failure);
				}
			}).setBulkActions(-1).setBulkSize(new ByteSizeValue(10, ByteSizeUnit.MB)).setFlushInterval(TimeValue.timeValueMinutes(4)).setConcurrentRequests(1).setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueMillis(100), 10)).build();

		}

		return fieldBulkProcessor;

	}

	public NodeStats getNodeStats()
	{
		NumberFormat nf = NumberFormat.getNumberInstance();
		for (Path root : FileSystems.getDefault().getRootDirectories())
		{

			// System.out.print(root + ": ");
			try
			{
				FileStore store = Files.getFileStore(root);
				// System.out.println("available=" + nf.format(store.getUsableSpace()) + ", total=" + nf.format(store.getTotalSpace()));
			}
			catch (IOException e)
			{
				log.error("error querying space: " + e.toString());
			}
		}

		NodesStatsResponse response = getClient().admin().cluster().prepareNodesStats().setJvm(true).setFs(true).setOs(true).setNodesIds(getLocalNodeId()).setThreadPool(true).get();

		ClusterHealthResponse healths = getClient().admin().cluster().prepareHealth().get();
		for (Iterator iterator = response.getNodesMap().keySet().iterator(); iterator.hasNext();)
		{
			String key = (String) iterator.next();
			NodeStats stats = response.getNodesMap().get(key);
			String id = stats.getNode().getId();
			String name = stats.getNode().getName();
			if (getLocalNodeId().equals(name))
			{
				stats.getJvm().getMem().getHeapMax();
				stats.getJvm().getMem().getHeapUsed();
				stats.getJvm().getMem().getHeapUsedPercent();
				stats.getOs().getCpuPercent();
				stats.getOs().getLoadAverage();
				stats.getOs().getMem().getFree();
				stats.getFs().getTotal().getAvailable();
				stats.getFs().getTotal().getTotal();
				stats.getFs().getTotal().getFree();
				//log.info(response);

				return stats;
			}
		}
		// NodeStats stats = response.getNodesMap().get(getLocalNodeId());

		//		 stats.getJvm().getMem().getHeapCommitted();
		return null;
	}

	public String getClusterHealth()
	{
		String healthstatus = null;

		try
		{
			ClusterHealthResponse healths = getClient().admin().cluster().prepareHealth().get();
			healthstatus = healths.getStatus().toString();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return healthstatus;
	}

	public Collection getRemoteEditClusters(String inCatalog)
	{
		//Not cached
		Collection nodes = getSearcherManager().getSearcher(inCatalog, "editingcluster").getAllHits();
		Collection others = new ArrayList();

		//TODO cache this
		for (Iterator iterator = nodes.iterator(); iterator.hasNext();)
		{
			Data node = (Data) iterator.next();
			String clusterid = node.get("clustername");
			if (!clusterid.equals(getLocalClusterId()))
			{
				others.add(node);
			}
		}
		return others;
	}

	public HitTracker getEditedDocuments(String inCatalogId, Date inAfter)
	{
		SearchRequestBuilder search = null;
		if (inCatalogId != null)
		{
			search = getClient().prepareSearch(toId(inCatalogId));
		}
		else
		{
			//across all?
			search = getClient().prepareSearch();
		}
		search.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
		search.setTypes(synctypes);

		if (inAfter == null)
		{
			throw new OpenEditException("No pulldate set");
		}
		RangeQueryBuilder date = QueryBuilders.rangeQuery("emrecordstatus.recordmodificationdate").from(inAfter);// .to(before);

		search.setQuery(date);
		search.setRequestCache(false);  //What does this do?

		//search.toString()
		ElasticHitTracker hits = new ElasticHitTracker(getClient(), search, date, 1000);
		hits.enableBulkOperations();
		if (getSearcherManager().getShowSearchLogs(inCatalogId))
		{
			String json = search.toString();
			log.info(toId(inCatalogId) + "/_search' -d '" + json + "' \n");
		}
		log.info("Found these changes: " + hits.size() + " since " + inAfter);
		//hits.setSearcherManager(getSearcherManager());
		//hits.setSearcher(this);
		//hits.setSearchQuery(inQuery);
		return hits;
	}
	public HitTracker getDocumentsByIds(String inCatalogId,Collection<String> inIds)
	{
		SearchRequestBuilder search = getClient().prepareSearch(toId(inCatalogId));
		search.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
		search.setTypes(synctypes);

		IdsQueryBuilder ids = QueryBuilders.idsQuery(synctypes);
		ids.addIds(inIds);
		search.setQuery(ids);
		search.setRequestCache(true);  //What does this do?

		ElasticHitTracker hits = new ElasticHitTracker(getClient(), search, ids, 1000);
		hits.enableBulkOperations();
		//hits.setSearcherManager(getSearcherManager());
		//hits.setSearcher(this);
		//hits.setSearchQuery(inQuery);
		return hits;
	}

	public void flushBulk()
	{

		try
		{
			getBulkProcessor().flush();
			getBulkProcessor().awaitClose(5, TimeUnit.MINUTES);
			if (fieldBulkErrors.size() > 0)
			{

				throw new OpenEditException("Error importing bulk data: " + fieldBulkErrors);
			}
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		finally
		{
			fieldBulkErrors.clear();
			fieldBulkProcessor = null;
		}
	}

	@Override
	public void connectoToDatabase()
	{
		try
		{
			connectCatalog("system");
		}
		catch( Throwable ex)
		{
			log.error("Connection not ready yet", ex);
		}
			

		boolean yellowok = false;
		
		do {
			try
			{
				AdminClient admin = getClient().admin();
				ClusterHealthResponse health = admin.cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();

				if (health.isTimedOut())
				{
					log.info("Keep Waiting for yellow status" );
				}
				else
				{
					yellowok = true;
				}

			}
			catch( Throwable ex)
			{
				log.error("Trying to init system catalog", ex); //Should never happen
			}
			
			if( !yellowok )
			{
				try
				{
					Thread.sleep(200);
				}
				catch (Exception ex)
				{
					//Ignore
				}
			}
			
		} while(!yellowok);
		
		log.info("Connected to system catalog");
		
	}

}
