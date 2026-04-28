package org.entermediadb.ai.automation;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.repository.ContentItem;
import org.openedit.repository.InputStreamItem;

public class AutomationModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(AutomationModule.class);

	public void loadScenarios(WebPageRequest inReq)
	{
		Collection scenarios = getMediaArchive(inReq).query("automationscenario").exact("isvisible", true).sort("ordering").search();
		inReq.putPageValue("scenarios", scenarios);
	}

	public void loadAutomationScenario(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);

		inReq.putPageValue("automationManager", getAutomationManager(inReq));

		Searcher automationpositionsearcher = archive.getSearcher("automationposition");
		inReq.putPageValue("automationpositionsearcher", automationpositionsearcher);

		String scenarioid = inReq.getRequestParameter("scenarioid");

		if (scenarioid == null)
		{
			scenarioid = (String) inReq.getPageValue("scenarioid");
		}

		if (scenarioid == null)
		{
			Collection<Data> scenarios = archive.query("automationscenario").exact("isvisible", true).sort("ordering").search();
			inReq.putPageValue("scenarios", scenarios);

			Searcher scenarioSearcher = archive.getSearcher("automationscenario");
			inReq.putPageValue("scenariosearcher", scenarioSearcher);

			Collection<Data> labels = archive.query("automationlabel").search();
			inReq.putPageValue("labels", labels);

			Searcher labelSearcher = archive.getSearcher("automationlabel");
			inReq.putPageValue("labelsearcher", labelSearcher);
			return;
		}

		Data scenario = archive.query("automationscenario").exact("id", scenarioid).searchOne();
		inReq.putPageValue("scenario", scenario);

		Searcher agentEnabledSearcher = archive.getSearcher("automationagentenabled");
		inReq.putPageValue("agentenabledsearcher", agentEnabledSearcher);

		Collection<MultiValued> agents = agentEnabledSearcher.query().exact("automationscenario", scenario.getId()).search();
		inReq.putPageValue("agents", agents);
	}

	/*
	 * public void loadAutomationScenarios(WebPageRequest inReq) { MediaArchive archive =
	 * getMediaArchive(inReq);
	 * 
	 * String scenarioid = inReq.getRequestParameter("scenarioid");
	 * 
	 * if(scenarioid == null) { return; }
	 * 
	 * 
	 * Data scenario = archive.query("automationscenario").exact("id", scenarioid).searchOne();
	 * inReq.putPageValue("scenario", scenario);
	 * 
	 * Searcher agentEnabledSearcher = archive.getSearcher("automationagentenabled");
	 * inReq.putPageValue("agentenabledsearcher", agentEnabledSearcher);
	 * 
	 * Collection<MultiValued> agents = agentEnabledSearcher.query().exact("automationscenario",
	 * scenario.getId()).search(); inReq.putPageValue("agents", agents); }
	 */
	public AutomationManager getAutomationManager(WebPageRequest inReq)
	{
		AutomationManager manager = (AutomationManager) getMediaArchive(inReq).getBean("automationManager");
		inReq.putPageValue("automationManager", manager);
		return manager;
	}

	public void runScenario(WebPageRequest inReq)
	{
		String id = inReq.findActionValue("automationscenario");
		if (id == null)
		{
			id = inReq.getRequestParameter("scenarioid");
		}
		if (id == null)
		{
			id = inReq.getPage().getPageName();
		}
		AutomationManager manager = getAutomationManager(inReq);
		ScriptLogger logger = (ScriptLogger) inReq.getPageValue("log");

		AgentContext context = new AgentContext();
		context.setScriptLogger(logger);
		context.put("webpagerequest", inReq);

		manager.runScenario(id, context);
	}

	public void handleAgentSaved(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);

		Data agentEnabledData = (Data) inReq.getPageValue("data");

		inReq.putPageValue("agentid", agentEnabledData.getId());
		inReq.putPageValue("scenarioid", agentEnabledData.get("automationscenario"));

		Data agent = archive.query("automationagent").exact("id", agentEnabledData.get("automationagent")).searchOne();

		agentEnabledData.setValue("agenttype", agent.get("agenttype"));

		archive.saveData("automationagentenabled", agentEnabledData);

		AutomationManager manager = getAutomationManager(inReq);
		manager.generateParams(agentEnabledData);
	}

	public void saveLayout(WebPageRequest inReq)
	{
		Map layout = inReq.getJsonRequest();
		if (layout == null)
		{
			return;
		}

		Map canvas = (Map) layout.get("canvas");
		if (canvas != null)
		{
			getAutomationManager(inReq).savePositions(canvas);
		}

		JSONArray data = (JSONArray) layout.get("data");
		if (data == null)
		{
			return;
		}

		Searcher agentEnabledSearcher = getMediaArchive(inReq).getSearcher("automationagentenabled");
		for (Iterator iterator = data.iterator(); iterator.hasNext();)
		{
			JSONObject agentdata = (JSONObject) iterator.next();

			String id = (String) agentdata.get("id");
			Data agentEnabled = agentEnabledSearcher.query().exact("id", id).searchOne();
			if (agentEnabled == null)
			{
				agentEnabled = (Data) agentEnabledSearcher.createNewData();
			}

			Boolean enabled = agentdata.get("enabled") instanceof Boolean ? (Boolean) agentdata.get("enabled") : Boolean.parseBoolean(String.valueOf(agentdata.get("enabled")));
			Double offsetx = Double.parseDouble((String.valueOf(agentdata.get("offsetx"))));
			Double offsety = Double.parseDouble((String.valueOf(agentdata.get("offsety"))));

			String runafter = (String) agentdata.get("runafter");
			if (runafter != null)
			{
				agentEnabled.setValue("runafter", runafter);
			}

			agentEnabled.setValue("enabled", enabled);
			agentEnabled.setValue("offsetx", offsetx);
			agentEnabled.setValue("offsety", offsety);

			agentEnabledSearcher.saveData(agentEnabled);

		}

		String scenarioid = (String) layout.get("scenarioid");
		String base64 = (String) layout.get("thumbnail");
		saveAutomationSnapshot(inReq, scenarioid, base64);
	}

	public void saveAutomationPreview(WebPageRequest inReq)
	{
		Map payload = inReq.getJsonRequest();
		if (payload == null)
		{
			return;
		}

		Map canvas = (Map) payload.get("canvas");
		if (canvas != null)
		{
			getAutomationManager(inReq).savePositions(canvas);
		}

		Collection<Map> scenarios = (Collection<Map>) payload.get("scenarios");
		getAutomationManager(inReq).savePositions(scenarios);
		getAutomationManager(inReq).connectScenarios(scenarios);

		Collection<Map> labels = (Collection<Map>) payload.get("labels");
		getAutomationManager(inReq).savePositions(labels);
		getAutomationManager(inReq).saveLabels(labels);

	}

	public void deletePreviewNodes(WebPageRequest inReq)
	{
		Map payload = inReq.getJsonRequest();
		if (payload == null)
		{
			return;
		}

		Collection<Map> deleteIds = (Collection<Map>) payload.get("deleteIds");

		Collection<String> scenarioIds = new ArrayList();
		Collection<String> agentIds = new ArrayList();
		Collection<String> labelIds = new ArrayList();

		for (Iterator iterator = deleteIds.iterator(); iterator.hasNext();)
		{
			Map node = (Map) iterator.next();
			if (node.get("type").equals("scenario"))
			{
				scenarioIds.add((String) node.get("id"));
			}
			else
				if (node.get("type").equals("agent"))
				{
					agentIds.add((String) node.get("id"));
				}
				else
					if (node.get("type").equals("label"))
					{
						labelIds.add((String) node.get("id"));
					}
		}

		MediaArchive archive = getMediaArchive(inReq);

		if (scenarioIds.size() > 0)
		{
			Searcher scenarioSearcher = archive.getSearcher("automationscenario");
			Collection<Data> scenarios = scenarioSearcher.query().ids(scenarioIds).search();
			scenarioSearcher.deleteAll(scenarios, inReq.getUser());
		}

		if (agentIds.size() > 0)
		{
			Searcher agentSearcher = archive.getSearcher("automationagentenabled");
			Collection<Data> agents = agentSearcher.query().ids(agentIds).search();
			agentSearcher.deleteAll(agents, inReq.getUser());
		}

		if (labelIds.size() > 0)
		{
			Searcher labelSearcher = archive.getSearcher("automationlabel");
			Collection<Data> labels = labelSearcher.query().ids(labelIds).search();
			labelSearcher.deleteAll(labels, inReq.getUser());
		}

	}

	public void saveAutomationSnapshot(WebPageRequest inReq, String filename, String base64)
	{
		if (base64 == null)
		{
			return;
		}

		MediaArchive archive = getMediaArchive(inReq);

		String apphome = (String) inReq.getPageValue("apphome");

		String sourcepath = apphome + "/views/automations/" + filename + ".png";

		ContentItem saveTo = archive.getPageManager().getPage(sourcepath).getContentItem();

		try
		{
			InputStreamItem revision = new InputStreamItem();

			revision.setAbsolutePath(saveTo.getAbsolutePath());
			revision.setPath(saveTo.getPath());
			revision.setAuthor(inReq.getUser().getId());
			revision.setType(ContentItem.TYPE_ADDED);
			revision.setMessage(saveTo.getMessage());

			revision.setPreviewImage(saveTo.getPreviewImage());

			InputStream input = null;

			String code = base64.substring(base64.indexOf(",") + 1, base64.length());
			byte[] tosave = Base64.getDecoder().decode(code);
			input = new ByteArrayInputStream(tosave);

			revision.setInputStream(input);

			archive.getPageManager().getRepository().put(revision);
			getPageManager().clearCache(revision.getPath());
			log.info("Saved To " + revision.getAbsolutePath());
		}
		catch (Exception ex)
		{
			log.error(ex);
		}

	}

}
