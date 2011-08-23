package org.openedit.entermedia.search;

import java.util.List;

import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.AssetArchive;
import org.openedit.entermedia.AssetPathFinder;
import org.openedit.entermedia.Category;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;

public interface AssetSearcher extends Searcher
{
	public HitTracker search(SearchQuery inQuery);

	/**
	 * Search for a category or any of its children
	 */
	public abstract HitTracker searchCategories(WebPageRequest inPageRequest, Category incategory) throws Exception;

	/**
	 * Only searches for an exact category match. No children.
	 */
	public abstract void searchExactCategories(WebPageRequest inPageRequest, Category incategory) throws Exception;

	public abstract void updateIndex(Data inAsset);

	public abstract void updateIndex(List inAssets, boolean inOptimize);

	public abstract AssetArchive getAssetArchive();

	public abstract void deleteFromIndex(Asset inAsset);

	public abstract void deleteFromIndex(String inId);

	public abstract void deleteFromIndex(HitTracker inOld);

	public abstract HitTracker getAllHits();

	/**
	 * Flush all pending writes. This operation is slow.
	 */
	public abstract void flush();

	String nextAssetNumber();
	

}