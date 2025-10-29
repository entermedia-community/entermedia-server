package org.entermediadb.ai.assistant;

import org.json.simple.JSONObject;
import org.openedit.util.DateRange;

public class AiSearchPart extends JSONObject
{
	public String getTargetTable()
	{
		return (String)get("targettable");
	}
	public void setTargetTable(String inTargetTable)
	{
		put("targettable",inTargetTable);
	}
	public String getParameterName()
	{
		return (String)get("parametername");
	}
	public void setParameterName(String inParameterName)
	{
		put("parametername",inParameterName);
	}
	public String getParameterValue()
	{
		return (String)get("parametervalue");
	}
	public void setParameterValue(String parametervalue)
	{
		put("parametervalue",parametervalue);
	}
	DateRange fieldDateRange;
	
	public DateRange getDateRange()
	{
		return fieldDateRange;
	}
	public void setDateRange(DateRange inDateRange)
	{
		fieldDateRange = inDateRange;
	}


}
