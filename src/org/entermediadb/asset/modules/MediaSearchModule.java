/*
 * Created on Jul 19, 2006
 */
package org.entermediadb.asset.modules;

import java.util.Iterator;
import java.util.List;

import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.location.GeoCoder;
import org.entermediadb.location.Position;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.profile.UserProfile;


//Use DataEditModule for searching asset data
public class MediaSearchModule extends BaseMediaModule
{
	protected GeoCoder fieldGeoCoder;
	

	public GeoCoder getGeoCoder() {
		if (fieldGeoCoder == null) {
			fieldGeoCoder = new GeoCoder();
			
		}

		return fieldGeoCoder;
	}

	public void setGeoCoder(GeoCoder fieldGeoCoder) {
		this.fieldGeoCoder = fieldGeoCoder;
	}
	public void searchLibrary(WebPageRequest inPageRequest) throws Exception
	{
		Data selectedlibrary = (Data)inPageRequest.getPageValue("selectedlibrary");
		if( selectedlibrary != null)
		{
			MediaArchive archive = getMediaArchive(inPageRequest);
			String catid = selectedlibrary.get("categoryid");
			inPageRequest.setRequestParameter("nodeID", catid);
			HitTracker tracker = archive.getAssetSearcher().query().enduser(true).exact("category",catid).search(inPageRequest);
			tracker.getSearchQuery().setProperty("categoryid", catid);
		}
	}

	public void searchCategories(WebPageRequest inPageRequest) throws Exception
	{
		MediaArchive archive = getMediaArchive(inPageRequest);
		Category category = archive.getCategory(inPageRequest);
		inPageRequest.putPageValue("category",category);
		inPageRequest.putPageValue("selectedcategory",category);
		
		
		String exact = inPageRequest.findValue("exact-search");
		HitTracker tracker = null;
		if( exact != null && Boolean.parseBoolean(exact))
		{
			tracker = archive.getAssetSearcher().query().exact("category-exact",category.getId()).search(inPageRequest);
		}
		else
		{
			tracker = archive.getAssetSearcher().searchCategories(inPageRequest, category);
		}
		if( tracker != null)
		{
				//TODO: Seems like this could be done within the searcher or something
				tracker.setDataSource(archive.getCatalogId() + "/categories/" + category.getId());
				Data librarycol = (Data) inPageRequest.getPageValue("librarycol");
				if(librarycol != null){
					tracker.getSearchQuery().setProperty("collectionid", librarycol.getId());
				}
				tracker.getSearchQuery().setProperty("categoryid", category.getId());

		}
		UserProfile prefs = (UserProfile)inPageRequest.getUserProfile();
		if( prefs != null)
		{
			prefs.setProperty("lastcatalog", archive.getCatalogId());
			//prefs.save();
		}
	}
	/**
	 * not used
	 * @param inPageRequest
	 * @throws Exception
	 */
	public void searchExactCategories(WebPageRequest inPageRequest) throws Exception
	{
		MediaArchive archive = getMediaArchive(inPageRequest);
		Category category = archive.getCategory(inPageRequest);

		archive.getAssetSearcher().searchExactCategories(inPageRequest, category);
	}
	
	public void searchFavories(WebPageRequest inPageRequest) throws Exception
	{
		String userid = inPageRequest.findValue("userid");
		if(userid == null){
			return;
		}
		MediaArchive archive = getMediaArchive(inPageRequest);

		Searcher searcher = archive.getSearcherManager().getSearcher(archive.getCatalogId(), "assetvotes");
		SearchQuery query = searcher.createSearchQuery();
		query.addExact("username", userid);
		query.addSortBy("timeDown");
		HitTracker assets = searcher.cachedSearch(inPageRequest, query);
		if( assets.size() > 0)
		{
			//Now do a big OR statement
			SearchQuery aquery = archive.getAssetSearcher().createSearchQuery();
			aquery.setSortBy(inPageRequest.findValue("sortby"));
			SearchQuery orquery = archive.getAssetSearcher().createSearchQuery();
			orquery.setAndTogether(false);
			for (Iterator iterator = assets.getPageOfHits().iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				String assetid = data.get("assetid");
				if(assetid != null){
					orquery.addExact("id", data.get("assetid"));
				}
			}
			aquery.addChildQuery(orquery);
			
			HitTracker tracker = archive.getAssetSearcher().cachedSearch(inPageRequest, aquery);
		}
	}

	public void searchUploads(WebPageRequest inPageRequest) throws Exception
	{
		String userid = inPageRequest.findValue("userid");
		
		MediaArchive archive = getMediaArchive(inPageRequest);

		SearchQuery aquery = archive.getAssetSearcher().createSearchQuery();
		aquery.setSortBy(inPageRequest.findValue("sortby"));
		aquery.addExact("owner", userid);
		archive.getAssetSearcher().cachedSearch(inPageRequest, aquery);
	}
	

	public void rangeSearch(WebPageRequest inReq) throws Exception {
		
			//This does a search in a square for the range (+/- the range in both directions from the point
		MediaArchive archive = getMediaArchive(inReq);

		String target = inReq.getRequestParameter("target");
		String rangeString = inReq.findValue("range");
		String detailid = inReq.findValue("field");
		
		
		double range = Double.parseDouble(rangeString);
	    range = range / 157253.2964;//convert to decimal degrees (FROM Meters)
		
	    List positions = getGeoCoder().getPositions(target);
		if(positions != null && positions.size() > 0){
			Position p = (Position)positions.get(0);
			Double latitude = p.getLatitude();
			Double longitude = p.getLongitude();
			Double maxlat = latitude + range;
			Double minlat = latitude - range;
			Double maxlong = longitude + range;
			Double minlong = longitude - range; 
			Searcher searcher = archive.getAssetSearcher();
			
			SearchQuery query = searcher.addStandardSearchTerms(inReq);
			if(query == null){
				query = searcher.createSearchQuery();
			}
			
			
			query.addBetween(detailid + "_lat_sortable", minlat, maxlat);
			query.addBetween(detailid + "_lng_sortable", minlong, maxlong );
			searcher.cachedSearch(inReq, query);
			
		}
	
	}

	
	
	public void findMappableAssets(WebPageRequest inReq){
		String detailid = inReq.findValue("detailid");
		
		MediaArchive archive = getMediaArchive(inReq);

		SearchQuery aquery = archive.getAssetSearcher().createSearchQuery();
		aquery.setSortBy(inReq.findValue("sortby"));
		aquery.addExact(detailid + "_available", "true");
		archive.getAssetSearcher().cachedSearch(inReq, aquery);
	}
	
	
	

	
}