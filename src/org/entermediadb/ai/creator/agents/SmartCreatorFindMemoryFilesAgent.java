package org.entermediadb.ai.creator.agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.assistant.AssistantManager;
import org.entermediadb.ai.assistant.SearchingManager;
import org.entermediadb.ai.creator.SmartCreatorManager;
import org.entermediadb.ai.llm.AgentContext;
import org.openedit.Data;
import org.openedit.hittracker.HitTracker;

public class SmartCreatorFindMemoryFilesAgent extends BaseAgent
{

	public SearchingManager getSearchingManager()
	{
		SearchingManager searchingManager = (SearchingManager) getMediaArchive().getBean("searchingManager");
		return searchingManager;
	}
	public SmartCreatorManager getSmartCreatorManager()
	{
		SmartCreatorManager smartCreatorManager = (SmartCreatorManager) getMediaArchive().getBean("smartCreatorManager");
		return smartCreatorManager;
	}
	@Override
	public void process(AgentContext inContext)
	{
		Data module = inContext.getCurrentEntityModule();
		Data entity = inContext.getCurrentEntity();

		AssistantManager assistant = (AssistantManager) getMediaArchive().getBean("assistantManager");
		Collection<String> localparentIds = assistant.findDocIdsForEntity(module.getId(), entity.getId());
		Set<String> parentIds = new HashSet();
		if( localparentIds != null)
		{
			parentIds.addAll(localparentIds);
		}
		Collection<String> searchcats = entity.getValues("searchcategory");
		if( searchcats != null && !searchcats.isEmpty())
		{
			HitTracker modules = getMediaArchive().query("module").exact("semanticenabled", true).cachedSearch();
			Collection<String> moduleids = modules.collectValues("id");
			HitTracker addedentites = getMediaArchive().query("modulesearch")
					.addFacet("entitysourcetype")
					.put("searchtypes", moduleids).includeDescription(true)
					.orgroup("searchcategory",searchcats)
					.exact("entityembeddingstatus", "embedded")
					.search();
	
			Collection moreids = addedentites.collectValues("id");
			parentIds.addAll(moreids);
		}

		if( parentIds.isEmpty())
		{
			inContext.error("Error state, dont process more"); //Mark as error?
			return;
		}
		Collection<String> finalparentIds = new ArrayList();
		for (Iterator iterator = parentIds.iterator(); iterator.hasNext();)
		{
			String parentid = (String) iterator.next();
			finalparentIds.add(parentid);
		}
		inContext.getAiSmartCreatorSteps().setEmbeddedParentIds(finalparentIds);
		super.process(inContext);
		
	}
}
