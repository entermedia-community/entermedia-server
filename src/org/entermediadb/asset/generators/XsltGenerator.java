/*
Copyright (c) 2003 eInnovation Inc. All rights reserved

This library is free software; you can redistribute it and/or modify it under the terms
of the GNU Lesser General Public License as published by the Free Software Foundation;
either version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Lesser General Public License for more details.
*/

/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.entermediadb.asset.generators;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Generator;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.generators.BaseGenerator;
import org.openedit.generators.Output;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.util.PathUtilities;


/**
 * This generator transforms XML input through an XSLT stylesheet.
 *
 * @author Eric Galluzzo
 * @author Matt Avery, mavery@einnovation.com
 */
public class XsltGenerator extends BaseGenerator implements Generator
{
	private static Log log = LogFactory.getLog(XsltGenerator.class);
	protected PageManager fieldPageManager;
	protected 	ThreadLocal fieldTransformers;
	/**
	 * Run the given page through the stylesheet specified in its config file and output the result
	 * to the given output stream.  This generator accepts a configuration which looks like this:
	 * <pre>
	 * &lt;generator name="xslt"&gt;
	 *   &lt;stylesheet&gt;/path/to/my/stylesheet.xsl&lt;/stylesheet&gt;
	 *   [&lt;use-request-parameters&gt;true&lt;/use-request-parameters&gt;]
	 *   [&lt;generator&gt;...&lt;/generator&gt;]
	 * &lt;/generator&gt;
	 * </pre>
	 *
	 * @see Generator#generate(Page, WebPageContext)
	 */
	public void generate( WebPageRequest inContext, Page inPage, Output inOut )
		throws OpenEditException
	{
		// Put all the request parameters as parameters to the stylesheet, if
		// the user so desired.  In addition, pass "requestURI" and
		// "queryString" parameters, which have the obvious values.
		try
		{
			String template = inContext.findValue("xsltlayout");
			Transformer transformer = getTransformer(template);

			if ( Boolean.parseBoolean(inContext.findValue("xsltaddrequestparameters") ) )
			{
				populateParameters( inContext, transformer );
			}

			StreamResult result = new StreamResult(inOut.getWriter());
			String home = (String) inContext.getPageValue("home");
			transformer.setParameter("home", home);

			if( !inPage.exists())
			{
				inPage = getPageManager().getPage(PathUtilities.extractPagePath(inPage.getPath()) + ".xml");
			}
			StreamSource xmlSource = new StreamSource(inPage.getReader());
			transformer.transform(xmlSource, result);
			transformer.clearParameters();
			inOut.getWriter().flush();
		}
		catch (Exception ex)
		{
			throw new OpenEditException(ex);
		}
	}
	protected 	ThreadLocal getTransformers()
	{
		if( fieldTransformers == null)
		{
			fieldTransformers = new ThreadLocal();
		}
		return fieldTransformers;
	}
	protected Transformer getTransformer(String inXslt) throws Exception
	{
		Page style = getPageManager().getPage( inXslt );
		long mod = style.getContentItem().getLastModified();
		Map map = (Map)getTransformers().get();
		if( map == null)
		{
			map = new HashMap();
			getTransformers().set(map);
		}
		TransformerModDate trans = (TransformerModDate)map.get(style.getContentItem().getActualPath());
		if ( trans == null || trans.modDate != mod)
		{
			Source source = new StreamSource(style.getReader());
			trans = new TransformerModDate();
			trans.modDate = mod;
			Transformer transform = TransformerFactory.newInstance().newTransformer(source);
			trans.transformer = transform;
			map.put(style.getContentItem().getActualPath(), trans);
		}
		return trans.transformer;
	}

	/**
	 * @param inContext
	 */
	protected void populateParameters( WebPageRequest inContext, Transformer inTrans ) throws Exception
	{
		HttpServletRequest request = inContext.getRequest();
		inTrans.setParameter("requestURI", request.getRequestURI());
		inTrans.setParameter("queryString", request.getQueryString());
		
		for (Enumeration e = request.getParameterNames(); e.hasMoreElements();)
		{
			String name = (String) e.nextElement();
			inTrans.setParameter(name, request.getParameter(name));
		}
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}
	public void setPageManager( PageManager pageManager )
	{
		fieldPageManager = pageManager;
	}

	
	class TransformerModDate 
	{
		protected long modDate;
		protected Transformer transformer;
	}
}