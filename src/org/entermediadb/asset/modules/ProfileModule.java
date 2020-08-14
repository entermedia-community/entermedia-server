package org.entermediadb.asset.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.users.UserProfileManager;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
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
		String catalogid = inReq.findValue("catalogid");
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
			profilelocation = inReq.findValue("catalogid");
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
		String searchtype = inReq.getRequestParameter("searchtype");
		String view = inReq.getRequestParameter("viewid");
		if( searchtype == null || "asset".equals(searchtype))
		{
			searchtype = "asset";
		}
		if( view == null && "asset".equals(searchtype))
		{
			view = "resultstable";
		} 
		String viewpath = searchtype + "/" + view; 
		
		List details = archive.getAssetSearcher().getDetailsForView(viewpath, inReq.getUserProfile());
		
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
				if (i < target)
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
		inReq.getUserProfile().setValues("view_" + searchtype + "_" + view, ids);
		//System.out.println(ids);
		getUserProfileManager().saveUserProfile(inReq.getUserProfile());
	}

	public void addRemoveColumn(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		UserProfile userProfile = inReq.getUserProfile();
		
		String searchtype = inReq.getRequestParameter("searchtype");
		String view = inReq.getRequestParameter("viewid");
		if( searchtype == null || "asset".equals(searchtype))
		{
			searchtype = "asset";
		}
		if( view == null && "asset".equals(searchtype))
		{
			view = "resultstable";
		} 
		String viewpath = searchtype + "/" + view; 

		String add = inReq.getRequestParameter("addcolumn");
		if (add != null)
		{
			List details = archive.getAssetSearcher().getDetailsForView(viewpath, userProfile);
			boolean exists = false;
			if( details != null)
			{
				for (Iterator iterator = details.iterator(); iterator.hasNext();)
				{
					PropertyDetail detail = (PropertyDetail) iterator.next();
					if (add.equals(detail.getId()))
					{
						exists = true;
						break;
					}
				}
			}
			if (!exists)
			{
				// add it
				Collection ids = new ArrayList();
				for (Iterator iterator = details.iterator(); iterator.hasNext();)
				{
					PropertyDetail detail = (PropertyDetail) iterator.next();
					ids.add(detail.getId());
				}
				ids.add(add);
				userProfile.setValues("view_" + searchtype + "_" + view, ids);
				getUserProfileManager().saveUserProfile(userProfile);
			}
		}

		String remove = inReq.getRequestParameter("removecolumn");
		if (remove != null)
		{
			List details = archive.getAssetSearcher().getDetailsForView(viewpath, userProfile);
			Collection ids = new ArrayList();
			for (Iterator iterator = details.iterator(); iterator.hasNext();)
			{
				PropertyDetail detail = (PropertyDetail) iterator.next();
				if (!remove.equals(detail.getId()))
				{
					ids.add(detail.getId());
				}
			}
			userProfile.setValues("view_" + searchtype + "_" + view, ids);
			getUserProfileManager().saveUserProfile(userProfile);
		}
	}

	public void setView(WebPageRequest inReq) throws Exception
	{
		String view = inReq.getRequestParameter("view");

		UserProfile userProfile = inReq.getUserProfile();
		String[] fields = inReq.getRequestParameters("field");
		userProfile.setValues("view_" + view.replace('/', '_'), Arrays.asList(fields));
		userProfile.save(inReq.getUser());
	}

	public void addFieldsToView(WebPageRequest inReq) throws Exception
	{
		String view = inReq.getRequestParameter("view");

		UserProfile userProfile = inReq.getUserProfile();
		String[] fields = inReq.getRequestParameters("field");

		String viewkey = "view_" + view.replace('/', '_');

		initList(inReq, view, userProfile, viewkey);

		for (int i = 0; i < fields.length; i++)
		{
			userProfile.addValue(viewkey, fields[i]);
		}

		userProfile.save(inReq.getUser());
	}
	/**
	 * This is only called once 
	 * @param inReq
	 * @param view
	 * @param userProfile
	 * @param viewkey
	 */
	protected void initList(WebPageRequest inReq, String view, UserProfile userProfile, String viewkey)
	{
		String value = userProfile.get(viewkey);
		if (value == null)
		{
			String type = inReq.findValue("searchtype"); 
			if (type == null)
			{
				type = "asset";
			}
			Searcher searcher = getSearcherManager().getSearcher(inReq.findValue("catalogid"), type);
			List<PropertyDetail> details = searcher.getDetailsForView(view, userProfile);
			userProfile.setValuesFromDetails(viewkey, details);
		}
	}

	public void removeFieldsFromView(WebPageRequest inReq) throws Exception
	{
		String view = inReq.getRequestParameter("view");

		UserProfile userProfile = inReq.getUserProfile();
		String[] fields = inReq.getRequestParameters("field");

		String viewkey = "view_" + view.replace('/', '_');

		//Check for null starting condition
		initList(inReq, view, userProfile, viewkey);

		String searchtype = inReq.getRequestParameter("searchtype");
		
		for (int i = 0; i < fields.length; i++)
		{
			userProfile.removeValue(viewkey, fields[i]);
			if( searchtype != null)
			{
				userProfile.removeValue(viewkey, searchtype +"." + fields[i]);
			}
		}
		String values = userProfile.get(viewkey);
		if(values == null || values.trim().length() == 0)
		{
			userProfile.setProperty(viewkey, null);
			initList(inReq, view, userProfile, viewkey);
		}
		
		userProfile.save(inReq.getUser());
	}

	public void changeResultView(WebPageRequest inReq)
	{
		UserProfile userProfile = inReq.getUserProfile();
		String resultviewtype = inReq.getRequestParameter("resultviewtype");
		if (resultviewtype == null) 
		{
			resultviewtype = "resultview";
		}
		String changerequest = inReq.getRequestParameter("resultview");
		if (changerequest != null )
		{
			userProfile.setProperty(resultviewtype, changerequest);
			userProfile.save();
		}
		HitTracker hits = (HitTracker) inReq.getPageValue("hits");
		if (hits == null)
		{
			hits = (HitTracker) inReq.getPageValue("albumitems");
		}
		inReq.putPageValue("hits", hits);
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
		String field = inReq.getRequestParameter("profilepreference");
		if (field == null)
		{
			return;
		}
		UserProfile prof = loadUserProfile(inReq);
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
	public void saveProperties(WebPageRequest inReq)
	{
		String[] fields = inReq.getRequestParameters("propertyfield");
		UserProfile prof = loadUserProfile(inReq);

		if (fields == null)
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

		Searcher profilesearcher = getSearcherManager().getSearcher(prof.getCatalogId(), "userprofile");

		profilesearcher.updateData(inReq, fields, prof);
		getUserProfileManager().saveUserProfile(prof);

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

		String sid = inReq.getRequestParameter("hitssessionid");
		if (sid != null)
		{
			HitTracker hits = (HitTracker) inReq.getSessionValue(sid);

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
/*
	public void updateIndex(WebPageRequest inReq)
	{
		// This is used if we've created a user profile but an associated user
		// account does not yet exist.
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

		MediaArchive archive = getMediaArchive(inReq);
		Searcher upsearcher = (Searcher) archive.getSearcher("userprofile");
		Data up = (Data) upsearcher.searchById(username);
		if (up != null)
		{
			upsearcher.updateIndex(up);
		}
	}
*/
	public void addRemoveColumnModule(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);

		UserProfile userProfile = inReq.getUserProfile();
		String searchtype = inReq.findValue("searchtype");
		String view = "";
		if (inReq.findValue("viewid") != null)
		{
			view = inReq.findValue("viewid");
		}
		else 
		{
			view = searchtype + "resultstable";
		}
		String viewpath = searchtype + "/" + view;

		String add = inReq.getRequestParameter("addcolumn");
		if (add != null)
		{
			List details = archive.getSearcher(searchtype).getDetailsForView(viewpath, userProfile);
			boolean exists = false;
			if (details != null)
			{
				for (Iterator iterator = details.iterator(); iterator.hasNext();)
				{
					PropertyDetail detail = (PropertyDetail) iterator.next();
					if (add.equals(detail.getId()))
					{
						exists = true;
						break;
					}
				}
			}
			if (!exists)
			{
				// add it
				Collection ids = new ArrayList();
				if (details != null)
				{
					for (Iterator iterator = details.iterator(); iterator.hasNext();)
					{
						PropertyDetail detail = (PropertyDetail) iterator.next();
						ids.add(detail.getId());
					}
				}
				ids.add(add);
				userProfile.setValues("view_" + searchtype + "_" + view, ids);
				getUserProfileManager().saveUserProfile(userProfile);
			}
		}

		String remove = inReq.getRequestParameter("removecolumn");
		if (remove != null)
		{
			List details = archive.getSearcher(searchtype).getDetailsForView(viewpath, userProfile);
			Collection ids = new ArrayList();
			for (Iterator iterator = details.iterator(); iterator.hasNext();)
			{
				PropertyDetail detail = (PropertyDetail) iterator.next();
				if (!remove.equals(detail.getId()))
				{
					ids.add(detail.getId());
				}
			}
			userProfile.setValues("view_" + searchtype + "_" + view, ids);
			getUserProfileManager().saveUserProfile(userProfile);
		}
	}
	
	
	public void clearProfile(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		String id = inReq.getRequestParameter("userid");
		getUserProfileManager().clearProfile(archive.getCatalogId(), id);
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
	
}
