package org.entermediadb.llm;

import org.json.simple.JSONObject;
import org.openedit.data.BaseData;

public abstract class BaseLLMResponse  implements LLMResponse{
    
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
