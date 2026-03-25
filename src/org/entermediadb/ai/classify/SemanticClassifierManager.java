package org.entermediadb.ai.classify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.knn.RankedResult;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;

public class SemanticClassifierManager extends BaseAiManager
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
		return loadSemanticTableManager(getConfigurationId()); //"semantictopics"
	}
	private static final Log log = LogFactory.getLog(SemanticClassifierManager.class);


	public void processRecords(ScriptLogger inLog, MultiValued inConfig,Collection<? extends MultiValued>  inRecords )
	{
		String fieldname = inConfig.get("fieldname");
		
		inLog.headline("Semantic Classifying " + inRecords.size() + " records");

		setConfigurationId(fieldname); //TODO: Read from config
		
		LlmConnection llmsemanticconnection = getMediaArchive().getLlmConnection("createSemanticTopics");

		long start = System.currentTimeMillis();
		
		for (Iterator iterator = inRecords.iterator(); iterator.hasNext();)
		{
			MultiValued data = (MultiValued) iterator.next();
			String moduleid = data.get("entitysourcetype");
			if( moduleid == null)
			{
				//throw new OpenEditException("Requires sourcetype be set "  + data);

				//Todo: Should we skip data with no sourcetype earlier?
				log.info(data +" doesn't contain a sourcetype");
				continue;
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
			
			try
			{
				Collection<String> newvalues = getSemanticTableManager().createSemanticValues(llmsemanticconnection, inConfig, moduleid,data);
				data.setValue(fieldname,newvalues);
			}
			catch( Throwable ex)
			{
				log.error("Could not process " + moduleid + " -> " + data, ex);
				data.setValue("llmerror", true);
			}
		}
		try
		{
			getSemanticTableManager().indexData(inLog,inRecords);
			//getSemanticTableManager().process(informatic);
		}
		catch( Throwable ex)
		{
			log.error("Could not process vectors. Stopping everything ", ex);
			if( ex instanceof RuntimeException)
			{
				throw ex;
			}
			throw new OpenEditException(ex);
		}
		long end = System.currentTimeMillis();
		double seconds = end - start / 1000d;
		inLog.info("SemanticClassifierManager Completed " + inRecords.size() + " records in " +  seconds + " seconds ");
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
	
	public Data searchOne(String textvalue, String inModuleId)
	{
		JSONObject response = getSemanticTableManager().execMakeVector(textvalue);
	
		JSONArray results = (JSONArray)response.get("results");
		
		Map hit = (Map)results.iterator().next();
		
		List vector = (List)hit.get("embedding");
		
		vector = getSemanticTableManager().collectDoubles(vector);
		
		Collection<RankedResult> found = getSemanticTableManager().searchNearestItems(vector);
		
		for (Iterator iterator = found.iterator(); iterator.hasNext();)
		{
			RankedResult rankedResult = (RankedResult) iterator.next();
			
			if(inModuleId.equals(rankedResult.getModuleId()))
			{
				Data data = getMediaArchive().getData(rankedResult.getModuleId(), rankedResult.getEntityId());
				return data;
			}
		}
		return null; //not found
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

}
