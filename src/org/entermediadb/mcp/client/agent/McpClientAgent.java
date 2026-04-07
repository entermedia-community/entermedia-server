package org.entermediadb.mcp.client.agent;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.mcp.client.McpClient;
import org.openedit.Data;

public class McpClientAgent extends BaseAgent {
    protected McpClient fieldClient;

    public McpClientAgent() {
        // fieldClient = new McpClient("un.org");
    }

    public McpClient getClient(AgentEnabled inEnabledAgent) {
        if (fieldClient == null) {
            String serverid = inEnabledAgent.getAgentData().get("aiserver");

            Data server = getMediaArchive().getData("aiservers", serverid);

            fieldClient = new McpClient();
            fieldClient.setServerUrl(server.get("url"));
            fieldClient.setApiKey(server.get("apikey"));
        }
        return fieldClient;
    }

    @Override
    public void process(AgentContext inContext) {
        McpClient client = getClient(inContext.getCurrentAgentEnable());

        String operation = inContext.getCurrentAgentEnable().getAgentData().get("runoperation");
        client.calltool(operation, inContext);

        // String operation = structure.get("operation");

        // Map response = fieldClient.sendRequest(operation, {keyword: dog});

        // TODO Auto-generated method stub
        super.process(inContext);
    }

}