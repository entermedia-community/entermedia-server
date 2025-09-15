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
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.infomatics.InformaticsInstructions;
import org.entermediadb.ai.knn.RankedResult;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.net.HttpSharedConnection;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;

public class SemanticFieldsManager extends BaseAiManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(SemanticFieldsManager.class);
	
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
		
		for (Iterator iteratorS = getInstructions().iterator(); iteratorS.hasNext();)
		{
			InformaticsInstructions instruction = (InformaticsInstructions) iteratorS.next();

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

	}
	
	protected MediaArchive getMediaArchive()
	{
		return (MediaArchive)getModuleManager().getBean(getCatalogId(),"mediaArchive");
	}

	public void index(MultiValued inData)
	{
		List one = new ArrayList();
		one.add(inData);
		
		for (Iterator iteratorS = getInstructions().iterator(); iteratorS.hasNext();)
		{
			InformaticsInstructions instruction = (InformaticsInstructions) iteratorS.next();
			index(instruction, one, null);
			instruction.getKMeansIndexer().setCentroids(inData);
		}
	}

	public InformaticsInstructions createSemanticInstructions(MultiValued inField)
	{
		InformaticsInstructions instructions = (InformaticsInstructions)getModuleManager().getBean(getCatalogId(),"semanticInstructions",false);
		instructions.setInstructionDetails(inField);
		return instructions;
	}
	
	public void indexAll(ScriptLogger inLog)
	{
		
		HitTracker all = getMediaArchive().query("module").exact("semanticenabled", true).search();
		Collection<String> ids = all.collectValues("id");
		log.info("Scanning modules " + ids);
		for (Iterator iteratorS = getInstructions().iterator(); iteratorS.hasNext();)
		{
			SemanticInstructions instruction = (SemanticInstructions) iteratorS.next();
			QueryBuilder query = getMediaArchive().localQuery("modulesearch");
			query.exists(instruction.getFieldName());
			query.exact(instruction.getFieldName() + "indexed", false);
			query.put("searchtypes", ids);
			query.put("searchasset", true); //this is needed to include asset
			
			HitTracker hits = query.search();
			hits.enableBulkOperations();
			log.info("Indexing " + instruction.getFieldName() + " in: " + hits);
			indexResults(inLog, instruction, hits);
		}
		
	}

	protected void indexResults(ScriptLogger inLog, InformaticsInstructions instruction,  HitTracker hits)
	{
		int indexed = 0;
		Collection<MultiValued> createdVectors = null;
		for(int i=0;i < hits.getTotalPages();i++)
		{
			hits.setPage(i+1);
			//long start = System.currentTimeMillis();
			Collection<MultiValued> onepage = hits.getPageOfHits();
			indexed = indexed + index(instruction, onepage, createdVectors);
			if (createdVectors!= null && createdVectors.size() > 5000)
			{
				instruction.getKMeansIndexer().setCentroids(inLog, createdVectors);
				createdVectors.clear();
			}
		}
		inLog.info("Total indexed: " + indexed + "  from " + hits);
		if (createdVectors!= null)
		{
			instruction.getKMeansIndexer().setCentroids(inLog, createdVectors);
		}
	}
	
	
	public void clusterInit(ScriptLogger log)
	{
		for (Iterator iteratorS = getInstructions().iterator(); iteratorS.hasNext();)
		{
			InformaticsInstructions instruction = (InformaticsInstructions) iteratorS.next();
			instruction.getKMeansIndexer().reinitClusters(log);
		}

	}
	
	public int index(InformaticsInstructions inStructions, Collection<MultiValued> inEntities, Collection<MultiValued> createdVectors)  //Page of data
	{
		String url = getMediaArchive().getCatalogSettingValue("ai_vectorizer_server");
		if( url == null)
		{
			log.error("No face server configured");
			return 0;
		}

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
			throw new OpenEditException("Error reading semantics" ,ex); //Should never error
		}
		
		return count;
	}

	protected Collection<MultiValued> extractVectors(InformaticsInstructions inStructions, String inModuleId, List<MultiValued> entitiestoscan) throws Exception
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
			
			JSONObject entry = new JSONObject();
			String moduleid = entity.get("entitysourcetype");
			if( moduleid == null)
			{
				moduleid = "asset";
			}
			entry.put("id",moduleid + ":" + entity.getId());
			Collection values = entity.getValues(inStructions.getFieldName());
			
			String out = collectText(values);
			if (out == null)
			{
				continue;
			}
			entry.put("text",out);
			list.add(entry);
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
			String dataid = (String)result.get("id");
			newdata.setId(dataid); //Avoid duplicates
			String[] parts = dataid.split(":");
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
		if (resp.getStatusLine().getStatusCode() != 200)
		{
			//remote server error, may be a broken image
			getSharedConnection().release(resp);
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
	
	public Collection<InformaticsInstructions> getInstructions()
	{
		Collection<InformaticsInstructions> instructions = (Collection<InformaticsInstructions>)getMediaArchive().getCacheManager().get("ai","semanticinstructions");
		if( instructions == null)
		{
			instructions = new ArrayList();
			
			Collection configs = getMediaArchive().query("semanticfield").exact("enabled", true).search();
			for (Iterator iterator = configs.iterator(); iterator.hasNext();)
			{
				MultiValued data = (MultiValued) iterator.next();
				InformaticsInstructions newins = createSemanticInstructions(data);
				instructions.add(newins);
			}
			getMediaArchive().getCacheManager().put("ai","semanticinstructions",instructions);
		}
		
		return instructions;
	}


	public Map<String,Collection<String>> searchAllSemanticFields(String text, Collection<String> excludedEntityIds, Collection<String> excludedAssetids)
	{
		//Collection allthepeopleinasset = getKMeansIndexer().searchNearestItems(startdata);
		JSONObject response = execMakeVector(text);

		JSONArray results = (JSONArray)response.get("results");
		Map hit = (Map)results.iterator().next();
		List vector = (List)hit.get("embedding");
		vector = collectDoubles(vector);

		List allIds = new ArrayList(); //for debugging
		Map<String,Collection<String>> bytype = new HashMap();

		for (Iterator iteratorS = getInstructions().iterator(); iteratorS.hasNext();)
		{
			InformaticsInstructions instruction = (InformaticsInstructions) iteratorS.next();
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
		}
		log.info("All matching IDs:" + allIds);

		//Search for them hits up to 1000
		//getMediaArchive().getSearcherManager().organizeHits(
		
		return bytype;		
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
		JSONObject objt;
		try
		{
			objt = (JSONObject)new JSONParser().parse(responseStr);
		}
		catch (ParseException e)
		{
			throw new OpenEditException(e);
		}
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
	
}

