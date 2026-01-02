package org.entermediadb.ai.assistant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
	
	public Collection<SemanticAction> createPossibleFunctionParameters(ScriptLogger inLog)
	{
		//List all functions
		Collection creations = getMediaArchive().query("aifunction").exact("functiongroup", "creation").search();
		
		Collection<SemanticAction> actions = new ArrayList();

		for (Iterator iterator = creations.iterator(); iterator.hasNext();)
		{
			Data function = (Data) iterator.next();
			
			Data module = null;
			
			if( function.getId().equals("createImage" ) )
			{
				module = getMediaArchive().getCachedData("module","asset");
			}
			
			Collection phrases  = function.getValues("phrases");
			for (Iterator iterator2 = phrases.iterator(); iterator2.hasNext();)
			{
				String phrase = (String) iterator2.next();
				SemanticAction action = new SemanticAction();
				action.setAiFunction(function.getId());
				action.setSemanticText(function.getName());
				action.setParentData(module);
				actions.add(action);
			}
		}
		SemanticTableManager manager = loadSemanticTableManager("aifunctionparameter");

		populateVectors(manager,actions);

		return actions;

//		List<Double> tosearch = manager.makeVector("Find all records in US States in 2023");
//		Collection<RankedResult> results = manager.searchNearestItems(tosearch);
//		log.info(results);
		
		
	}


}
