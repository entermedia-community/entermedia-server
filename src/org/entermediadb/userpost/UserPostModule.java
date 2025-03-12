package org.entermediadb.userpost;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;

public class UserPostModule extends BaseMediaModule
{
	protected Searcher getPostCommentSearcher(WebPageRequest inReq)
	{
		String catid = inReq.findPathValue("catalogid");
		Searcher searcher = getSearcherManager().getSearcher(catid, "postcomments");
		return searcher;
	}
	/**
	 * Cache this stuff. Make sure we update
	 * @param inReq
	 */
	public void listPostComments(WebPageRequest inReq)
	{
		//search DB reverse, divide by pages show only one page, flip, cache by post, clear cache often, pagination
		String useruploadid = inReq.getRequestParameter("useruploadid");
		if(useruploadid == null)
		{
			Data userupload = (Data)inReq.getPageValue("upload");
			if( userupload != null)
			{
				useruploadid = userupload.getId();
			}
		}
		if( useruploadid != null)
		{
			Searcher searcher = getPostCommentSearcher(inReq);
			HitTracker results = searcher.query().exact("useruploadid", useruploadid).sort("dateDown").search();
			results.setHitsPerPage(10);
			Collection page = results.getPageOfHits();
			ArrayList loaded = new ArrayList();
			for (Iterator iterator = page.iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				PostComment comment = (PostComment)searcher.loadData(data);
				loaded.add(comment);
			}
			Collections.reverse(loaded);
			inReq.putPageValue("comments", loaded);
		}
		
	}
	public void apppendComment(WebPageRequest inReq)
	{
		//search DB reverse, divide by pages show only one page, flip, cache by post, clear cache often, pagination
		String useruploadid = inReq.getRequestParameter("useruploadid");
		if( useruploadid != null)
		{
			Searcher searcher = getPostCommentSearcher(inReq);
			PostComment comment = (PostComment)searcher.createNewData();
			comment.setDate(new Date());
			comment.setValue("userid", inReq.getUserName());
			comment.setValue("useruploadid",useruploadid);
			String commenttext = inReq.getRequestParameter("commenttext");
			comment.setComment(commenttext);
			searcher.saveData(comment);
		}
	}
	
	public void getPostLikes(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		
		String postid = inReq.getRequestParameter("userpostid");
		String userid = inReq.getRequestParameter("userid");
		
		HitTracker exists = archive.query("userpostlikes").exact("userpost", postid).sort("dateDown").search(inReq);
		inReq.putPageValue("userpostlikes", exists);
		for (Iterator iterator = exists.iterator(); iterator.hasNext();) {
			Data postlike = (Data) iterator.next();
			if (postlike.get("user").equals(inReq.getUserName())) {
				inReq.putPageValue("likedbyme", true);
				return;
			}
		}
	}
	
	public void togglePostLike(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		
		String postid = inReq.getRequestParameter("userpostid");
		String userid = inReq.getRequestParameter("userid");
		if (postid != null && userid != null)
		{
			Data exists = archive.query("userpostlikes").exact("user", userid).exact("userpost", postid).searchOne();
			if (exists != null) {
				archive.getSearcher("userpostlikes").delete(exists, inReq.getUser());
			}
			else
			{
				exists = archive.getSearcher("userpostlikes").createNewData();
				exists.setValue("user", userid);
				exists.setValue("userpost", postid);
				exists.setValue("date", new Date());
				archive.getSearcher("userpostlikes").saveData(exists);
				
			}
			
			inReq.putPageValue("userpostid", postid);
		}
	}
	
	
}
