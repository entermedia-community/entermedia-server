package org.entermediadb.ai.assistant;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.ChatMessageHandler;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONObject;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;

public class AutoDetectChatManager extends BaseAiManager implements ChatMessageHandler
{
	private static final Log log = LogFactory.getLog(AutoDetectChatManager.class);

	
	@Override
	public LlmResponse processMessage(AgentContext inAgentContext, MultiValued inAgentMessage, MultiValued inAiFunction)
	{
		MultiValued usermessage = (MultiValued)getMediaArchive().getCachedData("chatterbox", inAgentMessage.get("replytoid"));
		String query = usermessage.get("message");
		
		String agentFn = inAgentContext.getFunctionName();
		if ("welcomeAutoDetectConversation".equals(agentFn))
		{
			inAgentMessage.setValue("chatmessagestatus", "completed");
			
			LlmConnection llmconnection = getMediaArchive().getLlmConnection(inAiFunction.getId()); //Should stay startSearch
			LlmResponse response = llmconnection.renderLocalAction(inAgentContext);
			inAgentContext.setFunctionName("autoDetectConversation");
			return response;
		}
		if ("autoDetectConversation".equals(agentFn))
		{
			JSONObject params = new JSONObject();
			params.put("userquery", query);
			
			LlmConnection llmconnection = getMediaArchive().getLlmConnection("default");
			
			LlmResponse response = llmconnection.callToolsFunction(params, agentFn);
			
			log.info(response.getRawResponse());
			
			String functionName = response.getFunctionName();
			JSONObject functionArgs = response.getFunctionArguments();
			
			if(functionName == null || "general_chat".equals(functionName))
			{
				inAgentContext.setFunctionName("autoDetectConversation");
				String message = (String) functionArgs.get("friendly_response");
				if(message != null)
				{
					response.setMessage(message);
					response.setMessagePlain(message);
				}
				
				return response;
			}
			
			inAgentContext.addContext("arguments", functionArgs);
			
			if("create_tutorial".equals(functionName))
			{
				inAgentContext.addContext("playbackentitymoduleid", "aitutorials");
				
				inAgentContext.addContext("processingtype", "Tutorial"); // shows up in processing message (i.e. Creating new Tutorial)
				
				inAgentContext.setTopLevelFunctionName("welcome_aitutorials");
				inAgentContext.setFunctionName("welcome_aitutorials");
				inAgentContext.setNextFunctionName("smartcreator_CreateNew");
			}
			else if("start_tutorial".equals(functionName))
			{
				inAgentContext.addContext("playbackentitymoduleid", "aitutorials");
				
				inAgentContext.setTopLevelFunctionName("welcomestart_aitutorials");
				inAgentContext.setFunctionName("welcomestart_aitutorials");
				inAgentContext.setNextFunctionName("welcomestart_aitutorials");
			}
			else if("image_creation".equals(functionName))
			{
				inAgentContext.setTopLevelFunctionName("welcomeQuestions");
				inAgentContext.setFunctionName("welcomeQuestions");
				inAgentContext.setNextFunctionName("welcomeQuestions");
			}
			else
			{
				inAgentContext.setFunctionName("autoDetectConversation");
			}
			
			return response;
		}
		
		
		throw new OpenEditException("Function not supported " + agentFn);
		
	}

	@Override
	public Collection<SemanticAction> createPossibleFunctionParameters(ScriptLogger inLog)
	{
		return null;
	}
	@Override
	public void savePossibleFunctionSuggestions(ScriptLogger inLog)
	{
		// Nothing to do.
	}	
	

}
