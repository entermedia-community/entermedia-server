package org.entermediadb.ai.llm;

import java.util.Collection;
import java.util.Map;

import org.openedit.Data;

public interface LlmConnection {
	
	public String getApikey();

	public String getServerName();
	
	public Boolean isReady();

    public String getEmbedding(String inQuery) throws Exception;

    public LlmResponse createImage(Map inParams, String inModel, int inI, String inString, String inImagestyle, String inTemplate);

    public String loadInputFromTemplate(String inString, Map inParams);
    
    public LlmResponse callFunction(Map inParams, String inModel, String inFunction, String inQuery) throws Exception;
    
    public LlmResponse callFunction(Map inParams, String inModel, String inFunction, String inQuery, String inBase64Image) throws Exception;

    public LlmResponse runPageAsInput(Map inParams, String inModel, String inChattemplate);

    public String getApiEndpoint();
	
	public Collection<String> callStructuredOutputList(String inStructureName,String inModel, Collection inFields, Map inParams) throws Exception;
}
