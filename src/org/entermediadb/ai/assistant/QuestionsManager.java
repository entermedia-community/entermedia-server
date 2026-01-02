package org.entermediadb.ai.assistant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.ChatMessageHandler;
import org.entermediadb.ai.Schema;
import org.entermediadb.ai.informatics.SemanticTableManager;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.markdown.MarkdownUtil;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;

public class QuestionsManager extends BaseAiManager implements ChatMessageHandler

{
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
		String agentFn = inAgentContext.getFunctionName();
		if ("startQuestions".equals(agentFn))
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

		throw new OpenEditException("Function not supported " + agentFn);
		
	}
	
	
	protected void handleLlmResponse(AgentContext inAgentContext, LlmResponse response)
	{
		//TODO: Use IF statements to sort what parsing we need to do. parseSearchParams parseWorkflowParams etc
		JSONObject content = response.getMessageStructured();
		
		String toolname = (String) content.get("next_function");  //request_type
		
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
		else if( toolname.equals("question") )
		{
			JSONObject structure = (JSONObject) details.get(toolname);
			if(structure == null)
			{
				throw new OpenEditException("No structure found for type: " + toolname);
			}
			toolname = "processQuestion"; //TODO: use question type to determine more precise function
		}
		response.setFunctionName(toolname);
	}
	
	
	public void indexPossibleFunctionParameters(ScriptLogger inLog)
	{
		SemanticTableManager manager = loadSemanticTableManager("aifunctionparameter");
		

		//Find modules that has embeding enabled
		
		//Add their name here so people can type them in?
		
		//Use this to filter the list down?
		Searcher embedsearcher = getMediaArchive().getSearcher("aifunctionparameter");
		
		for (Iterator iterator = getModules().iterator(); iterator.hasNext();)
		{
			Data parentmodule = (Data) iterator.next();
			
			Collection existing = embedsearcher.query().exact("parentmodule",parentmodule.getId()).search();
			if( !existing.isEmpty())
			{
				log.info("Skipping " + parentmodule);
				continue;
			}
			Collection<SemanticAction> actions = new ArrayList();
			
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
			action.setSemanticText("Ask about " + parentmodule.getName());
			action.setParentData(parentmodule);
			actions.add(action);
//			action = new SemanticAction();
//			action.setParentData(parentmodule);
//			action.setAiFunction("createEntity");
//			action.setSemanticText("Create a new " + parentmodule.getName());
//			actions.add(action);
			
			//Check for child views
			Collection<Data> children = shema.getChildrenOf(parentmodule.getId());
			
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

			//Save to db
			Collection tosave = new ArrayList();
			
			for (Iterator iterator2 = actions.iterator(); iterator2.hasNext();)
			{
				SemanticAction semanticAction = (SemanticAction) iterator2.next();
				Data data = embedsearcher.createNewData();
				data.setValue("parentmodule",semanticAction.getParentData().getId());
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
			
		}
		//Test search
		manager.reinitClusters(inLog);

//		List<Double> tosearch = manager.makeVector("Find all records in US States in 2023");
//		Collection<RankedResult> results = manager.searchNearestItems(tosearch);
//		log.info(results);
		
		
	}


}
