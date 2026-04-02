package org.entermediadb.mcp;

import org.json.simple.JSONObject;

public class McpResponse {
    String fieldMessageId;
    JSONObject fieldJsonReply;
    String fieldTextReply;

    public String getMessageId() {
        return fieldMessageId;
    }
    public void setMessageId(String inMessageId) {
        fieldMessageId = inMessageId;   
    }   
    public JSONObject getJsonReply() {
        return fieldJsonReply;
    }
    public void setJsonReply(JSONObject inJsonReply) {
        fieldJsonReply = inJsonReply;
    }

    public String getTextReply() {
        return fieldTextReply;
    }
    public void setTextReply(String inTextReply) {
        fieldTextReply = inTextReply;
    }
    
}
