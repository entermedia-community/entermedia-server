package org.entermediadb.events;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.openedit.OpenEditException;
import org.openedit.users.User;

public class BaseTask extends PropertyContainer
{
	protected User fieldUser;
	protected String fieldId;
	protected String fieldName;
	protected String fieldWorkflowID;
	protected int fieldActionIndex = 0;
	protected List fieldActions;
	protected boolean fieldAsleep;
	protected long fieldExpirationTime;
	protected Date fieldLastRun;
	protected String fieldLastOutput;
	
	public BaseTask()
	{
		
	}
	
	public long getExpirationTime()
	{
		return fieldExpirationTime;
	}

	public void setExpirationTime(long inExpirationTime)
	{
		fieldExpirationTime = inExpirationTime;
	}
	
	public List getActions()
	{
		if (fieldActions == null)
		{
			fieldActions = new ArrayList();
		}
		return fieldActions;
	}
	
	public Action getAction(int inIndex)
	{
		if (inIndex < getActions().size())
		{
			return (Action)getActions().get(inIndex);
		}
		return null;
	}
	
	public User getUser()
	{
		return fieldUser;
	}

	public void setUser(User inUser)
	{
		fieldUser = inUser;
	}

	public String getId()
	{
		return fieldId;
	}

	public void setId(String inId)
	{
		fieldId = inId;
	}
	
	public int getNumActions()
	{
		return getActions().size();
	}
	
	public void reset()
	{
		setActionIndex(0);
	}
	
	public void fail()
	{
		setActionIndex(-1);
	}
	
	public boolean hasFailed()
	{
		return (getActionIndex() == -1);
	}
	
	public boolean isFinished()
	{
		return (getActionIndex() >= getNumActions());
	}

	public Action getCurrentAction()
	{
		if (isFinished() || hasFailed())
		{
			return null;
		}
		
		return getAction(getActionIndex());
	}

	public void nextAction()
	{
		if (!hasFailed() && !isFinished())
		{
			int newIndex = getActionIndex() + 1;
			setActionIndex(newIndex);
		}
	}

	public int getActionIndex()
	{
		return fieldActionIndex;
	}

	public void setActionIndex(int inStepIndex)
	{
		fieldActionIndex = inStepIndex;
	}

	public void executeCurrentAction() throws OpenEditException
	{
		wakeup();
		Action action = getCurrentAction();
		if( action != null)
		{
			setLastRun(new Date());
			if( action.execute(this) )
			{
				nextAction();
			}
			else
			{
				sleep();
			}
		}
	}

	public String getWorkflowID() {
		return fieldWorkflowID;
	}

	public void setWorkflowID(String inWorkflowID) {
		fieldWorkflowID = inWorkflowID;
	}

	public String getName() {
		return fieldName;
	}

	public void setName(String inName) {
		fieldName = inName;
	}
	
	public Iterator getPropertyNameIterator()
	{
		return getProperties().keySet().iterator();
	}
	
	public String getProperty(String key)
	{
		return (String)getProperties().get(key);
	}
	public void setProperty(String key, String value)
	{
		getProperties().put(key, value);
	}
	public void clear()
	{
		getActions().clear();
	}
	public void addAction( Action inAction )
	{
		getActions().add(inAction);
		addPropertyChild(inAction);
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

	public BaseTask copy()
	{
		BaseTask task = new BaseTask();

		task.fieldUser = fieldUser;
		task.fieldId = fieldId;
		task.fieldName = fieldName;
		task.fieldWorkflowID = fieldWorkflowID;
		task.fieldActionIndex = fieldActionIndex;
		task.fieldActions = fieldActions;
		for ( Iterator i = getPropertyNameIterator(); i.hasNext();)
		{
			String propertyName = (String) i.next();
			task.putProperty(propertyName, getProperty(propertyName));
		}
		task.fieldAsleep = fieldAsleep;
		task.fieldExpirationTime = fieldExpirationTime;

		return task;
	}
	public String getFormattedLastRun()
	{
		if( getLastRun() != null)
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
	}

	public String getLastOutput()
	{
		return fieldLastOutput;
	}

	public void setLastOutput(String inLastOutput)
	{
		fieldLastOutput = inLastOutput;
	}
}