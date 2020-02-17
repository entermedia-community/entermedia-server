package org.entermediadb.asset;

import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.event.EventManager;
import org.openedit.event.WebEvent;
import org.openedit.hittracker.HitTracker;
import org.openedit.users.User;
import org.openedit.util.PathUtilities;

public class AssetStatsManager
{
	protected EventManager fieldEventManager;
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

	public EventManager getEventManager()
	{
		return fieldEventManager;
	}

	public void setEventManager(EventManager inEventManager)
	{
		fieldEventManager = inEventManager;
	}

	public void logAssetDownload(String inCatalogId, String inSourcePath, String inResult, User inUser)
	{
		WebEvent change = new WebEvent();
		change.setOperation("download");
		change.setSearchType("asset");
		change.setProperty("filename", PathUtilities.extractFileName(inSourcePath ));
		change.setSourcePath(inSourcePath);
		change.setUser(inUser);
		change.setProperty("result", inResult);
		change.setCatalogId(inCatalogId);
		getEventManager().fireEvent(change);
	}
	public void logAssetPreview(MediaArchive inArchive, Asset inAsset, User inUser)
	{
		WebEvent change = new WebEvent();
		change.setOperation("preview");
		change.setSearchType("asset");
		change.setSourcePath(inAsset.getSourcePath());
		change.setUser(inUser);
		change.setCatalogId(inArchive.getCatalogId());

		change.setProperty("assetid",inAsset.getId());
	
		long assetviews = 0;
		String views = inAsset.get("assetviews");
		if( views != null )
		{
			assetviews = Long.parseLong(views);
		}	
		assetviews++;
		inAsset.setProperty("assetviews",String.valueOf(assetviews)); //this will be overridden later

		getEventManager().fireEvent(change);
	}
	
	public long getViewsForAsset(Asset inAsset)
	{
		//check with the log files and cache the results?
		if( inAsset == null || inAsset.getCatalogId() == null)
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
			
			Searcher logsearcher = getSearcherManager().getSearcher(inAsset.getCatalogId(), "assetpreviewLog");
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
			getEventManager().fireEvent(change);
		}
	}
}
