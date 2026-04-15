package org.entermediadb.ai.classify.agents;

import java.util.Collection;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.classify.SemanticClassifierManager;
import org.entermediadb.ai.informatics.InformaticsContext;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.asset.Asset;
import org.openedit.MultiValued;

public class SemanticClassifierAgent extends BaseAgent
{
	public SemanticClassifierManager getSemanticClassifierManager()
	{
		SemanticClassifierManager manager = (SemanticClassifierManager) getMediaArchive().getBean("semanticClassifierManager");
		return manager;
	}

	@Override
	public void process(AgentContext inContext)
	{
		// MultiValued inConfig, Collection<MultiValued> inRecords
		InformaticsContext informatic = new InformaticsContext(inContext);
		MultiValued inConfig = inContext.getCurrentAgentEnable().getAgentData();
		// Try both assets and records
		Collection<Asset> inAssets = informatic.getAssetsToProcess();
		long startTime = System.currentTimeMillis();
		if (inAssets != null && !inAssets.isEmpty())
		{
			getSemanticClassifierManager().processRecords(informatic.getScriptLogger(), inConfig, inAssets);
			long duration = System.currentTimeMillis() - startTime;
			informatic.info("Classified: " + inAssets.size() + " assets took " + (duration > 1000L ? duration / 1000L + "s" : duration + " ms"));
		}
		Collection<MultiValued> inRecords = informatic.getRecordsToProcess();
		if (inRecords != null && !inRecords.isEmpty())
		{
			getSemanticClassifierManager().processRecords(informatic.getScriptLogger(), inConfig, inRecords);
			long duration = System.currentTimeMillis() - startTime;
			informatic.info("Classified: " + inRecords.size() + " records took " + (duration > 1000L ? duration / 1000L + "s" : duration + " ms"));
		}
		super.process(informatic);
	}

}
