/*
 * Created on Apr 22, 2006
 */
package org.openedit.entermedia.generators;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermedia.workspace.WorkspaceManager;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.generators.BaseGenerator;
import com.openedit.generators.Output;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.util.PageZipUtil;
import com.openedit.util.PathUtilities;

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
