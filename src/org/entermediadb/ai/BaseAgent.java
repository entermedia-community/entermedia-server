package org.entermediadb.ai;

import org.apache.velocity.tools.config.Data;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.MediaArchive;
import org.openedit.ModuleManager;

public class BaseAgent implements Agent {

    //Calls functions as he wants
    protected Data fieldAgentData;
    public Data getAgentData() {
        return fieldAgentData;
    }   
    public void setAgentData(Data inAgentData) {
        fieldAgentData = inAgentData;
    }
    
	protected String fieldCatalogId;

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}
	protected ModuleManager fieldModuleManager;
	

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}
	protected MediaArchive getMediaArchive()
	{
		return (MediaArchive)getModuleManager().getBean(getCatalogId(),"mediaArchive");
	}
	public LlmResponse handleError(AgentContext inAgentContext, String inError)
	{
		return handleError(inAgentContext, inError, 200);
	}
	public LlmResponse handleError(AgentContext inAgentContext, String inError, int inCode)
	{
		inAgentContext.addContext("error", inError);
		inAgentContext.addContext("errorcode", inCode);
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("render_error");
		LlmResponse response = llmconnection.renderLocalAction(inAgentContext, "render_error");
		//inAgentContext.setFunctionName(null);
		inAgentContext.setNextFunctionName(null);
		return response;
	}
    

}   