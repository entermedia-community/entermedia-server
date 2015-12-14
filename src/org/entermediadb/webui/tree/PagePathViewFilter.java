package org.entermediadb.webui.tree;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.page.Page;
import org.openedit.page.Permission;
import org.openedit.page.manage.PageManager;
import org.openedit.util.strainer.BaseFilter;
import org.openedit.util.strainer.FilterException;

public class PagePathViewFilter extends BaseFilter
{
	private static final Log log = LogFactory.getLog(PagePathViewFilter.class);
	
	PageManager fieldPageManager;
	WebPageRequest fieldLoadingWebPageRequest;
	
	public boolean passes(Object inObj) throws FilterException
	{
		String path = (String)inObj;
		try
		{
			Page page = getPageManager().getPage(path);
			Permission view = page.getPermission("view");
			if( view != null)
			{
				WebPageRequest copy = getLoadingWebPageRequest().copy(page);
				boolean ok = view.passes(copy);
				return ok;
			}
			return true;
		} catch ( OpenEditException ex)
		{
			throw new FilterException(ex);
		}
		finally
		{
			getPageManager().clearCache(path);
		}
		
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public WebPageRequest getLoadingWebPageRequest()
	{
		return fieldLoadingWebPageRequest;
	}

	public void setLoadingWebPageRequest(WebPageRequest inLoadingWebPageRequest)
	{
		fieldLoadingWebPageRequest = inLoadingWebPageRequest;
	}
	
	public String toString() 
	{
		return "Path=" + getValue();
	}
}
