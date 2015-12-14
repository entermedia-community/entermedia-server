/*
Copyright (c) 2003 eInnovation Inc. All rights reserved

This library is free software; you can redistribute it and/or modify it under the terms
of the GNU Lesser General Public License as published by the Free Software Foundation;
either version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Lesser General Public License for more details.
*/

/*--

 Copyright (C) 2001-2002 Anthony Eden.
 All rights reserved.

 */
package org.entermediadb.modules.scriptrunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.scripts.Script;
import org.entermediadb.scripts.ScriptLogger;
import org.entermediadb.scripts.ScriptManager;
import org.entermediadb.scripts.TextAppender;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.config.Configuration;
import org.openedit.modules.BaseModule;
import org.openedit.page.Page;
import org.openedit.page.PageRequestKeys;
import org.openedit.repository.ContentItem;
import org.openedit.repository.filesystem.StringItem;
import org.openedit.util.PathUtilities;

/**
 * An action which is implemented in a BSF supported scripting language.  Script     actions have
 * access to several varibles:
 * 
 * <p>
 * These are always available:
 * </p>
 * 
 * <p>
 * <b>site</b> - The SiteContext<br><b>syslog</b> - Standard logging stream (Log4J Category)<br>
 * </p>
 * 
 * <p>
 * If there is a context defined when the action is executed (all     actions excluding startup
 * actions):
 * </p>
 * 
 * <p>
 * <b>context</b> - The current context<br>
 * <b>application</b> - The ServletContext<br>
 * <b>request</b> - The HTTP request<br>
 * <b>response</b> - The HTTP response<br>
 * <b>session</b> - The HTTP session<br>
 * <b>page</b> - The Page object<br>
 * </p>
 *
 * @author Anthony Eden
 * @author Matt Avery, mavery@einnovation.com (converted to Spring framework)
 */
public class ScriptModule extends BaseModule implements PageRequestKeys
{

	public static final String SCRIPT_TAG = "script";
	private static Log log = LogFactory.getLog(ScriptModule.class);
	protected ScriptManager fieldScriptManager;
	public ScriptModule()
	{
		super();
	}
	
	public ScriptManager getScriptManager()
	{
		if( fieldScriptManager == null)
		{
			fieldScriptManager = (ScriptManager)getModuleManager().getBean("scriptManager");
		}
		return fieldScriptManager;
	}

	public void setScriptManager(ScriptManager inScriptManager)
	{
		fieldScriptManager = inScriptManager;
	}


	
	/**
	 * Execute the action script represented by this ScriptAction.
	 *
	 * @param context The current context
	 *
	 * @throws OpenEditException
	 */
	public Object run( WebPageRequest context) throws OpenEditException
	{
		String filepath = context.getPath();
		
		Script script = null;
		
		Configuration scriptconfig = context.getCurrentAction().getConfig().getChild("script");
			script = getScriptManager().loadScript(context, scriptconfig, filepath);			

		Map variableMap = context.getPageMap();
		variableMap.put("context", context );
		Object returned = getScriptManager().execScript(variableMap, script);
		if( returned != null)
		{
			context.putPageValue("return",returned);
		}
		
//		context.putPageValue("logs", logs.getLogs());
		return returned;
	}
	
	public void debugScript(WebPageRequest inReq ) throws Exception
	{
		String path = inReq.findValue("scriptpath");
		Script script = getScriptManager().loadScript(path);

		final StringBuffer output = new StringBuffer();
		TextAppender appender = new TextAppender()
		{
			public void appendText(String inText)
			{
				output.append(inText);
				output.append("<br>");
			}
		};
		
		ScriptLogger logs = new ScriptLogger();
		logs.setPrefix(script.getType());
		logs.setTextAppender(appender);
		try
		{
			logs.startCapture();
			Map variableMap = inReq.getPageMap();
			variableMap.put("context", inReq );
			variableMap.put("log", logs );
			
			Object returned = getScriptManager().execScript(variableMap, script);
			if( returned != null)
			{
				output.append("returned: " + returned);
			}
		}
		finally
		{
			logs.stopCapture();
		}
		inReq.putPageValue("output",output);
	}
	
//		
//		if (path.endsWith(".bsh"))
//		{
//			
//			execBshScript(variableMap, path);
//			return;
//		}
//		if (path.endsWith(".groovy"))
//		{
//			execGroovyScript(variableMap, path);
//			return;
//		}
//
//		log.info("Executing script: " + script.getDescription());
//		
//		try
//		{
//			if (script.getDescription().endsWith(".js"))
//			{
//				execWithRhino(script, variableMap);
//
//				//execWithBsf(context, configuration);
//			}
//			else
//			{
//				execWithBsf(script, variableMap);
//			}
//		}
//		catch (Exception e)
//		{
//			if ( e instanceof OpenEditException )
//			{
//				throw (OpenEditException)e;
//			}
//			throw new OpenEditException(e);
//		}


	public void saveScript(String code, String filepath, String inScript) throws OpenEditException
	{
		try
		{
			String relativecode = PathUtilities.buildRelative(code, filepath);
			Page scriptPage = getPageManager().getPage( relativecode );
			ContentItem scriptItem = new StringItem( relativecode, inScript,scriptPage.getCharacterEncoding() );
			scriptPage.setContentItem( scriptItem );
			getPageManager().putPage( scriptPage );
		}
		catch (Exception ex)
		{
			throw new OpenEditException(ex);
		}
	}


	public List listScripts(WebPageRequest inReq) throws Exception
	{
		String scriptroot = inReq.findValue("scriptroot");
		List pages = new ArrayList();
		Set dups = new HashSet();
		findScripts(scriptroot,scriptroot, pages, dups);
		Collections.sort(pages, new Comparator<Page>()
				{
			public int compare(Page o1, Page o2) {
				return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
			};
		});
		inReq.putPageValue("scripts", pages);
		return pages;
	}

	protected void findScripts(String scriptroot, String inPath,  List pages, Set dups)
	{
		List scripts = getPageManager().getChildrenPaths(inPath + "/", true);
		for (Iterator iterator = scripts.iterator(); iterator.hasNext();)
		{
			String path = (String) iterator.next();
			if (!path.endsWith(".xconf"))
			{
				Page page = getPageManager().getPage(inPath + "/" + PathUtilities.extractFileName(path),true);
				if( page.isFolder() || !page.exists()  )
				{
					findScripts( scriptroot,page.getPath(),pages, dups);
				}
				else
				{
					//findScripts(scriptroot, pages, dups, path, page);
					if( !dups.contains( page.getPath() ) )
					{
						pages.add(page);
						dups.add(page.getPath());
					}
					
					
				}
			}
		}
	}

	public void saveScript(WebPageRequest inReq) throws Exception
	{
		String scriptpath = inReq.findValue("scriptpath");
		String code = inReq.findValue("scriptcode");

		Page page = getPageManager().getPage(scriptpath);
		getPageManager().saveContent(page, inReq.getUser(), code, "web edit");

	}




}
