package org.entermediadb.modules.publishing;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.enteremdiadb.postiz.PostizManager;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseAsset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.repository.ContentItem;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.PathUtilities;

public class ContentModule extends BaseMediaModule
{

	private static final Log log = LogFactory.getLog(ContentModule.class);

	public void createHtmlView(WebPageRequest inReq)
	{
		// Check the type? Run the conversion
		String moduleid = inReq.findPathValue("module");
		String entityid = inReq.getRequestParameter("entityid");

		MediaArchive archive = getMediaArchive(inReq);
		Data entity = archive.getData(moduleid, entityid);
		// Exec
		//		Exec exec = (Exec)getModuleManager().getBean("exec");
		//		ExecResult res = exec.runExec("restartdocker", null, true);
		//		log.info( "Restarting site: " + res.getStandardOut() );
		//		inReq.putPageValue("result", res);

		// PDF?
	}
	
	public void createNewRequest(WebPageRequest inReq) throws Exception
	{
		// Add as child
	    MediaArchive archive = getMediaArchive(inReq);
	    Searcher requests = archive.getSearcher("contentcreator");
	    Data info = requests.createNewData();
	    info.setValue("owner", inReq.getUserName());

	    info.setValue("status", "new");
	    String [] fields = inReq.getRequestParameters("field");
	    requests.updateData(inReq, fields, info);
	    requests.saveData(info);  
	    
	    archive.fireSharedMediaEvent("llm/createentities");
	
	}

	public void processCreationQueue(WebPageRequest inReq) throws Exception
	{ 
		// Add as child
		MediaArchive archive = getMediaArchive(inReq);

		ContentPublishingManager manager = getContentManager(inReq);

		HitTracker hits = archive.query("contentcreator").exact("status", "new").search();

		AgentContext params = new AgentContext();
		//params.addContext(inReq.getParameterMap());  //TODO: Not implemented
		
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			MultiValued contentrequest = (MultiValued) iterator.next();
			LlmConnection llm = null;
			if ("image".equals(contentrequest.get("contentcreatortype")))
			{
				llm = (LlmConnection) archive.getLlmConnection("createAsset");
			}
			else
			{
				llm = (LlmConnection) archive.getLlmConnection("createRecord");
			}
			
			manager.createFromLLM(params, llm, contentrequest);
			contentrequest.setValue("status", "complete");
			archive.saveData("contentcreator", contentrequest);
		}
		

	}
	
	
	public void processAssetRequests(WebPageRequest inReq) throws Exception
	{
		// Add as child
		MediaArchive archive = getMediaArchive(inReq);

		ContentPublishingManager manager = getContentManager(inReq);

		HitTracker hits = archive.query("contentcreator").exact("status", "newimage").search();
		hits.enableBulkOperations();
		
		Map params = new HashMap();
		params.putAll(inReq.getParameterMap());
		

		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			MultiValued contentrequest = (MultiValued) iterator.next();
			String entitymoduleid = contentrequest.get("entitymoduleid");
			Data entity = archive.getCachedData(entitymoduleid, contentrequest.get("entityid"));
			if(entity != null) {
				Asset asset = manager.createAssetFromLLM(params, contentrequest);
				if(asset != null)
				{
					entity.setValue("primarymedia", asset.getId());	
					archive.saveData(entitymoduleid, entity);
				}
			}
		}

	}

	public void createNewImageRequest(WebPageRequest inReq) throws Exception
	{
		// Add as child
	    MediaArchive archive = getMediaArchive(inReq);
	    Searcher requests = archive.getSearcher("contentcreator");
	    Data info = requests.createNewData();
	    info.setValue("status", "newimage");
	    info.setValue("owner", inReq.getUserName());

	    String [] fields = inReq.getRequestParameters("field");
	    requests.updateData(inReq, fields, info);
	    requests.saveData(info);
	    
	    String entitymoduleid = info.get("entitymoduleid");
	    String entityid = info.get("entityid");
	    Data entity = (Data) archive.getCachedData(entitymoduleid, entityid);
	    Data entitymodule = (Data) archive.getCachedData("module", info.get("entitymoduleid"));

	    Category rootcat = archive.getEntityManager().loadDefaultFolder(entitymodule, entity, inReq.getUser());
			String sourcepathroot = rootcat.getCategoryPath();
	    
	    String filename = info.get("aitarget");
	    String similarto = info.get("aiexamples");
	    if( similarto != null)
	    {
	    	filename += "-";
	    	filename += similarto;
	    }
	    filename = filename.substring(0, Math.min(30, filename.length()));
	    int rand = (int) (Math.random() * 10000);
	    filename += "-" + rand + ".png";
	    //Collection styles = info.getValues("aistyle");
	    
	    String sourcePath = sourcepathroot + "/" +filename;
	    
	    Asset asset = archive.getAssetBySourcePath(sourcePath);
	    
	    if(asset == null) {
				asset = new BaseAsset(archive);
				asset.setSourcePath(sourcePath);
				asset.setName(filename.toString());
				asset.addCategory(rootcat);
				asset.setSourcePath(sourcePath);
				asset.setValue("importstatus", "uploading");
				asset.setValue("fileformat", "png");
				asset.setValue("previewstatus", "converting");
				asset.setValue("assetaddeddate", new Date());
				asset.setValue("contentcreator", info.getId());
				archive.saveAsset(asset);
	    }
	    info.setValue("primarymedia", asset.getId());
	    requests.saveData(info);
	    inReq.putPageValue("data", info);
	    archive.fireSharedMediaEvent("llm/createassets");
	}
	
	
//	public void createNewEntityFromAI(WebPageRequest inReq) throws Exception
//	{
//		// Add as child
//
//		Data entity = (Data) inReq.getPageValue("entity");
//		Data entitymodule = (Data) inReq.getPageValue("entitymodule");
//		Data entitypartentview = (Data) inReq.getPageValue("entitymoduleviewdata");
//		String submodsearchtype = entitypartentview.get("rendertable");
//
//		String lastprompt = inReq.getRequestParameter("lastprompt.value");
//		entity.setValue("lastprompt", lastprompt);
//
//		MediaArchive archive = getMediaArchive(inReq);
//		archive.saveData(entitymodule.getId(), entity);
//
//		ContentManager manager = getContentManager(inReq);
//		String model = inReq.findValue("llmmodel.value");
//		Data modelinfo = archive.getData("llmmodel", model);
//
//		String type = modelinfo != null ? modelinfo.get("llmtype") : null;
//
//		if (type == null)
//		{
//			type = "gptManager";
//		}
//		else
//		{
//			type = type + "Manager";
//		}
//		LlmConnection llm = (LlmConnection) archive.getBean(type);
//
//		Data newdata = manager.createFromLLM(inReq, llm, model, entitymodule.getId(), entity.getId(), submodsearchtype);
//		boolean createassets = Boolean.parseBoolean(inReq.findValue("createassets"));
//		Searcher targetsearcher = archive.getSearcher("contentcreator");
//
//		if (createassets)
//		{
//
//			Collection<PropertyDetail> details = targetsearcher.getDetailsForView("contentcreatoraddnewimages");
//
//			for (Iterator iterator = details.iterator(); iterator.hasNext();)
//			{
//				PropertyDetail detail = (PropertyDetail) iterator.next();
//				if (detail.isList() && ("asset".equals(detail.getListId()) || "asset".equals(detail.get("rendertype"))))
//				{
//					inReq.putPageValue("detail", detail);
//					inReq.putPageValue("newdata", newdata);
//
//					String template = llm.loadInputFromTemplate(inReq, "/" + archive.getMediaDbId() + "/gpt/templates/createentityassets.html");
//					Category rootcat = archive.getEntityManager().loadDefaultFolder(entitymodule, entity, inReq.getUser());
//					String sourcepathroot = rootcat.getCategoryPath();
//					Asset asset = manager.createAssetFromLLM(inReq, sourcepathroot, template);
//					asset.addCategory(rootcat);
//					archive.saveAsset(asset);
//					log.info("Saving asset as " + detail.getName() + ": " + detail.getId());
//					newdata.setValue(detail.getId(), asset.getId());
//
//					// Break out of the loop for now...
//				}
//			}
//
//			targetsearcher.saveData(newdata);
//
//		}
//	}

	public void createNewAssetsWithAi(WebPageRequest inReq) throws Exception
	{
		// Add as child
		Data entitypartentview = (Data) inReq.getPageValue("entitymoduleviewdata");
		Data entity = (Data) inReq.getPageValue("entity");
		Data entitymodule = (Data) inReq.getPageValue("entitymodule");

		String lastprompt = inReq.getRequestParameter("llmprompt.value");
		entity.setValue("llmprompt", lastprompt);
		getMediaArchive(inReq).saveData(entitymodule.getId(), entity);
		ContentPublishingManager manager = getContentManager(inReq);
		String type = inReq.findValue("llmtype.value");
		if (type == null)
		{
			type = "gptManager";
		}
		else
		{
			type = type + "Manager";
		}
		LlmConnection llm = (LlmConnection) getMediaArchive(inReq).getBean(type);
		String edithome = inReq.findPathValue("edithome");
		String template = llm.loadInputFromTemplate(new AgentContext(), edithome + "/aitools/createnewasset.html");
		
//		manager.createAssetFromLLM(inReq, entitymodule.getId(), entity.getId(), template);

	}

	public void loadDitaXml(WebPageRequest inReq)
	{
		String moduleid = inReq.findPathValue("module");
		String entityid = inReq.getRequestParameter("entityid");
		Data entity = getMediaArchive(inReq).getData(moduleid, entityid);
		String assetid = inReq.getRequestParameter("assetid");
		String renderformat = inReq.findValue("renderformat");
		Asset asset = getMediaArchive(inReq).getAsset(assetid);
		ContentPublishingManager manager = getContentManager(inReq);

		// Make sure file is still here?
		ContentItem item = getMediaArchive(inReq).getOriginalContent(asset);

		// Load XML tree
		File file = new File(item.getAbsolutePath());
		Element root = manager.getXmlUtil().getXml(file, "UTF-8");

		inReq.putPageValue("rootelement", root);

		// Look for inlcudes

		// chchapter topicref
		Collection nodes = root.element("chapter").elements("topicref");
		inReq.putPageValue("chapters", nodes);
	}

	public void loadVisual(WebPageRequest inReq)
	{
		String moduleid = inReq.findPathValue("module");
		String entityid = inReq.getRequestParameter("entityid");
		MediaArchive mediaArchive = getMediaArchive(inReq);
		Data entity = mediaArchive.getData(moduleid, entityid);
		String assetid = inReq.getRequestParameter("assetid");
		String renderformat = inReq.findValue("renderformat");
		Asset asset = mediaArchive.getAsset(assetid);
		ContentPublishingManager manager = getContentManager(inReq);

		// Make sure file is still here?
		ContentItem item = mediaArchive.getOriginalContent(asset);
		if (!item.exists())
		{
			asset.setValue("editstatus", "7");
			mediaArchive.saveAsset(asset);
			return;
		}
		else if ("7".equals(asset.get("editstatus")))
		{
			asset.setValue("editstatus", "2"); // Undelete
			mediaArchive.saveAsset(asset);
		}

		String path = manager.loadVisual(entity, renderformat, asset);
		inReq.putPageValue("renderedpath", path);
		String catpath = PathUtilities.extractDirectoryPath(path);
		Category cat = mediaArchive.getCategorySearcher().createCategoryPath(catpath);
		inReq.putPageValue("renderedcategory", cat);
	}

	public void loadXml(WebPageRequest inReq) throws Exception
	{
		String moduleid = inReq.findPathValue("module");
		String entityid = inReq.getRequestParameter("entityid");
		Data entity = getMediaArchive(inReq).getData(moduleid, entityid);
		String assetid = inReq.getRequestParameter("assetid");
		Asset asset = getMediaArchive(inReq).getAsset(assetid);
		ContentPublishingManager manager = getContentManager(inReq);
		manager.loadTree(moduleid, entity, asset);
		// inReq.putPageValue("components",components);
	}

	protected ContentPublishingManager getContentManager(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		ContentPublishingManager manager = (ContentPublishingManager) archive.getBean("contentPublishingManager");
		if (manager.getMediaArchive() == null)
		{
			manager.setMediaArchive(archive);
		}
		return manager;
	}

	public void renderDitaTable(WebPageRequest inReq) throws Exception
	{
		String entitymoduleid = inReq.getRequestParameter("entitymoduleid");
		String entityid = inReq.getRequestParameter("entityid");
		String targetmodule = inReq.findPathValue("submoduleid");

		MediaArchive mediaArchive = getMediaArchive(inReq);

		Data entity = mediaArchive.getCachedData(entitymoduleid, entityid);
		ContentPublishingManager manager = getContentManager(inReq);
		manager.renderDita(inReq, entitymoduleid, entity, targetmodule);

	}

	public void loadDitaViewer(WebPageRequest inReq) throws Exception
	{
		Data entity = (Data) inReq.getPageValue("entity");
		Data asset = (Data) inReq.getPageValue("asset");
		ContentPublishingManager manager = getContentManager(inReq);
		Collection menu = (Collection) manager.findDitaAssets(entity);
		if (menu == null)
		{
			log.error("No menu");
			return;
		}
		if (asset == null && !menu.isEmpty())
		{
			asset = (Data) menu.iterator().next();
			inReq.putPageValue("asset", asset);
			String path = manager.loadVisual(entity, "html", asset);
			inReq.putPageValue("renderedpath", path);

		}
		inReq.putPageValue("found", menu);
	}
	
	public void postToPostiz(WebPageRequest inReq) {
	    try {
	        PostizManager manager = (PostizManager) getMediaArchive(inReq).getBean("postizManager");

	        // Get and validate the post content
	        String postContent = inReq.findValue("postcontent");
	        if (postContent == null || postContent.trim().isEmpty()) {
	            throw new OpenEditException("Post content is required.");
	        }

	        // Parse the date from the form input directly
	        String dateStr = inReq.getRequestParameter("date.value");
	        Date postDate = null;
	        if (dateStr != null && !dateStr.isEmpty()) 
	        {
	        	postDate = DateStorageUtil.getStorageUtil().parse(dateStr,"yyyy-MM-dd'T'HH:mm", inReq.getTimeZone());
	        }
	        else 
	        {
	            postDate = new Date();
	        }

	        // Collect integration IDs
	        String[] integrations = inReq.getRequestParameters("integrations");
	        List<String> siteList = (integrations != null) ? Arrays.asList(integrations) : new ArrayList<>();

	        String[] assetids = inReq.getRequestParameters("assetid.value");	        
	        List<String> assets = (assetids != null) ? Arrays.asList(assetids) : new ArrayList<>();

	        String apiKey = getPostizKey(inReq);
	        
	        // Call Postiz API
	        JSONObject result = manager.createPost(apiKey, postContent, postDate, PostizManager.POST_TYPE_SCHEDULE, assets, siteList);
	       
	        Calendar cal = DateStorageUtil.getStorageUtil().getCalendar(postDate); 
	        int dayofweek =  cal.get(Calendar.DAY_OF_WEEK) - 2;
	        inReq.putPageValue("postdayofweek",dayofweek);
	        
	        int week = cal.get(Calendar.WEEK_OF_YEAR);
	        inReq.putPageValue("postweek",week);
	        
	        int year =  cal.get(Calendar.YEAR);
	        inReq.putPageValue("postyear",year);
	        inReq.putPageValue("postdate",dateStr);
	        
	        log.info("Post created successfully: " + result.toJSONString());
	        
	        
	    } catch (Exception e) {
	        log.error("Failed to create post with Postiz", e);
	        throw new OpenEditException("Error while posting to Postiz: " + e.getMessage(), e);
	    }
	}



	public void setSocialMediaProfile(WebPageRequest inReq)
	{
		String socialmediaprofile = inReq.getRequestParameter("socialmediaprofile");
		
		inReq.getUserProfile().setProperty("socialmediaprofile", socialmediaprofile);
		
		inReq.putPageValue("socialmediaprofile", socialmediaprofile);
	}
	
	
	public void  loadPostizIntegrations(WebPageRequest inReq)
	{
		PostizManager postiz = getPostizManager(inReq);
		
		String apikey = getPostizKey(inReq);
		if (apikey != null)
		{
			Collection postoptions = postiz.listIntegrations(apikey);
			inReq.putPageValue("postoptions", postoptions);
		}

	}
	
	protected String getPostizKey(WebPageRequest inReq)
	{
		String smprofileid = inReq.getUserProfile().get("socialmediaprofile");
		
		if (smprofileid != null)
		{
			MediaArchive archive = getMediaArchive(inReq);
			Data profile = archive.getCachedData("socialmediaprofile", smprofileid);
			if (profile != null)
			{
				String apikey = profile.get("apikey");
				if (apikey != null)
				{
					return apikey;
				}
			}
		}
		return null;
	}
	
	
	protected PostizManager getPostizManager(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		PostizManager manager = (PostizManager) archive.getBean("postizManager");
		return manager;
	}

//	public void splitDocument(WebPageRequest inReq) throws Exception
//	{
//		String assetid = inReq.getRequestParameter("assetid");
//		
//		ContentManager manager = getContentManager(inReq);
//		manager.splitDocument(assetid);
//	}

}
