package org.entermediadb.ai.automation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.Agent;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.events.EventTrigger;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.ValuesMap;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventListener;
import org.openedit.util.JSONParser;

/**
 * My plan is to have a UI where each Task can be seen and assigned to a Agent. The Agent will be
 * responsible for executing the task and updating the status of the task. The TaskManager will be
 * responsible for scheduling the tasks and keeping track of the status of the tasks. The
 * TaskManager will also be responsible for providing a UI for the tasks and allowing users to
 * interact with the tasks. A Task can be a big one or small ones. For example, a big task can be
 * "Classify all assets in the system" and a small task can be "Classify asset 12345". The
 * TaskManager will be responsible for breaking down big tasks into smaller tasks and scheduling
 * them accordingly.
 * 
 * A task can retry a few times if it fails. It will have a retry count and a retry delay. The
 * TaskManager will be responsible for retrying the task if it fails and updating the status of the
 * task accordingly.
 * 
 * Tasks will have Steps that are connected to AI Functions. The functions have their own
 * configuration. The TaskManager will be responsible for executing the steps in order and passing
 * the output of one step to the next step. The TaskManager will also be responsible for handling
 * errors and retrying steps if they fail.
 * 
 * Once the Tasks are identified there will be a set of Agents that look over the tasks and execute
 * them. The Agents will be responsible for executing the task and updating the status of the tasks.
 * 
 */

public class AutomationManager extends BaseAiManager implements WebEventListener
{
	private static final Log log = LogFactory.getLog(AutomationManager.class);

	protected Map<String, List<AgentContext>> fieldRecentContextByAutomation = new HashMap();
	protected JSONParser fieldJsonParser = new JSONParser();

	public Collection<AgentContext> getRecentScenerioContext(String inScenerio)
	{
		Collection<AgentContext> found = fieldRecentContextByAutomation.get(inScenerio);
		if (found != null)
		{
			List<AgentContext> list = new ArrayList<>(found);
			java.util.Collections.reverse(list);
			return list;
		}
		return found;
	}

	public AgentContext findContextForScenerio(String inScenerio, String inContextId)
	{
		Collection<AgentContext> found = fieldRecentContextByAutomation.get(inScenerio);
		if (found == null)
		{
			return null;
		}
		for (Iterator iterator = found.iterator(); iterator.hasNext();)
		{
			AgentContext agentContext = (AgentContext) iterator.next();
			if (agentContext.getId().equals(inContextId))
			{
				return agentContext;
			}
		}

		return null;
	}

	public void addContext(String inScenerio, AgentContext inContext)
	{
		List<AgentContext> found = fieldRecentContextByAutomation.get(inScenerio);
		if (found == null)
		{
			found = new ArrayList();
			fieldRecentContextByAutomation.put(inScenerio , found);
		}
		found.add(inContext);
		if (found.size() > 5)
		{
			found.remove(0);
		}
	}

	public void runScenario(String inId, ScriptLogger inLogger)
	{
		AgentContext context = new AgentContext();
		context.setScriptLogger(inLogger);
		runScenario(inId , context);
	}

	public void runScenario(String inId, AgentContext inContext)
	{
		MultiValued scenerio =
			(MultiValued) getMediaArchive().getCachedData("automationscenario" , inId);
		// query("automationscenerio").exact("enabled", true).sort("ordering").search();

		if (scenerio == null)
		{
			log.error("Could not find scenerio " + inId);
			return;
		}
		if (inContext.getId() == null)
		{
			inContext.setId(inCrementId());
		}
		addContext(inId , inContext);
		inContext.setCurrentScenerio(scenerio);

		// Lock it
		inContext.setValue("lastrunstart" , new Date());
		scenerio.setValue("lastrunstart" , new Date());
		scenerio.setValue("isrunning" , true);

		Collection<AgentEnabled> enabled = getEnabledAgents(inId);

		if (enabled == null || enabled.isEmpty())
		{
			log.info("No enabled agents for scenerio " + inId);
			return;
		}

		inContext.setAgentEnableChildren(enabled);

		for (AgentEnabled agentEnabled : enabled)
		{
			inContext.setCurrentAgentEnable(agentEnabled);
			agentEnabled.getAgent().process(inContext);
		}
		scenerio.setValue("isrunning" , false);
		getMediaArchive().saveData("automationscenario" , scenerio);
	}

	private void addContextValues(AgentEnabled inAgentEnabled)
	{
		MultiValued automationEnabledData = (MultiValued) inAgentEnabled.getAutomationEnabledData();
		String text = automationEnabledData.get("contextvalues");
		if (text == null && inAgentEnabled.getAgentData() != null)
		{
			text = inAgentEnabled.getAgentData().get("contextvalues");
		}
		if (text != null)
		{
			JSONObject json = (JSONObject) fieldJsonParser.parse(text);
			inAgentEnabled.setExtraContextValues(json);
		}
	}

	public Map<String, MultiValued> getAllPositions()
	{
		Map<String, MultiValued> map =
			(Map<String, MultiValued>) getMediaArchive().getCacheManager()
				.get("automationscenariopositionmap" , "all");
		if (map == null)
		{
			map = new HashMap();

			Collection positions = getMediaArchive().query("automationposition").all().search();
			for (Iterator iterator = positions.iterator(); iterator.hasNext();)
			{
				MultiValued data = (MultiValued) iterator.next();
				map.put(data.getId() , data);
			}
			getMediaArchive().getCacheManager().put("automationscenariopositionmap" , "all" , map);
		}
		return map;
	}

	public Map<String, MultiValued> getAllLabels()
	{
		Map<String, MultiValued> map =
			(Map<String, MultiValued>) getMediaArchive().getCacheManager()
				.get("automationlabelsmap" , "all");
		if (map == null)
		{
			map = new HashMap();

			Collection positions = getMediaArchive().query("automationlabel").all().search();
			for (Iterator iterator = positions.iterator(); iterator.hasNext();)
			{
				MultiValued data = (MultiValued) iterator.next();
				map.put(data.getId() , data);
			}
			getMediaArchive().getCacheManager().put("automationlabelsmap" , "all" , map);
		}
		return map;
	}

	public Data getPosition(String inId)
	{
		Data data = getAllPositions().get(inId);
		return data;
	}
	// public Map<String,MultiValued> getEnabledPositions(String inScenario)
	// {
	// Collection<AgentEnabled> found = getEnabledAgents(inScenario);
	// Map<String,MultiValued> map = new HashMap(found.size());
	// for (Iterator iterator2 = found.iterator(); iterator2.hasNext();)
	// {
	// AgentEnabled agentEnabled = (AgentEnabled) iterator2.next();
	// String enabledid = agentEnabled.getAutomationEnabledData().getId();
	// MultiValued data = getAllPositions().get(enabledid);
	// map.put(enabledid, data);
	// }
	// return map;
	// }

	public void savePositions(Map inPosition)
	{
		Collection<Map> tosave = new ArrayList<Map>();
		tosave.add(inPosition);
		savePositions(tosave);
	}

	public void savePositions(Collection<Map> inPositions)
	{
		Collection tosave = new ArrayList();
		for (Iterator iterator = inPositions.iterator(); iterator.hasNext();)
		{
			Map map = (Map) iterator.next();
			ValuesMap valuemap = new ValuesMap(map);
			MultiValued data = getAllPositions().get(valuemap.get("id"));
			if (data == null)
			{
				data = (MultiValued) getMediaArchive().getSearcher("automationposition")
					.createNewData();
				data.setId((String) valuemap.get("id"));
			}
			data.setValue("posx" , valuemap.getDouble("posx"));
			data.setValue("posy" , valuemap.getDouble("posy"));
			Double zoom = valuemap.getDouble("zoom");
			if (zoom != null)
			{
				data.setValue("zoom" , zoom);
			}
			tosave.add(data);
		}
		getMediaArchive().saveData("automationposition" , tosave);
		getMediaArchive().getCacheManager().remove("automationscenariopositionmap" , "all");
	}

	public void connectScenarios(Collection<Map> inConnections)
	{
		Collection ids = new ArrayList();
		Map connections = new HashMap();

		for (Iterator iterator = inConnections.iterator(); iterator.hasNext();)
		{
			Map map = (Map) iterator.next();
			if (map.containsKey("connectedtop"))
			{
				ids.add(map.get("id"));
			}

			connections.put(map.get("id") , map.get("connectedtop"));
		}

		Collection<Data> scenarios = getMediaArchive().query("automationscenario").all().search();

		Collection tosave = new ArrayList();

		for (Iterator iterator = scenarios.iterator(); iterator.hasNext();)
		{
			Data scenario = (Data) iterator.next();
			String id = scenario.getId();
			String connectedtop = (String) connections.get(id);

			scenario.setValue("connectedtop" , connectedtop);
			tosave.add(scenario);
		}

		getMediaArchive().saveData("automationscenario" , tosave);

	}

	public void saveLabels(Collection<Map> inData)
	{
		Collection tosave = new ArrayList();

		for (Iterator iterator = inData.iterator(); iterator.hasNext();)
		{
			Map map = (Map) iterator.next();

			String id = (String) map.get("id");

			Data label = getAllLabels().get(id);
			if (label == null)
			{
				label = (Data) getMediaArchive().getSearcher("automationlabel").createNewData();
				label.setId(id);
			}
			label.setValue("text" , map.get("text"));
			label.setValue("strokecolor" , map.get("strokecolor"));
			label.setValue("bgcolor" , map.get("bgcolor"));
			label.setValue("connectedbottom" , map.get("connectedbottom"));

			tosave.add(label);
		}

		getMediaArchive().saveData("automationlabel" , tosave);

	}

	public Collection<AgentEnabled> getEnabledAgents(String inId)
	{
		Collection<AgentEnabled> cached =
			(Collection<AgentEnabled>) getMediaArchive().getCacheManager()
				.get("agentsenabled" , inId);
		if (cached == null)
		{
			Collection found = getMediaArchive().query("automationagentenabled")
				.exact("automationscenario" , inId)
				.exact("enabled" , true)
				.search();
			Map<String, AgentEnabled> allparents = new HashMap();
			for (Iterator iterator = found.iterator(); iterator.hasNext();)
			{
				MultiValued agentenableddata = (MultiValued) iterator.next();
				AgentEnabled enabled = new AgentEnabled();
				enabled.setAutomationEnabledData(agentenableddata);
				String agentid = agentenableddata.get("automationagent");
				MultiValued agentdata =
					(MultiValued) getMediaArchive().getCachedData("automationagent" , agentid);
				enabled.setAgentData(agentdata);

				addContextValues(enabled);

				if (agentdata == null)
				{
					log.error("Could not find agent data for enabled agent "
						+ agentenableddata.getId() + " with agentid " + agentid);
					continue;
				}

				String bean = agentdata.get("bean");
				if (bean == null)
				{
					log.error("No bean defined for agent " + agentenableddata.getId());
					continue;
				}
				Agent agent = loadAgent(bean);
				enabled.setAgent(agent);

				allparents.put(agentenableddata.getId() , enabled);
			}
			// Sort the list
			cached = new ArrayList();
			for (Iterator iterator = allparents.values().iterator(); iterator.hasNext();)
			{
				AgentEnabled childAgent = (AgentEnabled) iterator.next();
				String myparent = childAgent.getParentAgent();
				AgentEnabled parentAgent = allparents.get(myparent);
				if (myparent == null || parentAgent == null)
				{
					cached.add(childAgent);
				}
				else
				{
					parentAgent.addChild(childAgent);
				}
			}
			getMediaArchive().getCacheManager().put("agentsenabled" , inId , cached);
		}

		return cached;
	}

	public void saveAllScenerios()
	{
		// Save events xconfs
	}

	public void generateParams(Data inAgentEnabledConfig)
	{
		String argumentString = inAgentEnabledConfig.get("parameterstructure");

		Collection agentArguments = null;
		if (argumentString != null)
		{
			JSONParser parser = new JSONParser();
			agentArguments = parser.parseCollection(argumentString);

			if (agentArguments != null && agentArguments.size() > 0)
			{
				return;
			}
		}

		if (agentArguments == null)
		{
			String skilloverview = inAgentEnabledConfig.get("skilloverview");

			if (skilloverview != null)
			{
				LlmConnection llmConnection =
					getMediaArchive().getLlmConnection("agentparamsfromskill");

				AgentContext context = new AgentContext();
				context.addContext("skilloverview" , skilloverview);

				LlmResponse res = llmConnection.callStructure(context , "agentparamsfromskill");

				JSONObject rawresponse = res.getMessageStructured();
				if (rawresponse != null)
				{
					JSONArray arguments = (JSONArray) rawresponse.get("arguments");

					if (arguments != null)
					{
						argumentString = arguments.toJSONString();
						inAgentEnabledConfig.setValue("parameterstructure" , argumentString);
						getMediaArchive().saveData("automationagentenabled" , inAgentEnabledConfig);
						getMediaArchive().getCacheManager()
							.remove("agentsenabled" , inAgentEnabledConfig
								.get("automationscenario"));
					}
				}
			}
		}
	}

	public Agent loadAgent(String inName)
	{
		if (inName == null)
		{
			throw new IllegalArgumentException("Bean name not provided");
		}
		Agent Agent = (Agent) getMediaArchive().getCacheManager().get("ai" , "Agent" + inName);
		if (Agent == null)
		{
			Agent = (Agent) getModuleManager().getBean(getCatalogId() , inName);
			getMediaArchive().getCacheManager().put("ai" , "Agent" + inName , Agent);
		}
		return Agent;
	}

	public Collection<MultiValued> getAgentsData()
	{
		Collection<MultiValued> records = getMediaArchive().query("automationagent")
			.exact("enabled" , true)
			.sort("ordering")
			.cachedSearch();
		return records;
	}

	@Override
	public void eventFired(WebEvent inEvent)
	{
		if (inEvent.getSource() instanceof EventTrigger)
		{
			String operation = inEvent.getOperation();
			if (operation.startsWith("running_"))
			{
				EventTrigger trigger = (EventTrigger) inEvent.getSource();
				String path = operation.substring("running_".length());
				Collection<String> ids = findSceneriosForEvent(path);
				if (!ids.isEmpty())
				{
					AgentContext context = new AgentContext();
					context.setCatalogId(getCatalogId());
					context.setScriptLogger(trigger.getLogger());
					context.put("webpagerequest" , trigger.getWebPageRequest());
					MultiValued module = (MultiValued) getMediaArchive()
						.getCachedData("module" , inEvent.getSearchType());
					context.setCurrentEntityModule(module);

					for (Iterator iterator = ids.iterator(); iterator.hasNext();)
					{
						String id = (String) iterator.next();
						runScenario(id , context);
					}
				}
				// see if we have a handler enabled with that id then start that scenerio
			}
		}
	}

	public Collection<String> findSceneriosForEvent(String inEvent)
	{
		Collection<String> cached =
			(Collection<String>) getMediaArchive().getCacheManager().get("eventlookup" , inEvent);
		if (cached == null)
		{
			cached = new HashSet();
			Collection found = getMediaArchive().query("automationagentenabled")
				.exact("runoperation" , inEvent)
				.exact("enabled" , true)
				.search();
			if (found != null)
			{
				for (Iterator iterator = found.iterator(); iterator.hasNext();)
				{
					Data enabled = (Data) iterator.next();
					String automationscenario = enabled.get("automationscenario");
					cached.add(automationscenario);
				}
			}
			getMediaArchive().getCacheManager().put("eventlookup" , inEvent , cached);
		}
		return cached;
	}

	long fieldCounter = System.currentTimeMillis();

	public String inCrementId()
	{
		fieldCounter++;
		return String.valueOf(fieldCounter);
	}

}
