package org.entermediadb.ai.assistant;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.profile.UserProfile;

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
	
	public void semanticHybridSearch(WebPageRequest inReq) throws Exception {

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

		for (Iterator iterator = arguments.keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			log.info("Arg: " + key + " = " + arguments.get(key));
			inReq.putPageValue(key, arguments.get(key));
		}

		String channelId = message.get("channel");
//		AiCurrentStatus currentStatus = getAssistantManager(inReq).loadCurrentStatus(channelId);
		UserProfile userprofile = (UserProfile) inReq.getPageValue("chatprofile");
		
		if(userprofile == null)
		{
			userprofile = (UserProfile) inReq.getPageValue("userprofile");
		}
		else
		{
			inReq.putPageValue("userprofile", userprofile);
		}
		
		AiSearch aiSearchArgs = getAssistantManager(inReq).processSematicSearchArgs(arguments);
		
		getResultsManager(inReq).searchByKeywords(inReq, aiSearchArgs);
		
	}

}
