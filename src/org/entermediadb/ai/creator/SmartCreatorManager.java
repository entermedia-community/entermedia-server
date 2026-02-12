package org.entermediadb.ai.creator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.ChatMessageHandler;
import org.entermediadb.ai.assistant.AssistantManager;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.markdown.MarkdownUtil;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.entermedia.util.Inflector;
import org.openedit.hittracker.HitTracker;

public class SmartCreatorManager extends BaseAiManager implements ChatMessageHandler
{
	private static final Log log = LogFactory.getLog(SmartCreatorManager.class);
	
	
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
		
		AiSmartCreatorSteps instructions = inAgentContext.getAiSmartCreatorSteps();
		
		String channelId = inAgentContext.get("channel");
			
		if(agentFn.startsWith("smartcreator_welcome_"))  
		{
			String playbackentitymoduleid = agentFn.substring("smartcreator_welcome_".length());
			Data playbackmodule = getMediaArchive().getCachedData("module", playbackentitymoduleid);
			inAgentContext.addContext("playbackmodule", playbackmodule);

			instructions = new AiSmartCreatorSteps();
			instructions.setTargetModule(playbackmodule);
			inAgentContext.setAiSmartCreatorSteps(instructions);
			
			String entityid = inAgentContext.get("entityid");
			String entitymoduleid = inAgentContext.get("entitymoduleid");
			
			Data entity = getMediaArchive().getCachedData(entitymoduleid, entityid);
			inAgentContext.addContext("entity", entity);
			
			String function = findLocalActionName(inAgentContext);
	
			LlmConnection llmconnection = getMediaArchive().getLlmConnection(function);
			LlmResponse response = llmconnection.renderLocalAction(inAgentContext, function);
			//This is for the chat UI to pass it back
			inAgentContext.setFunctionName("smartcreator_parse");
			return response;
		}
		else if(agentFn.startsWith("smartcreator_parse"))
		{
			Data usermessage = getMediaArchive().getCachedData("chatterbox", inAgentMessage.get("replytoid"));
			String prompt = usermessage.get("message");

			inAgentContext.addContext("creationprompt", prompt);
			LlmConnection llmconnection = getMediaArchive().getLlmConnection(agentFn);
			LlmResponse res = llmconnection.callStructure(inAgentContext, "smartcreator_parse");
			
			JSONObject paragraphs = res.getMessageStructured();
			instructions.loadJsonParts(paragraphs);
			
			//TODO: Show the user what they typed and say processing
			
			if("testchannel".equals(channelId))
			{
				inAgentContext.setFunctionName("smartcreator_createoutline");
				// This allows manually running next function in mediadb test environment
			}
			else
			{				
				inAgentContext.setNextFunctionName("smartcreator_createoutline");
			}
			
			//Show the user what we have thus far
			
			return res;
		}
		else if(agentFn.startsWith("smartcreator_createoutline"))
		{
			LlmConnection llmconnection = getMediaArchive().getLlmConnection("smartcreator_createoutline");
			
			Map payload = new HashMap();
			payload.put("query", instructions.getOutlineCreatePrompt());
			
			AssistantManager assistant = (AssistantManager) getMediaArchive().getBean("assistantManager");
			
			String entityid = inAgentContext.get("entityid");
			String entitymoduleid = inAgentContext.get("entitymoduleid");
			
			Collection<String> parentIds = assistant.findDocIdsForEntity(entitymoduleid, entityid);
			
			payload.put("parent_ids", parentIds);
			
			LlmResponse res = llmconnection.callJson("/create_outline", payload);
			
			JSONObject outlineJson = res.getRawResponse();
			
			Collection<String> outline = (Collection<String>) outlineJson.get("outline");
			
			Collection<String> cleanedOutline = new ArrayList<String>();
			
			for (Iterator iterator = outline.iterator(); iterator.hasNext();) {
				String section = (String) iterator.next();
				section = section.replaceAll("^\\s+", "");
				section = section.replaceAll("\\s+$", "");
				section = section.replaceFirst("^\\d+\\.\\s+", "");
				section = section.replaceFirst("^[A-Za-z].\\s+", "");
				section = section.replaceFirst("^[IVX]+\\.\\s+", "");
				
				cleanedOutline.add(section);
			}
			
			instructions.setProposedSections(cleanedOutline);
			
			inAgentContext.addContext("proposedoutline", instructions.getProposedSections());
			
			//Show the user
//			String function = findLocalActionName(inAgentContext);
			LlmConnection llmconnection2 = getMediaArchive().getLlmConnection(agentFn);
			LlmResponse response = llmconnection2.renderLocalAction(inAgentContext, agentFn);
			
			inAgentContext.setFunctionName("smartcreator_confirmoutline");
			
			return response;
		}
		else if(agentFn.startsWith("smartcreator_confirmoutline"))
		{
			LlmConnection llmconnection = getMediaArchive().getLlmConnection("smartcreator_confirmoutline");
			
			// Adjust the outline as needed using regular AI
			inAgentContext.addContext("proposedoutline", instructions.getProposedSections());
			
			Data usermessage = getMediaArchive().getCachedData("chatterbox", inAgentMessage.get("replytoid"));
			String prompt = usermessage.get("message");
			inAgentContext.addContext("confirmationprompt", prompt);
			
			LlmResponse res = llmconnection.callStructure(inAgentContext, "smartcreator_confirmoutline");
			
			JSONObject updatedSectionsJson = res.getMessageStructured();
			
			Collection<String> updatedSections = (Collection<String>)  updatedSectionsJson.get("updated_outline");
			instructions.setProposedSections(updatedSections);
			
			boolean changed = (boolean) updatedSectionsJson.get("changed");
			
			inAgentContext.addContext("changed", changed);
			
			if(changed)
			{
				inAgentContext.addContext("proposedoutline", instructions.getProposedSections());
				inAgentContext.setFunctionName("smartcreator_confirmoutline");
				
				String function = findLocalActionName(inAgentContext);  //Ask again
				
				LlmConnection llmconnection2 = getMediaArchive().getLlmConnection(function);
				LlmResponse response = llmconnection2.renderLocalAction(inAgentContext, function);
				
				return response;
			}
			else
			{
				Data playbackmodule = instructions.getTargetModule();
				Data playbackentity = getMediaArchive().getSearcher(playbackmodule.getId()).createNewData();
				
				String name = instructions.getTitleName();
				
				name = Inflector.getInstance().capitalize(name);
				
				playbackentity.setName(name);
				playbackentity.setValue(inAgentContext.get("entitymoduleid"), inAgentContext.get("entityid"));
				
				getMediaArchive().saveData(playbackmodule.getId(), playbackentity);
				
				instructions.setTargetEntity(playbackentity);
				
				createConfirmedSections(instructions);
				String step2CreatePrompt = instructions.getStepContentCreate();
				if(step2CreatePrompt != null && !step2CreatePrompt.isEmpty())
				{
					if("testchannel".equals(channelId))
					{
						inAgentContext.setFunctionName("smartcreator_createsectioncontents");
						// This allows manually running next function in mediadb test environment
					}
					else
					{						
						inAgentContext.setNextFunctionName("smartcreator_createsectioncontents");
					}
				}
				else
				{
					inAgentContext.addContext("confirmedoutline", instructions.getConfirmedSections());
					
					inAgentContext.addContext("playbackentity", instructions.getTargetEntity());
					inAgentContext.addContext("playbackentitymodule", instructions.getTargetModule());
					
					
					llmconnection = getMediaArchive().getLlmConnection("smartcreator_renderoutline");
					res = llmconnection.renderLocalAction(inAgentContext, "smartcreator_renderoutline");
				}
				return res;
			}
			
		}
		else if(agentFn.startsWith("smartcreator_createsectioncontents"))
		{	
			populateSectionsWithContents(inAgentContext);
			
			inAgentContext.addContext("confirmedoutline", instructions.getConfirmedSections());
			inAgentContext.addContext("playbackentity", instructions.getTargetEntity());
			inAgentContext.addContext("playbackentitymodule", instructions.getTargetModule());
			LlmConnection llmconnection = getMediaArchive().getLlmConnection("smartcreator_renderoutline");
			LlmResponse response = llmconnection.renderLocalAction(inAgentContext, "smartcreator_renderoutline");
			
			return response;
		}
		else if(agentFn.startsWith("smartcreator_play"))
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
				
				String entityid = inAgentContext.get("entityid");
				String entitymoduleid = inAgentContext.get("entitymoduleid");
				
				Collection<Data> playables = getMediaArchive().query(playbackentitymoduleid).exact("entityid", entityid).exact("entitymoduleid", entitymoduleid).search();
				
				inAgentContext.addContext("playables", playables);
			}
			
			
			String function = findLocalActionName(inAgentContext);
			
			LlmConnection llmconnection = getMediaArchive().getLlmConnection(function);
			LlmResponse response = llmconnection.renderLocalAction(inAgentContext, function);
			
			return response;
		}
		/*
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
*/
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
	
	protected Collection<Data> createConfirmedSections(AiSmartCreatorSteps inInstructions)
	{
		
		MediaArchive archive = getMediaArchive();
		Searcher sectionsearcher = archive.getSearcher("componentsection");

		Collection<Data> tosave = new ArrayList<Data>();

		Collection<String> sections = inInstructions.getProposedSections();
		
		int idx = 0;
		for (Iterator iterator = sections.iterator(); iterator.hasNext();) {
			String outline = (String) iterator.next();
			
			Data componentSection = sectionsearcher.createNewData();

			componentSection.setName(outline);
			componentSection.setValue("playbackentityid", inInstructions.getTargetEntity().getId());
			componentSection.setValue("playbackentitymoduleid", inInstructions.getTargetModule().getId());
			componentSection.setValue("ordering", idx);
			componentSection.setValue("creationdate", new Date());
			componentSection.setValue("modificationdate", new Date());

			tosave.add(componentSection);
			idx++;
		}
		
		sectionsearcher.saveAllData(tosave, null);
		
		inInstructions.setConfirmedSections(tosave);
		
		return tosave;
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
	
	protected boolean isListMd(String line) 
	{
		return Pattern.matches("^\\s*\\- .*", line) || 
			Pattern.matches("^\\d+\\. .*", line) || 
			Pattern.matches("^[A-Za-z]\\. .*", line) || 
			Pattern.matches("^[IVX]+\\. .*", line);
	}
	
	public void populateSectionsWithContents(AgentContext inAgentContext)
	{
		AiSmartCreatorSteps instructions = inAgentContext.getAiSmartCreatorSteps();
		Searcher contentearcher = getMediaArchive().getSearcher("componentcontent");

		Collection<Data> sections = instructions.getConfirmedSections();
		
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("smartcreator_createsectioncontents");

		Collection<Data> tosave = new ArrayList<Data>();

		for (Iterator iterator = sections.iterator(); iterator.hasNext();)
		{
			Data section = (Data) iterator.next();
			String sectionid = section.getId();
			
			Map payload = new HashMap();
			String query = "For a section named: " + section.getName() + "\n\n" + instructions.getContentCreatePrompt();
			payload.put("query", query);
			
			// log.info(query);
			
			String entityid = inAgentContext.get("entityid");
			String entitymoduleid = inAgentContext.get("entitymoduleid");
			
			AssistantManager assistant = (AssistantManager) getMediaArchive().getBean("assistantManager");
			Collection<String> parentIds = assistant.findDocIdsForEntity(entitymoduleid, entityid);
			
			payload.put("parent_ids", parentIds);
			
			LlmResponse res = llmconnection.callJson("/query", payload);
			
			JSONObject contentsJson = res.getRawResponse();
			
			String answer = (String) contentsJson.get("answer");
			
			// log.info("Received contents: " + contents);
			
			if(answer == null)
			{
				return;
			}

			answer = answer.replace("\\n", "\n");
			String[] lines = answer.split("\\n+");

			Collection<Map> boundaries = new ArrayList<Map>();
			List<String> listItems = new ArrayList<String>();
			
			int ordering = 0;
				
			MarkdownUtil md = new MarkdownUtil();

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
				boolean appendCheck = false;
				
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
				else if(isListMd(line))
				{
					listItems.add(line);
					appendCheck = (line.startsWith("*") && line.endsWith("*")) || line.endsWith(":");
				}
				else
				{
					if(!listItems.isEmpty() && !listEnded && appendCheck)
					{						
						String nextline = (i < lines.length - 1) ? lines[i + 1] : null;
						if(nextline != null && !isListMd(line))
						{
							String prevListItem = listItems.get(listItems.size() - 1);
							listItems.set(listItems.size() - 1, prevListItem + "\n" + line);
							continue;
						}
					}
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
			
			for (Iterator iterator2 = boundaries.iterator(); iterator2.hasNext();) 
			{
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

/*
 * Improve speed
			// try semantically matching an asset to the section
			SearchingManager searchingmanager = (SearchingManager) getMediaArchive().getBean("searchingManager");
			String playbackentityid = section.get("playbackentityid");
			String playbackentitymoduleid = section.get("playbackentitymoduleid");
			Data playbackentity = getMediaArchive().getCachedData(playbackentitymoduleid, playbackentityid);
			String creatorName = playbackentity.getName();
			String sectionName = section.getName();
			
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
*/
			
			if (tosave.size() >= 5) 
			{
				contentearcher.saveAllData(tosave, null);
				tosave.clear();
			}
			
		}
		if (!tosave.isEmpty()) 
		{
			contentearcher.saveAllData(tosave, null);
		}
	}


	public void correctGrammar(WebPageRequest inReq, String inComponentcontentid) {
		Data componentcontent = getMediaArchive().getCachedData("componentcontent", inComponentcontentid);
		String content = componentcontent.get("content");
		
		if(content == null || content.isEmpty())
		{
			return;
		}
		
		AgentContext agentcontext = new AgentContext();
		agentcontext.put("paragraph", content);
		
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("startCreator");
		LlmResponse response = llmconnection.callSmartCreatorAiAction(agentcontext, "grammar");
		
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
		
		
		AgentContext agentcontext = new AgentContext();
		agentcontext.put("paragraph", content);
		
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("startCreator");
		LlmResponse response = llmconnection.callSmartCreatorAiAction(agentcontext, "improve");
		
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
		
		
		AgentContext agentcontext = new AgentContext();
		agentcontext.put("prompt", inPrompt);
		
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("startCreator");
		LlmResponse response = llmconnection.callSmartCreatorAiAction(agentcontext, "generate");
		
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

}
