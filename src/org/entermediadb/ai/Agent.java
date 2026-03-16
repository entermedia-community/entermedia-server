package org.entermediadb.ai;

import org.entermediadb.ai.llm.AgentContext;
import org.openedit.CatalogEnabled;

public interface Agent {

	void process(AgentContext inContext);

    
} 