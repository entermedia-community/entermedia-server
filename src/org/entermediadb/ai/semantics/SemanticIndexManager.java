package org.entermediadb.ai.semantics;

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
import org.entermediadb.ai.KMeansIndexer;
import org.entermediadb.ai.RankedResult;
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

public class SemanticIndexManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(SemanticIndexManager.class);
	
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
	protected KMeansIndexer fieldKMeansIndexer;
	
	public KMeansIndexer getKMeansIndexer()
	{
		if (fieldKMeansIndexer == null)
		{
			fieldKMeansIndexer = (KMeansIndexer)getModuleManager().getBean(getCatalogId(),"kMeansIndexer");
			fieldKMeansIndexer.setType("semantic");
			fieldKMeansIndexer.setSearchType("semanticembedding");
			fieldKMeansIndexer.setRandomSortBy(null);
			fieldKMeansIndexer.setFieldSaveConfidence("kmeanconfidence");//;
			fieldKMeansIndexer.setFieldSaveVector("vectorarray");//facedatadoubles
			
		}
		return fieldKMeansIndexer;
	}
	public void reinitClusters(ScriptLogger logger)
	{
		if( true )
		{
		//	return;
		}
		
		logger.info(new Date() +  " reinitNodes Start reinit ");
		
		getMediaArchive().getCacheManager().clear(getKMeansIndexer().getType());
		getMediaArchive().getCacheManager().clear(getKMeansIndexer().getType() + "lookuprecord"); 
		
		getKMeansIndexer().reinitClusters(logger);
		logger.info(new Date() +  " reinitNodes Completed ");

	}
	
	protected MediaArchive getMediaArchive()
	{
		return (MediaArchive)getModuleManager().getBean(getCatalogId(),"mediaArchive");
	}

	public void index(Data inData)
	{
		List one = new ArrayList();
		one.add(inData);
		SemanticInstructions instructions = createSemanticInstructions();
		index(instructions, one);
	}
	
	public SemanticInstructions createSemanticInstructions()
	{
		SemanticInstructions instructions = new SemanticInstructions();
		return instructions;
	}
	
	public void indexAll(ScriptLogger log)
	{
		HitTracker all = getMediaArchive().query("module").exact("semanticenabled", true).search();
		Collection<String> ids = all.collectValues("id");
		
		QueryBuilder query = getMediaArchive().getSearcher("modulesearch").query();
		query.exact("semanticindexed", false);
		query.exists("semantictopics");
		query.put("searchtypes", ids);
		
		HitTracker hits = query.search();
		hits.enableBulkOperations();
		
		SemanticInstructions instructions = createSemanticInstructions();
		int indexed = 0;
		for(int i=0;i < hits.getTotalPages();i++)
		{
			hits.setPage(i+1);
			long start = System.currentTimeMillis();
			Collection<MultiValued> onepage = hits.getPageOfHits();
			indexed = indexed + index(instructions, onepage);;
			
		}
		log.info("Total indexed: " + indexed + " of " + hits.size());
		
	}
	
	
	public void clusterInit(ScriptLogger log)
	{
	
		getKMeansIndexer().reinitClusters(log);
	}
	
	public int index(SemanticInstructions inStructions, Collection<MultiValued> inEntities)  //Page of data
	{
		String url = getMediaArchive().getCatalogSettingValue("ai_vectorizer_server");
		if( url == null)
		{
			log.error("No face server configured");
			return 0;
		}

		HitTracker existingvectors = getMediaArchive().query(getKMeansIndexer().getSearchType() ).orgroup("dataid", inEntities).search();
		existingvectors.enableBulkOperations();
		
		Set<String> dataIds = (Set<String>)existingvectors.collectValues("dataid");
		inStructions.setExistingEntityIds(dataIds);
		
		Map<String,List> entitiestoprocess = new HashMap();
		
		for (Iterator iterator = inEntities.iterator(); iterator.hasNext();)
		{
			MultiValued entity = (MultiValued) iterator.next();

			String moduleid = entity.get("entitysourcetype");
			if( moduleid == null) { moduleid = "asset"; }; 
			
			List bytype = entitiestoprocess.get(moduleid);
			if( bytype == null)
			{
				bytype = new ArrayList();
				entitiestoprocess.put(moduleid,bytype);
			}
			bytype.add(entity);
			entity.setValue("semanticindexed", true);
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
				log.info(" Saved datas " + tosave.size() + " added:  " + foundsemanticstosave.size());
				getMediaArchive().saveData(getKMeansIndexer().getSearchType(),foundsemanticstosave);
				getMediaArchive().saveData(moduleid,tosave);
			}
			
		}
		catch( Throwable ex)
		{
			throw new OpenEditException("Error reading semantics" ,ex); //Should never error
		}
		
		return count;
	}

	protected Collection<MultiValued> extractVectors(SemanticInstructions inStructions, String inModuleId, List<MultiValued> entitiestoscan) throws Exception
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
				if( entity.getValue("semantictopics") == null)
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
		long start = System.currentTimeMillis();
		//log.debug("Facial Profile Detection sending " + inAsset.getName() );
		
		JSONObject tosendparams = new JSONObject();
		JSONArray list = new JSONArray();
		for (Iterator iterator = entitiestoscan.iterator(); iterator.hasNext();)
		{
			MultiValued entity = (MultiValued) iterator.next();
			
			JSONObject entry = new JSONObject();
			entry.put("id",entity.getId());
			Collection values = entity.getValues("semantictopics");
			
			StringBuffer out = new StringBuffer();
			for (Iterator iterator2 = values.iterator(); iterator2.hasNext();)
			{
				String text = (String) iterator2.next();
				out.append(text);
				out.append(" ");
				
			}
			entry.put("text",out.toString());
			list.add(entry);
		}
		tosendparams.put("data",list);
		
		CloseableHttpResponse resp = askServer(tosendparams);
		
		String responseStr = getSharedConnection().parseText(resp);
		JSONParser parser = new JSONParser();
		JSONObject jsonresponse = (JSONObject)parser.parse(responseStr);
		JSONArray results = (JSONArray)jsonresponse.get("results");
		
		Searcher searcher = getMediaArchive().getSearcher(getKMeansIndexer().getSearchType());
		
		Collection<MultiValued> newrecords = new ArrayList(results.size());
		for (int i = 0; i < results.size(); i++)
		{
			Map result = (Map)results.get(i);
			MultiValued newdata = (MultiValued)searcher.createNewData();
			String dataid = (String)result.get("id");
			newdata.setValue("dataid",dataid);
			
			Collection vectors = getKMeansIndexer().collectDoubles((Collection)result.get("embedding"));
			newdata.setValue(getKMeansIndexer().getFieldSaveVector(),vectors);
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

	public Collection<RankedResult> viewRelatedEntities(String inTerms)
	{
		//Collection allthepeopleinasset = getKMeansIndexer().searchNearestItems(startdata);
		JSONObject tosendparams = new JSONObject();
		JSONArray list = new JSONArray();
		JSONObject ask = new JSONObject();
		ask.put("id","search");
		ask.put("text",inTerms);
		list.add(ask);
		tosendparams.put("request",list);
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

		JSONArray results = (JSONArray)objt.get("result");
		Map hit = (Map)results.iterator().next();
		List vector = (List)hit.get("embeddings");
		vector = getKMeansIndexer().collectDoubles(vector);
		
		Collection<RankedResult> found = getKMeansIndexer().searchNearestItems(vector);
		
		//TODO: Go look up all the entities
		
		
		return found;		
	}

	
}
