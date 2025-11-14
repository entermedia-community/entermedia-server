package org.entermediadb.ai.classify;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.entermediadb.ai.informatics.InformaticsProcessor;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.emedia.EMediaAIResponse;
import org.entermediadb.asset.Asset;
import org.entermediadb.net.HttpSharedConnection;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONObject;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
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
	public void processInformaticsOnEntities(ScriptLogger inLog, MultiValued inConfig, Collection<MultiValued> inRecords)
	{
		String searchtype = inConfig.get("searchtype");

		for (Iterator iterator = inRecords.iterator(); iterator.hasNext();)
		{
			MultiValued entity = (MultiValued) iterator.next();
			
			String moduleid = entity.get("entitysourcetype");
			
			if( !searchtype.equals(moduleid) )
			{
				continue;
			}
			
			Collection<Asset> assets = new ArrayList();
			
			String assetid = entity.get("primarymedia");
			if( assetid == null)
			{
				assetid = entity.get("primaryimage");
			}
			if( assetid == null)
			{
				continue;
			}
			
			Asset document = getMediaArchive().getAsset(assetid);
			if (document.getBoolean("documentembedded"))
			{
				continue;
			}
			assets.add(document);
			
			embedDocuments(inLog, inConfig, assets);
			
		}
	}
	
	public void embedDocuments(ScriptLogger inLog, MultiValued inConfig, Collection<Asset> inAssets)
	{
		Collection<Asset> tosave = new ArrayList();
		for (Asset asset : inAssets)
		{
			if("pdf".equals(asset.getFileFormat()))
			{
				try
				{
					Map tosendpameters = new HashMap();
					ContentItem content = getMediaArchive().getOriginalContent(asset);
					tosendpameters.put("file", content);
					tosendpameters.put("assetid", asset.getId());
					
					CloseableHttpResponse resp;
					String url = getMediaArchive().getCatalogSettingValue("ai_llmembedding_server");
					resp = getSharedConnection().sharedMimePost(url + "/text",tosendpameters);
					
					int statuscode = resp.getStatusLine().getStatusCode();
					if (statuscode != 200)
					{
						//remote server error, may be a broken image
						inLog.error(resp.toString());
						//throw new OpenEditException("Server not working" + resp.getStatusLine());
					}
					getSharedConnection().release(resp);
					asset.setValue("documentembedded", true);	
					tosave.add(asset);
					
				}
				catch (Exception e)
				{
					inLog.error("Error embedding document for asset: " + asset.getId() + " - " + e.getMessage());
				}
			}
		}
		if (tosave.size() > 0) {
			getMediaArchive().saveAssets(tosave);
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
