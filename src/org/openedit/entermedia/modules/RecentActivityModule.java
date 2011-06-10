package org.openedit.entermedia.modules;

import org.openedit.data.Searcher;
import org.openedit.entermedia.EnterMedia;
import org.openedit.entermedia.friends.RecentActivityManager;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventListener;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.users.User;

public class RecentActivityModule extends BaseMediaModule
{
	protected RecentActivityManager fieldRecentActivityManager;
	
	public RecentActivityManager getRecentActivityManager()
	{
		return fieldRecentActivityManager;
	}

	public void setRecentActivityManager(RecentActivityManager inRecentActivityManager)
	{
		fieldRecentActivityManager = inRecentActivityManager;
	}

	public void addEventLog(WebPageRequest inReq)
	{
		WebEvent inEvent = (WebEvent)inReq.getPageValue("webevent");
		if( inEvent != null)
		{
			String appid = inReq.findValue("applicationid");
			Searcher found = getSearcherManager().getSearcher(appid, "recentactivityLog");
			if( found instanceof WebEventListener)
			{
				WebEventListener lucenelogsearcher= (WebEventListener)found;
				lucenelogsearcher.eventFired(inEvent);
			}
		}
	}
	
	public void searchRecentActivity( WebPageRequest inReq) throws Exception
	{
		User owner = (User)inReq.getPageValue("owner");
		if( owner != null)
		{
			String recentactivityid = inReq.findValue("recentactivityid");
			EnterMedia matt = getEnterMedia(recentactivityid);
			HitTracker activity = getRecentActivityManager().getActivityForUser(matt, owner, inReq);
			
			inReq.putPageValue("recentactivity", activity);
		}
	}

}
