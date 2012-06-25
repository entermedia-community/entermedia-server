package importing;

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.event.WebEvent

import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery;
import org.openedit.events.PathEventManager;
import java.util.ArrayList;
import java.util.Date;
import org.openedit.util.DateStorageUtil;

/*
public void checkLibraries() 
{
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	Searcher assetsearcher = mediaArchive.getAssetSearcher();
	SearchQuery q = assetsearcher.createSearchQuery();
	String ids = context.getRequestParameter("assetids");
	if( ids == null)
	{
		return;
	}
	String assetids = ids.replace(","," ");
	q.addOrsGroup( "id", assetids );

	HitTracker assets = assetsearcher.search(q);
	assets.each
	{
		Asset asset = mediaArchive.getAsset(it.id);	
		if( asset.getSourcePath().startsWith("Marketing_Department/Web" )
		{
			asset.addValue("libraries","marketing_web");		
		}
		mediaarchive.saveAsset( asset, user );
	}
}

checkLibraries();

*/