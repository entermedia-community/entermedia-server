package org.entermediadb.ai.document;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
import org.openedit.data.QueryBuilder;
import org.openedit.hittracker.HitTracker;

public class DocumentRagManager extends BaseAiManager {
	
	private static final Log log = LogFactory.getLog(DocumentRagManager.class);
	

	public void indexDocumentPages(ScriptLogger inLog) {
		Map<String, String> models = getModels();
		QueryBuilder query = getMediaArchive().getSearcher("modulesearch").query();
		query.exact("id", "entitydocumentpage");
		query.exact("semanticindexed", false);
		query.missing("semantictopics");
		
		HitTracker hits = query.search();
		hits.enableBulkOperations();
		
		if (hits.size() == 0)
		{
			inLog.info("No documents to index.");
			return;
		}
		
		LlmConnection llmconnection = getMediaArchive().getLlmConnection(models.get("ragmodel"));
		
		for(int i=0;i < hits.getTotalPages();i++)
		{
			hits.setPage(i+1);
			
			Collection<Data> docpages = hits.getPageOfHits();
			
			
			for (Iterator iterator = docpages.iterator(); iterator.hasNext();) {
				Data data = (Data) iterator.next();
				
			}
		}
	}
}
