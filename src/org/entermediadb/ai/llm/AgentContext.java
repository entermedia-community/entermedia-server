package org.entermediadb.ai.llm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.entermediadb.ai.assistant.AiCreation;
import org.entermediadb.ai.assistant.AiSearch;
import org.entermediadb.ai.creator.AiSmartCreatorSteps;
import org.entermediadb.ai.knn.RankedResult;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.data.BaseData;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;

public class AgentContext extends BaseData implements CatalogEnabled
{
	protected ScriptLogger fieldScriptLogger;

	public AgentContext() {

	}

	public AgentContext(AgentContext inParent) {
		setParentContext(inParent);
	}

	public ScriptLogger getScriptLogger()
	{
		if (fieldScriptLogger == null)
		{
			if (getParentContext() != null)
			{
				return getParentContext().getScriptLogger();
			}
			if (getParentContext() == null)
			{
				fieldScriptLogger = new ScriptLogger();
			}
		}
		return fieldScriptLogger;
	}

	public void setScriptLogger(ScriptLogger inScriptLogger)
	{
		fieldScriptLogger = inScriptLogger;
	}

	protected AgentContext fieldParentContext;

	public AgentContext getParentContext()
	{
		return fieldParentContext;
	}

	public void setParentContext(AgentContext inParentContext)
	{
		fieldParentContext = inParentContext;
	}

	protected String functionName;
	protected String nextFunctionName;
	protected Map<String, Object> context;
	// JSONObject arguments;

	protected Long fieldWaitTime;

	// TODO: Cache history here for performance

	public Long getWaitTime()
	{
		return fieldWaitTime;
	}

	public void setWaitTime(Long inWaitTime)
	{
		fieldWaitTime = inWaitTime;
	}

	protected UserProfile fieldUserProfile;

	protected ModuleManager fieldModuleManager;

	protected String fieldCatalogId;

	protected MultiValued fieldCurrentScenerio;

	public MultiValued getCurrentScenerio()
	{
		if (fieldCurrentScenerio == null && getParentContext() != null)
		{
			return getParentContext().getCurrentScenerio();
		}
		return fieldCurrentScenerio;
	}

	public void setCurrentScenerio(MultiValued inCurrentScenerio)
	{
		fieldCurrentScenerio = inCurrentScenerio;
	}

	protected AgentEnabled fieldCurrentAgentEnable;

	public AgentEnabled getCurrentAgentEnable()
	{
		if (fieldCurrentAgentEnable == null && getParentContext() != null)
		{
			return getParentContext().getCurrentAgentEnable();
		}
		return fieldCurrentAgentEnable;
	}

	public void setCurrentAgentEnable(AgentEnabled inCurrentAgentEnable)
	{
		fieldCurrentAgentEnable = inCurrentAgentEnable;
		if (inCurrentAgentEnable != null)
		{
			if (inCurrentAgentEnable.getExtraContextValues() != null)
			{
				JSONObject json = inCurrentAgentEnable.getExtraContextValues();
				for (Object key : json.keySet())
				{
					Object value = json.get(key);
					addContext(String.valueOf(key), value);
				}
			}
			Collection<AgentEnabled> children = inCurrentAgentEnable.getChildren();
			setAgentEnableChildren(children);
		}
	}

	protected Collection<AgentEnabled> fieldAgentEnableChildren;

	public void setAgentEnableChildren(Collection<AgentEnabled> inAgentEnableChildren)
	{
		fieldAgentEnableChildren = inAgentEnableChildren;
	}

	public void setAgentEnableChildren(AgentEnabled inAgentEnableChildren)
	{
		Collection<AgentEnabled> children = new ArrayList<>();
		children.add(inAgentEnableChildren);
		setAgentEnableChildren(children);
	}

	public Collection<AgentEnabled> getAgentEnableChildren()
	{
		if (fieldAgentEnableChildren == null && getParentContext() != null)
		{
			return getParentContext().getAgentEnableChildren();
		}
		return fieldAgentEnableChildren;
	}

	public String getCatalogId()
	{
		if (fieldCatalogId == null && getParentContext() != null)
		{
			return getParentContext().getCatalogId();
		}
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public ModuleManager getModuleManager()
	{
		if (fieldModuleManager == null && getParentContext() != null)
		{
			return getParentContext().getModuleManager();
		}
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public UserProfile getUserProfile()
	{
		if (fieldUserProfile == null && getParentContext() != null)
		{
			return getParentContext().getUserProfile();
		}
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
			if (!inId.equals("channel") && getChannel() != null)
			{
				value = getChannel().get("dataid");
			}
		}
		else
			if (value == null && inId.equals("entitymoduleid"))
			{
				if (!inId.equals("channel") && getChannel() != null)
				{
					value = getChannel().get("searchtype");
				}
			}
		if (value == null && getParentContext() != null)
		{
			return getParentContext().get(inId);
		}
		return value;
	}

	public String getNextFunctionName()
	{
		return get("nextfunctionname");
	}

	public void setNextFunctionName(String inNextFunctionName)
	{
		setValue("nextfunctionname", inNextFunctionName);
	}

	public String getTopLevelFunctionName()
	{
		return get("toplevelaifunctionid");
	}

	public void setTopLevelFunctionName(String inNextFunctionName)
	{
		setValue("toplevelaifunctionid", inNextFunctionName);
	}

	public Map<String, Object> getContext()
	{
		if (context == null)
		{
			context = new HashMap();

		}
		return context;
	}

	public Data getChannel()
	{
		Data channel = (Data) getContextValue("channel");
		if (channel == null)
		{

		}
		return channel;
	}

	public void setChannel(Data inChannel)
	{
		setValue("channel", inChannel.getId());
		addContext("channel", inChannel);
	}

	public Object getContextValue(String inKey)
	{
		Object obj = null;
		if (context != null)
		{
			obj = context.get(inKey);
		}
		if (obj == null && getParentContext() != null)
		{
			return getParentContext().getContextValue(inKey);
		}
		if (obj == null)
		{
			obj = get(inKey);
		}
		return obj;
	}

	public void setContext(Map<String, Object> inContext)
	{
		context = inContext;
	}

	public void put(String inKey, Object inValue)
	{
		addContext(inKey, inValue);
	}

	public void addContext(String inKey, Object inValue)
	{
		getContext().put(inKey, inValue);
	}

	// public JSONObject getArguments() {
	// return arguments;
	// }
	//
	// public void setArguments(JSONObject inArguments) {
	// arguments = inArguments;
	// }

	public String toString()
	{
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
		if (fieldRankedSuggestions == null && getParentContext() != null)
		{
			return getParentContext().getRankedSuggestions();
		}
		return fieldRankedSuggestions;
	}

	public void setRankedSuggestions(Collection<RankedResult> inRankedSuggestions)
	{
		fieldRankedSuggestions = inRankedSuggestions;
	}

	public AiSearch getAiSearchParams()
	{
		if (fieldAiSearchParams == null && getParentContext() != null)
		{
			return getParentContext().getAiSearchParams();
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
		if (fieldAiCreationParams == null && getParentContext() != null)
		{
			return getParentContext().getAiCreationParams();
		}
		if (fieldAiCreationParams == null)
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
		if (fieldAiSmartCreatorSteps == null && getParentContext() != null)
		{
			return getParentContext().getAiSmartCreatorSteps();
		}
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
		setValue("functionname", inFunctionName);
	}

	public String getMessagePrefix()
	{
		String message = get("messageprefix");
		if (message == null)
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
		if (fieldExcludedEntityIds == null && getParentContext() != null)
		{
			return getParentContext().getExcludedEntityIds();
		}
		return fieldExcludedEntityIds;
	}

	public void setExcludedEntityIds(Collection<String> inExcludedEntityids)
	{
		fieldExcludedEntityIds = inExcludedEntityids;
	}

	public void addExcludedEntityId(String inEntityid)
	{
		if (fieldExcludedEntityIds == null)
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
		if (fieldExcludedAssetIds == null)
		{
			fieldExcludedAssetIds = new java.util.ArrayList<>();
		}
		fieldExcludedAssetIds.add(inAssetid);
	}

	public User getChatUser()
	{
		if (getUserProfile() == null)
		{
			return null;
		}
		return getUserProfile().getUser();
	}

	public void setLocale(String inLocale)
	{
		setValue("locale", inLocale);
	}

	public String getLocale()
	{
		return get("locale");
	}

	/* Log to console only */
	public void log(String inLog)
	{
		getScriptLogger().info("[" + getCatalogId() + "] " + inLog);
	}

	public void info(String inLog)
	{
		getScriptLogger().info(inLog);
		addEntry("info", inLog);
	}

	public Date getLastActive()
	{
		if (getLogs().size() > 0)
		{
			return getLogs().iterator().next().getDate();
		}
		return null;
	}

	protected void addEntry(String inString, String inLog)
	{
		LogEntry entry = new LogEntry(inString, inLog);
		entry.setDate(new Date());
		if (getCurrentAgentEnable() != null)
		{
			entry.setCurrentAgentEnabledData(getCurrentAgentEnable().getAutomationEnabledData());
			entry.setAgentData(getCurrentAgentEnable().getAgentData());
		}
		getLogs().add(entry);
	}

	public void error(Exception inE)
	{
		getScriptLogger().error(inE);
		addEntry("error", inE.getMessage());
	}

	public void error(String inString, Throwable inE)
	{
		getScriptLogger().error(inString, inE);
		addEntry("error", inString + " " + inE.getMessage());
	}

	public void headline(String string)
	{
		getScriptLogger().headline(string);
		addEntry("headline", string);
	}

	public void error(String string)
	{
		getScriptLogger().error(string);
		addEntry("error", string);
	}

	public MultiValued getCurrentEntity()
	{
		MultiValued entity = (MultiValued) getContextValue("currententity");
		return entity;
	}

	public MultiValued getCurrentEntityModule()
	{
		return (MultiValued) getContextValue("currententitymodule");
	}

	public void setCurrentEntity(MultiValued inEntity)
	{
		put("currententity", inEntity);
	}

	public void setCurrentEntityModule(MultiValued inEntityModule)
	{
		put("currententitymodule", inEntityModule);
	}

	protected Collection<LogEntry> fieldLogs;

	public Collection<LogEntry> getLogs()
	{
		if (fieldLogs == null && getParentContext() != null)
		{
			return getParentContext().getLogs();
		}
		if (fieldLogs == null)
		{
			fieldLogs = new ArrayList(); // Just on the top parent
		}
		return fieldLogs;
	}

	public void setLogs(Collection<LogEntry> inLogs)
	{
		fieldLogs = inLogs;
	}

	public int getTotalErrorLogs()
	{
		int count = 0;
		for (Iterator<LogEntry> iterator = getLogs().iterator(); iterator.hasNext();)
		{
			LogEntry logEntry = (LogEntry) iterator.next();
			if ("error".equals(logEntry.getLogType()))
			{
				count++;
			}
		}

		return count;
	}

}
