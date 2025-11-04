package org.entermediadb.ai.assistant;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

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
	
	public void setParameters(Map<String, String>inParameters)
	{
		put("parameters",inParameters);
	} 
	
	public Map<String, String> getParameters()
	{
		Map<String, String> params = (Map<String, String>) get("parameters");
		return params;
	}
	
	public String getParameterValues() {
		String values = null;
		Map<String, String> params = getParameters();
		if (params != null) {
			values = String.join(" ", params.values());
		}
		return values;
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
