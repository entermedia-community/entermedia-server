package org.entermediadb.ai.classify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.informatics.InformaticsProcessor;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.manage.PageManager;
import org.openedit.servlet.OpenEditEngine;
import org.openedit.util.RequestUtils;

public class EmbeddingManager extends InformaticsProcessor 
{
	private static final Log log = LogFactory.getLog(EmbeddingManager.class);
	
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
	
	public boolean embedData(ScriptLogger inLog, JSONObject embeddingPayload)
	{
		LlmConnection connection = getMediaArchive().getLlmConnection("documentEmbedding");

		Map<String,String> header = new HashMap();
		header.put("x-customerkey", connection.getApiKey());
		LlmResponse response = connection.callJson( "/save",header,embeddingPayload);
		response.getMessage();
		return true;
	}

	@Override
	public void processInformaticsOnAssets(ScriptLogger inLog, MultiValued inConfig, Collection<MultiValued> inAssets)
	{
		return;
	}

	@Override
	public void processInformaticsOnEntities(ScriptLogger inLog, MultiValued inConfig, Collection<MultiValued> inRandomEntities)
	{

		String searchtype = inConfig.get("searchtype");
		
		Collection<Data> toprecess = new ArrayList();
		
		for (Iterator iterator = inRandomEntities.iterator(); iterator.hasNext();)
		{
			MultiValued entity = (MultiValued) iterator.next();
			String moduleid = entity.get("entitysourcetype");
			
			if( !searchtype.equals(moduleid) )
			{
				continue; //Skip other types
			}
			
			if( "embedded".equals(entity.get("entityembeddingstatus")) )
			{
				log.info("Already embedded " + entity);
				continue;
			}
			
			if( "failed".equals(entity.get("entityembeddingstatus")) )
			{
				log.info("Skipping previously failed embedding " + entity);
				continue;
			}
			
			if( "pending".equals(entity.get("entityembeddingstatus")) )
			{
				continue;
			}
			
			entity.setValue("entityembeddingstatus", "pending");
			
			toprecess.add(entity);
		}
		
		if(toprecess.isEmpty())
		{
			return;
		}
		Searcher pageSearcher = getMediaArchive().getSearcher(searchtype);
		
		pageSearcher.saveAllData(toprecess, null);
		
		inLog.headline("Embedding " + inRandomEntities.size() + " documents");
		
		if(searchtype.equals("entitydocument") || searchtype.equals("entitymarketingasset"))
		{			
			embedDocuments(inLog, searchtype, toprecess, pageSearcher);
		}
		else if(searchtype == "userpost")
		{			
			embedBlogs(inLog, searchtype, toprecess, pageSearcher);
		}
		else // if(searchtype.equals("asset"))
		{			
			embedAssets(inLog, searchtype, toprecess, pageSearcher);
		}
	}
	
	private void embedDocuments(ScriptLogger inLog, String inSearchtype, Collection<Data> inToprecess, Searcher inPageSearcher) {
		Collection<Data> tosave = new ArrayList();

		for (Iterator iterator = inToprecess.iterator(); iterator.hasNext();)
		{
			long start = System.currentTimeMillis();
			
			MultiValued document = (MultiValued) iterator.next();
			embedDocumentData(inLog, document, inSearchtype);	
			inLog.info("Embedded "+ inSearchtype + " in " + (System.currentTimeMillis() - start) + " ms");
			
			if(tosave.size() > 10)
			{
				inPageSearcher.saveAllData(tosave, null);
				tosave.clear();
			}
		}
		if(tosave.size() > 0)
		{
			inPageSearcher.saveAllData(tosave, null);
		}
	}

	private void embedDocumentData(ScriptLogger inLog, Data document, String searchtype)
	{
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
		Collection pages = getMediaArchive().query(searchtype + "page").exact(searchtype, document.getId()).search();  //TODO: Check a view?
		
		inLog.info("Embedding document: " + document.getName() + " with " + pages.size() + " pages");
		
		Collection allpages  = new ArrayList(pages.size());
		
		for (Iterator iterator2 = pages.iterator(); iterator2.hasNext();)
		{
			Data page = (Data) iterator2.next();
			String markdowncontent = page.get("markdowncontent");     ///TODO: Support on the fly option
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
		
		boolean OK = embedData(inLog, documentdata);
		if(OK)
		{		
			document.setValue("entityembeddingstatus", "embedded");
			document.setValue("entityembeddeddate", new Date());
		}
		else
		{
			document.setValue("entityembeddingstatus", "failed");
		}
	}
	
	private void embedAssets(ScriptLogger inLog, String inSearchtype, Collection<Data> inToprecess, Searcher inPageSearcher) {
		Collection<Data> tosave = new ArrayList();

		for (Iterator iterator = inToprecess.iterator(); iterator.hasNext();)
		{
			long start = System.currentTimeMillis();
			
			MultiValued asset = (MultiValued) iterator.next();
			embedAssetData(inLog, asset, inSearchtype);	
			inLog.info("Embedded "+ inSearchtype + " in " + (System.currentTimeMillis() - start) + " ms");
			
			if(tosave.size() > 10)
			{
				inPageSearcher.saveAllData(tosave, null);
				tosave.clear();
			}
		}
		if(tosave.size() > 0)
		{
			inPageSearcher.saveAllData(tosave, null);
		}
	}
	
	public void embedAssetData(ScriptLogger inLog, MultiValued inAsset, String searchtype)
	{
		Map<String, Object> inParams = new HashMap();
		Collection<PropertyDetail> contextFields = new ArrayList<PropertyDetail>();
		
		Searcher assetsearcher = getMediaArchive().getAssetSearcher();
		contextFields.add(assetsearcher.getDetail("name"));
		contextFields.add(assetsearcher.getDetail("assettype"));
		
		if(inAsset.hasValue("longcaption") && inAsset.get("longcaption").length() > 0)
		{
			contextFields.add( assetsearcher.getDetail("longcaption"));
		}
		if(inAsset.hasValue("keywords") && inAsset.getValues("keywords").size() > 0)
		{
			contextFields.add( assetsearcher.getDetail("keywords"));
		}
		
		inParams.put("contextfields", contextFields);
		
		String entityembeddingstatus = inAsset.get("entityembeddingstatus");
		if(entityembeddingstatus == null || !"embedded".equals(entityembeddingstatus)) { 
			JSONObject asetdata = new JSONObject();
			asetdata.put("doc_id", searchtype + "_" + inAsset.getId());
			asetdata.put("file_name", inAsset.get("assettitle"));
			
			asetdata.put("file_type", "text/plain");
			
			asetdata.put("creation_date", inAsset.get("entity_date"));
			
			JSONObject pagedata = new JSONObject();
			pagedata.put("page_id", searchtype + "page_" + inAsset.getId());
			
			LlmConnection llmconnection = getMediaArchive().getLlmConnection("documentEmbedding");
			
			String templatepath = getMediaArchive().getMediaDbId() + "/ai/default/calls/commons/context_fields.json";
			String responsetext = llmconnection.loadInputFromTemplate(templatepath, inParams);

			pagedata.put("text", responsetext);
			pagedata.put("page_label", inAsset.getName());
			
			Collection allpages  = new ArrayList(1);
			allpages.add(pagedata);
			
			asetdata.put("pages", allpages);
			
			boolean OK = embedData(inLog, asetdata);
			if(OK)
			{
				inAsset.setValue("entityembeddingstatus", "embedded");
				inAsset.setValue("entityembeddeddate", new Date());
			}
			else
			{
				inAsset.setValue("entityembeddingstatus", "failed");
			}
		} 
	}
	
	private void embedBlogs(ScriptLogger inLog, String inSearchtype, Collection<Data> inToprecess, Searcher inPageSearcher)
	{
		Collection<Data> tosave = new ArrayList();

		for (Iterator iterator = inToprecess.iterator(); iterator.hasNext();)
		{
			long start = System.currentTimeMillis();
			
			MultiValued blog = (MultiValued) iterator.next();
			embedBlogData(inLog, blog, inSearchtype);	
			inLog.info("Embedded "+ inSearchtype + " in " + (System.currentTimeMillis() - start) + " ms");
			
			if(tosave.size() > 10)
			{
				inPageSearcher.saveAllData(tosave, null);
				tosave.clear();
			}
		}
		if(tosave.size() > 0)
		{
			inPageSearcher.saveAllData(tosave, null);
		}
	}
	
	public void embedBlogData(ScriptLogger inLog, MultiValued blog, String searchtype) 
	{
		String entityembeddingstatus = blog.get("entityembeddingstatus");
		if(entityembeddingstatus == null || !"embedded".equals(entityembeddingstatus)) {

			JSONObject blogdata = new JSONObject();
			blogdata.put("doc_id", searchtype + "_" + blog.getId());
			blogdata.put("file_name", blog.getName());

			blogdata.put("file_type", "text/plain");
				
			blogdata.put("creation_date", blog.get("entity_date"));
			
			JSONObject pagedata = new JSONObject();
			pagedata.put("page_id", searchtype + "page_" + blog.getId());

			pagedata.put("text", blog.get("maincontent"));
			pagedata.put("page_label", blog.getName());
			
			Collection allpages  = new ArrayList(1);
			allpages.add(pagedata);
			
			blogdata.put("pages", allpages);
			
			boolean OK = embedData(inLog, blogdata);
			
			if(OK)
			{
				blog.setValue("entityembeddingstatus", "embedded");
				blog.setValue("entityembeddeddate", new Date());
			}
			else
			{
				blog.setValue("entityembeddingstatus", "failed");
			}
		}
	}

	/**
	This is from the handler API to deal with chats
	*/
	public LlmResponse processMessage(MultiValued message, AgentContext inAgentContext)
	{
		
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
