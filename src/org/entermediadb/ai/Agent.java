package org.entermediadb.ai;

import org.entermediadb.ai.llm.AgentContext;
import org.openedit.CatalogEnabled;

public interface Agent 
{
	public void processstart(AgentContext inContext);
	public void processend(AgentContext inContext);
	void process(AgentContext inContext);
} 