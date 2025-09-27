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
	
	public void chatAgentRegularSearch(WebPageRequest inReq) throws Exception 
	{	
		getAssistantManager(inReq).regularSearch(inReq, false);
	}
	
	public void chatAgentSemanticSearch(WebPageRequest inReq) throws Exception 
	{	
		getAssistantManager(inReq).semanticSearch(inReq);
	}
	
	public void chatAgentCreateImage(WebPageRequest inReq) throws Exception 
	{	
		getAssistantManager(inReq).createImage(inReq);
	}
	
	public void chatAgentCreateEntity(WebPageRequest inReq) throws Exception 
	{	
		getAssistantManager(inReq).createEntity(inReq);
	}
	
	public void chatAgentUpdateEntity(WebPageRequest inReq) throws Exception 
	{	
		getAssistantManager(inReq).updateEntity(inReq);
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

}
