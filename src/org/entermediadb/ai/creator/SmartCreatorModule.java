package org.entermediadb.ai.creator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.entermediadb.ai.assistant.AssistantManager;
import org.entermediadb.ai.assistant.CreationManager;
import org.entermediadb.ai.assistant.GuideStatus;
import org.entermediadb.ai.assistant.QuestionsManager;
import org.entermediadb.ai.assistant.SearchingManager;
import org.entermediadb.ai.assistant.SemanticAction;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;

public class SmartCreatorModule extends BaseMediaModule {
	
	public AssistantManager getAssistantManager(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		AssistantManager assistantManager = (AssistantManager) getMediaArchive(catalogid).getBean("assistantManager");
		return assistantManager;
	}

	public SmartCreatorManager getSmartCreatorManager(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		SmartCreatorManager smartCreatorManager = (SmartCreatorManager) getMediaArchive(catalogid).getBean("smartCreatorManager");
		return smartCreatorManager;
	}

	public QuestionsManager getQuestionsManager(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		QuestionsManager questionsManager = (QuestionsManager) getMediaArchive(catalogid).getBean("questionsManager");
		return questionsManager;
	}
	
	public SearchingManager getSearchingManager(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		SearchingManager searchingManager = (SearchingManager) getMediaArchive(catalogid).getBean("searchingManager");
		return searchingManager;
	}
	

	public void loadTutorials(WebPageRequest inReq) throws Exception 
	{
		Searcher tutorialsearcher = getMediaArchive(inReq).getSearcher("aitutorials");
		HitTracker hits = tutorialsearcher.query().exact("featured", true).search();
		
		inReq.putPageValue("tutorials", hits);
	}
	
	public void populateSection(WebPageRequest inReq) throws Exception 
	{
		SmartCreatorManager creatorManager = getSmartCreatorManager(inReq);
//		creatorManager.createCreatorAndPopulateSection(inReq);
	}
	
	
	public void loadCreator(WebPageRequest inReq) throws Exception 
	{
		SmartCreatorManager creatorManager = getSmartCreatorManager(inReq);
		creatorManager.getCreator(inReq);
	}
	
	public void createCreatorSection(WebPageRequest inReq) throws Exception 
	{
		
		SmartCreatorManager creatorManager = getSmartCreatorManager(inReq);
		String playbackentityid = inReq.getRequestParameter("playbackentityid");
		String playbackentitymoduleid = inReq.getRequestParameter("playbackentitymoduleid");
		
		Searcher searcher = getMediaArchive(inReq).getSearcher(playbackentitymoduleid);
		Data playbackentity = searcher.loadData(playbackentityid);
		
		if( playbackentity == null)
		{
			throw new IllegalArgumentException("No creator entity found for id: " + playbackentityid);
		}
		
		try
		{
			Map fields = new HashMap();
			String sectionid = inReq.getRequestParameter("sectionid");
			if( sectionid != null && sectionid.length() > 0)
			{
				fields.put("sectionid", sectionid);
				String name = inReq.getRequestParameter("name");
				fields.put("name", name);
			}
			else
			{
				String ordering = inReq.getRequestParameter("ordering");
				int orderingint = Integer.parseInt(ordering);
				fields.put("ordering", orderingint + 1);				
			}
			
			
			Data section = creatorManager.createCreatorSection(playbackentity, playbackentitymoduleid, fields);
			inReq.putPageValue("playbackentity", playbackentity);
			inReq.putPageValue("section", section);
		}
		catch( Exception ex)
		{
			throw new IllegalArgumentException("Ordering must be a number");
		}
	}
	
	public void deleteCreatorSection(WebPageRequest inReq) throws Exception 
	{
		String searchtype = inReq.getRequestParameter("searchtype");
		String dataid = inReq.getRequestParameter("id");
		
		SmartCreatorManager creatorManager = getSmartCreatorManager(inReq);
		creatorManager.deleteCreatorSection(searchtype, dataid);
	}
	
	public void createComponentContent(WebPageRequest inReq) throws Exception 
	{
		SmartCreatorManager creatorManager = getSmartCreatorManager(inReq);
		String sectionid = inReq.getRequestParameter("sectionid");
		
		Map componentfields = new HashMap();
		
		String[] fields = inReq.getRequestParameters("field");
		
		Collection<String> fieldlist = new ArrayList<String>();
		
		if( fields != null)
		{
			for (int i = 0; i < fields.length; i++)
			{
				String fieldname = fields[i];
				if( fieldname != null && fieldname.length() > 0)
				{
					fieldlist.add(fieldname);
				}
			}
		}
		
		if(!fieldlist.contains("content"))
		{
			fieldlist.add("content");
		}
		if(!fieldlist.contains("componenttype"))
		{
			fieldlist.add("componenttype");
		}
		if(!fieldlist.contains("ordering"))
		{
			fieldlist.add("ordering");
		}
		if(!fieldlist.contains("componentcontentid"))
		{
			fieldlist.add("componentcontentid");
		}
		
		for (Iterator iterator = fieldlist.iterator(); iterator.hasNext();) 
		{
			String fieldname = (String) iterator.next();
			
			String fieldvalue = inReq.getRequestParameter(fieldname+".value");
			if(fieldvalue == null)
			{
				fieldvalue = inReq.getRequestParameter(fieldname + "value");
			}
			if(fieldvalue == null)
			{
				fieldvalue = inReq.getRequestParameter(fieldname);
			}
			if(fieldvalue == null || fieldvalue.length() == 0)
			{
				continue;
			}
			componentfields.put(fieldname, fieldvalue);
		}
		
		Data componentcontent = creatorManager.createComponentContent(sectionid, componentfields);
		inReq.putPageValue("componentcontent", componentcontent);
		
		if(componentfields.get("componentcontent") == null)
		{
			inReq.putPageValue("newcontent", true);
		}
	}
	
	public void populateComponentContent(WebPageRequest inReq) throws Exception 
	{
		SmartCreatorManager creatorManager = getSmartCreatorManager(inReq);
//		creatorManager.autoPopulateComponentContent();
	}
	
	public void orderCreatorSection(WebPageRequest inReq)
	{
		SmartCreatorManager creatorManager = getSmartCreatorManager(inReq);
		creatorManager.orderCreatorSection(inReq);
	}
	
	public void duplicateCreatorSection(WebPageRequest inReq)
	{
		String searchtype = inReq.getRequestParameter("searchtype");
		String dataid = inReq.getRequestParameter("id");
		
		SmartCreatorManager creatorManager = getSmartCreatorManager(inReq);
		Data duplicate = creatorManager.duplicateCreatorSection(searchtype, dataid);
		
		inReq.putPageValue("searchtype", searchtype);

		if("componentcontent".equals(searchtype))
		{
			inReq.putPageValue("componentcontent", duplicate);
		}
		else if("componentsection".equals(searchtype))
		{			
			inReq.putPageValue("section", duplicate);
		}
	}

	
	
	public void creatorAiAction(WebPageRequest inReq)
	{
		String aiaction = inReq.getRequestParameter("aiaction");
		String componentcontentid = inReq.getRequestParameter("componentcontentid");
		
		SmartCreatorManager creatorManager = (SmartCreatorManager) getMediaArchive(inReq).getBean("smartCreatorManager");
		
		if("grammar".equals(aiaction))
		{
			creatorManager.correctGrammar(inReq, componentcontentid);
		}
		else if("improve".equals(aiaction))
		{
			creatorManager.improveContent(inReq, componentcontentid);
		}
		else if("generate".equals(aiaction))
		{
			String prompt = inReq.getRequestParameter("aiprompt");
			creatorManager.generateContent(inReq, componentcontentid, prompt);
		}
		else if("image".equals(aiaction))
		{
			String prompt = inReq.getRequestParameter("aiprompt");
			creatorManager.createImage(inReq, componentcontentid, prompt);
		}
		else if("caption".equals(aiaction))
		{
			String assetid = inReq.getRequestParameter("assetid");
			creatorManager.captionImage(inReq, componentcontentid, assetid);
		}
	}
	
	
}
