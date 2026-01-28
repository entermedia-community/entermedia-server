package org.entermediadb.ai.assistant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.ChatMessageHandler;
import org.entermediadb.ai.Schema;
import org.entermediadb.ai.classify.SemanticClassifier;
import org.entermediadb.ai.informatics.SemanticTableManager;
import org.entermediadb.ai.knn.RankedResult;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.find.ResultsManager;
import org.entermediadb.markdown.MarkdownUtil;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.profile.UserProfile;

public class SearchingManager extends BaseAiManager  implements ChatMessageHandler
{
	private static final Log log = LogFactory.getLog(SearchingManager.class);

	@Override
	public void savePossibleFunctionSuggestions(ScriptLogger inLog)
	{
		// Do nothing
	}

	
	public void searchTables(WebPageRequest inReq, AiSearch inAiSearchParams)
	{
		AiSearchTable step1 = inAiSearchParams.getStep1();
		AiSearchTable step2 = inAiSearchParams.getStep2();
		

		//		AiSearchPart part3 = inAiSearchParams.getPart3();
		
		HitTracker entityhits = null;
		HitTracker assethits = null;
		
		String step1Keyword = null;
		String step2Keyword = null;
		
		if(step1 != null && step2 != null)
		{
			
			step1Keyword = step1.getParameterValues();
			
			HitTracker foundhits = getMediaArchive().query(step1.getModule().getId()).freeform("description", step1Keyword).search();
			
			if( foundhits.isEmpty() )
			{
				return;
			}
			Collection<String> ids = foundhits.collectValues("id");

			inReq.putPageValue("module", step2.getModule());
			
			QueryBuilder search = getMediaArchive().query(step2.getModule().getId()).named("assitedsearch").orgroup(step1.getModule().getId(),ids);
			step2Keyword = step2.getParameterValues();
			if( step2Keyword != null)
			{
				if (step1Keyword == null || !step1Keyword.equalsIgnoreCase(step2Keyword))  //Remove the keyword if is  same
				{
					//Remove Module Name from Keywords
					/*
					String step1table = step1.getTargetTable();
					step2Keyowrd = step2Keyowrd.replace(step1table, "");
					if (step1table.endsWith("s"))
					{
						step1table = step1table.substring(0, step1table.length()-1);
						step2Keyowrd = step2Keyowrd.replace(step1table, "");
					}
					*/
					search.freeform("description", step2Keyword);
				}
			}
			entityhits = search.search();
			
			step2.setCount((long) entityhits.size());
			
			//inReq.putPageValue( finalhits.getSessionId(), finalhits);
			
		}
		else if(step1 != null)
		{
			
			step1Keyword  = step1.getParameterValues();
			
			inReq.putPageValue("module", step1.getModule());
			if(step1Keyword != null)
			{
				QueryBuilder search = null;
				
				Schema schema = loadSchema();
				Collection<Data> modules = schema.getChildrenOf(step1.getModule().getId());
				Collection<String> moduleids = new ArrayList<String>();
				moduleids.add(step1.getModule().getId());
				//step1.addModule(step1.getModule());
				for (Iterator iterator = modules.iterator(); iterator.hasNext();)
				{
					Data mod = (Data) iterator.next();
					moduleids.add(mod.getId());
					step1.addModule(mod);
				}
				
				if ( moduleids.contains("modulesearch") || moduleids.size() > 1)
				{
					
					if( moduleids.size() == 1 || moduleids.contains("modulesearch"))
					{
						moduleids = schema.getModuleIds();
					}
					
					search = getMediaArchive().query("modulesearch")
						.addFacet("entitysourcetype")
						.put("searchtypes", moduleids).includeDescription(true);
					entityhits = search.freeform("description", step1Keyword).search();
					log.info(" Here:  "+ entityhits.getActiveFilterValues() );
					//TODO: Not sure this is needed or works
					if( moduleids.contains("asset") )
					{
						//Loop over the categories in these entityhits
						Collection categories = entityhits.collectValues("rootcategory");
						search = getMediaArchive().query("asset").orgroup("category",categories);
						assethits = search.freeform("description", step1Keyword).search();
						step1.setCount( (long) (entityhits.size()  + assethits.size()));
					}
					else
					{
						step1.setCount((long) entityhits.size());
					}
					
				}
				else if( "asset".equals( step1.getModule().getId() ) )
				{
					search = getMediaArchive().query(step1.getModule().getId());
					assethits = search.freeform("description", step1Keyword).search();
					step1.setCount((long) assethits.size());
				}	
				else {
					search = getMediaArchive().query(step1.getModule().getId());
					entityhits = search.freeform("description", step1Keyword).search();
					step1.setCount((long) entityhits.size());
				}
			}
			else
			{
				//step1.addModule(step1.getModule());
				entityhits = getMediaArchive().query(step1.getModule().getId()).all().search();
				step1.setCount((long) entityhits.size()); 
			}
			
		}
		
		if(entityhits != null )
		{
			inReq.putPageValue("hits", entityhits);
		}
		else
		{
			inReq.putPageValue("hits", assethits);
		}
		
		organizeResults(inReq, entityhits, assethits);
		
		String semanticquery = "";
		
		if(step1Keyword != null)
		{
			semanticquery = step1Keyword;
		}
		if(step2Keyword != null)
		{
			semanticquery = semanticquery + " " + step2Keyword;
		}
		
		if(semanticquery.isBlank())
		{
			String originalQuery = inAiSearchParams.getOriginalSearchString();
			if( originalQuery != null)
			{
				originalQuery = originalQuery.replaceAll("search|find|look for|show me", "").trim();
				semanticquery = originalQuery;
			}
		}

		if(!semanticquery.isBlank())
		{			
			inReq.putPageValue("semanticquery", semanticquery.trim());
		}
		
	}

	public ResultsManager getResultsManager() {
		ResultsManager resultsManager = (ResultsManager) getMediaArchive().getBean("resultsManager");
		return resultsManager;
	}
	
	public void organizeResults(WebPageRequest inReq, HitTracker entityhits, HitTracker assetunsorted) 
	{

		inReq.putPageValue("assethits", assetunsorted);
		
		if(entityhits != null && assetunsorted == null)
		{
			inReq.putPageValue("totalhits", entityhits.size());
		}
		else if(entityhits == null && assetunsorted != null)
		{
			inReq.putPageValue("totalhits", assetunsorted.size());
		}
		else
		{
			inReq.putPageValue("totalhits", entityhits.size() + assetunsorted.size());
		}

		getResultsManager().loadOrganizedResults(inReq, entityhits, assetunsorted);
		
		/**
		 * log.info("Searching as:" + inReq.getUser().getName());
		MediaArchive archive = getMediaArchive();

		Collection<String> keywords = searchArgs.getKeywords();
		
		String plainquery = String.join(" ", keywords);
		
		QueryBuilder dq = archive.query("modulesearch").addFacet("entitysourcetype").freeform("description", plainquery).hitsPerPage(30);
		dq.getQuery().setIncludeDescription(true);
		
		Collection searchmodules = getResultsManager().loadUserSearchTypes(inReq, searchArgs.getSelectedModuleIds());
		
		Collection searchmodulescopy = new ArrayList(searchmodules);
		searchmodulescopy.remove("asset");
		dq.getQuery().setValue("searchtypes", searchmodulescopy);
		
		
		HitTracker unsorted = dq.search(inReq);
		
		log.info(unsorted);

		Map<String,String> keywordsLower = new HashMap();
		
		getResultsManager().collectMatches(keywordsLower, plainquery, unsorted);
		
		inReq.putPageValue("modulehits", unsorted);
		inReq.putPageValue("livesearchfor", plainquery);
		
		List finallist = new ArrayList();
		
		for (Iterator iterator = keywordsLower.keySet().iterator(); iterator.hasNext();)
		{
			String keyword = (String) iterator.next();
			String keywordcase = keywordsLower.get(keyword);
			finallist.add(keywordcase);
		}

		Collections.sort(finallist);
		
		
		inReq.putPageValue("livesuggestions", finallist);
		inReq.putPageValue("highlighter", new Highlighter());
		
		int assetmax = 15;
		if( unsorted.size() > 10)
		{
			assetmax = 5;
		}
		
		QueryBuilder assetdq = archive.query("asset")
				.freeform("description", plainquery)
				.hitsPerPage(assetmax);
				
		HitTracker assetunsorted = assetdq.search(inReq);
		getResultsManager().collectMatches(keywordsLower, plainquery, assetunsorted);
		inReq.putPageValue("assethits", assetunsorted);
		
		Collection pageOfHits = unsorted.getPageOfHits();
		pageOfHits = new ArrayList(pageOfHits);
		
		String[] excludeentityids = new String[unsorted.size()];
		String[] excludeassetids = new String[assetunsorted.size()];
		
		StringBuilder contextString = new StringBuilder();
		
		int idx = 0;
		for (Object entity : unsorted.getPageOfHits()) {
			Data d = (Data) entity;
			
			String parentassetid = d.get("parentasset");
			if(parentassetid != null)
			{
				String fulltext = d.get("longdescription");
				if(fulltext == null || fulltext.length() == 0)
				{
					Asset parent = archive.getAsset(parentassetid);
					fulltext = parent.get("fulltext");
				}
				if(fulltext != null && fulltext.length() > 0)
				{
					contextString.append("From " + d.getName() + "\n");
					contextString.append(fulltext);
					contextString.append("\n\n");
				}
			}
			excludeentityids[idx] = d.getId();
			idx++;
		}
		idx = 0;
		for (Object asset : assetunsorted.getPageOfHits()) {
			Data d = (Data) asset;
			
			String fulltext = d.get("longdescription");
			if(fulltext != null && fulltext.length() > 0)
			{
				contextString.append("From " + d.getName() + "\n");
				contextString.append(fulltext);
				contextString.append("\n\n");
			}
			
			excludeassetids[idx] = d.getId();
			idx++;
		}
		inReq.putPageValue("excludeentityids", excludeentityids);
		inReq.putPageValue("excludeassetids", excludeassetids);
		
		inReq.putPageValue("totalhits", unsorted.size() + assetunsorted.size());
		
		getResultsManager().loadOrganizedResults(inReq, unsorted,assetunsorted);
		
		if( contextString.length() > 0)
		{
			Data ragcontext = archive.getSearcher("ragcontext").createNewData();
			ragcontext.setValue("", "");
		}
		*/
		
	}
	
	public Collection<SemanticAction> createPossibleFunctionParameters(ScriptLogger inLog)
	{
		SemanticTableManager manager = loadSemanticTableManager("aifunctionparameter");
		
		//Create batch of english words that describe how to search all these things
		Schema schema = loadSchema();
		
		Searcher embedsearcher = getMediaArchive().getSearcher("aifunctionparameter");
		Collection<SemanticAction> actions = new ArrayList();

		for (Iterator iterator = schema.getModules().iterator(); iterator.hasNext();)
		{
			Data parentmodule = (Data) iterator.next();
			
			Collection existing = embedsearcher.query().exact("aifunction", "searchTables").exact("parentmodule",parentmodule.getId()).search();
			if( !existing.isEmpty())
			{
				log.info("Skipping " + parentmodule);
				continue;
			}
			
			SemanticAction action = new SemanticAction();
			/*
			 * "search",
							"creation",
							"how-to",
							"task",
							"conversation",
							"support request"
							*/
			action.setAiFunction("searchTables");
			action.setSemanticText("Search for " + parentmodule.getName());
			action.setParentData(parentmodule);
			actions.add(action);
//			action = new SemanticAction();
//			action.setParentData(parentmodule);
//			action.setAiFunction("createRecord");
//			action.setSemanticText("Create a new " + parentmodule.getName());
//			actions.add(action);
			
			//Check for child views
			Collection<Data> children = schema.getChildrenOf(parentmodule.getId());
			
			for (Iterator iterator2 = children.iterator(); iterator2.hasNext();)
			{
				Data childmodule = (Data) iterator2.next();
				action = new SemanticAction();
				action.setParentData(parentmodule);
				action.setChildData(childmodule);
				action.setAiFunction("searchTables");
				action.setSemanticText("Search for " + childmodule.getName() + " in " + parentmodule.getName());
				actions.add(action);
			}
			populateVectors(manager,actions);
		}

		return actions;
//		List<Double> tosearch = manager.makeVector("Find all records in US States in 2023");
//		Collection<RankedResult> results = manager.searchNearestItems(tosearch);
//		log.info(results);
		
		
	}


	
	protected String parseSearchParts(AgentContext inAgentContext, JSONObject structure, String type, String messageText) 
	{
		ArrayList tables = (ArrayList) structure.get("tables");
		
		if( tables == null)
		{
			return null;
		}
		
		log.info("AI Assistant Searching Tables: " + structure.toJSONString());
		
		AiSearch search = inAgentContext.getAiSearchParams();
		search.setOriginalSearchString(messageText);
		
		search.setStep1(null);
		search.setStep2(null);
		search.setStep3(null);
		
		for (Iterator iterator = tables.iterator(); iterator.hasNext();)
		{
			JSONObject jsontable = (JSONObject) iterator.next();
			AiSearchTable searchtable = new AiSearchTable();
			
			Map parameters = (Map) jsontable.get("parameters");
			searchtable.setParameters(parameters);

			String targetTable = (String) jsontable.get("table_name");
			
			if( "all".equalsIgnoreCase(targetTable))
			{
				if(parameters.keySet().contains("module"))
				{
					targetTable = (String) parameters.get("module");
				}
				else
				{
					targetTable = null;
				}
			}
			
			if(targetTable != null)
			{
				String[] check = targetTable.split("\\|");
				if( check.length == 2)
				{
					targetTable = check[1];
				}
				else
				{
					targetTable = check[0];
				}
			}
			
			searchtable.setTargetTable(targetTable);
			
			String foreigntablename = (String) jsontable.get("foreign_table");
			if( foreigntablename != null)
			{
				AiSearchTable ftable = new AiSearchTable();
				ftable.setTargetTable(foreigntablename);
				
				Map foreignparameters = (Map) jsontable.get("foreign_table_parameters");
				ftable.setParameters(foreignparameters);
				
				searchtable.setForeignTable(ftable);
				
			}
			
			
			if (search.getStep1() == null)
			{
				search.setStep1(searchtable);
			}
			else
			{
//				if( "join".equals( step.get("operation")) || search.getStep1().getTargetTable().equals(targetTable) )
//				{
//					continue; //Duplicate
//				}
				if (search.getStep2() == null)
				{
					search.setStep2(searchtable);
				}
				else if (search.getStep3() == null)
				{
					search.setStep3(searchtable);
				}
			}
			
//			if("all".equals(targetTable))
//			{
//				return "searchMultiple";
//			}

		}
		
		if(search.getStep1() == null)
		{
			type = "conversation"; 
			return type;
		}

		if( search.getStep1().getTargetTable() == null )
		{
			Data modulesearch = getMediaArchive().getCachedData("module", "modulesearch");
			search.getStep1().setModule(modulesearch);
			type = "searchTables";
		}
		else
		{
			
			if(search.getStep2() == null && search.getStep1().getForeignTable() != null)
			{
				search.setStep2( search.getStep1() );
				
				search.setStep1( search.getStep2().getForeignTable() );
			}
			
			String text = "Search";
			
			if( search.getStep2() != null)
			{
				text = text + " for " + search.getStep2().getTargetTable() + " in " + search.getStep1().getTargetTable();
			}
			else if( search.getStep1() != null)
			{
				text = text + " for " + search.getStep1().getTargetTable();
			}
			
			SemanticTableManager manager = loadSemanticTableManager("aifunctionparameter"); 
			List<Double> tosearch = manager.makeVector(text);
			Collection<RankedResult> suggestions = manager.searchNearestItems(tosearch);
			//Load more details into this request and possibly change the type
			if( !suggestions.isEmpty())
			{
				inAgentContext.setRankedSuggestions(suggestions);
				RankedResult top = (RankedResult)suggestions.iterator().next();
				if ( top.getDistance() < .7 )
				{
					type = top.getEmbedding().get("aifunction");  //More specific type of search
				
					AiSearch aisearch = processAISearchArgs(structure, top.getEmbedding(), inAgentContext);
					inAgentContext.setAiSearchParams(aisearch);
				}
			}
			else
			{
				type = "conversation";
			}
		}
		return type;
	}
	public AiSearch processAISearchArgs(JSONObject airesults, Data inEmbeddingMatch, AgentContext inContext)
	{
		//Search for tomatoes in sales departments
		//airesults
		AiSearch tables = inContext.getAiSearchParams();
		
		if (inEmbeddingMatch != null)
		{
			String parentid = inEmbeddingMatch.get("parentmodule");
			Data parentmodule = getMediaArchive().getCachedData("module", parentid);
			if (parentmodule != null)
			{
				tables.getStep1().setModule(parentmodule);
			}
			if (tables.getStep2() != null) //Not needed?
			{
				String childid = inEmbeddingMatch.get("childmodule");
				Data childmodule = getMediaArchive().getCachedData("module", childid);
				if (childmodule != null)
				{
					tables.getStep2().setModule(childmodule);
				}
			}
		}
		return tables;
	}

	@Override
	public LlmResponse processMessage(AgentContext inAgentContext, MultiValued inAgentMessage, MultiValued inAiFunction)
	{
		String agentFn = inAgentContext.getFunctionName();
		
		if ("welcomeSearch".equals(agentFn))
		{
			inAgentMessage.setValue("chatmessagestatus", "completed");
			
			LlmConnection llmconnection = getMediaArchive().getLlmConnection(inAiFunction.getId()); //Should stay startSearch
			LlmResponse response = llmconnection.renderLocalAction(inAgentContext);
			inAgentContext.setFunctionName("startSearch");
			return response;
		}
 
		if ("startSearch".equals(agentFn))
		{
			MultiValued usermessage = (MultiValued)getMediaArchive().getCachedData("chatterbox", inAgentMessage.get("replytoid"));
			MultiValued function = (MultiValued)getMediaArchive().getCachedData("aifunction", agentFn);

			LlmResponse response = startChat(inAgentContext, usermessage, inAgentMessage, function);
			
			//Handle right now
			String responseFn = response.getFunctionName();
			if ("conversation".equals(responseFn))
			{
				inAgentMessage.setValue("chatmessagestatus", "completed");
				
				String generalresponse  = response.getMessage();
				if(generalresponse != null)
				{
					MarkdownUtil md = new MarkdownUtil();
					generalresponse = md.render(generalresponse);
					//inAgentMessage.setValue("message",generalresponse);
				}
				//LlmResponse respond = new EMediaAIResponse();
				response.setMessage(generalresponse);
				
				inAgentContext.setNextFunctionName(null);

			}
			else
			{
				response.setMessage("");
				inAgentContext.setNextFunctionName(responseFn);
			}
			return response;
		}
		else if ("searchTables".equals(agentFn))
		{
			//search
			LlmConnection searcher = getMediaArchive().getLlmConnection("searchTables");
			LlmResponse response =  searcher.renderLocalAction(inAgentContext);
			
			String message = response.getMessage();
			
			if(message != null)
			{
				inAgentContext.setMessagePrefix(message);
			}
			
			return response;
		}
		else if ("searchSemantic".equals(agentFn))
		{	
			if("searchSemantic".equals(agentFn))
			{
				getMediaArchive().saveData("chatterbox",inAgentMessage); //TODO Why are we saving this?
			}

			LlmConnection llmconnection = getMediaArchive().getLlmConnection("searchSemantic");
			LlmResponse result = llmconnection.renderLocalAction(inAgentContext);
			return result;
		}
		
		throw new OpenEditException("Function not supported " + agentFn);
		
				
//		if(generalresponse != null)
//		{
//			MarkdownUtil md = new MarkdownUtil();
//			response.setMessage(md.render(generalresponse));
//		}
		
		//respond.setFunctionName(null);
//		getMediaArchive().saveData("chatterbox",inMessage);
//		server.broadcastMessage(getMediaArchive().getCatalogId(), inMessage);
	}


	protected void handleLlmResponse(AgentContext inAgentContext, LlmResponse response)
	{
		//TODO: Use IF statements to sort what parsing we need to do. parseSearchParams parseWorkflowParams etc
		JSONObject content = response.getMessageStructured();
		
		String toolname = (String) content.get("next_step");  //request_type
		
		if(toolname == null)
		{
			throw new OpenEditException("No type specified in results: " + content.toJSONString());
		}

		JSONObject details = (JSONObject) content.get("step_details");
		
		if(details == null)
		{
			throw new OpenEditException("No details specified in results: " + content.toJSONString());
		}
		if( toolname.equals("conversation"))
		{
			JSONObject conversation = (JSONObject) details.get("conversation");
			String generalresponse = (String) conversation.get("friendly_response");
			response.setMessage( generalresponse);
		}
		else if( toolname.equals("parseSearchParts") )  //TODO Use other functions from aifunction table. Dont use tool ids
		{
			JSONObject structure = (JSONObject) details.get(toolname);
			if(structure == null)
			{
				throw new OpenEditException("No structure found for type: " + toolname);
			}
			toolname = parseSearchParts(inAgentContext, structure, toolname, response.getMessage());
		}
		response.setFunctionName(toolname);
	}

	public void semanticSearch(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive();
		
		String semanticquery = inReq.getRequestParameter("semanticquery"); 
		
		Collection<String> excludeEntityIds = inReq.getRequestCollection("excludeentityids");
		Collection<String> excludeAssetIds = inReq.getRequestCollection("excludeassetids");
		
		log.info("Semantic Search for: " + semanticquery);
		inReq.putPageValue("input", semanticquery);
		
		Map<String, Collection<String>> relatedEntityIds = getSemanticTopicManager().search(semanticquery, excludeEntityIds, excludeAssetIds);
		
		log.info("Related Entity Ids: " + relatedEntityIds);

		Collection<Data> semanticentities = new ArrayList();
		Map<String, HitTracker> semanticentityhits = new HashMap();
		HitTracker semanticassethits = null;

		for (Iterator iterator = relatedEntityIds.keySet().iterator(); iterator.hasNext();)
		{
			String moduleid = (String) iterator.next();
			
			Data module = archive.getCachedData("module", moduleid);
			
			Collection<String> ids = relatedEntityIds.get(moduleid);
			
			HitTracker entites = getMediaArchive().query(moduleid).ids(ids).search();
			
			
			if(entites == null || entites.size() == 0)
			{
				continue;
			}
			
			semanticentities.add(module);
			
			if(moduleid.equals("asset"))
			{
				semanticassethits = entites;
			}
			else
			{				
				semanticentityhits.put(moduleid, entites);
			}
		}
		
		inReq.putPageValue("semanticentities", semanticentities);
		inReq.putPageValue("semanticentityhits", semanticentityhits);
		if(semanticassethits == null)
		{
			inReq.putPageValue("semanticassethits", new ArrayList());
		}
		else
		{	
			inReq.putPageValue("semanticassethits", semanticassethits);
		}		
	}

	public Data semanticSearchBestMatch(String inQuery, String inModuleId)
	{
		Data relatedEntityId = getSemanticTopicManager().searchOne(inQuery, inModuleId);
		return relatedEntityId;
	}
	
	protected SemanticClassifier fieldSemanticTopicManager;
	public SemanticClassifier getSemanticTopicManager()
	{
		if (fieldSemanticTopicManager == null)
		{
			fieldSemanticTopicManager = (SemanticClassifier)getModuleManager().getBean(getCatalogId(), "semanticClassifier",false);
			fieldSemanticTopicManager.setConfigurationId("semantictopics");
		}

		return fieldSemanticTopicManager;
	}

	public Map<String,Collection<String>> searchRelatedEntitiesBySearchCategory(MultiValued searchcategory)
	{
		if( searchcategory.getBoolean("semanticindexed"))
		{
			//Todo: Use the Vector DB?
		}
		Collection values = searchcategory.getValues("semantictopics");
		Map<String,Collection<String>> results = getSemanticTopicManager().search(values, null, null);
		return results;
	}

	public void rescanSearchCategories()
	{
		//For each search category go look for relevent records. Reset old ones?
		HitTracker tracker = getMediaArchive().query("searchcategory").exists("semantictopics").search();
		for (Iterator iterator = tracker.iterator(); iterator.hasNext();)
		{
			MultiValued searchcategory = (MultiValued) iterator.next();
			
			Map<String,Collection<String>> bytype = searchRelatedEntitiesBySearchCategory(searchcategory);
			for (Iterator iterator2 = bytype.keySet().iterator(); iterator2.hasNext();)
			{
				String moduleid = (String)iterator2.next();
				Collection<String> ids = bytype.get(moduleid);
				Collection addedentites = getMediaArchive().query(moduleid).ids(ids).not("searchcategory",searchcategory.getId()).search();
				//Collection addedentites = getMediaArchive().query(moduleid).ids(ids).search();
				Collection tosave = new ArrayList(addedentites.size());
				for (Iterator iterator3 = addedentites.iterator(); iterator3.hasNext();)
				{
					MultiValued entity = (MultiValued) iterator3.next();
					entity.addValue("searchcategory",searchcategory.getId());
					tosave.add(entity);
				}
				log.info("Added " + tosave.size() + " to category " + moduleid);
				getMediaArchive().saveData(moduleid,tosave);
			}
		}
	}

	public Collection<String> makeSearchSuggestions(UserProfile inUserProfile) {
		int count = 0;
		Collection<String> suggestions = new ArrayList<String>();
		for (Iterator iterator = inUserProfile.getEntities().iterator(); iterator.hasNext();) 
		{
			MultiValued module = (MultiValued) iterator.next();
			if(module.getBoolean("showonsearch") && !module.getId().equals("asset") && count < 5) 
			{
				Data entity = getMediaArchive().query(module.getId()).searchOne();
				if( entity != null ) 
				{
					String entityname = entity.getName();
					String prefix = "";
					if( count % 3 == 0 ) {
						prefix = "Search for ";
					} else {
						prefix = "Find ";
					}
					prefix = prefix + entityname + " in " + module.getName();
					suggestions.add( prefix );
					count++;
				}
			}
		}
		return suggestions;
	}
	
}
