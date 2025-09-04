package org.entermediadb.asset.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.data.ViewData;
import org.entermediadb.users.UserProfileManager;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;


public class ProfileModule extends MediaArchiveModule
{
	protected UserProfileManager fieldUserProfileManager;
	private static final Log log = LogFactory.getLog(ProfileModule.class);

	public UserProfileManager getUserProfileManager()
	{
		return fieldUserProfileManager;
	}

	public void setUserProfileManager(UserProfileManager inUserProfileManager)
	{
		fieldUserProfileManager = inUserProfileManager;
	}

	public void loadUserProfileManager(WebPageRequest inReq)
	{
		inReq.putPageValue("profileManager", getUserProfileManager());
	}

	public void reLoadUserProfile(WebPageRequest inReq)
	{
		inReq.setRequestParameter("reloadprofile", "true");

		// TODO: Find a way to clear all the search session ids
		String catalogid = inReq.findPathValue("catalogid");
		inReq.removeSessionValue("hitsasset" + catalogid); // hitsassetmedia/catalogs/public

		loadUserProfile(inReq);
	}

	public UserProfile loadUserProfile(WebPageRequest inReq)
	{
		User user = inReq.getUser();
		String userid = null;
		if (user != null && user.getId() != null && !user.getId().equals("null") && !user.isVirtual())
		{
			userid = user.getId();
		}

		String profilelocation = inReq.findValue("profilemanagerid");// catalogid
		if (profilelocation == null)
		{
			profilelocation = inReq.findPathValue("catalogid");
		}
		if (profilelocation == null)
		{
			profilelocation = inReq.findValue("applicationid");
		}
		return getUserProfileManager().loadUserProfile(inReq, profilelocation, userid);
	}

	public void moveColumn(WebPageRequest inReq) throws Exception
	{
		String source = inReq.getRequestParameter("source");
		String dest = inReq.getRequestParameter("destination");

		// Collection values =
		// inReq.getUserProfile().getValues("view_assets_tableresults");
		MediaArchive archive = getMediaArchive(inReq);
		String searchtype = inReq.findPathValue("searchtype");
		String viewid = inReq.getRequestParameter("viewid");
		if( searchtype == null || "asset".equals(searchtype))
		{
			searchtype = "asset";
		}
		if( viewid == null && "asset".equals(searchtype))
		{
			viewid = "resultstable";
		}
		else if( viewid == null)
		{
			viewid = searchtype+"resultstable";
		}
		String viewpath = searchtype + "/" + viewid; 
		Data viewdata = archive.getCachedData("view", viewid);

		List details = archive.getAssetSearcher().getDetailsForView(viewdata, inReq.getUserProfile());
		
		if(details == null) {
			return;
		}

		int target = details.size();
		PropertyDetail detail = null;
		for (int i = 0; i < details.size(); i++)
		{
			detail = (PropertyDetail) details.get(i);
			if (detail.getId().equals(dest))
			{
				target = i;
				break;
			}
		}
		
		for (int i = 0; i < details.size(); i++)
		{
			detail = (PropertyDetail) details.get(i);
			if (detail.getId().equals(source))
			{
				if (i < target && (target+1 <= details.size()))
				{
					details.add(target+1, detail);
					details.remove(i);
				}
				else {
					details.add(target, detail);
					details.remove(i+1);
				}
				//
				break;
			}
		}
		Collection ids = new ArrayList();
		for (Iterator iterator = details.iterator(); iterator.hasNext();)
		{
			detail = (PropertyDetail) iterator.next();
			ids.add(detail.getId());
		}
		inReq.getUserProfile().setValues("view_" + viewid, ids);
		//System.out.println(ids);
		getUserProfileManager().saveUserProfile(inReq.getUserProfile());
	}



	public void changeResultView(WebPageRequest inReq)
	{
		UserProfile userProfile = inReq.getUserProfile();
		String moduleid = inReq.findPathValue("module");
		String changerequest = inReq.getRequestParameter("changeresultview");

		if (changerequest != null )
		{
			inReq.setRequestParameter("resultview" + moduleid,changerequest);
			String type = moduleid + "resultview";
			userProfile.setProperty(type, changerequest);
			//userProfile.save();
			
			getUserProfileManager().saveUserProfile(userProfile);
		}
		HitTracker hits = (HitTracker) inReq.getPageValue("hits");
		if (hits == null)
		{
			hits = (HitTracker) inReq.getPageValue("albumitems");
		}
		inReq.putPageValue("hits", hits);
		
			//hits per page custom for module or userprofile
			if(moduleid != null) {
				String customhitsperpage = userProfile.get(moduleid+changerequest+"hitsperpage");
				if (customhitsperpage == null) {
					customhitsperpage = userProfile.get(moduleid+"hitsperpage");
				}
				if (customhitsperpage != null) {
					if (hits != null) {
						hits.setHitsPerPage(Integer.parseInt(customhitsperpage));
					}
					inReq.putPageValue(moduleid+"hitsperpage", Integer.parseInt(customhitsperpage));
				}
				
			}
			else {
				//inReq.putPageValue("hitsperpage", Integer.parseInt(customhitsperpage));
			}
			//Search userprofile first
			
		
	}
	
	public void changeHitsPerPage(WebPageRequest inReq)
	{
		String hitsperpage = inReq.getRequestParameter("hitsperpage");
		if (StringUtils.isNumeric(hitsperpage)) {
			UserProfile userProfile = inReq.getUserProfile();
			if (hitsperpage == null) 
			{
				hitsperpage = "15";
			}
			//custom for each resulttype
			String moduleid = inReq.findPathValue("module");
			
			String resultview = inReq.getRequestParameter("resultview");
			if(resultview != null && !"stackedgallery".equals(resultview)) {
				userProfile.setProperty(moduleid+resultview+"hitsperpage", hitsperpage);
			}
			else {
				if(moduleid != null) {
					userProfile.setProperty(moduleid+"hitsperpage", hitsperpage);
				}
				else {
					//userProfile.setProperty("assethitsperpage", hitsperpage);
				}
			}
			userProfile.save();

			//Save value to hitsperpage "hitcount"
			Data data;
			MediaArchive archive = getMediaArchive(inReq);
			Searcher searcher = archive.getSearcher("hitcount");
			data = (Data) searcher.searchById(hitsperpage);
			if(data == null) {
				data = searcher.createNewData();
				data.setId(hitsperpage);
				data.setName(hitsperpage);
				searcher.saveData(data, null);
			}
		}
		/*
		HitTracker hits = (HitTracker) inReq.getPageValue("hits");
		if (hits == null)
		{
			hits = (HitTracker) inReq.getPageValue("albumitems");
		}
		inReq.putPageValue("hits", hits);*/
	}

	public void saveValues(WebPageRequest inReq) throws Exception
	{
		String[] fields = inReq.getRequestParameters("profilesearchfilters");
		if (fields != null)
		{
			UserProfile profile = inReq.getUserProfile();
			for (int i = 0; i < fields.length; i++)
			{
				String[] values = inReq.getRequestParameters(fields[i]);
				List list = new ArrayList();
				if (values != null)
				{
					list.addAll(Arrays.asList(values));
				}
				profile.setValues(fields[i], list);
			}
			profile.save(inReq.getUser());
		}

	}

	public void savePreference(WebPageRequest inReq)
	{
		UserProfile prof = loadUserProfile(inReq);

		//Old style?
		String field = inReq.getRequestParameter("propertyfield");
		if (prof != null && field != null)
		{
			String value = inReq.getRequestParameter( "property.value");
			if( value == null)
			{
				value = inReq.getRequestParameter( "propertyvalue");
			}
			prof.setValue(field, value);
			getUserProfileManager().saveUserProfile(prof);
		}

		field = inReq.getRequestParameter("profilepreference");
		if (field == null)
		{
			return;
		}
		String value = inReq.getRequestParameter("profilepreference.value");
		
		String oldval = prof.get(field);
		if( oldval == value || (oldval != null && oldval.equals(value)) )
		{
			return;
		}
		prof.setProperty(field, value);
		getUserProfileManager().saveUserProfile(prof);
	}
	public void addPreferenceValue(WebPageRequest inReq)
	{
		String field = inReq.getRequestParameter("profilepreference");
		if (field == null)
		{
			return;
		}
		UserProfile prof = loadUserProfile(inReq);
		String value = inReq.getRequestParameter("profilepreference.value");
		if( value == null)
		{
			return;
		}
		Collection values = prof.getValues(field);
		if( values != null && values.contains(value) )
		{
			return;
		}
		prof.addValue(field, value);
		getUserProfileManager().saveUserProfile(prof);
	}
	public void removePreferenceValue(WebPageRequest inReq)
	{
		String field = inReq.getRequestParameter("profilepreference");
		if (field == null)
		{
			return;
		}
		UserProfile prof = loadUserProfile(inReq);
		String value = inReq.getRequestParameter("profilepreference.value");
		if( value == null)
		{
			return;
		}
		Collection values = prof.getValues(field);
		if( values != null && !values.contains(value) )
		{
			return;
		}
		prof.removeValue(field, value);
		getUserProfileManager().saveUserProfile(prof);
	}
	
	/**
	 * @deprecated This is too weird. Delete it
	 * */
	public void saveProperties(WebPageRequest inReq)
	{
		String[] fields = inReq.getRequestParameters("propertyfield");
		UserProfile prof = loadUserProfile(inReq);

		if (prof != null && fields == null)
		{
			String field = inReq.getCurrentAction().getChildValue("propertyfield");
			String value = inReq.getCurrentAction().getChildValue(field + ".value");
			String oldval = prof.get(field);
			if( oldval == value || (oldval != null && oldval.equals(value)) )
			{
				return;
			}

			if( field != null && value != null)
			{
				inReq.setRequestParameter("propertyfield", field );
				inReq.setRequestParameter(field + ".value", value );
				fields = inReq.getRequestParameters("propertyfield");
			}
			else
			{
				return;
			}

		}
		if(prof != null) 
		{
			Searcher profilesearcher = getSearcherManager().getSearcher(prof.getCatalogId(), "userprofile");
	
			profilesearcher.updateData(inReq, fields, prof);
			getUserProfileManager().saveUserProfile(prof);
		}
		
	}

	public void toggleUserPreference(WebPageRequest inReq)
	{
		UserProfile prof = loadUserProfile(inReq);
		String field = inReq.getRequestParameter("field");
		if (field == null)
		{
			return;
		}
		Boolean val = Boolean.parseBoolean(prof.get(field));
		if (val)
		{
			prof.setProperty(field, "false");
		}
		else
		{
			prof.setProperty(field, "true");
		}
		getUserProfileManager().saveUserProfile(prof);
	}

	public void addOrRemoveUserPreference(WebPageRequest inReq)
	{
		UserProfile prof = loadUserProfile(inReq);
		String field = inReq.getRequestParameter("field");
		if (field == null)
		{
			return;
		}
		String value = inReq.getRequestParameter("profilepreference.value");
		if( value == null)
		{
			return;
		}
		if( prof.containsValue(field, value) )
		{
			prof.removeValue(field, value);
		}
		else
		{
			prof.addValue(field, value);
		}
		getUserProfileManager().saveUserProfile(prof);
		inReq.putPageValue("userprofile",prof);
	}

	public void saveResultPreferences(WebPageRequest inReq) throws Exception
	{
		UserProfile pref = loadUserProfile(inReq);

		String[] resulttypes = inReq.getRequestParameters("resulttype");
		String[] newsettings = inReq.getRequestParameters("newresultview");
		String[] sortbys = inReq.getRequestParameters("sortby");
		String[] hitsperpage = inReq.getRequestParameters("hitsperpage");
		// View
		String oldresulttype = inReq.getRequestParameter("oldresulttype");

		for (int i = 0; i < resulttypes.length; i++)
		{
			if (newsettings != null)
			{
				pref.setResultViewPreference(resulttypes[i], newsettings[i]);
			}
			if (sortbys != null)
			{
				pref.setSortForSearchType(resulttypes[i], sortbys[i]);
			}
			if (hitsperpage != null)
			{
				int hpp = Integer.parseInt(hitsperpage[i]);
				pref.setHitsPerPageForSearchType(resulttypes[i], hpp);
			}
		}

		String moduleid = inReq.findPathValue("module");
		HitTracker hits = loadHitTracker(inReq, moduleid);

		if (hits != null)
		{
			String currentview = hits.getResultType();
			// TODO: maybe these should all be re-loaded in velocity?
			hits.getSearchQuery().setSortBy(pref.getSortForSearchType(currentview));
			hits.setHitsPerPage(pref.getHitsPerPageForSearchType(currentview));
			hits.setIndexId(String.valueOf(System.currentTimeMillis()));
			Searcher searcher = getSearcherManager().getSearcher(hits.getCatalogId(), "asset");
			searcher.cachedSearch(inReq, hits.getSearchQuery());
		}
		
	}

	public void checkUserAccount(WebPageRequest inReq)
	{
		// This is used if we've created a user profile but an associated user
		// account does not yet exist.
		boolean save = Boolean.parseBoolean(inReq.getRequestParameter("save"));
		if (!save)
		{
			return;
		}
		String username = inReq.getRequestParameter("userid");
		if (username == null)
		{
			username = inReq.getRequestParameter("username");
		}
		if (username == null)
		{
			username = inReq.getRequestParameter("id");
		}
		if (username == null)
		{
			return;
		}
		String password = inReq.getRequestParameter("password");
		User user = getUserManager(inReq).getUser(username);
		if (user == null)
		{
			user = getUserManager(inReq).createUser(username, password);

		}

	}

	
	public void clearProfile(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		String id = inReq.getRequestParameter("userid");
		getUserProfileManager().clearProfile(archive.getCatalogId(), id);
	}

	public void clearProfiles(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		getUserProfileManager().clearProfiles(archive.getCatalogId());
	}


	public void saveProperty(WebPageRequest inReq)
	{
		UserProfile prof = loadUserProfile(inReq);

		String field = inReq.findValue("field");
		String value = inReq.findValue(field + ".value");
		String oldval = prof.get(field);
		if( oldval == value || (oldval != null && oldval.equals(value)) )
		{
			return;
		}

		prof.setValue(field,value);
		try
		{
			getUserProfileManager().saveUserProfile(prof);
		}
		catch( Exception ex)
		{
			log.error("Could not save ", ex);
		}
		inReq.putPageValue("profile",prof);
	}
	
	public void updateLocation(WebPageRequest inReq) 
	{
		UserProfile prof = loadUserProfile(inReq);
		String geopoint = inReq.getRequestParameter("geo_point");

		String[] locations = geopoint.split(",");
		Map point = new HashMap();
		point.put("lat",locations[0]);
		point.put("long",locations[1]);
		prof.setValue("geo_point", point);
		
		try
		{
			getUserProfileManager().saveUserProfile(prof);
		}
		catch( Exception ex)
		{
			log.error("Could not save ", ex);
		}
		inReq.putPageValue("profile",prof);
	}

//	/**
//	 * This is only called once 
//	 * @param inReq
//	 * @param view
//	 * @param userProfile
//	 * @param viewkey
//	 */
//	protected void initList(WebPageRequest inReq, String view, UserProfile userProfile, String viewkey)
//	{
//		String value = userProfile.get(viewkey);
//		if (value == null)
//		{
//			String type = inReq.findPathValue("searchtype"); 
//			if (type == null)
//			{
//				type = "asset";
//			}
//			Searcher searcher = getSearcherManager().getSearcher(inReq.findPathValue("catalogid"), type);
//			List<PropertyDetail> details = searcher.getDetailsForView(view, userProfile);
//			userProfile.setValuesFromDetails(viewkey, details);
//		}
//	}	

	public void saveView(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		String viewid = inReq.getRequestParameter("viewid");
		
		String saveforall = inReq.getUserProfileValue("view_saveforallenabled");
		String[] sorted = inReq.getRequestParameters("ids");

		String propId = "view_" + viewid;

		if( Boolean.parseBoolean(saveforall) )
		{
			PropertyDetailsArchive detailarchive = archive.getPropertyDetailsArchive();
			
			if (sorted == null) {
				throw new OpenEditException("Missing sort list ids");
			}
			Data viewdata = archive.getCachedData("view", viewid);
			detailarchive.saveView(viewdata, sorted);
			
			archive.getUserProfileManager().clearUserProfileViewValues(archive.getCatalogId(),viewid);
			inReq.getUserProfile().setValue(propId,null);
			return;
		}
		
		UserProfile userProfile = inReq.getUserProfile();
		userProfile.setValues(propId, Arrays.asList(sorted));
		userProfile.save(inReq.getUser());
	}

	
	/**
	 */
	public void removeFieldsFromView(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		//See if we neeed to make it system side
		String viewid= inReq.getRequestParameter("viewid");
		String saveforall = inReq.getUserProfileValue("view_saveforallenabled");
		String detailid = inReq.getRequestParameter("detailid");

		ViewData viewdata = (ViewData)archive.getCachedData("view", viewid);

		if( Boolean.parseBoolean(saveforall) )
		{
			PropertyDetailsArchive detailarchive = archive.getPropertyDetailsArchive();
			detailarchive.removeFromView(viewdata, detailid);
			archive.getUserProfileManager().clearUserProfileViewValues(archive.getCatalogId(),viewid);
			String propId = "view_" + viewid;
			inReq.getUserProfile().setValue(propId,null);

			return;
		}

		UserProfile userProfile = inReq.getUserProfile();

		String viewkey = "view_" + viewid;

		//Check for null starting condition
		//initList(inReq, viewpath, userProfile, viewkey);
		
		Searcher searcher = viewdata.getSearcher();
		List<PropertyDetail> details = searcher.getDetailsForView(viewdata, userProfile);
		List<PropertyDetail> tosave = new ArrayList();
		for (Iterator iterator = details.iterator(); iterator.hasNext();) {
			PropertyDetail propertyDetail = (PropertyDetail) iterator.next();
			if (!propertyDetail.getId().equals(detailid)) {
				tosave.add(propertyDetail);
			}
			
		}
		userProfile.setValuesFromDetails(viewkey, tosave);
		userProfile.save(inReq.getUser());
	}

	public void addFieldsToView(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		String viewid = inReq.getRequestParameter("viewid");

		//See if we neeed to make it system side
		String saveforall = inReq.getUserProfileValue("view_saveforallenabled");
		String detailid = inReq.getRequestParameter("detailid");
		
		String propId = "view_" + viewid;
		Data viewdata = archive.getCachedData("view", viewid);

		if( Boolean.parseBoolean(saveforall) )
		{
			PropertyDetailsArchive detailarchive = archive.getPropertyDetailsArchive();
			
			detailarchive.addToView(viewdata, detailid); //System wide
				
			archive.getUserProfileManager().clearUserProfileViewValues(archive.getCatalogId(),viewid); //clear every user
			inReq.getUserProfile().setValue(propId,null);
			return;
		}
		
		UserProfile userProfile = inReq.getUserProfile();
		
		Collection ids = new ArrayList();  //Maintains the order
		String fieldsearcherid = viewdata.get("rendertable");
		if( fieldsearcherid == null)
		{
			fieldsearcherid = viewdata.get("moduleid");
		}
		Searcher fieldsearcher = archive.getSearcher(fieldsearcherid);

		List details = fieldsearcher.getDetailsForView(viewdata, userProfile);
		boolean exists = false;
		if( details != null)
		{
			for (Iterator iterator = details.iterator(); iterator.hasNext();)
			{
				PropertyDetail detail = (PropertyDetail) iterator.next();
				if (detailid.equals(detail.getId()))
				{
					exists = true;
					break;
				}
			}
		}
		if (!exists)
		{
			// add it to this users profile only
			if(details!=null) {
				for (Iterator iterator = details.iterator(); iterator.hasNext();)
				{
					PropertyDetail detail = (PropertyDetail) iterator.next();
					ids.add(detail.getId());
				}
			}
			ids.add(detailid);
		}
		userProfile.setValues(propId, ids);
		getUserProfileManager().saveUserProfile(userProfile);

	}
	
	
	
}
