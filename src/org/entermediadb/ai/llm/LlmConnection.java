package org.entermediadb.ai.llm;

import java.util.Map;

import org.json.simple.JSONObject;
import org.openedit.Data;

public interface LlmConnection {
	
	public String getApiKey();

	public String getLlmType();
	
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

    public Data getModelData();
    
    public void setModelData(Data inData);
	
	public JSONObject callStructuredOutputList(String inStructureName, Map inParams);
	
	public LlmResponse callOCRFunction(Map inParams, String inOCRInstruction, String inBase64Image);
}
