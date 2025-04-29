package org.entermediadb.llm;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.CatalogEnabled;
import org.openedit.ModuleManager;
import org.openedit.WebPageRequest;
import org.openedit.util.ExecutorManager;

public class McpManager implements CatalogEnabled {
    private static final Log log = LogFactory.getLog(McpManager.class);

    protected ModuleManager fieldModuleManager;
    protected VelocityRenderUtil fieldRender;
    protected String fieldCatalogId;
    protected Map<String, McpConnection> connections = new ConcurrentHashMap<>();
    
    public ModuleManager getModuleManager() {
        return fieldModuleManager;
    }

    public void setModuleManager(ModuleManager inModuleManager) {
        fieldModuleManager = inModuleManager;
    }

    public VelocityRenderUtil getRender() {
        return fieldRender;
    }

    public void setRender(VelocityRenderUtil inRender) {
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
    public McpConnection createConnection(WebPageRequest inReq) {
        String sessionId = inReq.getRequest().getSession().getId();
        if (connections.containsKey(sessionId)) {
            log.info("SSE connection already exists for session: " + sessionId);
            return getConnection(inReq);
        }
        String endpoint = inReq.findPathValue("mcp-endpoint");
        
        McpConnection conn = new McpConnection(inReq);
        connections.put(sessionId, conn);
        conn.openStream(endpoint);
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

    /**
     * Retrieves the existing connection for the session in inReq, or null if none.
     */
    public McpConnection getConnection(WebPageRequest inReq) {
        String sessionId = inReq.findValue("sessionId");
        return connections.get(sessionId);
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