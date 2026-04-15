package org.entermediadb.ai.classify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.creator.SmartCreatorManager;
import org.entermediadb.ai.creator.SmartCreatorSession;
import org.entermediadb.ai.informatics.InformaticsContext;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.markdown.MarkdownUtil;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.BaseData;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.page.manage.PageManager;
import org.openedit.servlet.OpenEditEngine;

@SuppressWarnings({"rawtypes", "unchecked"})
public class EmbeddingManager extends BaseAiManager
{
	private static final Log log = LogFactory.getLog(EmbeddingManager.class);

	public LlmConnection getDocumentEmbeddingConnection()
	{
		return getMediaArchive().getLlmConnection("documentEmbedding");
	}

	protected PageManager fieldPageManager;

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public SmartCreatorManager getSmartCreatorManager()
	{
		return (SmartCreatorManager) getMediaArchive().getModuleManager().getBean(getCatalogId(), "smartCreatorManager", true);
	}

	protected OpenEditEngine fieldEngine;

	public OpenEditEngine getEngine()
	{
		if (fieldEngine == null)
		{
			fieldEngine = (OpenEditEngine) getModuleManager().getBean("OpenEditEngine");

		}

		return fieldEngine;
	}

	public String embedData(ScriptLogger inLogger, Data inEntity, JSONObject embeddingPayload)
	{
		try
		{
			LlmConnection connection = getDocumentEmbeddingConnection();
			LlmResponse response = connection.callJson("/save", embeddingPayload);

			inEntity.setValue("entityembeddingstatus", "embedded");
			inEntity.setValue("entityembeddeddate", new Date());

			return response.getMessage();
		}
		catch (Exception e)
		{
			inEntity.setValue("llmerror", true);
			inEntity.setValue("entityembeddingstatus", "failed");
			inLogger.error("Embed failed on " + inEntity.getName() + " " + e.getMessage());
		}
		return null;
	}

	public void processRecords(ScriptLogger inLogger, MultiValued inAgentConfig, Collection<MultiValued> hits)
	{
		Collection<MultiValued> toembedd = new ArrayList();

		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			MultiValued entity = (MultiValued) iterator.next();

			String moduleid = entity.get("entitysourcetype");
			if (moduleid == null)
			{
				inLogger.info("Skipping entity with no source type: " + entity.getId() + " " + entity.getName());
				continue;
			}

			PropertyDetail embeddingstatus = getMediaArchive().getSearcher(moduleid).getDetail("entityembeddingstatus");
			if (embeddingstatus == null)
			{
				continue;
			}

			if ("embedded".equals(entity.get("entityembeddingstatus")))
			{
				inLogger.info("Already embedded " + entity.getName());
				continue;
			}

			if ("failed".equals(entity.get("entityembeddingstatus")))
			{
				inLogger.info("Skipping previously failed embedding " + entity.getName());
				continue;
			}

			if ("pending".equals(entity.get("entityembeddingstatus")))
			{
				continue;
			}
			toembedd.add(entity);
		}

		if (toembedd.size() < 1)
		{
			return;
		}

		inLogger.headline("Embedding " + toembedd.size() + " records"); // We only handle entites. No assets

		for (Iterator iterator = toembedd.iterator(); iterator.hasNext();)
		{
			MultiValued entity = (MultiValued) iterator.next();
			try
			{
				long startTime = System.currentTimeMillis();

				inLogger.info("Embedding entity: " + entity.getName());

				entity.setValue("entityembeddingstatus", "pending");

				String moduleid = entity.get("entitysourcetype");
				processOneEntity(inLogger, entity, moduleid);

				long duration = (System.currentTimeMillis() - startTime) / 1000L;
				inLogger.info("Took " + duration + "s to process entity: " + entity.getId() + " " + entity.getName());
			}
			catch (Exception e)
			{
				inLogger.error("LLM Error for entity: " + entity.getName(), e);
				entity.setValue("llmerror", true);
				entity.setValue("entityembeddingstatus", "failed");
			}
		}
	}

	protected void processOneEntity(ScriptLogger inLogger, MultiValued inEntity, String inModuleId) throws Exception
	{
		Data module = getMediaArchive().getData("module", inModuleId);
		String method = module.get("aicreationmethod");

		Searcher searcher = getMediaArchive().getSearcher(inModuleId);

		if (method != null && method.equals("smartcreator"))
		{
			embedHtmlData(inLogger, inEntity, inModuleId);
		}
		else
			if (inModuleId.equals("userpost"))
			{
				embedBlogData(inLogger, inEntity, inModuleId);
			}
			else
				if (inModuleId.equals("asset"))
				{
					// embedAssetData(inContext, inEntity, inModuleId);
				}
				else
					if (inModuleId.equals("projectgoal"))
					{
						// embedCollectionData(inContext, inEntity, inModuleId);
					}
					else
						if (inModuleId.equals("entitydocument") || inModuleId.equals("entityasset"))
						{
							Collection pages = getMediaArchive().query(inModuleId + "page").exact(inModuleId, inEntity.getId()).search();
							embedDocumentData(inLogger, inEntity, pages, inModuleId);
						}
						else
						{
							embedEntityData(inLogger, inEntity, inModuleId);
						}

		// Save after successful embedding
		searcher.saveData(inEntity, null);
	}
	/*
	 * protected void embedSections(ScriptLogger inLog, String inSearchtype, Collection<Data>
	 * inToprecess, Searcher inPageSearcher) { for (Iterator iterator = inToprecess.iterator();
	 * iterator.hasNext();) { long start = System.currentTimeMillis();
	 * 
	 * MultiValued parententity = (MultiValued) iterator.next();
	 * 
	 * 
	 * embedSectionData(inLog, parententity, inSearchtype); inLog.info("Embedded "+ inSearchtype +
	 * " in " + (System.currentTimeMillis() - start) + " ms"); } }
	 */
	/*
	 * protected void embedSectionData(ScriptLogger inLogger, Data inEntity, String searchtype) {
	 * JSONObject documentdata = new JSONObject(); documentdata.put("doc_id", searchtype + "_" +
	 * inEntity.getId());
	 * 
	 * SmartCreatorSession smartcreatorsession = getSmartCreatorManager().loadSections(searchtype,
	 * inEntity.getId()); Collection allpages = new ArrayList();
	 * 
	 * 
	 * for (Data section : smartcreatorsession.getSections()) { StringBuilder sectiontext = new
	 * StringBuilder(); Collection<Data> sectionComponents =
	 * smartcreatorsession.getSectionComponents(section.getId()); for (Data component:
	 * sectionComponents) { if( "paragraph".equals(component.get("componenttype"))) { String textcontent
	 * = component.get("content"); if (textcontent != null && !textcontent.isEmpty()) {
	 * sectiontext.append(textcontent).append("\n"); } } }
	 * 
	 * JSONObject pagedata = new JSONObject(); pagedata.put("page_id", searchtype + "page_" +
	 * section.getId()); pagedata.put("text", sectiontext.toString());
	 * 
	 * allpages.add(pagedata); } documentdata.put("pages", allpages);
	 * 
	 * embedData(inLogger, inEntity, documentdata); }
	 * 
	 */

	protected void embedHtmlData(ScriptLogger inLogger, Data inEntity, String searchtype)
	{
		JSONObject documentdata = new JSONObject();
		documentdata.put("doc_id", searchtype + "_" + inEntity.getId());

		String assetid = inEntity.get("primarymedia");
		if (assetid == null)
		{
			assetid = inEntity.get("primaryimage");
		}
		Asset documentAsset = getMediaArchive().getCachedAsset(assetid);
		if (documentAsset != null)
		{
			String fulltext = getMediaArchive().getAssetSearcher().getFulltext(documentAsset);

			if (fulltext == null)
			{
				inLogger.info("Can't Embed, asset doesn't contain fulltext: " + inEntity.getName());
				return;
			}

			documentdata.put("file_name", documentAsset.getName());

			String mime_type = getMediaArchive().getMimeTypeMap().getMimeType(documentAsset.getFileFormat());
			documentdata.put("file_type", mime_type);

			documentdata.put("creation_date", documentAsset.get("assetcreationdate"));

			JSONObject pagedata = new JSONObject();
			pagedata.put("page_id", searchtype);
			pagedata.put("text", fulltext);// Todo: Convert HTML to Markdown

			Collection allpages = new ArrayList();
			allpages.add(pagedata);

			documentdata.put("pages", allpages);

			embedData(inLogger, inEntity, documentdata);

		}

	}

	protected void embedEntityWithPages(ScriptLogger inLog, String inSearchtype, Collection<Data> inToprecess, Searcher inPageSearcher)
	{
		for (Iterator iterator = inToprecess.iterator(); iterator.hasNext();)
		{
			long start = System.currentTimeMillis();

			MultiValued document = (MultiValued) iterator.next();

			Collection pages = getMediaArchive().query(inSearchtype + "page").exact(inSearchtype, document.getId()).search(); // TODO: Check a view?
			embedDocumentData(inLog, document, pages, inSearchtype);
			inLog.info("Embedded " + inSearchtype + " in " + (System.currentTimeMillis() - start) + " ms");
		}
	}

	protected void embedDocumentData(ScriptLogger inLogger, Data document, Collection pages, String inSearchtype)
	{
		JSONObject documentdata = new JSONObject();
		documentdata.put("doc_id", inSearchtype + "_" + document.getId());

		String assetid = document.get("primarymedia");
		if (assetid == null)
		{
			assetid = document.get("primaryimage");
		}
		Asset documentAsset = getMediaArchive().getCachedAsset(assetid);
		if (documentAsset != null)
		{
			documentdata.put("file_name", documentAsset.getName());

			String mime_type = getMediaArchive().getMimeTypeMap().getMimeType(documentAsset.getFileFormat());
			documentdata.put("file_type", mime_type);

			documentdata.put("creation_date", documentAsset.get("assetcreationdate"));
		}

		inLogger.info("Embedding document: " + document.getName() + " with " + pages.size() + " pages");

		Collection allpages = new ArrayList(pages.size());

		for (Iterator iterator2 = pages.iterator(); iterator2.hasNext();)
		{
			Data page = (Data) iterator2.next();
			String markdowncontent = page.get("markdowncontent"); /// TODO: Support on the fly option
			if (markdowncontent == null || markdowncontent.isEmpty())
			{
				log.info("No markdowncontent found for page: " + page);
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
			log.info("No pages found for: " + document);
			return;
		}

		documentdata.put("pages", allpages);

		embedData(inLogger, document, documentdata);

	}

	protected void embedEntity(ScriptLogger inLog, String inSearchtype, Collection<Data> inToprecess, Searcher inPageSearcher)
	{
		for (Iterator iterator = inToprecess.iterator(); iterator.hasNext();)
		{
			long start = System.currentTimeMillis();

			MultiValued entityasset = (MultiValued) iterator.next();
			embedEntityData(inLog, entityasset, inSearchtype);
			inLog.info("Embedded " + inSearchtype + " in " + (System.currentTimeMillis() - start) + " ms");
		}
	}

	public void embedEntityData(ScriptLogger inLogger, MultiValued inEntity, String searchtype)
	{
		String entityembeddingstatus = inEntity.get("entityembeddingstatus");

		if (entityembeddingstatus == null || !"embedded".equals(entityembeddingstatus))
		{

			JSONObject entitydata = new JSONObject();

			entitydata.put("file_name", inEntity.getName());
			entitydata.put("creation_date", inEntity.get("entity_date"));
			entitydata.put("doc_id", searchtype + "page_" + inEntity.getId());
			entitydata.put("file_type", "text/plain");

			JSONObject pagedata = new JSONObject();
			pagedata.put("page_id", searchtype + "_" + inEntity.getId());
			pagedata.put("page_label", inEntity.getName());

			PropertyDetail detail = getMediaArchive().getSearcher(searchtype).getDetail("markdowncontent");
			if (detail != null)
			{
				String markdown = inEntity.get("markdowncontent");
				if (markdown == null || markdown.isEmpty())
				{
					inLogger.info("No markdowncontent found " + inEntity.getName());
					inEntity.setValue("entityembeddingstatus", "failed"); // TODO: add ways to retry
					return;
				}
				pagedata.put("text", markdown);
			}
			else
			{
				Collection<PropertyDetail> contextFields = new ArrayList<PropertyDetail>();

				Collection detailsfields = getMediaArchive().getSearcher(searchtype).getDetailsForView(searchtype + "general");

				String entityassetfield = null;
				for (Iterator iterator = detailsfields.iterator(); iterator.hasNext();)
				{
					PropertyDetail field = (PropertyDetail) iterator.next();
					if (inEntity.hasValue(field.getId()))
					{
						if (field.getId().equals("primarymedia"))
						{
							entityassetfield = field.getId();
						}
						else
							if (entityassetfield == null && field.getId().equals("primaryimage"))
							{
								entityassetfield = field.getId();
							}
							else
							{
								contextFields.add(field);
							}
					}
				}

				InformaticsContext agentcontext = new InformaticsContext();
				agentcontext.put("data", inEntity);
				RenderValues rendervalues = new RenderValues();
				rendervalues.setMediaArchive(getMediaArchive());
				rendervalues.setData(inEntity);
				rendervalues.setInFields(contextFields);
				agentcontext.put("rendervalues", rendervalues);
				agentcontext.put("contextfields", contextFields);

				if (entityassetfield != null)
				{
					String assetid = inEntity.get(entityassetfield);
					if (assetid != null)
					{
						Asset entityAsset = getMediaArchive().getCachedAsset(assetid);
						if (entityAsset != null)
						{
							agentcontext.put("primaryasset", entityAsset);
						}
					}
				}

				String templatepath = getMediaArchive().getMediaDbId() + "/ai/default/calls/commons/context_fields.json";

				LlmConnection llmconnection = getDocumentEmbeddingConnection();
				String responsetext = llmconnection.loadInputFromTemplate(agentcontext, templatepath);
				inLogger.info("Generated text for embedding: " + responsetext);
				pagedata.put("text", responsetext);
			}

			Collection allpages = new ArrayList(1);
			allpages.add(pagedata);

			entitydata.put("pages", allpages);

			embedData(inLogger, inEntity, entitydata);

		}
	}

	protected void embedBlogs(ScriptLogger inLog, String inSearchtype, Collection<Data> inToprecess, Searcher inPageSearcher)
	{

		for (Iterator iterator = inToprecess.iterator(); iterator.hasNext();)
		{
			long start = System.currentTimeMillis();

			MultiValued blog = (MultiValued) iterator.next();
			embedBlogData(inLog, blog, inSearchtype);
			inLog.info("Embedded " + inSearchtype + " in " + (System.currentTimeMillis() - start) + " ms");

		}
	}

	public void embedBlogData(ScriptLogger inLogger, MultiValued blog, String searchtype)
	{
		String entityembeddingstatus = blog.get("entityembeddingstatus");
		if (entityembeddingstatus == null || !"embedded".equals(entityembeddingstatus))
		{

			JSONObject blogdata = new JSONObject();
			blogdata.put("doc_id", searchtype + "_" + blog.getId());
			blogdata.put("file_name", blog.getName());

			blogdata.put("file_type", "text/plain");

			blogdata.put("creation_date", blog.get("entity_date"));

			JSONObject pagedata = new JSONObject();
			pagedata.put("page_id", searchtype + "page_" + blog.getId());

			String content = blog.get("maincontent");
			if (content == null)
			{
				content = blog.get("longcaption");
			}
			if (content == null)
			{
				blog.setValue("entityembeddingstatus", "notreadyyet");
				blog.setValue("entityembeddeddate", new Date());
				// not ready
				return;
			}
			pagedata.put("text", content);
			pagedata.put("page_label", blog.getName());

			Collection allpages = new ArrayList(1);
			allpages.add(pagedata);

			blogdata.put("pages", allpages);

			embedData(inLogger, blog, blogdata);
		}
	}

	/**
	 * @override This is from the handler API to deal with chats
	 */
	public LlmResponse findAnswer(AgentContext inAgentContext, Collection<String> docids, String inQuery)
	{
		return findAnswer(inAgentContext, docids, inQuery, true);
	}

	public LlmResponse findAnswer(AgentContext inAgentContext, Collection<String> docids, String inQuery, boolean includeSources)
	{
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("documentEmbedding");
		if (docids.isEmpty())
		{
			LlmResponse response = llmconnection.renderLocalAction(inAgentContext, "question_norecordsfound");
			return response;
		}

		JSONObject chatjson = new JSONObject();
		chatjson.put("query", inQuery);
		chatjson.put("parent_ids", docids);

		String customerkey = getMediaArchive().getCatalogSettingValue("catalog-storageid");
		if (customerkey == null)
		{
			customerkey = "demo";
		}

		Map headers = new HashMap();
		headers.put("x-customerkey", customerkey);

		log.info(" sending to server: " + chatjson.toJSONString());

		LlmResponse response = llmconnection.callJson("/query", headers, chatjson);
		response.setFunctionName("ragresponse"); // Remove this?

		if (includeSources)
		{
			processRagResponseWithSource(inAgentContext, response.getRawResponse(), response);
		}

		return response;
	}

	public String findAnswer(Collection<String> docids, String inQuery)
	{
		JSONObject chatjson = new JSONObject();
		chatjson.put("query", inQuery);
		chatjson.put("parent_ids", docids);

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("documentEmbedding");

		String customerkey = getMediaArchive().getCatalogSettingValue("catalog-storageid");
		if (customerkey == null)
		{
			customerkey = "demo";
		}

		Map headers = new HashMap();
		headers.put("x-customerkey", customerkey);

		log.info(" sending to server: " + chatjson.toJSONString());

		LlmResponse response = llmconnection.callJson("/query", headers, chatjson);

		if (response == null)
		{
			return null;
		}

		String answer = (String) response.getRawResponse().get("answer");

		if (!answer.equalsIgnoreCase("Empty Response"))
		{
			return answer;
		}

		return null;
	}

	public void processRagResponseWithSource(AgentContext inAgentContext, JSONObject ragresponse, LlmResponse response)
	{
		String answer = null;

		if (ragresponse == null)
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
			if (answer.equalsIgnoreCase("Empty Response"))
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
				if (page == null)
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

				if (duplicates.get(docid + pageid) != null)
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

		LlmConnection llmconnection = getMediaArchive().getLlmConnection("documentEmbedding"); // agentChat

		Data channel = inAgentContext.getChannel();
		String apphome = "/" + channel.get("chatapplicationid");
		String templatepath = apphome + "/views/agentresponses/ragresponse.html";
		String responsetext = llmconnection.loadInputFromTemplate(inAgentContext, templatepath);

		response.setMessage(responsetext);
		response.setMessagePlain(answer);
	}

	public void queueMissingEmbeddings(Map<String, Collection<MultiValued>> missing)
	{
		// immediately set to pending so we don't have multiple processes trying to embed the same records
		for (Iterator<String> iterator = missing.keySet().iterator(); iterator.hasNext();)
		{
			String searchtype = iterator.next();
			Collection<MultiValued> toEmbed = missing.get(searchtype);

			for (Iterator<MultiValued> iterator2 = toEmbed.iterator(); iterator2.hasNext();)
			{
				MultiValued data = (MultiValued) iterator2.next();
				data.setValue("entityembeddingstatus", "pending");
			}
			// TODO: Group and save in bulk
			getMediaArchive().saveData(searchtype, toEmbed);
		}

		ScriptLogger inLog = new ScriptLogger();

		for (Iterator<String> iterator = missing.keySet().iterator(); iterator.hasNext();)
		{
			String searchtype = iterator.next();
			Collection<MultiValued> toEmbed = missing.get(searchtype);
			BaseData config = new BaseData();
			config.setValue("searchtype", searchtype);
			// getAutomationManager().runScenario("rerun-missing-embedding",context); //TODO: Define this
			processRecords(inLog, config, toEmbed); // TODO:Load up the configs and enabled things

			getMediaArchive().saveData(searchtype, toEmbed);
		}
	}
}
