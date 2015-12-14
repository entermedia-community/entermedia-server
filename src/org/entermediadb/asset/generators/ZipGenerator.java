/*
 * Created on Apr 22, 2006
 */
package org.entermediadb.asset.generators;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.generators.BaseGenerator;
import org.openedit.generators.Output;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.util.PageZipUtil;
import org.openedit.util.PathUtilities;

public class ZipGenerator extends BaseGenerator
{
	protected File fieldRoot;
	protected PageManager pageManager;
	private static final Log log = LogFactory.getLog(ZipGenerator.class);
	public void generate(WebPageRequest inReq, Page inPage, Output inOut) throws OpenEditException
	{
		String path = inReq.getRequestParameter("path");
		if (path.indexOf("..") > -1)
		{
			throw new OpenEditException("Illegal path name");
		}
		//TODO: Add more security checks
		if( inReq.getUser() == null)
		{
			throw new OpenEditException("Illegal user");			
		}
		path = PathUtilities.resolveRelativePath( path, "/");

//		File root = new File( getRoot(), path);
		try
		{
			log.info("Zip up:" + path);
			PageZipUtil pageZipUtil = new PageZipUtil(getPageManager());
			String strip = inReq.getRequestParameter("stripfolders");
			if( strip != null)
			{
				pageZipUtil.setFolderToStripOnZip(strip);
			}
			pageZipUtil.setRoot(getRoot());
			pageZipUtil.zipFile(path, inOut.getStream());
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
