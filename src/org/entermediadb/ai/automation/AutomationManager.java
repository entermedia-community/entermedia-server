package org.entermediadb.ai.automation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.Agent;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.MultiValued;

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

	public void runScenario(String inId, ScriptLogger inLogger)
	{
		AgentContext context = new AgentContext();
		context.setScriptLogger(inLogger);
		runScenario(inId,context);
	}
	public void runScenario(String inId, AgentContext inContext)
	{
		MultiValued scenerio = (MultiValued)getMediaArchive().getCachedData("automationscenario",inId);//query("automationscenerio").exact("enabled", true).sort("ordering").search();
		
		inContext.setCurrentScenerio(scenerio);
		Collection<AgentEnabled> enabled = getEnabledAgents(inId);
		inContext.setAgentsEnabled(enabled);
	
		for (Iterator iterator = enabled.iterator(); iterator.hasNext();)
		{
			AgentEnabled agentEnabled = (AgentEnabled) iterator.next();
			inContext.setCurrentAgentEnable(agentEnabled);
			agentEnabled.getAgent().process(inContext);
		}
	}
	
	public Collection<AgentEnabled> getEnabledAgents(String inId)
	{
		Collection<AgentEnabled> cached = (Collection<AgentEnabled>)getMediaArchive().getCacheManager().get("agentsenabled", inId);
		if( cached == null)
		{
			Collection found = getMediaArchive().query("automationagentenabled").exact("automationscenario",inId).exact("enabled", true).search();
			Map<String,AgentEnabled> allparents = new HashMap();
			for (Iterator iterator = found.iterator(); iterator.hasNext();)
			{
				MultiValued data = (MultiValued) iterator.next();
				AgentEnabled enabled = new AgentEnabled();
				enabled.setAutomationEnabledData(data);
				String agentid = data.get("automationagent");
				MultiValued agentconfig = (MultiValued)getMediaArchive().getCachedData("automationagent",agentid);
				enabled.setAgentConfig(agentconfig);
				String bean = agentconfig.get("bean");
				Agent agent = loadAgent(bean);
				enabled.setAgent(agent);

				allparents.put(data.getId(),enabled);
			}
			//Sort the list
			cached = new ArrayList();
			for (Iterator iterator = allparents.values().iterator(); iterator.hasNext();)
			{
				AgentEnabled childAgent = (AgentEnabled) iterator.next();
				String myparent = childAgent.getParentAgent();
				AgentEnabled parentAgent = allparents.get(myparent);
				if( myparent == null|| parentAgent == null)
				{
					cached.add(childAgent);
				}
				else
				{
					parentAgent.addChild(childAgent);
				}
			}	
			getMediaArchive().getCacheManager().put("agentsenabled", inId,cached);
		}
		return cached;
	}
	
	public void saveAllScenerios()
	{
		//Save events xconfs
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
	
}
