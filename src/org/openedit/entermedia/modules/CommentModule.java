package org.openedit.entermedia.modules;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.openedit.entermedia.MediaArchive;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventListener;

import com.openedit.WebPageRequest;
import com.openedit.comments.Comment;
import com.openedit.comments.CommentArchive;
import com.openedit.modules.BaseModule;
import com.openedit.users.User;

public class CommentModule extends BaseMediaModule 
{
	protected CommentArchive fieldCommentArchive;
	protected WebEventListener fieldWebEventListener;
	
	public CommentArchive getCommentArchive() {
		return fieldCommentArchive;
	}

	public void setCommentArchive(CommentArchive inCommentArchive) {
		fieldCommentArchive = inCommentArchive;
	}
	
	public void loadComments(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String commentPath = findPath(inReq);
		
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

	
	private String findPath(WebPageRequest inReq) {
		String commentPath = inReq.findValue("commentpath");
		//old system was not safe
		if (commentPath == null)
		{
			String sourcePath = inReq.getRequestParameter("sourcepath");
		//	String catalogid = inReq.findValue("catalogid");
			commentPath =  sourcePath;	
		}
		else if(commentPath.contains("${albumid}"))
		{
			String albumid = inReq.getRequestParameter("albumid");
			if(albumid != null)
			{
				commentPath = commentPath.replace("${albumid}", albumid);
			}
		}
		return commentPath;
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
		String commentpath = findPath(inReq);
		
		String sourcePath = inReq.getRequestParameter("sourcepath");
		event.setProperty("sourcepath", sourcePath); //deprecated
		event.setProperty("commentpath", commentpath);
		event.setProperty("commenttext", inComment.getComment());
		event.setProperty("siteRoot", inReq.getSiteRoot());
		
		//add in extra info?
		for (Iterator iterator = inComment.getProperties().keySet().iterator(); iterator.hasNext();)
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
		
		WebEvent event = createEvent(comment, type + "commentadded", inReq);
		getWebEventListener().eventFired(event);

		String commentPath = findPath(inReq);
		getCommentArchive().addComment(commentPath, comment);
		//?????? Tuan check again
		loadComments(inReq, commentPath);
	}
	
	
	public void removeComment(WebPageRequest inReq)
	{
		String commentPath = findPath(inReq);
		String text = inReq.getRequestParameter("commenttext");
		//This is going to be the number from Date.getTime()
		String datestring = inReq.getRequestParameter("commentdate");
		Date date = new Date(Long.parseLong(datestring));
		String user = inReq.getRequestParameter("commentuser");
		
		Comment comment = new Comment();
		comment.setComment(text);
		User u = getUserManager().getUser(user);
		
		comment.setUser(u);
		comment.setDate(date);
		
		getCommentArchive().removeComment(commentPath, comment);
		//?????? Tuan check again
		loadComments(inReq, commentPath);
		
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
