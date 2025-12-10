package org.entermediadb.ai.llm.http;


import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.entermediadb.ai.llm.BaseLlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.net.HttpSharedConnection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.OpenEditException;

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
