package org.entermediadb.events;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.util.TimeParser;
import org.entermediadb.scripts.ScriptLogger;
import org.entermediadb.scripts.TextAppender;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.WebServer;
import org.openedit.event.WebEvent;
import org.openedit.generators.Output;
import org.openedit.page.Page;
import org.openedit.users.User;
import org.openedit.util.RequestUtils;

import com.sun.xml.internal.bind.v2.model.annotation.FieldLocatable;

public class PathEvent implements Comparable, TextAppender
{
	private static final Log log = LogFactory.getLog(PathEvent.class);
	protected User fieldUser;
	protected Page fieldPage;
	protected boolean fieldAsleep;
	protected boolean fieldHasFailed;
	protected long fieldExpirationTime;
	protected Date fieldLastRun;
	protected StringBuffer fieldLastOutput;
	protected WebServer fieldWebServer;
	protected String fieldFormattedPeriod;
	protected long fieldDelay = 0;
	protected long fieldPeriod = 0;
	protected String fieldFormattedDelay;
	protected boolean fieldEnabled = true;
	protected int fieldRunningCount = 0;
	protected RequestUtils fieldRequestUtils;
	
	public PathEvent()
	{
	}
	
	public boolean isRunning() 
	{
		return fieldRunningCount > 0;
	}

	public int getRunningCount()
	{
		return fieldRunningCount;
	}
	
	public long getPeriod()
	{
		return fieldPeriod;
	}

	public void setPeriod(long inPeriod)
	{
		fieldPeriod = inPeriod;
	}

	public void setPeriod(String inPeriod)
	{
		fieldPeriod = parse(inPeriod);
		fieldFormattedPeriod = inPeriod;
	}

	public String getFormattedPeriod()
	{
		return fieldFormattedPeriod;
	}

	private void setDefaults()
	{
		setPeriod(12 * 60 * 60 * 1000); //defaults to runing every 12 hours
		setDelay(60000L); //defaults to one minute
	}

	public long getDelay()
	{
		return fieldDelay;
	}

	public String getFormattedDelay()
	{
		return fieldFormattedDelay;
	}

	public void setDelay(long inDelay)
	{
		fieldDelay = inDelay;
	}

	public void setDelay(String inDelay)
	{
		fieldDelay = parse(inDelay);
		fieldFormattedDelay = inDelay;
	}

	/**
	 * @param inPeriodString
	 * @return
	 */
	private long parse(String inPeriodString)
	{
		if (inPeriodString == null)
		{
			return 0;
		}
		return new TimeParser().parse(inPeriodString);
	}

	public void putProperty(String key, String value)
	{
		if (key.equals("username"))
		{
			//This is3 handled in XMLSchedulerArchive.loadFromFile()
		}
		else if (key.equals("startdelay"))
		{
			setDelay(value);
		}
		else if (key.equals("period"))
		{
			setPeriod(value);
		}
		else if (key.equals("enabled"))
		{
			setEnabled(Boolean.parseBoolean(value));
		}
		getPage().setProperty(key, value);
	}

	public boolean isEnabled()
	{
		return fieldEnabled;
	}

	public void setEnabled(boolean inEnabled)
	{
		fieldEnabled = inEnabled;
	}

	public long getExpirationTime()
	{
		return fieldExpirationTime;
	}

	public void setExpirationTime(long inExpirationTime)
	{
		fieldExpirationTime = inExpirationTime;
	}

	public User getUser()
	{
		return fieldUser;
	}

	public void setUser(User inUser)
	{
		fieldUser = inUser;
	}

	public boolean hasFailed()
	{
		return fieldHasFailed;
	}

	public String getProperty(String key)
	{
		return (String) getProperties().get(key);
	}

	public void setProperty(String key, String value)
	{
		getProperties().put(key, value);
	}

	public boolean isSleeping()
	{
		return fieldAsleep;
	}

	public void sleep()
	{
		fieldAsleep = true;
	}

	public void wakeup()
	{
		fieldAsleep = false;
	}

	public String getFormattedLastRun()
	{
		if (getLastRun() != null)
		{
			return DateFormat.getDateTimeInstance().format(getLastRun());
		}
		return null;
	}

	public Date getLastRun()
	{
		return fieldLastRun;
	}

	public void setLastRun(Date inLastRun)
	{
		fieldLastRun = inLastRun;
		getLastOutput().append("event started: ");
		getLastOutput().append(DateFormat.getDateTimeInstance().format(fieldLastRun));
		getLastOutput().append('\n');

	}

	public StringBuffer getLastOutput()
	{
		if( fieldLastOutput == null )
		{
			fieldLastOutput = new StringBuffer();
		}
		return fieldLastOutput;
	}
	public String getLastOutputHtml()
	{
		if( fieldLastOutput == null)
		{
			return null;
		}
		StringBuffer out = new StringBuffer();
		out.append("<div class='emoutput'>");
		out.append("<div>");
		out.append(	getLastOutput().toString().replace("\n", "</div><div>" ));
		out.append("</div></div>");
		return out.toString();
	}

	public void appendText(String inLastOutput)
	{
//		getLastOutput().append("ran on: ");
//		getLastOutput().append(DateFormat.getDateTimeInstance().format(getLastRun()));
//		getLastOutput().append('\n');
		getLastOutput().append(inLastOutput);
		getLastOutput().append('\n');
		if( getLastOutput().length() > 10000 )
		{
			String cut = getLastOutput().substring(getLastOutput().length() - 7000, getLastOutput().length());
			fieldLastOutput = new StringBuffer(cut);
		}
	}

	public RequestUtils getRequestUtils()
	{
		return fieldRequestUtils;
	}

	public void setRequestUtils(RequestUtils inRequestUtils)
	{
		fieldRequestUtils = inRequestUtils;
	}
	
	/**
	 * This should only be called from the TaskRunner.run() method
	 * @param inReq
	 * @return
	 * @throws OpenEditException
	 */
	public boolean execute(WebPageRequest inReq) throws OpenEditException
	{
		//The task should make sure this does not run if it is a repeating type
//		if( isRunning() )
//		{
//			log.info("Event is already running " + getPage().getName() );
//			return false; //already running
//		}
		//Track if two parameterized events execute at the same moment. 
		fieldRunningCount++;
		try
		{
			return runNow(inReq);
		}
		finally
		{
			fieldRunningCount--;
		}
	}

	protected boolean runNow(WebPageRequest inReq) 
	{		
		WebPageRequest	request = inReq.copy(getPage());
		request.putProtectedPageValue("content", getPage());
		//		for (Iterator iterator = getProperties().keySet().iterator(); iterator.hasNext();) {
		//			String key = (String) iterator.next();
		//			request.setRequestParameter(key, getProperty(key));			
		//		}
		//request.putPageValue("home", "");

		//long start = System.currentTimeMillis();


		//Thread thread = Thread.currentThread();
		//ClassLoader oldLoader = thread.getContextClassLoader();
		ScriptLogger logs = null;
		StringWriter output = new StringWriter();
		try
		{
			logs = new ScriptLogger();
			//thread.setContextClassLoader(getClassLoader());
			Page page = request.getPage();
			logs.setPrefix(page.getName());
			logs.setTextAppender(this);
			
			request.putPageValue("log", logs);
			logs.startCapture();
			
			try
			{
				if( log.isDebugEnabled() )
				{
					log.info("running " + page.getPath());
				}

				//WebEvent event = (WebEvent)request.getPageValue("webevent");
				request.setWriter(output);
				getWebServer().getOpenEditEngine().createPageStreamer(page, request);
				setLastRun(new Date());

				if (getPage().getContentItem().exists())
				{
					getWebServer().getOpenEditEngine().beginRender(request);
				}
				else
				{
					getWebServer().getOpenEditEngine().executePathActions(request);
					if( !request.hasRedirected())
					{
						getWebServer().getModuleManager().executePageActions( page,request );
					}
					if( request.hasRedirected())
					{
						log.info("action was redirected");
					}
				}
			}
			catch( Throwable ex)
			{
				StringWriter ow = new StringWriter();
				while( ex != null)
				{
					ex.printStackTrace(new PrintWriter(ow));
					ex = ex.getCause();
				}
				logs.error("\n" + ow.toString() + "\n");
			}
//			finally
//			{
//				for (Iterator iterator = logs.listLogs().iterator(); iterator.hasNext();)
//				{
//					String log = (String) iterator.next();
//					output.append(log.toString());
//					output.append("\n");
//				}
//				appendText(output.toString());
//			}
		}
		finally
		{
			//thread.setContextClassLoader(oldLoader);
			logs.stopCapture();
		}

		if (request.hasCancelActions())
		{
			appendText("Action may have failed to run. Check permissions.");
		}
		//appendText(output.toString());
//		List oldlogs = (List)request.getPageValue("logs");
//		if( oldlogs != null)
//		{
//			for (Iterator iterator = oldlogs.iterator(); iterator.hasNext();)
//			{
//				Object object = (Object) iterator.next();
//				output.append(object.toString());			
//				output.append("<br/>\n");
//			}
//		}
//		long end = System.currentTimeMillis();
//		setLastRunTime((end - start) / 1000L); //minutes
		//log.info(output);
		return true;
	}

	protected Map getProperties()
	{
		return getPage().getProperties();

	}

	public WebServer getWebServer()
	{
		return fieldWebServer;
	}

	public void setWebServer(WebServer inWebServer)
	{
		fieldWebServer = inWebServer;
	}

//	public ClassLoader getClassLoader()
//	{
//		return fieldClassLoader;
//	}
//
//	public void setClassLoader(ClassLoader inClassLoader)
//	{
//		fieldClassLoader = inClassLoader;
//	}
	public String getName()
	{
		String name = getPage().getProperty("eventname");
		if( name == null)
		{
			name = getPage().getName();
		}
		return name;
	}
	public Page getPage()
	{
		return fieldPage;
	}

	public void setPage(Page inPage)
	{
		fieldPage = inPage;
		setDelay(inPage.getProperty("delay"));
		setEnabled(Boolean.parseBoolean(inPage.getProperty("enabled")));
		setPeriod(inPage.getProperty("period"));
	}
	public String toString()
	{
		if( getPage() != null)
		{
			return getPage().getDirectoryName() + "/" + getPage().getName();
		}
		return super.toString();
	}
	public int compareTo(Object inO)
	{
		PathEvent event = (PathEvent) inO;
		String name = event.toString();
		
		return toString().compareTo(name);
	}

	public void clearLog()
	{
		// TODO Auto-generated method stub
		fieldLastOutput = null;
	}

}
