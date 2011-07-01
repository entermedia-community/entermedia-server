package org.openedit.events;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.data.Searcher;
import org.openedit.event.WebEvent;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.util.PathUtilities;

public class TaskRunner extends java.util.TimerTask
{
	protected static final Log log = LogFactory.getLog(TaskRunner.class);
	protected PathEvent fieldTask;
	protected PathEventManager fieldEventManager;
	protected int fieldExpectedRunCountId;
	protected WebPageRequest fieldWebPageRequest;
	protected Date fieldTimeToStart;

	public TaskRunner(PathEvent inTask,long inDelay, PathEventManager inManager)
	{
		this( inTask, inDelay, null,null,inManager);
	}
	public TaskRunner(PathEvent inTask,long inDelay, Map inParams, Map inPageValues, PathEventManager inManager)
	{
		fieldTask = inTask;
		fieldEventManager = inManager;
		fieldExpectedRunCountId = inTask.getRunCountId();
		
		WebPageRequest request =  inManager.getRequestUtils().createPageRequest(inTask.getPage().getPath(), inTask.getUser());
		if( inParams != null)
		{
			for (Iterator iterator = inParams.keySet().iterator(); iterator.hasNext();)
			{
				String  key = (String ) iterator.next();
				Object value = inParams.get(key);
				if( value instanceof String[])
				{
					request.setRequestParameter(key, (String[])value);
				}
				else
				{
					request.setRequestParameter(key, (String)value);
				}
			}
		}
		if( inPageValues != null)
		{
			for (Iterator iterator = inPageValues.keySet().iterator(); iterator.hasNext();)
			{
				String  key = (String ) iterator.next();
				Object value = inPageValues.get(key);
				request.putPageValue(key, value);
				request.putSessionValue(key, value);
			}
		}
		fieldWebPageRequest = request; 
		setTimeToStart(new Date(System.currentTimeMillis() + inDelay));
	}

	public Date getTimeToStart()
	{
		return fieldTimeToStart;
	}
	
	public void setTimeToStart(Date inTimeToStart)
	{
		fieldTimeToStart = inTimeToStart;
	}
	protected WebPageRequest getWebPageRequest()
	{
		return fieldWebPageRequest;
	}
	public int getExpectedRunCountId()
	{
		return fieldExpectedRunCountId;
	}
	protected void setWebPageRequest(WebPageRequest inWebPageRequest)
	{
		fieldWebPageRequest = inWebPageRequest;
	}

	
	public PathEvent getTask()
	{
		return fieldTask;
	}

	public void setTask(PathEvent inTask)
	{
		fieldTask = inTask;
	}

	public PathEventManager getEventManager()
	{
		return fieldEventManager;
	}

	public void setEventManager(PathEventManager inEventManager)
	{
		fieldEventManager = inEventManager;
	}

	public boolean isCurrent()
	{
		PathEvent event = getEventManager().getPathEvent(getTask().getPage().getPath());
		return event.getRunCountId() == getExpectedRunCountId();
	}
	public void run()
	{
		try
		{
			//before we run this make sure our event is still enabled
			//make sure this event did not get reloaded
			PathEvent event = getEventManager().getPathEvent(getTask().getPage().getPath());
	
			//make sure nobody is running this
			boolean oktorun = !event.isRunning() && isCurrent();
			if(  event.isMultipleCopies() || oktorun )
			{
				try
				{
					if( event.isEnabled() )
					{
						executeNow(getWebPageRequest(),event);
					}
				}
				finally
				{
					getEventManager().getRunningTasks().remove(this);
				}
				if(event.getPeriod() > 0)
				{
					TaskRunner runner = new TaskRunner(getTask(), getTask().getPeriod(),new HashMap() ,new HashMap(), getEventManager());
					getEventManager().getRunningTasks().push(runner);
					getEventManager().getTimer().schedule(runner, getTask().getPeriod());
				}
			}
			else
			{
				getEventManager().getRunningTasks().remove(this);
			}
		}
		catch ( Throwable ex)
		{
			log.error("Error from action ",ex);
		}
	}
	protected void executeNow(WebPageRequest inReq, PathEvent event) 
	{
		if( inReq == null)
		{
			throw new OpenEditException("Request must not be null");
		}
		
		inReq.putPageValue("ranevent", event);
		inReq.setRequestParameter("runpath", event.getPage().getPath());
		
		event.execute(inReq);
		
		if( getEventManager().isLogEvents())
		{
			//TODO: Capture error logs
			String type = PathUtilities.extractPageName(event.getPage().getPath());
	
			StringBuffer stdout = new StringBuffer();
			WebEvent runevent = new WebEvent();
			runevent.setUser(inReq.getUser());
			runevent.setOperation(type);
			runevent.setProperty("time", "" + event.getLastRun().getTime() /1000L);
			WebEvent outsideevent = (WebEvent)inReq.getPageValue("webevent");
			if( outsideevent != null)
			{
				stdout.append(outsideevent.getOperation() + " event fired. ");
				stdout.append(outsideevent.getProperties().toString());
				stdout.append("<br>\n");
			}
			String last = event.getLastOutput();
			if( last != null && last.length() > 0)
			{
				int max = Math.min(1000, last.length());
				//cut the end
				stdout.append(last.substring(last.length() - max,last.length()));
			}
			runevent.setProperty("details", stdout.toString());
			Searcher patheventseacher = getEventManager().getSearcherManager().getSearcher(getEventManager().getCatalogId(), "patheventLog");
			patheventseacher.saveData(runevent,inReq.getUser());
		}		
	}
}