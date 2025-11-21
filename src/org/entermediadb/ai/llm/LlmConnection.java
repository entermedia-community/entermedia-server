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

    public LlmResponse createImage(String inPrompt) throws Exception;
    public LlmResponse createImage(String inPrompt, int inCount, String inSize) throws Exception;

    public String loadInputFromTemplate(String inString, Map<String, Object> inParams);
    public String loadInputFromTemplate(String inTemplate, AgentContext agentcontext);
    
    public LlmResponse renderLocalAction(AgentContext llmreuest);
    
    public LlmResponse callCreateFunction(Map params, String inFunction);
    
    public LlmResponse callClassifyFunction(Map inParams, String inFunction, String inBase64Image);
    public LlmResponse callClassifyFunction(Map inParams, String inFunction, String inBase64Image, String textContent);

    public LlmResponse runPageAsInput(AgentContext llmRequest, String inChattemplate);
    public LlmResponse callPlainMessage(AgentContext llmRequest, String inChitChatPageName);

	
	public Data getAiServerData();
	public void setAiServerData(Data fieldMainServerUrl);

    public JSONObject callStructuredOutputList(String inStructureName, Map inParams);
	
	public LlmResponse callOCRFunction(Map inParams, String inOCRInstruction, String inBase64Image);

	public LlmResponse callJson(String inPath, Map<String, String> inHeaders, JSONObject inEmbeddingPayload);
}
