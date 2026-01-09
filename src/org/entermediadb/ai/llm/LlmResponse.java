package org.entermediadb.ai.llm;

import java.util.ArrayList;
import java.util.Collection;

import org.json.simple.JSONObject;

public interface LlmResponse {

    JSONObject getMessageStructured();
	
    JSONObject getRawResponse();
	void setRawResponse(JSONObject inObj);
    
    Collection getRawCollection();
	void setRawCollection(Collection inObj);
	
	Collection getCollection(String inKey);
    
    boolean isToolCall();
    
    String getMessage();
    void setMessage(String inMessage);
    
    String getMessagePlain();    
    void setMessagePlain(String inMessagePlain);

    String getFunctionName();

    void setFunctionName(String inFunction);

    boolean isSuccessful();
    
    int getTokensUsed();
    
    String getModel();
   
    ArrayList<String> getImageUrls();
    
    ArrayList<String> getImageBase64s();    
    
    String getFileName();

}
