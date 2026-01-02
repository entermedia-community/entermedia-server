package org.entermediadb.ai;

import java.util.Collection;

import org.entermediadb.ai.assistant.SemanticAction;
import org.entermediadb.ai.informatics.SemanticTableManager;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.MultiValued;

public interface ChatMessageHandler
{
	public LlmResponse processMessage(AgentContext inAgentContext,MultiValued message, MultiValued inAiFunction);

	public void indexPossibleFunctionParameters(ScriptLogger inLog);

}
