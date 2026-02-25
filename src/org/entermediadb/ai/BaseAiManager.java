package org.entermediadb.ai;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.assistant.SemanticAction;
import org.entermediadb.ai.informatics.SemanticTableManager;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.util.JsonUtil;
import org.entermediadb.manager.BaseManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.profile.UserProfile;
import org.openedit.repository.ContentItem;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;

public abstract class BaseAiManager extends BaseManager 
{
	private static final Log log = LogFactory.getLog(BaseAiManager.class);
	
	
	protected Collection<PropertyDetail> loadActiveDetails(String inModuleId)
	{
		Collection detailsviews = getMediaArchive().query("view").exact("moduleid", inModuleId).exact("systemdefined", false).cachedSearch();  //Cache this

		if( detailsviews == null)
		{
			return null;
		}
		
		Collection<PropertyDetail> detailsfields = new ArrayList<PropertyDetail>();

		for (Iterator iterator = detailsviews.iterator(); iterator.hasNext();)
		{
			Data view = (Data) iterator.next();
			Collection viewfields = getMediaArchive().getSearcher(inModuleId).getDetailsForView(view);
			if( viewfields != null)
			{
				for (Iterator iterator2 = viewfields.iterator(); iterator2.hasNext();)
				{
					PropertyDetail detail = (PropertyDetail) iterator2.next();
					detailsfields.add(detail);
				}
			}
		}
		return detailsfields;
	}
	
	public Collection<MultiValued> loadUserSearchModules(UserProfile inProfile)
	{
		Collection<Data> modules = inProfile.getEntities();
		Collection<MultiValued> searchmodules = new ArrayList<MultiValued>();
		for (Iterator iterator = modules.iterator(); iterator.hasNext();)
		{
			MultiValued module = (MultiValued) iterator.next();
			if(module.getBoolean("showonsearch"))
			{
				searchmodules.add(module);
			}
		}
		return searchmodules;
	} 
	
	protected String collectText(Collection inValues)
	{
		StringBuffer words = new StringBuffer();
		if( inValues == null)
		{
			return null;
		}
		for (Iterator iterator = inValues.iterator(); iterator.hasNext();)
		{
			String text = (String) iterator.next();
			words.append(text);
			if (iterator.hasNext())
			{
				words.append(", ");
			}
			
		}
		return words.toString();
	}

	protected void clearAllCaches()
	{
//		// TODO Auto-generated method stub
//		getMediaArchive().getCacheManager().clear("aifacedetect"); //Standard cache for this fieldname
//		getMediaArchive().getCacheManager().clear("faceboxes"); //All related boxes. TODO: Limit to this record
//		//getMediaArchive().getCacheManager().clear("facepersonlookuprecord");
//		//?
////		getMediaArchive().getCacheManager().clear("aifacedetect");
////		getMediaArchive().getCacheManager().clear("faceboxes");
////		getMediaArchive().getCacheManager().clear("aifacedetect"); 

	}
	
	
	protected String loadBase64Png(Data inAsset, String imagesize)
	{
		ContentItem item = getMediaArchive().getGeneratedContent(inAsset, imagesize);
		if(!item.exists())
		{
			log.info("Missing " + imagesize + " generated image for asset ("+inAsset.getId()+") " + inAsset.getName());
			return null;
		}
		return loadBase64Image(item);
	}
	
	protected String loadBase64Png(ContentItem item)
	{
		if(!item.exists())
		{
			log.info("Missing generated image " + item.getAbsolutePath());
			return null;
		}
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		long starttime = System.currentTimeMillis();
		ArrayList<String> args = new ArrayList<String>();
		args.add("-density");
		args.add("300");
		args.add(item.getAbsolutePath());
		args.add("-antialias"); 
		args.add("-resize");
		args.add("1500x1500>");
		args.add("-background");
		args.add("white");
		args.add("-alpha");
		args.add("remove");
		args.add("-alpha");
		args.add("off");
		args.add("-strip");
		args.add("png:-");
		
		
		//convert  -density 600  Ford.pdf[3] -antialias -background white -alpha remove  -strip  -resize 11% ford4.png 

		
		Exec exec = (Exec)getMediaArchive().getBean("exec");
		exec.runExecStream("convert", args, output, 5000);
		
		byte[] bytes = output.toByteArray();  // Read InputStream as bytes
		String base64EncodedString = Base64.getEncoder().encodeToString(bytes); // Encode to Base64
		
		long duration = (System.currentTimeMillis() - starttime) ;
		log.info("Loaded and encoded " + item.getName() + " in "+duration+"ms");
		
		if(base64EncodedString == null || base64EncodedString.length() < 100)
		{
			return null;
		}
		
		return "data:image/png;base64," + base64EncodedString;

	}
	
	protected String loadBase64Image(Data inAsset, String imagesize)
	{
		ContentItem item = getMediaArchive().getGeneratedContent(inAsset, imagesize);
		if(!item.exists())
		{
			log.info("Missing " + imagesize + " generated image for asset ("+inAsset.getId()+") " + inAsset.getName());
			return null;
		}
		return loadBase64Image(item);
	}
	
	protected String loadBase64Image(ContentItem item)
	{
		if(!item.exists())
		{
			log.info("Missing generated image " + item.getAbsolutePath());
			return null;
		}
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		long starttime = System.currentTimeMillis();
		ArrayList<String> args = new ArrayList<String>();
		args.add(item.getAbsolutePath());
		args.add("-resize");
		args.add("1024x1024>");
		args.add("-quality");
		args.add("70");
		args.add("-strip"); //very important (!!)
		args.add("jpg:-");
		Exec exec = (Exec)getMediaArchive().getBean("exec");

		ExecResult result = exec.runExecStream("convert", args, output, 5000);
		if (!result.isRunOk())
		{
			throw new OpenEditException("Error converting image: "+ result.getReturnValue());
		}
		long duration = (System.currentTimeMillis() - starttime) ;
		log.info("Converted " + item.getName() + " in "+duration+"ms");

		starttime = System.currentTimeMillis();
		byte[] bytes = output.toByteArray();  // Read InputStream as bytes
		String base64EncodedString = Base64.getEncoder().encodeToString(bytes); // Encode to Base64
		duration = (System.currentTimeMillis() - starttime) ;
		log.info("Encoded " + item.getName() + " in "+duration+"ms" + " base64length:" + base64EncodedString.length());
		
		return "data:image/jpeg;base64," + base64EncodedString;

	}
	
	protected String loadTranscript(Data inAsset)
	{
		Searcher captionSearcher = getMediaArchive().getSearcher("videotrack");
		Data inTrack = captionSearcher.query().exact("assetid", inAsset.getId()).searchOne();
		if( inTrack != null)
		{
			String status = inTrack.get("transcribestatus");
			if(status != null && status.equals("complete"))
			{
				Collection captions = (Collection) inTrack.getValue("captions");
				if( captions != null)
				{
					StringBuffer fulltext = new StringBuffer();
					for (Iterator iterator = captions.iterator(); iterator.hasNext();)
					{
						Map caption = (Map) iterator.next();
						String text = (String) caption.get("cliplabel");
						if( text != null)
						{
							fulltext.append(text);
							fulltext.append(" ");
						}
					}
					return fulltext.toString();
				}
			}
		}
		return null;
	}
	
	protected Map<String, Collection> groupByModule(Collection<MultiValued> inPendingrecords)
	{
		Map<String,Collection> groupbymodule = new HashMap();
		for (Iterator iterator = inPendingrecords.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			String moduleid = data.get("entitysourcetype");
			Collection tosave = groupbymodule.get(moduleid);
			if ( tosave ==  null)
			{
				tosave = new ArrayList();
				groupbymodule.put(moduleid,tosave);
			}
			tosave.add(data);
		}
		return groupbymodule;
	}
	
	protected Collection<PropertyDetail> populateFields(String inModuleId, MultiValued inData, Collection<PropertyDetail> inExcludeFields)
	{
		Collection<PropertyDetail> detailsfields = loadActiveDetails(inModuleId);

		Collection<PropertyDetail> contextfields = new ArrayList<PropertyDetail>();
		
		Set<String> contextfieldids = new HashSet<String>();
		
		Set<String> excludeids = new HashSet<String>();
		for (Iterator iterator = inExcludeFields.iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();
			excludeids.add(detail.getId());
		}

		for (Iterator iter = detailsfields.iterator(); iter.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iter.next();
			if(excludeids.contains(detail.getId()) || contextfieldids.contains(detail.getId()))
			{
				continue;
			}
			contextfields.add(detail);
			contextfieldids.add(detail.getId());
		}
		
		if(!inModuleId.equals("asset") && !contextfieldids.contains("fulltext") && inData.get("pagenum") == null  )
		{
			addPrimaryMediaFulltext(inData, contextfields);
		}
		
		return contextfields;
	}
	
	private void addPrimaryMediaFulltext(MultiValued inData, Collection<PropertyDetail> contextfields) {
		String primarymedia = inData.get("primarymedia");
		if(primarymedia == null || primarymedia.isEmpty())
		{
			primarymedia = inData.get("primaryimage");
		}
		if(primarymedia != null)
		{
			MultiValued primaryasset = getMediaArchive().getAsset(primarymedia);
			if(primaryasset != null)
			{
				if (primaryasset.getBoolean("hasfulltext"))
				{
					String mediatype = getMediaArchive().getMediaRenderType(primaryasset);
					if(mediatype.equals("document"))
					{
						String fulltext = primaryasset.get("fulltext");
						if (fulltext != null)
						{
							fulltext = fulltext.replaceAll("\\s+", " ");
							fulltext = fulltext.substring(0, Math.min(4000, fulltext.length()));
							PropertyDetail fieldMap = new PropertyDetail();
							fieldMap.setName("Parsed Document Content");
							fieldMap.setId("fulltext");
							
							JsonUtil jsonutils = new JsonUtil();
							inData.setValue("fulltext", jsonutils.escape(fulltext));
							
							contextfields.add(fieldMap);
						}
					}
				}
			}
		}
	}

	public LlmResponse processMessage(AgentContext inAgentContext, MultiValued inMessage, MultiValued inAiFunction)
	{
		throw new OpenEditException("Not implemented");
	}

	protected Schema loadSchema()
	{
		Schema schema = (Schema)getMediaArchive().getCacheManager().get("assitant","schema");
		
		if( schema == null)
		{
			schema = new Schema();
			HitTracker allmodules = getMediaArchive().query("module").exact("showonsearch",true).search();
			Collection<Data> modules = new ArrayList();
			Collection<String> moduleids = new ArrayList();
			
			for (Iterator iterator = allmodules.iterator(); iterator.hasNext();)
			{
				Data module = (Data) iterator.next();
				Data record = getMediaArchive().query(module.getId()).all().searchOne();
				
				if(record != null)
				{
					modules.add(module);
					moduleids.add(module.getId());
					
					Collection detailsviews = getMediaArchive().query("view").exact("moduleid", module.getId()).exact("rendertype", "entitysubmodules").cachedSearch();  //Cache this

					for (Iterator iterator2 = detailsviews.iterator(); iterator2.hasNext();)
					{
						Data view = (Data) iterator2.next();
						String listid = view.get("rendertable");
						if( moduleids.contains(listid) )
						{
							Data childmodule = getMediaArchive().getCachedData("module", listid);
							schema.addChildOf(module.getId(),childmodule);
						}
					}
				}
			}
			schema.setModules(modules);
			schema.setModuleIds(moduleids);
			getMediaArchive().getCacheManager().put("assitant", "schema", schema);
			
		}
		
		return schema;
	}
	
	public Collection<String> getModulesAsEnum()
	{
		Collection<String> nameenums = new HashSet<String>();
		for (Data module : loadSchema().getModules())
		{
			String name = module.getName();
			nameenums.add(name);
			
		}
		//add asset types
		Collections.addAll(nameenums, "files", "images", "videos", "documents", "audio");
		
		
		return nameenums;
	}

	
	public SemanticTableManager loadSemanticTableManager(String inConfigId)
	{
		SemanticTableManager table = (SemanticTableManager)getMediaArchive().getCacheManager().get("semantictables",inConfigId);
		if( table == null)
		{
			table = (SemanticTableManager)getModuleManager().getBean(getCatalogId(),"semanticTableManager",false);
			table.setConfigurationId(inConfigId);
			getMediaArchive().getCacheManager().put("semantictables",inConfigId,table);
		}
		
		return table;
	}
/*
	protected LlmResponse startChat(AgentContext inAgentContext, MultiValued inAgentMessage, MultiValued userMessage, MultiValued inAiFunction )
	{
		
		MediaArchive archive = getMediaArchive();
		
		LlmConnection llmconnection = archive.getLlmConnection(inAiFunction.getId()); //Should stay search_start
		
		//inAgentContext.addContext("message", userMessage);
		
		LlmResponse response = llmconnection.callStructuredOutputList(inAgentContext.getContext()); //TODO: Replace with local API that is faster
		if(response == null)
		{
			throw new OpenEditException("No results from AI for message: " + userMessage.get("message"));
		}
		
		handleLlmResponse(inAgentContext, response);
//		else if(toolname.equals("create_image"))
//		{
//			toolname = "image_creation_start";
//			
//			AiCreation creation = inAgentContext.getAiCreationParams();					
//			creation.setCreationType("image");
//			JSONObject structure = (JSONObject) details.get("create_image");
//			creation.setImageFields(structure);
//		}
//		else if(toolname.equals("create_record"))
//		{
//			toolname = "createRecord";
//			
//			AiCreation creation = inAgentContext.getAiCreationParams();
//			creation.setCreationType("entity");
//			JSONObject structure = (JSONObject) details.get("create_record");
//			creation.setEntityFields(structure);
//		}
		
		return response;
	}
*/
	protected void handleLlmResponse(AgentContext inAgentContext, LlmResponse response)
	{
		//Do nothin
	}

	public void populateVectors(SemanticTableManager manager, Collection<SemanticAction> inActions)
	{
		Collection<String> textonly = new ArrayList(inActions.size());
		Map<String,SemanticAction> actions = new HashMap();
		Integer count = 0;
		for (Iterator iterator = inActions.iterator(); iterator.hasNext();)
		{
			SemanticAction action = (SemanticAction) iterator.next();
			textonly.add(action.getSemanticText());
			actions.put( String.valueOf(count) , action);
			count++;
		}
		
		JSONObject response = manager.execMakeVector(textonly);
		
		JSONArray results = (JSONArray)response.get("results");
		if( results == null)
		{
			return;
		}
		Collection<MultiValued> newrecords = new ArrayList(results.size());
		for (int i = 0; i < results.size(); i++)
		{
			Map hit = (Map)results.get(i);
			String countdone = (String)hit.get("id");
			SemanticAction action = actions.get(countdone);
			List vector = (List)hit.get("embedding");
			vector = manager.collectDoubles(vector);
			action.setVectors(vector);
		}
		
	}
	
	
	public LlmResponse handleError(AgentContext inAgentContext, String inError)
	{
		inAgentContext.addContext("error", inError);
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("render_error");
		LlmResponse response = llmconnection.renderLocalAction(inAgentContext, "render_error");
		//inAgentContext.setFunctionName(null);
		inAgentContext.setNextFunctionName(null);
		return response;
	}
	
	protected String findLocalActionName(AgentContext inAgentContext)
	{
		String agentFn = inAgentContext.getFunctionName();
		String apphome = (String) inAgentContext.getContextValue("apphome");

		String templatepath = apphome + "/views/agentresponses/" + agentFn + ".html";
		boolean pageexists = getMediaArchive().getPageManager().getPage(templatepath).exists();
		if(!pageexists)
		{
			int lastone = agentFn.lastIndexOf("_");
			agentFn = agentFn.substring(0,lastone);
		}
		return agentFn;
	}
}
