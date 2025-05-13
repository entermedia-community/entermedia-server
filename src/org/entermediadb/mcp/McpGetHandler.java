package org.entermediadb.mcp;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import javax.servlet.http.HttpServletResponse;

import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.util.OutputFiller;

public class McpGetHandler
{
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
		res.setStatus(202); 		
		//res.setStatus(HttpServletResponse.SC_OK);
		res.setContentType("text/event-stream");
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
			while(true)
			{
				//filler.fill(fieldClientReader, output)
				getReq().getRequest().getInputStream().read(); //This should block forever?
				
			}
			//Shut down?
		}
		catch (IOException e)
		{
			throw new OpenEditException("Failed to get SSE output stream", e);
		}
	}
}
