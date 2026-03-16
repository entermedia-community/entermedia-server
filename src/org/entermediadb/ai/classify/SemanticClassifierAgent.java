package org.entermediadb.ai.classify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.informatics.InformaticsContext;
import org.entermediadb.ai.llm.AgentContext;
import org.openedit.MultiValued;

public class SemanticClassifierAgent extends BaseAgent
{
	public SemanticClassifierManager getSemanticClassifierManager()
	{
		SemanticClassifierManager manager = (SemanticClassifierManager)getMediaArchive().getBean("semanticClassifierManager");
		return manager;
	}
	@Override
	public void process(AgentContext inContext)
	{
		//MultiValued inConfig, Collection<MultiValued> inRecords
		InformaticsContext informatic = new InformaticsContext(inContext);
		MultiValued inConfig = inContext.getCurrentAgentEnable().getAgentConfig();
		//Try both assets and records
		Collection<MultiValued>  inAssets = informatic.getAssetsToProcess();
		if( inAssets != null)
		{
			getSemanticClassifierManager().processRecords(informatic, inConfig, inAssets);
		}
		Collection<MultiValued>  inRecords  = informatic.getRecordsToProcess();
		if( inRecords != null)
		{
			getSemanticClassifierManager().processRecords(informatic, inConfig, inRecords);
		}
		super.process(informatic);
	}
	
}
