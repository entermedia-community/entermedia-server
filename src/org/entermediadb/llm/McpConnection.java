package org.entermediadb.llm;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.users.User;

public class McpConnection implements Runnable
{
	private static final Log log = LogFactory.getLog(McpConnection.class);

	private final WebPageRequest req;
	private final OutputStream out;
	private volatile boolean active = true;
	protected User fieldUser;

	public User getUser()
	{
		return fieldUser;
	}

	public void setUser(User inUser)
	{
		fieldUser = inUser;
	}

	public McpConnection(WebPageRequest inReq)
	{
		this.req = inReq;
		HttpServletResponse res = inReq.getResponse();

		res.resetBuffer();
		res.setStatus(HttpServletResponse.SC_OK);
		res.setContentType("text/event-stream");
		res.setCharacterEncoding("UTF-8");
		res.setHeader("Cache-Control", "no-cache");
		res.setHeader("Connection", "keep-alive");

		inReq.setCancelActions(true);
		inReq.setHasRedirected(true);

		try
		{
			this.out = res.getOutputStream();
		}
		catch (IOException e)
		{
			throw new OpenEditException("Failed to get SSE output stream", e);
		}
	}

	public void openStream(String inEndpoint)
	{
		String sessionId = getSessionId();
		log.info("Opening  ID Was: + " + sessionId);

		try
		{
			writeRaw(": connected\n\n");

			String postUrl = inEndpoint + "?sessionId=" + sessionId;
			writeRaw("event: endpoint\n");
			writeRaw("data: " + postUrl + "\n\n");

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

			writeRaw("event: " + eventName + "\n");
			writeRaw("data: " + cleanJson + "\n\n");
		}
		catch (IOException e)
		{
			active = false;
			log.error("Client disconnected or SSE send failed", e);
			close();
		}
		catch (Exception e)
		{
			log.error("Failed to send SSE event '" + eventName + "' for session: " + getSessionId(), e);
		}
	}

	private void writeRaw(String text) throws IOException
	{
		byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
		out.write(bytes);
		out.flush(); // this will throw IOException if the client has disconnected
	}

	public void sendStatus(String status)
	{
		String json = "{\"status\":\"" + status + "\"}";
		sendEvent("status", json);
	}

	public void sendPing()
	{
		// Optionally send keep-alive event
		// writeRaw("event: ping\ndata: {}\n\n");
	}

	public void close()
	{
		active = false;
		try
		{
			out.close();
		}
		catch (IOException e)
		{
			log.error("Error closing SSE stream for session: " + getSessionId(), e);
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
