package org.entermediadb.ai.automation;

import java.util.Collection;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;

public class AutomationModule extends BaseMediaModule {
    

    public void loadScenarios(WebPageRequest inRequest)
    {
        Collection list = getMediaArchive(inRequest).getList("automationscenario");
        inRequest.putPageValue("scenarios", list);

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
    	
    	Collection<MultiValued> agents = archive.query("automationagentenabled").exact("automationscenario", scenario.getId()).search();
    	inReq.putPageValue("agents", agents);
	}

}
