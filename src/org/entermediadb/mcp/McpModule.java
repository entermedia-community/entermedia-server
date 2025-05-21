package org.entermediadb.mcp;

import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.asset.util.JsonUtil;
import org.entermediadb.llm.VelocityRenderUtil;
import org.json.simple.JSONArray;
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
	
	public void handleMpcHttpRequest(WebPageRequest inReq) throws Exception
	{
		//This request is from some random client like copilot - we told it what endpoint to use:
		//client/key
		
		///http://172.17.0.1:8080/oneliveweb/mcp/test.html
		MediaArchive archive = getMediaArchive(inReq);
		McpManager manager = (McpManager) archive.getBean("mcpManager");
		

		String method = inReq.getRequest().getMethod();

		 McpGetHandler gethandler = manager.loadGetHandler(inReq);

		if ("GET".equals(method))
		{
			inReq.setCancelActions(true);
			inReq.setHasRedirected(true); //Dont render anything else after this

			//TODO: Block on this one forever? Stream back events to the client? Not used?
			inReq.getResponse().setContentType("text/event-stream");
//			String response = getRender().loadInputFromTemplate(inReq,  appid + "/mcp/method/" + cmd + ".html");
			// gethandler.listen(); //This will block forver
			inReq.getResponse().setHeader("mcp-session-id", gethandler.getMcpSessionId());
			inReq.getResponse().flushBuffer();
			//Only use Event-streams here
			
			//inReq.getResponse().setContentLength(100); //Without this we will use chunk encoding			
			Thread.sleep(60000);
			 return;
//
//			new Thread(() -> {
//				try {
//					currentconnnection.sendMessage(response);
//				} catch (Exception e) {
//					log.error("Failed to send SSE message", e);
//				}
//			}).start();
		}

		
		//Otherwise they are all POST requests and we stream back the reply
		

		
		//Authenticate:
//		if(cmd == null) {
//			cmd = "initialize";
//		}
		String appid = inReq.findPathValue("applicationid");
	
		JSONObject payload = (JSONObject) inReq.getJsonRequest();
		String cmd = (String) payload.get("method");

		inReq.getResponse().setHeader("mcp-session-id", gethandler.getMcpSessionId()); 

		inReq.setCancelActions(true);
		//inReq.setHasRedirected(true);

		if( cmd.equals("notifications/initialized") )
		{
			inReq.getResponse().setHeader("mcp-session-id", gethandler.getMcpSessionId());
			inReq.getResponse().setStatus(HttpServletResponse.SC_ACCEPTED); //A blank response
			inReq.setCancelActions(true);
			inReq.setHasRedirected(true);
			inReq.getResponse().flushBuffer();
			return;
		}
		
		inReq.getResponse().setStatus(HttpServletResponse.SC_OK);
		inReq.getResponse().setHeader("content-type", "application/json");
		
		inReq.putPageValue("payload", payload);
		inReq.putPageValue("protocolVersion", "2025-03-26");
		inReq.putPageValue("serverName", "EnterMedia MCP");
		inReq.putPageValue("serverVersion", "1.0.0");

		inReq.putPageValue("responsetext", "accepted");
		inReq.putPageValue("render", getRender());

		//This could be null if anonymous
		//inReq.putPageValue("user", currentconnnection.getUser());
		//TODO: Change this to be a stream of JSON in chunks. Support Streaming responses
		String fp = "/" + appid + "/mcp/method/" + cmd + ".html";
		String response = getRender().loadInputFromTemplate(inReq,  fp); //We need this to get the content length
		//inReq.getPageStreamer().include(fp);
		//inReq.getResponse().setContentLength(response.length());
		inReq.getResponse().getOutputStream().write(response.getBytes());  //This should chunk it up
		//inReq.getPageStreamer().getOutput().getWriter().write(response);
		inReq.getResponse().flushBuffer();
		inReq.setHasRedirected(true); //Dont render anything more now

		//Close?
		
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
		User user = currentconnnection.getUser();
		inReq.putPageValue("user", user);
		inReq.putPageValue("userprofile", archive.getUserProfile(user.getId()));
		
		JSONObject params = (JSONObject) payload.get("params");
		
		String response = null;
		
		if(cmd.equals("tools/call"))
		{
			String functionname = (String) params.get("name");
			
			
	//
	//		{
	//		  "jsonrpc": "2.0",
	//		  "id": $payload.id,
	//		  "result": {
	//		    "content": [
	//		      {
	//		        "type": "text",
	//		        "text": #jesc($raw)
	//		      }
	//		    ],
	//		    "isError": false
	//		  }
	//		}
			
			String responsetext = getRender().loadInputFromTemplate(inReq,  appid + "/mcp/functions/" + functionname + ".html");
			
			JSONObject jsonresponse = new JSONObject();
			jsonresponse.put("jsonrpc", "2.0");
			jsonresponse.put("id", payload.get("id"));
			
			JSONObject jsonresult = new JSONObject();
			
			JSONArray jsoncontentarray = new JSONArray();
			
			JSONObject jsoncontent = new JSONObject();
			jsoncontent.put("type", "text");
			jsoncontent.put("text", getJsonUtil().escape(responsetext));
			
			jsoncontentarray.add(jsoncontent);
			
			jsonresult.put("content", jsoncontentarray);
			jsonresult.put("isError", false);
			
			jsonresponse.put("result", jsonresult);
			
			response = jsonresponse.toJSONString();
			
		}
		else
		{
			response = getRender().loadInputFromTemplate(inReq,  appid + "/mcp/method/" + cmd + ".html");
		}
		
		
		
		String res = response;
		
		inReq.getResponse().setStatus(202);		

		new Thread(() -> {
			try {
				currentconnnection.sendMessage(res);
			} catch (Exception e) {
				log.error("Failed to send SSE message", e);
			}
		}).start();
		
	}
	
	protected JsonUtil fieldJsonUtil;
	
	
	public JsonUtil getJsonUtil()
	{
		if (fieldJsonUtil == null)
		{
			fieldJsonUtil = (JsonUtil)getModuleManager().getBean("jsonUtil");
		}
		return fieldJsonUtil;
	}

}
