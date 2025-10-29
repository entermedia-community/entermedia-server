package org.entermediadb.ai.llm;

import java.util.ArrayList;

import org.json.simple.JSONObject;

public class BasicLlmResponse implements LlmResponse
{
	protected String fieldMessage;
	protected String fieldMessagePlain;
	protected String fieldFunctionName;

	public void setMessage(String inMessage)
	{
		fieldMessage = inMessage;
	}
	
	@Override
	public String getMessage()
	{
		return fieldMessage;
	}
	
	public String setMessagePlain(String inMessage)
	{
		fieldMessagePlain = inMessage;
		return fieldMessagePlain;
	}
	
	@Override
	public String getMessagePlain()
	{
		return fieldMessagePlain;
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
	public JSONObject getArguments()
	{
		return getRawResponse();
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
	
	@Override
	public String getFileName()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	

}
