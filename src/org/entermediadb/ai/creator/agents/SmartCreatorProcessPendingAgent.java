package org.entermediadb.ai.creator.agents;

import java.util.Collection;
import java.util.Iterator;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.creator.AiSmartCreatorSteps;
import org.entermediadb.ai.creator.SmartCreatorManager;
import org.entermediadb.ai.llm.AgentContext;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;

public class SmartCreatorProcessPendingAgent extends BaseAgent
{

	
	public SmartCreatorManager getSmartCreatorManager()
	{
		SmartCreatorManager smartCreatorManager = (SmartCreatorManager) getMediaArchive().getBean("smartCreatorManager");
		return smartCreatorManager;
	}
	@Override
	public void process(AgentContext inContext)
	{
		
		Collection<String> values = inContext.getCurrentAgentEnable().getAutomationEnabledData().getValues("searchtypes");
		for (Iterator iterator1 = values.iterator(); iterator1.hasNext();)
		{
			String moduleid = (String) iterator1.next();
			
			MultiValued module = (MultiValued)getMediaArchive().getCachedData("module", moduleid);
			
			if (module == null)
			{
				inContext.error("No module found " + inContext.getCurrentAgentEnable().getAutomationEnabledData());
				continue;
			}
			//Find pending then process each one
			Collection found = getMediaArchive().query(module.getId()).exact("processingstatus","new").search();
			//Then save?
			
			String llmprompt = inContext.getCurrentAgentEnable().getAutomationEnabledData().get("llmprompt");
			
			AiSmartCreatorSteps instructions = new AiSmartCreatorSteps(); //Fresh
			instructions.setTargetModule(module);
			inContext.setAiSmartCreatorSteps(instructions);
			
			getSmartCreatorManager().parseCreationPrompt(inContext, llmprompt);
			
			for (Iterator iterator = found.iterator(); iterator.hasNext();)
			{
				MultiValued entity = (MultiValued) iterator.next();
				AgentContext childcontext = new AgentContext(inContext);
				childcontext.setCurrentEntityModule( module );
				childcontext.setCurrentEntity(entity);
				super.process(childcontext); //To Create outline
				
				entity.setValue("processingstatus","complete");
				getMediaArchive().saveData(module.getId(), entity);
			}
		}
	}
}
