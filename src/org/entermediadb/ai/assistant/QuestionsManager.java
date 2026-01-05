package org.entermediadb.ai.assistant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.ChatMessageHandler;
import org.entermediadb.ai.classify.EmbeddingManager;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.markdown.MarkdownUtil;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;

public class QuestionsManager extends BaseAiManager implements ChatMessageHandler
{
	private static final Log log = LogFactory.getLog(QuestionsManager.class);

	@Override
	public LlmResponse processMessage(AgentContext inAgentContext, MultiValued inAgentMessage, MultiValued inAiFunction)
	{
//		if(output == null || output.isEmpty())
//		{  //What is this for?
//			agentContext.addContext("messagetosend", message.get("message") );
//			LlmResponse chatresponse = llmconnection.callMessageTemplate(agentContext,response.getFunctionName()); 
//			//TODO: Add message history
//			output = chatresponse.getMessage();
//		}
		//ChatServer server = (ChatServer) getMediaArchive().getBean("chatServer");
		
		
//		String output = inMessage.getMessage();
//		agentmessage.setValue("message", output);  //Needed"
//		agentmessage.setValue("messageplain", output);
//		
		MultiValued usermessage = (MultiValued)getMediaArchive().getCachedData("chatterbox", inAgentMessage.get("replytoid"));
		String query = usermessage.get("mesage");
		
		String agentFn = inAgentContext.getFunctionName();
		if ("startQuestions".equals(agentFn))
		{
			
			//Make sure they have already picked the documents
			String entiyid = inAgentContext.getChannel().get("dataid");
			String moduleid = inAgentContext.getChannel().get("searchtype");
			
			if(entiyid != null && moduleid != null)
			{
				Collection<String> docids = findDocIdsForEntity(moduleid,entiyid);
				EmbeddingManager embeddings = (EmbeddingManager)getMediaArchive().getBean("embeddingManager");
				LlmResponse response = embeddings.findAnswer(inAgentContext,docids,query);
				return response;
			}
			
			//Else DO a search first
			//If we dont know what we are asking then run a search for related records and do a RAG on them. 
			//Was president Obama effective?   //Pullout the keywords and search across all embedded data
			//What product sold the most invoices? //Limit to products and invoices
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
		else if("searchByKeywordForQuestioning".equals(agentFn))
		{
			//1 Do the search from keyword,
			//2 grab the ids
			//Do an embedding search
			String keyword = inAgentContext.get("question_scope_keyword");
			
			HitTracker hits = getMediaArchive().query("modulesearch").exact("description", keyword).exact("entityembeddingstatus", "embedded").search();
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
	
	protected void handleLlmResponse(AgentContext inAgentContext, LlmResponse response)
	{
		//TODO: Use IF statements to sort what parsing we need to do. parseSearchParams parseWorkflowParams etc
		JSONObject content = response.getMessageStructured();
		
		String toolname = (String) content.get("next_step");  //request_type
		
		if(toolname == null)
		{
			throw new OpenEditException("No type specified in results: " + content.toJSONString());
		}

		JSONObject details = (JSONObject) content.get("request_details");
		
		if(details == null)
		{
			throw new OpenEditException("No details specified in results: " + content.toJSONString());
		}
		if( toolname.equals("conversation"))
		{
//				JSONObject structure = (JSONObject) results.get(type);
			JSONObject conversation = (JSONObject) details.get("conversation");
			String generalresponse = (String) conversation.get("friendly_response");
//				if(generalresponse != null)
//				{
			//String generalresponse = (String) content.get("response");
//				}
			response.setMessage( generalresponse);
//			response.setFunctionName("conversation");
		}
		else if( toolname.equals("searchByKeywordsForQuestioning") )
		{
			JSONObject structure = (JSONObject) details.get(toolname);
			if(structure == null)
			{
				throw new OpenEditException("No structure found for type: " + toolname);
			}
			String question_scope_keyword = (String)structure.get("question_scope_keyword");
			inAgentContext.setValue("question_scope_keyword",question_scope_keyword);
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
				HitTracker hits = searcher.query().exact("entityembeddingstatus", "embedded").search();
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

}
