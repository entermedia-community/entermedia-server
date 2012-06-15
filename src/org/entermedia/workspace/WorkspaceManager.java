package org.entermedia.workspace;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.zip.ZipOutputStream;

import org.openedit.Data;
import org.openedit.data.SearcherManager;

import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.util.PageZipUtil;

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
	
	public void exportWorkspace(String apppath, OutputStream inOut) throws Exception
	{
		Page apppage = getPageManager().getPage(apppath);
		String catalogid = apppage.get("catalogid"); 
		
		
		PageZipUtil pageZipUtil = new PageZipUtil(getPageManager());
		//pageZipUtil.setFolderToStripOnZip(false);
		
		ZipOutputStream finalZip = new ZipOutputStream(inOut);
		Collection files = getSearcherManager().getList("media","workspacefiles");
		for (Iterator iterator = files.iterator(); iterator.hasNext();)
		{
			Data folder = (Data) iterator.next();
			String path = folder.getName();
			path = apppage.replaceProperty(path);
			pageZipUtil.zip(path, finalZip);
		}
		finalZip.close();
	}
}
