package org.entermediadb.ai.assistant;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.jsonrpc.JsonRpcResponseBuilder;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.hittracker.HitTracker;
import org.openedit.profile.UserProfile;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;

public class AgentModule extends BaseMediaModule {
	private static final Log log = LogFactory.getLog(AgentModule.class);
	
	public AssistantManager getAssistantManager(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		AssistantManager assistantManager = (AssistantManager) getMediaArchive(catalogid).getBean("assistantManager");
		return assistantManager;
	}

	public void loadFields(WebPageRequest inReq)
	{
//		MediaArchive archive = getMediaArchive(inReq);
//		
//		HitTracker allmodules = archive.query("module").exact("semanticenabled", true).search();
//		Collection<String> ids = allmodules.collectValues("id");
//		
//		Collection commonDetails = new ArrayList<>();
//		
//		Set existing = new HashSet();
//		
//		for (Iterator iterator = ids.iterator(); iterator.hasNext();) {
//			String id = (String) iterator.next();
//			Collection fields = archive.getSearcher(id).getPropertyDetails().findAiSearchableProperties();
//			for (Iterator iterator2 = fields.iterator(); iterator2.hasNext();) {
//				PropertyDetail detail = (PropertyDetail) iterator2.next();
//				if( existing.contains(detail.getId()) )
//				{ 
//					commonDetails.add(detail);
//				}
//			}
//		}
//		
//		inReq.putPageValue("commonfields", commonDetails);
//		
//		Collection uniqueDetails = new ArrayList<>();
//		
//		for (Iterator iterator = ids.iterator(); iterator.hasNext();) {
//			String id = (String) iterator.next();
//			Collection fields = archive.getSearcher(id).getPropertyDetails().findAiSearchableProperties();
//			for (Iterator iterator2 = fields.iterator(); iterator2.hasNext();) {
//				PropertyDetail detail = (PropertyDetail) iterator2.next();
//				if( !uniqueDetails.contains(detail) )
//				{
//					//log.info("Already have field: " + detail.getId());
//					uniqueDetails.add(detail);
//				}
//			}
//		}
//		
//		inReq.putPageValue("uniquefields", uniqueDetails);
//		
	}
	
	public void loadModules(WebPageRequest inReq)
	{
		User user = inReq.getUser();

		if(user != null)
		{
			return;
		}
		MediaArchive archive = getMediaArchive(inReq);
		UserProfile profile = archive.getUserProfile(user.getId());
		inReq.putPageValue("modules", profile.getEntities());
	}
	public void semanticHybridSearch(WebPageRequest inReq) throws Exception 
	{	
		semanticHybridSearch(inReq, false);
	}
	
	public void semanticHybridSearch(WebPageRequest inReq, boolean isMcp) throws Exception {

		Data message = (Data) inReq.getPageValue("message");
		if (message == null) {
			log.error("No message found in request");
			return;
		}
		inReq.putPageValue("message", message.getValue("message"));
		JSONObject arguments = (JSONObject) inReq.getPageValue("arguments");
		
		if(arguments == null)
		{
			log.warn("No arguments found in request");
			return;
		}

//		String channelId = message.get("channel");
//		AiCurrentStatus currentStatus = getAssistantManager(inReq).loadCurrentStatus(channelId);
		
		UserProfile userprofile = (UserProfile) inReq.getPageValue("chatprofile");
		
		if(userprofile == null)
		{
			userprofile = (UserProfile) inReq.getPageValue("userprofile");
		}
		
		inReq.putPageValue("userprofile", userprofile);

		AiSearch aiSearchArgs = getAssistantManager(inReq).processSematicSearchArgs(arguments, userprofile);
		
		if(isMcp)
		{
			addMcpVars(inReq, aiSearchArgs);
		}
		
		getResultsManager(inReq).searchByKeywords(inReq, aiSearchArgs);
		
	}
	
	public void mcpSemanticHybridSearch(WebPageRequest inReq) throws Exception
	{
		semanticHybridSearch(inReq, true);
	}
	
	public void addMcpVars(WebPageRequest inReq, AiSearch searchArgs)	
	{
		Collection<String> keywords = searchArgs.getKeywords();
		inReq.putPageValue("keywordsstring", getResultsManager(inReq).joinWithAnd(new ArrayList(keywords)));
		
		Collection<Data> modules = searchArgs.getSelectedModules();
		
	
		Collection<String> moduleNames = new ArrayList();
			
		for (Iterator iterator = modules.iterator(); iterator.hasNext();)
		{
			Data module = (Data) iterator.next();
			if(!moduleNames.contains(module.getName()))
			{
				moduleNames.add(module.getName());
			}
		}
			
		inReq.putPageValue("modulenamestext", getResultsManager(inReq).joinWithAnd(new ArrayList(moduleNames)));
		
	}
	
	public void mcpGenerateReport(WebPageRequest inReq) throws Exception {
		JSONObject arguments = (JSONObject) inReq.getPageValue("arguments");
		
		Collection<String> keywords = getResultsManager(inReq).parseKeywords(arguments.get("keywords"));
		 
		MediaArchive archive = getMediaArchive(inReq);
		
		HitTracker pdfs = archive.query("asset").freeform("description", String.join(" ", keywords)).search();
		
		Collection pdfTexts = new ArrayList<String>();
		
		for (Iterator iterator = pdfs.iterator(); iterator.hasNext();) {
			Data pdf = (Data) iterator.next();
			ContentItem item = getPageManager().getRepository().getStub("/WEB-INF/data/" + archive.getCatalogId() +"/assets/" + pdf.getSourcePath() + "/fulltext.txt");
			
			try(InputStream inputStream = item.getInputStream())
			{				
				String text = new String(inputStream.readAllBytes());
				if(text.length() > 0)
				{
					pdfTexts.add(text); 					
				}
				log.info(text);
			}
		}

		String fullText = String.join("\n\n", pdfTexts);
		
		if(fullText.replaceAll("\\s|\\n", "").length() == 0)
		{ 
			return;
		}

		Map params = new HashMap();
		params.put("fulltext", fullText);
		
		String model = archive.getCatalogSettingValue("mcp_report_model");
		if(model == null)
		{
			model = "gpt-5-nano";
		}
		
		String report = getAssistantManager(inReq).generateReport(params, model);
		inReq.putPageValue("report", report);
	}

}
