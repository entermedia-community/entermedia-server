package org.entermediadb.ai.assistant;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.ChatMessageHandler;
import org.entermediadb.ai.Schema;
import org.entermediadb.ai.informatics.SemanticTableManager;
import org.entermediadb.ai.knn.RankedResult;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.markdown.MarkdownUtil;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.repository.ContentItem;
import org.openedit.repository.InputStreamItem;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;

public class CreationManager extends BaseAiManager implements ChatMessageHandler
{
	private static final Log log = LogFactory.getLog(CreationManager.class);
	
	
	@Override
	public LlmResponse processMessage(AgentContext inAgentContext, MultiValued inAgentMessage, MultiValued inAiFunction)
	{
		String agentFn = inAgentContext.getFunctionName();
		if("image_creation_welcome".equals(inAgentContext.getFunctionName()))
		{
			String entityid = (String) inAgentContext.getValue("entityid");
			String entitymoduleid = (String) inAgentContext.getValue("entitymoduleid");
			
			Data entity = getMediaArchive().getCachedData(entitymoduleid, entityid);
			inAgentContext.addContext("entity", entity);
			
			LlmConnection llmconnection = getMediaArchive().getLlmConnection(inAgentContext.getFunctionName());
			LlmResponse response = llmconnection.renderLocalAction(inAgentContext, inAgentContext.getFunctionName());
			//This is for the chat UI to pass it back
			inAgentContext.setFunctionName("image_creation_parse");
			return response;
		}
		else if("image_creation_parse".equals(inAgentContext.getFunctionName()))
		{
			LlmConnection llmconnection = getMediaArchive().getLlmConnection("image_creation_parse");
			LlmResponse response = llmconnection.callStructuredOutputList(inAgentContext.getContext(),"image_creation_parse");
			if(response == null)
			{
				throw new OpenEditException("No results from AI for function: " + inAgentContext.getFunctionName());
			}
			AiCreation creation = inAgentContext.getAiCreationParams();
			JSONObject content = response.getMessageStructured();
			creation.setCreationFields( content );
			response.setMessage("");
			inAgentContext.setNextFunctionName("image_creation_create");
			return response;
		}
		else if("image_creation_create".equals(inAgentContext.getFunctionName()))
		{
			LlmResponse result = createImage(inAgentContext);
			inAgentContext.setNextFunctionName("image_creation_render");
			return result;
		}
		else if("image_creation_render".equals(inAgentContext.getFunctionName()))
		{
			String assetid = inAgentContext.get("assetid");
			
			Asset asset = getMediaArchive().getAsset(assetid);
			inAgentContext.addContext("asset", asset); //Get the updated asset

			inAgentContext.addContext("refreshing", "true");
			
			LlmConnection llmconnection = getMediaArchive().getLlmConnection("image_creation_render");
			
			LlmResponse result = llmconnection.renderLocalAction(inAgentContext);
			
			log.info("Next function: " + inAgentContext.getNextFunctionName());
			
			return result;
			
		}
		else if("creation_entity_create".equals(inAgentContext.getFunctionName()))
		{
			//This was parsed from AutoDetectManager so app params should be in agentcontext already
			
			//TODO: Save the new entity with any foreigh keys
			
			//SHow the link in the chat
		}
		/*
		else if ("createRecord".equals(inAgentContext.getFunctionName()))
		{
			MultiValued usermessage = (MultiValued)getMediaArchive().getCachedData("chatterbox", inAgentMessage.get("replytoid"));
			
			LlmResponse result = createRecord(usermessage, inAgentContext);
			
			return result;
		}
		*/
		throw new OpenEditException("Function not supported " + agentFn);
		
	}
	
	
	public LlmResponse createImage(AgentContext inAgentContext) 
	{
		MediaArchive archive = getMediaArchive();
		AiCreation aiCreation = inAgentContext.getAiCreationParams();
		LlmConnection llmconnection = archive.getLlmConnection("image_creation_create");
		
		JSONObject imagefields = (JSONObject) aiCreation.getCreationFields();
		
		if (imagefields == null)
		{
			return null;
		}
		
		String prompt = (String) imagefields.get("prompt");

		if (prompt == null)
		{
			return null;
		}
		
		User chatuser = inAgentContext.getUserProfile().getUser();
		String filename = (String) imagefields.get("image_name");
		LlmResponse results = llmconnection.createImage(prompt);
		

		for (Iterator iterator = results.getImageBase64s().iterator(); iterator.hasNext();)
		{
			String base64 = (String) iterator.next();

			Asset asset = (Asset) archive.getAssetSearcher().createNewData();

			asset.setValue("importstatus", "created");

			if( filename == null || filename.length() == 0)
			{
				filename = "aiimage_" + System.currentTimeMillis() ;
			}
			
			asset.setName(filename + ".png");
			asset.setValue("assettitle", filename);
			asset.setValue("assetaddeddate", new Date());
			
			String sourcepath = "Channels/" + chatuser.getId() + "/" + DateStorageUtil.getStorageUtil().getTodayForDisplay() + "/" + filename;
			asset.setSourcePath(sourcepath);

			String path = "/WEB-INF/data/" + asset.getCatalogId() + "/originals/" + asset.getSourcePath();
			ContentItem saveTo = archive.getPageManager().getPage(path).getContentItem();
			
			
			try
			{
				InputStreamItem revision = new InputStreamItem();
				
				revision.setAbsolutePath(saveTo.getAbsolutePath());
				revision.setPath(saveTo.getPath());
				revision.setAuthor( chatuser.getId() );
				revision.setType( ContentItem.TYPE_ADDED );
				revision.setMessage( saveTo.getMessage());
				
				revision.setPreviewImage(saveTo.getPreviewImage());
				revision.setMakeVersion(false);
				
				log.info("Saving image -> " + path + "/" + filename);
				
				InputStream input = null;
				
				String code = base64.substring(base64.indexOf(",") +1, base64.length());
				byte[] tosave = Base64.getDecoder().decode(code);
				input = new ByteArrayInputStream(tosave);
				
				revision.setInputStream(input);
				
				archive.getPageManager().getRepository().put( revision );
				asset.setProperty("importstatus", "created");
				archive.saveAsset(asset);
			}
			catch (Exception ex)
			{
				asset.setProperty("importstatus", "error");
				log.error(ex);
				archive.saveAsset(asset);
			}
			
			// inReq.putPageValue("asset", asset);
			inAgentContext.addContext("asset", asset);
			inAgentContext.setNextFunctionName("image_creation_render");
			inAgentContext.setValue("assetid", asset.getId());
			inAgentContext.setValue("wait", 1000);
		}
		
		
		archive.fireSharedMediaEvent("importing/assetscreated");
		
		return results;
	}
}
