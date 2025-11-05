package org.entermediadb.ai.llm.emedia;

import org.entermediadb.ai.llm.BasicLlmResponse;

public class EMediaAIResponse extends BasicLlmResponse
{

	public String getFunctionName()
	{
		return fieldFunctionName;
	}

	public void setFunctionName(String inFunctionName)
	{
		fieldFunctionName = inFunctionName;
	}
	
	String fieldGeneralResponse;
	
	public String getGeneralResponse()
	{
		return fieldGeneralResponse;
	}
	
	public void setGeneralResponse(String inGeneralResponse)
	{
		fieldGeneralResponse = inGeneralResponse;
	}

	public String getFileName()
	{
		return getFunctionName();
	}
	
	protected boolean isToolCall;

	public boolean isToolCall()
	{
		return isToolCall;
	}

	public void setToolCall(boolean inIsToolCall)
	{
		isToolCall = inIsToolCall;
	}
	
}
