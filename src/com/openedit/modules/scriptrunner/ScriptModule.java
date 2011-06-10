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
package com.openedit.modules.scriptrunner;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.repository.ContentItem;
import org.openedit.repository.filesystem.StringItem;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.config.Configuration;
import com.openedit.entermedia.scripts.Script;
import com.openedit.entermedia.scripts.ScriptLogger;
import com.openedit.entermedia.scripts.ScriptManager;
import com.openedit.entermedia.scripts.ScriptRunner;
import com.openedit.modules.BaseModule;
import com.openedit.page.Page;
import com.openedit.page.PageRequestKeys;
import com.openedit.util.PathUtilities;

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
		 
		Configuration scriptconfig = context.getCurrentAction().getConfig().getChild("script");
		
		Script script = getScriptManager().loadScript(context, scriptconfig, filepath);

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






}
