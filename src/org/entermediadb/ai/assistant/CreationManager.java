package org.entermediadb.ai.assistant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.ChatMessageHandler;
import org.entermediadb.ai.Schema;
import org.entermediadb.ai.informatics.SemanticTableManager;
import org.entermediadb.ai.knn.RankedResult;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.markdown.MarkdownUtil;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;

public class CreationManager extends BaseAiManager implements ChatMessageHandler
{
	private static final Log log = LogFactory.getLog(CreationManager.class);

	@Override
	public void savePossibleFunctionSuggestions(ScriptLogger inLog)
	{
		savePossibleFunctionSuggestions(inLog, "Creation"); 
	}
	
	public Collection<SemanticAction> createPossibleFunctionParameters(ScriptLogger inLog)
	{
		//List all functions
		Collection creations = getMediaArchive().query("aifunction").exact("messagehandler", "contentManager").search();
		
		Searcher embedsearcher = getMediaArchive().getSearcher("aifunctionparameter");
		
		Collection<SemanticAction> actions = new ArrayList();

		for (Iterator iterator = creations.iterator(); iterator.hasNext();)
		{
			Data function = (Data) iterator.next();
			
			Data module = null;

			Collection phrases  = function.getValues("phrases");
			if(phrases != null)
			{
				for (Iterator iterator2 = phrases.iterator(); iterator2.hasNext();)
				{
					String phrase = (String) iterator2.next();
					Collection existing = embedsearcher.query().exact("aifunction", function.getId()).exact("name", phrase).search();
					if( !existing.isEmpty())
					{
						log.info("Skipping existing: " + phrase);
						continue;
					}
					SemanticAction action = new SemanticAction();
					action.setAiFunction(function.getId());
					action.setSemanticText(phrase);
					action.setParentData(module);
					actions.add(action);
				}
			}

			if( function.getId().equals("createImage" ) )
			{
				module = getMediaArchive().getCachedData("module", "asset");
			}
			
			if( function.getId().equals("createRecord" ) )
			{
				Schema schema = loadSchema();
				
				for (Iterator iterator2 = schema.getModules().iterator(); iterator2.hasNext();)
				{
					Data parentmodule = (Data) iterator2.next();

					Collection existing = embedsearcher.query().exact("aifunction", function.getId()).exact("parentmodule",parentmodule.getId()).search();
					if( !existing.isEmpty())
					{
						log.info("Skipping existing: " + parentmodule);
						continue;
					}
					SemanticAction action = new SemanticAction();
					action.setAiFunction(function.getId());
					action.setParentData(parentmodule);
					action.setSemanticText("Create a record in " + parentmodule.getName());
					actions.add(action);
					
					Collection<Data> children = schema.getChildrenOf(parentmodule.getId());
					
					for (Iterator iterator3 = children.iterator(); iterator3.hasNext();)
					{
						Data childmodule = (Data) iterator3.next();
						action = new SemanticAction();
						action.setParentData(parentmodule);
						action.setChildData(childmodule);
						action.setAiFunction(function.getId());
						action.setSemanticText("Create a " + childmodule.getName() + " in " + parentmodule.getName());
						actions.add(action);
					}
					
				}
			}
		}
		
		SemanticTableManager manager = loadSemanticTableManager("aifunctionparameter");

		populateVectors(manager, actions);

		return actions;

//		List<Double> tosearch = manager.makeVector("Find all records in US States in 2023");
//		Collection<RankedResult> results = manager.searchNearestItems(tosearch);
//		log.info(results);
		
		
	}
	
	protected String parseCreationParts(AgentContext inAgentContext, JSONObject structure, String type, String messageText) 
	{ 
		String creationtask = (String) structure.get("creation_task");
		if( creationtask == null)
		{
			throw new OpenEditException("No creation task specified in results: " + structure.toJSONString());
		}
		
		AiCreation creation = inAgentContext.getAiCreationParams();
		
		SemanticTableManager manager = loadSemanticTableManager("aifunctionparameter"); 
		List<Double> tosearch = manager.makeVector( sanitizeCreationTask(creationtask) );
		Collection<RankedResult> suggestions = manager.searchNearestItems(tosearch);
		
		if( !suggestions.isEmpty())
		{
			inAgentContext.setRankedSuggestions(suggestions);
			RankedResult top = (RankedResult) suggestions.iterator().next();
			if ( top.getDistance() < .7 )
			{
				String creationFunction = top.getEmbedding().get("aifunction");
				creation.setCreationFunction(creationFunction);
				String parentmodule = top.getEmbedding().get("parentmodule");
				creation.setCreationModule(parentmodule);
				
				type = "loadCreationFields";
				
			}
			else
			{
				type = "conversation";
			}
		}
		else
		{
			type = "conversation";
		}
		return type;
	}
	
	private String sanitizeCreationTask(String inTask)
	{
		inTask = inTask.replaceAll("[\\n\\r]+", " ");
		if(!inTask.contains("create"))
		{
			inTask = "create " + inTask;
		}
		return inTask;
	}
	
	protected void handleLlmResponse(AgentContext inAgentContext, LlmResponse response)
	{
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
		else if(toolname.equals("parseCreationParts"))  //One at a time until the cancel or finish
		{
			JSONObject structure = (JSONObject) details.get(toolname);		
			if(structure == null)
			{
				throw new OpenEditException("No structure found for type: " + toolname);
			}
			toolname = parseCreationParts(inAgentContext, structure, toolname, response.getMessage());
		}
		response.setFunctionName(toolname);
	}
	
	@Override
	public LlmResponse processMessage(AgentContext inAgentContext, MultiValued inAgentMessage, MultiValued inAiFunction)
	{
		String agentFn = inAgentContext.getFunctionName();
		if ("startCreation".equals(agentFn))
		{
			MultiValued usermessage = (MultiValued)getMediaArchive().getCachedData("chatterbox", inAgentMessage.get("replytoid"));
			MultiValued function = (MultiValued)getMediaArchive().getCachedData("aifunction", agentFn);

			LlmResponse response = startChat(inAgentContext, usermessage, inAgentMessage, function);
			
			String responseFn = response.getFunctionName();
			if ("conversation".equals(responseFn))
			{
				inAgentMessage.setValue("chatmessagestatus", "completed");
				
				String generalresponse  = response.getMessage();
				if(generalresponse != null)
				{
					MarkdownUtil md = new MarkdownUtil();
					generalresponse = md.render(generalresponse);
				}
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
		else if ("loadCreationFields".equals(agentFn))
		{
			String creationfunction = inAgentContext.getAiCreationParams().getCreationFunction();
			return loadCreationParameters(inAgentContext, creationfunction);
		}
		
		throw new OpenEditException("Function not supported " + agentFn);
		
	}
	
	 
	protected LlmResponse loadCreationParameters(AgentContext inAgentContext, String creationFn)
	{
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("loadCreationFields");
		inAgentContext.addContext("creationfunction", creationFn);
		LlmResponse response = llmconnection.callStructuredOutputList(inAgentContext.getContext());
		if(response == null)
		{
			throw new OpenEditException("No results from AI for function: " + inAgentContext.getFunctionName());
		}
		AiCreation creation = inAgentContext.getAiCreationParams();
		JSONObject content = response.getMessageStructured();
		creation.setCreationFields( content );
		response.setMessage("");
		inAgentContext.setNextFunctionName(creationFn);
		response.setFunctionName(creationFn);
		return response;
	}

	@Override
	public void getDetectorParams(AgentContext inAgentContext, MultiValued inTopLevelFunction) {
		// TODO Auto-generated method stub
		
	}
}
