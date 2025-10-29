package org.entermediadb.ai.assistant;

import java.util.Collection;

import org.openedit.Data;
import org.openedit.util.DateRange;

public class AiSearch 
{
	
	Collection<Data> fieldSelectedModules; //Name or IDs
	Collection<String> fieldKeywords;
	Collection<String> fieldFilters;

	DateRange fieldDateRange;
	public Data getParentModule()
	{
		return fieldParentModule;
	}
	public void setParentModule(Data inParentModule)
	{
		fieldParentModule = inParentModule;
	}
	public Data getChildModule()
	{
		return fieldChildModule;
	}
	public void setChildModule(Data inChildModule)
	{
		fieldChildModule = inChildModule;
	}
	public boolean isStrictSearch()
	{
		return fieldStrictSearch;
	}
	public void setStrictSearch(boolean inStrictSearch)
	{
		fieldStrictSearch = inStrictSearch;
	}

	Data fieldParentModule;
	Data fieldChildModule;
	
	
	public DateRange getDateRange()
	{
		return fieldDateRange;
	}
	public void setDateRange(DateRange inDateRange)
	{
		fieldDateRange = inDateRange;
	}

	boolean fieldStrictSearch;
	boolean bulkSearch;
	
	public Collection<String> getKeywords() {
		return fieldKeywords;
	}
	public void setKeywords(Collection<String> inKeywords) {
		fieldKeywords = inKeywords;
	}
	
	public Collection<Data> getSelectedModules() {
		return fieldSelectedModules;
	}
	public Collection<String> getSelectedModuleIds() {
		if( fieldSelectedModules == null ) {
			return null;
		}
		return fieldSelectedModules.stream().map(m -> m.getId()).toList();
	}
	public void setSelectedModules(Collection<Data> inModules) {
		fieldSelectedModules = inModules;
	}
	
	public Collection<String> getFilters() {
		return fieldFilters;
	}
	
	public void setFilters(Collection<String> inFilters) {
		fieldFilters = inFilters;
	}
	
	public String toSemanticQuery() {
		return String.join(" ", fieldKeywords);
	}
	
}
