package org.entermediadb.ai.informatics;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.classify.DocumentSplitterManager;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.asset.Asset;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.QueryBuilder;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;

/**
 * My plan is to have a UI where each Task can be seen and assigned to a Agent. 
 * The Agent will be responsible for executing the task and updating the status of the task. 
 * The TaskManager will be responsible for scheduling the tasks and keeping track of the status of the tasks. 
 * The TaskManager will also be responsible for providing a UI for the tasks and allowing users to interact with the tasks.
 * A Task can be a big one or small ones. For example, a big task can be "Classify all assets in the system" and a small task can be "Classify asset 12345".
 * The TaskManager will be responsible for breaking down big tasks into smaller tasks and scheduling them accordingly.
 * 
 * A task can retry a few times if it fails. It will have a retry count and a retry delay. The TaskManager will be responsible for retrying the task if it fails and updating the status of the task accordingly.
 * 
 * Tasks will have Steps that are connected to AI Functions. The functions have their own configuration. The TaskManager will be responsible for executing the steps in order and passing the output of one step to the next step. The TaskManager will also be responsible for handling errors and retrying steps if they fail.
 * 
 * Once the Tasks are identified there will be a set of Agents that look over the tasks and execute them. The Agents will be responsible for executing the task and updating the status of the tasks.
 * 
 */

public class InformaticsProcessorAgent extends BaseAgent
{
	private static final Log log = LogFactory.getLog(InformaticsProcessorAgent.class);
	
	
	public InformaticsProcessorManager getInformaticsProcessorManager()
	{
		InformaticsProcessorManager manager = (InformaticsProcessorManager)getMediaArchive().getBean("informaticsProcessorManager");
		return manager;
	}

	
	public void process(AgentContext inContext)
	{
		InformaticsContext informatic = null;
		if( inContext instanceof InformaticsContext)
		{
			informatic = (InformaticsContext)inContext;
		}
		else
		{
			//TODO Just load up the assets first
			HitTracker pendingassets = getInformaticsProcessorManager().findPendingAssets(informatic);
			informatic.setAssetsToProcess(pendingassets);
			
			HitTracker pendingrecords = getInformaticsProcessorManager().findPendingRecords(informatic);
			informatic.setRecordsToProcess(pendingrecords);
		}
		processPendingAssets(informatic); //Calls the childten
		processPendingRecords(informatic); //Calls the childten
	}
	
	public void processPendingAssets(InformaticsContext inContext)
	{
		inContext.info("Processing Assets Informatics");

		HitTracker inPendingAssets = (HitTracker)inContext.getAssetsToProcess();
		if (!inPendingAssets.isEmpty())
		{
			//inLog.info("Adding metadata to: " + pendingrecords.size() + " assets in category: " + categoryid + ", added after: " + startdate);

			for (int i = 0; i < inPendingAssets.getTotalPages(); i++)
			{
				inPendingAssets.setPage(i+1);
				List pageofhits = new ArrayList();
				try
				{
					long startTime = System.currentTimeMillis();
					for (Iterator iterator = inPendingAssets.getPageOfHits().iterator(); iterator.hasNext();)
					{
						Data data = (Data) iterator.next();
						Asset asset = (Asset)getMediaArchive().getAssetSearcher().loadData(data);
						pageofhits.add(asset);
					}
					
					processAssets(inContext, pageofhits);
					
					long duration = (System.currentTimeMillis() - startTime) / 1000L;
					inContext.info("Processing " + pageofhits.size() + " records took "+duration +"s");
				}
				catch(Exception e)
				{
					inContext.error(e);
					getMediaArchive().saveData("asset", pageofhits);
					
					if (e instanceof OpenEditException) 
					{
						throw (OpenEditException) e;
					}
					throw new OpenEditException(e);
					
				}
			}
		}
		else
		{
			inContext.info("No Assets to Process:` " + inPendingAssets.getFriendlyQuery());
			//inLog.info("AI assets to tag:` " + pendingrecords.getFriendlyQuery());
		}
	}


	
	protected void processAssets(InformaticsContext inContext, List pageofhits)
	{
		//Lock Assets
		User agent = getMediaArchive().getUser("agent");
		for (Iterator iterator = pageofhits.iterator(); iterator.hasNext();)
		{
			Asset asset = (Asset) iterator.next();
			asset.lock(true, agent);
		}
		getMediaArchive().saveData("asset", pageofhits);
		
		HitTracker workinghits = new ListHitTracker(pageofhits);
		try
		{
			InformaticsContext subcontext = new InformaticsContext(inContext);
			subcontext.setAssetsToProcess(workinghits);
			super.process(subcontext);
			
			getMediaArchive().saveData("asset", pageofhits); //Not need it?
		}
		catch(Throwable e)
		{
			//This should never happen
			inContext.error("Processing Informatics Error", e);
			return;
		}
		
		for (Iterator iterator = pageofhits.iterator(); iterator.hasNext();)
		{
			Asset asset = (Asset) iterator.next();
			if(!asset.getBoolean("llmerror"))
			{
				asset.setValue("taggedbyllm", true);
			}
			asset.lock(false, agent); //Todo: Implement Release
		}

		getMediaArchive().saveData("asset", pageofhits);
	
		inContext.info("Processing Informatics on Assets Complete");

	}
	protected void processPendingRecords(InformaticsContext inContext)
	{
		HitTracker pendingrecords = (HitTracker)inContext.getRecordsToProcess();
		
		Collection validhits = new ArrayList();
		if (!pendingrecords.isEmpty())
		{
			//inLog.info("Adding metadata to: " + pendingrecords);

			for (int i = 0; i < pendingrecords.getTotalPages(); i++)
			{
				pendingrecords.setPage(i+1);
				Collection pageofhits = pendingrecords.getPageOfHits();
				
				validhits = getInformaticsProcessorManager().findValidRecords(pendingrecords.getPageOfHits());
				
				if (validhits.isEmpty())
				{
					//inContext.info("No Entities to Process in modules: " + ids + "  | Search Query: "+ pendingrecords.getFriendlyQuery());
					continue;
				}

				Collection workinghits = new ArrayList(validhits);
				try
				{
					InformaticsContext subcontext = new InformaticsContext(inContext);
					subcontext.setRecordsToProcess(workinghits);
					super.process(subcontext);
					
					getMediaArchive().saveData("asset", pageofhits); //Not need it?
				}
				catch(Throwable e)
				{
					//This should never happen
					inContext.error("Processing Informatics Error", e);
					return;
				}

				Map<String, Collection> groupbymodule = getInformaticsProcessorManager().groupByModule(validhits);
				for (Iterator iterator = groupbymodule.keySet().iterator(); iterator.hasNext();)
				{
					String moduleid = (String) iterator.next();
					Collection tosave = groupbymodule.get(moduleid);
					getMediaArchive().saveData(moduleid,tosave);
					
				}

			}
		}
		if (validhits.isEmpty())
		{
			inContext.info("No records found");
		}
		else
		{
			inContext.info("Processing " + validhits.size() +" Entities Informatics Complete.");	
		}
	}
	
}
