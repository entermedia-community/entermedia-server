package org.entermediadb.ai.automation.agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.asset.Asset;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;

public  class AssetImportCreated extends BaseAgent {
    
    public void process(AgentContext inContext)
    {
        WebPageRequest request =  (WebPageRequest)inContext.getContextValue("webpagerequest");

        HitTracker mhits = getMediaArchive().query("asset").exact("importstatus", "modified").search();
        Searcher asssetsearcher = getMediaArchive().getAssetSearcher();
		Collection<Asset> massets = new ArrayList(mhits.size());
		for (Iterator iterator = mhits.iterator(); iterator.hasNext();) {
			Data hit = (Data) iterator.next();
			Asset asset = (Asset)asssetsearcher.loadData(hit);
			getMediaArchive().removeGeneratedImages(asset,true);
			massets.add(asset);
		}
		if(!massets.isEmpty()) {
			inContext.put("hits", massets);
            request.putPageValue("currentagentcontext",inContext);
            super.process(inContext);
			//archive.firePathEvent("importing/importassets",inReq.getUser(),massets);
		}

		HitTracker hits = getMediaArchive().query("asset").exact("importstatus", "created").search();
		Collection<Asset> assets = new ArrayList(1000);
		for (Iterator iterator = hits.iterator(); iterator.hasNext();) {
			Data hit = (Data) iterator.next();
			Asset asset = (Asset)asssetsearcher.loadData(hit);
			assets.add(asset);
			if( assets.size() == 1000 )
			{
				inContext.put("hits", assets);
        
                request.putPageValue("currentagentcontext",inContext);
                super.process(inContext);
				//archive.firePathEvent("importing/importassets",inReq.getUser(),assets);
				assets = new ArrayList(1000);
			}
		}
		inContext.put("hits", assets);
        
        request.putPageValue("currentagentcontext",inContext);
        super.process(inContext);
		//archive.firePathEvent("importing/importassets",inReq.getUser(),assets);
    }
}
