package org.entermediadb.events;

import java.util.Iterator;

public class ScheduledTask extends BaseTask
{
	protected String fieldFormattedPeriod;
	protected long fieldDelay = 0;
	protected long fieldPeriod = 0;
	protected String fieldFormattedDelay;
	protected boolean fieldEnabled = true;
	
	public ScheduledTask()
	{
		
	}
	
	public ScheduledTask( long inDelay, long inPeriod)
	{
		setDelay(inDelay);
		setPeriod(inPeriod);
	}
	
	public ScheduledTask(String inDelay, String inPeriod)
	{
		setDefaults();
		if (inDelay != null && inDelay.length() > 0)
		{
			setDelay(inDelay);
		}
		if (inPeriod != null && inPeriod.length() > 0)
		{
			setPeriod(inPeriod);
		}
		
	}
	

	
	public long getPeriod()
	{
		return fieldPeriod;
	}
	public void setPeriod(long inPeriod)
	{
		fieldPeriod = inPeriod;
	}
	public void setPeriod(String inPeriod)
	{
		fieldPeriod = parse(inPeriod);
		fieldFormattedPeriod = inPeriod;
	}
	
	public String getFormattedPeriod()
	{
		return fieldFormattedPeriod;
	}
	
	private void setDefaults()
	{
		setPeriod(12 * 60 * 60 * 1000); //defaults to runing every 12 hours
		setDelay(60000L); //defaults to one minute
	}
	
	public long getDelay()
	{
		return fieldDelay;
	}
	public String getFormattedDelay()
	{
		return fieldFormattedDelay;
	}
	public void setDelay(long inDelay)
	{
		fieldDelay = inDelay;
	}
	public void setDelay(String inDelay)
	{
		fieldDelay = parse(inDelay);
		fieldFormattedDelay = inDelay;
	}
	
	/**
	 * @param inPeriodString
	 * @return
	 */
	private long parse(String inPeriodString)
	{
		if (inPeriodString == null)
		{
			return 0;
		}
		
		inPeriodString = inPeriodString.trim().toLowerCase();
		
		if ( inPeriodString.endsWith("d"))
		{
			long days = Long.parseLong(inPeriodString.substring(0,inPeriodString.length()-1));
			long period = days * 24 * 60L * 60L * 1000L;
			return period;
		}
		else if ( inPeriodString.endsWith("h"))
		{
			long hours = Long.parseLong(inPeriodString.substring(0,inPeriodString.length()-1));
			long period = hours * 60L * 60L * 1000L;
			return period;
		}
		else if ( inPeriodString.endsWith("m"))
		{
			long min = Long.parseLong(inPeriodString.substring(0,inPeriodString.length()-1));
			long period = min * 60L * 1000L;
			return period;
		}
		else if ( inPeriodString.endsWith("s"))
		{
			long sec = Long.parseLong(inPeriodString.substring(0,inPeriodString.length()-1));
			long period = sec * 1000L;
			return period;
		}
		else
		{
			long period = Long.parseLong( inPeriodString );
			return period;
		}
	}
	
	public void putProperty(String key, String value)
	{
		if (key.equals("id"))
		{
			setId(value);
		}
		else if (key.equals("username"))
		{
			//This is3 handled in XMLSchedulerArchive.loadFromFile()
		}
		else if (key.equals("name"))
		{
			setName(value);
		}
		else if (key.equals("path"))
		{
			//THis is handled in XMLSchedulerArchive.loadFromFile()
		}
		else if(key.equals("startdelay"))
		{
			setDelay(value);
		}
		else if (key.equals("period"))
		{
			setPeriod(value);
		}
		else if (key.equals("enabled"))
		{
			setEnabled(Boolean.parseBoolean(value));
		}
		else
		{
			super.putProperty(key, value);
		}
	}
	
	public boolean isEnabled() {
		return fieldEnabled;
	}

	public void setEnabled(boolean inEnabled) {
		fieldEnabled = inEnabled;
	}
	
	public BaseTask copy()
	{
		ScheduledTask task = new ScheduledTask();

		task.fieldFormattedPeriod = fieldFormattedPeriod;
		task.fieldDelay = fieldDelay;
		task.fieldPeriod = fieldPeriod;
		task.fieldFormattedDelay = fieldFormattedDelay;
		task.fieldUser = fieldUser;
		task.fieldId = fieldId;
		task.fieldName = fieldName;
		task.fieldWorkflowID = fieldWorkflowID;
		task.fieldActionIndex = fieldActionIndex;
		task.fieldActions = fieldActions;
		for (Iterator i = getPropertyNameIterator(); i.hasNext();)
		{
			String propertyName = (String) i.next();
			task.putProperty(propertyName, getProperty(propertyName));
		}
		task.fieldAsleep = fieldAsleep;
		task.fieldEnabled = fieldEnabled;
		return task;
	}
	

}
