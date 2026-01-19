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
import org.openedit.MultiValued;
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
			batchCreateCreatorSection(tutorial, parseOutlines(sections));
			
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

	
	
	public void batchCreateCreatorSection(Data inTutorial, Collection<String> inSections)
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
	
	public Data createCreatorSection(Data inTutorial, Map inFields)
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
		section.setValue("tutorialid", inTutorial.getId());
		section.setValue("entitymoduleid", inTutorial.get("entitymoduleid"));
		section.setValue("entityid", inTutorial.get("entityid"));
		section.setValue("ordering", inOrdering);
		section.setValue("creationdate", new Date());
		section.setValue("modificationdate", new Date());
		
		sectionsearcher.saveData(section, null);
		
		Collection<MultiValued> allSections = sectionsearcher.query().exact("tutorialid", inTutorial.getId()).search();
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
				all = sectionsearcher.query().exact("tutorialid", section.get("tutorialid")).search();
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
	}
}
