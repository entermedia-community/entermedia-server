package org.entermediadb.ai.creator.agents;

import java.util.Collection;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.assistant.AssistantManager;
import org.entermediadb.ai.creator.SmartCreatorManager;
import org.entermediadb.ai.llm.AgentContext;
import org.openedit.Data;

public class SmartCreatorCreateOutlineAgent extends BaseAgent
{

	public SmartCreatorManager getSmartCreatorManager()
	{
		SmartCreatorManager smartCreatorManager = (SmartCreatorManager) getMediaArchive().getBean("smartCreatorManager");
		return smartCreatorManager;
	}
	@Override
	public void process(AgentContext inContext)
	{
		Data module = inContext.getCurrentEntityModule();
		Data entity = inContext.getCurrentEntity();
		
		AssistantManager assistant = (AssistantManager) getMediaArchive().getBean("assistantManager");
		getSmartCreatorManager().createOutLine(inContext, inContext.getAiSmartCreatorSteps());
		getSmartCreatorManager().initConfirmedSections( inContext.getAiSmartCreatorSteps());

		
		super.process(inContext);
		
	}
}
