package org.entermediadb.ai.assistant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.ChatMessageHandler;
import org.entermediadb.ai.Schema;
import org.entermediadb.ai.classify.EmbeddingManager;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.markdown.MarkdownUtil;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.hittracker.FilterNode;
import org.openedit.hittracker.HitTracker;
import org.openedit.profile.UserProfile;

public class QuestionsManager extends BaseAiManager implements ChatMessageHandler
{
	private static final Log log = LogFactory.getLog(QuestionsManager.class);

	@Override
	public void savePossibleFunctionSuggestions(ScriptLogger inLog)
	{
		UserProfile profile = getMediaArchive().getUserProfileManager().getUserProfile(getMediaArchive().getCatalogId(), "admin"); 
		Collection<Data> moduleids = findEnabledModules(profile);

		Collection<String> embeddedcontents = new ArrayList<String>();
		
		for (Data module : moduleids)
		{
			String searchtype = module.getId();
			HitTracker moduleentities = getMediaArchive().query(module.getId()).exact("entityembeddingstatus", "embedded").search();
			if(!moduleentities.isEmpty())
			{
				for (Iterator iterator = moduleentities.iterator(); iterator.hasNext();)
				{
					MultiValued entity = (MultiValued) iterator.next();
					String content = null;
					
					if(searchtype.equals("userpost"))
					{			
						content = entity.get("maincontent");
					}
					else if(searchtype.equals("entitydocument") || searchtype.equals("entityasset"))
					{			
						content = entity.get("markdowncontent");
					}
					else
					{	
						Collection<PropertyDetail> contextFields = new ArrayList<PropertyDetail>();
						
						Collection detailsfields = getMediaArchive().getSearcher(searchtype).getDetailsForView(searchtype+"general");
						for (Iterator iterator3 = detailsfields.iterator(); iterator3.hasNext();)
						{
							PropertyDetail field = (PropertyDetail) iterator3.next();
							if(entity.hasValue(field.getId()))
							{
								contextFields.add(field);
							}
						}
						
						Map<String, Object> inParams = new HashMap();
						inParams.put("data", entity);
						inParams.put("contextfields", contextFields);
						
						String templatepath = getMediaArchive().getMediaDbId() + "/ai/default/calls/commons/context_fields.json";
						LlmConnection llmconnection = getMediaArchive().getLlmConnection("documentEmbedding");
						content = llmconnection.loadInputFromTemplate(templatepath, inParams);

					}
					 
					if( content == null || content.isEmpty())
					{
						continue;
					}
					String cleanText = content.replaceAll("<[^>]*>", "").trim();
					if(cleanText.length() > 100)
					{
						embeddedcontents.add(cleanText);
					}
				}
			}
		}
		
		Collection pickedcontents = getRandomFromCollection(embeddedcontents, 5);
		
		Map params = new HashMap();
		params.put("contents", pickedcontents);
		
		savePossibleFunctionSuggestions(inLog, "Questions", params);
	}
	
	private Collection getRandomFromCollection(Collection inCollection, int inMax)
	{
		if(inCollection.size() <= inMax)
		{
			return inCollection;
		}
		
		Collection picked = new ArrayList();
		int count = 0;
		for (Iterator iterator = inCollection.iterator(); iterator.hasNext();)
		{
			Object obj = (Object) iterator.next();
			picked.add(obj);
			count++;
			if(count >= inMax)
			{
				break;
			}
		}
		return picked;
	}

	@Override
	public LlmResponse processMessage(AgentContext inAgentContext, MultiValued inAgentMessage, MultiValued inAiFunction)
	{
		MultiValued usermessage = (MultiValued)getMediaArchive().getCachedData("chatterbox", inAgentMessage.get("replytoid"));
		String query = usermessage.get("message");
		
		String agentFn = inAgentContext.getFunctionName();
		if ("question_welcome".equals(agentFn))
		{
			inAgentMessage.setValue("chatmessagestatus", "completed");
			
			LlmConnection llmconnection = getMediaArchive().getLlmConnection(inAiFunction.getId()); //Should stay search_start
			LlmResponse response = llmconnection.renderLocalAction(inAgentContext);
			inAgentContext.setFunctionName("askQuestion");
			return response;
		}
		if ("question_ask".equals(agentFn))
		{
			
			//Make sure they have already picked the documents
			String entiyid = inAgentContext.getChannel().get("dataid");
			String moduleid = inAgentContext.getChannel().get("searchtype");
			
			if(entiyid != null && moduleid != null && !"admin".equals(entiyid))
			{
				Collection<String> docids = findDocIdsForEntity(moduleid,entiyid);
				EmbeddingManager embeddings = (EmbeddingManager) getMediaArchive().getBean("embeddingManager");
				LlmResponse response = embeddings.findAnswer(inAgentContext, docids, query);
				return response;
			}
			
			//Else DO a search first
			//If we dont know what we are asking then run a search for related records and do a RAG on them. 
			//Was president Obama effective?   //Pullout the keywords and search across all embedded data
			//What product sold the most invoices? //Limit to products and invoices
			MultiValued function = (MultiValued) getMediaArchive().getCachedData("aifunction", agentFn);
			
			LlmConnection llmconnection = getMediaArchive().getLlmConnection(function.getId()); //Should stay search_start
			LlmResponse response = llmconnection.callStructuredOutputList(inAgentContext.getContext()); //TODO: Replace with local API that is faster
			if(response == null)
			{
				throw new OpenEditException("No results from AI for message: " + usermessage.get("message"));
			}
			
			handleLlmResponse(inAgentContext, response);

			
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
		else if("question_search".equals(agentFn))
		{
			//1 Do the search from keyword,
			//2 grab the ids
			//Do an embedding search
			String keyword = inAgentContext.get("search_keyword");
			
			Schema schema = loadSchema();
			
			Collection<String> moduleids = schema.getModuleIds();
			
			HitTracker hits = getMediaArchive().query("modulesearch")
					.put("searchtypes", moduleids)
					.freeform("description", keyword)
					.exact("entityembeddingstatus", "embedded")
					.search();
			
			Collection<String> docids = new JSONArray();
			for (Iterator iterator = hits.iterator(); iterator.hasNext();)
			{
				MultiValued doc = (MultiValued) iterator.next();
				String searchtype = doc.get("entitysourcetype");
				String docid = searchtype + "_" + doc.getId();
				docids.add(docid);
			}
			EmbeddingManager embeddings = (EmbeddingManager)getMediaArchive().getBean("embeddingManager");
			LlmResponse response = embeddings.findAnswer(inAgentContext,docids,query);
			return response;
		}
		
		
		throw new OpenEditException("Function not supported " + agentFn);
		
	}
	
	public Collection<Data> findEnabledModules(UserProfile inProfile)
	{
		Schema schema = loadSchema();
		
		Collection<String> moduleids = schema.getModuleIds();
		HitTracker tracker = getMediaArchive().query("modulesearch")
			.put("searchtypes", moduleids)
			.facet("entitysourcetype")
			.all()
			.exact("entityembeddingstatus", "embedded")
			.search();
		
		Collection<Data> modules = new ArrayList();
		
		FilterNode nodes = tracker.findFilterValue("entitysourcetype");
		if( nodes != null)
		{
			Collection<String> enabled = inProfile.getModuleIds();
	
			for (Iterator iterator = nodes.getChildren().iterator(); iterator.hasNext();)
			{
				FilterNode node = (FilterNode) iterator.next();
				if( enabled.contains(node.getId()))
				{
					Data module = getMediaArchive().getCachedData("module", node.getId());
					modules.add(module);
				}
			}
		}
		return modules;
		
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
//			response.setFunctionName("conversation");
		}
		else if( toolname.equals("question_search") )
		{
			JSONObject structure = (JSONObject) details.get(toolname);
			if(structure == null)
			{
				throw new OpenEditException("No structure found for type: " + toolname);
			}
			String search_keyword = (String) structure.get("search_keyword");
			inAgentContext.setValue("search_keyword", search_keyword);
			//Search system wise for keyword hits that are embedded
		}

		response.setFunctionName(toolname);
	}
	/**
	The UI needs to ask the user to limit his search to one data type. ie. documents or document pages or marketing assets or product xyz?
	So the startFunction will just work
	Othertimes we need to build a data search and find what we want to search across. Like "Search for political documents and tell me Who is the president of Mexico. 
	*/
	public Collection<SemanticAction> createPossibleFunctionParameters(ScriptLogger inLog)
	{
		//Filter down to specific modules they might be asking about?
		
		//Or focus on the Entity we are on.
		
		
		return new ArrayList();
	}

	public Collection<String> findDocIdsForEntity(String parentmoduleid, String inEntityId)
	{
		MediaArchive archive = getMediaArchive();
		AssistantManager assistant = (AssistantManager) archive.getBean("assistantManager");
		
//		String parentmoduleid = inAgentContext.getChannel().get("searchtype"); 
		Data module = archive.getCachedData("module", parentmoduleid);

//		String entityid = inAgentContext.getChannel().get("dataid");
		Data entity = archive.getCachedData(parentmoduleid, inEntityId);
		
		Collection<GuideStatus> statuses = assistant.prepareDataForGuide(module, entity);
		JSONArray docids = new JSONArray();
//		Data inDocument = getMediaArchive().getCachedData(entityid, moduleid);
		
//		MultiValued parent = (MultiValued)archive.getCachedData("chatterbox",message.get("replytoid"));
//		String query = parent.get("message");
//		chatjson.put("query",query);
		for(GuideStatus stat : statuses)
		{
			if(stat.isReady())
			{
				String searchtype = stat.getSearchType();
				Searcher searcher = archive.getSearcher(searchtype);
				HitTracker hits = searcher.query().exact(parentmoduleid, inEntityId).search();
				for (Iterator iterator = hits.iterator(); iterator.hasNext();)
				{
					MultiValued doc = (MultiValued) iterator.next();
					String docid = searchtype + "_" + doc.getId();
					docids.add(docid);
				}
			}
		}
		return docids;
	}
	
	public String getAnswerByEntity(String inModuleId, String inEntityid, String inQuestion)
	{
		Collection<String> docIds = findDocIdsForEntity(inModuleId, inEntityid);
		EmbeddingManager embeddings = (EmbeddingManager) getMediaArchive().getBean("embeddingManager");
		String answer = embeddings.findAnswer(docIds, inQuestion);
		return answer;
	}

	@Override
	public void getDetectorParams(AgentContext inAgentContext, MultiValued inTopLevelFunction) {
		// TODO Auto-generated method stub
		
	}

}
