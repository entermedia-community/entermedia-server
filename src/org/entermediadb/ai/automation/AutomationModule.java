package org.entermediadb.ai.automation;

import java.util.Collection;

import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.WebPageRequest;

public class AutomationModule extends BaseMediaModule {
    

    public void loadScenarios(WebPageRequest inRequest)
    {
        Collection list = getMediaArchive(inRequest).getList("automationscenerio");
        inRequest.putPageValue("scenarios", list);

    }

}
