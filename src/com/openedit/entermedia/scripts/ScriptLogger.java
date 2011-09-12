package com.openedit.entermedia.scripts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ScriptLogger extends Handler
{
	private static final Log log = LogFactory.getLog(ScriptLogger.class);
	protected List fieldLogs;
	protected String fieldPrefix;
	
	public String getPrefix()
	{
		return fieldPrefix;
	}
	public void setPrefix(String inPrefix)
	{
		fieldPrefix = inPrefix;
	}
	public void debug(String inText, Throwable ex)
	{
		log.debug(inText,ex);
	}
	
	public void debug(String inText)
	{
		log.debug(inText);
		//getLogs().add("debug: " + inText);
	}
	public void info(String inText, Throwable ex)
	{
		log.info(inText,ex);
	}
	public void info(String inText)
	{
		log.info(inText);
		//getLogs().add("info: " + inText);
	}
	public void error(String inText, Throwable ex)
	{
		log.error(inText,ex);
	}
	public void error(String inText)
	{
		log.error(inText);
		//getLogs().add("error: " + inText);
	}
	public void error(Object inObject, Throwable inThrowable)
	{
		log.error(inObject, inThrowable);
		//getLogs().add("error: " + inText);
	}
	public void add(Object inVal)
	{
		info(String.valueOf( inVal ) );
	}
	public List<LogEntry> getLogs()
	{
		if (fieldLogs == null)
		{
			fieldLogs = new ArrayList();
		}

		return fieldLogs;
	}

	public void publish(LogRecord inRecord)
	{
		//getRealHandler().publish(inRecord);
	
		getLogs().add(new LogEntry(inRecord));
		
		if( getLogs().size() > 10000)
		{
			getLogs().remove(0);
		}
		//System.out.println(inRecord.getMessage()); 
	}

	public void flush()
	{
		
	}

	public void close() throws SecurityException
	{

	}

	public void startCapture()
	{
		//This breaks Resin logs. 
		//3.x - Logs never come back
		//4 - Logs comeback
		//Tomcat - Log dissapear for a moment
		//If I try to wrap the existing logger terrible things happen
		//This may be fixed with thread context not set
		CompositeHandler composite = loadComposite();
		composite.addChild(this);
	}

	private CompositeHandler loadComposite()
	{
		Logger logger = Logger.getLogger("");
		CompositeHandler composite = null;

		Handler[] children = logger.getHandlers();
		if( children != null)
		{
			for (int i = 0; i < children.length; i++)
			{
				if( children[i] instanceof CompositeHandler)
				{
					composite = (CompositeHandler)children[i];
				}
			}
		}
		if( composite == null)
		{
			composite = new CompositeHandler();
			logger.addHandler(composite);
		}
		return composite;
	}
	public void stopCapture()
	{
		CompositeHandler composite = loadComposite();
		composite.removeChild(this);
		if( composite.getChildren().size() == 0)
		{
			Logger logger = Logger.getLogger("");
			logger.removeHandler(composite);
		}
	}
	public List listLogs()
	{
		List text = new ArrayList();
		List all = new ArrayList(getLogs()); //in case another thread is appending to the list
		for (Iterator iterator = all.iterator(); iterator.hasNext();)
		{
			LogEntry entry = (LogEntry) iterator.next();
			if( entry.getName().equals(getClass().getName() ) )
			{
				text.add(entry.getMessage());
			}
			else
			{
				text.add(entry.toString());
			}
		}
		
		return text;
	}

}
