package org.entermediadb.ai.llm.emedia;

import java.util.Collection;

import org.entermediadb.ai.assistant.AiSearch;
import org.entermediadb.ai.knn.RankedResult;
import org.entermediadb.ai.llm.BasicLlmResponse;

public class EMediaAIResponse extends BasicLlmResponse
{
	protected String fieldFunctionName;
	protected AiSearch fieldAiSearchParams;
	
	Collection<RankedResult> fieldRankedSuggestions;
	
	public Collection<RankedResult> getRankedSuggestions()
	{
		return fieldRankedSuggestions;
	}

	public void setRankedSuggestions(Collection<RankedResult> inRankedSuggestions)
	{
		fieldRankedSuggestions = inRankedSuggestions;
	}


	public AiSearch getAiSearchParams()
	{
		return fieldAiSearchParams;
	}

	public void setAiSearchParams(AiSearch inAiSearchParams)
	{
		fieldAiSearchParams = inAiSearchParams;
	}

	public String getFunctionName()
	{
		return fieldFunctionName;
	}

	public void setFunctionName(String inFunctionName)
	{
		fieldFunctionName = inFunctionName;
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
