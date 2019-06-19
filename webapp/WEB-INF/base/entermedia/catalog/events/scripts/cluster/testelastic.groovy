package cluster

import org.elasticsearch.search.SearchHit
import org.entermediadb.asset.MediaArchive
import org.entermediadb.elasticsearch.ElasticNodeManager
import org.entermediadb.elasticsearch.SearchHitData
import org.openedit.hittracker.HitTracker

public void runit()
{
	ElasticNodeManager nodeManager = moduleManager.getBean("elasticNodeManager");
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	
	
	HitTracker docs = nodeManager.getAllDocuments(null, new Date());

	
	docs.each{
		SearchHitData hit = it;
		SearchHit sh = hit.getSearchHit();
	    log.info(sh.getIndex());
		log.info(sh.getType());
		nodeManager.getAliasForIndex(sh.getIndex());
		
		log.info(it.toJsonString());
	}
	
}

runit();

