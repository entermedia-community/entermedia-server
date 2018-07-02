package org.entermediadb.asset.search;

import java.util.Collection;
import java.util.List;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.AssetArchive;
import org.entermediadb.asset.Category;
import org.json.simple.JSONArray;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;

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

	public abstract void updateIndex(List inAssets);

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
	
	public HitTracker searchByIds(Collection<String> inIds);
	public Asset getAssetBySourcePath(String inSourcepath, boolean autocreate);
	public Asset getAssetBySourcePath(String inSourcepath);

	public String getFulltext(Asset asset);

	public void saveJson(Collection inJsonArray);


}