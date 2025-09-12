package org.entermediadb.ai.assistant;

import org.entermediadb.asset.modules.BaseMediaModule;
import org.json.simple.JSONObject;
import org.openedit.WebPageRequest;

public class AgentModule extends BaseMediaModule {
	
	public AssistantManager getAssistantManager(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		AssistantManager assistantManager = (AssistantManager) getMediaArchive(catalogid).getBean("assistantManager");
		return assistantManager;
	}
	
	public void chatAgentSearch(WebPageRequest inReq) throws Exception 
	{	
		getAssistantManager(inReq).regularSearch(inReq, false);
	}
	
	public void mcpSearch(WebPageRequest inReq) throws Exception
	{
		getAssistantManager(inReq).regularSearch(inReq, true);
	}
	
	public void loadSemanticMatches(WebPageRequest inReq) throws Exception
	{
		getAssistantManager(inReq).semanticSearch(inReq);
	}

	public void mcpGenerateReport(WebPageRequest inReq) throws Exception {
		JSONObject arguments = (JSONObject) inReq.getPageValue("arguments");
		String report = getAssistantManager(inReq).generateReport(arguments);
		inReq.putPageValue("report", report);
	}

	public void semanticSearch(WebPageRequest inReq) throws Exception 
	{	
		getAssistantManager(inReq).regularSearch(inReq, false);
	}

}
