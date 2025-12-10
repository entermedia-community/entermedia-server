package org.entermediadb.ai.assistant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.openedit.Data;
import org.openedit.data.BaseData;
import org.openedit.util.DateRange;

public class AiSearchTable extends BaseData
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
		setValue("targettable",inTargetTable);
	}
	public String getParameterName()
	{
		return (String)get("parametername");
	}
	
	public void setParameters(Map<String, String>inParameters)
	{
		setValue("parameters", inParameters);
	} 
	
	public Map<String, Object> getParameters()
	{
		Map<String, Object> params = (Map<String, Object>) getValue("parameters");
		return params;
	}
	
	public String getParameterValues() {
		String returnvalues = "";
		Map<String, Object> params = getParameters();
		if (params != null) {
			 
			 for (Map.Entry<String, Object> entry : params.entrySet())
			{
				String key = entry.getKey();
				Object val = entry.getValue();
			
				if (val instanceof String) {
					returnvalues += val.toString();
				}
				else if (val instanceof JSONArray) {
					String values2 = null;
					for (Object obj : (JSONArray) val) {
						if (values2 == null) {
							values2 = obj.toString();
						} else {
							values2 += " " + obj.toString();
						}
					}
					returnvalues = String.join(" ", values2);
				}
			}
			
		}
		return returnvalues;
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
