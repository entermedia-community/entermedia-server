package org.entermediadb.ai.llm;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.entermediadb.ai.assistant.AiSearch;
import org.entermediadb.ai.knn.RankedResult;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.ModuleManager;
import org.openedit.data.BaseData;
import org.openedit.profile.UserProfile;

public class AgentContext extends BaseData implements CatalogEnabled {
	String functionName;
	String nextFunctionName;
	Map<String, Object> context;
//	JSONObject arguments;
	
	UserProfile fieldUserProfile;
	
	ModuleManager fieldModuleManager;
	
	String fieldCatalogId;
	
	
	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public UserProfile getUserProfile()
	{
		return fieldUserProfile;
	}

	public void setUserProfile(UserProfile inUserProfile)
	{
		fieldUserProfile = inUserProfile;
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
	
	public String toString() {
		JSONObject obj = new JSONObject();
		obj.put("function", functionName);
		obj.put("nextfunction", nextFunctionName);
		return obj.toJSONString();
	}
	
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
}
