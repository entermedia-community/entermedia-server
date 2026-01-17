package org.entermediadb.ai.creator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.assistant.QuestionsManager;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;

public class CreatorManager extends BaseAiManager 
{
	public void getCreator(WebPageRequest inReq) {
		String tutorialid = inReq.getRequestParameter("tutorialid");
		if(tutorialid == null)
		{
			throw new IllegalArgumentException("Missing tutorialid parameter");
		}
		Searcher tutorialsearcher = getMediaArchive().getSearcher("aitutorials");
		Data tutorial = tutorialsearcher.query().id(tutorialid).searchOne();
		inReq.putPageValue("tutorial", tutorial);
		
		Searcher sectionsearcher = getMediaArchive().getSearcher("componentsection");
		HitTracker hits = sectionsearcher.query().exact("tutorialid", tutorialid).sort("ordering").search();
		inReq.putPageValue("componentsections", hits);
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
	
		String command = "Create a simple list of tutorial index/outline for " + tutorialTopic;
		String sections = questionsmanager.getAnswerByEntity(moduleid, entityid, command);
		if(sections != null)
		{			
			createComponentSection(tutorial, parseOutlines(sections));
			
			inReq.putPageValue("tutorial", tutorial);
		}
		
	}

	protected Collection<String> parseOutlines(String inOutlines)
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

	public void createComponentSection(String inTutorialId, String inSection)
	{
		MediaArchive archive = getMediaArchive();
		Searcher tutorialsearcher = archive.getSearcher("aitutorials");
		Data inTutorial = tutorialsearcher.loadData(inTutorialId);
		
		Collection<String> sections = new ArrayList<String>();
		sections.add(inSection);
		
		createComponentSection(inTutorial, sections);
	}
	
	public void createComponentSection(Data inTutorial, Collection<String> inSections)
	{
		MediaArchive archive = getMediaArchive();
		Searcher sectionsearcher = archive.getSearcher("componentsection");

		String entitymoduleid = inTutorial.get("entitymoduleid");
		String entityid = inTutorial.get("entityid");
		
		Collection<Data> tosave = new ArrayList<Data>();
		int idx = 0;
		for (Iterator iterator = inSections.iterator(); iterator.hasNext();) {
			String outline = (String) iterator.next();
			
			Data componentSection = sectionsearcher.createNewData();

			componentSection.setName(outline);
			componentSection.setValue("tutorialid", inTutorial.getId());
			componentSection.setValue("entitymoduleid", entitymoduleid);
			componentSection.setValue("entityid", entityid);
			componentSection.setValue("ordering", idx);
			componentSection.setValue("creationdate", new Date());
			componentSection.setValue("modificationdate", new Date());

			tosave.add(componentSection);
			idx++;
			
		}
		
		sectionsearcher.saveAllData(tosave, null);
		
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
		content = content.replaceAll("^\\s+", "");
		content = content.replaceAll("\\s+$", "");
		
		String componentcontentid = (String) inComponents.get("componentcontentid");
		
		String order = (String)  inComponents.get("ordering");
		int ordering = Integer.parseInt(order);
		if(ordering < 0)
		{
			ordering = 0;
		}
		
		if(componentcontentid != null)
		{
			Data existing = contentsearcher.loadData(componentcontentid);
			existing.setValue("content", content);
			existing.setValue("modificationdate", new Date());
			existing.setValue("ordering", ordering);
			contentsearcher.saveData(existing, null);
			return existing;
		}
		else
		{
			ordering = Math.max(ordering + 1, 0);
		}
		
		Data componentSection = contentsearcher.createNewData();

		componentSection.setValue("content", content);
		componentSection.setValue("componentsectionid", inSectionId);
		componentSection.setValue("componenttype", inComponents.get("componenttype"));
		componentSection.setValue("ordering", ordering);
		componentSection.setValue("creationdate", new Date());
		componentSection.setValue("modificationdate", new Date());
		
		
		contentsearcher.saveData(componentSection, null);
		
		return componentSection;
		
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
		
		source.setValue("ordering", Integer.parseInt(targetorder));
		target.setValue("ordering", Integer.parseInt(sourceorder));
		
		searcher.saveData(source, inReq.getUser());
		searcher.saveData(target, inReq.getUser());
	}

	public void deleteCreatorSection(String inSearchType, String inId) {
		Searcher sectionsearcher = getMediaArchive().getSearcher(inSearchType);
		Data section = sectionsearcher.loadData(inId);
		if(section != null)
		{			
			sectionsearcher.delete(section, null);
		}
	}
}
