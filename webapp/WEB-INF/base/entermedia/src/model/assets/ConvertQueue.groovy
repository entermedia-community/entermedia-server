package model.assets

import java.util.concurrent.Executor;

import org.entermedia.cache.CacheManager;
import org.openedit.Data
import org.openedit.MultiValued
import org.openedit.data.BaseData
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive

import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.util.ExecutorManager;

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

	public Executor getExecutor()
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
			fieldGeneralConvertExecutor = getExecutorManager().createExecutor(1, max);
		}
		return fieldGeneralConvertExecutor;
	}

	
}
