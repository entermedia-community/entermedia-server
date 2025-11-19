package org.entermediadb.ai.classify;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.entermediadb.ai.informatics.InformaticsProcessor;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.emedia.EMediaAIResponse;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.net.HttpSharedConnection;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.manage.PageManager;
import org.openedit.servlet.OpenEditEngine;
import org.openedit.util.RequestUtils;

public class DocumentEmbeddingManager extends InformaticsProcessor 
{
	private static final Log log = LogFactory.getLog(DocumentEmbeddingManager.class);
	
	protected PageManager fieldPageManager;
	public PageManager getPageManager() {
		return fieldPageManager;
	}
	
	protected RequestUtils fieldRequestUtils;
	public RequestUtils getRequestUtils() {
		return fieldRequestUtils;
	}
	
	protected OpenEditEngine fieldEngine;
	public OpenEditEngine getEngine() {
		if (fieldEngine == null) {
			fieldEngine = (OpenEditEngine) getModuleManager().getBean("OpenEditEngine");

		}

		return fieldEngine;
	}

	@Override
	public void processInformaticsOnAssets(ScriptLogger inLog, MultiValued inConfig, Collection<MultiValued> inAssets)
	{
		//Do nothing
		
	}

	@Override
	public void processInformaticsOnEntities(ScriptLogger inLog, MultiValued inConfig, Collection<MultiValued> inRandomEntities)
	{		
		inLog.headline("Embedding " + inRandomEntities.size() + " documents");

		String searchtype = inConfig.get("searchtype");
		Searcher pageSearcher = getMediaArchive().getSearcher(searchtype);
		
		Collection<Data> tosave = new ArrayList();

		for (Iterator iterator = inRandomEntities.iterator(); iterator.hasNext();)
		{
			long start = System.currentTimeMillis();
			
			MultiValued document = (MultiValued) iterator.next();
			String moduleid = document.get("entitysourcetype");
			
			if( !searchtype.equals(moduleid) )
			{
				continue; //Skip other types
			}
			
			if( document.getBoolean("documentembedded") )
			{
				log.info("Already embedded " + document);
				continue;
			}
			

			JSONObject documentdata = new JSONObject();
			documentdata.put("doc_id", searchtype + "_" + document.getId());

			String asset_id = document.get("primarymedia");
			if(asset_id == null)
			{
				asset_id = document.get("primaryimage");
			}
			Asset documentAsset = getMediaArchive().getCachedAsset(asset_id);
			if(documentAsset != null)
			{
				documentdata.put("file_name", documentAsset.getName());
				
				String mime_type = getMediaArchive().getMimeTypeMap().getMimeType(documentAsset.getFileFormat());
				documentdata.put("file_type", mime_type);
				
				documentdata.put("creation_date", documentAsset.get("assetcreationdate"));
			}

			//Get all the pages
			Collection pages = getMediaArchive().query(searchtype + "page").exact(searchtype, document.getId()).search();
			
			inLog.info("Embedding document: " + document.getName() + " with " + pages.size() + " pages");
			
			Collection allpages  = new ArrayList(pages.size());
			
			for (Iterator iterator2 = pages.iterator(); iterator2.hasNext();)
			{
				Data page = (Data) iterator2.next();
				String markdowncontent = page.get("markdowncontent");
				if( markdowncontent == null || markdowncontent.isEmpty())
				{
					log.info("No markdowncontent found "+ document);
					continue;
				}

				JSONObject pagedata = new JSONObject();
				pagedata.put("page_id", searchtype + "page_" + page.getId());
				pagedata.put("text", markdowncontent);
				pagedata.put("page_label", page.get("pagenum"));
				allpages.add(pagedata);
				
			}		
			
			documentdata.put("pages", allpages);
			
			boolean ok = embedDocument(inLog, documentdata);
			
			if( ok )
			{
				document.setValue("documentembedded", ok);
				document.setValue("documentembeddeddate", new Date());
				tosave.add(document);
				
				inLog.info("Embedded in " + (System.currentTimeMillis() - start) + " ms");
			}
			
			if(tosave.size() > 10)
			{
				pageSearcher.saveAllData(tosave, null);
				tosave.clear();
			}
		}
		if(tosave.size() > 0)
		{
			pageSearcher.saveAllData(tosave, null);
		}
	}
	
	public boolean embedDocument(ScriptLogger inLog, JSONObject embeddingPayload)
	{
		try
		{
			String endpoint = getMediaArchive().getCatalogSettingValue("ai_llmembedding_server") +  "/save";
			HttpPost method = new HttpPost(endpoint);
			method.setHeader("Content-Type", "application/json");
			
			String customerkey = getMediaArchive().getCatalogSettingValue("customer-key");
			if( customerkey == null)
			{
				customerkey = "demo";
			}
			method.setHeader("x-customerkey", customerkey);
			
			method.setEntity(new StringEntity(embeddingPayload.toJSONString(), StandardCharsets.UTF_8));
			
			HttpSharedConnection connection = getSharedConnection();
			CloseableHttpResponse resp = connection.sharedExecute(method);
			
			try
			{
				if (resp.getStatusLine().getStatusCode() != 200)
				{
					inLog.info("Embedding Server error status: " + resp.getStatusLine().getStatusCode());
					inLog.info("Error response: " + resp.toString());
					try
					{
						String error = EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);
						inLog.info(error);
					}
					catch(Exception e)
					{ 
						//Ignore }
					}
					return false;
				}
				else
				{
					return true;
				}

			}
			catch (Exception ex)
			{
				inLog.error("Error calling Embedding", ex);
				return false;
			}
			finally
			{
				connection.release(resp);
			}
			
		}
		catch (Exception e)
		{
			return false;
		}
	}

	
	public EMediaAIResponse processMessage(MultiValued message, AgentContext inAgentContext)
	{
		MediaArchive archive = getMediaArchive();
		
		String entityid = inAgentContext.getChannel().get("dataid");
		String parentmoduleid = inAgentContext.getChannel().get("searchtype"); //librarycollection
		
//		Data inDocument = getMediaArchive().getCachedData(entityid, moduleid);
		
		String query = message.get("message");
		JSONObject chat = new JSONObject();
		chat.put("query",query);
		
		JSONArray docids = new JSONArray();

		if(parentmoduleid.equals("entitydocument")) // TODO: check if rag enabled
		{
			docids.add("entitydocument_" + entityid);
		}
		else
		{
			//TODO: Support SearchCategorties and system wide search
			//TODO: Load up views and include all of them
			//TODO: Pass in the config data
			String submoduleid = "entitydocument";
	//		String parentmoduleid = "librarycollection"; 
			
			HitTracker children = archive.query(submoduleid).exact(parentmoduleid, entityid).search();
			Collection ids = children.collectValues("id");
			
			for (Object id : ids)
			{
				docids.add(submoduleid + "_" + id);
			}
		}	
		
		chat.put("doc_ids", docids);
		
		String url = archive.getCatalogSettingValue("ai_llmembedding_server");
		
		//CloseableHttpResponse resp = getSharedConnection().sharedPostWithJson(url + "/query",chat);
		
		HttpPost method = new HttpPost(url+"/query");
		
		String customerkey = archive.getCatalogSettingValue("customer-key");
		if( customerkey == null)
		{
			customerkey = "demo";
		}
		method.setHeader("x-customerkey", customerkey);
		
		method.setHeader("Content-Type", "application/json");
		method.setEntity(new StringEntity(chat.toJSONString(), StandardCharsets.UTF_8));

		CloseableHttpResponse resp = getSharedConnection().sharedExecute(method);
		
		try
		{
			if (resp.getStatusLine().getStatusCode() != 200)
			{
				log.info("Embedding Server error status: " + resp.getStatusLine().getStatusCode());
				log.info("Error response: " + resp.toString());
				
				String error =	getSharedConnection().parseText(resp);
				log.info(error);
				throw new OpenEditException("server down" + url);
			}
			
			JSONObject ragresponse = getSharedConnection().parseJson(resp);
			
			EMediaAIResponse response = new EMediaAIResponse();
			response.setFunctionName("ragresponse");
			response.setRawResponse(ragresponse);
			
			processRagResponseWithSource(inAgentContext, ragresponse, response);
			
			return response;
		}
		finally
		{
			 getSharedConnection().release(resp);
		}
	}
	
	public void processRagResponseWithSource(AgentContext inAgentContext, JSONObject ragresponse, EMediaAIResponse response)
	{
		String answer = null;
		
		if(ragresponse == null || ragresponse.isEmpty())
		{
			answer = "Didn't get any response from RAG";
		}
		else
		{
			answer = (String) ragresponse.get("answer");
		}

		// **Regular Text Response**
		if (answer != null)
		{
			if(answer.equals("Empty Response"))
			{
				answer = "No relevant information found for your question.";
			}
			
			JSONArray sourcesdata = (JSONArray) ragresponse.get("sources");
			
			Collection sources = new ArrayList();
			
			HashMap duplicates = new HashMap();
			
			for (Iterator iterator = sourcesdata.iterator(); iterator.hasNext();) 
			{
				JSONObject sourcedata = (JSONObject) iterator.next();
				
				HashMap source = new HashMap();
				
				String filename = (String) sourcedata.get("file_name");
				String pagelabel = (String) sourcedata.get("page_label");
				
				String page = (String) sourcedata.get("id");
				if(page == null)
				{
					continue;
				}
				
				String pageentityid = page.split("_")[0];
				String pageid = page.replace(pageentityid + "_", "");
				
				String doc = (String) sourcedata.get("parent_id");
				if (doc == null)
				{
					continue;
				}
				
				String docentityid = doc.split("_")[0];
				String docid = doc.replace(docentityid + "_", "");
				
				if(duplicates.get(docid + pageid) != null)
				{
					continue;
				}
				
				duplicates.put(docid + pageid, source);
				
				source.put("filename", filename);
				source.put("pagelabel", pagelabel);
				
				source.put("pageentityid", pageentityid);
				source.put("pageid", pageid);
				
				source.put("docentity", docentityid);
				source.put("docid", docid);
				
				sources.add(source);
			}
			
			inAgentContext.addContext("ragsources", sources);
		}
		
		inAgentContext.addContext("raganswer", answer);
		
		String model = getMediaArchive().getCatalogSettingValue("llmagentmodel");
		if (model == null)
		{
			model = "qwen3vl";
		}
		LlmConnection llmconnection = getMediaArchive().getLlmConnection(model);
		
		Data channel = inAgentContext.getChannel();
		String apphome = "/"+ channel.get("chatapplicationid");
		String templatepath = apphome + "/views/modules/modulesearch/results/agentresponses/ragresponse.html";
		String responsetext = llmconnection.loadInputFromTemplate(templatepath, inAgentContext);
		
		response.setMessage(responsetext);
		response.setMessagePlain(answer);
	}
	
}
