package org.entermediadb.ai.automation;

import java.util.Collection;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;

public class AutomationModule extends BaseMediaModule {
    

    public void loadScenarios(WebPageRequest inReq)
    {
        Collection list = getMediaArchive(inReq).getList("automationscenario");
        inReq.putPageValue("scenarios", list);

    }
    
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
		
		manager.runScenario(id,logger);
	}

}
