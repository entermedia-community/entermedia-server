package org.entermediadb.ai.document;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.classify.ClassifyManager;
import org.entermediadb.manager.BaseManager;

public class DocumentRagManager extends BaseManager {
	
	private static final Log log = LogFactory.getLog(DocumentRagManager.class);
	
	public Map<String, String> getModels()
	{
		Map<String, String> models = new HashMap<>();
		String visionmodel = getMediaArchive().getCatalogSettingValue("llmvisionmodel");
		if(visionmodel == null) {
			visionmodel = "gpt-5-nano";
		}
		models.put("vision", visionmodel);

		String semanticmodel = getMediaArchive().getCatalogSettingValue("llmsemanticmodel");
		if(semanticmodel == null) {
			semanticmodel = "gpt-4o-mini";
		}
		models.put("semantic", semanticmodel);
		
		return models;
	}
	public void indexDocumentPages(String inAssetId) {

	}
}
