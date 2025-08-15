package org.entermediadb.llm;

import java.util.Map;

public interface LlmConnection {
	
	public String getApikey();

	public String getType();
	
	public Boolean isReady();

    public String getEmbedding(String inQuery) throws Exception;

    public LLMResponse createImage(Map inParams, String inModel, int inI, String inString, String inImagestyle, String inTemplate);

    public String loadInputFromTemplate(Map inParams, String inString);
    
    public LLMResponse callFunction(Map inParams, String inModel, String inFunction, String inQuery, int temp, int maxtokens) throws Exception;
    
    public LLMResponse callFunction(Map inParams, String inModel, String inFunction, String inQuery, int temp, int maxtokens, String inBase64Image) throws Exception;

    public LLMResponse runPageAsInput(Map inParams, String inModel, String inChattemplate);

    public String getApiEndpoint();
}
