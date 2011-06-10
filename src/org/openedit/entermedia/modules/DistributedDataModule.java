package org.openedit.entermedia.modules;

import java.util.Date;

import org.openedit.entermedia.cluster.LocalDataManager;
import org.openedit.util.DateStorageUtil;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;

public class DistributedDataModule extends BaseMediaModule
{
	protected LocalDataManager fieldLocalDataManager;

	
	public LocalDataManager getLocalDataManager()
	{
		return fieldLocalDataManager;
	}


	public void setLocalDataManager(LocalDataManager inLocalDataManager)
	{
		fieldLocalDataManager = inLocalDataManager;
	}


	public void listRecentDataChanges(WebPageRequest inReq)
	{
		String since = inReq.getRequestParameter("since");
		Date from = DateStorageUtil.getStorageUtil().parseFromStorage(since);
		String appid = inReq.findValue("applicationid");
		HitTracker changes = getLocalDataManager().listChangesSince(appid, from);
		//needs to be rendered as XML
		inReq.putPageValue("fileitemlist", changes);
	}
}
