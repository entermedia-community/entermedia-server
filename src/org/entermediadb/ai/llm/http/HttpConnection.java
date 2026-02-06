package org.entermediadb.ai.llm.http;


import java.nio.charset.StandardCharsets;
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
import org.openedit.util.JSONParser;

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
		
		log.info( "Sent: " + payload.toJSONString());
		
		HttpPost method = new HttpPost(getServerRoot() + "/" + inApiPath);
		method.addHeader("Authorization", "Bearer " + getApiKey());
		method.setHeader("Content-Type", "application/json");
		method.setEntity(new StringEntity(payload.toJSONString(), StandardCharsets.UTF_8));

		CloseableHttpResponse resp = getConnection().sharedExecute(method);

		try
		{
			if (resp.getStatusLine().getStatusCode() != 200)
			{
				throw new OpenEditException("OpenAI error: " + resp.getStatusLine());
			}
	
			JSONObject json = (JSONObject)getConnection().parseMap(resp);

			log.info("Returned: " + json.toJSONString());
		
			LlmResponse response = createResponse();
			response.setRawResponse(json);
			return response;
		}
		catch (Exception ex)
		{
			throw new OpenEditException(ex);
		}
		finally
		{
			getConnection().release(resp);
		}
	}


}
