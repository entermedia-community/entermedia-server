package org.entermediadb.posts;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.servlet.Site;

public class PostModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(PostModule.class);

	public void loadPostFromSiteData(WebPageRequest inReq)
	{
		//Add the domain before the page name
		Site sitedata = (Site)inReq.getPageValue("site");
		if( sitedata != null)
		{
			String domain = sitedata.getFirstDomain();
			if( domain != null)
			{
				String name = inReq.getContentPage().getName();
				String sourcepath = domain + "/" + name;
				loadPost(inReq,sourcepath);				
				return;
			}
		}
		//loadPost(inReq);		//
		
	}
	public void loadPost(WebPageRequest inReq)
	{
		String sourcepath = inReq.getContentProperty("postsourcepath");
		if(sourcepath == null)
		{
			sourcepath = loadFromAppHome(inReq);
		}
		loadPost(inReq,sourcepath);
	}
	public void loadPost(WebPageRequest inReq, String sourcepath)
	{
		
		//getSearcherManager().getCacheManager()
		String catalogid = inReq.findPathValue("catalogid");
		//TODO: Cache this
		//TODO: Fallback? Use the actual page
		Searcher searcher = getSearcherManager().getSearcher(catalogid, "postdata");
		Data post =  (Data) searcher.query().exact("sourcepath", sourcepath).searchOne();
		if (post == null)
		{
			if (sourcepath.endsWith("index.html"))
			{
				sourcepath = sourcepath.substring(0, sourcepath.length() - 10);
				post = (Data) searcher.query().exact("sourcepath", sourcepath).searchOne();
			}
		}
		
		

		if (post == null && inReq.getUser() != null)
		{
			//TODO: Check they are in post edit mode
			String mode = inReq.getUser().get("oe_edit_mode");
			if("postedit".equals(mode))
			{
				post = (Data) searcher.createNewData();
				//post.setValue("siteid", siteid);
				post.setValue("sourcepath", sourcepath);
				post.setValue("maincontent", "");
				searcher.saveData(post);
			}
			else
			{
				//TODO: send 404
				return;
			}
		}
		
		//TODO: Set mod time
		inReq.putPageValue("postdata", post);
		
		
		//SEO
		if(post!=null)
		{
			if(post.getValue("metatitle") != null)
			{
				inReq.putPageValue("metatitle", post.getValue("metatitle"));
			}
		}
		
		//Load up a $sitehome and $postdata 
	}

	protected String loadFromAppHome(WebPageRequest inReq)
	{
		String path = inReq.getPath();

		String home = (String)inReq.getPageValue("communityhome");
		String found = cutroot(home, path);
		if(found == null )
		{
			home = (String)inReq.getPageValue("apphome");
			found = cutroot(home, path);
		}
		if( found == null)
		{
			found = path;
		}
		return found;
	}
	protected String cutroot(String apphome, String path)
	{
		if( apphome == null)
		{
			log.info("no apphome set for " + path);
			return null;
		}

		String sourcepath = null;
		
		if( path.startsWith(apphome) && apphome.contains("/"))
		{
			//int loc = path.indexOf("/",1);
			sourcepath = path.substring(apphome.length() + 1,path.length());
		}
		else
		{
			return null;
		}
		if( sourcepath.startsWith("/"))
		{
			sourcepath = sourcepath.substring(1,sourcepath.length());
		}
		return sourcepath;
	}
}
