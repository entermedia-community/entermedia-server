package org.entermediadb.llm;

import org.json.simple.JSONObject;
import org.openedit.WebPageRequest;

public interface LLMManager
{

	public String getEmbedding(String inQuery) throws Exception;

	public JSONObject createImage(WebPageRequest inReq, String inModel, int inI, String inString, String inImagestyle, String inTemplate);

	public String loadInputFromTemplate(WebPageRequest inReq, String inString);
	public JSONObject callFunction(WebPageRequest inReq, String inModel, String inFunction, String inQuery, int temp, int maxtokens) throws Exception ;
	public JSONObject callFunction(WebPageRequest inReq, String inModel, String inFunction, String inQuery, int temp, int maxtokens, String inBase64Image) throws Exception;
	

}
