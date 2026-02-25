package org.entermediadb.ai.classify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.creator.SmartCreatorManager;
import org.entermediadb.ai.creator.SmartCreatorSession;
import org.entermediadb.ai.informatics.InformaticsProcessor;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.Asset;
import org.entermediadb.markdown.MarkdownUtil;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
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
	
	
	public SmartCreatorManager getSmartCreatorManager() {
		return (SmartCreatorManager) getMediaArchive().getModuleManager().getBean(getCatalogId(),"smartCreatorManager",true);
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
	
	public String embedData(ScriptLogger inLog, Data inEntity, JSONObject embeddingPayload)
	{
		try
		{
			LlmConnection connection = getMediaArchive().getLlmConnection("documentEmbedding");
			LlmResponse response = connection.callJson( "/save", embeddingPayload);
			
			inEntity.setValue("entityembeddingstatus", "embedded");
			inEntity.setValue("entityembeddeddate", new Date());
			
			return response.getMessage();
		}
		catch (Exception e)
		{
			inEntity.setValue("llmerror", true);
			inEntity.setValue("entityembeddingstatus", "failed");
			inLog.error("Embed failed on " + inEntity +" "+ e.getMessage());
		}
		return null;
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

		inLog.headline("Embedding " + inRandomEntities.size() + " entities");
		
		long start = System.currentTimeMillis();
		
		Searcher pageSearcher = getMediaArchive().getSearcher(searchtype);
		
		pageSearcher.saveAllData(toprecess, null);
		
		Data module = getMediaArchive().getData("module", searchtype);
		
		String method = module.get("aicreationmethod");
		
		if (method != null && method.equals("smartcreator") )
		{
			embedSections(inLog, searchtype, toprecess, pageSearcher);
		}
		else if(searchtype.equals("userpost"))
		{			
			embedBlogs(inLog, searchtype, toprecess, pageSearcher);
		}
		else if(searchtype.equals("asset"))
		{			
			//embedAssets(inLog, searchtype, toprecess, pageSearcher);
		}
		else if(searchtype.equals("projectgoal"))
		{			
			//embedCollections(inLog, searchtype, toprecess, pageSearcher);
		}
		else if(searchtype.equals("entitydocument") || searchtype.equals("entityasset"))
		{			
			embedEntityWithPages(inLog, searchtype, toprecess, pageSearcher);
		}
		else
		{	
			embedEntity(inLog, searchtype, toprecess, pageSearcher);
		}
		
		long duration = System.currentTimeMillis() - start;
		inLog.headline("Embedding " + inRandomEntities.size() + " documents, took " + duration + " ms");
	}
	
	protected void embedSections(ScriptLogger inLog, String inSearchtype, Collection<Data> inToprecess, Searcher inPageSearcher) {
		for (Iterator iterator = inToprecess.iterator(); iterator.hasNext();)
		{
			long start = System.currentTimeMillis();
			
			MultiValued parententity = (MultiValued) iterator.next();
			
			
			embedSectionData(inLog, parententity, inSearchtype);	
			inLog.info("Embedded "+ inSearchtype + " in " + (System.currentTimeMillis() - start) + " ms");
		}
	}
	
	protected void embedSectionData(ScriptLogger inLog, Data inEntity,String searchtype)
	{
		JSONObject documentdata = new JSONObject();
		documentdata.put("doc_id", searchtype + "_" + inEntity.getId());
		
		SmartCreatorSession smartcreatorsession =  getSmartCreatorManager().loadSections(searchtype, inEntity.getId());
		Collection allpages  = new ArrayList();
		
		
		for (Data section : smartcreatorsession.getSections())
		{
			StringBuilder sectiontext = new StringBuilder();
			Collection<Data> sectionComponents  = smartcreatorsession.getSectionComponents(section.getId());
			for (Data component: sectionComponents)
			{
				if( "paragraph".equals(component.get("componenttype")))
				{
					String textcontent = component.get("content");
					if (textcontent != null && !textcontent.isEmpty())
					{
						sectiontext.append(textcontent).append("\n");
					}
				}
			}
						
			JSONObject pagedata = new JSONObject();
			pagedata.put("page_id", searchtype + "page_" + section.getId());
			pagedata.put("text", sectiontext.toString());
			
			allpages.add(pagedata);
		}
		documentdata.put("pages", allpages);
		
		String message = embedData(inLog, inEntity, documentdata);
	}
	
	protected void embedEntityWithPages(ScriptLogger inLog, String inSearchtype, Collection<Data> inToprecess, Searcher inPageSearcher) {
		for (Iterator iterator = inToprecess.iterator(); iterator.hasNext();)
		{
			long start = System.currentTimeMillis();
			
			MultiValued document = (MultiValued) iterator.next();
			
			Collection pages = getMediaArchive().query(inSearchtype + "page").exact(inSearchtype, document.getId()).search();  //TODO: Check a view?
			embedDocumentData(inLog, document, pages, inSearchtype);
			inLog.info("Embedded "+ inSearchtype + " in " + (System.currentTimeMillis() - start) + " ms");
		}
	}

	protected void embedDocumentData(ScriptLogger inLog, Data document, Collection pages, String inSearchtype)
	{
		JSONObject documentdata = new JSONObject();
		documentdata.put("doc_id", inSearchtype + "_" + document.getId());

		String assetid = document.get("primarymedia");
		if(assetid == null)
		{
			assetid = document.get("primaryimage");
		}
		Asset documentAsset = getMediaArchive().getCachedAsset(assetid);
		if(documentAsset != null)
		{
			documentdata.put("file_name", documentAsset.getName());
			
			String mime_type = getMediaArchive().getMimeTypeMap().getMimeType(documentAsset.getFileFormat());
			documentdata.put("file_type", mime_type);
			
			documentdata.put("creation_date", documentAsset.get("assetcreationdate"));
		}
		
		inLog.info("Embedding document: " + document.getName() + " with " + pages.size() + " pages");
		
		Collection allpages  = new ArrayList(pages.size());
		
		for (Iterator iterator2 = pages.iterator(); iterator2.hasNext();)
		{
			Data page = (Data) iterator2.next();
			String markdowncontent = page.get("markdowncontent");     ///TODO: Support on the fly option
			if( markdowncontent == null || markdowncontent.isEmpty())
			{
				log.info("No markdowncontent found for: "+ document);
				continue;
			}

			JSONObject pagedata = new JSONObject();
			pagedata.put("page_id", inSearchtype + "page_" + page.getId());
			pagedata.put("text", markdowncontent);
			pagedata.put("page_label", page.get("pagenum"));

			allpages.add(pagedata);
			
		}	
		
		if (allpages.isEmpty())
		{
			log.info("No pages found for: "+ document);
			return;
		}
		
		documentdata.put("pages", allpages);
		
		String message = embedData(inLog, document, documentdata);

	}
	
	protected void embedEntity(ScriptLogger inLog, String inSearchtype, Collection<Data> inToprecess, Searcher inPageSearcher) {
		for (Iterator iterator = inToprecess.iterator(); iterator.hasNext();)
		{
			long start = System.currentTimeMillis();
			
			MultiValued entityasset = (MultiValued) iterator.next();
			embedEntityData(inLog, entityasset, inSearchtype);	
			inLog.info("Embedded "+ inSearchtype + " in " + (System.currentTimeMillis() - start) + " ms");
		}
	}
	
	public void embedEntityData(ScriptLogger inLog, MultiValued inEntity, String searchtype)
	{	
		String entityembeddingstatus = inEntity.get("entityembeddingstatus");
		
		if(entityembeddingstatus == null || !"embedded".equals(entityembeddingstatus)) { 

			JSONObject entitydata = new JSONObject();
			
			entitydata.put("file_name", inEntity.getName());
			entitydata.put("creation_date", inEntity.get("entity_date"));
			entitydata.put("doc_id", searchtype + "page_" + inEntity.getId());
			entitydata.put("file_type", "text/plain");
			
			JSONObject pagedata = new JSONObject();
			pagedata.put("page_id", searchtype + "_" + inEntity.getId());
			pagedata.put("page_label", inEntity.getName());
			
			PropertyDetail detail = getMediaArchive().getSearcher(searchtype).getDetail("markdowncontent");
			if( detail != null)
			{
				String markdown = inEntity.get("markdowncontent");
				if(markdown == null || markdown.isEmpty())
				{
					log.info("No markdowncontent found "+ inEntity);
					inEntity.setValue("entityembeddingstatus", "failed"); // TODO: add ways to retry
					return;
				}
				pagedata.put("text", markdown);
			}
			else
			{
				Collection<PropertyDetail> contextFields = new ArrayList<PropertyDetail>();
				
				Collection detailsfields = getMediaArchive().getSearcher(searchtype).getDetailsForView(searchtype+"general");
				
				String entityassetfield = null;
				for (Iterator iterator = detailsfields.iterator(); iterator.hasNext();)
				{
					PropertyDetail field = (PropertyDetail) iterator.next();
					if(inEntity.hasValue(field.getId()))
					{
						if (field.getId().equals("primarymedia"))
						{
							entityassetfield = field.getId(); 
						}
						else if (entityassetfield== null && field.getId().equals("primaryimage"))
						{
							entityassetfield = field.getId(); 
						}
						else 
						{
							contextFields.add(field);
						}
					}
				}
				
				AgentContext agentcontext = new AgentContext();
				agentcontext.put("data", inEntity);
				agentcontext.put("contextfields", contextFields);
				
				if (entityassetfield != null)
				{
					String assetid = inEntity.get(entityassetfield);
					if (assetid != null)
					{
						Asset entityAsset = getMediaArchive().getCachedAsset(assetid);
						if(entityAsset != null)
						{
							agentcontext.put("primaryasset", entityAsset);
						}
					}
				}
				
				
				String templatepath = getMediaArchive().getMediaDbId() + "/ai/default/calls/commons/context_fields.json";
				
				LlmConnection llmconnection = getMediaArchive().getLlmConnection("documentEmbedding");
				String responsetext = llmconnection.loadInputFromTemplate(agentcontext, templatepath);
				log.info("Generated text for embedding: " + responsetext);
				pagedata.put("text", responsetext);
			}
			
			Collection allpages  = new ArrayList(1);
			allpages.add(pagedata);
			
			entitydata.put("pages", allpages);
			
			embedData(inLog, inEntity, entitydata);
			
		} 
	}
	
	protected void embedBlogs(ScriptLogger inLog, String inSearchtype, Collection<Data> inToprecess, Searcher inPageSearcher)
	{

		for (Iterator iterator = inToprecess.iterator(); iterator.hasNext();)
		{
			long start = System.currentTimeMillis();
			
			MultiValued blog = (MultiValued) iterator.next();
			embedBlogData(inLog, blog, inSearchtype);	
			inLog.info("Embedded "+ inSearchtype + " in " + (System.currentTimeMillis() - start) + " ms");
			
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

			String content = blog.get("maincontent");
			if( content == null)
			{
				content = blog.get("longcaption");
			}
			if( content == null)
			{
				blog.setValue("entityembeddingstatus", "notreadyyet");
				blog.setValue("entityembeddeddate", new Date());
				//not ready
				return;
			}
			pagedata.put("text", content);
			pagedata.put("page_label", blog.getName());
			
			Collection allpages  = new ArrayList(1);
			allpages.add(pagedata);
			
			blogdata.put("pages", allpages);
			
			embedData(inLog, blog, blogdata);
		}
	}

	
	/**
	 * @override
	 * This is from the handler API to deal with chats
	*/
	public LlmResponse findAnswer(AgentContext inAgentContext, Collection<String> docids, String inQuery)
	{  
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("documentEmbedding");
		if( docids.isEmpty())
		{
			LlmResponse response = llmconnection.renderLocalAction(inAgentContext, "question_norecordsfound");
			return response;
		}
		
		
		JSONObject chatjson = new JSONObject();
		chatjson.put("query", inQuery);
		chatjson.put("parent_ids", docids);
		

		String customerkey = getMediaArchive().getCatalogSettingValue("catalog-storageid");
		if( customerkey == null)
		{
			customerkey = "demo";
		}
		
		Map headers = new HashMap();
		headers.put("x-customerkey", customerkey);
		
		log.info(" sending to server: " +  chatjson.toJSONString());
		
		LlmResponse response = llmconnection.callJson("/query", headers, chatjson);
		response.setFunctionName("ragresponse"); //Remove this?
		
		//TODO: Handle in second request?
		processRagResponseWithSource(inAgentContext, response.getRawResponse(), response);
			
		return response;
	}
	
	public String findAnswer(Collection<String> docids, String inQuery)
	{  
		JSONObject chatjson = new JSONObject();
		chatjson.put("query", inQuery);
		chatjson.put("parent_ids", docids);
		
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("documentEmbedding");

		String customerkey = getMediaArchive().getCatalogSettingValue("catalog-storageid");
		if( customerkey == null)
		{
			customerkey = "demo";
		}
		
		Map headers = new HashMap();
		headers.put("x-customerkey", customerkey);
		
		log.info(" sending to server: " +  chatjson.toJSONString());
		
		LlmResponse response = llmconnection.callJson("/query", headers, chatjson);
		
		if(response == null)
		{
			return null;
		}		
		
		String answer = (String) response.getRawResponse().get("answer");
		
		if(!answer.equalsIgnoreCase("Empty Response"))
		{
			return answer;
		}
		
		return null;
	}
	
	public void processRagResponseWithSource(AgentContext inAgentContext, JSONObject ragresponse, LlmResponse response)
	{
		String answer = null;
		
		if(ragresponse == null)
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
			if(answer.equalsIgnoreCase("Empty Response"))
			{
				answer = "No relevant information found for your question.";
			}
			else
			{
				MarkdownUtil md = new MarkdownUtil();
				answer = md.render(answer);
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
		String templatepath = apphome + "/views/agentresponses/ragresponse.html";
		String responsetext = llmconnection.loadInputFromTemplate(inAgentContext, templatepath);
		
		response.setMessage(responsetext);
		response.setMessagePlain(answer);
	}
	
}
