package org.entermediadb.ai.llm.openai;

import java.util.ArrayList;

import org.entermediadb.ai.llm.BasicLlmResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.util.JSONParser;

public class OpenAiResponse extends BasicLlmResponse {

    @Override
    public boolean isToolCall() {
        if (rawResponse == null) 
    	{
    	return false;
    	}

        JSONArray choices = (JSONArray) rawResponse.get("choices");
        if (choices == null || choices.isEmpty()) 
        {
        	return false;
        }

        JSONObject choice = (JSONObject) choices.get(0);
        JSONObject message = (JSONObject) choice.get("message");

        return message != null && message.get("function_call") != null;
    }

    @Override
    public JSONObject getMessageStructured() {
      /*  if (!isToolCall())
        {
        	
        	return null;
        }*/
    	JSONParser parser = new JSONParser();
    	JSONObject arguments = null;
        JSONArray choices = (JSONArray) rawResponse.get("choices");
        if (choices == null || choices.isEmpty()) 
        {
        	return null;
        }
        JSONObject choice = (JSONObject) choices.get(0);
        JSONObject message = (JSONObject) choice.get("message");
        if (message == null || message.isEmpty()) 
        {
        	return null;
        }
        JSONObject functionCall = (JSONObject) message.get("function_call");
        if (functionCall != null) 
		{
        	String argumentsString = (String) functionCall.get("arguments");
            
            arguments = parser.parse(argumentsString);
		}
        //gpt-5-nano tool_calls response
        JSONArray tool_calls = (JSONArray) message.get("tool_calls");
        if (tool_calls != null) 
		{
        	JSONObject function = (JSONObject) tool_calls.get(0);
        	JSONObject function0 = (JSONObject) function.get("function");
        	//JSONObject functionarguments = (JSONObject) function0.get("arguments");
        	String argumentsString = (String) function0.get("arguments");
            
            arguments = parser.parse(argumentsString);
		}
        else 
        {
        	String argumentsString = (String) message.get("content");
            if (!argumentsString.startsWith("{"))
            {
            	//log.info("Response is Plain Text: " + argumentsString);
            	return null;
            }
            arguments = parser.parse(argumentsString);
        }
        
        return arguments;
        
        
        /**
         	JSONArray outputs = (JSONArray) json.get("output");
			if (outputs == null || outputs.isEmpty())
			{
				log.info("No output found in OpenAI response");
				return results;
			}
			
			JSONObject output = null;
			for (Object outputObj : outputs)
			{
				if (!(outputObj instanceof JSONObject))
				{
					log.info("Output is not a JSONObject: " + outputObj);
					continue;
				}
				JSONObject obj = (JSONObject) outputObj;
				String role = (String) obj.get("role");
				if(role != null && role.equals("assistant"))
				{
					output = obj;
					break;
				}
			}
			if (output == null || !output.get("status").equals("completed"))
			{
				log.info("No completed output found in GPT response");
				return results;
			}
			JSONArray contents = (JSONArray) output.get("content");
			if (contents == null || contents.isEmpty())
			{
				log.info("No content found in GPT response");
				return results;
			}
			JSONObject content = (JSONObject) contents.get(0);
			
			if (content == null || !content.containsKey("text"))
			{
				log.info("No structured data found in GPT response");
				return results;
			}
			String text = (String) content.get("text");
			if (text == null || text.isEmpty())
			{
				log.info("No text found in structured data");
				return results;
			}
			results = (JSONObject) parser.parse(new StringReader(text));

			if(results.containsKey("type") && results.get("type").equals("object") && results.containsKey("properties"))
			{
				results = (JSONObject) results.get("properties"); // gpt-4o-mini sometimes wraps in properties
			}
         * 
         */
    }

    @Override
    public String getMessage() 
    {
    	if( fieldMessage != null)
    	{
    		return fieldMessage;
    	}
        if (rawResponse == null) return null;

        JSONArray choices = (JSONArray) rawResponse.get("choices");
        if (choices == null || choices.isEmpty()) return null;

        JSONObject choice = (JSONObject) choices.get(0);
        JSONObject message = (JSONObject) choice.get("message");

        return message != null ? (String) message.get("content") : null;
    }

    @Override
    public String getFunctionName() 
    {
    	if( fieldFunctionName != null)
    	{
    		return fieldFunctionName;
    	}
    	
        if (!isToolCall()) return null;

        JSONArray choices = (JSONArray) rawResponse.get("choices");
        JSONObject choice = (JSONObject) choices.get(0);
        JSONObject message = (JSONObject) choice.get("message");
        JSONObject functionCall = (JSONObject) message.get("function_call");

        return functionCall != null ? (String) functionCall.get("name") : null;
    }

    @Override
    public boolean isSuccessful() {
        if (rawResponse == null) return false;

        JSONArray choices = (JSONArray) rawResponse.get("choices");
        return choices != null && !choices.isEmpty();
    }

    @Override
    public int getTokensUsed() {
        if (rawResponse == null) return 0;

        JSONObject usage = (JSONObject) rawResponse.get("usage");
        if (usage == null) return 0;

        Object totalTokens = usage.get("total_tokens");
        if (totalTokens instanceof Long) {
            return ((Long) totalTokens).intValue();
        } else if (totalTokens instanceof Integer) {
            return (Integer) totalTokens;
        }
        return 0;
    }

    @Override
    public String getModel() {
        if (rawResponse == null) return "unknown";
        return (String) rawResponse.get("model");
    }

    @Override
    public ArrayList<String> getImageUrls() {
    	ArrayList<String> images = new ArrayList<String>();
        if (rawResponse == null) {
        	return images;
        }
        if (!rawResponse.containsKey("data")) {
        	return images;
        }

        JSONArray dataArray = (JSONArray) rawResponse.get("data");
        if (dataArray == null || dataArray.isEmpty()) {
        	return images;
        }

        for (int i = 0; i < dataArray.size(); i++) {
            JSONObject imageObject = (JSONObject) dataArray.get(i);
            String url = (String) imageObject.get("url");
            images.add(url);
        }
        return images;
    }
    
    @Override
    public ArrayList<String> getImageBase64s() {
    	ArrayList<String> images = new ArrayList<String>();
        if (rawResponse == null) {
        	return images;
        }
        
        if (!rawResponse.containsKey("data")) {
        	return images;
        }
        
        JSONArray dataArray = (JSONArray) rawResponse.get("data");
        
        if (dataArray == null || dataArray.isEmpty()) {
        	return images;
        }

        for (int i = 0; i < dataArray.size(); i++) {
            JSONObject imageObject = (JSONObject) dataArray.get(i);
            String url = (String) imageObject.get("b64_json");
            images.add(url);
        }
        return images;
    }
    
    @Override
    public String getFileName() {
		String filename = null;
		if(rawResponse != null && rawResponse.containsKey("filename")) {
			filename = (String) rawResponse.get("filename");
            if(filename != null) {
            	int rand = (int) (Math.random() * 10000);
            	if(filename.endsWith(".png")) {
					filename = filename.substring(0, filename.length() - 4) + "-" + rand + ".png";
				} 
            	else
				{
					filename = filename + "-" + rand + ".png";
				}
            }
		}
		if(filename == null) {
			filename = System.currentTimeMillis() + ".png";
		}
		return filename;
	}
}
