package org.entermediadb.events;

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
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.util.TimeCalculator;
import org.openedit.CatalogEnabled;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.Shutdownable;
import org.openedit.WebPageRequest;
import org.openedit.data.SearcherManager;
import org.openedit.error.ErrorHandler;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.users.User;
import org.openedit.util.ExecutorManager;
import org.openedit.util.PathUtilities;
import org.openedit.util.RequestUtils;
import org.openedit.util.SynchronizedLinkedList;

/**
 * Loads up the events for this one application i.e. /openedit/events
 * 
 * Then runs them on a timmer if needed. Also provides a UI to listing and
 * running the tasks
 * 
 * @author cburkey
 * 
 */
public class PathEventManager implements Shutdownable, CatalogEnabled
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
		if( fieldExecutorManager == null)
		{
			fieldExecutorManager = (ExecutorManager)getModuleManager().getBean("executorManager");
		}
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
	public boolean runSharedPathEvent(String runpath)
	{
		return runSharedPathEvent(runpath,false);
	}
	
	/**
	 * This will only add the event if it is not already queued up. There is no reason to queue up multiple copies. Kind of like mouse events. 
	 * @param runpath
	 * @return
	 */
	public boolean runSharedPathEvent(String runpath, boolean inUpdateRunTimes )
	{
		if (runpath == null)
		{
			return false;
		}
		PathEvent event = getPathEvent(runpath);
		if (event != null)
		{ 
			String name = event.getName();
//			if( name.equals("Run media conversions") )
//			{
//				log.info("Running YYYY conversion task ");
//			}
			synchronized (getRunningTasks())
			{
				Date now = new Date();
				TaskRunner runner = null;
				//Date soon = new Date( System.currentTimeMillis() + 10000L);//is it already going to run within the next 10 seconds
				List<TaskRunner> copy = new ArrayList<TaskRunner>(getRunningTasks());
				for (Iterator iterator = copy.iterator(); iterator.hasNext();)
				{
					TaskRunner task = (TaskRunner) iterator.next();
					if( name.equals( task.getPathEvent().getName() ) )
					{
//						if( name.equals("Run media conversions") )
//						{
//							log.info("Found conversion task " + now + "  "  + name + " " + task.isRepeating());
//						}
						if( task.getPathEvent().isRunning() )
						{
							//task.run();
							task.setRunAgainSoon(true);
//							if(task.isRepeating())
//							{
//								task.setRunAgainSoon(true); //Will cause it to run again after it finishes
//								return true;
//							}
						}
//						else if( task.getTimeToStart().before(now) )
//						{
//							return true;
//						}
						else
						{
							//update the start time and call run now
							//task.setTimeToStart(now);
							task.run();
						}
						runner = task;
						break;
						//else this will add a duplicate. Since it is already running it will just return
					}
				}
				if( runner == null)
				{
					runner = new TaskRunner(event, this);
					//runner.setWithParameters(true); //To make sure we only run this once since the scheduled one should already be in there
					//runner.setTimeToStart(now);
					getRunningTasks().push(runner);
					runner.run();
				}
				if(runner.isRepeating() && inUpdateRunTimes)
				{
					long later  = now.getTime() + runner.getPathEvent().getPeriod();
					runner.setTimeToStart(new Date(later));
				}
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
		//synchronized( event )
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

	/*
	protected void schedule(PathEvent event, TaskRunner runner)
	{
		//getRunningTasks().push(runner);
		try
		{
			schedule(runner,event.getPeriod()); 
		} 
		catch (Exception e)
		{
			fieldTimer = null;
			getRunningTasks().clear();
			getRunningTasks().push(runner);
			schedule(runner,event.getPeriod() );
			//to fix  java.lang.IllegalStateException: Timer already cancelled.
		}
	}
	*/

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
			fieldTimer = new Timer("Scheduler_" + getCatalogId(), true);
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
			initialize();
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

	public void loadTasks() throws OpenEditException
	{
		// TODO Auto-generated method stub
//		String username = eventpage.get("eventuser");
//		if( username == null)
//		{
//			username = "admin";
//		}
//		
//		String catalogid = eventpage.getProperty("catalogid");
//		if( catalogid == null)
//		{
//			catalogid = "system";
//		}
		User user = (User)getSearcherManager().getData(getCatalogId(), "user", "admin");
//		//UserManager usermanager = (UserManager)getModuleManager().getBean(catalogid,"userManager");
		if( user == null)
		{
			throw new OpenEditException("No such user: admin");
		}
		
		for (Iterator iterator = getPathEvents().iterator(); iterator.hasNext();)
		{
			PathEvent event = (PathEvent) iterator.next();
			event.setUser(user);
			
			if (event.isEnabled())
			{
				if (event.getPeriod() > 0)
				{
					TaskRunner runner = new TaskRunner(event, this);
					//runner.setRepeating(true);
					getRunningTasks().push(runner);
				}
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
//		if( path.endsWith(".xconf"))
//		{
//			path = PathUtilities.extractPagePath(path) + ".xconf";
//		}
		if( !path.endsWith(".xconf") || path.endsWith("_site.xconf"))
		{
			return;
		}
		String htmlpage = PathUtilities.extractPagePath(path) + ".html";
		if( duplicates.contains(htmlpage))
		{
			return;
		}
		//path = PathUtilities.extractFileName(path);
		duplicates.add(htmlpage);
		//make .xconf into .html
		//ignore folders and html pages
		//get rid of duplicates from base
		
		loadPathEvent(htmlpage);
	}

	public PathEvent loadPathEvent(String htmlpage)
	{
		Page eventpage = getPageManager().getPage(htmlpage, true);
		PathEvent event = (PathEvent) getModuleManager().getBean("pathEvent");
		event.setPage(eventpage);
		//loadTask(event);
		User user = (User)getSearcherManager().getData(getCatalogId(), "user", "admin");
		event.setUser(user);

		getPathEvents().add(event);
		return event;
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
		getExecutorManager().execute(inExecrun);
		
	}

	public void reload(String inEventPath)
	{
		PathEvent event = getPathEvent(inEventPath);
		getPathEvents().remove(event);
		List<TaskRunner> copy = new ArrayList<TaskRunner>(getRunningTasks());
		for (Iterator iterator = copy.iterator(); iterator.hasNext();)
		{
			TaskRunner runner = (TaskRunner) iterator.next();
			if( event == runner.getPathEvent() )
			{
				getRunningTasks().remove(runner);
//				if(runner.isRepeating())
//				{
//					long later  = new Date().getTime() + runner.getTask().getPeriod();
//					runner.setTimeToStart(new Date(later));
//				}
			}
		}
		
		loadPathEvent(inEventPath);
		reloadScheduler();
		
		
	}

	private void reloadScheduler()
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
		for (Iterator iterator = getPathEvents().iterator(); iterator.hasNext();)
		{
			PathEvent type = (PathEvent) iterator.next();
			if( type.getPeriod() > 0)
			{
				RunSharedEventPath runthing = new RunSharedEventPath(type.getPage().getPath() );
				getTimer().schedule(runthing, type.getPeriod(), type.getPeriod());
			}
		}
	}

	public class RunSharedEventPath extends TimerTask
	{
		protected String path;
		public RunSharedEventPath(String inPath)
		{
			path = inPath;
		}
		public void run()
		{
			runSharedPathEvent(path,true);
		}
	}

	public void initialize()
	{
		loadPathEvents();
		loadTasks();
		reloadScheduler();
	}
	
}
