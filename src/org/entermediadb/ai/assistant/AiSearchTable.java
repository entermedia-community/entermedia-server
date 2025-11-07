package org.entermediadb.ai.assistant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.util.DateRange;

public class AiSearchTable extends JSONObject
{
	
	protected Long fieldCount;
	protected Collection<Data> fieldModules = new ArrayList();
	protected AiSearchTable fieldForeignTable;
	
	public AiSearchTable getForeignTable()
	{
		return fieldForeignTable;
	}


	public void setForeignTable(AiSearchTable inForeingTable)
	{
		fieldForeignTable = inForeingTable;
	}


	public Collection<Data> getModules()
	{
		return fieldModules;
	}


	public void setModules(Collection<Data> inModules)
	{
		fieldModules = inModules;
	}


	public Boolean hasMultipleTables()
	{
		if (getModules() != null && getModules().size() > 1) 
		{
		return true;
		}
		
		return false;
	}
	
	public Data getModule()
	{
		if (fieldModules!= null && !fieldModules.isEmpty())
		{
			return fieldModules.iterator().next();
		}
		return null;
	}

	public void setModule(Data inModule)
	{
		getModules().clear();
		getModules().add(inModule);
	}

	public void addModule(Data inModule)
	{
		getModules().add(inModule);
	}
	

	public Long getCount()
	{
		return fieldCount;
	}
	public void setCount(Long inCount)
	{
		fieldCount = inCount;
	}
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
