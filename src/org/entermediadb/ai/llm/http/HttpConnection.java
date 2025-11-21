package org.entermediadb.ai.llm.http;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.llm.BaseLlmConnection;

public class HttpConnection extends BaseLlmConnection {
	private static Log log = LogFactory.getLog(HttpConnection.class);

	@Override
	public String getLlmProtocol()
	{
		return "http";
	}
	
}
