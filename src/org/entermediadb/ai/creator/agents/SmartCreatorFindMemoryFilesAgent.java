package org.entermediadb.ai.creator.agents;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.assistant.AssistantManager;
import org.entermediadb.ai.assistant.SearchingManager;
import org.entermediadb.ai.creator.SmartCreatorManager;
import org.entermediadb.ai.llm.AgentContext;
import org.json.simple.JSONArray;
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
		// Set<String> parentIds = new HashSet();
		Set<String> finalparentIds = new HashSet<>();
		if (localparentIds != null)
		{
			finalparentIds.addAll(localparentIds);
		}
		Collection<String> searchcats = entity.getValues("searchcategory");
		if (searchcats != null && !searchcats.isEmpty())
		{
			HitTracker modules = getMediaArchive().query("module").exact("semanticenabled", true).cachedSearch();
			Collection<String> moduleids = modules.collectValues("id");
			HitTracker addedentites = getMediaArchive().query("modulesearch")
				.addFacet("entitysourcetype")
				.put("searchtypes", moduleids)
				.includeDescription(true)
				.orgroup("searchcategory", searchcats)
				.exact("entityembeddingstatus", "embedded")
				.search();
			for (Iterator iterator = addedentites.iterator(); iterator.hasNext();)
			{
				Data doc = (Data) iterator.next();
				String type = doc.get("entitysourcetype");
				String docid = type + "_" + doc.getId();
				if (!finalparentIds.contains(docid))
				{
					finalparentIds.add(docid);
				}

			}

		}

		if (finalparentIds.isEmpty())
		{
			inContext.error("Error state, No embeded Documents to Process, dont process more"); // Mark as error?
			return;
		}
		JSONArray array = new JSONArray();
		array.addAll(finalparentIds);
		inContext.getAiSmartCreatorSteps().setEmbeddedParentIds(array);
		super.process(inContext);

	}
}
