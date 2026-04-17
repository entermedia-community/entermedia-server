package org.entermediadb.tasks;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.automation.agents.ToolsCallingAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.json.simple.JSONArray;
import org.openedit.Data;
import org.openedit.hittracker.HitTracker;

public class EmbeddingFinderAgent extends ToolsCallingAgent
{
	@Override
	public void process(AgentContext inContext)
	{
		Collection<String> categories =
			getMediaArchive().query("searchcategory").all().cachedSearch().collectValues("id");

		// TODO: reduce this list by Passing all categories with their semantic topics and tools
		// pick the categories

		if (categories != null && !categories.isEmpty())
		{
			Set<String> finalParentIds = new HashSet<>();

			HitTracker modules =
				getMediaArchive().query("module").exact("semanticenabled" , true).cachedSearch();
			Collection<String> moduleids = modules.collectValues("id");
			HitTracker addedentites = getMediaArchive().query("modulesearch")
				.addFacet("entitysourcetype")
				.put("searchtypes" , moduleids)
				.includeDescription(true)
				.orgroup("searchcategory" , categories)
				.exact("entityembeddingstatus" , "embedded")
				.search();
			for (Iterator iterator = addedentites.iterator(); iterator.hasNext();)
			{
				Data doc = (Data) iterator.next();
				String type = doc.get("entitysourcetype");
				String docid = type + "_" + doc.getId();
				if (!finalParentIds.contains(docid))
				{
					finalParentIds.add(docid);
				}
			}

			if (finalParentIds.isEmpty())
			{
				inContext.error("Unable to find embedded documents to find answer");
			}
			else
			{
				JSONArray embeddings = new JSONArray();
				embeddings.addAll(finalParentIds);
				inContext.addContext("embeddings" , embeddings);
			}

			super.process(inContext);

		}
	}
}
