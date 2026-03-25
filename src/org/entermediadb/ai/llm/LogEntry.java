package org.entermediadb.ai.llm;

import java.util.Date;

import org.openedit.Data;

public class LogEntry
{
	public LogEntry(String inType,String inMessage)
	{
		setLogType(inType);
		setMessage(inMessage);		
	}
	
	protected Date fieldDate;
	
	public Date getDate()
	{
		return fieldDate;
	}
	public void setDate(Date inDate)
	{
		fieldDate = inDate;
	}

	protected String fieldLogType;
	public String getLogType()
	{
		return fieldLogType;
	}
	public void setLogType(String inLogType)
	{
		fieldLogType = inLogType;
	}
	public String getMessage()
	{
		return fieldMessage;
	}
	public void setMessage(String inMessage)
	{
		fieldMessage = inMessage;
	}
	public Data getCurrentAgentEnabledConfig()
	{
		return fieldCurrentAgentEnabledConfig;
	}
	public void setCurrentAgentEnabledConfig(Data inCurrentAgentEnabledConfig)
	{
		fieldCurrentAgentEnabledConfig = inCurrentAgentEnabledConfig;
	}
	protected String fieldMessage;
	protected Data fieldCurrentAgentEnabledConfig;
}
