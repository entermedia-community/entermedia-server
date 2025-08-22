package org.entermediadb.ai.chat;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.entermediadb.asset.MediaArchive;
//import org.entermediadb.websocket.chat.ChatManager;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.modules.BaseModule;

public class ChatParseModule extends BaseModule {
	private static final Log log = LogFactory.getLog(ChatParseModule.class);
	
	public void parseChatMessage(WebPageRequest inReq) {
//		ChatManager chatManager = (ChatManager) getBeanLoader().getBean("chatManager");
//		MediaArchive archive = chatManager.getMediaArchive();
//		
		Data message = (Data) inReq.getPageValue("message");
		if (message == null) {
			log.error("No message found in request");
			return;
		}
		inReq.putPageValue("message", message.getValue("message"));
		JSONObject arguments = (JSONObject) inReq.getPageValue("arguments");
		
		if(arguments != null)
		{
			for (Iterator iterator = arguments.keySet().iterator(); iterator.hasNext();) {
				String key = (String) iterator.next();
				log.info("Arg: " + key + " = " + arguments.get(key));
				inReq.putPageValue(key, arguments.get(key));
			}
		} 
		else {
			log.warn("No arguments found in request");
		}
		
	}
	
}
