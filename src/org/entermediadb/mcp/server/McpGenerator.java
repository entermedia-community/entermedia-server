package org.entermediadb.mcp.server;

import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.jsonrpc.JsonRpcResponseBuilder;
import org.json.simple.JSONObject;
import org.openedit.Generator;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.generators.Output;
import org.openedit.page.Page;

/**
 * Basic SSE-compatible generator that bypasses Velocity to stream live events.
 */
public class McpGenerator implements Generator
{
	protected ModuleManager fieldModuleManager;
	protected String fieldName;
	private static final Log log = LogFactory.getLog(McpGenerator.class);


	@Override
	public void generate(WebPageRequest inReq, Page inPage, Output inOut) throws OpenEditException {
		String catalogid = inReq.findPathValue("catalogid");
		MediaArchive archive = (MediaArchive) getModuleManager().getBean(catalogid, "mediaArchive");
    	McpManager manager = (McpManager) archive.getBean("mcpManager");
		try {

	        String method = inReq.getRequest().getMethod();
	         if ("GET".equalsIgnoreCase(method)) {
	        	// /sse/somekey
	        	manager.createConnection(archive,inReq);
	        	//manager.staConnection(archive,inReq);
	        	//This won't ever return - it stays alive indefinitely
 	            return;    

	         }
	        // Handle SSE connect (GET)	       

	        if ("POST".equalsIgnoreCase(method)) {

	        	JSONObject payload = (JSONObject) inReq.getJsonRequest();
	        	HttpServletRequest request = inReq.getRequest();
	        	String mcpSessionId = request.getHeader("mcp-session-id");
	        	if (mcpSessionId == null || mcpSessionId.isEmpty()) {
	        		mcpSessionId = request.getParameter("sessionId");
	        	}

	        	String cmd = payload == null ? null : (String) payload.get("method");

	        	if ("initialize".equals(cmd)) {
	        		if (mcpSessionId == null || mcpSessionId.isEmpty()) {
	        			mcpSessionId = manager.createSessionId();
	        		}
	        		inReq.getResponse().setHeader("mcp-session-id", mcpSessionId);

	        		Object id = payload != null ? payload.get("id") : null;
	        		String response = new JsonRpcResponseBuilder(id)
	        			.withServer("eMedia Live")
	        			.build();

					McpConnection connection = manager.getConnection(mcpSessionId);
        			inReq.getResponse().setStatus(HttpServletResponse.SC_ACCEPTED);
	        		if (connection != null) 
					{
	        			connection.sendMessage(response);
	        		}

	        		inReq.getResponse().flushBuffer();
			       	inReq.setCancelActions(true);
			       	inReq.setHasRedirected(true);

	        		return;
	        	}

	        	if (payload == null || cmd == null || cmd.isEmpty())
	        	{
	        		writeJsonError(inReq, HttpServletResponse.SC_BAD_REQUEST, null, "Missing JSON-RPC method.");
	        		return;
	        	}

	        	if (mcpSessionId == null || mcpSessionId.isEmpty())
	        	{
	        		writeJsonError(inReq, HttpServletResponse.SC_BAD_REQUEST, payload.get("id"), "Missing MCP session id.");
	        		return;
	        	}

					McpConnection connection = manager.getConnection(mcpSessionId);
	        	if (connection == null)
	        	{
	        		writeJsonError(inReq, HttpServletResponse.SC_CONFLICT, payload.get("id"), "No active MCP connection for session.");
	        		return;
	        	}

	        	if (mcpSessionId != null && !mcpSessionId.isEmpty()) {
	        		inReq.getResponse().setHeader("mcp-session-id", mcpSessionId);
	        	}
	        	inReq.getResponse().setStatus(HttpServletResponse.SC_ACCEPTED);
				manager.handleCall(inReq, connection, cmd, payload);
				
	        	if (cmd != null && cmd.startsWith("notifications/")) {
	        	}

	        	// Default for stream POST requests handled asynchronously over SSE.
	        	inReq.getResponse().flushBuffer();
		       	inReq.setCancelActions(true);
		       	inReq.setHasRedirected(true);

	        	return;
	        }
			
	        


	    }
	    catch (Exception ex) {
	        log.error("Error in MCP stream", ex);
	        throw new OpenEditException("Error in MCP stream", ex);
	    }
	}

	protected void writeJsonError(WebPageRequest inReq, int inStatusCode, Object inId, String inMessage) throws Exception
	{
		String response = new JsonRpcResponseBuilder(inId)
				.withResponse(inMessage, true)
				.build();
		inReq.getResponse().setStatus(inStatusCode);
		inReq.getResponse().setContentType("application/json");
		inReq.getResponse().getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));
		inReq.getResponse().flushBuffer();
		inReq.setCancelActions(true);
		inReq.setHasRedirected(true);
	}

	@Override
	public boolean canGenerate(WebPageRequest inReq)
	{
		return true;
	}

	@Override
	public String getName()
	{
		return fieldName;
	}

	@Override
	public void setName(String inName)
	{
		fieldName = inName;
	}

	@Override
	public boolean hasGenerator(Generator inChild)
	{
		return false;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}
}
