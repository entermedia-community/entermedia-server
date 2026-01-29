package org.entermediadb.ai.assistant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.entermediadb.ai.ChatMessageHandler;
import org.entermediadb.ai.creator.CreatorManager;
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

	public void addModules(WebPageRequest inReq) throws Exception 
	{
		ScriptLogger log = (ScriptLogger)inReq.getPageValue("log");
		
		
		//Add toplevelfunctions
		Collection<Data> modules =  getMediaArchive(inReq).getList("module");
		List tosave = new ArrayList();
		for (Iterator iterator = modules.iterator(); iterator.hasNext();)
		{
			MultiValued module = (MultiValued) iterator.next();
			String method = module.get("aicreationmethod");
			if( method != null)
			{
				String id = "welcome_" + module.getId();
				Data found = getMediaArchive(inReq).getData("aifunction",id);
				if( found != null)
				{
					continue;
				}
				String messagehandler = "";
				if( method.equals("fieldsonly"))
				{
					messagehandler = "entityCreationManager";
				}
				else if( method.equals("smartcreator"))
				{
					 messagehandler = "creatorManager";
				}
				
				 found = getMediaArchive(inReq).getSearcher("aifunction").createNewData();
				 found.setId(id);
				 found.setValue("messagehandler", messagehandler);
				 found.setValue("toplevel",true);
				 found.setName("Create " + module.getName());
				 tosave.add(found);
				 
				 //TODO: Add create
				 found = getMediaArchive(inReq).getSearcher("aifunction").createNewData();
				 found.setId("create_" + module.getId());
				 found.setValue("messagehandler", messagehandler);
				 tosave.add(found);
				 
				 log.info("AI "+ module.getName()+" functions created");
			
				
			}
		}
		getMediaArchive(inReq).saveData("aifunction", tosave);
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
		
		Searcher aifunctionsearcher = getMediaArchive(inReq).getSearcher("aifunction");
				
		Searcher aisuggestions = getMediaArchive(inReq).getSearcher("aisuggestion");
		
		HitTracker hits = aisuggestions.query().orgroup("aifunction", toplevel).exact("featured", "true").search();
		
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
	
	public void populateSection(WebPageRequest inReq) throws Exception 
	{
		CreatorManager creatorManager = (CreatorManager) getMediaArchive(inReq).getBean("creatorManager");
		creatorManager.createCreatorAndPopulateSection(inReq);
	}
	
	
	public void loadCreator(WebPageRequest inReq) throws Exception 
	{
		String module = inReq.findValue("module");
		String entityid =  inReq.findValue("entityid");
		
		inReq.setRequestParameter("playbackentityid", entityid);
		inReq.setRequestParameter("playbackentitymoduleid", module);

		CreatorManager creatorManager = (CreatorManager) getMediaArchive(inReq).getBean("creatorManager");
		creatorManager.getCreator(inReq);
	}
	
	public void createCreatorSection(WebPageRequest inReq) throws Exception 
	{
		
		CreatorManager creatorManager = (CreatorManager) getMediaArchive(inReq).getBean("creatorManager");
		String playbackentityid = inReq.getRequestParameter("playbackentityid");
		String playbackentitymoduleid = inReq.getRequestParameter("playbackentitymoduleid");
		
		Searcher searcher = getMediaArchive(inReq).getSearcher(playbackentitymoduleid);
		Data playbackentity = searcher.loadData(playbackentityid);
		
		if( playbackentity == null)
		{
			throw new IllegalArgumentException("No creator entity found for id: " + playbackentityid);
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
			
			
			Data section = creatorManager.createCreatorSection(playbackentity, playbackentitymoduleid, fields);
			inReq.putPageValue("playbackentity", playbackentity);
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
		
		Map componentfields = new HashMap();
		
		String[] fields = inReq.getRequestParameters("field");
		
		Collection<String> fieldlist = new ArrayList<String>();
		
		if( fields != null)
		{
			for (int i = 0; i < fields.length; i++)
			{
				String fieldname = fields[i];
				if( fieldname != null && fieldname.length() > 0)
				{
					fieldlist.add(fieldname);
				}
			}
		}
		
		if(!fieldlist.contains("content"))
		{
			fieldlist.add("content");
		}
		if(!fieldlist.contains("componenttype"))
		{
			fieldlist.add("componenttype");
		}
		if(!fieldlist.contains("ordering"))
		{
			fieldlist.add("ordering");
		}
		if(!fieldlist.contains("componentcontentid"))
		{
			fieldlist.add("componentcontentid");
		}
		
		for (Iterator iterator = fieldlist.iterator(); iterator.hasNext();) 
		{
			String fieldname = (String) iterator.next();
			
			String fieldvalue = inReq.getRequestParameter(fieldname+".value");
			if(fieldvalue == null)
			{
				fieldvalue = inReq.getRequestParameter(fieldname + "value");
			}
			if(fieldvalue == null)
			{
				fieldvalue = inReq.getRequestParameter(fieldname);
			}
			if(fieldvalue == null || fieldvalue.length() == 0)
			{
				continue;
			}
			componentfields.put(fieldname, fieldvalue);
		}
		
		Data componentcontent = creatorManager.createComponentContent(sectionid, componentfields);
		inReq.putPageValue("componentcontent", componentcontent);
		
		if(componentfields.get("componentcontent") == null)
		{
			inReq.putPageValue("newcontent", true);
		}
	}
	
	public void populateComponentContent(WebPageRequest inReq) throws Exception 
	{
		CreatorManager creatorManager = (CreatorManager) getMediaArchive(inReq).getBean("creatorManager");
		creatorManager.autoPopulateComponentContent();
	}
	
	public void orderCreatorSection(WebPageRequest inReq)
	{
		CreatorManager creatorManager = (CreatorManager) getMediaArchive(inReq).getBean("creatorManager");
		creatorManager.orderCreatorSection(inReq);
	}
	
	public void duplicateCreatorSection(WebPageRequest inReq)
	{
		String searchtype = inReq.getRequestParameter("searchtype");
		String dataid = inReq.getRequestParameter("id");
		
		CreatorManager creatorManager = (CreatorManager) getMediaArchive(inReq).getBean("creatorManager");
		Data duplicate = creatorManager.duplicateCreatorSection(searchtype, dataid);
		
		inReq.putPageValue("searchtype", searchtype);

		if("componentcontent".equals(searchtype))
		{
			inReq.putPageValue("componentcontent", duplicate);
		}
		else if("componentsection".equals(searchtype))
		{			
			inReq.putPageValue("section", duplicate);
		}
	}

	public void startFunction(WebPageRequest inReq) throws Exception
	{
		
		AssistantManager assistantManager = (AssistantManager) getMediaArchive(inReq).getBean("assistantManager");
		
		//Get the contenxt and update it first
		String channelid = inReq.getRequestParameter("channel");
		AgentContext context = assistantManager.loadContext(channelid);
		String toplevel = inReq.getRequestParameter("toplevelaifunctionid");
		
		boolean changed = false;
		if( toplevel != null && !toplevel.equals(context.getTopLevelFunctionName() ) )
		{
			context.setTopLevelFunctionName(toplevel);
			changed = true;
		}
		String functionname = inReq.getRequestParameter("functionname");
		if( functionname != null && !functionname.equals(context.getFunctionName()))
		{
			context.setFunctionName(functionname);
			changed = true;
		}
		if( changed )
		{
			getMediaArchive(inReq).saveData("agentcontext",context);
			
			//Now that Context is set. Let the chat respond
			
			assistantManager.sendSystemMessage(context,inReq.getUserName(),null);
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
			//context.setTopLevelFunctionName("welcomeAutoDetectConversation");
			//context.setFunctionName("welcomeAutoDetectConversation");
			inReq.setRequestParameter("channel", channelid);
			inReq.setRequestParameter("toplevelaifunctionid", "welcomeAutoDetectConversation");
			inReq.setRequestParameter("functionname", "welcomeAutoDetectConversation");
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
	
	public void creatorAiAction(WebPageRequest inReq)
	{
		String aiaction = inReq.getRequestParameter("aiaction");
		String componentcontentid = inReq.getRequestParameter("componentcontentid");
		
		CreatorManager creatorManager = (CreatorManager) getMediaArchive(inReq).getBean("creatorManager");
		
		if("grammar".equals(aiaction))
		{
			creatorManager.correctGrammar(inReq, componentcontentid);
		}
		else if("improve".equals(aiaction))
		{
			creatorManager.improveContent(inReq, componentcontentid);
		}
		else if("generate".equals(aiaction))
		{
			String prompt = inReq.getRequestParameter("aiprompt");
			creatorManager.generateContent(inReq, componentcontentid, prompt);
		}
		else if("image".equals(aiaction))
		{
			String prompt = inReq.getRequestParameter("aiprompt");
			creatorManager.createImage(inReq, componentcontentid, prompt);
		}
		else if("caption".equals(aiaction))
		{
			String assetid = inReq.getRequestParameter("assetid");
			creatorManager.captionImage(inReq, componentcontentid, assetid);
		}
	}
	
}
