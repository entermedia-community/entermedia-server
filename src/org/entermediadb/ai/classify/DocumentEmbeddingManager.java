package org.entermediadb.ai.classify;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.common.recycler.Recycler.V;
import org.entermediadb.ai.informatics.InformaticsProcessor;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
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

		String searchtype = inConfig.get("searchtype");
		Searcher pageSearcher = getMediaArchive().getSearcher(searchtype);
		
		
		Collection<MultiValued> toprecess = new ArrayList();
		
		for (Iterator iterator = inRandomEntities.iterator(); iterator.hasNext();)
		{
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
			
			toprecess.add(document);
		}
		
		if(toprecess.isEmpty())
		{
			return;
		}
		
		inLog.headline("Embedding " + inRandomEntities.size() + " documents");
		
		
		Collection<Data> tosave = new ArrayList();

		for (Iterator iterator = toprecess.iterator(); iterator.hasNext();)
		{
			long start = System.currentTimeMillis();
			
			MultiValued document = (MultiValued) iterator.next();

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
		LlmConnection connection = getMediaArchive().getLlmConnection("documentEmbedding");

		Map<String,String> header = new HashMap();
		header.put("x-customerkey", connection.getApiKey());
		LlmResponse response = connection.callJson( "/save",header,embeddingPayload);
		response.getMessage();
		return true;
	}

	
	public LlmResponse processMessage(MultiValued message, AgentContext inAgentContext)
	{
		
		/**
		 * 
		 //Set the function name
		//getMediaArchive().saveData("agentcontext", agentContext);

//		if (functionName.equals("ragresponse"))
//		{
//			messageToUpdate.setValue("messageplain", response.getMessagePlain());
//			messageToUpdate.setValue("message", response.getMessage());
//			messageToUpdate.setValue("chatmessagestatus", "completed");
//
//			chats.saveData(messageToUpdate);
//			server.broadcastMessage(archive.getCatalogId(), messageToUpdate);
//		}
//		else //add option to run AI based functions like create an image
//		{
		 */
		
		MediaArchive archive = getMediaArchive();
		
		String entityid = inAgentContext.getChannel().get("dataid");
		String parentmoduleid = inAgentContext.getChannel().get("searchtype"); //librarycollection
		
//		Data inDocument = getMediaArchive().getCachedData(entityid, moduleid);
		
		MultiValued parent = (MultiValued)archive.getCachedData("chatterbox",message.get("replytoid"));
		String query = parent.get("message");
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
		
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("documentEmbedding");

		String customerkey = llmconnection.getApiKey();
		if( customerkey == null)
		{
			customerkey = "demo";
		}
		Map headers = new HashMap();
		headers.put("x-customerkey", customerkey);
		
		log.info(" sending to server: " +  chat.toJSONString());
		
		LlmResponse response = llmconnection.callJson("/query", headers, chat);
		response.setFunctionName("ragresponse");
		
		//TODO: Handle in second request?
		processRagResponseWithSource(inAgentContext, response.getRawResponse(), response);
			
		return response;
	}
	
	public void processRagResponseWithSource(AgentContext inAgentContext, JSONObject ragresponse, LlmResponse response)
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
		
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("documentEmbedding"); //agentChat
		
		Data channel = inAgentContext.getChannel();
		String apphome = "/"+ channel.get("chatapplicationid");
		String templatepath = apphome + "/views/modules/modulesearch/results/agentresponses/ragresponse.html";
		String responsetext = llmconnection.loadInputFromTemplate(templatepath, inAgentContext);
		
		response.setMessage(responsetext);
		response.setMessagePlain(answer);
	}
	
}
