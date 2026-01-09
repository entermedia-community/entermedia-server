package org.entermediadb.ai.llm;

import java.util.ArrayList;
import java.util.Collection;

import org.entermediadb.ai.assistant.AiSearch;
import org.entermediadb.ai.knn.RankedResult;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class BasicLlmResponse implements LlmResponse
{
	protected String fieldMessage;
	protected String fieldMessagePlain;
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

		
	@Override
	public String getMessage()
	{
		return fieldMessage;
	}
	
	public void setMessagePlain(String inMessage)
	{
		fieldMessagePlain = inMessage;
	}
	
	@Override
	public String getMessagePlain()
	{
		return fieldMessagePlain;
	}
	
	protected Collection fieldRawCollection;
	

	public Collection getRawCollection()
	{
		return fieldRawCollection;
	}

	public void setRawCollection(JSONArray inRawCollection)
	{
		fieldRawCollection = inRawCollection;
	}

	protected JSONObject rawResponse;

    public JSONObject getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(JSONObject inRawResponse) {
        rawResponse = inRawResponse;
    }

	@Override
	public boolean isToolCall()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public JSONObject getMessageStructured()
	{
		return getRawResponse();
	}

	@Override
	public String getFunctionName()
	{
		return fieldFunctionName;
	}

	public void setFunctionName(String inFunctionName)
	{
		fieldFunctionName = inFunctionName;
	}

	
	@Override
	public boolean isSuccessful()
	{
		// TODO Auto-generated method stub
		return false;
	}

	//Are these needed?
	
	@Override
	public int getTokensUsed()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getModel()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<String> getImageUrls()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<String> getImageBase64s()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getFileName()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setMessage(String inMessage)
	{
		fieldMessage = inMessage;
	}

	@Override
	public void setRawCollection(Collection inObj)
	{
		fieldRawCollection = inObj;
	}

	@Override
	public Collection getCollection(String inKey) {
		Object obj = getMessageStructured().get(inKey);
		if( obj instanceof JSONArray || obj instanceof Collection)
		{
			return (Collection) obj;
		}
		return null;
	}

}
