package org.entermediadb.ai.llm.http;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.llm.BaseLlmConnection;
import org.entermediadb.ai.llm.LlmResponse;

public class HttpConnection extends BaseLlmConnection 
{
	private static Log log = LogFactory.getLog(HttpConnection.class);

	@Override
	public String getLlmProtocol()
	{
		return "http";
	}

	@Override
	public LlmResponse createResponse()
	{
		return new HttpResponse();
	}


}
