package org.entermediadb.ai.assistant;

import java.util.Collection;

import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.WebPageRequest;

public class AgentModule extends BaseMediaModule {
	
	public AssistantManager getAssistantManager(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		AssistantManager assistantManager = (AssistantManager) getMediaArchive(catalogid).getBean("assistantManager");
		return assistantManager;
	}

	public void searchTables(WebPageRequest inReq) throws Exception 
	{	
		AgentContext agentContext =  (AgentContext)inReq.getPageValue("agentcontext");
		
		getAssistantManager(inReq).searchTables(inReq, agentContext.getAiSearchParams());
	}
	
	public void chatAgentSemanticSearch(WebPageRequest inReq) throws Exception 
	{	
		AgentContext agentContext =  (AgentContext) inReq.getPageValue("agentcontext");
		
		String semanticquery = agentContext.get("semanticquery");

		agentContext.setValue("semanticquery", null);
		agentContext.setNextFunctionName(null);
		
		inReq.setRequestParameter("semanticquery", semanticquery);
		if (agentContext.getExcludedEntityIds() != null)
		{
			String[] excluded = agentContext.getExcludedEntityIds().toArray(new String[0]);
			inReq.setRequestParameter("excludeentityids", excluded);
		}
		if (agentContext.getExcludedAssetIds() != null)
		{
			String[] excluded = agentContext.getExcludedAssetIds().toArray(new String[0]);
			inReq.setRequestParameter("excludeassetids", excluded);
		}
		
		if( semanticquery == null)
		{
			return;
		}
		
		String query = (String) semanticquery;
		
		if(query != null && !"null".equals(query))
		{		
			getAssistantManager(inReq).semanticSearch(inReq);
		}
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
			getAssistantManager(inReq).semanticSearch(inReq);
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

	public void prepareDataForGuide(WebPageRequest inReq) throws Exception 
	{
		MediaArchive archive = getMediaArchive(inReq);
		

		String chatterboxhome = inReq.getRequestParameter("chatterboxhome");
		inReq.putPageValue("chatterboxhome", chatterboxhome);
		
		Data entity = (Data) inReq.getPageValue("entity");
		if(entity == null)
		{
			String entityid = inReq.getRequestParameter("entityid");
			entity = archive.getCachedData("entity", entityid);
			inReq.putPageValue("entity", entity);
		}
		
		Data module = (Data) inReq.getPageValue("module");
		if(module == null)
		{
			String moduleid = inReq.getRequestParameter("moduleid");
			module = archive.getCachedData("module", moduleid);
			inReq.putPageValue("module", module);
		}
		
		Collection<GuideStatus> statuses =  getAssistantManager(inReq).prepareDataForGuide(module, entity);
		boolean refresh= false;
		for(GuideStatus stat : statuses)
		{
			if(!stat.isReady())
			{
				refresh = true;
				break;
			}
		}
		inReq.putPageValue("refresh", refresh);
		inReq.putPageValue("statuses", statuses);
	}

}
