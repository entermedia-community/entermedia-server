package org.entermediadb.mcp;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.util.OutputFiller;

public class McpGetHandler
{
	private static final Log log = LogFactory.getLog(McpGetHandler.class);

	OutputFiller filler = new OutputFiller();
	
	protected WebPageRequest fieldReq;
	public Writer getClientWriter()
	{
		return fieldClientWriter;
	}


	public void setClientWriter(Writer inClientWriter)
	{
		fieldClientWriter = inClientWriter;
	}


	public Reader getClientReader()
	{
		return fieldClientReader;
	}


	public void setClientReader(Reader inClientReader)
	{
		fieldClientReader = inClientReader;
	}
	protected boolean fieldActive;
	
	public boolean isActive()
	{
		return fieldActive;
	}


	public void setActive(boolean inActive)
	{
		fieldActive = inActive;
	}
	protected String fieldMcpSessionId;
	
	public String getMcpSessionId()
	{
		return fieldMcpSessionId;
	}


	public void setMcpSessionId(String inRandomSessionId)
	{
		fieldMcpSessionId = inRandomSessionId;
	}

	protected Writer fieldClientWriter;
	protected Reader fieldClientReader;
	
	public WebPageRequest getReq()
	{
		return fieldReq;
	}


	public void setReq(WebPageRequest inReq)
	{
		fieldReq = inReq;
	}


	//Just wait and listen for stuff.. Handle reconnect?
	public void listen()
	{
		HttpServletResponse res = getReq().getResponse();

		//res.resetBuffer(); //? Needed?
		res.setStatus(HttpServletResponse.SC_OK);
		res.setContentType("application/json");
		res.setHeader("mcp-session-id", getMcpSessionId()); 
		res.setCharacterEncoding("UTF-8");
		res.setHeader("Cache-Control", "no-cache");
		res.setHeader("Connection", "keep-alive");

		getReq().setCancelActions(true);
		getReq().setHasRedirected(true);

		try
		{
			Writer output = new OutputStreamWriter( getReq().getResponse().getOutputStream() );
			setClientWriter(output); //Send stuff these
			//Now block forever?
			try
			{
				while (isActive())
				{
					Thread.sleep(15000);
					sendNothing();
				}
			}
			catch (Exception e)
			{
				setActive( false );
				log.warn("Ping loop interrupted for session: " + getMcpSessionId(), e);
			}

			//Shut down?
		}
		catch (IOException e)
		{
			throw new OpenEditException("Failed to get SSE output stream", e);
		}
	}


	private void sendNothing() throws Exception
	{
		// TODO Auto-generated method stub
		JSONObject payload = new JSONObject();
		//payload.put("method", "initialize");
		payload.put("jsonrpc", "2.0");
		payload.put("result", new JSONObject());
		
		getClientWriter().write(payload.toJSONString());
		getClientWriter().flush();
	}
}
