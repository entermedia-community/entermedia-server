package org.entermediadb.ai.automation;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.asset.upload.FileUpload;
import org.entermediadb.asset.upload.UploadRequest;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.repository.ContentItem;

public class AutomationModule extends BaseMediaModule {
	private static final Log log = LogFactory.getLog(AutomationModule.class);

	public void loadScenarios(WebPageRequest inReq)
	{
		Collection list = getMediaArchive(inReq).getList("automationscenario");
		inReq.putPageValue("scenarios", list);
	}
    
	public void loadAutomationScenario(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		
		String scenarioid = inReq.getRequestParameter("scenarioid");
		
		if(scenarioid == null)
		{
				scenarioid = (String) inReq.getPageValue("scenarioid");
		}
			
		if(scenarioid == null)
		{
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
  public void loadAutomationScenarios(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		
		String scenarioid = inReq.getRequestParameter("scenarioid");
		
		if(scenarioid == null)
		{
			return;
		}
    	
    	
		Data scenario = archive.query("automationscenario").exact("id", scenarioid).searchOne();
		inReq.putPageValue("scenario", scenario);
		
		Searcher agentEnabledSearcher = archive.getSearcher("automationagentenabled");
		inReq.putPageValue("agentenabledsearcher", agentEnabledSearcher);
		
		Collection<MultiValued> agents = agentEnabledSearcher.query().exact("automationscenario", scenario.getId()).search();
		inReq.putPageValue("agents", agents);
	}
	*/
	public AutomationManager getAutomationManager(WebPageRequest inReq)
	{
		AutomationManager manager = (AutomationManager)getMediaArchive(inReq).getBean("automationManager");
		inReq.putPageValue("automationManager", manager);
		return manager;
	}

	public void runScenario(WebPageRequest inReq)
	{	
		String id = inReq.findActionValue("automationscenario");
		if( id == null)
		{
			id = inReq.getPage().getPageName();
		}
		AutomationManager manager = getAutomationManager(inReq);
		ScriptLogger logger =  (ScriptLogger) inReq.getPageValue("log");
		
		AgentContext context = new AgentContext();
		context.setScriptLogger(logger);
		
		
		manager.runScenario(id,context);
	}
	
	
	public void handleAgentSaved(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		
		Data agentEnabled = (Data) inReq.getPageValue("data");
		
		inReq.putPageValue("agentid", agentEnabled.getId());
		inReq.putPageValue("scenarioid", agentEnabled.get("automationscenario"));
		
		Data agent = archive.query("automationagent").exact("id", agentEnabled.get("automationagent")).searchOne();
		
		agentEnabled.setValue("agenttype", agent.get("agenttype"));
		
		archive.saveData("automationagentenabled", agentEnabled);
		
	}
	
	public void saveLayout(WebPageRequest inReq)
	{
		Map layout = inReq.getJsonRequest();
		if(layout == null)
		{
			return;
		}
		JSONArray data = (JSONArray) layout.get("data");
		if(data == null)
		{
			return;
		}
		
		Searcher agentEnabledSearcher = getMediaArchive(inReq).getSearcher("automationagentenabled");
		for (Iterator iterator = data.iterator(); iterator.hasNext();) {
			JSONObject agentdata = (JSONObject) iterator.next();
			
			String id = (String) agentdata.get("id");
			Data agentEnabled = agentEnabledSearcher.query().exact("id", id).searchOne();
			if(agentEnabled == null)
			{
				agentEnabled = (Data) agentEnabledSearcher.createNewData();
			}
			
			Boolean enabled = Boolean.parseBoolean((String) agentdata.get("enabled"));
			Double offsetx = Double.parseDouble((String.valueOf(agentdata.get("offsetx"))));
			Double offsety = Double.parseDouble((String.valueOf(agentdata.get("offsety"))));
			
			String runafter = (String) agentdata.get("runafter");
			if(runafter != null)
			{
				agentEnabled.setValue("runafter", runafter);
			}
			
			agentEnabled.setValue("enabled", enabled);
			agentEnabled.setValue("offsetx", offsetx);
			agentEnabled.setValue("offsety", offsety);
			
			agentEnabledSearcher.saveData(agentEnabled);
			
		}
	}
	

	public void saveAutomationSnapshot(WebPageRequest inReq)
	{
		//Regular upload
		MediaArchive archive = getMediaArchive(inReq);
		FileUpload command = (FileUpload) archive.getBean("fileUpload");
		UploadRequest properties = command.parseArguments(inReq);
		
		if (properties == null) {
			return;
		}
		if (properties.getFirstItem() == null) 
		{
			log.info("No upload found");
			return;
		}
		String apphome = inReq.findValue("apphome");
		String automationid = inReq.getRequestParameter("automationid");
		String sourcepath = apphome + "/views/automations/" + automationid + ".png" ;
		//TODO: Check for formats
		
		//Save to temp place to change format
		String tmpplace = "/WEB-INF/trash/" + archive.getCatalogId()	+ "/originals/" + sourcepath;
		ContentItem tosave = archive.getPageManager().getRepository().getStub(tmpplace);
		
		ContentItem saved = properties.saveFileAs(properties.getFirstItem(), tosave, inReq.getUser());

	}
	

}
