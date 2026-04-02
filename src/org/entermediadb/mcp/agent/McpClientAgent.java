import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.mcp.client.McpClient;
import org.openedit.Data;

public class McpClientAgent extends BaseAgent
{
    private static final Log log = LogFactory.getLog(McpClientAgent.class);
    
    protected McpClient fieldClient;
    
    public McpClientAgent()
    {
       // fieldClient = new McpClient("un.org");
    }
    
    public McpClient getClient(AgentEnabled inEnabledAgent)
    {
        if( fieldClient == null)
        {
            String serverid = inEnabledAgent.getAgentConfig().get("aiserver");

            Data server = getMediaArchive().getData("aiservers", serverid);

            fieldClient = new McpClient();
            fieldClient.setServerUrl(server.get("url"));
            fieldClient.setApiKey(server.get("apikey"));
        }
        return fieldClient;
    }
@Override
    public void process(AgentContext inContext) 
    {
        McpClient client  = getClient( inContext.getCurrentAgentEnable());
        
        String  operation = inContext.getCurrentAgentEnable().getAgentConfig().get("runoperation");
        client.calltool(operation, inContext);

        //String operation = structure.get("operation");

        // Map response = fieldClient.sendRequest(operation, {keyword: dog});

        // TODO Auto-generated method stub
        super.process(inContext);
    }

}