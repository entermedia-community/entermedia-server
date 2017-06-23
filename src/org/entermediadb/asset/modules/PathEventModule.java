package org.entermediadb.asset.modules;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.events.PathEvent;
import org.entermediadb.events.PathEventManager;
import org.entermediadb.scripts.ScriptManager;
import org.openedit.WebPageRequest;
import org.openedit.config.XMLConfiguration;
import org.openedit.modules.BaseModule;
import org.openedit.page.Page;
import org.openedit.page.PageAction;
import org.openedit.page.PageProperty;
import org.openedit.page.PageSettings;

public class PathEventModule extends BaseModule
{
	private static final Log log = LogFactory.getLog(PathEventModule.class);

	public boolean runEvent(WebPageRequest inReq)
	{
		String runpath = inReq.findValue("runpath");
		//Page page  = getPageManager().getPage(runpath,true);
		
		//WebPageRequest child = inReq.copy(page);
		
		//TODO: First check with the 	PathEventManager and run that one instead
		PathEventManager manager = getPathEventManager(inReq);
		return manager.runPathEvent(runpath, inReq);
	}
	public void runSharedEvent(WebPageRequest inReq)
	{
		String runpath = inReq.findValue("runpath");
		//Page page  = getPageManager().getPage(runpath,true);
		
		//WebPageRequest child = inReq.copy(page);
		
		//TODO: First check with the 	PathEventManager and run that one instead
		PathEventManager manager = getPathEventManager(inReq);
	    manager.runSharedPathEvent(runpath);
	    PathEvent event = manager.getPathEvent(runpath);
	    inReq.putPageValue("ranevent", event);
	    inReq.putPageValue("pathevent", event); 
	}

	public PathEventManager getPathEventManager(WebPageRequest inReq)
	{
		String catalogid = inReq.getRequestParameter("targetcatalogid");
		if(catalogid == null)
		{
			catalogid = inReq.getContentProperty("catalogid");
		}
		if(catalogid == null)
		{
			catalogid = inReq.getContentProperty("applicationid");
		}
		PathEventManager manager = (PathEventManager)getModuleManager().getBean(catalogid, "pathEventManager");
		inReq.putPageValue("patheventmanager", manager);

		return manager;
	}
	
	/**
	 * This is run from path-events. Should include schedled and unscheduled events
	 * @param inReq
	 * @return
	 */
	public void enableLog(WebPageRequest inReq)
	{
		PathEventManager manager = getPathEventManager(inReq);
		if( manager != null)
		{
			String enabled = inReq.findValue("enableeventlogs");
			manager.setLogEvents(Boolean.parseBoolean(enabled));
		}
	}	
	
	public void getPathEvents(WebPageRequest inReq)
	{
		PathEventManager manager = getPathEventManager(inReq);
		List events = manager.getPathEvents();
		Collections.sort(events);
		inReq.putPageValue("pathevents", events);
	}
	
	public PathEvent loadPathEvent(WebPageRequest inReq)
	{
		PathEventManager manager = getPathEventManager(inReq);
		String eventPath = inReq.getRequestParameter("eventpath");
		PathEvent pathevent = manager.getPathEvent(eventPath);
		inReq.putPageValue("pathevent", pathevent);
		return pathevent;
	}
	public void removePathEvent(WebPageRequest inReq)
	{
		PathEventManager manager = getPathEventManager(inReq);
		String eventPath = inReq.getRequestParameter("eventpath");
		PageSettings settings = getPageManager().getPageSettingsManager().getPageSettings(eventPath);
		Page todelete = getPageManager().getPage(settings.getPath());
		getPageManager().removePage(todelete);
		manager = getPathEventManager(inReq); 
		manager.reload(eventPath);
	}	
	public void savePathEvent(WebPageRequest inReq)
	{
		PathEventManager manager = getPathEventManager(inReq);
		String eventPath = inReq.getRequestParameter("eventpath");
		PathEvent event = manager.getPathEvent(eventPath);
		if( event == null)
		{
			event = manager.loadPathEvent(eventPath);
		}
		String period = inReq.getRequestParameter("period");
		event.setPeriod(period);
		event.setDelay(inReq.getRequestParameter("delay"));
		String enabled = inReq.getRequestParameter("enabled");
		event.setEnabled("true".equals(enabled));
		
//		Page eventpage = event.getPage();
//		eventpage.setProperty("period", event.getFormattedPeriod());
//		eventpage.setProperty("delay", event.getFormattedDelay());
//		eventpage.setProperty("enabled", String.valueOf(event.isEnabled()));
//		getPageManager().saveSettings(eventpage);
		
		PageSettings settings = getPageManager().getPageSettingsManager().getPageSettings(eventPath);
		PageProperty prop = new PageProperty("period");
		prop.setValue(event.getFormattedPeriod());
		settings.putProperty(prop);
		prop = new PageProperty("delay");
		prop.setValue(event.getFormattedDelay());
		settings.putProperty(prop);
		prop = new PageProperty("enabled");
		prop.setValue(String.valueOf(event.isEnabled()));
		settings.putProperty(prop);
		
		prop = new PageProperty("eventname");
		prop.setValue(inReq.getRequestParameter("eventname"));
		settings.putProperty(prop);
		
		getPageManager().getPageSettingsManager().saveSetting(settings);
		manager = getPathEventManager(inReq); 
		manager.reload(eventPath);
		inReq.putPageValue("pathevent", event);
	}
	
	public void restartEvents(WebPageRequest inReq)
	{
		PathEventManager manager = getPathEventManager(inReq); 
		manager.shutdown();
	}
	public void clearPathEventLog(WebPageRequest inReq)
	{
		PathEventManager manager = getPathEventManager(inReq);
		String eventPath = inReq.getRequestParameter("eventpath");
		PathEvent event = manager.getPathEvent(eventPath);
		event.clearLog();
	}
	public void loadScriptForEvent(WebPageRequest inReq)
	{
		PathEvent event = loadPathEvent(inReq);
		String pathtoscript = findScriptName(event);
		Page script = getPageManager().getPage(pathtoscript, true);
		if( !script.exists() )
		{
			String catalogid = event.getPage().get("catalogid");
			script = getPageManager().getPage("/" + catalogid + "/events/scripts/template.groovy");
		}
		inReq.putPageValue("script", script);
	}
	public void deleteScriptForEvent(WebPageRequest inReq)
	{
		PathEvent event = loadPathEvent(inReq);
		String pathtoscript = findScriptName(event);
		Page script = getPageManager().getPage(pathtoscript);
		if( script.exists() && !pathtoscript.startsWith("/WEB-INF/base") )
		{
			getPageManager().removePage(script);
			getPageManager().clearCache(event.getPage());
			getScriptManager().clearCache();

		}
		loadScriptForEvent(inReq);
	}
	private String findScriptName(PathEvent event) {
		String eventname = event.getPage().getPageName();
		eventname = eventname + ".groovy";
		
		String folder = event.getPage().getDirectoryName();
		String catalogid = event.getPage().get("catalogid");
		String pathtoscript = "/" + catalogid + "/events/scripts/" + folder +"/"+ eventname ;

		return pathtoscript;
	}
	public void saveScriptForEvent(WebPageRequest inReq)
	{
		PathEvent event = loadPathEvent(inReq);
		String pathtoscript = findScriptName(event);
		Page script = getPageManager().getPage(pathtoscript);
		String  code = inReq.getRequestParameter("scriptcode");
		getPageManager().saveContent(script, null,code, null);
		inReq.putPageValue("script", script);
		
		PageSettings settings = event.getPage().getPageSettings();
		String catalogid = event.getPage().get("catalogid");
		pathtoscript = pathtoscript.replace(catalogid, "${catalogid}");
		if( !containsScript(settings,pathtoscript)) //Fix 
		{
			XMLConfiguration config = new XMLConfiguration("path-action");
			config.setAttribute("name", "Script.run");
			config.setAttribute("allowduplicates", "true");
			XMLConfiguration child = new XMLConfiguration("script");
			child.setValue(pathtoscript);
			config.addChild(child);
			PageAction action = new PageAction("Script.run");
			
			action.setConfig(config);
			//settings.
			settings.addPathAction(action);
			getPageManager().saveSettings(event.getPage());
			getScriptManager().clearCache();
		}
		getPageManager().clearCache(event.getPage());
		//getPageManager().clearCache();
		
		/**
		 * 	<path-action name="Script.run"  allowduplicates="true">
		<script>/${catalogid}/events/scripts/publishing/publishassets.groovy</script>
	</path-action>

		 */
	}
	
	public ScriptManager getScriptManager()
	{
		ScriptManager fieldScriptManager = (ScriptManager)getModuleManager().getBean("scriptManager");
		return fieldScriptManager;
	}
	
	protected boolean containsScript(PageSettings settings, String pathtoscript) 
	{
		for (Iterator iterator = settings.getPathActions().iterator(); iterator.hasNext();) 
		{
			PageAction action = (PageAction) iterator.next();
			if( action.getActionName().equals("Script.run") )
			{
				String name = action.getConfig().getChildValue("script");
				if( name != null && name.equals(pathtoscript))
				{
					return true;
				}
			}
		}
		
		return false;
	}

	
}
