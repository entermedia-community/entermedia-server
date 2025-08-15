package org.entermediadb.ai.llm;

import org.json.simple.JSONObject;

public abstract class BaseLlmResponse  implements LlmResponse{
    
    protected JSONObject rawResponse;

    public JSONObject getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(JSONObject inRawResponse) {
        rawResponse = inRawResponse;
    }

    // Standardized methods for extracting LLM responses
    public abstract boolean isToolCall();
    
    public abstract JSONObject getArguments();
    
    public abstract String getMessage();
    
    public abstract String getFunctionName();

    public abstract boolean isSuccessful();

    public abstract int getTokensUsed();

    public abstract String getModel();

}
