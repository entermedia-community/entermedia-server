package org.openedit.entermedia.cluster;

import java.io.File;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.manage.PageManager;
import com.openedit.users.UserManager;

public class LocalDataManager
{
	//we should at least have a dirty flag to speed up checking
	private static final Log log = LogFactory.getLog(LocalDataManager.class);

	protected UserManager fieldUserManager;
	protected PageManager fieldPageManager;
	protected File fieldRoot;
	protected SearcherManager fieldSearcherManager;
	public UserManager getUserManager()
	{
		return fieldUserManager;
	}
	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}
	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}
	public File getRoot()
	{
		return fieldRoot;
	}
	public void setRoot(File inRoot)
	{
		fieldRoot = inRoot;
	}
	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}
	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}
	
	public HitTracker listChangesSince(String inAppId, Date inFrom)
	{
		//first update the index
		DataFileSearcher searcher = (DataFileSearcher)getSearcherManager().getSearcher(inAppId, "dataFile");
		searcher.updateFileList();
	
		SearchQuery query = searcher.createSearchQuery();
		query.addAfter("lastmodified",inFrom);
		
		HitTracker tracker = searcher.search(query);
		return tracker;
	}
	

	protected void verifyClustersFile(WebPageRequest inReq)
	{
		String applicationid = inReq.findValue("applicationid");
		Searcher clusters = getSearcherManager().getSearcher(applicationid, "cluster");
		String thishost = inReq.getSiteRoot();
		boolean hasself = false;
		for (Iterator iterator = clusters.getAllHits().iterator(); iterator.hasNext();)
		{
			Data remoteserver = (Data) iterator.next();
			String host = remoteserver.get("siteroot");
			if(thishost.equals(host))
			{
				hasself = true;
				String status =remoteserver.get("status");
				if( !"active".equals(status))
				{
					remoteserver.setProperty("status", "active");
					clusters.saveData(remoteserver, inReq.getUser());
					return;
				}
			}
		}
		if( !hasself )
		{
			Data self = clusters.createNewData();
			self.setProperty("siteroot", inReq.getSiteRoot());
			self.setProperty("status", "active");
			clusters.saveData(self, inReq.getUser());
		}
		
	}
	
}
