package org.entermediadb.jsonrpc;

import java.util.Collection; 

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

enum Capability {
	tools, prompts, resources
}

public class JsonRpcResponseBuilder {
	final private String version = "2.0";
	private Object id;
    private JSONObject result;
    private Object params;
    
    final private String[] capabilities = {"tools", "prompts", "resources"};

    public JsonRpcResponseBuilder(Object id) {
        this.id = id;
    }

    public JsonRpcResponseBuilder withServer(String serverName)
    {
    	JSONObject result = new JSONObject();
    	
    	result.put("protocolVersion", "2025-03-26");
    	
    	JSONObject serverInfo = new JSONObject();

    	serverInfo.put("name", serverName);
    	serverInfo.put("version", "1.0.0");
    			
        result.put("serverInfo", serverInfo);
    	
    	JSONObject capObj = new JSONObject();
    	
    	for (String capability : this.capabilities) {
			JSONObject innerObj = new JSONObject();
			innerObj.put("listChanged", true);
			capObj.put(capability, innerObj);
    	}
    	capObj.put("logging", new JSONObject());
    	
    	result.put("capabilities", capObj);
    	
    	this.result = result;
        
        return this;
    }
    
    private JSONObject getTool(String name, String description) {
    	return getTool(name, description, null);
    }
    
    private JSONObject getTool(String name, String description, JSONObject inputSchema) {
    	JSONObject tool = new JSONObject();
    	tool.put("name", name);
    	tool.put("description", description);
    	if(inputSchema != null)
    	{
    		tool.put("inputSchema", inputSchema);
    	}
    	
    	return tool;
    }
    
    public JsonRpcResponseBuilder withToolsList()
    {
        JSONObject result = new JSONObject();

        JSONArray tools = new JSONArray();
        
        tools.add(getTool("show_hint", "Show hints on how to properly use the search function and provide examples."
        		+ " Use this function when people ask questions like: 'What can you do?', 'How can I search using this"
        		+ " tool?', 'Give me example of some search queries'."));

        return this;

    }

    public JsonRpcResponseBuilder withToolResponse(String text, Boolean isError)
    {
    	JSONObject result = new JSONObject();
    	
    	JSONArray content = new JSONArray();
    	
    	JSONObject responseObj = new JSONObject();
    	responseObj.put("type", "text");
    	responseObj.put("text", text);
    	
    	content.add(responseObj);
    	
    	result.put("content", content);
    	result.put("isError", isError);
    	
    	this.result = result;
    	
    	return this;
    }

    public String build() {
        JSONObject json = new JSONObject();
        json.put("jsonrpc", version);
        json.put("id", id);

        if (result != null) 
        {
            json.put("result", result);
        }

        return json.toString();
    }
}
