package org.entermediadb.asset;

import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventHandler;
import org.openedit.hittracker.HitTracker;
import org.openedit.users.User;
import org.openedit.util.PathUtilities;

public class AssetStatsManager
{
	protected WebEventHandler fieldMediaEventHandler;
	protected SearcherManager fieldSearcherManager;
//	protected Map fieldViewCache;
//	protected long fieldViewExpireTime;
//	
//	public long getViewExpireTime()
//	{
//		return fieldViewExpireTime;
//	}
//
//	public void setViewExpireTime(long inViewExpireTime)
//	{
//		fieldViewExpireTime = inViewExpireTime;
//	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public WebEventHandler getMediaEventHandler()
	{
		return fieldMediaEventHandler;
	}

	public void setMediaEventHandler(WebEventHandler inMediaEventHandler)
	{
		fieldMediaEventHandler = inMediaEventHandler;
	}

	public void logAssetDownload(String inCatalogId, String inSourcePath, String inResult, User inUser)
	{
		WebEvent change = new WebEvent();
		change.setOperation("asset/download");
		change.setSearchType("asset");
		change.setProperty("filename", PathUtilities.extractFileName(inSourcePath ));
		change.setSourcePath(inSourcePath);
		change.setUser(inUser);
		change.setProperty("result", inResult);
		change.setCatalogId(inCatalogId);
		getMediaEventHandler().eventFired(change);
	}
	public void logAssetPreview(MediaArchive inArchive, Asset inAsset, User inUser)
	{
		WebEvent change = new WebEvent();
		change.setOperation("asset/preview");
		change.setSearchType("asset");
		change.setSourcePath(inAsset.getSourcePath());
		change.setUser(inUser);
		change.setCatalogId(inArchive.getCatalogId());

		change.setProperty("assetid",inAsset.getId());
	
		String views = inAsset.get("assetviews");
		if( views != null )
		{
			long assetviews = Long.parseLong(views);
			assetviews++;
			inAsset.setProperty("assetviews",String.valueOf(assetviews)); //this will be overridden 
		}

		getMediaEventHandler().eventFired(change);
	}
	
	public long getViewsForAsset(String inCatalogId, Asset inAsset)
	{
		//check with the log files and cache the results?
		if( inAsset == null)
		{
			return 0L;
		}
		if(inAsset.getId().startsWith("multi")){
			return 0L;
		}
		
		long assetexpire = 0L;
		String expires = inAsset.get("assetviewsexpires");
		if( expires != null )
		{
			assetexpire = Long.parseLong(expires);
		}
		long now = System.currentTimeMillis();
		if( assetexpire == 0 || assetexpire > now)
		{
			assetexpire = now + 1000*60*60; //once an hour
			
			Searcher logsearcher = getSearcherManager().getSearcher(inCatalogId, "assetpreviewLog");
			HitTracker all = logsearcher.fieldSearch("sourcepath", inAsset.getSourcePath());
			Long views = Long.valueOf(all.size());
			inAsset.setProperty("assetviews",String.valueOf(views));
			inAsset.setProperty("assetviewsexpires",String.valueOf(assetexpire));
			return views.longValue();
		}
		else
		{
			String views = inAsset.get("assetviews");
			if( views != null )
			{
				return Long.parseLong(views);
			}
			return -1;
		}
	}

	protected void checkAssetSave(Asset inAsset, long newcount)
	{
		//save it to the asset index if it has not been updated within 10 hits or 24 hours?
		long oldcount = 0;
		String views = inAsset.get("assetviews");
		if( views != null)
		{
			oldcount = Long.parseLong(views);
		}
		inAsset.setProperty("assetviews",String.valueOf(newcount));

		if( newcount - 10 >  oldcount)
		{
			WebEvent change = new WebEvent();
			change.setOperation("assetsave");
			change.setSearchType("asset");
			change.setSourcePath(inAsset.getSourcePath());
			change.setCatalogId(inAsset.getCatalogId());
			getMediaEventHandler().eventFired(change);
		}
	}
}
