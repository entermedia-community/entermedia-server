package org.entermediadb.mcp.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.openedit.WebPageRequest;

public class McpGetHandlerManager
{
	protected final Map<String, McpGetHandler> fieldGetHandlers = new ConcurrentHashMap<>();

	public Map<String,McpGetHandler> getGetHandlers()
	{
		return fieldGetHandlers;
	}

	public McpGetHandler loadGetHandler(WebPageRequest inReq)
	{
		String sessionid = inReq.getRequest().getHeader("mcp-session-id");
		
		McpGetHandler connection = getGetHandlers().get(sessionid);
		
		if(connection != null)
		{
			if(!connection.isActive())
			{
				connection.setActive(true);
			}
		}
		if( connection == null)
		{
			sessionid = UUID.randomUUID().toString();
			connection = new McpGetHandler();
			connection.setMcpSessionId(sessionid);
			getGetHandlers().put(sessionid,connection);  //TODO: Make these expire? 
		}
		else
		{
			connection.setReq(inReq);
		}
		return connection;
	}
}
