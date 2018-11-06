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

		apphome = apphome.substring(sitehome.length() + 1,apphome.length());

		
		String 	sourcepath = path.substring(path.indexOf(apphome),path.length());
		
		//getSearcherManager().getCacheManager()
		String catalogid = inReq.findValue("catalogid");
		//TODO: Cache this
		//TODO: Fallback? Use the actual page
		Searcher searcher = getSearcherManager().getSearcher(catalogid, "postdata");
		PostData post = (PostData) searcher.query().exact("sourcepath", sourcepath).searchOne();
		if (post == null)
		{
			if (sourcepath.endsWith("index.html"))
			{
				sourcepath = sourcepath.substring(0, sourcepath.length() - 10);
			}
		}
		
		post = (PostData) searcher.query().exact("sourcepath", sourcepath).searchOne();

//		if (post == null)
//		{
//			post = (PostData) searcher.createNewData();
//			//post.setValue("siteid", siteid);
//			post.setValue("sourcepath", sourceppath);
//			post.setValue("maincontent", "Hello World");
//			searcher.saveData(post);
//		}
		inReq.putPageValue("postdata", post);
		//Load up a $sitehome and $postdata 
	}
}
