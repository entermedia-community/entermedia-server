package org.entermediadb.userpost;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;

public class UserPostModule extends BaseMediaModule
{
	protected Searcher getPostCommentSearcher(WebPageRequest inReq)
	{
		String catid = inReq.findValue("catalogid");
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
}
