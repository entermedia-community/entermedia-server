package org.entermediadb.ai.assistant;

import java.util.Collection;

public class AiSearch {
	Collection<String> fieldSelectedModules;
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
	
	public Collection<String> getSelectedModules() {
		return fieldSelectedModules;
	}
	public void setSelectedModules(Collection<String> inModules) {
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
	
}
