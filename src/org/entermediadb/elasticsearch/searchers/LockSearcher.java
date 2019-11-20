package org.entermediadb.elasticsearch.searchers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.openedit.hittracker.HitTracker;

public class LockSearcher extends BaseElasticSearcher 
{
	private static final Log log = LogFactory.getLog(LockSearcher.class);

	protected boolean fieldClearIndexOnStart;

	
	/**
	 * @override
	 */
	protected boolean isTrackEdits()
	{
		return false;
	}
	
	public boolean isClearIndexOnStart()
	{
		return fieldClearIndexOnStart;
	}

	public void setClearIndexOnStart(boolean inClearIndexOnStart)
	{
		fieldClearIndexOnStart = inClearIndexOnStart;
	}
	
//	@Override
//	protected void connect()
//	{
//		if( !isConnected() )
//		{
//			super.connect();
//
//			clearStaleLocks();
//
//		}
//		else
//		{
//			super.connect();
//		}
//		
//	}

	//TODO: move this to the ClientPool shutdown ruitine
	public void clearStaleLocks()
	{
		String id = getElasticNodeManager().getLocalNodeId();
		
		HitTracker hits = query().exact("nodeid", id).search();
		deleteAll(hits, null);
		log.info("Deleted nodeid=" + id + " size" + hits.size() + " records from " + getSearchType() + " table");
		
		
	}

	@Override
	public boolean initialize()
	{
		boolean init = super.initialize();
		clearStaleLocks();
		return init;
	}
	
//	public void shutdown()
//	{
//		if( isConnected() )
//		{
//			clearStaleLocks();
//		}
//		if (fieldElasticNodeManager != null)
//		{
//			fieldElasticNodeManager.shutdown();
//			fieldConnected = false;
//			fieldElasticNodeManager = null;
//		}
//	}
}
