package org.entermediadb.ai.llm;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

public class LlmRequest {
	String functionName;
	String nextFunctionName;
	Map<String, Object> context;
//	JSONObject arguments;
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
	
	public Map<String,Object> getContext() {
		return context;
	}
	
	public Object getContextValue(String inKey) {
		if (context == null) {
			return null;
		}
		return context.get(inKey);
	}
	
	public void setContext(Map<String,Object> inContext) {
		context = inContext;
	}
	
	public void addContext(String inKey, Object inValue) {
		if (context == null) {
			context = new HashMap<String,Object>();
		}
		context.put(inKey, inValue);
	}
	
//	public JSONObject getArguments() {
//		return arguments;
//	}
//	
//	public void setArguments(JSONObject inArguments) {
//		arguments = inArguments;
//	}
	
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
		obj.put("nextfunction", nextFunctionName);
		obj.put("parameters", parameters);
		return obj.toJSONString();
	}
	
}
