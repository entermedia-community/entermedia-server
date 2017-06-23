package model.assets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.entermediadb.asset.convert.ConvertQueue;
import org.entermediadb.scripts.EnterMediaObject;
import org.openedit.OpenEditException;
import org.openedit.util.ExecutorManager;

public class BaseConvertQueue extends EnterMediaObject implements ConvertQueue
{
	///Load this on a per catallogid basis
	protected ExecutorManager fieldExecutorManager;
	protected static Executor fieldGeneralConvertExecutor;
	
	public ExecutorManager getExecutorManager()
	{
		return fieldExecutorManager;
	}
	public void setExecutorManager(ExecutorManager inExecutorManager)
	{
		fieldExecutorManager = inExecutorManager;
	}

	public ExecutorService getExecutor()
	{
		if(fieldGeneralConvertExecutor == null )
		{
			int max =  Runtime.getRuntime().availableProcessors();
			if( max > 20)
			{
				max = 15; //Disk IO gets crazy
			}
			else if( max < 4)
			{
				max = 4;
			}
			max--;
			fieldGeneralConvertExecutor = getExecutorManager().createExecutor(max, max); //Max does not seem to work as advertised

		}
		return (ExecutorService) fieldGeneralConvertExecutor;
	}
	
	public void execute( List<Runnable> inTasks) throws OpenEditException
	{
		List runnow = new ArrayList(inTasks.size());
	
		for (Iterator iterator = inTasks.iterator(); iterator.hasNext();) {
			Runnable runner = (Runnable) iterator.next();
			runnow.add(Executors.callable(runner));
		}
	
		try {
			getExecutor().invokeAll(runnow);
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			throw new OpenEditException(e);
		}
	}
	
}
