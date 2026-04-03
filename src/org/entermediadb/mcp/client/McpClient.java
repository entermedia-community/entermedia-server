package org.entermediadb.mcp.client;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.UUID;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.jsonrpc.JsonRpcScanner;
import org.entermediadb.mcp.server.McpRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.OpenEditException;
import org.openedit.util.HttpSharedConnection;
import org.openedit.util.JSONParser;

/**
 * Make a Java client that can call any compatible MCP REST Endpoints. This will be used by the MCP UI to get the list of tools and other information about the client.
 */

public class McpClient
{
    protected static final long DEFAULT_RESPONSE_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(30);
    protected final Map<String, JSONObject> fieldRecentResponses = new HashMap<>();
    protected final List<JSONObject> fieldRecentEvents = new ArrayList<>();  //Use a thread-safe list if you expect concurrent access, or synchronize access to this list.
    protected final AtomicLong fieldRequestCounter = new AtomicLong();
    protected String fieldServerUrl;
    protected String fieldApiKey;
    protected HttpSharedConnection fieldConnection;
    protected Collection<AvailableTool> fieldTools;
    protected String fieldSessionId;

    public String getApiKey()
    {
        return fieldApiKey;
    }

    public void setApiKey(String inApiKey)
    {
        fieldApiKey = inApiKey;
    }

    public String getServerUrl()
    {
        if (fieldServerUrl == null || fieldServerUrl.isEmpty())
        {
            throw new OpenEditException("MCP server URL is not set.");
        }
        return fieldServerUrl;
    }

    public void setServerUrl(String inServerUrl)
    {
        fieldServerUrl = inServerUrl;
    }

    public HttpSharedConnection getConnection()
    {
        if (fieldConnection == null)
        {
            fieldConnection = new HttpSharedConnection();
            if (getApiKey() != null && !getApiKey().isEmpty())
            {
                fieldConnection.addSharedHeader("Api-Key", getApiKey());
            }
        }
        return fieldConnection;
    }

    public void connectToServer() throws Exception
    {
        ensureSessionId();
        startListeningInBackground();
        sendInit();
    }

    protected void ensureSessionId()
    {
        if (fieldSessionId == null || fieldSessionId.isEmpty())
        {
            fieldSessionId = UUID.randomUUID().toString();
            getConnection().addSharedHeader("mcp-session-id", fieldSessionId);
        }
    }
    
    public void startListeningInBackground()
    {
       listener = new Thread("mcp-client-listener") {
            public void run() {
                   startListening();
            }
        };
        listener.setDaemon(true);
        listener.start();
    }

    public void startListening()
    {
        //This is a simple client that just sends a request and waits for the response. It doesn't maintain a persistent connection like the server does.
        //In a real implementation, you might want to use WebSockets or Server-Sent Events to maintain a live connection and receive asynchronous notifications from the server.
        //getConnection().sharedGet(getServerUrl() ,);
        //Loop over in a thread 
        CloseableHttpResponse response = getConnection().sharedGet(getServerUrl());
        try
        {
            if (response.getStatusLine().getStatusCode() != 200)
            {
                throw new OpenEditException("Failed to connect to MCP server: " + response.getStatusLine());
            }
            InputStream input = response.getEntity().getContent();
            // Read from the input stream and process events as they come in. This is a simplified example and doesn't handle all edge cases or the full SSE protocol.
            // In a real implementation, you would want to handle reconnection logic, parse the SSE format properly, and manage the lifecycle of the connection more robustly.
            JsonRpcScanner scanner = new JsonRpcScanner(input, "UTF-8");
                while (true)
                {
                    JSONObject event = scanner.nextEvent();
                    eventReceived(event);
                }
        }
         catch (Exception e)
        {
            throw new OpenEditException("Error while listening to MCP server", e);
        }
        finally 
        {     
            getConnection().release(response); // Close the response to free resources. In a real implementation, you would keep the connection open and listen for events.  

        }

    }

    protected Thread listener;

    public void disconnect()
    {
        if (fieldConnection != null)
        {
            try
            {
                if (listener != null)
                {
                    listener.interrupt();
                }
                synchronized (fieldRecentResponses)
                {
                    fieldRecentResponses.notifyAll();
                }
                fieldConnection = null;
            }
            catch (Exception e)
            {
                // Log and ignore
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void sendInit()
    {
        ensureSessionId();
        String messageid = "init01";
        JSONObject body = new JSONObject();
        body.put("jsonrpc", "2.0");
        body.put("id", messageid);
        body.put("method", "initialize");
        //body.put("params", new JSONObject(inParams));

        //Post to get the Header back
        CloseableHttpResponse response = getConnection().sharedPostWithJson(getServerUrl(), body);
        try
        {
            int status = response.getStatusLine().getStatusCode();
            if (status != 202 && status != 200)
            {
                throw new OpenEditException("Unexpected response from MCP server: " + response.getStatusLine());
            }
            if (response.getFirstHeader("mcp-session-id") != null)
            {
                fieldSessionId = response.getFirstHeader("mcp-session-id").getValue();
            }
        }
        finally
        {
            getConnection().release(response);
        }

    }

    /**
     * Get a list of tool names from this MCP server.
     */
    @SuppressWarnings("unchecked")
    public McpRequest sendRequest(String method, Map<String, Object> inParams)
    {
        String messageid = "request-" + fieldRequestCounter.incrementAndGet();
        JSONObject body = new JSONObject();
        body.put("jsonrpc", "2.0");
        body.put("id", messageid);
        body.put("method", method);
        body.put("params", new JSONObject(inParams));

        CloseableHttpResponse response = getConnection().sharedPostWithJson(getServerUrl(), body);
        try
        {
            int status = response.getStatusLine().getStatusCode();
            if (status != 202 && status != 200)
            {
                throw new OpenEditException("Unexpected response from MCP server: " + response.getStatusLine());
            }

            String result = getConnection().parseText(response);
            McpRequest mcpResponse = new McpRequest();
            mcpResponse.setMessageId(messageid);
            mcpResponse.setTextReply(result);
            mcpResponse.setJsonReply(parseJson(result));
            return mcpResponse;
        }
        finally
        {
            getConnection().release(response);
        }
    }

    protected JSONObject parseJson(String inText)
    {
        if (inText == null || inText.isEmpty())
        {
            return null;
        }
        try
        {
            return (JSONObject) new JSONParser().parse(inText);
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    public JSONObject waitForResponse(McpRequest inRequest)
    {
        return waitForResponse(inRequest, DEFAULT_RESPONSE_TIMEOUT_MS);
    }

    public JSONObject waitForResponse(McpRequest inRequest, long inTimeoutMs)
    {
        long timeoutMs = Math.max(1, inTimeoutMs);
        long deadline = System.currentTimeMillis() + timeoutMs;

        synchronized (fieldRecentResponses)
        {
            while (!fieldRecentResponses.containsKey(inRequest.getMessageId()))
            {
                try
                {
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0)
                    {
                        throw new OpenEditException("Timed out waiting for MCP response: " + inRequest.getMessageId());
                    }
                    fieldRecentResponses.wait(Math.min(remaining, 1000));
                }
                catch (InterruptedException ex)
                {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            JSONObject found = fieldRecentResponses.remove(inRequest.getMessageId());
            return found;
        }
    }

    public void addData(String inKey, JSONObject inValue)
    {
        synchronized (fieldRecentResponses)
        {
            fieldRecentResponses.put(inKey, inValue);
            fieldRecentResponses.notifyAll();
        }
    }

    public void eventReceived(JSONObject inEvent)
    {
        if (inEvent == null)
        {
            return;
        }

        synchronized (fieldRecentEvents)
        {
            fieldRecentEvents.add(inEvent);
            fieldRecentEvents.notifyAll();
        }

        Object id = inEvent.get("id");
        if (id != null)
        {
            addData(String.valueOf(id), inEvent);
        }
    }

    public Collection<JSONObject> getRecentEvents()
    {
        synchronized (fieldRecentEvents)
        {
            return Collections.unmodifiableList(new ArrayList<>(fieldRecentEvents));
        }
    }
    Collection<AvailableTool> fieldAvailableTools = null;

    public Collection<AvailableTool> getAvailableTools() 
    {
        if( fieldAvailableTools != null)
        {
            return fieldAvailableTools;
        }
        McpRequest request = sendRequest("tools/list", new HashMap<>());
        JSONObject response = waitForResponse(request);
        if (response != null)
        {
            JSONObject result = (JSONObject) response.get("result");
            JSONArray toolsArray = result != null ? (JSONArray) result.get("tools") : (JSONArray) response.get("tools");
            if (toolsArray != null) {
                List<AvailableTool> tools = new ArrayList<>();
                for (Object toolObj : toolsArray) {
                    JSONObject toolJson = (JSONObject) toolObj;
                    String name = (String) toolJson.get("name");
                    String description = (String) toolJson.get("description");
                    tools.add(new AvailableTool(name, description));
                }
                fieldAvailableTools = tools;
                return tools;
            }
        }
        return Collections.emptyList();
    }

     public void callAsyncTool(String operation, AgentContext inContext)
    {
        //Send a call, no response
         String callId = "async-" + fieldLastToolCallId++;
        Map<String, Object> params = new HashMap<>();
        params.put("operation", operation);
        params.put("callId", callId);
        //Add more params from context as needed            
        sendRequest(operation, params);

    }
    int fieldLastToolCallId = 0;

    public JSONObject calltool(String operation, AgentContext inContext)
    {
        String callId = "tool-" + fieldLastToolCallId++;
        Map<String, Object> params = new HashMap<>();
        params.put("operation", operation);
        params.put("callId", callId);
        //Add more params from context as needed            
        McpRequest request =sendRequest(operation, params);

        JSONObject response = waitForResponse(request);
        //Send a call, wait for response?
        return response;
    }
}
