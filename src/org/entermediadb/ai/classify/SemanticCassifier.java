package org.entermediadb.ai.classify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.informatics.InformaticsProcessor;
import org.entermediadb.ai.informatics.SemanticTableManager;
import org.entermediadb.ai.knn.RankedResult;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;

//TODO: Rename to SemanticCassifier
public class SemanticCassifier extends InformaticsProcessor implements CatalogEnabled
{
	protected SemanticTableManager fieldSemanticTableManager;
	protected String fieldConfigurationId;
	
	
	public String getConfigurationId()
	{
		return fieldConfigurationId;
	}

	public void setConfigurationId(String inConfigurationId)
	{
		fieldConfigurationId = inConfigurationId;
	}

	public SemanticTableManager getSemanticTableManager()
	{
		return loadSemanticTableManager("semantictopics");
		/*
		if (fieldSemanticTableManager == null)
		{
			fieldSemanticTableManager = (SemanticTableManager)getModuleManager().getBean(getCatalogId(),"semanticTableManager",false);
			if( getConfigurationId() == null)
			{
				throw new OpenEditException("Configuration id required");
			}
			fieldSemanticTableManager.setConfigurationId(getConfigurationId());
		}

		return fieldSemanticTableManager;*/
	}

	public void setSemanticTableManager(SemanticTableManager inSemanticTableManager)
	{
	
		fieldSemanticTableManager = inSemanticTableManager;
	}
	
	
	public SemanticTableManager loadSemanticTableManager(String inConfigId)
	{
		SemanticTableManager table = (SemanticTableManager)getMediaArchive().getCacheManager().get("semantictables",inConfigId);
		if( table == null)
		{
			table = (SemanticTableManager)getModuleManager().getBean(getCatalogId(),"semanticTableManager",false);
			table.setConfigurationId(inConfigId);
			getMediaArchive().getCacheManager().put("semantictables",inConfigId,table);
		}
		
		return table;
	}

	private static final Log log = LogFactory.getLog(SemanticCassifier.class);

	@Override
	public void processInformaticsOnAssets(ScriptLogger inLog, MultiValued inConfig, Collection<MultiValued> inAssets)
	{
		processInformaticsOnEntities(inLog,inConfig,inAssets);
	}

	@Override
	public void processInformaticsOnEntities(ScriptLogger inLog, MultiValued inConfig, Collection<MultiValued> inRecords)
	{
		String fieldname = inConfig.get("fieldname");
		
		Map<String, String> models = getModels();

		String model = models.get("semantic");
		
		LlmConnection llmsemanticconnection = getMediaArchive().getLlmConnection(model);

		long start = System.currentTimeMillis();
		
		inLog.info("SemanticFieldManager Start values and indexing " + fieldname);
		
		for (Iterator iterator = inRecords.iterator(); iterator.hasNext();)
		{
			MultiValued data = (MultiValued) iterator.next();
			String moduleid = data.get("entitysourcetype");
			if( moduleid == null)
			{
				throw new OpenEditException("Requires sourcetype be set "  + data);
			}
			MultiValued module = (MultiValued)getMediaArchive().getCachedData("module", moduleid);
			if( !module.getBoolean("semanticenabled") )
			{
				continue;
			}
			
			Collection existing = data.getValues(fieldname);
			if(existing != null && !existing.isEmpty())
			{
				continue;
			}
			Collection<String> newvalues = getSemanticTableManager().createSemanticValues(llmsemanticconnection,inConfig,model,moduleid,data);
			data.setValue(fieldname,newvalues);
		}
		if( getSemanticTableManager().isIndexingVectors() )
		{
			log.info("Skipping indexing vectors for now, event will handle it later");
			return;
		}
		getSemanticTableManager().setIndexingVectors(true);
		try
		{
			getSemanticTableManager().indexData(inLog,inRecords);
		}
		finally
		{
			getSemanticTableManager().setIndexingVectors(false);
		}
		long end = System.currentTimeMillis();
		double seconds = end - start / 1000d;
		inLog.info("SemanticFieldManager Completed " + inRecords.size() + " records in " +  seconds + " seconds ");
	}
	public Map<String,Collection<String>> search(Collection<String> textvalues, Collection<String> excludedEntityIds, Collection<String> excludedAssetids)
	{
		Map<String,Collection<String>> bytype = new HashMap();

		for (Iterator iterator = textvalues.iterator(); iterator.hasNext();)
		{
			String textsemantic = (String) iterator.next();
			
			JSONObject response = getSemanticTableManager().execMakeVector(textsemantic);
	
			JSONArray results = (JSONArray)response.get("results");
			Map hit = (Map)results.iterator().next();
			List vector = (List)hit.get("embedding");
			vector = getSemanticTableManager().collectDoubles(vector);
	
			searchForVector(vector, bytype, excludedEntityIds, excludedAssetids);
		}		
		
		return bytype;		
	}

	public Map<String,Collection<String>> search(String text, Collection<String> excludedEntityIds, Collection<String> excludedAssetids)
	{
		Collection<String> values = new ArrayList(1);
		values.add(text);
		return search(values, excludedEntityIds, excludedAssetids);
	}
	
	
	
	protected void searchForVector(List<Double> inVector, Map<String, Collection<String>> bytype, Collection<String> excludedEntityIds, Collection<String> excludedAssetids)
	{
		Collection<RankedResult> found = getSemanticTableManager().searchNearestItems(inVector);
		
		//List allIdsX = new ArrayList(); //for debugging

		for (Iterator iterator = found.iterator(); iterator.hasNext();)
		{
			RankedResult rankedResult = (RankedResult) iterator.next();
			
			//allIds.add(rankedResult.getModuleId() + ":" + rankedResult.getEntityId());
			
			if(rankedResult.getModuleId().equals("asset"))
			{
				if(excludedAssetids != null && excludedAssetids.contains(rankedResult.getEntityId()))
				{
					continue;
				}
			}
			else if(excludedEntityIds != null && excludedEntityIds.contains(rankedResult.getEntityId()))
			{
				continue;
			}
			
			Collection hits = bytype.get(rankedResult.getModuleId());
			if( hits == null)
			{
				hits = new ArrayList();
				bytype.put(rankedResult.getModuleId(),hits);
			}
			if( hits.size() < 1000)
			{
				hits.add(rankedResult.getEntityId());
			}
		}
		//log.info("Found matching IDs:" + allIds);

	}
	
	public void indexAll(ScriptLogger inLog)
	{
		getSemanticTableManager().indexAll(inLog);
	}
}
