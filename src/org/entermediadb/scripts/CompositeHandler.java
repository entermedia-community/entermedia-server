package org.entermediadb.scripts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;


public class CompositeHandler extends java.util.logging.Handler
{
	protected List fieldChildren;
	
	public List getChildren()
	{
		if (fieldChildren == null)
		{
			fieldChildren = new ArrayList();
		}
		return fieldChildren;
	}

	public void setChildren(List inChildren)
	{
		fieldChildren = inChildren;
	}

	public void publish(LogRecord inRecord)
	{
		for (Iterator iterator = getChildren().iterator(); iterator.hasNext();)
		{
			Handler handler = (Handler) iterator.next();
			handler.publish(inRecord);
		}
	}

	public void flush()
	{
		for (Iterator iterator = getChildren().iterator(); iterator.hasNext();)
		{
			Handler handler = (Handler) iterator.next();
			handler.flush();
		}
		
	}

	public void close() throws SecurityException
	{
		for (Iterator iterator = getChildren().iterator(); iterator.hasNext();)
		{
			Handler handler = (Handler) iterator.next();
			handler.close();
		}		
	}

	public void addChild(Handler inScriptLogger)
	{
		//deal with Concurent Modification. Dont want getChildren() to fail mid way
		synchronized (this)
		{
			List copy = new ArrayList(getChildren());
			copy.remove(inScriptLogger);
			copy.add(inScriptLogger);
			setChildren(copy);
		}
	}

	public void removeChild(Handler inScriptLogger)
	{
		synchronized (this)
		{
			List copy = new ArrayList(getChildren());
			copy.remove(inScriptLogger);
			setChildren(copy);
		}
	}

}
