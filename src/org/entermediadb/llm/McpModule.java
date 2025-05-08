package org.entermediadb.llm;

import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.users.User;

public class McpModule extends BaseMediaModule
{
	private static Log log = LogFactory.getLog(McpModule.class);

	protected McpManager fieldMpcManager;
	protected VelocityRenderUtil fieldRender;

	public VelocityRenderUtil getRender()
	{
		return fieldRender;
	}

	public void setRender(VelocityRenderUtil inRender)
	{
		fieldRender = inRender;
	}

	

	public void generateMcpKey(WebPageRequest inReq) {
		
		MediaArchive archive = getMediaArchive(inReq);
		Searcher keys = archive.getSearcher("appkeys");
		User user = inReq.getUser();
		

		Data keyinfo = keys.query().exact("user", user).searchOne();
		if(keyinfo == null) {
			keyinfo = keys.createNewData();
			keyinfo.setValue("user", user.getId());
		}		
		String newkey = UUID.randomUUID().toString();
		keyinfo.setValue("key", newkey);
		keys.saveData(keyinfo);
		
		
	}
	
	

	public void handleMpcRequest(WebPageRequest inReq) throws Exception
	{
		//This request is from some random client like copilot - we told it what endpoint to use:
		//client/key
		
		///http://172.17.0.1:8080/oneliveweb/mcp/test.html
		MediaArchive archive = getMediaArchive(inReq);
		McpManager manager = (McpManager) archive.getBean("mcpManager");
		
		String method = inReq.getRequest().getMethod();
		
		if ("GET".equals(method))
		{
		//	throw new OpenEditException("GET is handled by McpGenerator");
		}
		
		McpConnection currentconnnection = manager.getConnection(inReq);
		
		
		
		JSONObject payload = (JSONObject) inReq.getJsonRequest();
		if (payload == null)
		{
			payload = new JSONObject();
			payload.put("method", "initialize");
			payload.put("id", 0);

		}
		String cmd = (String) payload.get("method");
		if(cmd == null) {
			cmd = "initialize";
		}
		inReq.putPageValue("currentconnection", currentconnnection);
		
		String appid = inReq.findPathValue("applicationid");
	
		inReq.putPageValue("payload", payload);
		inReq.putPageValue("protocolVersion", "2025-03-26");
		inReq.putPageValue("serverName", "EnterMedia MCP");
		inReq.putPageValue("serverVersion", "1.0.0");

		inReq.putPageValue("responsetext", "accepted");
		inReq.putPageValue("render", getRender());

		//This could be null if anonymous
		inReq.putPageValue("user", currentconnnection.getUser());
		
		
		
		String response = getRender().loadInputFromTemplate(inReq,  appid + "/mcp/method/" + cmd + ".html");
		
		inReq.getResponse().setStatus(202);		

		new Thread(() -> {
			try {
				currentconnnection.sendMessage(response);
			} catch (Exception e) {
				log.error("Failed to send SSE message", e);
			}
		}).start();
		
	}

}
