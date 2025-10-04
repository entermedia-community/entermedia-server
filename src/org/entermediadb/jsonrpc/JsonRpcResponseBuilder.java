package org.entermediadb.jsonrpc;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.util.JSONParser;

public class JsonRpcResponseBuilder {
	final private String version = "2.0";
	private Object id;
    private JSONObject result;
    
    final private String[] capabilities = {"tools", "prompts", "resources"};
    
    final JSONParser parser = new JSONParser();

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
    
    public JsonRpcResponseBuilder withToolsList(String toolsStr) throws Exception  {
    	JSONObject result = new JSONObject();
    	
    	try {
    		JSONArray toolsArray = (JSONArray) parser.parseCollection(toolsStr);
    		result.put("tools", toolsArray);
		} catch (Exception e) {
			result.put("text", "Invalid JSON Array in tools/list.html");
			result.put("isError", true);
		}
    	
    	this.result = result;
    	
    	return this;
    }

    public JsonRpcResponseBuilder withResponse(String text, Boolean isError)
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
