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
			//TODO: Move to tools call
			if( query.contains("question") )
			{
				inAgentContext.setFunctionName("welcomeQuestions");
				inAgentContext.setNextFunctionName("welcomeQuestions");
			}
			else if( query.contains("tutorial") )
			{
				inAgentContext.setFunctionName("welcomecreate_aitutorials");
				inAgentContext.setNextFunctionName("welcomecreate_aitutorials");
			}
			else if( query.contains("search") )
			{
				inAgentContext.setFunctionName("askQuestion");
			}
			
//			MultiValued function = (MultiValued) getMediaArchive().getCachedData("aifunction", agentFn);
//			LlmResponse response = startChat(inAgentContext, usermessage, inAgentMessage, function);
//			
//			//Set the new top level function
//			String next = inAgentContext.getNextFunctionName();
//			if( next != null)
//			{
//				inAgentContext.setTopLevelFunctionName(next); //Switch modes
//			}
			
			//return response;
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
		
	}	
	

}
