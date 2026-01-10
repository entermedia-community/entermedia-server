package org.entermediadb.ai.llm;

import java.util.Map;

import org.json.simple.JSONObject;
import org.openedit.Data;

public interface LlmConnection {
	
	public String getApiKey();

	public String getServerRoot();
	
	public String getLlmProtocol();

	public String getModelName();
	
	public String getAiFunctionName();
	public Data getAiFunctionData();
	public void setAiFunctionData(Data aiFunctionData);

	public Boolean isReady();

    public LlmResponse createImage(String inPrompt);
    public LlmResponse createImage(String inPrompt, int inCount, String inSize);

    public String loadInputFromTemplate(String inString, Map<String, Object> inParams);
    public String loadInputFromTemplate(String inTemplate, AgentContext agentcontext);
    
    public LlmResponse renderLocalAction(AgentContext llmreuest);
    
    public LlmResponse callCreateFunction(Map params, String inFunction);
    
    public LlmResponse callClassifyFunction(Map inParams, String inFunction, String inBase64Image);
    public LlmResponse callClassifyFunction(Map inParams, String inFunction, String inBase64Image, String textContent);

    public LlmResponse runPageAsInput(AgentContext llmRequest, String inChattemplate);

	
	public Data getAiServerData();
	public void setAiServerData(Data fieldMainServerUrl);

    public LlmResponse callStructuredOutputList(Map inParams);
	
	public LlmResponse callOCRFunction(Map inParams, String inBase64Image);

	public LlmResponse callJson(String inPath, JSONObject inPayload);	
	public LlmResponse callJson(String inPath, Map<String, String> inHeaders, Map inMap);
	public LlmResponse callJson(String inPath, Map<String, String> inHeaders, JSONObject inEmbeddingPayload);
	
	LlmResponse createResponse();
	
}
