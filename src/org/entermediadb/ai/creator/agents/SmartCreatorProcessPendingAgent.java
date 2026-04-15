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
		if (values == null)
		{
			inContext.error("No Agents Enabled");
			return;
		}
		for (Iterator iterator1 = values.iterator(); iterator1.hasNext();)
		{
			String moduleid = (String) iterator1.next();

			MultiValued module = (MultiValued) getMediaArchive().getCachedData("module", moduleid);

			if (module == null)
			{
				// inContext.error("No module found " +
				// inContext.getCurrentAgentEnable().getAutomationEnabledData());
				continue;
			}
			// Find pending then process each one
			Collection found = getMediaArchive().query(module.getId()).exact("processingstatus", "new").search();
			// Then save?

			String llmprompt = inContext.getCurrentAgentEnable().getAutomationEnabledData().get("llmprompt");

			inContext.info("Found " + found.size() + " records in " + inContext.getCurrentAgentEnable().getAutomationEnabledData());

			for (Iterator iterator = found.iterator(); iterator.hasNext();)
			{
				long startTime = System.currentTimeMillis();
				MultiValued entity = (MultiValued) iterator.next();
				inContext.setCurrentEntityModule(module);
				inContext.setCurrentEntity(entity);

				inContext.put("data", entity);
				llmprompt = getMediaArchive().getReplacer().replace(llmprompt, inContext.getContext());

				inContext.info("Processing: " + llmprompt);

				AiSmartCreatorSteps instructions = new AiSmartCreatorSteps(); // Fresh
				instructions.setTargetModule(module);
				instructions.setTargetEntity(entity);

				inContext.setAiSmartCreatorSteps(instructions);

				getSmartCreatorManager().parseCreationPrompt(inContext, llmprompt);

				super.process(inContext); // To Create outline

				entity.setValue("processingstatus", "complete");
				getMediaArchive().saveData(module.getId(), entity);
				long duration = (System.currentTimeMillis() - startTime) / 1000L;
				inContext.info("Finished processing in " + duration + "s: " + entity.getName());
			}
		}
	}
}
