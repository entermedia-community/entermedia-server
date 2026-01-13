package org.entermediadb.ai.assistant;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.ChatMessageHandler;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONObject;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.util.JSONParser;

public class TutorialsManager extends BaseAiManager implements ChatMessageHandler
{
	@Override
	public LlmResponse processMessage(AgentContext inAgentContext, MultiValued inAgentMessage, MultiValued inAiFunction)
	{
		// Currently no specific implementation for TutorialsManager
		return null;
	}

	@Override
	public Collection<SemanticAction> createPossibleFunctionParameters(ScriptLogger inLog) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void savePossibleFunctionSuggestions(ScriptLogger inLog)
	{
		savePossibleFunctionSuggestions(inLog, "Tutorial");
	}
	
	public JSONObject createTutorialOutline(ScriptLogger inLog, String tutorialTitle)
	{
		Map params = new HashMap();
		params.put("tutorialtitle", tutorialTitle);
		
		MediaArchive archive = getMediaArchive();
		
		String templatepath = "/" + getMediaArchive().getMediaDbId() + "/ai/default/calls/creations/createTutorial.json";
		
		Page template = archive.getPageManager().getPage(templatepath);
			
		if (!template.exists())
		{
			templatepath = "/" + archive.getCatalogId() + "/ai/default/calls/creations/createTutorial.json";
			template = archive.getPageManager().getPage(templatepath);
		}
		
		if (!template.exists())
		{
			throw new OpenEditException("creaeTutorial.json template not found at " + templatepath);
		}
		
		LlmConnection llmconnection = archive.getLlmConnection("startTutorials");
		String definition = llmconnection.loadInputFromTemplate(templatepath, params);

		JSONParser parser = new JSONParser();
		JSONObject payload = (JSONObject) parser.parse(definition);
		
		LlmResponse response = llmconnection.callJson("/chat/completions", payload);
		
		return response.getMessageStructured();
	}

}
