package org.entermediadb.ai.llm;

import java.util.Map;

import org.json.simple.JSONObject;

public interface LlmConnection {
	
	public String getApiKey();

	public String getServerName();
	
	public Boolean isReady();

    public LlmResponse createImage(String inModel, String inPrompt) throws Exception;
    public LlmResponse createImage(String inModel, String inPrompt, int inCount, String inSize) throws Exception;

    public String loadInputFromTemplate(String inString, Map<String, Object> inParams);
    
    public LlmResponse renderLocalAction(AgentContext llmreuest);
    
    public LlmResponse callCreateFunction(Map params, String inModel, String inFunction);
    
    public LlmResponse callClassifyFunction(Map inParams, String inModel, String inFunction, String inBase64Image);
    public LlmResponse callClassifyFunction(Map inParams, String inModel, String inFunction, String inBase64Image, String textContent);

    public LlmResponse runPageAsInput(AgentContext llmRequest, String inChattemplate);
    public LlmResponse callPlainMessage(AgentContext llmRequest, String inChitChatPageName);

    public String getApiEndpoint();
	
	public JSONObject callStructuredOutputList(String inStructureName, String inModel, Map inParams);
}
