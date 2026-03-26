package org.entermediadb.ai.creator.agents;

import java.util.Collection;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.assistant.AssistantManager;
import org.entermediadb.ai.creator.AiSmartCreatorSteps;
import org.entermediadb.ai.creator.SmartCreatorManager;
import org.entermediadb.ai.llm.AgentContext;
import org.openedit.Data;

public class SmartCreatorMakeSectionsAgent extends BaseAgent
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
		//getSmartCreatorManager().populateSectionsWithContents(inContext);
		AiSmartCreatorSteps instructions = inContext.getAiSmartCreatorSteps();
		getSmartCreatorManager().createSections(inContext, instructions);

		super.process(inContext);
		
	}
}
