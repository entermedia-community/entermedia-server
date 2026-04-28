package org.entermediadb.ai;

import org.entermediadb.ai.llm.AgentContext;
import org.openedit.CatalogEnabled;

public interface Skill
{
	public void processstart(AgentContext inContext);

	public void processend(AgentContext inContext);

	void process(AgentContext inContext);
}
