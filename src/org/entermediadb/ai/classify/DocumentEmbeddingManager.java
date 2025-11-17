package org.entermediadb.ai.classify;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.entermediadb.ai.informatics.InformaticsProcessor;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.emedia.EMediaAIResponse;
import org.entermediadb.asset.Asset;
import org.entermediadb.net.HttpSharedConnection;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;

public class DocumentEmbeddingManager extends InformaticsProcessor 
{
	private static final Log log = LogFactory.getLog(DocumentEmbeddingManager.class);
	

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
		
		Collection<Data> tosave = new ArrayList();

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

			JSONObject metadata = new JSONObject();
			metadata.put("id", document.getId());

			Asset documentAsset = getMediaArchive().getCachedAsset(document.get("parentasset"));
			if(documentAsset != null)
			{
				metadata.put("file_name", documentAsset.getName());
				metadata.put("file_type", documentAsset.get("fileformat"));
				metadata.put("creation_date", documentAsset.get("assetcreationdate"));
			}

			//Get all the pages
			Collection pages = getMediaArchive().query(searchtype + "pages").exact(searchtype, document.getId()).search();
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
				pagedata.put("id", page.getId());
				pagedata.put("data", markdowncontent);
				metadata.put("page_label", page.get("pagenum"));
				allpages.add(pagedata);
				
			}			
			metadata.put("pages", allpages);
			
			boolean ok = embedDocument(inLog, metadata);
			
			if( ok )
			{
				document.setValue("documentembedded", ok);
				document.setValue("documentembeddeddate", new Date());
				tosave.add(documentAsset);
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
						log.info(error);
					}
					catch(Exception e)
					{ 
						//Ignore }
					}
					return false;
				}
				else
				{
					inLog.info("Embedded document: " + embeddingPayload.get("id"));
					return true;
				}

			}
			catch (Exception ex)
			{
				log.error("Error calling Embedding", ex);
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
		String entityid = inAgentContext.getChannel().get("dataid");
		String moduleid = inAgentContext.getChannel().get("searchtype"); //librarycollection
		
		Data inDocument = getMediaArchive().getCachedData(entityid, moduleid);
		
		String prompt = message.get("prompt");
		JSONObject chat = new JSONObject();
		chat.put("prompt",prompt);
		
		//TODO: Support SearchCategorties and system wide search
		//TODO: Load up views and include all of them
		//TODO: Pass in the config data
		String submoduleid = "entitydocument";
		String parentmoduleid = "librarycollection"; 
		
		HitTracker children = getMediaArchive().query(submoduleid).exact(parentmoduleid, entityid).search();
		
		Collection ids = children.collectValues("id");
		chat.put("embed_ids", ids);
		
		String url = getMediaArchive().getCatalogSettingValue("ai_llmembedding_server");
		CloseableHttpResponse resp = getSharedConnection().sharedPostWithJson(url + "/chat",chat);
		
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
			String reply = getSharedConnection().parseText(resp);
			EMediaAIResponse response = new EMediaAIResponse();
			response.setMessage(reply);
			return response;
		}
		finally
		{
			 getSharedConnection().release(resp);
		}
	}
	
}
