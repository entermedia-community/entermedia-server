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
		// String outlines = "- **Prepare the Beetroot Puree**  \n"
		// 		+ "  - Toast coriander, fennel, cumin, and mustard seeds in a dry pan until fragrant, then grind to a fine powder.  \n"
		// 		+ "  - Roast Datterini tomatoes with red wine vinegar, olive oil, sea salt, and black pepper at 180°C until wrinkled.  \n"
		// 		+ "  - Boil and peel beetroot, then dress with balsamic vinegar, olive oil, and salt.  \n"
		// 		+ "  - Combine roasted tomatoes, beetroot, and spice mix; blend until smooth. Fold in Greek yogurt, horseradish, and chopped herbs. Adjust seasoning.  \n"
		// 		+ "\n"
		// 		+ "- **Make the Parsley Oil**  \n"
		// 		+ "  - Blanch spinach and parsley in boiling water, then ice bath. Drain and squeeze excess water.  \n"
		// 		+ "  - Blend with grapeseed oil and a pinch of salt until smooth and warm. Strain through muslin for a clean oil.  \n"
		// 		+ "\n"
		// 		+ "- **Assemble the Salad**  \n"
		// 		+ "  - Plate the beetroot puree as a base.  \n"
		// 		+ "  - Drizzle with parsley oil.  \n"
		// 		+ "  - Garnish with roasted tomatoes, toasted seeds, and fresh herbs.  \n"
		// 		+ "  - Serve with additional components like salt-baked beetroot (if prepared separately) and green salad elements.";
		
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
			if(line.startsWith("- "))
			{
				lastParent++;
				outlineitem.put("id", "outline-" + lastParent);
				outlineitem.put("parent", null);
			}
			else if(Pattern.matches("^\\s+\\- .*", line))
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
