package org.entermediadb.llm;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;

/**
 * Represents a single SSE connection for MCP. Handles SSE framing: initial
 * handshake, endpoint discovery, ping loop, and message sending.
 */
public class McpConnection implements Runnable
{
	private static final Log log = LogFactory.getLog(McpConnection.class);

	private final WebPageRequest req;
	private final Writer writer;
	private volatile boolean active = true;

	public McpConnection(WebPageRequest inReq)
	{
		this.req = inReq;
		HttpServletResponse res = inReq.getResponse();
		

			res.resetBuffer();              // clears the body only, not the status or headers
	        res.setStatus(HttpServletResponse.SC_OK);
	        res.setContentType("text/event-stream");
	        res.setCharacterEncoding("UTF-8");
	        res.setHeader("Cache-Control", "no-cache");
	        res.setHeader("Connection", "keep-alive");
		
			inReq.setCancelActions(true);		
			inReq.setHasRedirected(true);

		this.writer = inReq.getWriter();
	}

	/**
	 * Sends initial SSE framing: comment, endpoint event, then starts ping
	 * loop.
	 */
	public void openStream(String inEndpoint)
	{
		String sessionId = getSessionId();
		log.info("Opening  ID Was: + " + getSessionId() );

		try
		{
			// 1) Prime connection for proxies
			writer.write(": connected\n\n");
			writer.flush();

			// 2) Handshake: tell client where to POST JSON-RPC
		
			String postUrl = inEndpoint +  "?sessionId=" + sessionId;
			writer.write("event: endpoint\n");
			writer.write("data: " + postUrl + "\n\n");
			writer.flush();

			// 3) Send initial status
			sendStatus("open");

		}
		catch (IOException e)
		{
			active = false;
			log.error("Failed to open SSE stream for session: " + sessionId, e);
			throw new OpenEditException("Failed to open SSE stream", e);
		}
	}

	@Override
	public void run()
	{
		try
		{
			while (active)
			{
				Thread.sleep(15000);
				sendPing();
			}
		}
		catch (InterruptedException e)
		{
			active = false;
			log.warn("Ping loop interrupted for session: " + getSessionId(), e);
		}
		
	}

	/**
	 * Sends a JSON-RPC payload as an SSE "message" event.
	 */
	public synchronized void sendMessage(String jsonPayload)
	{
		sendEvent("message", jsonPayload);
	}

	private synchronized void sendEvent(String eventName, String data)
	{
		
		
		if (!active)
		{
			log.warn("Attempted to send on inactive connection for session: " + getSessionId());
			return;
		}
		try
		{
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(data);
			String cleanJson = ((JSONObject) obj).toJSONString();
			
			log.info("Session ID Was: + " + getSessionId() + " DATA: " + cleanJson);
			writer.write("event: " + eventName + "\n");
			writer.write("data: " + cleanJson + "\n\n");
			writer.flush();
			
		}
		catch (Exception e)
		{
			active = false;
			//log.error("Failed to send SSE event '" + eventName + "' for session: " + getSessionId(), e);
			throw new OpenEditException("Failed to send SSE event", e);
		}
	}

	/** Sends a status event (e.g., 'open', 'connected'). */
	public void sendStatus(String status)
	{
		String json = "{\"status\":\"" + status + "\"}";
		sendEvent("status", json);
	}

	/** Sends a ping event to keep the connection alive. */
	public void sendPing()
	{
		//sendEvent("ping", "{}");
	}

	/** Closes the SSE stream. */
	public void close()
	{
		active = false;
		try
		{
			writer.close();
		}
		catch (IOException e)
		{
			log.error("Error closing SSE writer for session: " + getSessionId(), e);
			throw new OpenEditException("Error closing SSE writer", e);
		}
	}

	public boolean isActive()
	{
		return active;
	}

	private String getSessionId()
	{
		return req.getRequest().getSession().getId();
	}

	
}
