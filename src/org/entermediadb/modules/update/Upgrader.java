package org.entermediadb.modules.update;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.modules.scriptrunner.ScriptModule;
import org.entermediadb.scripts.Script;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.PlugIn;
import org.openedit.WebPageRequest;

public class Upgrader
{
	private static final Log log = LogFactory.getLog(Upgrader.class);
	
	protected ScriptModule fieldScriptModule;
	protected File fieldRoot;
	protected PlugInFinder fieldPlugInFinder;
	protected String[] fieldToUpgrade;
	protected Set fieldCompleted;
	protected Set fieldInProgress;
	protected boolean cancel = false;
	
	
	public String[] getList()
	{
		return fieldToUpgrade;
	}
	public void setToUpgrade(String[] inList)
	{
		fieldToUpgrade = inList;
	}
	public List upgrade( String inPlugInId, WebPageRequest inContext )
	{
		ScriptLogger logger = new ScriptLogger();
		logger.startCapture();
		try
		{
			doUpgrade(inPlugInId, inContext, logger);
		}
		finally
		{
			logger.stopCapture();
		}
		return logger.listLogs();
	}
	
	protected void doUpgrade( String inPlugInId, WebPageRequest inContext,ScriptLogger inLogger )
	{
		if( getInProgress().contains(inPlugInId))
		{
			log.info(inPlugInId + " is in progress");
			return;
		}
		if( cancel)
		{
			log.info(inPlugInId + " is canceled");
			return;
		}
		getInProgress().add(inPlugInId);
		
		String strOutputFile = "/WEB-INF/install.js";
		File out = new File(getRoot(), strOutputFile);
		PlugIn plugin = (PlugIn)getPlugInFinder().getPlugIn(inPlugInId);
		if( plugin.getInstallScript() == null)
		{
			log.info("No script configured");
		}
		else
		{
			try {
				// *** connect to configured web site
				new Downloader().download(plugin.getInstallScript(), out);
		
				Map variables = new HashMap();
				variables.put("context", inContext);
				variables.put("log", inLogger);
				Script script = getScriptModule().getScriptManager().loadScript(strOutputFile);
				getScriptModule().getScriptManager().execScript(variables, script);
			} catch (Exception ex) 
			{
				log.error(ex);
			}
		}
		getCompleted().add(inPlugInId);
		getInProgress().remove(inPlugInId);
		if( getCompleted().size() == getList().length)
		{
			inContext.removeSessionValue("upgrader");
		}
		log.info(inPlugInId + " is complete");
	}
	
	
	public ScriptModule getScriptModule()
	{
		return fieldScriptModule;
	}

	public void setScriptModule(ScriptModule inScriptModule)
	{
		fieldScriptModule = inScriptModule;
	}


	public File getRoot()
	{
		return fieldRoot;
	}

	public void setRoot(File inRoot)
	{
		fieldRoot = inRoot;
	}

	public PlugInFinder getPlugInFinder()
	{
		return fieldPlugInFinder;
	}

	public void setPlugInFinder(PlugInFinder inAllPluginS)
	{
		fieldPlugInFinder = inAllPluginS;
	}
	public Set getCompleted()
	{
		if( fieldCompleted == null)
		{
			fieldCompleted = new HashSet();
		}
		return fieldCompleted;
	}
	public Set getInProgress()
	{
		if (fieldInProgress == null)
		{
			fieldInProgress = new HashSet();
		}
		return fieldInProgress;
	}
	public void cancel()
	{
		cancel = true;
	}
	public boolean isCanceled()
	{
		return cancel;
	}
	public boolean isComplete()
	{
		return getCompleted().size() == getList().length;
	}
}
