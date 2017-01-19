package org.entermediadb.scripts;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.config.Configuration;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.users.UserManager;
import org.openedit.util.PathUtilities;

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
		//variableMap.put("userManager", getModuleManager().getBean( "userManager" ) ); //now catalogid based.
		variableMap.put("moduleManager", getModuleManager() ); 
		variableMap.put("beanFactory", getModuleManager() ); 
		variableMap.put("pageManager", getPageManager() ); 
		variableMap.put("root", getModuleManager().getBean( "root" ) ); 
		
		Object returned = null;
		long start = System.currentTimeMillis();
		try
		{
		logger.startCapture();
		logger.debug("Running "  +inScript.getPage() );
		returned = runner.exec(inScript, variableMap);
		}
		catch( Throwable ex)
		{
			logger.error("Error running "  +inScript.getPage(), ex );
		}
		finally 
		{
			long used = System.currentTimeMillis() - start;
			logger.debug("Completed in "  + (used / 1000L) + " seconds" );
			logger.stopCapture();
		}
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
