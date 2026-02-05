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
import org.entermediadb.ai.informatics.InformaticsProcessor;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.markdown.MarkdownUtil;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.BaseData;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.hittracker.FilterNode;
import org.openedit.hittracker.HitTracker;
import org.openedit.profile.UserProfile;

public class QuestionsManager extends BaseAiManager implements ChatMessageHandler
{
	private static final Log log = LogFactory.getLog(QuestionsManager.class);

	@Override
	public LlmResponse processMessage(AgentContext inAgentContext, MultiValued inAgentMessage, MultiValued inAiFunction)
	{
		MultiValued usermessage = (MultiValued)getMediaArchive().getCachedData("chatterbox", inAgentMessage.get("replytoid"));
		String query = usermessage.get("message");
		
		String agentFn = inAgentContext.getFunctionName();
		if ("question_welcome".equals(agentFn))
		{
			inAgentMessage.setValue("chatmessagestatus", "completed");
			
			String entityid = (String) inAgentContext.get("entityid");
			String entitymoduleid = (String) inAgentContext.get("entitymoduleid");
			
			Data entity = getMediaArchive().getCachedData(entitymoduleid, entityid);
			inAgentContext.addContext("entity", entity);

			Data entitymodule = getMediaArchive().getCachedData("module", entitymoduleid);
			inAgentContext.addContext("entitymodule", entitymodule);

			Collection aisuggestions = getMediaArchive().query("aisuggestion").exact("entityid", entity).search();
			inAgentContext.addContext("aisuggetions", aisuggestions);
			
			LlmConnection llmconnection = getMediaArchive().getLlmConnection(inAiFunction.getId()); //Should stay search_start
			LlmResponse response = llmconnection.renderLocalAction(inAgentContext);
			if( aisuggestions.isEmpty())
			{
				inAgentContext.setNextFunctionName("question_create_suggestions");
			}
			else
			{
				inAgentContext.setFunctionName("question_ask");
				inAgentContext.setWaitTime(null);
			}
			return response;
		}
		else if ("question_create_suggestions".equals(agentFn))
		{
			Data entity= (Data) inAgentContext.getContextValue("entity");
			Data entitymodule = (Data) inAgentContext.getContextValue("entitymodule");

			String text = findSampleOfEmbeddedData(entitymodule,entity);
			
			if (text == null || text.isEmpty())
			{
				text = entity.getName();
				if (entity.get("longcaption") != null)
				{
					text = text + " " +entity.get("longcaption");
				}
			}
			
			inAgentContext.addContext("embeddedtext", text);
			LlmConnection llmconnection = getMediaArchive().getLlmConnection(agentFn);
			LlmResponse response = llmconnection.callStructuredOutputList(inAgentContext.getContext(),agentFn);
			
			Searcher searcher = getMediaArchive().getSearcher("aisuggestion");

			JSONObject json = response.getMessageStructured();
			Collection suggestions = (Collection)json.get("suggestions");
			for (Iterator iterator = suggestions.iterator(); iterator.hasNext();)
			{
				Map	airesponse = (Map)iterator.next();
				Data suggestiondata = searcher.createNewData();
				suggestiondata.setValue("aifunction", "question_welcome");
				suggestiondata.setValue("entityid", entity.getId());
				suggestiondata.setValue("entitymoduleid", entitymodule.getId());
				suggestiondata.setName( (String)airesponse.get("title"));
				suggestiondata.setValue("prompt", airesponse.get("prompt"));
				searcher.saveData(suggestiondata);
			}
			inAgentContext.setNextFunctionName("question_welcome");
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
			LlmResponse response = llmconnection.callStructuredOutputList(inAgentContext.getContext(),function.getId()); //TODO: Replace with local API that is faster
			
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

	public String findSampleOfEmbeddedData(Data inEntityModule, Data inEntity)
	{
		//Should we look for children...
		StringBuffer foundtext = new StringBuffer();
		
		String mystatus = inEntity.get("entityembeddingstatus"); 
		if(mystatus == null)
		{
			mystatus = "notembedded";
		}
		if(mystatus != null && "embedded".equals(mystatus))
		{
			String markdown = inEntity.get("markdowncontent");
			if( markdown == null)
			{
				markdown = inEntity.get("maincontent");
				if( markdown == null)
				{
					markdown = inEntity.get("longcaption");
				}
			}
			if( markdown != null)
			{
				foundtext.append( markdown);
			}
		}
		Collection detailsviews = getMediaArchive().query("view").exact("moduleid", inEntityModule.getId()).exact("systemdefined",false).cachedSearch(); 
		for (Iterator iterator = detailsviews.iterator(); iterator.hasNext();)
		{
			Data view = (Data) iterator.next();
			
			String listid = view.get("rendertable");
			if( listid != null)
			{
				GuideStatus status = new GuideStatus();
				status.setSearchType(listid);
				status.setViewData(view);
				
				HitTracker found = null;
				try
				{
					found = getMediaArchive().query(listid).exact(inEntityModule.getId(),inEntity.getId()).facet("entityembeddingstatus").search();
				}
				catch (Exception e)
				{
					log.debug(inEntityModule + " search error " + inEntity);
					continue;
				}
				
				for (Iterator iterator2 = found.iterator(); iterator2.hasNext();)
				{
					Data data = (Data) iterator2.next();
					String markdown = data.get("markdowncontent");
					if( markdown == null)
					{
						markdown = data.get("maincontent");
						if( markdown == null)
						{
							markdown = data.get("longcaption");
						}
					}
					if( markdown == null)
					{
						String assetid = data.get("primarymedia");
						Asset asset = getMediaArchive().getEntityManager().getAsset(data);
						if (asset != null)
						{
							markdown = asset.get("longcaption");
						}
					}
					if( markdown != null)
					{
						foundtext.append( markdown);
					}
					
					if( foundtext.length() > 2000)
					{
						return foundtext.toString();							
					}
				}
			}
		}
		return foundtext.toString();
	}
	

/*
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
	*/

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

}
