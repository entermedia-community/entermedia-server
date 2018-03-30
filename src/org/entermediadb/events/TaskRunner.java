package org.entermediadb.events;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;

public class TaskRunner extends java.util.TimerTask
{
	protected static final Log log = LogFactory.getLog(TaskRunner.class);
	protected PathEvent fieldPathEvent;
	protected PathEventManager fieldEventManager;
	protected WebPageRequest fieldWebPageRequest;
	protected Date fieldTimeToStart;
	protected boolean fieldWithParameters;
	protected boolean fieldRunAgainSoon;
	protected boolean fieldQueuedToRun;
	
	public boolean isQueuedToRun()
	{
		return fieldQueuedToRun;
	}
	public void setQueuedToRun(boolean inQueuedToRun)
	{
		fieldQueuedToRun = inQueuedToRun;
	}
	protected Map fieldParams;
	public Map getParams()
	{
		return fieldParams;
	}
	public void setParams(Map inParams)
	{
		fieldParams = inParams;
	}
	public Map getPageValues()
	{
		return fieldPageValues;
	}
	public void setPageValues(Map inPageValues)
	{
		fieldPageValues = inPageValues;
	}
	protected Map fieldPageValues;
	
	
	public boolean isRunAgainSoon()
	{
		return fieldRunAgainSoon;
	}
	public void setRunAgainSoon(boolean inRunAgainSoon)
	{
		fieldRunAgainSoon = inRunAgainSoon;
	}
	public boolean isWithParameters()
	{
		return fieldWithParameters;
	}
	public void setWithParameters(boolean inWithParameters)
	{
		fieldWithParameters = inWithParameters;
	}
	public TaskRunner(PathEvent inPathEvent,PathEventManager inManager)
	{
		this( inPathEvent,  null,null,inManager);
		setWithParameters(false);
	}
	public TaskRunner(PathEvent inPathEvent, Map inParams, Map inPageValues, PathEventManager inManager)
	{
		fieldPathEvent = inPathEvent;
		fieldEventManager = inManager;
		setParams(inParams);
		setPageValues(inPageValues);
		setWithParameters(true);
		setTimeToStart(new Date(System.currentTimeMillis() + inPathEvent.getPeriod() ));
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
		if (fieldWebPageRequest == null)
		{
			if( getPathEvent().getUser() == null)
			{
				throw new OpenEditException("admin User is required");
			}
			fieldWebPageRequest =  getEventManager().getRequestUtils().createPageRequest(getPathEvent().getPage().getPath(), getPathEvent().getUser());
			if( fieldParams != null)
			{
				for (Iterator iterator = fieldParams.keySet().iterator(); iterator.hasNext();)
				{
					String  key = (String ) iterator.next();
					Object value = fieldParams.get(key);
					if( value instanceof String[])
					{
						fieldWebPageRequest.setRequestParameter(key, (String[])value);
					}
					else
					{
						fieldWebPageRequest.setRequestParameter(key, (String)value);
					}
				}
			}
			if( fieldPageValues != null)
			{
				for (Iterator iterator = fieldPageValues.keySet().iterator(); iterator.hasNext();)
				{
					String  key = (String ) iterator.next();
					Object value = fieldPageValues.get(key);
					fieldWebPageRequest.putPageValue(key, value);
					fieldWebPageRequest.putSessionValue(key, value); //seems redudant
				}
			}
		}
		return fieldWebPageRequest;
	}
	protected void setWebPageRequest(WebPageRequest inWebPageRequest)
	{
		fieldWebPageRequest = inWebPageRequest;
	}
	
	public PathEvent getPathEvent()
	{
		return fieldPathEvent;
	}

	public void setPathEvent(PathEvent inPathEvent)
	{
		fieldPathEvent = inPathEvent;
	}

	public PathEventManager getEventManager()
	{
		return fieldEventManager;
	}

	public void setEventManager(PathEventManager inEventManager)
	{
		fieldEventManager = inEventManager;
	}

	/**
	 * This is non blocking
	 */
	public void run()
	{
		try
		{
			setQueuedToRun(true);
			Runnable execrun = new Runnable()
			{
				public void run()
				{
					runBlocking();
				}
			};
			getEventManager().addToRunQueue(execrun);
			
		}
		catch ( Throwable ex)
		{
			log.error("Error from action ",ex);
		}
	}
	
	public void runBlocking()
	{
		// TODO Auto-generated method stub
		//before we run this make sure our event is still enabled
		//make sure this event did not get reloaded
		PathEvent event = getEventManager().getPathEvent(getPathEvent().getPage().getPath());

		//make sure nobody is running this
		try
		{
			if( event.isEnabled() )
			{
				executeNow(getWebPageRequest(),event);
			}
		}
		finally
		{
			if( event.isEnabled() && isRunAgainSoon() )
			{
				setRunAgainSoon(false);
				setQueuedToRun(false);
				getEventManager().runSharedPathEvent(getPathEvent().getPage().getPath());
			}
			else
			{
				if( !isRepeating() )
				{
					getEventManager().getRunningTasks().remove(this);
				}
				setQueuedToRun(false);
			}
		}
		
		//Just update time and reshedule
//		if( isRepeating() )  //Duplicate ones will not have a period and expire
//		{
//			Date now = new Date(); //see if its already scheduled for the future
			
//			//make sure we just have one in the queue
//			TaskRunner runner = new TaskRunner(getTask(), getEventManager());
//			getEventManager().getRunningTasks().push(runner);
//			if( isRunAgainSoon() )
//			{
//				getEventManager().schedule(runner, 0);				
//			}
//			else
//			{
//				getEventManager().schedule(runner, getTask().getPeriod());
//			}
//		}
				
	}

	
	public boolean isRepeating()
	{
		return !isWithParameters() && getPathEvent().getPeriod() > 0 && getPathEvent().isEnabled();
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

		/*
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
		*/		
	}
}