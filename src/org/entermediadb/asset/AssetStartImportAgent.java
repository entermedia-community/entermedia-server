package org.entermediadb.asset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.scripts.ScriptLogger;

import model.assets.AssetTypeManager;

public class AssetStartImportAgent extends BaseAgent
{
	
	/**
	 * 
	 	<path-action name="AssetImportModule.importAssets" />
	
	<path-action name="PathEventModule.runEvent" runpath="/${catalogid}/events/importing/assetsreadmetadata.html"  allowduplicates="true"/>
	<path-action name="PathEventModule.runEvent" runpath="/${catalogid}/events/importing/assetsimportedcustom.html"  allowduplicates="true"/>
	
	<path-action name="EntityModule.handleAssetsImported" alltypes="true"  />
	
	<path-action name="PathEventModule.runEvent" runpath="/${catalogid}/events/importing/queueconversions.html"  allowduplicates="true"/>
	<path-action name="PathEventModule.runSharedEvent" runpath="/${catalogid}/events/conversions/runconversions.html"  allowduplicates="true"/>	

	 * 
	 */

	@Override
	public void process(AgentContext inContext)
	{
		
		MediaArchive archive = getMediaArchive(inReq);
		Collection<Asset> hits = (Collection<Asset>)inReq.getPageValue("hits");
		if( hits == null)
		{
			log.error("No hits found");
			return;
		}

		for (Iterator iterator = hits.iterator(); iterator.hasNext();) 
		{
			Asset newasset = (Asset) iterator.next();
			newasset.setValue("importstatus", "needsmetadata"); //Will be saved at bottom
		}
		
		//Set the asset type
		AssetTypeManager manager = new AssetTypeManager();
		manager.setContext(inReq);
		ScriptLogger logger = (ScriptLogger)inReq.getPageValue("log");
		manager.setLog(logger);
		manager.setAssetTypes(hits, true); 

		//save everything
		List tosave = new ArrayList(); //Might be a hit tracker
		for (Iterator iterator = hits.iterator(); iterator.hasNext();) {
			Asset asset = (Asset) iterator.next();
			tosave.add(asset);
		}
		archive.getAssetSearcher().saveAllData(tosave, inReq.getUser());
		inReq.putPageValue("hits", tosave);
		
		super.process(inContext);
	}
}
