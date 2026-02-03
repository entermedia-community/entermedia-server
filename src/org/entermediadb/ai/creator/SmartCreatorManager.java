package org.entermediadb.ai.creator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.ChatMessageHandler;
import org.entermediadb.ai.assistant.QuestionsManager;
import org.entermediadb.ai.assistant.SearchingManager;
import org.entermediadb.ai.assistant.SemanticAction;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.markdown.MarkdownUtil;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;

public class SmartCreatorManager extends BaseAiManager implements ChatMessageHandler
{
	@Override
	public Collection<SemanticAction> createPossibleFunctionParameters(ScriptLogger inLog) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void savePossibleFunctionSuggestions(ScriptLogger inLog) {
		// Do Nothing
	}
	
	protected String findLocalActionName(AgentContext inAgentContext)
	{
		String agentFn = inAgentContext.getFunctionName();
		String apphome = (String) inAgentContext.getContextValue("apphome");

		String templatepath = apphome + "/views/modules/modulesearch/results/agentresponses/" + agentFn + ".html";
		boolean pageexists = getMediaArchive().getPageManager().getPage(templatepath).exists();
		if(!pageexists)
		{
			int lastone = agentFn.lastIndexOf("_");
			agentFn = agentFn.substring(0,lastone);
		}
		return agentFn;
	}
	
	@Override
	public LlmResponse processMessage(AgentContext inAgentContext, MultiValued inAgentMessage, MultiValued inAiFunction)
	{
		
		String agentFn = inAgentContext.getFunctionName();
			
		if(agentFn.startsWith("smartcreator_welcome_"))  
		{
			String playbackentitymoduleid = agentFn.substring("smartcreator_welcome_".length());
			Data playbackmodule = getMediaArchive().getCachedData("module", playbackentitymoduleid);
			inAgentContext.addContext("playbackmodule", playbackmodule);
			
			String entityid = (String) inAgentContext.getValue("entityid");
			String entitymoduleid = (String) inAgentContext.getValue("entitymoduleid");
			
			Data entity = getMediaArchive().getCachedData(entitymoduleid, entityid);
			inAgentContext.addContext("entity", entity);
			
			String function = findLocalActionName(inAgentContext);
	
			LlmConnection llmconnection = getMediaArchive().getLlmConnection(function);
			LlmResponse response = llmconnection.renderLocalAction(inAgentContext, function);
			inAgentContext.setNextFunctionName("smartcreator_create_" + playbackentitymoduleid);
			return response;
		}
		else if(agentFn.startsWith("smartcreator_create_"))
		{
			String playbackentitymoduleid = agentFn.substring("smartcreator_create_".length());
			inAgentContext.addContext("playbackentitymoduleid", playbackentitymoduleid);
			
			JSONObject arguments = (JSONObject) inAgentContext.getContextValue("arguments");
			
			if(arguments != null && arguments.get("name") != null)
			{
				inAgentContext.addContext("usertopic", arguments.get("name"));
				inAgentContext.addContext("arguments", null);
			}
			else
			{	
				Data usermessage = getMediaArchive().getCachedData("chatterbox", inAgentMessage.get("replytoid"));
				inAgentContext.addContext("usertopic", usermessage.get("message"));
			}
			
			String function = findLocalActionName(inAgentContext);
			
			LlmConnection llmconnection = getMediaArchive().getLlmConnection(function);
			LlmResponse response = llmconnection.renderLocalAction(inAgentContext, function);
			
			return response;
		}
		else if(agentFn.startsWith("smartcreator_play_"))
		{
			String playbackentitymoduleid = agentFn.substring("smartcreator_play_".length());
			
			String playbackentityid = (String) inAgentContext.getContextValue("playbackentityid");
			String viewfallback = "play";
			if(playbackentityid != null)
			{
				inAgentContext.setFunctionName( "smartcreator_playon_" + playbackentitymoduleid);
				inAgentContext.addContext("playbackentityid", playbackentityid);
				inAgentContext.addContext("playbackentitymoduleid", playbackentitymoduleid);
			}
			else
			{				
				
				Data playbackmodule = getMediaArchive().getCachedData("module", playbackentitymoduleid);
				inAgentContext.addContext("playbackmodule", playbackmodule);
				
				String entityid = (String) inAgentContext.getValue("entityid");
				String entitymoduleid = (String) inAgentContext.getValue("entitymoduleid");
				
				Collection<Data> playables = getMediaArchive().query(playbackentitymoduleid).exact("entityid", entityid).exact("entitymoduleid", entitymoduleid).search();
				
				inAgentContext.addContext("playables", playables);
			}
			
			
			String function = findLocalActionName(inAgentContext);
			
			LlmConnection llmconnection = getMediaArchive().getLlmConnection(function);
			LlmResponse response = llmconnection.renderLocalAction(inAgentContext, function);
			
			return response;
		}
		else if ("conversation".equals(agentFn))
		{
			MultiValued usermessage = (MultiValued)getMediaArchive().getCachedData("chatterbox", inAgentMessage.get("replytoid"));
			MultiValued function = (MultiValued)getMediaArchive().getCachedData("aifunction", agentFn);
			LlmResponse response = startChat(inAgentContext, usermessage, inAgentMessage, function);
			String generalresponse  = response.getMessage();
			if(generalresponse != null)
			{
				MarkdownUtil md = new MarkdownUtil();
				generalresponse = md.render(generalresponse);
			}
			response.setMessage(generalresponse);
			inAgentContext.setNextFunctionName(null);
			return response;
		}

		throw new OpenEditException("Function not handled: " + agentFn);
	}

	
	public void getCreator(WebPageRequest inReq) {
		String playbackentityid = inReq.getRequestParameter("playbackentityid");
		String playbackentitymoduleid = inReq.getRequestParameter("playbackentitymoduleid");

		AgentContext agentContext =  (AgentContext) inReq.getPageValue("agentcontext");

		if(playbackentityid == null && agentContext != null)
		{
			playbackentityid = (String) agentContext.getContextValue("playbackentityid");
			playbackentitymoduleid = (String) agentContext.getContextValue("playbackentitymoduleid");
		}
		
		if(playbackentityid == null)
		{
			playbackentityid = inReq.findValue("entityid");
			playbackentitymoduleid = inReq.findValue("module");
		}
		
		if(playbackentityid == null || playbackentitymoduleid == null)
		{
			throw new IllegalArgumentException("Missing playbackentityid or playbackentitymoduleid parameter");
		}
		
		Data playbackmodule = getMediaArchive().getCachedData("module", playbackentitymoduleid);
		inReq.putPageValue("playbackmodule", playbackmodule);
		
		Data playbackentity = getMediaArchive().getCachedData(playbackentitymoduleid, playbackentityid);
		inReq.putPageValue("playbackentity", playbackentity);
		
		
		
		Searcher sectionsearcher = getMediaArchive().getSearcher("componentsection");
		
		Integer playbacksection = null;
		
		String secpage = inReq.getRequestParameter("playbacksection");
		if(secpage != null)
		{
			playbacksection = Integer.parseInt(secpage);
		}
		if(playbacksection == null && agentContext != null)
		{
			playbacksection = (Integer) agentContext.getContextValue("playbacksection");
		}
		
		if(playbacksection != null)
		{
			Data selectedsection = sectionsearcher.query().exact("playbackentityid", playbackentityid)
					.exact("ordering", String.valueOf(playbacksection)).searchOne();
			inReq.putPageValue("selectedsection", selectedsection);
			
			Data prevsection = sectionsearcher.query().exact("playbackentityid", playbackentityid)
					.exact("ordering", String.valueOf(playbacksection - 1)).searchOne();
			if(prevsection != null)
			{				
				inReq.putPageValue("prevsection", prevsection);
			}
			
			Data nextsection = sectionsearcher.query().exact("playbackentityid", playbackentityid)
					.exact("ordering", String.valueOf(playbacksection + 1)).searchOne();
			if(nextsection != null)
			{
				inReq.putPageValue("nextsection", nextsection);
			}
		}
		else
		{
			HitTracker hits = sectionsearcher.query().exact("playbackentityid", playbackentityid).sort("ordering").search();
			inReq.putPageValue("componentsections", hits);
		}
	}
	
	public void createCreatorAndPopulateSection(WebPageRequest inReq)
	{
		String entitymoduleid = inReq.getRequestParameter("entitymoduleid");
		String entityid = inReq.getRequestParameter("entityid");

		String topicName = inReq.getRequestParameter("usertopic");
		if(entityid == null || entitymoduleid == null || topicName == null || topicName.length() < 5)
		{
			throw new IllegalArgumentException("Missing required parameters");
		}
		
		String playbackentitymoduleid = (String) inReq.getRequestParameter("playbackentitymoduleid");
		
		Searcher searcher = getMediaArchive().getSearcher(playbackentitymoduleid);
		
		Data playback = searcher.createNewData();
		playback.setName(topicName);
		playback.setValue("entitymoduleid", entitymoduleid);
		playback.setValue("entityid", entityid);
		playback.setValue(entitymoduleid, entityid);
		//playback.setValue("featured", featured);
		playback.setValue("creatorstatus", "creating");
		
		searcher.saveData(playback, inReq.getUser());

		QuestionsManager questionsmanager = (QuestionsManager) getMediaArchive().getBean("questionsManager");
		
	
		String command = "Create a simple list of index/outline for " + topicName;
		String sections = questionsmanager.getAnswerByEntity(entitymoduleid, entityid, command);
		if(sections != null)
		{
			batchCreateCreatorSection(playback, playbackentitymoduleid, parseSectionString(sections));
			playback.setValue("creatorstatus", "populating");
			
			searcher.saveData(playback, inReq.getUser());
			
			inReq.putPageValue("playbackentity", playback);
			Data playbackentitymodule = getMediaArchive().getCachedData("module", playbackentitymoduleid);
			inReq.putPageValue("playbackentitymodule", playbackentitymodule);
		}
		
		getMediaArchive().fireSharedMediaEvent("llm/creatorcomponentcontent");
		
	}
	
	protected void batchCreateCreatorSection(Data inPlayback, String playbackentitymoduleid, Collection<String> inSections)
	{
		MediaArchive archive = getMediaArchive();
		Searcher sectionsearcher = archive.getSearcher("componentsection");

		Collection<Data> tosave = new ArrayList<Data>();

		int idx = 0;
		for (Iterator iterator = inSections.iterator(); iterator.hasNext();) {
			String outline = (String) iterator.next();
			
			Data componentSection = sectionsearcher.createNewData();

			componentSection.setName(outline);
			componentSection.setValue("playbackentityid", inPlayback.getId());
			componentSection.setValue("playbackentitymoduleid", playbackentitymoduleid);
			componentSection.setValue("entitymoduleid", inPlayback.get("entitymoduleid"));
			componentSection.setValue("entityid", inPlayback.get("entityid"));
			componentSection.setValue("ordering", idx);
			componentSection.setValue("creationdate", new Date());
			componentSection.setValue("modificationdate", new Date());

			tosave.add(componentSection);
			idx++;
		}
		
		sectionsearcher.saveAllData(tosave, null);
		
	}

	protected Collection<String> parseSectionString(String inOutlines)
	{
		Collection<String> outlineitems = new ArrayList<String>();
		
		String parentType = null;
		
		String[] lines = inOutlines.split("\n");
		for (int i = 0; i < lines.length; i++)
		{	
			String line = lines[i];
			
			if(parentType == null)
			{				
				if(Pattern.matches("^\\s*\\- .*", line))
				{
					parentType = "^\\s*\\- .*";
				}
				else if(Pattern.matches("^\\d+\\. .*", line))
				{
					parentType = "^\\d+\\. .*";
				}
				else
				{
					continue;
				}
			}
			if(!Pattern.matches(parentType, line))
			{
				continue;
			}
			String cleaned = line.trim();
			cleaned = cleaned.replaceAll("^\\d+\\. ", "");
			cleaned = cleaned.replaceAll("^\\- ", "");
			cleaned = cleaned.replaceAll("^\\*+", "");
			cleaned = cleaned.replaceAll("\\*+$", "");
			cleaned = cleaned.trim();
			
			outlineitems.add(cleaned);
		}
		
		return outlineitems;
	}
	
	public Data createCreatorSection(Data inPlayback, String inPlaybackModuleId, Map inFields)
	{
		MediaArchive archive = getMediaArchive();
		
		Searcher sectionsearcher = archive.getSearcher("componentsection");
		
		String sectionid = (String) inFields.get("sectionid");
		
		if(sectionid != null)
		{
			Data existing = sectionsearcher.loadData(sectionid);
			existing.setName((String) inFields.get("name"));
			existing.setValue("modificationdate", new Date());
			sectionsearcher.saveData(existing, null);
			return existing;
		}
		
		
		int inOrdering = (int) inFields.get("ordering");
		
		Data section = sectionsearcher.createNewData();

		section.setName("New Section");
		section.setValue("playbackentityid", inPlayback.getId());
		section.setValue("playbackentitymoduleid", inPlaybackModuleId);
		section.setValue("entitymoduleid", inPlayback.get("entitymoduleid"));
		section.setValue("entityid", inPlayback.get("entityid"));
		section.setValue("ordering", inOrdering);
		section.setValue("creationdate", new Date());
		section.setValue("modificationdate", new Date());
		
		sectionsearcher.saveData(section, null);
		
		Collection<MultiValued> allSections = sectionsearcher.query()
				.exact("playbackentityid", inPlayback.getId())
				.moreThan("ordering", inOrdering-1)
				.search();
		Collection<Data> tosave = new ArrayList<Data>();
		for (Iterator iterator = allSections.iterator(); iterator.hasNext();) {
			MultiValued data = (MultiValued) iterator.next();
			if(data.getId().equals(section.getId()))
			{
				continue;
			}
			int currentordering = data.getInt("ordering");
			if(currentordering >= inOrdering)
			{
				data.setValue("ordering", currentordering + 1);
				tosave.add(data);
			}
		}
		sectionsearcher.saveAllData(tosave, null);
		
		reorderAll(sectionsearcher);
		
		return section;
	}
	
	public Data createComponentContent(String inSectionId, Map inComponents)
	{
		MediaArchive archive = getMediaArchive();
		Searcher contentsearcher = archive.getSearcher("componentcontent");
		
		String content = (String) inComponents.get("content");
		if(content == null)
		{
			content = "";
		}
		
		content = content.replaceAll("<p.*>&nbsp;</p>", "\n");
		content = content.replaceAll("<p.*></p>", "\n");
		content = content.replaceAll("^\\s+", "");
		content = content.replaceAll("\\s+$", "");
		
		Data componentSection = contentsearcher.createNewData();
		
		String componentcontentid = (String) inComponents.get("componentcontentid");
		
		if(componentcontentid != null)
		{
			componentSection = contentsearcher.loadData(componentcontentid);			
		}
		
		componentSection.setValue("content", content);
		componentSection.setValue("assetid", inComponents.get("assetid"));
		componentSection.setValue("modificationdate", new Date());
		
		if(inComponents.get("question") != null)
		{
			createQuestionForContent(componentSection, inComponents);
		}
		
		if(componentcontentid != null)
		{
			contentsearcher.saveData(componentSection, null);
			return componentSection;
		}
		
		Collection<MultiValued> allCompononets = contentsearcher.query().exact("componentsectionid", inSectionId).search();
		
		String orderingStr = (String)  inComponents.get("ordering");
		
		int ordering = -1;
		
		try
		{			
			ordering = Integer.parseInt(orderingStr) + 1;
		}
		catch (Exception e)
		{
			//ignore
		}
		
		if(ordering < 0)
		{
			ordering = allCompononets.size();
		}

		componentSection.setValue("componentsectionid", inSectionId);
		componentSection.setValue("componenttype", inComponents.get("componenttype"));
		componentSection.setValue("ordering", ordering);
		componentSection.setValue("creationdate", new Date());
		
		
		contentsearcher.saveData(componentSection, null);
		
		Collection<Data> tosave = new ArrayList<Data>();
		for (Iterator iterator = allCompononets.iterator(); iterator.hasNext();) {
			MultiValued data = (MultiValued) iterator.next();
			if(data.getId().equals(componentSection.getId()))
			{
				continue;
			}
			int currentordering = data.getInt("ordering");
			if(currentordering >= ordering)
			{
				data.setValue("ordering", currentordering + 1);
				tosave.add(data);
			}
		}
		contentsearcher.saveAllData(tosave, null);
		
		reorderAll(contentsearcher);
		
		return componentSection;
		
	}
	
	private void createQuestionForContent(Data inComponentSection, Map inComponents) {
		Searcher questionsearcher = getMediaArchive().getSearcher("entityquestion");
		Data question = questionsearcher.createNewData();
		if(inComponentSection.get("questionid") != null)
		{
			question = questionsearcher.loadData(inComponentSection.get("questionid"));
		}
		question.setValue("question", inComponents.get("question"));

		question.setValue("mcq", inComponents.get("mcq"));
		question.setValue("option_a", inComponents.get("option_a"));
		question.setValue("option_b", inComponents.get("option_b"));
		question.setValue("option_c", inComponents.get("option_c"));
		question.setValue("option_d", inComponents.get("option_d"));
		question.setValue("mcqcognitivelevel", inComponents.get("mcqcognitivelevel"));
		question.setValue("mcqoptions", inComponents.get("mcqoptions"));
		question.setValue("rationale", inComponents.get("rationale"));
		question.setValue("grade", inComponents.get("grade"));
		
		questionsearcher.saveData(question, null);
		
		inComponentSection.setValue("questionid", question.getId());
	}


	public Data duplicateCreatorSection(String inSearchType, String inId) {
		Searcher sectionsearcher = getMediaArchive().getSearcher(inSearchType);
		MultiValued section = (MultiValued) sectionsearcher.loadData(inId);
		
		if(section != null)
		{			
			int currentordering = section.getInt("ordering");

			Data newsection = sectionsearcher.createNewData();
			
			for (Iterator iterator = section.keySet().iterator(); iterator.hasNext();) {
				String key = (String) iterator.next();
				if(key.equals("id") || key.startsWith("."))
				{
					continue;
				}
				if("ordering".equals(key))
				{
					newsection.setValue("ordering", currentordering + 1);
					continue;
				}
				newsection.setValue(key, section.getValue(key));
			}
			sectionsearcher.saveData(newsection, null);
			
			Collection<MultiValued> all = new ArrayList<MultiValued>();
			if("componentcontent".equals(inSearchType))
			{
				all = sectionsearcher.query().exact("componentsectionid", section.get("componentsectionid")).search();
			}
			else if("componentsection".equals(inSearchType))
			{			
				all = sectionsearcher.query().exact("playbackentityid", section.get("playbackentityid")).search();
			}
			
			Collection<Data> tosave = new ArrayList<Data>();
			for (Iterator iterator = all.iterator(); iterator.hasNext();) {
				MultiValued data = (MultiValued) iterator.next();
				if(data.getId().equals(newsection.getId()))
				{
					continue;
				}
				int ordering = data.getInt("ordering");
				if(ordering >= currentordering)
				{
					data.setValue("ordering", ordering + 1);
					tosave.add(data);
				}
			}
			sectionsearcher.saveAllData(tosave, null);
			
			reorderAll(sectionsearcher);
			
			return newsection;
		}
		
		return null;
		
	}
	
	public void orderCreatorSection(WebPageRequest inReq)
	{
		String sourceid = inReq.getRequestParameter("source");
		String targetid = inReq.getRequestParameter("target");
		String sourceorder = inReq.getRequestParameter("sourceorder");
		String targetorder = inReq.getRequestParameter("targetorder");
		
		String searchtype = inReq.getRequestParameter("searchtype");
		Searcher searcher = getMediaArchive().getSearcher(searchtype);
		
		Data source = searcher.loadData(sourceid);
		Data target = searcher.loadData(targetid);
		
		try 
		{
			if(source != null)
			{				
				source.setValue("ordering", Integer.parseInt(targetorder));
				searcher.saveData(source, inReq.getUser());
			}
			if(target != null)
			{				
				target.setValue("ordering", Integer.parseInt(sourceorder));
				searcher.saveData(target, inReq.getUser());
			}
			
			reorderAll(searcher);
		}
		catch (Exception e) 
		{
			throw new RuntimeException(e);
		}
		
		
	}

	public void deleteCreatorSection(String inSearchType, String inId) {
		Searcher sectionsearcher = getMediaArchive().getSearcher(inSearchType);
		Data section = sectionsearcher.loadData(inId);
		if(section != null)
		{			
			sectionsearcher.delete(section, null);
		}
		reorderAll(sectionsearcher);
	}
	
	protected void reorderAll(Searcher searcher)
	{
		HitTracker inHits = searcher.query().sort("ordering").search();
		Collection<Data> tosave = new ArrayList<Data>();
		int idx = 0;
		for (Iterator iterator = inHits.iterator(); iterator.hasNext();) {
			MultiValued data = (MultiValued) iterator.next();
			data.setValue("ordering", idx);
			tosave.add(data);
			idx++;
		}
		searcher.saveAllData(tosave, null);
	}
	
	public void autoPopulateComponentContent() 
	{
		Searcher contentearcher = getMediaArchive().getSearcher("componentcontent");
		Searcher sectionsearcher = getMediaArchive().getSearcher("componentsection");
		
		QuestionsManager questionsmanager = (QuestionsManager) getMediaArchive().getBean("questionsManager");

		HitTracker sections = sectionsearcher.query().all().search();
		
		Collection<Data> tosave = new ArrayList<Data>();

		MarkdownUtil md = new MarkdownUtil();
		
		for (Iterator iterator = sections.iterator(); iterator.hasNext();) 
		{
			MultiValued section = (MultiValued) iterator.next();
			String sectionid = section.getId();
			
			SearchQuery orquery = contentearcher.query().exists("content").exists("asseid").or().getQuery();
			HitTracker existingcomponentcontents = contentearcher.query().exact("componentsectionid", sectionid).addchild(orquery).search();
			
			if(!existingcomponentcontents.isEmpty())
			{
				continue;
			}
			
			String playbackentityid = section.get("playbackentityid");
			String playbackentitymoduleid = section.get("playbackentitymoduleid");
			Data playbackentity = getMediaArchive().getCachedData(playbackentitymoduleid, playbackentityid);
			
			String creatorName = playbackentity.getName();
			
			String sectionName = section.getName();
			
			String command = "Create a detailed description of " + sectionName + " that is relevant to " + creatorName;
			
			String contents = questionsmanager.getAnswerByEntity(section.get("entitymoduleid"), section.get("entityid"), command);
			
			if(contents == null)
			{
				continue;
			}
			
			contents = contents.replace("\\n", "\n");
			
			String[] lines = contents.split("\\n+");
			
			Collection<Map> boundaries = new ArrayList<Map>();
			
			Collection<String> listItems = new ArrayList<String>();
			
			for (int i = 0; i < lines.length; i++)
			{
				String line = lines[i];
				line = line.replaceAll("^\\s+", "");
				line = line.replaceAll("\\s+$", "");
				
				if(line.length() == 0)
				{
					continue;
				}
				boolean listEnded = false;
				
				if(line.startsWith("#"))
				{
					listEnded = true;
					Map boundary = new HashMap();
					boundary.put("componenttype", "heading");
					
					line = line.replaceAll("^#+", "");
					line = md.renderPlain(line);
					boundary.put("content", line);
					boundaries.add(boundary);
				}
				else if(Pattern.matches("^\\s*\\- .*", line) ||	Pattern.matches("^\\d+\\. .*", line))
				{
					listItems.add(line);
				}
				else
				{
					listEnded = true;
					Map boundary = new HashMap();
					boundary.put("componenttype", "paragraph");
					line = md.renderPlain(line);
					boundary.put("content", line);
					boundaries.add(boundary);
				}
				
				if(listEnded && !listItems.isEmpty())
				{
					String listcontent = String.join("\n", listItems);
					listcontent = md.renderPlain(listcontent);
					Map boundary = new HashMap();
					boundary.put("componenttype", "paragraph");
					boundary.put("content", listcontent);
					boundaries.add(boundary);
					
					listItems.clear();
				}
			}
			
			
			int ordering = 0;
			for (Iterator iterator2 = boundaries.iterator(); iterator2.hasNext();) {
				Map boundary = (Map) iterator2.next();
				
				
				Data componentcontent = contentearcher.createNewData();
				componentcontent.setValue("componentsectionid", sectionid);
				componentcontent.setValue("content", boundary.get("content"));
				componentcontent.setValue("componenttype", boundary.get("componenttype"));
				componentcontent.setValue("ordering", ordering);
				componentcontent.setValue("creationdate", new Date());
				componentcontent.setValue("modificationdate", new Date());
				
				tosave.add(componentcontent);
				
				ordering++;
			}
			
			SearchingManager searchingmanager = (SearchingManager) getMediaArchive().getBean("searchingManager");
			
			Data asset = searchingmanager.semanticSearchBestMatch(creatorName + " " + sectionName, "asset");
			if(asset != null)
			{
				Data componentcontent = contentearcher.createNewData();
				componentcontent.setValue("componentsectionid", sectionid);
				componentcontent.setValue("assetid", asset.getId());
				String caption = asset.get("headline");
				if(caption == null)
				{
					caption = asset.get("longcaption");
				}
				if(caption != null)
				{					
					componentcontent.setValue("content", caption);
				}
				componentcontent.setValue("componenttype", "asset");
				componentcontent.setValue("ordering", ordering);
				componentcontent.setValue("creationdate", new Date());
				componentcontent.setValue("modificationdate", new Date());
			}
			
		}
		
		contentearcher.saveAllData(tosave, null);
	}


	public void correctGrammar(WebPageRequest inReq, String inComponentcontentid) {
		Data componentcontent = getMediaArchive().getCachedData("componentcontent", inComponentcontentid);
		String content = componentcontent.get("content");
		
		if(content == null || content.isEmpty())
		{
			return;
		}
		
		
		Map params = new HashMap();
		params.put("paragraph", content);
		
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("startCreator");
		LlmResponse response = llmconnection.callSmartCreatorAiAction(params, "grammar");
		
		JSONObject result = response.getMessageStructured();
		if(result != null)
		{
			String corrected_text = (String) result.get("corrected_text");
			if(corrected_text != null)
			{
				inReq.putPageValue("paragraph", corrected_text);
			}
		}
		
	}


	public void improveContent(WebPageRequest inReq, String inComponentcontentid) {
		Data componentcontent = getMediaArchive().getCachedData("componentcontent", inComponentcontentid);
		String content = componentcontent.get("content");
		
		if(content == null || content.isEmpty())
		{
			return;
		}
		
		
		Map params = new HashMap();
		params.put("paragraph", content);
		
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("startCreator");
		LlmResponse response = llmconnection.callSmartCreatorAiAction(params, "improve");
		
		JSONObject result = response.getMessageStructured();
		if(result != null)
		{
			String paragraph = (String) result.get("paragraph");
			if(paragraph != null)
			{
				inReq.putPageValue("paragraph", paragraph);
			}
		}
	}


	public void generateContent(WebPageRequest inReq, String inComponentcontentid, String inPrompt) {
		Data componentcontent = getMediaArchive().getCachedData("componentcontent", inComponentcontentid);
		String content = componentcontent.get("content");
		
		if(content == null || content.isEmpty())
		{
			return;
		}
		
		
		Map params = new HashMap();
		params.put("prompt", inPrompt);
		
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("startCreator");
		LlmResponse response = llmconnection.callSmartCreatorAiAction(params, "generate");
		
		JSONObject result = response.getMessageStructured();
		if(result != null)
		{
			String paragraph = (String) result.get("paragraph");
			if(paragraph != null)
			{
				inReq.putPageValue("paragraph", paragraph);
			}
		}
		
	}


	public void createImage(WebPageRequest inReq, String inComponentcontentid, String inPrompt) {
		// TODO Auto-generated method stub
		
	}


	public void captionImage(WebPageRequest inReq, String inComponentcontentid, String inAssetid) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void getDetectorParams(AgentContext inAgentContext, MultiValued inTopLevelFunction) {
		// TODO Auto-generated method stub
		
	}
}
