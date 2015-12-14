/*
 * Created on Apr 22, 2006
 */
package org.entermediadb.asset.generators;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.workspace.WorkspaceManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.generators.BaseGenerator;
import org.openedit.generators.Output;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;

public class WorkspaceExportGenerator extends BaseGenerator
{
	protected File fieldRoot;
	protected PageManager pageManager;
	protected WorkspaceManager fieldWorkspaceManager;
	
	public WorkspaceManager getWorkspaceManager()
	{
		return fieldWorkspaceManager;
	}

	public void setWorkspaceManager(WorkspaceManager inWorkspaceManager)
	{
		fieldWorkspaceManager = inWorkspaceManager;
	}

	private static final Log log = LogFactory.getLog(WorkspaceExportGenerator.class);
	public void generate(WebPageRequest inReq, Page inPage, Output inOut) throws OpenEditException
	{
		try
		{
			String apppath = inReq.getRequestParameter("apppath");
			getWorkspaceManager().exportWorkspace(apppath, inOut.getStream());
		}
		catch ( Exception ex)
		{
			log.error(ex);
		}
	}

	protected File getRoot()
	{
		return fieldRoot;
	}

	public void setRoot(File inRoot)
	{
		fieldRoot = inRoot;
	}
	public boolean canGenerate(WebPageRequest inReq)
	{
		return true;
	}

	public PageManager getPageManager()
	{
		return pageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		pageManager = inPageManager;
	}

}
