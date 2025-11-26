package org.entermediadb.ai.llm.llama;


import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.ai.llm.openai.OpenAiConnection;
import org.entermediadb.asset.MediaArchive;
import org.json.simple.JSONObject;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.util.JSONParser;

public class LlamaOpenAiConnection extends OpenAiConnection {
	private static Log log = LogFactory.getLog(LlamaOpenAiConnection.class);
	
	@Override
	public String getLlmProtocol()
	{
		//return "openai"; //This might not work. We might need to tweak the JSON
		return "llama";
	}
	
	
	public LlmResponse callStructuredOutputList(Map inParams)
	{
		inParams.put("model", getModelName()); 
		
		MediaArchive archive = getMediaArchive();
		String structurepath = "/" + getMediaArchive().getMediaDbId() + "/ai/" + getLlmProtocol() +"/classify/structures/" + getAiFunctionName() + "_structure.json";
		Page defpage = archive.getPageManager().getPage(structurepath);
		
		if (defpage.exists())
		{
			String structure = loadInputFromTemplate(structurepath, inParams);
			inParams.put("structure", structure);
		}
		
		
		String propmpt = loadInputFromTemplate("/" + getMediaArchive().getMediaDbId() + "/ai/" + getLlmProtocol() +"/classify/structures/" + getAiFunctionName() + ".json", inParams);

		JSONObject structureDef = (JSONObject) new JSONParser().parse(propmpt);

		String endpoint = getServerRoot() + "/v1/chat/completions"; 
		
		HttpPost method = new HttpPost(endpoint);
		method.addHeader("authorization", "Bearer " + getApiKey());
		method.setHeader("Content-Type", "application/json");
		method.setEntity(new StringEntity(structureDef.toJSONString(), StandardCharsets.UTF_8));

		CloseableHttpResponse resp = getConnection().sharedExecute(method);
		
		JSONObject results = new JSONObject();
		JSONObject json = null;
		try
		{
			if (resp.getStatusLine().getStatusCode() != 200)
			{
				throw new OpenEditException("OpenAI error: " + resp.getStatusLine());
			}
	
			json = (JSONObject)getConnection().parseMap(resp);
			log.info("Returned: " + json.toJSONString());
			LlmResponse response = createResponse();
			response.setRawResponse(json);
			return response;
			
		} finally {
			getConnection().release(resp);
		}

		
	}
	
	
	@Override 
	public LlmResponse callClassifyFunction(Map params, String inFunction, String inBase64Image, String textContent)
	{
		MediaArchive archive = getMediaArchive();
		// Handle function call definition
		String templatepath = "/" + archive.getMediaDbId() + "/ai/" + getLlmProtocol() +"/classify/functions/" + inFunction + ".json";
		
		Page defpage = archive.getPageManager().getPage(templatepath);
		
		if (!defpage.exists())
		{
			throw new OpenEditException("Requested Function Does Not Exist in MediaDB or Catalog:" + templatepath);
		}
		
		if(textContent != null)
		{
			params.put("textcontent", textContent);
		}
		
		
		params.put("aimodel", getModelName());
		
		
		String definition = loadInputFromTemplate(templatepath, params);

		JSONParser parser = new JSONParser();
		JSONObject functionDef = (JSONObject) parser.parse(definition);

		LlmResponse res = callJson("/v1/chat/completions", functionDef);
	    return res;

	}
}
