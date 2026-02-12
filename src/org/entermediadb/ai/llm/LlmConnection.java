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

    public String loadInputFromTemplate(AgentContext agentcontext, String inTemplate);
    
    public LlmResponse renderLocalAction(AgentContext llmreuest);
    public LlmResponse renderLocalAction(AgentContext llmreuest, String inTemplate);
    
    public LlmResponse callCreateFunction(AgentContext agentcontext, String inFunction);
    
    public LlmResponse callClassifyFunction(AgentContext agentcontext, String inFunction, String inBase64Image);
    public LlmResponse callClassifyFunction(AgentContext agentcontext, String inFunction, String inBase64Image, String textContent);
    
    public LlmResponse callToolsFunction(AgentContext inAgentContext, String inFunction);

    public LlmResponse runPageAsInput(AgentContext llmRequest, String inChattemplate);

	
	public Data getAiServerData();
	public void setAiServerData(Data fieldMainServerUrl);

    public LlmResponse callStructure(AgentContext agentcontext, String inFuction);
	
	public LlmResponse callOCRFunction(AgentContext agentcontext, String inBase64Image, String inFunctioName);
	
	public LlmResponse callSmartCreatorAiAction(AgentContext agentcontext, String inActionName);

	public LlmResponse callJson(String inPath, Map inPayload);
	public LlmResponse callJson(String inPath, JSONObject inPayload);
	public LlmResponse callJson(String inPath, Map<String, String> inHeaders, Map inMap);
	public LlmResponse callJson(String inPath, Map<String, String> inHeaders, JSONObject inEmbeddingPayload);
	
	LlmResponse createResponse();
	
}
