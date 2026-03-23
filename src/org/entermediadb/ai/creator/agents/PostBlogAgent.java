package org.entermediadb.ai.creator.agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.creator.SmartCreatorManager;
import org.entermediadb.ai.informatics.InformaticsContext;
import org.entermediadb.ai.llm.AgentContext;
import org.openedit.MultiValued;

public class PostBlogAgent extends BaseAgent
{
	public SmartCreatorManager getSmartCreatorManager()
	{
		SmartCreatorManager manager = (SmartCreatorManager)getMediaArchive().getBean("smartCreatorManager");
		return manager;
	}
	
	/**
	 * Calls render to html
	 * Attaches and asset version
	 * sends it to a blog?
	 */
	@Override
	public void process(AgentContext inContext)
	{
//		MultiValued entity = inContext.getCurrentEntity();
//		
//		if(entity != null)
//		{
//			getSmartCreatorManager().processRecords(inContext.getScriptLogger(),inContext.getCurrentAgentEnable().getAgentConfig(),pageofhits);;
//			for (Iterator iterator2 = pageofhits.iterator(); iterator2.hasNext();)
//			{
//				MultiValued data = (MultiValued) iterator2.next();
//				if(data.getBoolean("llmerror"))
//				{
//					workinghits.remove(data); //We do not process more.
//				}
//			}
//			mycontext.setRecordsToProcess(workinghits);
//		}
//		super.process(mycontext);
	}
	
}
