package org.entermediadb.ai.llm;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.entermediadb.ai.assistant.AiCreation;
import org.entermediadb.ai.assistant.AiSearch;
import org.entermediadb.ai.creator.AiSmartCreatorSteps;
import org.entermediadb.ai.knn.RankedResult;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.data.BaseData;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;

public class AgentContext extends BaseData implements CatalogEnabled {
	String functionName;
	String nextFunctionName;
	Map<String, Object> context;
//	JSONObject arguments;
	
	Long fieldWaitTime;
	
	
	
	//TODO: Cache history here for performance
	
	public Long getWaitTime()
	{
		return fieldWaitTime;
	}

	public void setWaitTime(Long inWaitTime)
	{
		fieldWaitTime = inWaitTime;
	}

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
	
	@Override
	public String get(String inId)
	{
		// TODO Auto-generated method stub
		String value = super.get(inId);
		
		if (value == null && inId.equals("entityid"))
		{
			value = getChannel().get("dataid");
		}
		else if (value == null && inId.equals("entitymoduleid"))
		{
			value = getChannel().get("searchtype");
		}
		
		return value;
	}

	
	public String getNextFunctionName() {
		return get("nextfunctionname");
	}
	
	public void setNextFunctionName(String inNextFunctionName) {
		setValue("nextfunctionname",inNextFunctionName);
	}

	public String getTopLevelFunctionName() {
		return get("toplevelaifunctionid");
	}
	
	public void setTopLevelFunctionName(String inNextFunctionName) {
		setValue("toplevelaifunctionid",inNextFunctionName);
	}
	
	public Map<String,Object> getContext() {
		return context;
	}
	
	public Data getChannel()
	{
		Data channel = (Data)getContextValue("channel");
		if( channel == null)
		{
			
		}
		return channel;
	}
	
	public void setChannel(Data inChannel)
	{
		setValue("channel",inChannel.getId());
		addContext("channel",inChannel);
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
	
	public void put(String inKey, Object inValue)
	{
		addContext(inKey, inValue);
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
		if( fieldAiSearchParams == null)
		{
			fieldAiSearchParams  = new AiSearch();
		}
		return fieldAiSearchParams;
	}

	public void setAiSearchParams(AiSearch inAiSearchParams)
	{
		fieldAiSearchParams = inAiSearchParams;
	}
	
	
	AiCreation fieldAiCreationParams;
	
	public AiCreation getAiCreationParams()
	{
		if( fieldAiCreationParams == null)
		{
			fieldAiCreationParams = new AiCreation();
		}
		return fieldAiCreationParams;
	}

	public void setAiCreationParams(AiCreation inAiCreationParams)
	{
		fieldAiCreationParams = inAiCreationParams;
	}
	
	AiSmartCreatorSteps fieldAiSmartCreatorSteps;
	

	public AiSmartCreatorSteps getAiSmartCreatorSteps()
	{
		return fieldAiSmartCreatorSteps;
	}

	public void setAiSmartCreatorSteps(AiSmartCreatorSteps inAiCreatorSteps)
	{
		fieldAiSmartCreatorSteps = inAiCreatorSteps;
	}

	public String getFunctionName()
	{
		return get("functionname");
	}

	public void setFunctionName(String inFunctionName)
	{
		setValue("functionname",inFunctionName);
	}
	
	public String getMessagePrefix()
	{
		String message = get("messageprefix");
		if( message == null)
		{
			message = "";
		}
		return message;
	}

	public void setMessagePrefix(String inMessagePrefix)
	{
		setValue("messageprefix", inMessagePrefix);
	}
	
	Collection<String> fieldExcludedEntityIds;
	Collection<String> fieldExcludedAssetIds;
	
	public Collection<String> getExcludedEntityIds()
	{
		return fieldExcludedEntityIds;
	}
	
	public void setExcludedEntityIds(Collection<String> inExcludedEntityids)
	{
		fieldExcludedEntityIds = inExcludedEntityids;
	}
	
	public void addExcludedEntityId(String inEntityid)
	{
		if( fieldExcludedEntityIds == null)
		{
			fieldExcludedEntityIds = new java.util.ArrayList<>();
		}
		fieldExcludedEntityIds.add(inEntityid);
	}
	
	public Collection<String> getExcludedAssetIds()
	{
		return fieldExcludedAssetIds;
	}
	
	public void setExcludedAssetIds(Collection<String> inExcludedAssetids)
	{
		fieldExcludedAssetIds = inExcludedAssetids;
	}
	
	public void addExcludedAssetId(String inAssetid)
	{
		if( fieldExcludedAssetIds == null)
		{
			fieldExcludedAssetIds = new java.util.ArrayList<>();
		}
		fieldExcludedAssetIds.add(inAssetid);
	}

	public User getChatUser()
	{
		return getUserProfile().getUser();
	}
	
	public void setLocale(String inLocale)
	{
		setValue("locale",inLocale);
	}
	
	
	public String getLocale()
	{
		return get("locale");
	}

}
