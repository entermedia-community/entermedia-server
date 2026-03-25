package org.entermediadb.ai.classify.agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.classify.ClassifyManager;
import org.entermediadb.ai.informatics.InformaticsContext;
import org.entermediadb.ai.llm.AgentContext;
import org.openedit.MultiValued;

public class ClassifyAgent extends BaseAgent
{
	public ClassifyManager getClassifyManager()
	{
		ClassifyManager manager = (ClassifyManager)getMediaArchive().getBean("classifyManager");
		return manager;
	}

	public void process(AgentContext inContext)
	{
		InformaticsContext mycontext =  new InformaticsContext(inContext); 
		
		Collection pageofhits = mycontext.getAssetsToProcess();
		if( pageofhits != null && !pageofhits.isEmpty())
		{
			List workinghits = new ArrayList(pageofhits); 
			mycontext.setAssetsToProcess(workinghits);
			getClassifyManager().processAssets(mycontext);
			for (Iterator iterator2 = pageofhits.iterator(); iterator2.hasNext();)
			{
				MultiValued data = (MultiValued) iterator2.next();
				if(data.getBoolean("llmerror"))
				{
					workinghits.remove(data); //We do not process more.
				}
			}
			mycontext.setAssetsToProcess(workinghits);
		}
		else
		{
			mycontext.setAssetsToProcess(Collections.emptyList());
		}

		Collection recordspageofhits = mycontext.getRecordsToProcess();
		if( recordspageofhits != null && !recordspageofhits.isEmpty())
		{
			List workinghits = new ArrayList(recordspageofhits); 
			mycontext.setRecordsToProcess(workinghits);
			getClassifyManager().processRecords(mycontext);
			for (Iterator iterator2 = recordspageofhits.iterator(); iterator2.hasNext();)
			{
				MultiValued data = (MultiValued) iterator2.next();
				if(data.getBoolean("llmerror"))
				{
					workinghits.remove(data); //We do not process more.
				}
			}
			mycontext.setRecordsToProcess(workinghits);
		}
		else
		{
			mycontext.setRecordsToProcess(Collections.emptyList());
		}
		super.process(mycontext);
	}
}
