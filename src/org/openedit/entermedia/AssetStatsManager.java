package org.openedit.entermedia;

import java.util.HashMap;
import java.util.Map;

import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventHandler;

import com.openedit.hittracker.HitTracker;
import com.openedit.users.User;
import com.openedit.util.PathUtilities;

public class AssetStatsManager
{
	protected WebEventHandler fieldMediaEventHandler;
	protected SearcherManager fieldSearcherManager;
	protected Map fieldViewCache;
	protected long fieldViewExpireTime;
	
	public long getViewExpireTime()
	{
		return fieldViewExpireTime;
	}

	public void setViewExpireTime(long inViewExpireTime)
	{
		fieldViewExpireTime = inViewExpireTime;
	}

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
	public void logAssetPreview(String inCatalogId, String inSourcePath,String inAssetId, User inUser)
	{
		WebEvent change = new WebEvent();
		change.setOperation("asset/preview");
		change.setSearchType("asset");
		change.setSourcePath(inSourcePath);
		change.setUser(inUser);
		change.setCatalogId(inCatalogId);

		change.setProperty("assetid",inAssetId);

		getMediaEventHandler().eventFired(change);
	}
	
	public long getViewsForAsset(Asset inAsset)
	{
		//check with the log files and cache the results?
		if( inAsset == null)
		{
			return 0L;
		}
		if( System.currentTimeMillis() > getViewExpireTime())
		{
			getViewCache().clear(); //TODO: cache by asset
			setViewExpireTime(System.currentTimeMillis() + 1000*60*60); //once an hour
		}
		String key = inAsset.getCatalogId() + inAsset.getId();
		Long views = (Long)getViewCache().get(key);
		if( views == null)
		{
			Searcher logsearcher = getSearcherManager().getSearcher(inAsset.getCatalogId(), "assetpreviewLog");
			HitTracker all = logsearcher.fieldSearch("sourcepath", inAsset.getSourcePath());
			views = Long.valueOf(all.size());
			getViewCache().put(key, views); 
			checkAssetSave(inAsset, views);
		}
		inAsset.setProperty("assetviews",String.valueOf(views));
		return views.longValue();
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
			change.setOperation("asset/assetsave");
			change.setSearchType("asset");
			change.setSourcePath(inAsset.getSourcePath());
			change.setCatalogId(inAsset.getCatalogId());
			getMediaEventHandler().eventFired(change);
		}
	}

	protected Map getViewCache()
	{
		if (fieldViewCache == null)
		{
			fieldViewCache = new HashMap();
		}

		return fieldViewCache;
	}
}
