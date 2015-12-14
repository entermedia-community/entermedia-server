package org.entermediadb.scripts;

import java.util.logging.LogRecord;

public class LogEntry
{
	public LogEntry()
	{
		// TODO Auto-generated constructor stub
	}
	public LogEntry(LogRecord inRecord)
	{
		setLogRecord(inRecord);
	}
	protected LogRecord fieldLogRecord;
	
	public LogRecord getLogRecord()
	{
		return fieldLogRecord;
	}

	public void setLogRecord(LogRecord inLogRecord)
	{
		fieldLogRecord = inLogRecord;
	}

	public String getName()
	{
		return getLogRecord().getSourceClassName();
	}
	public String getMethod()
	{
		return getLogRecord().getSourceMethodName();
	}
	
	public String getMessage()
	{
		return getLogRecord().getMessage();
	}
	public String toString()
	{
		String prefix = "";
		if( getLogRecord().getLevel().intValue() > 800)
		{
			prefix = "ERROR: ";
		}
		return prefix + getMessage() + " [" + getName()+ "#" + getMethod() + "]";
	}
}
