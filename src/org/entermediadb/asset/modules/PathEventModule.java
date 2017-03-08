package org.entermediadb.asset.modules;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.events.PathEvent;
import org.entermediadb.events.PathEventManager;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.hittracker.HitTracker;
import org.openedit.modules.BaseModule;
import org.openedit.page.Page;
import org.openedit.page.PageProperty;
import org.openedit.page.PageSettings;

public class PathEventModule extends BaseModule
{
	private static final Log log = LogFactory.getLog(PathEventModule.class);

	public boolean runEvent(WebPageRequest inReq)
	{
		String runpath = inReq.findValue("runpath");
		Page page  = getPageManager().getPage(runpath,true);
		
		//WebPageRequest child = inReq.copy(page);
		
		//TODO: First check with the 	PathEventManager and run that one instead
		PathEventManager manager = getPathEventManager(inReq);
		return manager.runPathEvent(runpath, inReq);
	}
	public void runSharedEvent(WebPageRequest inReq)
	{
		String runpath = inReq.findValue("runpath");
		Page page  = getPageManager().getPage(runpath,true);
		
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
	
	public void loadPathEvent(WebPageRequest inReq)
	{
		PathEventManager manager = getPathEventManager(inReq);
		String eventPath = inReq.getRequestParameter("eventpath");
		inReq.putPageValue("pathevent", manager.getPathEvent(eventPath));
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
}
