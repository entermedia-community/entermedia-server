package org.entermediadb.ai.assistant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.ai.creator.CreatorManager;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
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

	/*
	public void mcpGenerateReport(WebPageRequest inReq) throws Exception {
		JSONObject arguments = (JSONObject) inReq.getPageValue("arguments");
		String report = getSearchingManager(inReq).generateReport(arguments);
		inReq.putPageValue("report", report);
	}*/

	public void indexActions(WebPageRequest inReq) throws Exception 
	{
		ScriptLogger logger = (ScriptLogger)inReq.getPageValue("log");
		Collection<SemanticAction> actions = new ArrayList();
		
		Collection<SemanticAction> found = getSearchingManager(inReq).createPossibleFunctionParameters(logger);
		actions.addAll(found);

		found = getCreationManager(inReq).createPossibleFunctionParameters(logger);
		actions.addAll(found);

		found = getQuestionsManager(inReq).createPossibleFunctionParameters(logger);
		actions.addAll(found);

		Searcher embedsearcher = getMediaArchive(inReq).getSearcher("aifunctionparameter");

		//TODO: Call the other ones
		
		//Save to db
		Collection tosave = new ArrayList();
		
		for (Iterator iterator2 = actions.iterator(); iterator2.hasNext();)
		{
			SemanticAction semanticAction = (SemanticAction) iterator2.next();
			Data data = embedsearcher.createNewData();
			if(semanticAction.getParentData() != null)
			{				
				data.setValue("parentmodule",semanticAction.getParentData().getId());
			}
			if( semanticAction.getChildData() != null)
			{
				data.setValue("childmodule",semanticAction.getChildData().getId());
			}
			data.setValue("vectorarray",semanticAction.getVectors());
			data.setValue("aifunction",semanticAction.getAiFunction());
			data.setName(semanticAction.getSemanticText());
			
			tosave.add(data);
		}
		embedsearcher.saveAllData(tosave, null);
		
		
		//Now save all the suggestions?
		
		//Test search
		//populateVectors(manager,actions);

		//manager.reinitClusters(inLog); //How to do this?
	}

	public void saveSuggestions(WebPageRequest inReq) throws Exception 
	{
		ScriptLogger logger = (ScriptLogger)inReq.getPageValue("log");
//		getSearchingManager(inReq).savePossibleFunctionSuggestions(logger);
		getCreationManager(inReq).savePossibleFunctionSuggestions(logger);
		getQuestionsManager(inReq).savePossibleFunctionSuggestions(logger);
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
	
	public void loadModuleSchemaForJson(WebPageRequest inReq) throws Exception 
	{
		AssistantManager assistant = (AssistantManager) getMediaArchive(inReq).getBean("assistantManager");
		Map<String,String> modulesenum = assistant.getModulesAsEnum();
		inReq.putPageValue("modulesenum", modulesenum);
	}
	
	public void loadSuggestions(WebPageRequest inReq) throws Exception 
	{
		String functiongroup = (String) inReq.findValue("functiongroup");
		if(functiongroup == null)
		{
			return;
		}
		
		Searcher aifunctionsearcher = getMediaArchive(inReq).getSearcher("aifunction");
		
		HitTracker functionhits = aifunctionsearcher.query().exact("functiongroup", functiongroup).search();
		Collection<String> functionids = functionhits.collectValues("id");
		functionids.add("start" + functiongroup); //Also add self
		
		Searcher aisuggestions = getMediaArchive(inReq).getSearcher("aisuggestion");
		
		HitTracker hits = aisuggestions.query().orgroup("aifunction", functionids).exact("featured", "true").search();
		
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
	
	public void loadTutorials(WebPageRequest inReq) throws Exception 
	{
		Searcher tutorialsearcher = getMediaArchive(inReq).getSearcher("aitutorials");
		HitTracker hits = tutorialsearcher.query().exact("featured", true).search();
		
		inReq.putPageValue("tutorials", hits);
	}
	
	public void saveTutorial(WebPageRequest inReq) throws Exception 
	{
		CreatorManager creatorManager = (CreatorManager) getMediaArchive(inReq).getBean("creatorManager");
		creatorManager.createTutorial(inReq);
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
	
	
	public void loadCreator(WebPageRequest inReq) throws Exception 
	{
		CreatorManager creatorManager = (CreatorManager) getMediaArchive(inReq).getBean("creatorManager");
		creatorManager.getCreator(inReq);
	}
	
	public void createCreatorSection(WebPageRequest inReq) throws Exception 
	{
		
		CreatorManager creatorManager = (CreatorManager) getMediaArchive(inReq).getBean("creatorManager");
		String tutorialid = inReq.getRequestParameter("tutorialid");
		Searcher tutorialsearcher = getMediaArchive(inReq).getSearcher("aitutorials");
		Data tutorial = tutorialsearcher.loadData(tutorialid);
		
		if( tutorial == null)
		{
			throw new IllegalArgumentException("No tutorial found for id: " + tutorialid);
		}
		
		try
		{
			Map fields = new HashMap();
			String sectionid = inReq.getRequestParameter("sectionid");
			if( sectionid != null && sectionid.length() > 0)
			{
				fields.put("sectionid", sectionid);
				String name = inReq.getRequestParameter("name");
				fields.put("name", name);
			}
			else
			{
				String ordering = inReq.getRequestParameter("ordering");
				int orderingint = Integer.parseInt(ordering);
				fields.put("ordering", orderingint + 1);				
			}
			
			
			Data section = creatorManager.createCreatorSection(tutorial, fields);
			inReq.putPageValue("tutorial", tutorial);
			inReq.putPageValue("section", section);
		}
		catch( Exception ex)
		{
			throw new IllegalArgumentException("Ordering must be a number");
		}
	}
	
	public void deleteCreatorSection(WebPageRequest inReq) throws Exception 
	{
		String searchtype = inReq.getRequestParameter("searchtype");
		String dataid = inReq.getRequestParameter("id");
		
		CreatorManager creatorManager = (CreatorManager) getMediaArchive(inReq).getBean("creatorManager");
		creatorManager.deleteCreatorSection(searchtype, dataid);
	}
	
	public void createComponentContent(WebPageRequest inReq) throws Exception 
	{
		CreatorManager creatorManager = (CreatorManager) getMediaArchive(inReq).getBean("creatorManager");
		String sectionid = inReq.getRequestParameter("sectionid");
		
		Map component = new HashMap();
		component.put("content", inReq.getRequestParameter("content"));
		String componenttype = inReq.getRequestParameter("componenttype");
		component.put("componenttype", componenttype);
		component.put("ordering", inReq.getRequestParameter("ordering"));
		component.put("componentcontentid", inReq.getRequestParameter("componentcontentid"));
		
		Data componentcontent = creatorManager.createComponentContent(sectionid, component);
		inReq.putPageValue("componentcontent", componentcontent);
	}
	
	public void orderCreatorSection(WebPageRequest inReq)
	{
		CreatorManager creatorManager = (CreatorManager) getMediaArchive(inReq).getBean("creatorManager");
		creatorManager.orderCreatorSection(inReq);
	}
	
}
