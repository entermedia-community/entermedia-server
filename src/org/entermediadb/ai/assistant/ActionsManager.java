package org.entermediadb.ai.assistant;

import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.ChatMessageHandler;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmResponse;
import org.json.simple.JSONObject;
import org.openedit.OpenEditException;

public class ActionsManager extends BaseAiManager implements ChatMessageHandler
{

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

}
