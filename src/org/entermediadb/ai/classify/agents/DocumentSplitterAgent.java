package org.entermediadb.ai.classify.agents;

import java.util.Collection;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.classify.DocumentSplitterManager;
import org.entermediadb.ai.informatics.InformaticsContext;
import org.entermediadb.ai.llm.AgentContext;
import org.openedit.MultiValued;

public class DocumentSplitterAgent extends BaseAgent
{
	public DocumentSplitterManager getDocumentSplitterManager()
	{
		DocumentSplitterManager manager = (DocumentSplitterManager)getMediaArchive().getBean("documentSplitterManager");
		return manager;
	}

	public void process(AgentContext inContext)	
	{
		InformaticsContext informatic = new InformaticsContext(inContext);
		Collection<MultiValued>  inRecords  = informatic.getRecordsToProcess();
		
		if(inRecords == null)
		{
			super.process(informatic);
			return;
		}
		getDocumentSplitterManager().splitStuff(informatic );
		super.process(informatic);
	}
}
