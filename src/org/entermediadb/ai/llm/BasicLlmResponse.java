package org.entermediadb.ai.llm;

import java.util.ArrayList;

import org.json.simple.JSONObject;

public class BasicLlmResponse implements LlmResponse
{
	protected String fieldMessage;
	protected String fieldFunctionName;
	
	public void setFunctionName(String inFunctionName)
	{
		fieldFunctionName = inFunctionName;
	}

	public void setMessage(String inMessage)
	{
		fieldMessage = inMessage;
	}

	protected JSONObject rawResponse;

    public JSONObject getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(JSONObject inRawResponse) {
        rawResponse = inRawResponse;
    }

	public String getNextFunctionName()
	{
		String nextFunction = getParameters().get("nextfunction").toString();
		return nextFunction;
	}

	@Override
	public boolean isToolCall()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public JSONObject getArguments()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMessage()
	{
		return fieldMessage;
	}

	@Override
	public String getFunctionName()
	{
		return fieldFunctionName;
	}

	@Override
	public boolean isSuccessful()
	{
		// TODO Auto-generated method stub
		return false;
	}

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

	JSONObject fieldParameters;
	
	public void setParameters(JSONObject inParameters)
	{
		fieldParameters = inParameters;
	}

	@Override
	public JSONObject getParameters()
	{
		return fieldParameters;
	}
    

}
