package org.entermediadb.mcp.server;

import org.json.simple.JSONObject;

public class McpRequest {
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
