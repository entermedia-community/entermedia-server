package org.entermediadb.scripts;

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
	protected String fieldPrefix = "";
	protected TextAppender fieldTextAppender;
	protected boolean fieldAppendLogs;
	
	public TextAppender getTextAppender()
	{
		return fieldTextAppender;
	}

	public void setTextAppender(TextAppender inTextAppender)
	{
		fieldTextAppender = inTextAppender;
	}

	public ScriptLogger()
	{
	}

	public void saveLog(String intype, String text, Throwable ex)
	{
		if( ex != null)
		{
			log.info(getPrefix() + " " + intype + " " + text, ex );
		}
		else if( "debug".equals(intype))
		{
			log.debug(getPrefix() + " " + intype + " " + text );
		}	
		else
		{
			log.info(getPrefix() + " " + intype + " " + text );
		}
		if( !fieldAppendLogs)
		{
			return;
		}
		StringBuffer buffer = new StringBuffer();
		
		if( getPrefix() != null)
		{
			buffer.append(getPrefix());
			buffer.append(" ");
		}
		buffer.append(text);
//		buffer.append(" ");
//		buffer.append(intype);
		if( ex != null)
		{
			buffer.append("\n");
			buffer.append(ex.toString());
		}
		
		if( fieldTextAppender != null )
		{
			fieldTextAppender.appendText(buffer.toString());
		}
		else
		{
			//LogEntry entry = new LogEntry();
			
			getLogs().add(buffer.toString());
			
			if( getLogs().size() > 10000)
			{
				getLogs().remove(0);
			}
		}

	}
	
	public String getPrefix()
	{
		return fieldPrefix;
	}
	public void setPrefix(String inPrefix)
	{
		fieldPrefix = inPrefix + " ";
	}
	public void debug(String inText, Throwable ex)
	{
		//log.debug(getPrefix() + inText,ex);
		saveLog("debug", inText, ex);
	}
	
	public void debug(String inText)
	{
		saveLog("debug", inText, null);
	}
	public void info(Object inObj)
	{
		saveLog("info", String.valueOf(inObj),null);
	}
	public void info(String inText, Throwable ex)
	{
		saveLog("info", inText, ex);
	}
	public void info(String inText)
	{
		saveLog("info", inText, null);
	}
	public void error(String inText, Throwable ex)
	{
		saveLog("error", inText, ex);
	}
	public void error(String inText)
	{
		saveLog("info", inText, null);
	}
	public void error(Object inObject, Throwable inThrowable)
	{
		saveLog("info", String.valueOf(inObject), inThrowable);
	}
	public void error(Object inObject)
	{
		saveLog("info", String.valueOf(inObject), null);
	}
//	public void add(Object inVal)
//	{
//		info(getPrefix() + String.valueOf( inVal ) );
//	}
	public List getLogs()
	{
		if (fieldLogs == null)
		{
			fieldLogs = new ArrayList();
		}
		return fieldLogs;
	}

	public void publish(LogRecord inRecord)
	{
		saveLog(inRecord.getLevel().getName(), inRecord.getMessage(), inRecord.getThrown());
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
		//CompositeHandler composite = loadComposite();
		//composite.addChild(this);
		fieldAppendLogs = true;
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
//		CompositeHandler composite = loadComposite();
//		composite.removeChild(this);
//		if( composite.getChildren().size() == 0)
//		{
//			Logger logger = Logger.getLogger("");
//			logger.removeHandler(composite);
//		}
		fieldAppendLogs = false;
	}
	public List listLogs()
	{
		List text = new ArrayList();
		List all = new ArrayList(getLogs()); //in case another thread is appending to the list
		for (Iterator iterator = all.iterator(); iterator.hasNext();)
		{
			Object entry = iterator.next();
			if( entry != null )
			{
				text.add(String.valueOf( entry) );
			}
		}
		
		return text;
	}

	protected String toString(LogEntry inEntry)
	{
		if( inEntry == null)
		{
			return "null LogEntry";
		}
		if(inEntry.getName() != null && inEntry.getName().equals(getClass().getName() ) )
		{
			return inEntry.getMessage();
		}
		else
		{
			return inEntry.toString();
		}
	}

}
