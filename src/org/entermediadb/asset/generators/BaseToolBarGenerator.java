/*
 * Created on Dec 28, 2004
 */
package org.entermediadb.asset.generators;

import java.io.IOException;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Generator;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.generators.BaseGenerator;
import org.openedit.generators.Output;
import org.openedit.page.manage.PageManager;

/**
 * Inserts a decoration header just after the opening "body" tag
 * and inserts a decoration footer just before the closing "body" tag.
 * 
 * @author Matthew Avery, mavery@einnovation.com
 */
public abstract class BaseToolBarGenerator extends BaseGenerator
{
	public static Log log = LogFactory.getLog(BaseToolBarGenerator.class);

	protected Generator fieldWraps;
	protected PageManager fieldPageManager;
	protected String fieldHeaderPath;
	protected String fieldFooterPath;
	
	public BaseToolBarGenerator()
	{
	}
	protected void debug( String inMessage )
	{
		//log.debug( inMessage );
	}
	protected void writePage( String pageContent, Output inOut ) throws OpenEditException
	{
		//write it out to wout
		Writer wout = inOut.getWriter();
		try
		{
			wout.write(pageContent); //this content is actually just a string
			wout.flush();
		}
		catch (IOException ex)
		{
			throw new OpenEditException(ex);
		}
	}
	public String getHeaderPath()
	{
		return fieldHeaderPath;
	}
	public void setHeaderPath(String inHeaderPath)
	{
		fieldHeaderPath = inHeaderPath;
	}
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}
	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public Generator getWraps()
	{
		return fieldWraps;
	}

	public void setWraps(Generator inWraps)
	{
		fieldWraps = inWraps;
	}

	public String getFooterPath()
	{
		return fieldFooterPath;
	}

	public void setFooterPath(String inFooterPath)
	{
		fieldFooterPath = inFooterPath;
	}
	public boolean canGenerate(WebPageRequest inReq)
	{
		return getWraps().canGenerate(inReq);
	}
}
