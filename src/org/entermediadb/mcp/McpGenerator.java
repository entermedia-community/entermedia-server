package org.entermediadb.mcp;

import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.llm.VelocityRenderUtil;
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

	        		Object id = payload.get("id");
	        		String response = new JsonRpcResponseBuilder(id)
	        			.withServer("eMedia Live")
	        			.build();

					McpConnection connection = manager.getConnection(mcpSessionId);
	        		if (connection != null) 
					{
	        			connection.sendMessage(response);
	        			inReq.getResponse().setStatus(HttpServletResponse.SC_ACCEPTED);
	        		} else {
	        			// POST-first flow: return initialize payload directly.
	        			inReq.getResponse().setStatus(HttpServletResponse.SC_OK);
	        			inReq.getResponse().setContentType("application/json");
	        			byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
	        			inReq.getResponse().getOutputStream().write(bytes);
	        		}

	        		inReq.getResponse().flushBuffer();
			       	inReq.setCancelActions(true);
			       	inReq.setHasRedirected(true);

	        		return;
	        	}

				McpConnection connection = manager.getConnection(mcpSessionId);

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
	        log.error("Error in MCP stream- Client likely closed" );
	        ex.printStackTrace();
	     
	      //  throw new OpenEditException("Error in MCP stream", ex);
	    }
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
