package org.entermediadb.ai.llm.llama;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.llm.openai.OpenAiConnection;

public class LlamaOpenAiConnection extends OpenAiConnection {
	private static Log log = LogFactory.getLog(LlamaOpenAiConnection.class);
	
	@Override
	public String getLlmProtocol()
	{
		//return "openai"; //This might not work. We might need to tweak the JSON
		return "llama";
	}
	
//	@Override
//	public LlmResponse callStructuredOutputList(Map inParams)
//	{
//		inParams.put("model", getModelName()); 
//		
//		MediaArchive archive = getMediaArchive();
//		String structurepath = "/" + getMediaArchive().getMediaDbId() + "/ai/" + getLlmProtocol() +"/classify/structures/" + getAiFunctionName() + "_structure.json";
//		Page defpage = archive.getPageManager().getPage(structurepath);
//		
//		if (defpage.exists())
//		{
//			String structure = loadInputFromTemplate(structurepath, inParams);
//			inParams.put("structure", structure);
//		}
//		
//		
//		String propmpt = loadInputFromTemplate("/" + getMediaArchive().getMediaDbId() + "/ai/" + getLlmProtocol() +"/classify/structures/" + getAiFunctionName() + ".json", inParams);
//
//		JSONObject structureDef = (JSONObject) new JSONParser().parse(propmpt);
//
//		String endpoint = getServerRoot() + "/chat/completions"; 
//		
//		HttpPost method = new HttpPost(endpoint);
//		method.addHeader("authorization", "Bearer " + getApiKey());
//		method.setHeader("Content-Type", "application/json");
//		method.setEntity(new StringEntity(structureDef.toJSONString(), StandardCharsets.UTF_8));
//
//		CloseableHttpResponse resp = getConnection().sharedExecute(method);
//		
//		JSONObject results = new JSONObject();
//		JSONObject json = null;
//		try
//		{
//			if (resp.getStatusLine().getStatusCode() != 200)
//			{
//				throw new OpenEditException("OpenAI error: " + resp.getStatusLine());
//			}
//	
//			json = (JSONObject)getConnection().parseMap(resp);
//			log.info("Returned: " + json.toJSONString());
//			LlmResponse response = createResponse();
//			response.setRawResponse(json);
//			return response;
//			
//		} finally {
//			getConnection().release(resp);
//		}
//
//		
//	}
}
