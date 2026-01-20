package org.entermediadb.ai.assistant;

import java.util.Collection;

import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.ChatMessageHandler;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
import org.openedit.MultiValued;

public class TutorialsManager extends BaseAiManager implements ChatMessageHandler
{
	@Override
	public LlmResponse processMessage(AgentContext inAgentContext, MultiValued inAgentMessage, MultiValued inAiFunction)
	{
		Data channel = inAgentContext.getChannel();
		String agentFn = inAgentContext.getFunctionName();
		if("startTutorials".equals(agentFn))
		{
			
		}
		return null;
	}

	@Override
	public Collection<SemanticAction> createPossibleFunctionParameters(ScriptLogger inLog) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void savePossibleFunctionSuggestions(ScriptLogger inLog)
	{
		savePossibleFunctionSuggestions(inLog, "Tutorial");
	}
	
	

}
