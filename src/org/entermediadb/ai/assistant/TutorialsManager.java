package org.entermediadb.ai.assistant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.ChatMessageHandler;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
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
	
	public void createTutorial(WebPageRequest inReq)
	{
		String moduleid = inReq.getRequestParameter("entitymoduleid");
		String entityid = inReq.getRequestParameter("entityid");
		String tutorialTopic = inReq.getRequestParameter("name");
		if(moduleid == null || entityid == null || tutorialTopic == null || tutorialTopic.length() < 5)
		{
			throw new IllegalArgumentException("Missing required parameters");
		}
		boolean featured = "on".equals(inReq.getRequestParameter("featured"));
		
		Searcher searcher = getMediaArchive().getSearcher("aitutorials");
		Data tutorial = searcher.createNewData();
		tutorial.setName(tutorialTopic);
		tutorial.setValue("entitymoduleid", moduleid);
		tutorial.setValue("entityid", entityid);
		tutorial.setValue("featured", featured);
		
		QuestionsManager questionsmanager = (QuestionsManager) getMediaArchive().getBean("questionsManager");
		
		searcher.saveData(tutorial, inReq.getUser());
	
		String command = "Create a outline in a list format for " + tutorialTopic;
		String outlines = questionsmanager.getAnswerByEntity(moduleid, entityid, command);
		
		createComponentContentForTutorial(tutorial, parseOutlines(outlines));
		
		inReq.putPageValue("tutorial", tutorial);
	}
	
	int lastParent = 0;

	protected Collection<Map> parseOutlines(String inOutlines)
	{
		Collection<Map> outlineitems = new ArrayList<Map>();
		
		String[] lines = inOutlines.split("\n");
		for (int i = 0; i < lines.length; i++)
		{
			Map outlineitem = new HashMap();
			
			String line = lines[i];
			if(line.matches("^\\- ")) 
			{
				lastParent++;
				outlineitem.put("id", "outline-" + lastParent);
				outlineitem.put("parent", null);
			}
			else if(line.matches("^\\s+\\- ")) 
			{
				outlineitem.put("id", "outline-child-" + lastParent + "-" + i);
				outlineitem.put("parent", "outline-" + lastParent);
			}
			else
			{
				continue;
			}
			String cleaned = line.trim();
			cleaned = cleaned.replaceAll("^\\- ", "");
			cleaned = cleaned.replaceAll("^\\s+\\- ", "");
			cleaned = cleaned.replaceAll("^\\*+", "");
			cleaned = cleaned.replaceAll("\\*+$", "");
			outlineitem.put("label", cleaned.trim());
			
			outlineitems.add(outlineitem);
		}
		
		return outlineitems;
	}

	protected void createComponentContentForTutorial(Data inTutorial, Collection<Map> inOutlines)
	{
		MediaArchive archive = getMediaArchive();
		Searcher contentsearcher = archive.getSearcher("componentcontent");

		String entitymoduleid = inTutorial.get("entitymoduleid");
		String entityid = inTutorial.get("entityid");
		
		Collection<Data> tosave = new ArrayList<Data>();
		int idx = 0;
		for (Iterator iterator = inOutlines.iterator(); iterator.hasNext();) {
			Map outline = (Map) iterator.next();
			
			Data componentContent = contentsearcher.createNewData();
			String label = (String) outline.get("label");
			componentContent.setName(label);
			componentContent.setValue("tutorialid", inTutorial.getId());
			componentContent.setValue("entitymoduleid", entitymoduleid);
			componentContent.setValue("entityid", entityid);
			componentContent.setValue("componenttype", "text");

			componentContent.setValue("json", createJSONForOutline(idx, outline));

			tosave.add(componentContent);
			idx++;
			
		}
		
		contentsearcher.saveAllData(tosave, null);
		
	}

	protected String createJSONForOutline(int index, Map inOutline)
	{
		MediaArchive archive = getMediaArchive();
		
		String id = (String) inOutline.get("id");
		String parent = (String) inOutline.get("parent");
		String label = (String) inOutline.get("label");
		
		Map attr = new HashMap();

		attr.put("groupid", id);
		int x = 50;
		if( parent != null ) {
			x = 100;
			attr.put("parent", parent);
		}
		attr.put("x", x);
		int y = 50 + (index * 100);
		attr.put("y", y);
		int width = 400;
		attr.put("width", width);
		int height = 80;
		attr.put("height", height);
		
		attr.put("label", label);
		attr.put("labelx", x + (width/2) - 5);
		attr.put("labely", y + 10);
		
		String templatepath = "/" + getMediaArchive().getMediaDbId() + "/ai/default/calls/aitutorials/component.json";
			
		Page template = archive.getPageManager().getPage(templatepath);
				
		if (!template.exists())
		{
			templatepath = "/" + archive.getCatalogId() + "/ai/default/calls/aitutorials/component.json";
			template = archive.getPageManager().getPage(templatepath);
		}
			
		if (!template.exists())
		{
			throw new OpenEditException("component.json template not found at " + templatepath);
		}
			
		LlmConnection llmconnection = archive.getLlmConnection("startTutorials");
	
		String comnponentJson = llmconnection.loadInputFromTemplate(templatepath, attr);

		return comnponentJson;
	}

}
