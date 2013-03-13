package org.openedit.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Timer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.util.TimeCalculator;
import org.openedit.util.SynchronizedLinkedList;

import com.openedit.ModuleManager;
import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.error.ErrorHandler;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.users.UserManager;
import com.openedit.util.ExecutorManager;
import com.openedit.util.PathUtilities;
import com.openedit.util.RequestUtils;

/**
 * Loads up the events for this one application i.e. /openedit/events
 * 
 * Then runs them on a timmer if needed. Also provides a UI to listing and
 * running the tasks
 * 
 * @author cburkey
 * 
 */
public class PathEventManager
{
	protected static final Log log = LogFactory.getLog(PathEventManager.class);

	protected boolean fieldLogEvents;
	protected String fieldCatalogId;
	protected PageManager fieldPageManager;
	protected ModuleManager fieldModuleManager;
	protected SearcherManager fieldSearcherManager;
	protected RequestUtils fieldRequestUtils;
	protected List fieldPathActions;
	protected LinkedList<TaskRunner> fieldRunningTasks;
	protected ErrorHandler fieldErrorHandler;
	protected Timer fieldTimer; // Should this be shared across the system?
	protected TimeCalculator fieldTimeCalculator;
	protected ExecutorManager fieldExecutorManager;
	
	
	public ExecutorManager getExecutorManager()
	{
		return fieldExecutorManager;
	}

	public void setExecutorManager(ExecutorManager inExecutorManager)
	{
		fieldExecutorManager = inExecutorManager;
	}

	public TimeCalculator getTimeCalculator()
	{
		if (fieldTimeCalculator == null)
		{
			fieldTimeCalculator = new TimeCalculator();
		}
		return fieldTimeCalculator;
	}

	public void setTimeCalculator(TimeCalculator inTimeCalculator)
	{
		fieldTimeCalculator = inTimeCalculator;
	}

	public PathEventManager()
	{
	}

	public LinkedList<TaskRunner> getRunningTasks()
	{
		if (fieldRunningTasks == null)
		{
			fieldRunningTasks = new SynchronizedLinkedList<TaskRunner>();
		}
		return fieldRunningTasks;
	}
	public List<TaskRunner> getRunningTasksSorted()
	{
		List<TaskRunner> copy = new ArrayList<TaskRunner>(getRunningTasks());
		Collections.sort(copy, new Comparator<TaskRunner>()
		{
			public int compare(TaskRunner inT1, TaskRunner inT2)
			{
		       return inT1.getTimeToStart().compareTo(inT2.getTimeToStart());
			}
		});
		
		return copy;
	}
	public boolean isLogEvents() {
		return fieldLogEvents;
	}

	public void setLogEvents(boolean fieldLogEvent) {
		this.fieldLogEvents = fieldLogEvent;
	}


	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}
	/**
	 * This will only add the event if it is not already queued up. There is no reason to queue up multiple copies. Kind of like mouse events. 
	 * @param runpath
	 * @return
	 */
	public boolean runSharedPathEvent(String runpath )
	{
		if (runpath == null)
		{
			return false;
		}
		PathEvent event = getPathEvent(runpath);
		if (event != null)
		{ 
			TaskRunner runner = null;
			synchronized (getRunningTasks())
			{
				String name = event.getName();
				Date now = new Date();
				//Date soon = new Date( System.currentTimeMillis() + 10000L);//is it already going to run within the next 10 seconds
				List<TaskRunner> copy = new ArrayList<TaskRunner>(getRunningTasks());
				int count = 0;
				for (Iterator iterator = copy.iterator(); iterator.hasNext();)
				{
					TaskRunner task = (TaskRunner) iterator.next();
					if( name.equals( task.getTask().getName() ) )
					{
						if( task.getTask().isRunning() )
						{
							task.setRunAgainSoon(true); //Will cause it to run again after it finishes
						}
						else
						{
							runner.setTimeToStart(new Date());
							getTimer().schedule(runner,0);
						}
						return true;   							
					}
				}
				runner = new TaskRunner(event, this);
				runner.setWithParameters(true); //To make sure we only run this once since the scheduled one should already be in there
				runner.setTimeToStart(new Date());
				getRunningTasks().push(runner);
			}
			if( runner != null )
			{
				getTimer().schedule(runner,0);
			}
			return true;
		}
		else
		{
			//I guess sometimes events fire that are not actually configured
			if( !runpath.endsWith(".html"))
			{
				throw new OpenEditException("Event path must end with .html " + runpath);
				
			}
			
			if( log.isDebugEnabled() )
			{
				log.debug("No actions enabled for this event: " + runpath);
			}
			return false;
		}

		
	}
	
	/**
	 * This is the public API that event listeners need to run to exec
	 * /catalogid/events/* actions
	 * 
	 * @param runpath
	 * @param inReq
	 * @return
	 */
	public boolean runPathEvent(String runpath, WebPageRequest inReq )
	{
		if (runpath == null)
		{
			return false;
		}
		PathEvent event = getPathEvent(runpath);
		inReq.putPageValue("ranevent", event);
//		String force = inReq.getRequestParameter("forcerun");
				
		if (event != null)
		{ 
//			if( Boolean.parseBoolean(force) || event.getDelay() == 0 )
			TaskRunner runner = new TaskRunner(event, inReq.getParameterMap(), getRequestUtils().extractValueMap(inReq), this);
//			{
				getRunningTasks().push(runner);
				runner.runBlocking(); //this will remove it again
//			}
//			else
//			{
//				schedule(event, runner);
//			}
			return true;
		}
		else
		{
			//I guess sometimes events fire that are not actually configured
			if( !runpath.endsWith(".html"))
			{
				throw new OpenEditException("Event path must end with .html " + runpath);
				
			}
			
			//do nothingthrow new OpenEditException("Event not found " + runpath);
			if( log.isDebugEnabled() )
			{
				log.debug("No actions enabled for this event: " + runpath);
			}
			return false;
		}
	}

	protected void schedule(PathEvent event, TaskRunner runner)
	{
		getRunningTasks().push(runner);
		try
		{
			getTimer().schedule(runner,event.getPeriod()); 
		} 
		catch (Exception e)
		{
			fieldTimer = null;
			getRunningTasks().clear();
			getRunningTasks().push(runner);
			getTimer().schedule(runner,event.getPeriod() );
			//to fix  java.lang.IllegalStateException: Timer already cancelled.
		}
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public RequestUtils getRequestUtils()
	{
		return fieldRequestUtils;
	}

	public void setRequestUtils(RequestUtils inRequestUtils)
	{
		fieldRequestUtils = inRequestUtils;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public Timer getTimer()
	{
		if (fieldTimer == null)
		{
			fieldTimer = new Timer(true);
		}
		return fieldTimer;
	}

	public void removeTask(PathEvent inTask)
	{
		getPathEvents().remove(inTask);
	}

	public List<PathEvent> getPathEvents()
	{
		if (fieldPathActions == null)
		{
			fieldPathActions = new ArrayList();
			loadPathEvents();
		}
		return fieldPathActions;
	}

	public void setPathEvents(List inTaskList)
	{
		fieldPathActions = inTaskList;
	}



	public ErrorHandler getErrorHandler()
	{
		return fieldErrorHandler;
	}

	public void setErrorHandler(ErrorHandler inErrorHandler)
	{
		fieldErrorHandler = inErrorHandler;
	}

	public void shutdown()
	{
		if (fieldTimer != null)
		{
			try
			{
				getTimer().cancel();
			}
			catch ( Throwable ex)
			{
				log.error("ignoring error",ex);
			}
			fieldTimer = null;
		}
		clear();
		fieldPathActions  = null;
		fieldRunningTasks = null;
	}

	public void removeTask(ScheduledTask inTask)
	{
		inTask.setEnabled(false);
		getPathEvents().remove(inTask);
	}

	public void loadTask(PathEvent inTask) throws OpenEditException
	{
		//, boolean inRun, boolean inAsync f t
		if( log.isDebugEnabled() )
		{
			log.debug("Adding new Workflow Task: " + inTask.getPage());
		}
		getPathEvents().add(inTask);
		if (inTask.isEnabled())
		{
			if (inTask.getPeriod() > 0)
			{
				TaskRunner runner = new TaskRunner(inTask, this);
				//runner.setRepeating(true);
				getRunningTasks().push(runner);
				getTimer().schedule(runner, inTask.getPeriod());
			}
		}
	}

	public void clear()
	{
		if( fieldPathActions != null)
		{
			getPathEvents().clear();
		}
	}

	protected void loadPathEvents()
	{
		clear();
		//getPageManager().clearCache();
		String root = "/" + getCatalogId() + "/events";
		Set duplicates = new HashSet();
		loadPathEvents(root, duplicates);
		Collections.sort(getPathEvents());
	}

	protected void loadPathEvents(String inRoot, Set inDuplicates)
	{
		List events = getPageManager().getChildrenPaths(inRoot + "/", true);
		for (Iterator iterator = events.iterator(); iterator.hasNext();)
		{
			String path = (String) iterator.next();
			Page page = getPageManager().getPage(path); 
			String vpath = inRoot + "/" + PathUtilities.extractFileName(path);
			Page vchild = getPageManager().getPage(vpath); 
			
			if( page.isFolder() && !page.getName().startsWith(".versions") )
			{
				loadPathEvents(vchild.getPath(), inDuplicates);
			}
			else
			{
				loadByPath(vpath, inRoot, inDuplicates);
			}
		}
		
	}

	protected void loadByPath(String path,String root, Set duplicates)
	{
		path = PathUtilities.extractFileName(path);
		if( path.endsWith("html"))
		{
			path = PathUtilities.extractPagePath(path) + ".xconf";
		}
		if( duplicates.contains(path))
		{
			return;
		}
		if( !path.endsWith(".xconf"))
		{
			return;
		}
		duplicates.add(path);
		//make .xconf into .html
		//ignore folders and html pages
		//get rid of duplicates from base
		String htmlpage = root + "/" + PathUtilities.extractPagePath(path) + ".html";
		
		loadPathEvent(htmlpage);
	}

	protected void loadPathEvent(String htmlpage)
	{
		Page eventpage = getPageManager().getPage(htmlpage);
		PathEvent event = (PathEvent) getModuleManager().getBean("pathEvent");
		event.setPage(eventpage);
	
		String username = eventpage.get("eventuser");
		if( username == null)
		{
			username = "admin";
		}
		UserManager usermanager = (UserManager)getModuleManager().getBean("userManager");
		User user = usermanager.getUser(username);
		event.setUser(user);
		loadTask(event);
	}
	
	public PathEvent getPathEvent(String inPath)
	{
		for (Iterator iterator = getPathEvents().iterator(); iterator.hasNext();)
		{
			PathEvent event = (PathEvent) iterator.next();
			String path =event.getPage().getPath(); 
			if (path.equals(inPath))
			{
				return event;
			}
		}
		return null;
	}

	public void addToRunQueue(Runnable inExecrun)
	{
		getExecutorManager().getSharedExecutor().execute(inExecrun);
		
	}

	public void reload(String inEventPath)
	{
		PathEvent event = getPathEvent(inEventPath);
		getPathEvents().remove(event);
		loadPathEvent(inEventPath);
		
	}

}
