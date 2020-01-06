package org.entermediadb.asset.pull;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.elasticsearch.SearchHitData;
import org.entermediadb.net.HttpSharedConnection;
import org.openedit.Data;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.node.NodeManager;
import org.openedit.util.OutputFiller;

public abstract class BasePuller
{
	protected SearcherManager fieldSearcherManager;
	protected NodeManager fieldNodeManager;
	protected String fieldCatalogId;
	OutputFiller filler = new OutputFiller();
	private static final Log log = LogFactory.getLog(BasePuller.class);

	
	
	protected HitTracker removeRemotesMasterNodeEdits(String masterNodeId, HitTracker inLocalchanges)
	{
		HitTracker finallist = new ListHitTracker();
		
		for (Iterator iterator = inLocalchanges.iterator(); iterator.hasNext();)
		{
			SearchHitData hit = (SearchHitData) iterator.next();
			Map localrecordstatus = (Map) hit.getSearchHit().getSource().get("emrecordstatus");
			if( localrecordstatus == null)
			{
				continue;
			}
			String remotemasterclusterid = (String) localrecordstatus.get("mastereditclusterid");
			String remotelastmodifiedclusterid = (String) localrecordstatus.get("lastmodifiedclusterid");
	
			if (masterNodeId.equals(remotemasterclusterid) && remotemasterclusterid.equals(remotelastmodifiedclusterid))
			{
				continue; //This is an identical record to what we have						
			}
			finallist.add(hit);
		}
	
		return finallist;
	}

	protected HttpSharedConnection createConnection(Data node)
	{
		HttpSharedConnection connection = new HttpSharedConnection();
		connection.addSharedHeader("X-token", node.get("entermediakey"));
		connection.addSharedHeader("X-tokentype", "entermedia");
		return connection;
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

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public void setNodeManager(NodeManager inNodeManager)
	{
		fieldNodeManager = inNodeManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public NodeManager getNodeManager()
	{
		return fieldNodeManager;
	}

	protected Data loadMasterDataForAsset(MediaArchive inArchive, Asset inAsset)
	{
		Map emEditStatus = inAsset.getEmRecordStatus();
		String clusterid = (String) emEditStatus.get("mastereditclusterid");
		String localClusterId = inArchive.getNodeManager().getLocalClusterId();
	
		if (localClusterId.equals(clusterid))
		{
			log.info("This is our own asset, nothing to do");
			return null;
		}
		Data node = (Data) inArchive.getSearcher("editingcluster").searchByField("clustername", clusterid);
		if (node == null)
		{
			log.info("Cannot find information for : " + clusterid + " so cannot download asset " + inAsset.getId());
			return null;
		}
		return node;
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

}
