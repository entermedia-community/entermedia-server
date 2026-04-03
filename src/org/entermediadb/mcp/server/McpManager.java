package org.entermediadb.mcp.server;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.llm.VelocityRenderUtil;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.jsonrpc.JsonRpcResponseBuilder;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;

public class McpManager implements CatalogEnabled {
    private static final Log log = LogFactory.getLog(McpManager.class);

    protected ModuleManager fieldModuleManager;
    protected VelocityRenderUtil fieldRender;
    protected String fieldCatalogId;
    protected Map<String, McpConnection> connections = new ConcurrentHashMap<>();
   
    protected McpGetHandlerManager fieldMcpGetHandlerManager;

	public McpGetHandlerManager getMcpGetHandlerManager()
	{
		if (fieldMcpGetHandlerManager == null)
		{
			fieldMcpGetHandlerManager = new McpGetHandlerManager();
		}
		return fieldMcpGetHandlerManager;
	}

	public void setMcpGetHandlerManager(McpGetHandlerManager inMcpGetHandlerManager)
	{
		fieldMcpGetHandlerManager = inMcpGetHandlerManager;
	}

	public ModuleManager getModuleManager() {
        return fieldModuleManager;
    }

    public void setModuleManager(ModuleManager inModuleManager) {
        fieldModuleManager = inModuleManager;
    }

    public VelocityRenderUtil getRenderUtil() {
        return fieldRender;
    }

    public void setRenderUtil(VelocityRenderUtil inRender) {
        fieldRender = inRender;
    }

    public String getCatalogId() {
        return fieldCatalogId;
    }

    public void setCatalogId(String inCatalogId) {
        fieldCatalogId = inCatalogId;
    }

    /**
     * Opens a new SSE connection for the session in inReq if none exists.
     */
    public McpConnection createConnection(MediaArchive inArchive,WebPageRequest inReq) {
        String requestedSessionId = inReq.getRequest().getHeader("mcp-session-id");
        if (requestedSessionId == null || requestedSessionId.isEmpty())
        {
            requestedSessionId = inReq.getRequest().getParameter("sessionId");
        }

        String sessionId = (requestedSessionId != null && !requestedSessionId.isEmpty()) ? requestedSessionId : createSessionId();
        String endpoint = inReq.findPathValue("mcp-endpoint");
        //This is something like /sse/userkey
        String key = inReq.getPage().getPageName();
        McpConnection stale = connections.remove(sessionId);
        if (stale != null)
        {
            stale.close();
            log.info("Replacing stale MCP connection for session: " + sessionId);
        }

        McpConnection conn = new McpConnection(inReq);
        conn.setSessionId(sessionId);
        connections.put(sessionId, conn);
        conn.connect();
        conn.openStream(endpoint);

        conn.setKey(key);
        
        Data row = inArchive.query("appkeys").exact("key", key).searchOne();
        if(row != null) {
    		String userid = row.get("user");
    		User user = inArchive.getUser(userid);	        	
    		conn.setUser(user);
    	}

        log.info("Created MCP connection for session: " + sessionId);

        try
        {
            // this blocks until conn.active == false
            conn.run();
        }
        finally
        {
            // guarantee we remove it—even on exceptions
            connections.remove(sessionId);
            log.info("Cleaned up MCP connection for session: " + sessionId);
            // ensure the socket is closed if not already
            if (conn.isActive())
            {
                conn.close();
            }
        }

        return conn;
        
    }

    public void handleCall(WebPageRequest inReq, McpConnection inConnection, String cmd, JSONObject payload) throws Exception{
        if (inConnection == null)
        {
            throw new OpenEditException("No active MCP connection for command: " + cmd);
        }

        Object id = payload != null ? payload.get("id") : null;

        inReq.putPageValue("id", id);
       	String appid = inReq.findPathValue("applicationid");
        UserProfile profile = inReq.getUserProfile();
        JSONObject params = payload != null ? (JSONObject) payload.get("params") : null;
        String response;

        if ("logging/setLevel".equals(cmd))
        {
            response = new JsonRpcResponseBuilder(id)
                    .withServer("eMedia Live")
                    .build();
        }
        else if ("tools/list".equals(cmd))
        {
                    if (profile == null)
                    {
                        response = new JsonRpcResponseBuilder(id)
                                .withResponse("Authentication failed! User profile not found.", true)
                                .build();
                    }
                    else
                    {
                        String fp = "/" + appid + "/ai/mcp/method/tools/list.json";
                        inReq.putPageValue("modules", profile.getEntities());

                        String toolsArrString = getRenderUtil().loadInputFromTemplate(inReq, fp);

                        response = new JsonRpcResponseBuilder(id)
                                .withToolsList(toolsArrString)
                                .build();
                    }
        }
        else if ("tools/call".equals(cmd))
        {
                    String functionname = params != null ? (String) params.get("name") : null;
                    if (functionname == null || functionname.isEmpty())
                    {
                        response = new JsonRpcResponseBuilder(id)
                                .withResponse("Invalid tools/call request. Missing tool name.", true)
                                .build();
                    }
                    else
                    {
                        String siteid = inReq.findValue("siteid");
                        inReq.putPageValue("mcpapplicationid", siteid + "/find");
                        String fp = "/" + appid + "/ai/mcp/functions/" + functionname + ".html";

                        String text = getRenderUtil().loadInputFromTemplate(inReq, fp);

                        text = text.replaceAll("(?m)^\\s*$\\n?", "");
                        text = text.replaceAll("(\\r?\\n){2,}", "\n");

                        response = new JsonRpcResponseBuilder(id)
                                .withResponse(text, false)
                                .build();
                    }
        }
        else
        {
            log.info("Called " + cmd); //"notifications/initialized"
            response = new JsonRpcResponseBuilder(id)
                    .withResponse("CMD Received " + cmd, false)
                    .build();
        }

        inConnection.sendMessage(response);
		//inReq.getResponse().getOutputStream().write(response.getBytes());  //This should chunk it up
		
		//inReq.getPageStreamer().getOutput().getWriter().write(response);
		//inReq.getResponse().flushBuffer();
    }

    public String createSessionId()
    {
        return UUID.randomUUID().toString();
    }

    /**
     * Retrieves the existing connection for the session in inReq, or null if none.
     */
    public McpConnection getConnection(String sessionId) 
    {
    	
        //String sessionId = inReq.findValue("sessionId");
        return connections.get(sessionId);
    }

    public McpGetHandler loadGetHandler(WebPageRequest inReq) 
    {
    	McpGetHandler handler = getMcpGetHandlerManager().loadGetHandler(inReq);
    	return handler;
    }

    /**
     * Removes and closes the connection for the given session ID.
     */
    public void removeConnection(String inSessionId) {
        McpConnection conn = connections.remove(inSessionId);
        if (conn != null) {
            conn.close();
            log.info("Removed MCP connection for session: " + inSessionId);
        }
    }

    /**
     * Scans and removes any inactive or expired connections.
     */
    public void cleanupExpiredConnections() {
        Iterator<Map.Entry<String, McpConnection>> it = connections.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, McpConnection> entry = it.next();
            if (!entry.getValue().isActive()) {
                it.remove();
                log.info("Cleaned up expired MCP connection for session: " + entry.getKey());
            }
        }
    }
}