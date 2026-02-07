package org.entermediadb.ai.llm.http;


import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.entermediadb.ai.llm.BaseLlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
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

	@Override
	public LlmResponse callLlamaIndexStructured(Map inParams, String inApiPath)
	{
		JSONObject payload = new JSONObject();
		payload.put("query", inParams.get("query"));
		payload.put("parent_ids", inParams.get("parent_ids"));
		
		Map<String,String> header = new HashMap();
		
		String customerkey = getMediaArchive().getCatalogSettingValue("catalog-storageid");
		if( customerkey == null)
		{
			customerkey = "demo";
		}
		header.put("x-customerkey", customerkey);
		// Add API key
		
		log.info("Sent: " + payload.toJSONString());
		
		LlmResponse response = callJson( "/" + inApiPath,header, payload);
		
		return response;
	}


}
