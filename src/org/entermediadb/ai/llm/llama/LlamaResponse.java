package org.entermediadb.ai.llm.llama;

import org.entermediadb.ai.llm.openai.OpenAiResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.util.JSONParser;

public class LlamaResponse extends OpenAiResponse {
	
	
    @Override
    public JSONObject getArguments() {

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

}
