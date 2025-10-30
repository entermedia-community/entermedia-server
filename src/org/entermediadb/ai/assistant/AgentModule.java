package org.entermediadb.ai.assistant;

import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONObject;
import org.openedit.WebPageRequest;

public class AgentModule extends BaseMediaModule {
	
	public AssistantManager getAssistantManager(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		AssistantManager assistantManager = (AssistantManager) getMediaArchive(catalogid).getBean("assistantManager");
		return assistantManager;
	}

	public void chatAgentJoinSearch(WebPageRequest inReq) throws Exception 
	{	
		AgentContext request =  (AgentContext)inReq.getPageValue("llmrequest");
		
		getAssistantManager(inReq).searchJoin(inReq, request.getAiSearchParams());
	}

	public void chatAgentRegularSearch(WebPageRequest inReq) throws Exception 
	{	
		AgentContext request =  (AgentContext)inReq.getPageValue("llmrequest");
		
		getAssistantManager(inReq).searchRegular(inReq, request.getAiSearchParams());
	}
	
	public void chatAgentSemanticSearch(WebPageRequest inReq) throws Exception 
	{	
		getAssistantManager(inReq).semanticSearch(inReq);
	}
	
	public void chatAgentExecuteRAG(WebPageRequest inReq) throws Exception 
	{	
		getAssistantManager(inReq).executeRag(inReq);
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
	
//	public void mcpSearch(WebPageRequest inReq) throws Exception
//	{
//		getAssistantManager(inReq).regularSearch(inReq, true);
//	}
	
	public void loadSemanticMatches(WebPageRequest inReq) throws Exception
	{
		getAssistantManager(inReq).semanticSearch(inReq);
	}

	public void mcpGenerateReport(WebPageRequest inReq) throws Exception {
		JSONObject arguments = (JSONObject) inReq.getPageValue("arguments");
		String report = getAssistantManager(inReq).generateReport(arguments);
		inReq.putPageValue("report", report);
	}

	public void indexActions(WebPageRequest inReq) throws Exception 
	{
		ScriptLogger logger = (ScriptLogger)inReq.getPageValue("log");
		getAssistantManager(inReq).loadAllActions(logger);
	}

}
