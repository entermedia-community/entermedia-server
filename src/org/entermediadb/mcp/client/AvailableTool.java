package org.entermediadb.mcp.client;

public class AvailableTool {

    protected String toolId;
    protected String toolName;

    public AvailableTool(String name, String description) {
        //TODO Auto-generated constructor stub
        this.toolId = name;
        this.toolName = description;    
    }

    public String getToolId() {
        return toolId;
    }

    public void setToolId(String toolId) {
        this.toolId = toolId;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

}
