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
	
	public void chatSemanticHybridSearch(WebPageRequest inReq) throws Exception 
	{	
		getAssistantManager(inReq).semanticHybridSearch(inReq, false);
	}
	
	public void mcpSemanticHybridSearch(WebPageRequest inReq) throws Exception
	{
		getAssistantManager(inReq).semanticHybridSearch(inReq, true);
	}

	public void mcpGenerateReport(WebPageRequest inReq) throws Exception {
		JSONObject arguments = (JSONObject) inReq.getPageValue("arguments");
		String report = getAssistantManager(inReq).generateReport(arguments);
		inReq.putPageValue("report", report);
	}

}
