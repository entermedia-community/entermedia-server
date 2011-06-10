package org.openedit.entermedia.modules;

import java.util.Date;

import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.entermedia.MediaArchive;
import org.openedit.event.WebEventListener;
import org.openedit.util.DateStorageUtil;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.User;
import com.openedit.util.PathUtilities;

public class MessageModule extends UserProfileModule 
{
	
	protected WebEventListener fieldWebEventListener;
	

	

	
	public void postMessage(WebPageRequest inReq){
		
		String messagesid = inReq.findValue("tweetid");
		Searcher messages = getSearcherManager().getSearcher(messagesid, "message");
		Data newpost = messages.createNewData();
		String username = inReq.getUserName();
		if(username == null){
			username = "anonymous";
		}
		newpost.setSourcePath("users/" + username);
		String[] fields = inReq.getRequestParameters("field");
		messages.updateData(inReq, fields, newpost);
		newpost.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
		newpost.setProperty("user", username);
		newpost.setProperty("status", "pending");
		messages.saveData(newpost, inReq.getUser());
	}
	
	public User loadOwner(WebPageRequest inReq)
	{
		String id = inReq.getRequestParameter("userid");
		if( id == null)
		{
			id = inReq.getContentProperty("username");
		}
		if( id == null)
		{
			id = PathUtilities.extractDirectoryName( inReq.getPath() );
		}
		User user = getUserManager().getUser(id);
		inReq.putPageValue("owner", user);
		return user;
	}
	
	
	
	public void loadUserPosts(WebPageRequest inReq){
		User user = loadOwner(inReq);
		String messagesid = inReq.findValue("tweetid");
		Searcher messages = getSearcherManager().getSearcher(messagesid, "message");
		SearchQuery q = messages.createSearchQuery();
		q.addMatches("user", user.getUserName());
		q.addSortBy("dateDown");
		messages.cachedSearch(inReq, q);

	}

	public void loadRecent(WebPageRequest inReq){
		
		String messagesid = inReq.findValue("tweetid");
		Searcher messages = getSearcherManager().getSearcher(messagesid, "message");
		SearchQuery q = messages.createSearchQuery();
		q.addMatches("id", "*");
		q.addMatches("status", "approved");
		
		q.addSortBy("dateDown");
		messages.cachedSearch(inReq, q);

	}

	public WebEventListener getWebEventListener()
	{
		return fieldWebEventListener;
	}
	public void setWebEventListener(WebEventListener webEventListener)
	{
		fieldWebEventListener = webEventListener;
	}
}
