package org.entermediadb.ai.automation;

import java.util.Iterator;
import java.util.Map;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.events.PathEventManager;
import org.openedit.BaseWebPageRequest;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.page.PageRequestKeys;

public class RunEventAgent extends BaseAgent
{
	
	/**
	 * Calls render to html
	 * Attaches and asset version
	 * sends it to a blog?
	 */
	@Override
	public void process(AgentContext inContext)
	{
		WebPageRequest request =  (WebPageRequest)inContext.getContextValue("webpagerequest");
		
		if(request != null)
		{
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
			
			String operation = inContext.getCurrentAgentEnable().getAgentConfig().get("operation");
			if( operation != null)
			{
				runPathEvent(inContext, operation, request);
			}
		}
		super.process(inContext);
	}

	private void runPathEvent(AgentContext inContext, String operation, WebPageRequest inRequest)
	{
		String runpath = "/" + getCatalogId() + "/events/" + operation + ".html";
		PathEventManager manager = (PathEventManager) getModuleManager().getBean(getCatalogId(), "pathEventManager");
		WebPageRequest request = new BaseWebPageRequest(inRequest);

		request.setRequestParameter("catalogid", getCatalogId());
		manager.runPathEvent(runpath, request);
		
	}
	
}
