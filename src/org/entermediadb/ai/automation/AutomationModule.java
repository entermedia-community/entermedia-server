package org.entermediadb.ai.automation;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.asset.modules.ConvertStatusModule;
import org.entermediadb.asset.upload.FileUpload;
import org.entermediadb.asset.upload.UploadRequest;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.repository.ContentItem;
import org.openedit.util.PathUtilities;

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
	public AutomationManager getAutomationManager(WebPageRequest inRequest)
	{
		AutomationManager manager = (AutomationManager)getMediaArchive(inRequest).getBean("automationManager");
		inRequest.putPageValue("automationManager", manager);
		return manager;
	}

	public void runScenario(WebPageRequest inRequest)
	{	
		String id = inRequest.findActionValue("automationscenario");
		if( id == null)
		{
			id = inRequest.getPage().getPageName();
		}
		AutomationManager manager = getAutomationManager(inRequest);
		ScriptLogger logger =  (ScriptLogger)inRequest.getPageValue("log");
		
		AgentContext context = new AgentContext();
		context.setScriptLogger(logger);
		context.put("webpagerequest", inRequest);
		
		
		manager.runScenario(id,context);
	}
	
	
	public void saveLayout(WebPageRequest inRequest)
	{
		
		
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
