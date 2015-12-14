package org.entermediadb.data;

import org.openedit.hittracker.SearchQuery;

public class FileSearchQuery extends SearchQuery {

	protected boolean fieldRecursive;
	protected boolean fieldIncludeFallback;
    protected String fieldRootFolder;
    protected boolean includeVersions = false;
    
    
	public boolean isIncludeVersions() {
		return includeVersions;
	}

	public void setIncludeVersions(boolean inIncludeVersions) {
		includeVersions = inIncludeVersions;
	}

	public String getRootFolder() {
		return fieldRootFolder;
	}

	public void setRootFolder(String inRootFolder) {
		fieldRootFolder = inRootFolder;
	}

	public boolean isIncludeFallback() {
		return fieldIncludeFallback;
	}

	public void setIncludeFallback(boolean inIncludeFallback) {
		fieldIncludeFallback = inIncludeFallback;
	}

	public boolean isRecursive() {
		return fieldRecursive;
	}

	public void setRecursive(boolean fieldRecursive) {
		this.fieldRecursive = fieldRecursive;
	}
	
	
	
}
