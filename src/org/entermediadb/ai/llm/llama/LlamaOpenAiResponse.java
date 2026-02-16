package org.entermediadb.ai.llm.llama;

import org.entermediadb.ai.llm.openai.OpenAiResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.util.JSONParser;

public class LlamaOpenAiResponse extends OpenAiResponse {
	
	@Override
    public boolean isToolCall() {
        if (rawResponse == null) 
    	{
        	return false;
    	}

        JSONArray choices = (JSONArray) rawResponse.get("choices");
        if (choices == null || choices.isEmpty()) 
        {
        	return false;
        }

        JSONObject choice = (JSONObject) choices.get(0);
        JSONObject message = (JSONObject) choice.get("message");

        return message != null && message.get("tool_calls") != null;
    }
	
    @Override
    public JSONObject getMessageStructured() {

        JSONArray choices = (JSONArray) rawResponse.get("choices");
        JSONObject choice = (JSONObject) choices.get(0);
        JSONObject message = (JSONObject) choice.get("message");
        
        String contentString = (String) message.get("content");
        
        JSONParser parser = new JSONParser();

        JSONObject content = parser.parse(contentString);

        return content;
    }
    
    protected String ocrResponse;
    
    public String getOcrResponse() {    	
    	return ocrResponse;
    }
    
    public void setOcrResponse(JSONObject inRawResponse) {
    	JSONArray choices = (JSONArray) inRawResponse.get("choices");
        JSONObject choice = (JSONObject) choices.get(0);
        JSONObject message = (JSONObject) choice.get("message");
        
        ocrResponse = (String) message.get("content");
	}
    
    @Override
    public String getFunctionName() 
    {
    	if( fieldFunctionName != null)
    	{
    		return fieldFunctionName;
    	}
    	
        if (!isToolCall()) return null;
        
        try
        {
        	
        	JSONArray choices = (JSONArray) rawResponse.get("choices");
        	JSONObject choice = (JSONObject) choices.get(0);
        	JSONObject message = (JSONObject) choice.get("message");
        	JSONArray functionCalls = (JSONArray) message.get("tool_calls");
        	
        	JSONObject functionCall = (JSONObject) functionCalls.get(0);
        	JSONObject function = (JSONObject) functionCall.get("function");
        	
        	return function != null ? (String) function.get("name") : null;
        }
        catch( Exception ex)
        {
        	return null;
        }

    }

}
