package org.entermediadb.ai.llm.ollama;

import java.io.StringReader;
import java.util.ArrayList;

import org.entermediadb.ai.llm.BasicLlmResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.util.JSONParser;
import org.openedit.OpenEditException;

public class OllamaResponse extends BasicLlmResponse
{

	@Override
	public boolean isToolCall()
	{
		if (!rawResponse.containsKey("message"))
		{
			return false; // No message object, so no tool call
		}

		JSONObject message = (JSONObject) rawResponse.get("message");
		if (!message.containsKey("tool_calls"))
		{
			return false; // No tool calls in the message
		}

		JSONArray toolCalls = (JSONArray) message.get("tool_calls");
		return toolCalls != null && !toolCalls.isEmpty(); // Tool calls exist
	}

	@Override
	public JSONObject getMessageStructured()
	{
		/*
		JSONArray choices = (JSONArray) res.getRawResponse().get("choices");
        JSONObject choice = (JSONObject) choices.get(0);
        JSONObject message = (JSONObject) choice.get("message");
        
        String contentString = (String) message.get("content"); // Possibly a string
                
        if (contentString != null)
        {
        	JSONObject content = parser.parse(contentString);
        	return content;
        }
		 */
		
		JSONObject message = (JSONObject) rawResponse.get("message");
		JSONArray toolCalls = (JSONArray) message.get("tool_calls");

		/**
		 * 
		 
		 
		 
		 		JSONObject results = new JSONObject();

		JSONObject message = (JSONObject) res.getRawResponse().get("message");
		if (message == null || !message.get("role").equals("assistant"))
		{
			log.info("No message found in GPT response");
			return results;
		}

		String content = (String) message.get("content");
			
		if (content == null || content.isEmpty())
		{
			log.info("No structured data found in GPT response");
			return results;
		}
		JSONParser parser = new JSONParser();
		results = (JSONObject) parser.parse(new StringReader(content));

	
		 */
		
		
		if (toolCalls == null || toolCalls.isEmpty())
		{
			String content = (String) message.get("content");
			JSONParser parser = new JSONParser();
			try {
				JSONObject contentargs = (JSONObject) parser.parse(content);
				return contentargs;
			}
			catch (Exception e){
				return null;
			}
		}
		else 
		{
			JSONObject firstToolCall = (JSONObject) toolCalls.get(0); // Assuming one function call at a time
			JSONObject function = (JSONObject) firstToolCall.get("function");

			return function.containsKey("arguments") ? (JSONObject) function.get("arguments") : null;
	
		}
	}

	@Override
	public String getFunctionName()
	{
		if (!isToolCall())
		{
			return null;
		}

		JSONObject message = (JSONObject) rawResponse.get("message");
		JSONArray toolCalls = (JSONArray) message.get("tool_calls");

		if (toolCalls == null || toolCalls.isEmpty())
		{
			return null;
		}

		JSONObject firstToolCall = (JSONObject) toolCalls.get(0);
		JSONObject function = (JSONObject) firstToolCall.get("function");

		return function.containsKey("name") ? function.get("name").toString() : null;
	}

	@Override
	public String getMessage()
	{
		if (!rawResponse.containsKey("message"))
		{
			return null;
		}

		JSONObject message = (JSONObject) rawResponse.get("message");
		if (!message.containsKey("content"))
		{
			return null;
		}

		String content = message.get("content").toString();
		return content.isEmpty() ? null : content; // Return null if content is empty
	}

	@Override
	public boolean isSuccessful()
	{
		if (!rawResponse.containsKey("message"))
		{
			return false;
		}

		JSONObject message = (JSONObject) rawResponse.get("message");
		return message.containsKey("tool_calls") || message.containsKey("content");
	}

	@Override
	public int getTokensUsed()
	{
		if (!rawResponse.containsKey("usage"))
		{
			return 0; // No usage info provided
		}

		JSONObject usage = (JSONObject) rawResponse.get("usage");
		Object tokens = usage.get("total_tokens");

		if (tokens instanceof Long)
		{
			return ((Long) tokens).intValue();
		}
		else if (tokens instanceof Integer)
		{
			return (Integer) tokens;
		}

		return 0; // Default if type is unexpected
	}

	@Override
	public String getModel()
	{
		if (!rawResponse.containsKey("model"))
		{
			return "unknown"; // No model info provided
		}
		return rawResponse.get("model").toString();
	}

	@Override
	public ArrayList<String> getImageUrls()
	{
		throw new OpenEditException("Ollama cannot create images");
	}
	
	@Override
	public ArrayList<String> getImageBase64s()
	{
		throw new OpenEditException("Ollama cannot create images");
	}
}
