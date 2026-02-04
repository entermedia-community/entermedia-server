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
	public void savePossibleFunctionSuggestions(ScriptLogger inLog)
	{
		savePossibleFunctionSuggestions(inLog, "Creation"); 
	}
	
	public Collection<SemanticAction> createPossibleFunctionParameters(ScriptLogger inLog)
	{
		//List all functions
		Collection creations = getMediaArchive().query("aifunction").exact("messagehandler", "contentManager").search();
		
		Searcher embedsearcher = getMediaArchive().getSearcher("aifunctionparameter");
		
		Collection<SemanticAction> actions = new ArrayList();

		for (Iterator iterator = creations.iterator(); iterator.hasNext();)
		{
			Data function = (Data) iterator.next();
			
			Data module = null;

			Collection phrases  = function.getValues("phrases");
			if(phrases != null)
			{
				for (Iterator iterator2 = phrases.iterator(); iterator2.hasNext();)
				{
					String phrase = (String) iterator2.next();
					Collection existing = embedsearcher.query().exact("aifunction", function.getId()).exact("name", phrase).search();
					if( !existing.isEmpty())
					{
						log.info("Skipping existing: " + phrase);
						continue;
					}
					SemanticAction action = new SemanticAction();
					action.setAiFunction(function.getId());
					action.setSemanticText(phrase);
					action.setParentData(module);
					actions.add(action);
				}
			}

			if( function.getId().equals("image_creation_start" ) )
			{
				module = getMediaArchive().getCachedData("module", "asset");
			}
			
			if( function.getId().equals("createRecord" ) )
			{
				Schema schema = loadSchema();
				
				for (Iterator iterator2 = schema.getModules().iterator(); iterator2.hasNext();)
				{
					Data parentmodule = (Data) iterator2.next();

					Collection existing = embedsearcher.query().exact("aifunction", function.getId()).exact("parentmodule",parentmodule.getId()).search();
					if( !existing.isEmpty())
					{
						log.info("Skipping existing: " + parentmodule);
						continue;
					}
					SemanticAction action = new SemanticAction();
					action.setAiFunction(function.getId());
					action.setParentData(parentmodule);
					action.setSemanticText("Create a record in " + parentmodule.getName());
					actions.add(action);
					
					Collection<Data> children = schema.getChildrenOf(parentmodule.getId());
					
					for (Iterator iterator3 = children.iterator(); iterator3.hasNext();)
					{
						Data childmodule = (Data) iterator3.next();
						action = new SemanticAction();
						action.setParentData(parentmodule);
						action.setChildData(childmodule);
						action.setAiFunction(function.getId());
						action.setSemanticText("Create a " + childmodule.getName() + " in " + parentmodule.getName());
						actions.add(action);
					}
					
				}
			}
		}
		
		SemanticTableManager manager = loadSemanticTableManager("aifunctionparameter");

		populateVectors(manager, actions);

		return actions;

//		List<Double> tosearch = manager.makeVector("Find all records in US States in 2023");
//		Collection<RankedResult> results = manager.searchNearestItems(tosearch);
//		log.info(results);
		
		
	}
	
	protected String parseCreationParts(AgentContext inAgentContext, JSONObject structure, String type, String messageText) 
	{ 
		String creationtask = (String) structure.get("creation_task");
		if( creationtask == null)
		{
			throw new OpenEditException("No creation task specified in results: " + structure.toJSONString());
		}
		
		AiCreation creation = inAgentContext.getAiCreationParams();
		
		SemanticTableManager manager = loadSemanticTableManager("aifunctionparameter"); 
		List<Double> tosearch = manager.makeVector( sanitizeCreationTask(creationtask) );
		Collection<RankedResult> suggestions = manager.searchNearestItems(tosearch);
		
		if( !suggestions.isEmpty())
		{
			inAgentContext.setRankedSuggestions(suggestions);
			RankedResult top = (RankedResult) suggestions.iterator().next();
			if ( top.getDistance() < .7 )
			{
				String creationFunction = top.getEmbedding().get("aifunction");
				creation.setCreationFunction(creationFunction);
				String parentmodule = top.getEmbedding().get("parentmodule");
				creation.setCreationModule(parentmodule);
				
				type = "loadCreationFields";
				
			}
			else
			{
				type = "conversation";
			}
		}
		else
		{
			type = "conversation";
		}
		return type;
	}
	
	private String sanitizeCreationTask(String inTask)
	{
		inTask = inTask.replaceAll("[\\n\\r]+", " ");
		if(!inTask.contains("create"))
		{
			inTask = "create " + inTask;
		}
		return inTask;
	}
	
	protected void handleLlmResponse(AgentContext inAgentContext, LlmResponse response)
	{
		JSONObject content = response.getMessageStructured();
		
		String toolname = (String) content.get("next_step");  //request_type
		
		if(toolname == null)
		{
			throw new OpenEditException("No type specified in results: " + content.toJSONString());
		}

		JSONObject details = (JSONObject) content.get("step_details");
		
		if(details == null)
		{
			throw new OpenEditException("No details specified in results: " + content.toJSONString());
		}
		if( toolname.equals("conversation"))
		{
			JSONObject conversation = (JSONObject) details.get("conversation");
			String generalresponse = (String) conversation.get("friendly_response");
			response.setMessage( generalresponse);
		}
		else if(toolname.equals("parseCreationParts"))  //One at a time until the cancel or finish
		{
			JSONObject structure = (JSONObject) details.get(toolname);		
			if(structure == null)
			{
				throw new OpenEditException("No structure found for type: " + toolname);
			}
			toolname = parseCreationParts(inAgentContext, structure, toolname, response.getMessage());
		}
		response.setFunctionName(toolname);
	}
	
	
	
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
			LlmResponse response = loadCreationParameters(inAgentContext);
			inAgentContext.setNextFunctionName("image_creation_create");
			return response;
		}
		else if("image_creation_create".equals(inAgentContext.getFunctionName()))
		{
			LlmResponse result = createImage(inAgentContext);
			//inAgentContext.setNextFunctionName("image_creation_render");
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
	
	 
	protected LlmResponse loadCreationParameters(AgentContext inAgentContext)
	{
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("image_creation_parse");
		LlmResponse response = llmconnection.callStructuredOutputList(inAgentContext.getContext());
		if(response == null)
		{
			throw new OpenEditException("No results from AI for function: " + inAgentContext.getFunctionName());
		}
		AiCreation creation = inAgentContext.getAiCreationParams();
		JSONObject content = response.getMessageStructured();
		creation.setCreationFields( content );
		response.setMessage("");
		return response;
	}

	@Override
	public void getDetectorParams(AgentContext inAgentContext, MultiValued inTopLevelFunction) {
		// TODO Auto-generated method stub
		
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
