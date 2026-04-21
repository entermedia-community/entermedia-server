package org.entermediadb.ai.automation.agents;

import java.util.Collection;
import java.util.Iterator;
import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.convert.QueueManager;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;

public class QueueConversionsAgent extends BaseAgent
{
	@Override
	public void process(AgentContext inContext)
	{
		Collection<Asset> hits = (Collection<Asset>) inContext.getContextValue("hits");

		Searcher assetsearcher = getMediaArchive().getAssetSearcher();

		if (hits.size() == 0)
		{
			inContext.info("No assets found. " + hits);
			return;
		}
		Searcher tasksearcher = getMediaArchive().getSearcher("conversiontask");
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			try
			{
				Asset asset = (Asset) assetsearcher.loadData(hit);
				getMediaArchive().getPresetManager().queueConversions(getMediaArchive(), tasksearcher, asset);
			}
			catch (Throwable ex)
			{
				inContext.error(hit.getId(), ex);
			}
		}
		super.process(inContext);
		// getMediaArchive().fireSharedMediaEvent("conversions/runconversions");
	}
}
