package org.entermediadb.asset.scanner;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.asset.MediaArchive;

public class HotFolderScanningAgent extends BaseAgent
{
	
	@Override
	public void process(AgentContext inContext)
	{
		MediaArchive archive = getMediaArchive();
		archive.getAssetManager().scanSources(inContext.getScriptLogger()); //TODO: Pull into Agents each source. 
		//Keep a list of Agents not sources
		super.process(inContext);
	}

}
