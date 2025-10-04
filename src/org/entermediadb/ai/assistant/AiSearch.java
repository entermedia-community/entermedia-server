package org.entermediadb.ai.assistant;

import java.util.Collection;

import org.openedit.Data;

public class AiSearch {
	Collection<Data> fieldSelectedModules; //Name or IDs
	Collection<String> fieldKeywords;
	Collection<String> fieldFilters;

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
	
	public boolean isStrictSearch() {
		return fieldStrictSearch;
	}
	public void setStrictSearch(boolean inStrictSearch) {
		fieldStrictSearch = inStrictSearch;
	}
	
	public boolean isBulkSearch() {
		return bulkSearch;
	}
	public void setBulkSearch(boolean inBulkSearch) {
		bulkSearch = inBulkSearch;
	}
	
	public String toSemanticQuery() {
		return String.join(" ", fieldKeywords);
	}
	
}
