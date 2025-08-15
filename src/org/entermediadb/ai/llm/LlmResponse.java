package org.entermediadb.ai.llm;

import java.util.ArrayList;

import org.json.simple.JSONObject;

public interface LlmResponse {

	
	
    JSONObject getRawResponse();
    
    boolean isToolCall();
    
    JSONObject getArguments();
    
    String getMessage();
    
    String getFunctionName();
    
    boolean isSuccessful();
    
    int getTokensUsed();
    
    String getModel();
    ArrayList getImageUrls();

    
    
}
