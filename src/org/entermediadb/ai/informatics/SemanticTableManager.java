package org.entermediadb.ai.informatics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.knn.RankedResult;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.net.HttpSharedConnection;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.PropertyDetail;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;

public class SemanticTableManager extends BaseAiManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(SemanticTableManager.class);
	
	protected boolean fieldIndexingVectors;
	
	protected String fieldConfigurationId;
	
	public String getConfigurationId()
	{
		return fieldConfigurationId;
	}

	public void setConfigurationId(String inSemanticSettingId)
	{
		fieldConfigurationId = inSemanticSettingId;
	}

	public boolean isIndexingVectors()
	{
		return fieldIndexingVectors;
	}

	public void setIndexingVectors(boolean inWorking)
	{
		fieldIndexingVectors = inWorking;
	}
	protected String fieldCatalogId;

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}
	protected ModuleManager fieldModuleManager;
	

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}
	
	public void reBalance(ScriptLogger logger)
	{
//		if( true )
//		{
//		//	return;
//		}
		long start = System.currentTimeMillis();
		logger.info(new Date() +  " reinitNodes Start reinit ");
		
		SemanticConfig instruction = getSemanticInstructions();

		//Clear all centroids
		HitTracker tracker = getMediaArchive().query(instruction.getSearchType()).exact("iscentroid",true).search(); 
		tracker.enableBulkOperations();

		Collection tosave = new ArrayList(1000);
		for (Iterator iterator = tracker.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			data.setValue("iscentroid",false);
			tosave.add(data);
			if( tosave.size() == 1000)
			{
				getMediaArchive().saveData(instruction.getSearchType(),tosave);
				tosave.clear();
			}
		}
		getMediaArchive().saveData(instruction.getSearchType(),tosave);
		getMediaArchive().getCacheManager().clear(instruction.getFieldName());
		//getMediaArchive().getCacheManager().clear(instructions.getFieldName() + "lookuprecord"); 
		
		instruction.getKMeansIndexer().reinitClusters(logger);
		long end = System.currentTimeMillis();
		double seconds = (end - start)  / 1000d;
		logger.info(" reinitNodes Completed in  " + seconds  + " seconds ");

	}
	
	protected MediaArchive getMediaArchive()
	{
		return (MediaArchive)getModuleManager().getBean(getCatalogId(),"mediaArchive");
	}

	/**
	 * Not used
	 */
	public void index(ScriptLogger inLogger, MultiValued inEntity)
	{
		List one = new ArrayList();
		one.add(inEntity);
		
		SemanticConfig instruction = getSemanticInstructions();
		
		Collection<MultiValued> createdVectors = new ArrayList();
		index(instruction, one, createdVectors);
		
		instruction.getKMeansIndexer().setCentroids(inLogger, createdVectors);
	}

	public SemanticConfig getSemanticInstructions()
	{
		String sematicsettingsid = getConfigurationId();
		if( sematicsettingsid == null)
		{
			throw new OpenEditException("SemanticSettingId is required");
			//= "semantictopics";
		}
		
		SemanticConfig instructions = (SemanticConfig)getMediaArchive().getCacheManager().get("semantictopicsinstructions",sematicsettingsid);
		if( instructions == null)
		{
			instructions = (SemanticConfig)getModuleManager().getBean(getCatalogId(),"semanticConfig",false);
			getMediaArchive().getCacheManager().put("semantictopicsinstructions", getConfigurationId(),instructions);
			
			MultiValued settings = (MultiValued)getMediaArchive().getData("informatics",sematicsettingsid);
			if (settings == null)
			{
				throw new OpenEditException("Empty settings for " + sematicsettingsid);
			}
			instructions.setInstructionDetails(settings);
		}
		return instructions;
	}

	

	
	public void indexAll(ScriptLogger inLog)
	{
		//Check that we are not already scanning
		if( isIndexingVectors() )
		{
			inLog.error("Already Indexing");
			return;
		}
		setIndexingVectors(true);
		try
		{
			HitTracker all = getMediaArchive().query("module").exact("semanticenabled", true).search();
			if(all.isEmpty())
			{
				log.info("No modules enabled for semantics");
				return;
			}
			Collection<String> ids = all.collectValues("id");
			log.info("Scanning modules " + ids);
			SemanticConfig instruction = getSemanticInstructions();
			QueryBuilder query = getMediaArchive().query("modulesearch");
			query.exists(instruction.getFieldName());
			query.exact(instruction.getFieldName() + "indexed", false);
			query.put("searchtypes", ids);
			query.put("searchasset", true); //this is needed to include asset
			
			HitTracker hits = query.search();
			hits.enableBulkOperations();
			log.info("Indexing " + instruction.getFieldName() + " in: " + hits);
			indexTracker(inLog, instruction, hits);
		}
		finally
		{
			setIndexingVectors(false);
		}
	}
	public void indexData(ScriptLogger inLog, Collection<MultiValued> inRecords)
	{
		SemanticConfig instruction = getSemanticInstructions();
		indexData(inLog, instruction, inRecords);
	}
	
	protected void indexData(ScriptLogger inLog, SemanticConfig instruction, Collection<MultiValued> inRecords)
	{
		Collection<MultiValued> createdVectors = new ArrayList();
		int indexed = index(instruction, inRecords, createdVectors);
		instruction.getKMeansIndexer().setCentroids(inLog, createdVectors);
		
		for (Iterator iterator = inRecords.iterator(); iterator.hasNext();)
		{
			MultiValued data = (MultiValued) iterator.next();
			data.setValue(instruction.getFieldName()+ "indexed", true);
		}
		
		instruction.getKMeansIndexer().checkReinit();
		log.info("Indexed " + instruction.getFieldName() + " on: " + indexed + "/" + inRecords.size());

	}

	
	protected void indexTracker(ScriptLogger inLog, SemanticConfig instruction,  HitTracker hits)
	{
		for(int i=0;i < hits.getTotalPages();i++)
		{
			hits.setPage(i+1);
			//long start = System.currentTimeMillis();
			Collection<MultiValued> onepage = hits.getPageOfHits();
			indexData(inLog, instruction, onepage);
		}
	}
	
	
	public void clusterInit(ScriptLogger log)
	{
		SemanticConfig instruction = getSemanticInstructions();
		instruction.getKMeansIndexer().reinitClusters(log);

	}
	
	public int index(SemanticConfig inStructions, Collection<MultiValued> inEntities, Collection<MultiValued> createdVectors)  //Page of data
	{
		String url = getMediaArchive().getCatalogSettingValue("ai_vectorizer_server");
		if( url == null)
		{
			log.error("No face server configured");
			return 0;
		}
		
		log.info("Indexing: " + inStructions);

		HitTracker existingvectors = getMediaArchive().query(inStructions.getSearchType() ).orgroup("dataid", inEntities).search();
		existingvectors.enableBulkOperations();
		
		Set<String> dataIds = (Set<String>)existingvectors.collectValues("dataid");
		inStructions.setExistingEntityIds(dataIds);
		
		Map<String,List> entitiestoprocess = new HashMap();
		
		for (Iterator iterator = inEntities.iterator(); iterator.hasNext();)
		{
			MultiValued entity = (MultiValued) iterator.next();

			String moduleid = entity.get("entitysourcetype");
			if( moduleid == null) 
			{ 
				moduleid = "asset"; 
			}; 
			
			List bytype = entitiestoprocess.get(moduleid);
			if( bytype == null)
			{
				bytype = new ArrayList();
				entitiestoprocess.put(moduleid,bytype);
			}
			bytype.add(entity);
			entity.setValue(inStructions.getFieldName() + "indexed", true);
		}  
		
		int count = 0;
		try
		{
			for (Iterator iterator = entitiestoprocess.keySet().iterator(); iterator.hasNext();)
			{
				String moduleid = (String) iterator.next();
				List<MultiValued> tosave = entitiestoprocess.get(moduleid);
				Collection<MultiValued> foundsemanticstosave = extractVectors(inStructions, moduleid, tosave);
				count = count + foundsemanticstosave.size();
				getMediaArchive().saveData(inStructions.getSearchType(),foundsemanticstosave);
				getMediaArchive().saveData(moduleid,tosave);
				log.info(" Saved datas " + tosave.size() + " added:  " + foundsemanticstosave.size());
				if (createdVectors != null)
				{
					createdVectors.addAll(foundsemanticstosave);
				}
			}
			
		}
		catch( Throwable ex)
		{
			throw new OpenEditException("Error reading semantics", ex); //Should never error
		}
		
		return count;
	}

	protected Collection<MultiValued> extractVectors(SemanticConfig inStructions, String inModuleId, List<MultiValued> entitiestoscan) throws Exception
	{
		
		Collection toscan = new ArrayList(entitiestoscan.size());
		
		if( inStructions.isSkipExistingRecords() ) //This is the default, but when rescanning one asset dont skip
		{
			for (Iterator iterator = entitiestoscan.iterator(); iterator.hasNext();)
			{
				MultiValued entity = (MultiValued) iterator.next();
				String dataid = entity.getId();
				if( inStructions.getExistingEntityIds().contains(dataid ) )
				{
					log.error("Skipping, Already have dataid " + dataid);
					continue;
				}	
				if( entity.getValue(inStructions.getFieldName()) == null)
				{
					continue;
				}
				toscan.add(entity);
			}
		}
		else
		{
			toscan = entitiestoscan;
		}
		//long start = System.currentTimeMillis();
		//log.debug("Facial Profile Detection sending " + inAsset.getName() );
		
		JSONArray list = new JSONArray();
		for (Iterator iterator = entitiestoscan.iterator(); iterator.hasNext();)
		{
			MultiValued entity = (MultiValued) iterator.next();
			Collection values = entity.getValues(inStructions.getFieldName());
			if( values == null || values.isEmpty())
			{
				continue;
			}
			int i = 0;
			for (Iterator iterator2 = values.iterator(); iterator2.hasNext();)
			{
				String topictext = (String) iterator2.next();
				JSONObject entry = new JSONObject();
				String moduleid = entity.get("entitysourcetype");
				if( moduleid == null)
				{
					moduleid = "asset";
				}
				entry.put("id",moduleid + ":" + entity.getId() + ":" + i);  //More unique
				entry.put("text",topictext);
				list.add(entry);
				i++;
			}
		}
		if( list.size() == 0)
		{
			return new ArrayList();
		}
		JSONObject tosendparams = new JSONObject();
		tosendparams.put("data",list);
		
		JSONObject jsonresponse = askServer(tosendparams);
		JSONArray results = (JSONArray)jsonresponse.get("results");
		
		Searcher searcher = getMediaArchive().getSearcher(inStructions.getSearchType());
		
		Collection<MultiValued> newrecords = new ArrayList(results.size());
		for (int i = 0; i < results.size(); i++)
		{
			Map result = (Map)results.get(i);
			MultiValued newdata = (MultiValued)searcher.createNewData();
			String uid = (String)result.get("id");
			newdata.setId(uid); //Avoid duplicates
			String[] parts = uid.split(":");
			newdata.setValue("moduleid",parts[0]);
			newdata.setValue("dataid",parts[1]);
			Collection vectors = inStructions.getKMeansIndexer().collectDoubles((Collection)result.get("embedding"));
			newdata.setValue(inStructions.getKMeansIndexer().getFieldSaveVector(),vectors);
			newrecords.add(newdata);
		}
		return newrecords;
		

	}

	protected JSONObject askServer(JSONObject tosendparams)
	{
		LlmConnection connection = getMediaArchive().getLlmConnection("vectorizeText");
		LlmResponse resp = connection.callJson("/text", null, tosendparams);
		return resp.getRawResponse();
	}
	
	protected HttpSharedConnection fieldSharedConnection;

	protected HttpSharedConnection getSharedConnection()
	{
		if (fieldSharedConnection == null)
		{
			HttpSharedConnection connection = new HttpSharedConnection();
			//connection.addSharedHeader("x-api-key", api);
			fieldSharedConnection = connection;
		}

		return fieldSharedConnection;
	}

	public void setSharedConnection(HttpSharedConnection inSharedConnection)
	{
		fieldSharedConnection = inSharedConnection;
	}


	public JSONObject execMakeVector(Collection<String> texts)
	{
		JSONObject tosendparams = new JSONObject();
		JSONArray list = new JSONArray();
		int count = 0;
		for (Iterator iterator = texts.iterator(); iterator.hasNext();)
		{
			String text = (String) iterator.next();
			JSONObject ask = new JSONObject();
			ask.put("id",String.valueOf(count++));
			ask.put("text",text);
			list.add(ask);
		}
		if( list.size() == 0)
		{
			return new JSONObject();
		}
		tosendparams.put("data",list);

		JSONObject jsonresponse = askServer(tosendparams);
		
		//log.info("Got response " + objt.keySet());
		return jsonresponse;
	}

	public List<Double> makeVector(String text)
	{
		JSONObject response = execMakeVector(text);
		JSONArray results = (JSONArray)response.get("results");
		Map hit = (Map)results.iterator().next();
		List<Double> vector = (List)hit.get("embedding");
		vector = collectDoubles(vector);
		return vector;
	}

	public JSONObject execMakeVector(String text)
	{
		JSONObject tosendparams = new JSONObject();
		JSONArray list = new JSONArray();
		JSONObject ask = new JSONObject();
		ask.put("id","search");
		ask.put("text",text);
		list.add(ask);
		tosendparams.put("data",list);
		JSONObject objt = askServer(tosendparams);
		//log.info("Got response " + objt.keySet());
		return objt;
	}


	//Move to vector util package
	public List<Double> collectDoubles(Collection vector) 
		{
			List<Double> floats = new ArrayList(vector.size());
			for (Iterator iterator = vector.iterator(); iterator.hasNext();)
			{
				Object floatobj = iterator.next();
				double f;
				if( floatobj instanceof Double)
				{
					f = (Double)floatobj;
				}
				else if( floatobj instanceof Float)
				{
					f = (Double)floatobj;
				}
				else
				{
					f = Double.parseDouble(floatobj.toString());
				}
				floats.add(f);
			}
			return floats;
		}
	
		public Collection<String> createSemanticValues(LlmConnection llmconnection, MultiValued inConfig, String inModuleId, MultiValued inData)
		{

			String fieldname = inConfig.get("fieldname"); 
			
			Collection existing = inData.getValues(fieldname);
			if(existing != null && !existing.isEmpty())
			{
				log.info("No data found"); //Should not happen
				return null;
			}
			
			Map params = new HashMap();
			params.put("fieldparams", inConfig);
			
			Collection<PropertyDetail> exclude = new ArrayList();
			PropertyDetail fielddetail = getMediaArchive().getSearcher(inModuleId).getDetail(fieldname);
			exclude.add(fielddetail);
		
			Collection<PropertyDetail> contextfields = populateFields(inModuleId, inData, exclude);
			
			if(contextfields.isEmpty())
			{
				log.info("No fields to check for semantic topics in " + inData.getId() + " " + inData.getName());
				return null;
			}
			
			params.put("contextfields", contextfields);
			params.put("data", inData);
			
			LlmResponse structure = llmconnection.callStructuredOutputList(params,"createSemanticTopics");
			if (structure == null)
			{
				log.info("No structured data returned");
				return null;
			}
			JSONObject content = structure.getMessageStructured();
			JSONArray jsonvalues = (JSONArray) content.get(fieldname);
			Collection<String> values = new ArrayList();
			//replace underscore with spaces
			if (jsonvalues != null)
			{
				for (Iterator iterator = jsonvalues.iterator(); iterator.hasNext();) {
					Object topicobj = iterator.next();  
					if(topicobj instanceof String)
					{
						String topic = (String) topicobj;
						topic = topic.replaceAll("_", " ").trim();
						if( topic.length() > 0)
						{
							values.add(topic);
						}
					}
				}
			}
			return values;
		}

		public Collection<RankedResult> searchNearestItems(List<Double> inVector)
		{
			Collection<RankedResult> results = getSemanticInstructions().getKMeansIndexer().searchNearestItems(inVector);
			return results;
		}
		
		public void reinitClusters(ScriptLogger inLog) 
		{
			getSemanticInstructions().getKMeansIndexer().reinitClusters(inLog);
		}

}

