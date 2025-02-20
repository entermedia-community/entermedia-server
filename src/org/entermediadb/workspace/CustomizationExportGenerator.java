/*
 * Created on Apr 22, 2006
 */
package org.entermediadb.workspace;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.generators.BaseGenerator;
import org.openedit.generators.Output;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;

public class CustomizationExportGenerator extends BaseGenerator
{
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

	private static final Log log = LogFactory.getLog(CustomizationExportGenerator.class);
	public void generate(WebPageRequest inReq, Page inPage, Output inOut) throws OpenEditException
	{
		try
		{
			String catalogid = inReq.findPathValue("catalogid");
			String[] configids = inReq.getRequestParameters("configid");
			if( configids == null )
			{
				String[] moduleids = inReq.getRequestParameters("moduleid");
				MediaArchive archive  = (MediaArchive)getWorkspaceManager().getSearcherManager().getModuleManager().getBean(catalogid,"mediaArchive");

				HitTracker hits = archive.query("customization").or().orgroup("targetid",moduleids).orgroup("moduleids",moduleids).search();
				configids = (String[]) hits.collectValues("id").toArray(new String[hits.size()]);
			}
			getWorkspaceManager().exportCustomizations(catalogid, configids, inOut.getStream());
		}
		catch ( Exception ex)
		{
			log.error(ex);
		}
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
