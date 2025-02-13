package org.entermediadb.llm;

import org.openedit.WebPageRequest;

public interface LLMManager {
	
	
	public String getType();

    public String getEmbedding(String inQuery) throws Exception;

    public LLMResponse createImage(WebPageRequest inReq, String inModel, int inI, String inString, String inImagestyle, String inTemplate);

    public String loadInputFromTemplate(WebPageRequest inReq, String inString);
    
    public LLMResponse callFunction(WebPageRequest inReq, String inModel, String inFunction, String inQuery, int temp, int maxtokens) throws Exception;
    
    public LLMResponse callFunction(WebPageRequest inReq, String inModel, String inFunction, String inQuery, int temp, int maxtokens, String inBase64Image) throws Exception;

    public LLMResponse runPageAsInput(WebPageRequest inReq, String inModel, String inChattemplate);
}
