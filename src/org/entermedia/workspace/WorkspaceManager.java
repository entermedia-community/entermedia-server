package org.entermedia.workspace;

import org.openedit.data.SearcherManager;

import com.openedit.page.manage.PageManager;

public class WorkspaceManager
{
	protected SearcherManager fieldSearcherManager;
	protected PageManager fieldPageManager;
	
	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}
	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}
	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}
	
}
