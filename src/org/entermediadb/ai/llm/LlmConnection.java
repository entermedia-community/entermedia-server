package org.entermediadb.ai.llm;

import java.util.Map;

import org.json.simple.JSONObject;
import org.openedit.Data;

public interface LlmConnection {
	
	public String getApiKey();

	public String getServerRoot();
	
	public String getLlmProtocol();

	public String getModelName();
	public Boolean isReady();

    public LlmResponse createImage(String inPrompt);
    public LlmResponse createImage(String inPrompt, int inCount, String inSize);

    public String loadInputFromTemplate(String inString, Map<String, Object> inParams);
    public String loadInputFromTemplate(String inTemplate, AgentContext agentcontext);
    
    public LlmResponse renderLocalAction(AgentContext llmreuest);
    public LlmResponse renderLocalAction(AgentContext llmreuest, String inTemplate);
    
    public LlmResponse callCreateFunction(Map params, String inFunction);
    
    public LlmResponse callClassifyFunction(Map inParams, String inFunction, String inBase64Image);
    public LlmResponse callClassifyFunction(Map inParams, String inFunction, String inBase64Image, String textContent);
    
    public LlmResponse callToolsFunction(Map inParams, String inFunction);

    public LlmResponse runPageAsInput(AgentContext llmRequest, String inChattemplate);

	
	public Data getAiServerData();
	public void setAiServerData(Data fieldMainServerUrl);

    public LlmResponse callStructure(Map inParams, String inFuction);
	
	public LlmResponse callOCRFunction(Map inParams, String inBase64Image, String inFunctioName);
	
	public LlmResponse callSmartCreatorAiAction(Map params, String inActionName);

	public LlmResponse callJson(String inPath, Map inPayload);
	public LlmResponse callJson(String inPath, JSONObject inPayload);
	public LlmResponse callJson(String inPath, Map<String, String> inHeaders, Map inMap);
	public LlmResponse callJson(String inPath, Map<String, String> inHeaders, JSONObject inEmbeddingPayload);
	
	LlmResponse createResponse();
	
}
