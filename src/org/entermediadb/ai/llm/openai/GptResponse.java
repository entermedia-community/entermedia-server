package org.entermediadb.ai.llm.openai;

import java.util.ArrayList;

import org.entermediadb.ai.llm.BaseLlmResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class GptResponse extends BaseLlmResponse {

    @Override
    public boolean isToolCall() {
        if (rawResponse == null) return false;

        JSONArray choices = (JSONArray) rawResponse.get("choices");
        if (choices == null || choices.isEmpty()) return false;

        JSONObject choice = (JSONObject) choices.get(0);
        JSONObject message = (JSONObject) choice.get("message");

        return message != null && message.get("function_call") != null;
    }

    @Override
    public JSONObject getArguments() {
        if (!isToolCall()) return null;

        try {
            JSONArray choices = (JSONArray) rawResponse.get("choices");
            JSONObject choice = (JSONObject) choices.get(0);
            JSONObject message = (JSONObject) choice.get("message");
            JSONObject functionCall = (JSONObject) message.get("function_call");

            String argumentsString = (String) functionCall.get("arguments");
            JSONParser parser = new JSONParser();
            return (JSONObject) parser.parse(argumentsString); // Parse the stringified JSON
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getMessage() {
        if (rawResponse == null) return null;

        JSONArray choices = (JSONArray) rawResponse.get("choices");
        if (choices == null || choices.isEmpty()) return null;

        JSONObject choice = (JSONObject) choices.get(0);
        JSONObject message = (JSONObject) choice.get("message");

        return message != null ? (String) message.get("content") : null;
    }

    @Override
    public String getFunctionName() {
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
    public ArrayList getImageUrls() {
    	ArrayList images = new ArrayList();
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

        String[] urls = new String[dataArray.size()];
        for (int i = 0; i < dataArray.size(); i++) {
            JSONObject imageObject = (JSONObject) dataArray.get(i);
            String url = (String) imageObject.get("url");
            images.add(url);
        }
        return images;
    }
}
