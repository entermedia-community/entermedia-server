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

	public void searchSpecifiedTables(WebPageRequest inReq) throws Exception 
	{	
		AgentContext agentContext =  (AgentContext)inReq.getPageValue("agentcontext");
		
		getAssistantManager(inReq).searchSpecifiedTables(inReq, agentContext.getAiSearchParams());
	}

	public void searchAllTables(WebPageRequest inReq) throws Exception 
	{	
		AgentContext agentContext =  (AgentContext)inReq.getPageValue("agentcontext");
		
		getAssistantManager(inReq).searchAllTables(inReq, agentContext.getAiSearchParams());
	}
	
	public void chatAgentSemanticSearch(WebPageRequest inReq) throws Exception 
	{	
		AgentContext agentContext =  (AgentContext)inReq.getPageValue("agentcontext");
		
		Object semanticquery = agentContext.getValue("semanticquery");

		agentContext.setValue("semanticquery", null);
		agentContext.setNextFunctionName(null);
		
		if( semanticquery == null)
		{
			return;
		}
		
		String query = (String) semanticquery;
		
		getAssistantManager(inReq).semanticSearch(inReq, query);
	}
	
	public void chatAgentExecuteRAG(WebPageRequest inReq) throws Exception 
	{	
		getAssistantManager(inReq).executeRag(inReq);
	}
	
	public void chatAgentCreateImage(WebPageRequest inReq) throws Exception 
	{	
		AgentContext agentContext =  (AgentContext)inReq.getPageValue("agentcontext");
		getAssistantManager(inReq).createImage(inReq, agentContext.getAiCreationParams());
	}
	
	public void chatAgentCreateEntity(WebPageRequest inReq) throws Exception 
	{	
		AgentContext agentContext =  (AgentContext)inReq.getPageValue("agentcontext");
		getAssistantManager(inReq).createEntity(inReq, agentContext.getAiCreationParams());
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
		String query = inReq.getRequestParameter("semanticquery");
		
		if(query != null && !"null".equals(query))
		{
			getAssistantManager(inReq).semanticSearch(inReq, query);
		}
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
