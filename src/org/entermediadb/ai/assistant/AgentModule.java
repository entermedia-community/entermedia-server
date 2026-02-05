package org.entermediadb.ai.assistant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;

public class AgentModule extends BaseMediaModule {
	
	public AssistantManager getAssistantManager(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		AssistantManager assistantManager = (AssistantManager) getMediaArchive(catalogid).getBean("assistantManager");
		return assistantManager;
	}

	public CreationManager getCreationManager(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		CreationManager creationManager = (CreationManager) getMediaArchive(catalogid).getBean("creationManager");
		return creationManager;
	}

	public QuestionsManager getQuestionsManager(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		QuestionsManager questionsManager = (QuestionsManager) getMediaArchive(catalogid).getBean("questionsManager");
		return questionsManager;
	}
	
	public SearchingManager getSearchingManager(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		SearchingManager searchingManager = (SearchingManager) getMediaArchive(catalogid).getBean("searchingManager");
		return searchingManager;
	}
	public void searchTables(WebPageRequest inReq) throws Exception 
	{	
		AgentContext agentContext =  (AgentContext)inReq.getPageValue("agentcontext");
		
		getSearchingManager(inReq).searchTables(inReq, agentContext.getAiSearchParams());
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
			getSearchingManager(inReq).semanticSearch(inReq);
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
			getSearchingManager(inReq).semanticSearch(inReq);
		}
	}

	public void addModules(WebPageRequest inReq) throws Exception 
	{
		ScriptLogger log = (ScriptLogger)inReq.getPageValue("log");
		AssistantManager assistant = (AssistantManager) getMediaArchive(inReq).getBean("assistantManager");
		assistant.addMissingFunctions(log);
	}

	public void createSuggestions(WebPageRequest inReq)
	{
		getMediaArchive(inReq).fireSharedMediaEvent("llm/autocreatesuggestions");
	}

	public void prepareDataForGuide(WebPageRequest inReq) throws Exception 
	{
		MediaArchive archive = getMediaArchive(inReq);

		String chatterboxhome = inReq.getRequestParameter("chatterboxhome");
		inReq.putPageValue("chatterboxhome", chatterboxhome);
		
		AgentContext context = loadAgentContext(inReq);
		
		String moduleid = context.get("entitymoduleid");
		if (moduleid == null) {
			moduleid = inReq.getRequestParameter("moduleid");
		}
		Data module = archive.getCachedData("module", moduleid);
		inReq.putPageValue("module", module);
		
		String entityid = context.get("entityid");
		if (entityid == null) {
			entityid = inReq.getRequestParameter("entityid");
		}
		
		Data entity = archive.getCachedData(moduleid, entityid);
		inReq.putPageValue("entity", entity);
		
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
		
		if (refresh)
		{
			
			context.setValue("wait", 1000L);
			context.setNextFunctionName(context.getFunctionName());
		}
	}
	
	public void loadModuleSchemaForJson(WebPageRequest inReq) throws Exception 
	{
		AssistantManager assistant = (AssistantManager) getMediaArchive(inReq).getBean("assistantManager");
		Map<String,String> modulesenum = assistant.getModulesAsEnum();
		inReq.putPageValue("modulesenum", modulesenum);
	}
	
	public void loadSuggestions(WebPageRequest inReq) throws Exception 
	{
		String toplevel = inReq.getRequestParameter("toplevelaifunctionid");
		if(toplevel == null)
		{
			return;
		}
		
		String entityid = inReq.getRequestParameter("entityid");
		String entitymoduleid = inReq.getRequestParameter("entitymoduleid");
		
		Searcher aifunctionsearcher = getMediaArchive(inReq).getSearcher("aifunction");
				
		Searcher aisuggestions = getMediaArchive(inReq).getSearcher("aisuggestion");
		
		HitTracker hits = aisuggestions.query()
					.exact("aifunction", toplevel)
					.exact("entityid", entityid).search();
		
		Collection<Map<String, String>> suggestions = new ArrayList<Map<String, String>>();
		
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			
			Map<String, String> suggestion = new HashMap<String, String>();
			suggestion.put("name", data.getName());
			suggestion.put("prompt", data.get("prompt"));
			
			suggestions.add(suggestion);
			
		}
		inReq.putPageValue("suggestions", suggestions);
	}
	
	public void loadSearchSuggestions(WebPageRequest inReq) throws Exception 
	{
		SearchingManager searching = (SearchingManager) getMediaArchive(inReq).getBean("searchingManager");
		Collection<String> suggestions = searching.makeSearchSuggestions(inReq.getUserProfile());
		inReq.putPageValue("suggestions", suggestions);
	}
	
	public void saveAgentContextField(WebPageRequest inReq) throws Exception 
	{
		AgentContext agentContext =  (AgentContext) inReq.getPageValue("agentcontext");
		String fieldname = inReq.getRequestParameter("fieldname");
		String fieldvalue = inReq.getRequestParameter("fieldvalue");
		agentContext.setValue(fieldname, fieldvalue);
		Searcher searcher =  getMediaArchive(inReq).getSearcher("agentcontext");
		searcher.saveData(agentContext, inReq.getUser());
	}
	
	public void loadTutorials(WebPageRequest inReq) throws Exception 
	{
		Searcher tutorialsearcher = getMediaArchive(inReq).getSearcher("aitutorials");
		HitTracker hits = tutorialsearcher.query().exact("featured", true).search();
		
		inReq.putPageValue("tutorials", hits);
	}
	
	public void startFunction(WebPageRequest inReq) throws Exception
	{
		
		AssistantManager assistantManager = (AssistantManager) getMediaArchive(inReq).getBean("assistantManager");
		
		//Get the contenxt and update it first
		String channelid = inReq.getRequestParameter("channel");
		AgentContext agentContext = assistantManager.loadContext(channelid);
		String toplevel = inReq.getRequestParameter("toplevelaifunctionid");
		String previousTopLevel = agentContext.getTopLevelFunctionName();
		
		boolean changed = false;
		if( toplevel != null && !toplevel.equals(previousTopLevel) )
		{
			agentContext.setTopLevelFunctionName(toplevel);
			changed = true;
		}

		String functionname = inReq.getRequestParameter("functionname");
		if( functionname != null)
		{
			agentContext.setFunctionName(functionname);
			changed = true;
			
			String playbackentityid = inReq.getRequestParameter("playbackentityid");
			if(playbackentityid != null)
			{
				agentContext.addContext("playbackentityid", playbackentityid);
				agentContext.addContext("playbacksection", inReq.getRequestParameter("playbacksection"));
			}
		}
		
		if( changed )
		{
			getMediaArchive(inReq).saveData("agentcontext",agentContext);
			
			//Now that Context is set. Let the chat respond
			
			assistantManager.sendSystemMessage(agentContext,inReq.getUserName(),null);
		}
		//Refresh drop down area?
	}
	
	public AgentContext loadAgentContext(WebPageRequest inReq) throws Exception
	{
		AssistantManager assistantManager = (AssistantManager) getMediaArchive(inReq).getBean("assistantManager");
		
		//Get the contenxt and update it first
		String channelid = inReq.getRequestParameter("channel");
		if( channelid == null)
		{
			Data currentchannel = (Data)inReq.getPageValue("currentchannel");
			channelid = currentchannel.getId();
		}
		AgentContext context = assistantManager.loadContext(channelid);
		
		inReq.putPageValue("agentcontext", context);
		
		String toplevel = inReq.getRequestParameter("toplevelaifunctionid");

		if( toplevel == null && context.getTopLevelFunctionName() == null)
		{
			inReq.setRequestParameter("channel", channelid);
			inReq.setRequestParameter("toplevelaifunctionid", "auto_detect_welcome");
			inReq.setRequestParameter("functionname", "auto_detect_welcome");
			startFunction(inReq);
			return context;
		}
		
//		if( toplevel != null )
//		{
//			context.setTopLevelFunctionName(toplevel);
//		}
//		String functionname = inReq.getRequestParameter("functionname");
//		if( functionname != null )
//		{
//			context.setFunctionName(functionname);
//		}
//		if( toplevel != null ||functionname != null )
//		{
//			getMediaArchive(inReq).saveData("agentcontext",context);
//		}
		return context;
		
		//Now that Context is set. Let the chat respond
		//Refresh drop down area?
	}
	public void monitorChannels(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		AssistantManager assistantManager = (AssistantManager) archive.getBean("assistantManager");
		ScriptLogger log = (ScriptLogger) inReq.getPageValue("log");
		assistantManager.monitorChannels(log);
	}
	
	
	public void verifyRevisions(WebPageRequest inReq)
	{
		Data data = (Data)inReq.getPageValue("data");
		
		
	}
	
	public void monitorAiServers(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		AssistantManager assistantManager = (AssistantManager) archive.getBean("assistantManager");
		ScriptLogger log = (ScriptLogger) inReq.getPageValue("log");
		assistantManager.monitorAiServers(log);
	}
		
}
