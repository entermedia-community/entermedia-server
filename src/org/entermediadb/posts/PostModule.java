package org.entermediadb.posts;

import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;

public class PostModule extends BaseMediaModule
{
	public void loadPost(WebPageRequest inReq)
	{
		String path = inReq.getPath();

		String sitehome = (String) inReq.getPageValue("sitehome");
		String apphome = (String)inReq.getPageValue("apphome");

		
		if( sitehome.isEmpty() )
		{
			apphome = apphome.substring(sitehome.length() + 1,apphome.length());
		}
		String sourceppath = path.substring(apphome.length()+1, path.length());
		//getSearcherManager().getCacheManager()
		String catalogid = inReq.findValue("catalogid");
		//TODO: Cache this
		//TODO: Fallback? Use the actual page
		Searcher searcher = getSearcherManager().getSearcher(catalogid, "postdata");
		PostData post = (PostData) searcher.query().exact("sourcepath", sourceppath).searchOne();
		if (post == null)
		{
			if (path.endsWith("index.html"))
			{
				path = path.substring(0, path.length() - 10);
			}
		}
		
		post = (PostData) searcher.query().exact("sourcepath", path).searchOne();

		if (post == null)
		{
			post = (PostData) searcher.createNewData();
			//post.setValue("siteid", siteid);
			post.setValue("sourcepath", sourceppath);
			post.setValue("maincontent", "Hello World");
			searcher.saveData(post);
		}
		inReq.putPageValue("postdata", post);
		//Load up a $sitehome and $postdata 
	}
}
