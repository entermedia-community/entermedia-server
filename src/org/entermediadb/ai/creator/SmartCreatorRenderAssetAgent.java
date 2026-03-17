package org.entermediadb.ai.creator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.informatics.InformaticsContext;
import org.entermediadb.ai.llm.AgentContext;
import org.openedit.Data;
import org.openedit.MultiValued;

public class SmartCreatorRenderAssetAgent extends BaseAgent
{
	public SmartCreatorManager getSmartCreatorManager()
	{
		SmartCreatorManager manager = (SmartCreatorManager)getMediaArchive().getBean("smartCreatorManager");
		return manager;
	}
	
	/**
	 * Calls render to html
	 * Attaches and asset version
	 * sends it to a blog?
	 */
	@Override
	public void process(AgentContext inContext)
	{
		MultiValued entity = inContext.getCurrentEntity();
		
		if(entity != null)
		{
			//render html and save to asset
			MultiValued entitymodule = inContext.getCurrentEntity();
			String html = getSmartCreatorManager().renderToHtml(inContext,entitymodule,entity);
			getSmartCreatorManager().exportAsAsset(inContext, entitymodule, entity, html);
		}
		super.process(inContext);
	}
	
}
