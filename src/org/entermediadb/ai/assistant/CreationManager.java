package org.entermediadb.ai.assistant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.ChatMessageHandler;
import org.entermediadb.ai.informatics.SemanticTableManager;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;

public class CreationManager extends BaseAiManager implements ChatMessageHandler
{
	private static final Log log = LogFactory.getLog(CreationManager.class);

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
	
	protected void handleLlmResponse(AgentContext inAgentContext, LlmResponse response)
	{
		JSONObject content = response.getMessageStructured();
		
		String next_function = (String) content.get("next_function");  //request_type
		
		if(next_function == null)
		{
			throw new OpenEditException("No type specified in results: " + content.toJSONString());
		}

		JSONObject details = (JSONObject) content.get("request_details");
		
		if(details == null)
		{
			throw new OpenEditException("No details specified in results: " + content.toJSONString());
		}
		if( next_function.equals("conversation"))
		{
//				JSONObject structure = (JSONObject) results.get(type);
			JSONObject conversation = (JSONObject) details.get("conversation");
			String generalresponse = (String) conversation.get("friendly_response");
//				if(generalresponse != null)
//				{
			//String generalresponse = (String) content.get("response");
//				}
			response.setMessage( generalresponse);
			return;
//			response.setFunctionName("conversation");
		}
//		else if(next_function.equals("runAction"))  //One at a time until the cancel or finish
//		{
//			//Simplify what they are asking for
//			//action:create
//			//target: image
//			//Vector search "create image" -> function  name=createImage  //We can confirm with user
//			
//			AiCreation creation = inAgentContext.getAiCreationParams();					
//			creation.setCreationType("image");
//			JSONObject structure = (JSONObject) details.get("run_workflow");
//			creation.setImageFields(structure);
//			toolname = "runWorkflow";
//		}
		response.setFunctionName(next_function);
	}
	
	@Override
	public LlmResponse processMessage(AgentContext inAgentContext, MultiValued inMessage, MultiValued inAiFunction)
	{
		return null;
	}

}
