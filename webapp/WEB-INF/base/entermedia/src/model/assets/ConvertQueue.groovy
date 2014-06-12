package model.assets

import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors

import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.util.ExecutorManager

public class ConvertQueue extends EnterMediaObject
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
			//fieldGeneralConvertExecutor = getExecutorManager().createUnlimitedExecutor();
			//System.out.println("Creating unlimited" + max);
		}
		return fieldGeneralConvertExecutor;
	}
	public void execute( List<Runnable> inTasks)
	{
		List<Callable> runnow = new ArrayList<Callable>(inTasks.size());
	
		for (Runnable runner: inTasks)
		{ 
			runnow.add(Executors.callable(runner)); 
		}
	
		getExecutor().invokeAll(runnow);
	}
	
}
