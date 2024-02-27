/*
 * Created on Jul 19, 2006
 */
package org.entermediadb.asset.modules;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.facedetect.FaceProfileManager;
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
	private static final Log log = LogFactory.getLog(MediaSearchModule.class);
	

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
	public void showCategory(WebPageRequest inReq) throws Exception
	{
		//Look for a hitsessionid and make sure this category is in there
		MediaArchive archive = getMediaArchive(inReq);
		Category category = archive.getCategory(inReq);
		if(category == null) {
			return;
		}
		inReq.putPageValue("category",category);
		inReq.putPageValue("selectedcategory",category);
		
		String searchtype = resolveSearchType(inReq);
		if( searchtype == null)
		{
			searchtype = "asset";
		}
		
		HitTracker hits = archive.getSearcher(searchtype).loadHits(inReq);
		if (hits == null)
		{
			if( searchtype.equals("asset"))
			{
				hits = archive.getAssetSearcher().searchCategories(inReq, category);
			}
			else
			{
				hits  = archive.query( searchtype).match("catagory",category.getId()).search(inReq);
			}
			hits.getSearchQuery().setProperty("userinputsearch", "true"); //So it caches
		}
		else if( hits.getSearchQuery().getDetail("category") == null)
		{
			hits.getSearchQuery().addExact("category",category.getId());
			hits.invalidate();
			hits = archive.getAssetSearcher().cachedSearch(inReq, hits.getSearchQuery());
		}
		hits.getSearchQuery().setProperty("selectedcategory", category.getId());
	}
	
	public void loadHitsCategory(WebPageRequest inReq) throws Exception
	{
		//Look for a hitsessionid and make sure this category is in there
		MediaArchive archive = getMediaArchive(inReq);
		HitTracker hits = (HitTracker) inReq.getPageValue("hits");
		
		
		String catid = hits.getInput("selectedcategory");
		if(catid != null) {
			Category category = archive.getCategory(catid);
			inReq.putPageValue("category", category);
		}
		
		inReq.setRequestParameter("nodeID", catid);

	}
	
	
	/**
	 * @param inPageRequest
	 * @throws Exception
	 */
	
	public void searchCategories(WebPageRequest inPageRequest) throws Exception
	{
		MediaArchive archive = getMediaArchive(inPageRequest);
		Category category = archive.getCategory(inPageRequest);
		if (category == null) {
			return;
		}
		inPageRequest.putPageValue("category",category);
		inPageRequest.putPageValue("selectedcategory",category);
		
		String exact = inPageRequest.findValue("exact-search");
		HitTracker tracker = null;
		
		
		String searchtype = resolveSearchType(inPageRequest);
		if( searchtype == null)
		{
			searchtype = "asset";
		}
		
		Searcher assetsearcher = archive.getSearcher(searchtype);
		SearchQuery search = assetsearcher.addStandardSearchTerms(inPageRequest);
		
		if(search == null) {
			search = assetsearcher.createSearchQuery();
		}
		
		if( exact != null && Boolean.parseBoolean(exact))
		{
			search.addExact("category-exact",category.getId());
		}
		else
		{
			search.addExact("category",category.getId());
		}
		
		if( search.getHitsName() == null)
		{
			String hitsname = inPageRequest.getRequestParameter("hitsname");
			if(hitsname == null)
			{
				hitsname = inPageRequest.findValue("hitsname");
			}
			if (hitsname != null )
			{
				search.setHitsName(hitsname);
			}
		}
			
		
		tracker = assetsearcher.cachedSearch(inPageRequest, search);

		if( tracker != null)
		{
				//TODO: Seems like this could be done within the searcher or something
				tracker.setDataSource(archive.getCatalogId() + "/categories/" + category.getId());
				Data librarycol = (Data) inPageRequest.getPageValue("librarycol");
				if(librarycol != null){
					tracker.getSearchQuery().setProperty("collectionid", librarycol.getId());
				}
				tracker.getSearchQuery().setProperty("categoryid", category.getId());
				//tracker.setPage(1);  //<---why?

		}
			
		UserProfile prefs = (UserProfile)inPageRequest.getUserProfile();
		if( prefs != null)
		{
			prefs.setProperty("lastcatalog", archive.getCatalogId());
			//prefs.save();
		}
		if(category != null && tracker != null) 
		{
			tracker.getSearchQuery().setProperty("selectedcategory", category.getId());
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
	
	
	public HitTracker showFaceProfileGroupAssets(WebPageRequest inReq){
		String faceprofilegroupid = inReq.findValue("faceprofilegroupid");
		
		MediaArchive archive = getMediaArchive(inReq);
		HitTracker tracker = archive.query("asset").exact("faceprofiles.faceprofilegroup", faceprofilegroupid).sort("assetaddeddate").search(inReq);
		return tracker;

	}

	public void loadCategory(WebPageRequest inPageRequest) throws Exception
	{
		MediaArchive archive = getMediaArchive(inPageRequest);
		Category category = archive.getCategory(inPageRequest);
		if (category == null) {
			return;
		}
		inPageRequest.putPageValue("category",category);

	}

	public void searchProfiles(WebPageRequest inPageRequest) throws Exception
	{
		Data person = (Data)inPageRequest.getPageValue("entity");
		if( person == null)
		{
			log.info("No entity");
			return;
		}
		MediaArchive archive = getMediaArchive(inPageRequest);
		FaceProfileManager manager = (FaceProfileManager)archive.getBean("faceProfileManager");
		Collection assets = manager.findAssetsForPerson(person,1000);
		inPageRequest.putPageValue("faceassets", assets);


	}
	
	public void searchProfileAssets(WebPageRequest inPageRequest) throws Exception
	{
		String faceprofileid = (String)inPageRequest.getPageValue("entityid");
		if( faceprofileid == null)
		{
			faceprofileid = (String)inPageRequest.findValue("entityid");
		}
		if( faceprofileid == null)
		{
			log.info("No Face profile");
			return;
		}
		MediaArchive archive = getMediaArchive(inPageRequest);
		FaceProfileManager manager = (FaceProfileManager)archive.getBean("faceProfileManager");
		Collection assets = manager.findAssetsForProfile(faceprofileid,1000);
		inPageRequest.putPageValue("faceassets", assets);


	}

	
}