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
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkProcessor.Listener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.metadata.AliasOrIndex;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.snapshots.SnapshotInfo;
import org.elasticsearch.transport.RemoteTransportException;
import org.entermediadb.asset.cluster.BaseNodeManager;
import org.entermediadb.asset.search.BaseAssetSearcher;
import org.entermediadb.elasticsearch.searchers.BaseElasticSearcher;
import org.entermediadb.elasticsearch.searchers.ElasticAssetDataConnector;
import org.openedit.OpenEditException;
import org.openedit.Shutdownable;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.Searcher;
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

public class ElasticNodeManager extends BaseNodeManager implements Shutdownable
{
	protected Log log = LogFactory.getLog(ElasticNodeManager.class);

	protected Client fieldClient;
	protected boolean fieldShutdown = false;
	protected List fieldMappingErrors;

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
				NodeBuilder nb = NodeBuilder.nodeBuilder();//.client(client)local(true);

				Page config = getPageManager().getPage("/WEB-INF/node.xml"); //Legacy DO Not use REMOVE sometime
				if (!config.exists())
				{
					//throw new OpenEditException("Missing " + config.getPath());
					config = getPageManager().getPage("/system/configuration/node.xml");
				}

				for (Iterator iterator = getLocalNode().getElement().elementIterator("property"); iterator.hasNext();)
				{
					Element prop = (Element) iterator.next();
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

				fieldClient = nb.node().client(); //when this line executes, I get the error in the other node 
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
		if (value == null)
		{
			return null;
		}
		if (value.startsWith("."))
		{
			value = PathUtilities.resolveRelativePath(value, abs);
		}

		return replace.replace(value, params);
	}

	//called from the lock manager
	public void shutdown()
	{
		if (!fieldShutdown)
		{
			if (fieldClient != null)
			{
				fieldClient.close();
			}
		}
		fieldShutdown = true;
		System.out.println("OpenEditEngine shutdown complete");
	}

	protected String toId(String inId)
	{
		String id = inId.replace('/', '_');
		return id;
	}

	public String createSnapShot(String inCatalogId)
	{
		Lock lock = null;
		try
		{
			lock = getLockManager(inCatalogId).lock("snapshot", "elasticNodeManager");
			return createSnapShot(inCatalogId, lock);
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

		Settings settings = Settings.settingsBuilder().put("location", path).build();
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
		builder.setRepository(indexid).setIndices(indexid).setWaitForCompletion(true).setSnapshot(snapshotid);
		builder.execute().actionGet();

		return snapshotid;
	}

	public String createDailySnapShot(String inCatalogId)
	{
		Lock lock = null;

		try
		{
			lock = getLockManager(inCatalogId).lock("snapshot", "elasticNodeManager");

			List list = listSnapShots(inCatalogId);
			if (list.size() > 0)
			{
				SnapshotInfo recent = (SnapshotInfo) list.iterator().next();
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
				Date date = format.parse(recent.name());
				Calendar yesterday = new GregorianCalendar();
				yesterday.add(Calendar.DAY_OF_YEAR, -1);
				if (date.after(yesterday.getTime()))
				{
					return recent.name();
				}
			}
			return createSnapShot(inCatalogId, lock);
		}
		catch (Throwable ex)
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

		if (!new File(path).exists())
		{
			return Collections.emptyList();
		}
		Settings settings = Settings.settingsBuilder().put("location", path).build();

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
			CloseIndexRequestBuilder closeIndexRequestBuilder = new CloseIndexRequestBuilder(getClient(), CloseIndexAction.INSTANCE);
			closeIndexRequestBuilder.setIndices(indexid);
			closeIndexRequestBuilder.execute().actionGet();

			try
			{
				ClusterHealthResponse health = admin.cluster().prepareHealth(indexid).setWaitForYellowStatus().execute().actionGet();
			}
			catch (Exception ex)
			{
				log.error(ex);
			}

			// Now execute the actual restore action
			RestoreSnapshotRequestBuilder restoreBuilder = new RestoreSnapshotRequestBuilder(getClient(), RestoreSnapshotAction.INSTANCE);
			restoreBuilder.setRepository(indexid).setSnapshot(inSnapShotId);
			restoreBuilder.execute().actionGet();

			try
			{
				ClusterHealthResponse health = admin.cluster().prepareHealth(indexid).setWaitForYellowStatus().execute().actionGet();
			}
			catch (Exception ex)
			{
				log.error(ex);
			}
		}
		catch (Throwable ex)
		{
			log.error(ex);
		}
		admin.indices().open(new OpenIndexRequest(indexid));
		try
		{
			ClusterHealthResponse health = admin.cluster().prepareHealth(indexid).setWaitForYellowStatus().execute().actionGet();
		}
		catch (Exception ex)
		{
			log.info(ex);
		}
	}

	public void exportKnapsack(String inCatalogId)
	{
		Lock lock = null;

		try
		{
			lock = getLockManager(inCatalogId).lock("snapshot", "elasticNodeManager");
			Client client = getClient();
			Date date = new Date();
			Page target = getPageManager().getPage("/WEB-INF/data" + inCatalogId + "snapshots/knapsack-bulk-" + date.getTime() + ".bulk.gz");
			Page folder = getPageManager().getPage(target.getParentPath());
			File file = new File(folder.getContentItem().getAbsolutePath());
			file.mkdirs();
			Path exportPath = Paths.get(URI.create("file:" + target.getContentItem().getAbsolutePath()));
			KnapsackExportRequestBuilder requestBuilder = new KnapsackExportRequestBuilder(client.admin().indices()).setArchivePath(exportPath).setOverwriteAllowed(true).setIndex(inCatalogId);
			KnapsackExportResponse knapsackExportResponse = requestBuilder.execute().actionGet();

			KnapsackStateRequestBuilder knapsackStateRequestBuilder = new KnapsackStateRequestBuilder(client.admin().indices());
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
		catch (Throwable ex)
		{
			throw new OpenEditException(ex);
		}
		finally
		{
			getLockManager(inCatalogId).release(lock);
		}
	}

	public HitTracker listKnapsacks(String inCatalogId)
	{
		List snaps = getPageManager().getChildrenPathsSorted("/WEB-INF/data/" + inCatalogId + "/snapshots/");
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
		Lock lock = null;

		try
		{
			lock = getLockManager(inCatalogId).lock("snapshot", "elasticNodeManager");

			Page target = getPageManager().getPage("/WEB-INF/" + inCatalogId + "snapshots/" + inFile);

			Client client = getClient();
			File exportFile = new File(target.getContentItem().getAbsolutePath());
			Path exportPath = Paths.get(URI.create("file:" + exportFile.getAbsolutePath()));
			KnapsackImportRequestBuilder knapsackImportRequestBuilder = new KnapsackImportRequestBuilder(client.admin().indices()).setArchivePath(exportPath).setIndex(inCatalogId);
			KnapsackImportResponse knapsackImportResponse = knapsackImportRequestBuilder.execute().actionGet();

		}
		catch (Throwable ex)
		{
			throw new OpenEditException(ex);
		}
		finally
		{
			getLockManager(inCatalogId).release(lock);
		}
	}

	public void addMappingError(String inSearchType, String inMessage)
	{
		MappingError error = new MappingError();
		error.setError(inMessage);
		error.setSearchType(inSearchType);
		if(inMessage.contains("Mapper for [")){
			String guessdetail = inMessage.substring("Mapper for [".length(), inMessage.length());
			guessdetail = guessdetail.substring(0, guessdetail.indexOf("]"));
			error.setDetail(guessdetail);
		}
		
		getMappingErrors().add(error);

	}

	public boolean hasMappingErrors()
	{
		return !getMappingErrors().isEmpty();
	}

	public boolean connectCatalog(String inCatalogId)
	{
		if (!getConnectedCatalogIds().contains(inCatalogId))
		{
			getConnectedCatalogIds().add(inCatalogId);

			String alias = toId(inCatalogId);
			String index = getIndexNameFromAliasName(alias);//see if we already have an index  

			if (index == null)
			{
				index = alias + "-0";
			}

			boolean createdIndex = prepareIndex(index);
			if (createdIndex)
			{
				AdminClient admin = getClient().admin();

				admin.indices().prepareAliases().addAlias(index, alias).execute().actionGet();//This sets up an alias that the app uses so we can flip later.
			}
			//			PropertyDetailsArchive archive = getSearcherManager().getPropertyDetailsArchive(inCatalogId);
			//			List sorted = archive.listSearchTypes();
			//			for (Iterator iterator = sorted.iterator(); iterator.hasNext();)
			//			{
			//				String type = (String) iterator.next();
			//				Searcher searcher = getSearcherManager().getSearcher(inCatalogId, type);
			//				searcher.initialize();	
			//			}

		}
		return true;//what does this mean?
	}

	public boolean prepareIndex(String index)
	{

		AdminClient admin = getClient().admin();

		ClusterHealthResponse health = admin.cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();

		if (health.isTimedOut())
		{
			throw new OpenEditException("Could not get yellow status");
		}
		boolean indexexists = false;
		//		AliasOrIndex indexToAliasesMap = admin.cluster().state(Requests.clusterStateRequest()).actionGet().getState().getMetaData().getAliasAndIndexLookup().get(alias);
		//		if(indexToAliasesMap != null && !indexToAliasesMap.isAlias()){
		//			    throw new OpenEditException("An old index exists with the name we want to use for the alias!");
		//		}
		//		if(indexToAliasesMap != null && indexToAliasesMap.isAlias()){
		//		    indexexists = true;//we have an index already with this alias..
		//			   index= indexToAliasesMap.getIndices().iterator().next().getIndex();;
		//		} 

		IndicesExistsRequest existsreq = Requests.indicesExistsRequest(index);
		IndicesExistsResponse res = admin.indices().exists(existsreq).actionGet();
		boolean createdIndex = false;
		if (!res.isExists())
		{
			try
			{
				XContentBuilder settingsBuilder = XContentFactory.jsonBuilder().startObject().startObject("analysis")
				//								.startObject("filter").
				//									startObject("snowball").field("type", "snowball").field("language", "English")
				//									.endObject()
				//								.endObject()
				.startObject("analyzer").startObject("lowersnowball").field("type", "snowball").field("language", "English").endObject().endObject().endObject().endObject();

				CreateIndexResponse newindexres = admin.indices().prepareCreate(index).setSettings(settingsBuilder).execute().actionGet();
				//CreateIndexResponse newindexres = admin.indices().prepareCreate(cluster).execute().actionGet();

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
				throw new OpenEditException(e);
			}
			return createdIndex;
		}

		//TODO: This should have beeb already setup by the NodeManager

		//from this point forward we can use the alias = media_catalogs_public instead of the actual index string..

		//		ClusterState cs = admin.cluster().prepareState().setIndices(alias).execute().actionGet().getState();
		//		IndexMetaData data = cs.getMetaData().index(alias);
		//		if (data != null)
		//		{
		//			if (data.getMappings() != null)
		//			{
		//				MappingMetaData fields = data.getMappings().get(getSearchType());
		//				if (fields != null && fields.source() != null)
		//				{
		//					runmapping = false;
		//				}
		//			}
		//		}
		RefreshRequest req = Requests.refreshRequest(index);
		RefreshResponse rres = admin.indices().refresh(req).actionGet();
		if (rres.getFailedShards() > 0)
		{
			log.error("Could not refresh shards");
		}
		
		return createdIndex;

		//		if(createdIndex){
		//			
		//			initializeCatalog(getCatalogId());
		//		}
		//return runmapping;
	}

	protected String getIndexNameFromAliasName(final String aliasName)
	{
		AliasOrIndex indexToAliasesMap = getClient().admin().cluster().state(Requests.clusterStateRequest()).actionGet().getState().getMetaData().getAliasAndIndexLookup().get(aliasName);

		if (indexToAliasesMap == null)
		{
			return null;
		}

		if (indexToAliasesMap.isAlias() && indexToAliasesMap.getIndices().size() > 0)
		{
			return indexToAliasesMap.getIndices().iterator().next().getIndex();
		}

		return null;
	}

	public boolean reindexInternal(String inCatalogId)
	{
		try
		{
			Date date = new Date();
			String id = toId(inCatalogId);
			String tempindex = id + date.getTime();
			prepareIndex(tempindex);
			//need to reset/creat the mappings here!
			getMappingErrors().clear();
				PropertyDetailsArchive archive = getSearcherManager().getPropertyDetailsArchive(inCatalogId);
				List sorted = archive.listSearchTypes();
				for (Iterator iterator = sorted.iterator(); iterator.hasNext();)
				{
					String type = (String) iterator.next();
					Searcher searcher = getSearcherManager().getSearcher(inCatalogId, type);
					//searcher.reIndexAll();
					if (searcher instanceof BaseElasticSearcher)
					{
						BaseElasticSearcher new_name = (BaseElasticSearcher) searcher;

							new_name.putMappings(tempindex);

						
					}
					
					if (searcher instanceof BaseAssetSearcher)
					{
						BaseAssetSearcher new_name = (BaseAssetSearcher) searcher;
							ElasticAssetDataConnector con = (ElasticAssetDataConnector) new_name.getDataConnector();
							con.putMappings(tempindex);

						
					}
					
					
					
				}

			if(!getMappingErrors().isEmpty()){
				return false;
			}
				
				
			
			SearchResponse searchResponse = getClient().prepareSearch(id).setQuery(QueryBuilders.matchAllQuery()).setSearchType(SearchType.SCAN).setScroll(new TimeValue(600000)).setSize(500).execute().actionGet();

			BulkProcessor bulkProcessor = BulkProcessor.builder(getClient(), createLoggingBulkProcessorListener()).setBulkActions(10000).setConcurrentRequests(2).setFlushInterval(TimeValue.timeValueSeconds(5)).build();

			while (true)
			{
				searchResponse = getClient().prepareSearchScroll(searchResponse.getScrollId()).setScroll(new TimeValue(600000)).execute().actionGet();

				if (searchResponse.getHits().getHits().length == 0)
				{
					bulkProcessor.flush();

					bulkProcessor.close();
					break; //Break condition: No hits are returned
				}

				for (SearchHit hit : searchResponse.getHits())
				{
					IndexRequest request = new IndexRequest(tempindex, hit.type(), hit.id());
					request.source(hit.sourceAsString());
					bulkProcessor.add(request);
				}
			}
			String oldindex = getIndexNameFromAliasName(id);

			getClient().admin().indices().prepareAliases().removeAlias(oldindex, id).addAlias(tempindex, id).execute().actionGet();
			DeleteIndexResponse response = getClient().admin().indices().delete(new DeleteIndexRequest(oldindex)).actionGet();
			log.info("Dropped: " + response.isAcknowledged());
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		return true;

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

}
