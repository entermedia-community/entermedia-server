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
import org.openedit.Data;
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
		if ("auto_detect_welcome".equals(agentFn))
		{
			inAgentMessage.setValue("chatmessagestatus", "completed");
			
			LlmConnection llmconnection = getMediaArchive().getLlmConnection(inAiFunction.getId()); //Should stay search_start
			LlmResponse response = llmconnection.renderLocalAction(inAgentContext);
			inAgentContext.setFunctionName("auto_detect_conversation");
			return response;
		}
		if ("auto_detect_conversation".equals(agentFn))  //Todo: Rename to Parse
		{
			JSONObject params = new JSONObject();
			params.put("userquery", query);
			
			Collection<Data> toplevelfunctions = getMediaArchive().query("aifunctions").exact("toplevel", true).search();
			params.put("toplevelfunctions", toplevelfunctions);
			
			LlmConnection llmconnection = getMediaArchive().getLlmConnection(agentFn);
			
			LlmResponse response = llmconnection.callToolsFunction(params, agentFn);
			
			log.info(response.getRawResponse());
			
			String functionName = response.getFunctionName();
			JSONObject functionArgs = response.getFunctionArguments();
			
			if(functionName == null || "general_chat".equals(functionName))
			{
				inAgentContext.setFunctionName("auto_detect_conversation");
				String message = (String) functionArgs.get("friendly_response");
				if(message != null)
				{
					response.setMessage(message);
					response.setMessagePlain(message);
				}
				return response;
			}
			inAgentContext.addContext("messagestructured", response.getMessageStructured());
			inAgentContext.addContext("userquery", query);
			inAgentContext.addContext("arguments", functionArgs);
			inAgentContext.setNextFunctionName(functionName);
			
			/*
			// TODO: sync with auto created function names
			if("create_tutorial".equals(functionName)) 
			{
				inAgentContext.addContext("playbackentitymoduleid", "aitutorials");
				inAgentContext.setTopLevelFunctionName("welcome_aitutorials");
				inAgentContext.setFunctionName("welcome_aitutorials");
				inAgentContext.setNextFunctionName("create_aitutorials");
			}
			else if("play_tutorial".equals(functionName))
			{
				inAgentContext.addContext("playbackentitymoduleid", "aitutorials");
				inAgentContext.setTopLevelFunctionName("welcome_aitutorials");
				inAgentContext.setFunctionName("play_tutorial");
				inAgentContext.setNextFunctionName("play_tutorial");
			}
			else if("image_creation".equals(functionName))
			{
				inAgentContext.setTopLevelFunctionName("welcomeQuestions");
				inAgentContext.setFunctionName("welcomeQuestions");
				inAgentContext.setNextFunctionName("welcomeQuestions");
			}
			else
			{
				inAgentContext.setFunctionName("auto_detect_conversation");
			}
			*/
			
			
			return response;
		}
		else if ("auto_detect_sitewide_welcome".equals(agentFn))
		{
			inAgentMessage.setValue("chatmessagestatus", "completed");
			
			LlmConnection llmconnection = getMediaArchive().getLlmConnection(inAiFunction.getId()); //Should stay search_start
			LlmResponse response = llmconnection.renderLocalAction(inAgentContext);
			inAgentContext.setFunctionName("auto_detect_sitewide_parse");
			return response;
		}
		else if ("auto_detect_sitewide_parse".equals(agentFn))
		{
			LlmConnection llmconnection = getMediaArchive().getLlmConnection(inAiFunction.getId()); //Should stay search_start
			LlmResponse response = llmconnection.callToolsFunction(inAgentContext.getContext(), agentFn);
			
			log.info(response.getRawResponse());
			
			String functionName = response.getFunctionName();
			JSONObject functionArgs = response.getFunctionArguments();
			inAgentContext.addContext("arguments", functionArgs);
			inAgentContext.setNextFunctionName(functionName);
		}
		
		
		throw new OpenEditException("Function not supported " + agentFn);
		
	}
	

}
