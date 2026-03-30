package org.entermediadb.mcp;

import java.util.Collection;

import org.json.simple.JSONObject;
import org.openedit.util.HttpSharedConnection;

/**
 * Make a Java client that can call any compatible MCP REST Endpoints. This will be used by the MCP UI to get the list of tools and other information about the client.
 */

public class McpClient {
    HttpSharedConnection fieldConnection;
    public HttpSharedConnection getConnection() {
        if( fieldConnection == null ) {
            fieldConnection = new HttpSharedConnection();
            fieldConnection.addSharedHeader("Key", "123");
        }   
        return fieldConnection;
    }

    /**
     * Get a list of tools that are currently active for this client. This is used to determine which tools to show in the UI.
     * @return
     */
    public Collection<String> getTools () 
    {
        JSONObject body = new JSONObject();
        JSONObject params = new JSONObject();

        body.put("jsonrpc", "2.0");
        body.put("id", "req-001");
        body.put("method", "tools/list");
        body.put("action", "gettools");
        body.put("params", params);

        String server = "http://localhost:8888/mcp";    

        JSONObject response = getConnection().getJson(server, body);
        if( response == null ) {
            return null;
        }
        Collection<String> tools = (Collection<String>) response.get("tools");

        return tools;
    }
}
