package org.entermediadb.ai.creator.agents;

import java.util.Collection;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.creator.SmartCreatorManager;
import org.entermediadb.ai.llm.AgentContext;

public class SearchCategorySummaryAgent extends BaseAgent
{
	@Override
	public void process(AgentContext inContext)
	{
		summarizeLatestContentInSearchCategories(inContext);
		super.process(inContext);
	}
	
	//Auto content creation
	public void summarizeLatestContentInSearchCategories(AgentContext inReq)
	{
		//search for ones with an output type
		//lastsummary_date
		SmartCreatorManager creatorManager = (SmartCreatorManager) getMediaArchive().getBean("smartCreatorManager");
		
		Collection categories = getMediaArchive().query("searchcategory").exact("lastsummary_enabled", true).search();
		creatorManager.createContentFromSearchCategories(categories);
		//What kind of content
		
	}


}
