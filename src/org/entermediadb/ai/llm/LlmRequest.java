package org.entermediadb.ai.llm;

import org.json.simple.JSONObject;

public class LlmRequest {
	String functionName;
	String nextFunctionName;
	JSONObject arguments;
	JSONObject parameters;
	
	
	public String getFunctionName() {
		return functionName;
	}
	
	public void setFunctionName(String inFunctionName) {
		functionName = inFunctionName;
	}
	
	public String getNextFunctionName() {
		return nextFunctionName;
	}
	
	public void setNextFunctionName(String inNextFunctionName) {
		nextFunctionName = inNextFunctionName;
	}
	
	public JSONObject getArguments() {
		return arguments;
	}
	
	public void setArguments(JSONObject inArguments) {
		arguments = inArguments;
	}
	
	public JSONObject getParameters() {
		return parameters;
	}
	
	public void setParameters(JSONObject inParameters) {
		parameters = inParameters;
	}
	
	public void setParameter(String inKey, Object inValue) {
		if (parameters == null) {
			parameters = new JSONObject();
		}
		parameters.put(inKey, inValue);
	}
	
	public String toString() {
		JSONObject obj = new JSONObject();
		obj.put("function", functionName);
		obj.put("arguments", arguments);
		return obj.toJSONString();
	}
	
}
