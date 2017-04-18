package org.entermediadb.events;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.ModuleManager;
import org.openedit.WebPageRequest;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventListener;

/**
 * Listens for web events such as upload and runs the related path event
 * @author cburkey
 */

public class PathEventHandler implements WebEventListener
{
	protected ModuleManager fieldModuleManager;
	private static final Log log = LogFactory.getLog(PathEventHandler.class);
	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public void eventFired(WebEvent inEvent)
	{
		
		String runpath = "/" + inEvent.getCatalogId() + "/events/" + inEvent.getOperation() + ".html";
		PathEventManager manager = (PathEventManager)getModuleManager().getBean(inEvent.getCatalogId(),"pathEventManager");
		//log.info("path event running" + runpath);
		//log.info("web event called : " + inRunpath);
		WebPageRequest request = manager.getRequestUtils().createPageRequest(runpath, inEvent.getUser());
		for (Iterator iterator = inEvent.getProperties().keySet().iterator(); iterator.hasNext();)
		{
			String key = (String) iterator.next();
			Object value = inEvent.getValue(key);
			if( value instanceof String)
			{
				request.setRequestParameter(key, (String)value);
			}
			request.putPageValue(key, value);
		}
		request.setRequestParameter("catalogid", inEvent.getCatalogId());
		request.putPageValue("webevent", inEvent);
		manager.runPathEvent(runpath, request);

	}

}
