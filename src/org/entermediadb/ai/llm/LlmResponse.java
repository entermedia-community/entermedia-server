package org.entermediadb.ai.llm;

import java.util.ArrayList;

import org.json.simple.JSONObject;

public interface LlmResponse {

    JSONObject getArguments();
	
    JSONObject getRawResponse();
    
    boolean isToolCall();
    
    String getMessage();
    
    String getMessagePlain();

    String getFunctionName();

    boolean isSuccessful();
    
    int getTokensUsed();
    
    String getModel();
   
    ArrayList<String> getImageUrls();
    
    ArrayList<String> getImageBase64s();    
    
    String getFileName();
}
