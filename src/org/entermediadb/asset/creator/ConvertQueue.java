package org.entermediadb.asset.creator;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.openedit.util.ExecutorManager;

public interface ConvertQueue
{
	public ExecutorManager getExecutorManager();
	public void setExecutorManager(ExecutorManager inExecutorManager);
	public ExecutorService getExecutor();
	public void execute( List<Runnable> inTasks);
}
