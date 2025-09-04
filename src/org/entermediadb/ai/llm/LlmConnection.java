package org.entermediadb.ai.llm;

import java.util.Collection;
import java.util.Map;

import org.json.simple.JSONObject;

public interface LlmConnection {
	
	public String getApikey();

	public String getServerName();
	
	public Boolean isReady();

    public String getEmbedding(String inQuery) throws Exception;

    public LlmResponse createImage(Map inParams);
    public LlmResponse createImage(Map inParams, int inCount, String inSize);

    public String loadInputFromTemplate(String inString, Map inParams);
    
    public LlmResponse callCreateFunction(Map params, String inModel, String inFunction) throws Exception;
    public LlmResponse callClassifyFunction(Map inParams, String inModel, String inFunction, String inQuery, String inBase64Image) throws Exception;

    public LlmResponse runPageAsInput(Map inParams, String inModel, String inChattemplate);

    public String getApiEndpoint();
	
	public JSONObject callStructuredOutputList(String inStructureName,String inModel, Collection inFields, Map inParams) throws Exception;
}
