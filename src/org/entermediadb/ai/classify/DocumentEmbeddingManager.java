package org.entermediadb.ai.classify;

import java.io.StringReader;
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
import org.openedit.repository.ContentItem;
import org.openedit.util.JSONParser;

public class DocumentEmbeddingManager extends InformaticsProcessor 
{
	private static final Log log = LogFactory.getLog(DocumentEmbeddingManager.class);
	
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


	@Override
	public void processInformaticsOnAssets(ScriptLogger inLog, MultiValued inConfig, Collection<MultiValued> inAssets)
	{
		//Do nothing
		
	}

	@Override
	public void processInformaticsOnEntities(ScriptLogger inLog, MultiValued inConfig, Collection<MultiValued> inDocumentPages)
	{
		
		String generatedsearchtype = inConfig.get("generatedsearchtype");
		Searcher pageSearcher = getMediaArchive().getSearcher(generatedsearchtype);
		
		Collection<Data> tosave = new ArrayList();

		for (Iterator iterator = inDocumentPages.iterator(); iterator.hasNext();)
		{
			MultiValued documentPage = (MultiValued) iterator.next();
			
			String moduleid = documentPage.get("entitysourcetype");
			
			if( !generatedsearchtype.equals(moduleid) )
			{
				continue;
			}
			
			String markdowncontent = documentPage.get("markdowncontent");
			if( markdowncontent == null || markdowncontent.isEmpty())
			{
				continue;
			}
			
			JSONObject embeddingPayload = new JSONObject();
			embeddingPayload.put("id", documentPage.getId());
			embeddingPayload.put("data", markdowncontent);
			
			JSONObject metadata = new JSONObject();
			metadata.put("page_label", documentPage.get("pagenum"));

			String searchtype = inConfig.get("searchtype");
			
			Data documentRecord = getMediaArchive().getCachedData(searchtype, documentPage.getId());
			if( documentRecord != null)
			{
				metadata.put("file_name", documentRecord.getName());
			}
			
			Asset documentAsset = getMediaArchive().getCachedAsset(documentPage.get("parentasset"));
			if(documentAsset != null)
			{
				metadata.put("file_type", documentAsset.get("fileformat"));
				metadata.put("creation_date", documentAsset.get("assetcreationdate"));
			}

			embeddingPayload.put("metadata", metadata);
			
			boolean ok = embedDocuments(inLog, embeddingPayload);
			
			if( ok )
			{
				documentPage.setValue("documentembedded", ok);
				documentPage.setValue("documentembeddeddate", new Date());
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
	
	public boolean embedDocuments(ScriptLogger inLog, JSONObject embeddingPayload)
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
					{}
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
		String url = getMediaArchive().getCatalogSettingValue("ai_llmembedding_server");
		HttpPost method = new HttpPost(url);
		CloseableHttpResponse resp = getSharedConnection().sharedExecute(method);
		JSONParser parser = new JSONParser();
		try
		{
			JSONObject json = (JSONObject) parser.parse(new StringReader(EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8)));
	
			log.info("Returned: " + json.toJSONString());
			
			//JSONObject results = parser.parse();
			//response.setRawResponse(results);
		}
		catch (Exception e) 
		{
			throw new OpenEditException(e);
		}
		finally
		{
			getSharedConnection().release(resp);
		}
		EMediaAIResponse response = new EMediaAIResponse();
		return response;
	}
	
	
}
