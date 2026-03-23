package org.entermediadb.ai.automation.agents;

import java.util.Map;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;

public class HandleWebEventAgent extends BaseAgent
{
	@Override
	public void process(AgentContext inContext)
	{
		//String runoperation = inContext.getCurrentAgentEnable().getAutomationEnabledData().get("runoperation");
		WebPageRequest request =  (WebPageRequest)inContext.getContextValue("webpagerequest");
		
		if(request == null)
		{
			//error
			inContext.error("Requires webpagerequest ");
			return;
		}
		Map params = request.getParameterMap();
		//Map values = getRequestUtils().extractValueMap(request);
		inContext.put("parameters", params);
		
		String entityid = request.getRequestParameter("entityid");
		String entitymoduleid = request.getRequestParameter("entitymoduleid");

		MultiValued entity = (MultiValued)getMediaArchive().getCachedData(entitymoduleid, entityid);
		inContext.put("currententity",entity);
		
		MultiValued entitymodule = (MultiValued)getMediaArchive().getCachedData("module",entitymoduleid);
		inContext.put("currententitymodule",entitymodule);
		
		String triggerapplicationid = (String) request.getPageValue("triggerapplicationid");
		inContext.put("triggerapplicationid", triggerapplicationid);
		inContext.put("triggersiteroot", request.getSiteRoot());
		
		inContext.setUserProfile(request.getUserProfile());
		
		request.putPageValue("currentagentcontext",inContext);
		
		//String operation = inContext.getCurrentAgentEnable().getAgentConfig().getId();
		//runPathEvent(inContext, operation, request);
		//Filter by operation
		super.process(inContext);
	}
}
