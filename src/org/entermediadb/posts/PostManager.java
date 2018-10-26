package org.entermediadb.posts;

import java.util.Collection;
import java.util.Iterator;

import org.openedit.Data;
import org.openedit.cache.CacheManager;
import org.openedit.data.SearcherManager;
import org.openedit.servlet.SiteData;

public class PostManager
{
	protected CacheManager fieldCacheManager;
	protected SearcherManager fieldSearcherManager;
	protected SiteData NULLSITE = new SiteData();
	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public CacheManager getCacheManager()
	{
		return fieldCacheManager;
	}

	public void setCacheManager(CacheManager inCacheManager)
	{
		fieldCacheManager = inCacheManager;
	}
	
	public String getDomain(String base) {
		// string off start
		String basestring = base.substring(base.lastIndexOf("//") + 2,
				base.length());
		int port = basestring.indexOf(":");
		if( port > -1)
		{
			basestring = basestring.substring(0,port);
		}
		
		int nextslash = basestring.indexOf("/");
		if( nextslash > -1)
		{
			basestring = basestring.substring(0,nextslash);
		}
		basestring = basestring.toLowerCase();
		return basestring;
	}
	
	public SiteData findSiteData(String inUrl)
	{
		String domain = getDomain(inUrl);
		SiteData found = (SiteData)getCacheManager().get("systemsitedata", domain);
		if( found == null)
		{
			found = (SiteData)getSearcherManager().query("system", "site").exact("domains", domain ).searchOne();
			if(found == null)
			{
				found = NULLSITE;
			}
			else
			{
				Collection hits = getSearcherManager().query("system", "siteparameters").exact("siteid", found.getId() ).search();
				for (Iterator iterator = hits.iterator(); iterator.hasNext();)
				{
					Data data = (Data) iterator.next();
					found.setSiteParameter(data.get("parametername"),data.get("parametervalue"));
				}
			}
				
			getCacheManager().put("systemsitedata", domain, found);
		}
		if( found == NULLSITE)
		{
			return null;
		}
		return found;
	}
	
}
