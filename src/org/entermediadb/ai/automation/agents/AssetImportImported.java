package org.entermediadb.ai.automation.agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.asset.Asset;

import model.assets.AssetTypeManager;

public class AssetImportImported extends BaseAgent {
     public void process(AgentContext inContext)
     {
        Collection<Asset> hits = (Collection<Asset>)inContext.getContextValue("hits");

		if( hits == null)
		{
			inContext.error("No hits found");
			return;
		}

		for (Iterator iterator = hits.iterator(); iterator.hasNext();) 
		{
			Asset newasset = (Asset) iterator.next();
			newasset.setValue("importstatus", "needsmetadata"); //Will be saved at bottom
		}
		
		inContext.info(hits.size() + " asset" + (hits.size() == 1 ? "" : "s") + " imported");
		
		//Set the asset type
		AssetTypeManager manager = new AssetTypeManager();
		manager.setLog(inContext.getScriptLogger());
		manager.setAssetTypes(getMediaArchive(), hits, true); 

		//save everything
		List tosave = new ArrayList(); //Might be a hit tracker
		for (Iterator iterator = hits.iterator(); iterator.hasNext();) {
			Asset asset = (Asset) iterator.next();
			tosave.add(asset);
		}

		getMediaArchive().getAssetSearcher().saveAllData(tosave, null);
		
        super.process(inContext);
     }
}
