package org.entermediadb.modules.update;

import java.io.File;
import java.util.Collection;
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
import org.openedit.Shutdownable;
import org.openedit.WebPageRequest;
import org.openedit.node.NodeManager;

public class PluginUpgrader
{
	private static final Log log = LogFactory.getLog(PluginUpgrader.class);
	
	protected ScriptModule fieldScriptModule;
	protected File fieldRoot;
	protected PlugInFinder fieldPlugInFinder;
	protected Collection fieldToUpgrade;
	protected Set fieldCompleted;
	protected Set fieldInProgress;
	protected boolean cancel = false;
	
	
	public Collection getList()
	{
		return fieldToUpgrade;
	}
	public void setToUpgrade(Collection inList)
	{
		fieldToUpgrade = inList;
	}
	public List upgrade( PlugIn inPlugIn, WebPageRequest inContext )
	{
		ScriptLogger logger = new ScriptLogger();
		logger.startCapture();
		try
		{
			doUpgrade(inPlugIn, inContext, logger);
		}
		finally
		{
			logger.stopCapture();
		}
		return logger.listLogs();
	}
	
	protected void doUpgrade( PlugIn plugin, WebPageRequest inContext,ScriptLogger inLogger )
	{
		if( getInProgress().contains(plugin.getId()))
		{
			log.info(plugin + " is in progress");
			return;
		}
		if( cancel)
		{
			log.info(plugin.getId() + " is canceled");
			return;
		}
		getInProgress().add(plugin.getId());
		
		String strOutputFile = "/WEB-INF/install.js";
		File out = new File(getRoot(), strOutputFile);
		if( plugin.getInstallScript() == null)
		{
			log.info("No script configured");
		}
		else
		{
			try {
				// *** connect to configured web site
				log.info("Downloading " + plugin.getInstallScript());
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
		getCompleted().add(plugin.getId());
		getInProgress().remove(plugin.getId());
		if( getCompleted().size() == getList().size())
		{
			inContext.removeSessionValue("upgrader");
		}
		log.info(plugin.getId() + " is complete");
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
		return getCompleted().size() == getList().size();
	}
	public void shutdown()
	{
		Shutdownable manager = (Shutdownable)getScriptModule().getModuleManager().getBean("elasticNodeManager");
		manager.shutdown();
		
		//Touch web.xml
		File web = new File( getRoot(), "WEB-INF/web.xml");
		web.setLastModified(System.currentTimeMillis());
				

	}
}
