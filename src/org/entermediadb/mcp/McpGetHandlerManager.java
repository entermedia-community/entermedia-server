package org.entermediadb.mcp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.openedit.WebPageRequest;

public class McpGetHandlerManager
{
	protected Map fieldGetHandlers;

	public Map<String,McpGetHandler> getGetHandlers()
	{
		if (fieldGetHandlers == null)
		{
			fieldGetHandlers = new HashMap();
		}

		return fieldGetHandlers;
	}

	public McpGetHandler loadGetHandler(WebPageRequest inReq)
	{
		String sessionid = inReq.getRequest().getHeader("mcp-session-id");
		
		McpGetHandler connection = getGetHandlers().get(sessionid);
		
		if( connection == null)
		{
			sessionid = UUID.randomUUID().toString();
			connection = new McpGetHandler();
			connection.setMcpSessionId(sessionid);
			connection.setReq(inReq);
			getGetHandlers().put(sessionid,connection);  //TODO: Make these expire? 
		}
		return connection;
	}
}
