package com.openedit.entermedia.scripts;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.openedit.ModuleManager;
import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.config.Configuration;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.UserManager;
import com.openedit.util.PathUtilities;

public class ScriptManager
{
	private static final Log log = LogFactory.getLog(ScriptManager.class);
	
	protected Map fieldScriptRunners;
	protected ModuleManager fieldModuleManager;
	protected PageManager fieldPageManager;
	protected UserManager fieldUserManager;
	
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public Map getScriptRunners()
	{
		if (fieldScriptRunners == null)
		{
			fieldScriptRunners = new HashMap();
		}
		return fieldScriptRunners;
	}

	public void setScriptRunners(Map inScriptRunners)
	{
		fieldScriptRunners = inScriptRunners;
	}
	
	public ScriptRunner getRunner(String inName)
	{
		ScriptRunner runner = (ScriptRunner)getScriptRunners().get(inName);
		if( runner == null)
		{
			runner = (ScriptRunner)getModuleManager().getBean(inName + "ScriptRunner");
			getScriptRunners().put(inName,runner);
		}
		return runner;
	}
	
	public Script loadScript(String code) throws OpenEditException
	{
		try
		{

			Page scriptPage = getPageManager().getPage( code );

			Script script = new Script();
			script.setPage(scriptPage);
			if (code.endsWith(".bsh"))
			{
				script.setType("bsh");
			}
			else if( code.endsWith(".js"))
			{
				script.setType("rhino");
			}
			else if (code.endsWith(".groovy"))
			{
				script.setType("groovy");
			}
			else
			{
				script.setType("bsf");
			}

			return script;
		}
		catch (Exception ex)
		{
			throw new OpenEditException(ex);
		}
	}
	
	public Object execScript(Map variableMap, Script inScript) throws OpenEditException
	{
		ScriptRunner runner = getRunner(inScript.getType());

		ScriptLogger logger = (ScriptLogger)variableMap.get("log");
		if( logger == null)
		{
			logger = new ScriptLogger();
			logger.setPrefix(inScript.getPage().getName());
		}
		variableMap.put("log", logger);
		variableMap.put("userManager", getModuleManager().getBean( "userManager" ) ); 
		variableMap.put("moduleManager", getModuleManager() ); 
		variableMap.put("beanFactory", getModuleManager() ); 
		variableMap.put("pageManager", getPageManager() ); 
		variableMap.put("root", getModuleManager().getBean( "root" ) ); 
		
		logger.debug("Running "  +inScript.getPage() );
		
		Object returned = runner.exec(inScript, variableMap);
		return returned;
		
	}

	public Script loadScript(WebPageRequest inContext, Configuration inScriptconfig, String inFilepath)
	{
		String code = inScriptconfig.getValue();
		//log.info("Start value: " + code);
		code = inContext.getPage().getPageSettings().replaceProperty(code);
		//log.info("Replaced value: " + code);
		code = PathUtilities.resolveRelativePath(code, inFilepath);
		//log.info("Final script: " + code + " using " + inFilepath);
		Script script = loadScript(code);
		String method = inScriptconfig.getAttribute("method");
		script.setMethod(method);
		script.setConfiguration(inScriptconfig);
		
		
		return script;
	}

}
