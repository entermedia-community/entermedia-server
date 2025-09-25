package org.entermediadb.ai.classify;

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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.entermediadb.ai.informatics.InformaticsProcessor;
import org.entermediadb.ai.knn.RankedResult;
import org.entermediadb.ai.llm.LlmConnection;
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
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.util.JSONParser;

public class SemanticFieldManager extends InformaticsProcessor implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(SemanticFieldManager.class);
	
	protected boolean fieldIndexingVectors;
	
	protected String fieldSemanticSettingId = "semantictopics";
	
	public String getSemanticSettingId()
	{
		return fieldSemanticSettingId;
	}

	public void setSemanticSettingId(String inSemanticSettingId)
	{
		fieldSemanticSettingId = inSemanticSettingId;
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


	public void index(MultiValued inData)
	{
		List one = new ArrayList();
		one.add(inData);
		
		SemanticConfig instruction = getSemanticInstructions();
		index(instruction, one, null);
		instruction.getKMeansIndexer().setCentroids(inData);
	}

	public SemanticConfig getSemanticInstructions()
	{
		String sematicsettingsid = getSemanticSettingId();
		SemanticConfig instructions = (SemanticConfig)getMediaArchive().getCacheManager().get("semantictopicsinstructions",sematicsettingsid);
		if( instructions == null)
		{
			instructions = (SemanticConfig)getModuleManager().getBean(getCatalogId(),"semanticConfig",false);
			getMediaArchive().getCacheManager().put("semantictopicsinstructions", getSemanticSettingId(),instructions);
			
			MultiValued settings = (MultiValued)getMediaArchive().getData("informatics",sematicsettingsid);
			if (settings == null)
			{
				log.info("Emppty settings for " + sematicsettingsid);
			}
			instructions.setInstructionDetails(settings);
		}
		return instructions;
	}
	
	public Map<String,Collection<String>> search(String text, Collection<String> excludedEntityIds, Collection<String> excludedAssetids)
	{
		JSONObject response = execMakeVector(text);

		JSONArray results = (JSONArray)response.get("results");
		Map hit = (Map)results.iterator().next();
		List vector = (List)hit.get("embedding");
		vector = collectDoubles(vector);

		List allIds = new ArrayList(); //for debugging
		Map<String,Collection<String>> bytype = new HashMap();

		SemanticConfig instruction = getSemanticInstructions();
		Collection<RankedResult> found = instruction.getKMeansIndexer().searchNearestItems(vector);
		
		for (Iterator iterator = found.iterator(); iterator.hasNext();)
		{
			RankedResult rankedResult = (RankedResult) iterator.next();
			
			allIds.add(rankedResult.getModuleId() + ":" + rankedResult.getEntityId());
			
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
		log.info("All matching IDs:" + allIds);

		//Search for them hits up to 1000
		//getMediaArchive().getSearcherManager().organizeHits(
		
		return bytype;		
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
		
		JSONObject tosendparams = new JSONObject();
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
		tosendparams.put("data",list);
		
		CloseableHttpResponse resp = askServer(tosendparams);
		
		String responseStr = getSharedConnection().parseText(resp);
		JSONParser parser = new JSONParser();
		JSONObject jsonresponse = (JSONObject)parser.parse(responseStr);
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

	protected CloseableHttpResponse askServer(JSONObject tosendparams)
	{
		CloseableHttpResponse resp;
		String url = getMediaArchive().getCatalogSettingValue("ai_vectorizer_server");
		resp = getSharedConnection().sharedPostWithJson(url + "/text",tosendparams);
		int statuscode = resp.getStatusLine().getStatusCode();
		if (statuscode != 200)
		{
			//remote server error, may be a broken image
			getSharedConnection().release(resp);
			log.error(resp.toString());
			throw new OpenEditException("Server not working" + resp.getStatusLine());
		}
		return resp;
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




	protected JSONObject execMakeVector(String text)
	{
		JSONObject tosendparams = new JSONObject();
		JSONArray list = new JSONArray();
		JSONObject ask = new JSONObject();
		ask.put("id","search");
		ask.put("text",text);
		list.add(ask);
		tosendparams.put("data",list);
		CloseableHttpResponse resp = askServer(tosendparams);
		String responseStr = getSharedConnection().parseText(resp);
		JSONObject objt = (JSONObject) new JSONParser().parse(responseStr);
		log.info("Got response " + objt.keySet());
		return objt;
	}


	//Move to vector util package
		private List<Double> collectDoubles(Collection vector) 
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
	
		public Collection<String> createSemanticValues(LlmConnection llmconnection, MultiValued inConfig, String inModel, String inModuleId, MultiValued inData)
		{
//			MediaArchive archive = getMediaArchive();

			Map<String,Map> contextfields = populateFields(inModuleId, inData);
			if(contextfields.isEmpty())
			{
				log.info("No fields to check for semantic topics in " + inData.getId() + " " + inData.getName());
				return null;
			}

			String fieldname = inConfig.get("fieldname"); 
			
			Collection existing = inData.getValues(fieldname);
			if(existing != null && !existing.isEmpty())
			{
				log.info("No data found"); //Should not happen
				return null;
			}
			
			Map params = new HashMap();
			params.put("fieldparams", inConfig);
		
			Map validcontext = new HashMap(contextfields);
			validcontext.remove(fieldname);
			
			Collection<Map> context = validcontext.values();
			
			params.put("contextfields", context);
			
			JSONObject structure = llmconnection.callStructuredOutputList("semantics", inModel, params);
			if (structure == null)
			{
				log.info("No structured data returned");
				return null;
			}

			JSONArray jsonvalues = (JSONArray) structure.get(fieldname);
			Collection<String> values = new ArrayList();
			//replace underscore with spaces
			for (Iterator iterator = jsonvalues.iterator(); iterator.hasNext();) {
				String val = (String) iterator.next();
				if(val != null)
				{
					val = val.replaceAll("_", " ").trim();
					if( val.length() > 0)
					{
						values.add(val);
					}
				}
			}
			return values;
		}

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
				Collection<String> newvalues = createSemanticValues(llmsemanticconnection,inConfig,model,moduleid,data);
				data.setValue(fieldname,newvalues);
			}
			if( isIndexingVectors() )
			{
				log.info("Skipping indexing vectors for now, event will handle it later");
				return;
			}
			setIndexingVectors(true);
			try
			{
				indexData(inLog,inRecords);
			}
			finally
			{
				setIndexingVectors(false);
			}

		}
		

}

