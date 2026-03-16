package org.entermediadb.translator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.informatics.InformaticsContext;
import org.entermediadb.ai.llm.AgentContext;
import org.openedit.MultiValued;

public class TranslationAgent extends BaseAgent
{
	public TranslationManager getTranslationManager()
	{
		TranslationManager manager = (TranslationManager)getMediaArchive().getBean("translationManager");
		return manager;
	}
	@Override
	public void process(AgentContext inContext)
	{
		InformaticsContext mycontext =  new InformaticsContext(inContext);
		
		Collection<MultiValued> pageofhits = mycontext.getRecordsToProcess();
		if( pageofhits != null && !pageofhits.isEmpty())
		{
			List workinghits = new ArrayList(pageofhits);
			mycontext.setRecordsToProcess(workinghits);
			MultiValued config = mycontext.getCurrentAgentEnable().getAgentConfig();
			getTranslationManager().translateDataFields( inContext.getScriptLogger(), config, pageofhits);
			for (Iterator iterator2 = pageofhits.iterator(); iterator2.hasNext();)
			{
				MultiValued data = (MultiValued) iterator2.next();
				if(data.getBoolean("llmerror"))
				{
					workinghits.remove(data); //We do not process more.
				}
			}
			mycontext.setRecordsToProcess(workinghits);
		}
		super.process(mycontext);
	}
	
}
