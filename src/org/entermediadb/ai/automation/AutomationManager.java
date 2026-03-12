package org.entermediadb.ai.automation;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.QueryBuilder;
import org.openedit.hittracker.HitTracker;
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

public class AutomationManager extends BaseAiManager
{
	
	private static final Log log = LogFactory.getLog(AutomationManager.class);
	
	public void processTasks(ScriptLogger inLog)
	{		
		HitTracker allmodules = getMediaArchive().query("module").exact("semanticenabled", true).search();
		Collection<String> ids = allmodules.collectValues("id");
		
		if(!ids.isEmpty())
		{
			ids.remove("asset");
		}
		if(ids.isEmpty())
		{
			inLog.info("No modules with semantic enabled found.");
			return;
		}
		
		inLog.info("Processing Entities Informatics");
		
		QueryBuilder query = null;
		String allowclassifyothernodes = getMediaArchive().getCatalogSettingValue("allowclassifyothernodes");
		if (Boolean.valueOf(allowclassifyothernodes) )
		{
			//Classify assets from other nodes
			query = getMediaArchive().query("modulesearch");	
		}
		else {
			//Scan only local entities
			query = getMediaArchive().localQuery("modulesearch");
		}
				//.exact("semantictopicsindexed", false)
				//.missing("semantictopics")
		query.exact("taggedbyllm", false)
				.exact("llmerror", false)
				.put("searchtypes", ids);

		String startdate = getMediaArchive().getCatalogSettingValue("ai_metadata_startdate");
		Date date = null;
		if (startdate == null || startdate.isEmpty())
		{
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_YEAR, -30);
			Date thirtyDaysAgo = cal.getTime();
			date = DateStorageUtil.getStorageUtil().parseFromObject(thirtyDaysAgo);
		}
		else {
			date = DateStorageUtil.getStorageUtil().parseFromStorage(startdate);
		}
		query.after("entity_date", date);

		//inLog.info("Running entity search query: " + query);

		HitTracker pendingrecords = query.search();
		pendingrecords.enableBulkOperations();
		pendingrecords.setHitsPerPage(5); //TODO:

		//inLog.info("Entities  " + ids + " with " + pendingrecords + " from date: " + date );

	
	}
	
	
	

	private Collection findValidRecords(List inPageOfHits)
	{
		// TODO Auto-generated method stub
		Collection valid = new ArrayList();
		for (Iterator iterator = inPageOfHits.iterator(); iterator.hasNext();)
		{
			MultiValued entity = (MultiValued) iterator.next();
			String searchtype = entity.get("entitysourcetype");
			String assetid = entity.get("primarymedia");
			if(assetid == null)
			{
				assetid = entity.get("primaryimage");
			}
			if (assetid != null || entity.get("markdowncontent") != null || entity.get("longcaption") != null || entity.get("collectivedescription") != null )
			{
				valid.add(entity);
			}
			else
			{
				//add Smart Creator enabled entities
				
				if(searchtype != null)
				{
					Data module = getMediaArchive().getCachedData("module", searchtype);
					String method = module.get("aicreationmethod");
					if(module != null && "smartcreator".equals(method))
					{
						valid.add(entity);
					}
				}
			}
		}
		return valid;
	}

	public Agent loadAgent(String inName)
	{
		if(inName == null)
		{
			throw new IllegalArgumentException("Bean name not provided");
		}
		Agent Agent = (Agent) getMediaArchive().getCacheManager().get("ai", "Agent" + inName);
		if (Agent == null)
		{
			Agent = (Agent) getModuleManager().getBean(getCatalogId(), inName );
			getMediaArchive().getCacheManager().put("ai", "Agent" + inName, Agent);
		}
		return Agent;
	}
	
	public Collection<MultiValued> getAgentsData()
	{
		Collection<MultiValued> records = getMediaArchive().query("automationagent").exact("enabled", true).sort("ordering").cachedSearch();
		return records;
	}
	
	public void resetInformatics(String inEntityModuleId, Collection inEntities) 
	{
		
		ArrayList saveAll = new ArrayList();
		int count = 0;
		Collection ids = new HashSet();
		for (Iterator iterator = inEntities.iterator(); iterator.hasNext();)
		{
			Data entity = (Data) iterator.next();
			
			entity.setValue("taggedbyllm", false);
			entity.setValue("llmerror", false);
				
			entity.setValue("semantictopics", null);
			
			entity.setValue("entityembeddingstatus", null);
			entity.setValue("pagescreatedfor", null);
			entity.setValue("totalpages", null);
			
			//entity.setValue("searchcategory", null);
				
			//entity.setValue("keywords", null);
			//entity.setValue("longcaption", null);
			ids.add(entity.getId());
			saveAll.add(entity);
				
		}
		getMediaArchive().saveData(inEntityModuleId, saveAll);
		log.info("Saved " + saveAll.size() + " in " + inEntityModuleId);
		
		HitTracker todelete = getMediaArchive().query("semanticembedding").orgroup("dataid", ids).exact("moduleid", inEntityModuleId).search();
		getMediaArchive().getSearcher("semanticembedding").deleteAll(todelete, null);
		
		
		getMediaArchive().fireSharedMediaEvent("llm/addmetadata");
		
		
		
		
	}
}
