package org.entermediadb.asset.modules;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.comments.Comment;
import org.entermediadb.comments.CommentArchive;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventListener;
import org.openedit.users.User;

public class CommentModule extends BaseMediaModule 
{
	protected CommentArchive fieldCommentArchive;
	
	public CommentArchive getCommentArchive() {
		return fieldCommentArchive;
	}

	public void setCommentArchive(CommentArchive inCommentArchive) {
		fieldCommentArchive = inCommentArchive;
	}
	
	public void loadComments(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String commentPath = findSourcePath(inReq);
		
		Collection comments = getCommentArchive().loadComments(archive.getCatalogId(), commentPath);
		inReq.putPageValue("comments", comments);
	}

	
	public void loadComments(WebPageRequest inReq, String commentPath)
	{
		//MediaArchive arch/ive = getMediaArchive(inReq);
		if (!commentPath.endsWith(".xml"))
		{
			commentPath = commentPath + ".xml";
		}
		
		Collection comments = getCommentArchive().loadComments(commentPath);
		inReq.putPageValue("comments", comments);
	}

	
	protected String findSourcePath(WebPageRequest inReq) 
	{
		Data data = (Data)inReq.getPageValue("asset");
		if( data == null )
		{
			data = (Data)inReq.getPageValue("data");
		}
		String sourcePath = null;
		if( data != null )
		{
			sourcePath = data.getSourcePath();
		}
		else
		{
			sourcePath = inReq.getRequestParameter("sourcepath");
		}
		
		return sourcePath;
	}
	
	private WebEvent createEvent(Comment inComment, String inOperation, WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		WebEvent event = new WebEvent();
		if(catalogid == null)
		{
			catalogid = inReq.findValue("applicationid");
		}
		event.setCatalogId(catalogid);
		String commentpath = findSourcePath(inReq);
		
		String sourcePath = inReq.getRequestParameter("sourcepath");
		event.setProperty("sourcepath", sourcePath); //deprecated
		event.setProperty("commentpath", commentpath);
		event.setProperty("commenttext", inComment.getComment());
		event.setProperty("siteRoot", inReq.getSiteRoot());
		
		//add in extra info?
		for (Iterator iterator = inComment.keySet().iterator(); iterator.hasNext();)
		{
			String key = (String) iterator.next();
			event.setProperty(key, inComment.get(key));
		}
		//event.addDetail("data", )
		event.setSearchType("comment");
		event.setSource(this);
		event.setOperation(inOperation);
		event.setDate(inComment.getDate());
		event.setUser(inReq.getUser());
		return event;
	}
	
	public void addComment(WebPageRequest inReq)
	{
		String text= inReq.getRequestParameter("comment");
		if(text == null || text.length() == 0)
		{
			return;
		}
		Comment comment = new Comment();
		comment.setComment(text);
		comment.setUser(inReq.getUser());
		comment.setDate(new Date());
		
		String type = inReq.findValue("commenttype");
		if( type == null)
		{
			type = ""; //should be album, asset etc..
		}
		Map all = inReq.getParameterMap();
		for (Iterator iterator = all.keySet().iterator(); iterator.hasNext();)
		{
			String	key= (String) iterator.next();
			if( key.startsWith("commentproperty."))
			{
				String value = (String)all.get(key);
				String akey = key.substring(key.indexOf(".") + 1,key.length());
				comment.setProperty(akey, value);				
			}
		}
		
//		WebEvent event = createEvent(comment, type + "commentadded", inReq);
//		getWebEventListener().eventFired(event);
		String catalogid = inReq.findValue("catalogid");
		String sourcepath = findSourcePath(inReq);
		getCommentArchive().addComment(catalogid, sourcepath, comment);
		loadComments(inReq);
	}
	
	
	public void removeComment(WebPageRequest inReq)
	{
		String sourcepath = findSourcePath(inReq);
		String text = inReq.getRequestParameter("commenttext");
		//This is going to be the number from Date.getTime()
		String datestring = inReq.getRequestParameter("commentdate");
		Date date = new Date(Long.parseLong(datestring));
		String user = inReq.getRequestParameter("commentuser");
		
		Comment comment = new Comment();
		comment.setComment(text);
		User u = getUserManager(inReq).getUser(user);
		
		comment.setUser(u);
		comment.setDate(date);
		
		String catalogid = inReq.findValue("catalogid");
		getCommentArchive().removeComment(catalogid, sourcepath, comment);
		loadComments(inReq);
		
	}

	
}
