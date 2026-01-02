package org.entermediadb.ai;

import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.MultiValued;

public interface ChatMessageHandler
{
	public LlmResponse processMessage(AgentContext inAgentContext,MultiValued message, MultiValued inAiFunction);

}
