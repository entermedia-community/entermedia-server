package org.entermediadb.ai.creator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;

public class CreatorManager extends BaseAiManager implements ChatMessageHandler
{
	@Override
	public Collection<SemanticAction> createPossibleFunctionParameters(ScriptLogger inLog) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void savePossibleFunctionSuggestions(ScriptLogger inLog) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public LlmResponse processMessage(AgentContext inAgentContext, MultiValued inAgentMessage, MultiValued inAiFunction)
	{
		Data channel = inAgentContext.getChannel();
		String playbackentitymoduleid = channel.get("playbackentitymoduleid");
		String playbackentityid = channel.get("playbackentityid");
		
		String agentFn = inAgentContext.getFunctionName();
		if(agentFn.startsWith("startCreator"))
		{
			inAgentContext.addContext("playbackentityid", playbackentityid);
			inAgentContext.addContext("playbackentitymoduleid", playbackentitymoduleid);

			LlmConnection llmconnection = getMediaArchive().getLlmConnection("startCreator");
			LlmResponse response = llmconnection.renderLocalAction(inAgentContext);
			return response;
		}
		else if ("conversation".equals(agentFn))
		{
			// TODO
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

		}
		return null;
	}

	
	public void getCreator(WebPageRequest inReq) {
		String playbackentityid = inReq.getRequestParameter("playbackentityid");
		String playbackentitymoduleid = inReq.getRequestParameter("playbackentitymoduleid");

		AgentContext agentContext =  (AgentContext) inReq.getPageValue("agentcontext");

		if(playbackentityid == null)
		{
			playbackentityid = (String) agentContext.getContextValue("playbackentityid");
			playbackentitymoduleid = (String) agentContext.getContextValue("playbackentitymoduleid");
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

		String topicName = inReq.getRequestParameter("name");
		if(entityid == null || entitymoduleid == null || topicName == null || topicName.length() < 5)
		{
			throw new IllegalArgumentException("Missing required parameters");
		}
		boolean featured = "on".equals(inReq.getRequestParameter("featured"));
		
		String playbackentitymoduleid = (String) inReq.getRequestParameter("creatortype");
		Searcher searcher = getMediaArchive().getSearcher(playbackentitymoduleid);
		
		Data playback = searcher.createNewData();
		playback.setName(topicName);
		playback.setValue("entitymoduleid", entitymoduleid);
		playback.setValue("entityid", entityid);
		playback.setValue("featured", featured);
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
			
			inReq.putPageValue("data", playback);
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
				if(line.startsWith("^\\- .*"))
				{
					parentType = "^\\*+ .*";
				}
				else if(Pattern.matches("^\\s*\\- .*", line))
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
		
		String componentcontentid = (String) inComponents.get("componentcontentid");
		
		if(componentcontentid != null)
		{
			Data existing = contentsearcher.loadData(componentcontentid);
			existing.setValue("content", content);
			existing.setValue("assetid", inComponents.get("assetid"));
			existing.setValue("modificationdate", new Date());
			contentsearcher.saveData(existing, null);
			return existing;
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
		
		Data componentSection = contentsearcher.createNewData();

		componentSection.setValue("content", content);
		componentSection.setValue("componentsectionid", inSectionId);
		componentSection.setValue("componenttype", inComponents.get("componenttype"));
		componentSection.setValue("ordering", ordering);
		componentSection.setValue("creationdate", new Date());
		componentSection.setValue("modificationdate", new Date());
		
		
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
			
			String[] paragraphs = contents.split("\\n+");
			int ordering = 0;
			for (int i = 0; i < paragraphs.length; i++)
			{
				String paragraph = paragraphs[i];
				paragraph = paragraph.replaceAll("^\\s+", "");
				paragraph = paragraph.replaceAll("\\s+$", "");
				
				String componenttype = "paragraph";
				if(paragraph.startsWith("#"))
				{
					componenttype = "heading";
				}
				
				if(paragraph.trim().length() == 0)
				{
					continue;
				}
				
				paragraph = paragraph.replaceAll("^#+", "");
				
				String content = md.renderPlain(paragraph);
				
				Data componentcontent = contentearcher.createNewData();
				componentcontent.setValue("componentsectionid", sectionid);
				componentcontent.setValue("content", content.trim());
				componentcontent.setValue("componenttype", componenttype);
				componentcontent.setValue("ordering", ordering);
				componentcontent.setValue("creationdate", new Date());
				componentcontent.setValue("modificationdate", new Date());
				
				tosave.add(componentcontent);
				
				ordering++;
			}
			
			//TODO: add image
			
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
}
