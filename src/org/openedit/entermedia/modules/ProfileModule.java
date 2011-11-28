package org.openedit.entermedia.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.soap.Detail;

import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.entermedia.MediaArchive;
import org.openedit.profile.UserProfile;
import org.openedit.profile.UserProfileManager;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.users.User;

public class ProfileModule extends MediaArchiveModule
{
	protected UserProfileManager fieldUserProfileManager;

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
		loadUserProfile(inReq);
	}
	public UserProfile loadUserProfile(WebPageRequest inReq)
	{
		User user = inReq.getUser();
		if( user == null || user.getId() == null || user.getId().equals("null"))
		{
			return null;
		}
		String profilelocation = inReq.findValue("profilemanagerid");// catalogid
		if (profilelocation == null)
		{
			profilelocation = inReq.findValue("catalogid");
		}
		if( profilelocation == null)
		{
			profilelocation  = inReq.findValue("applicationid");
		}
		return getUserProfileManager().loadUserProfile(inReq, profilelocation, user.getId());
	}

	public void moveColumn(WebPageRequest inReq) throws Exception
	{
		String source = inReq.getRequestParameter("source");
		String dest = inReq.getRequestParameter("destination");
		
		//Collection values = inReq.getUserProfile().getValues("view_assets_tableresults");
		MediaArchive archive = getMediaArchive(inReq);
		List details = archive.getAssetSearcher().getDetailsForView("asset/resultstable",inReq.getUserProfile());
		
		int target = details.size();
		
		for (int i = 0; i < details.size(); i++)
		{
			PropertyDetail detail = (PropertyDetail) details.get(i);
			if( detail.getId().equals(dest))
			{
				target = i;
				break;
			}
		}
		for (int i = 0; i < details.size(); i++)
		{
			PropertyDetail detail = (PropertyDetail) details.get(i);
			if( detail.getId().equals(source))
			{
				details.add(target, detail);
				if( i > target)
				{
					i++; //there are two now
				}
				details.remove(i);
				break;
			}
		}
		Collection ids = new ArrayList();
		for (Iterator iterator = details.iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();
			ids.add(detail.getId());
		}
		inReq.getUserProfile().setValues("view_asset_resultstable", ids);		
		getUserProfileManager().saveUserProfile(inReq.getUserProfile());
	}

	public void addRemoveColumn(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);

		UserProfile userProfile = inReq.getUserProfile();

		String add = inReq.getRequestParameter("addcolumn");
		if( add != null)
		{
			List details = archive.getAssetSearcher().getDetailsForView("asset/resultstable",userProfile);
			boolean exists = false;
			for (Iterator iterator = details.iterator(); iterator.hasNext();)
			{
				PropertyDetail detail = (PropertyDetail) iterator.next();
				if( add.equals( detail.getId() ) )
				{
					exists = true;
					break;
				}
			}
			if( !exists)
			{
				//add it
				Collection ids = new ArrayList();
				for (Iterator iterator = details.iterator(); iterator.hasNext();)
				{
					PropertyDetail detail = (PropertyDetail) iterator.next();
					ids.add(detail.getId());
				}
				ids.add(add);
				userProfile.setValues("view_asset_resultstable", ids);
				getUserProfileManager().saveUserProfile(userProfile);
			}
		}
		
		String remove = inReq.getRequestParameter("removecolumn");
		if( remove != null)
		{
			List details = archive.getAssetSearcher().getDetailsForView("asset/resultstable",userProfile);
			Collection ids = new ArrayList();
			for (Iterator iterator = details.iterator(); iterator.hasNext();)
			{
				PropertyDetail detail = (PropertyDetail) iterator.next();
				if( !remove.equals(detail.getId() ) )
				{
					ids.add(detail.getId());
				}
			}
			userProfile.setValues("view_asset_resultstable", ids);
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

	protected void initList(WebPageRequest inReq, String view, UserProfile userProfile, String viewkey)
	{
		String value = userProfile.getValue(viewkey);
		if( value == null)
		{
			String type = inReq.findValue("searchtype");
			if( type == null)
			{
				type = "asset";
			}
			Searcher searcher = getSearcherManager().getSearcher(inReq.findValue("catalogid"), type);
			List<PropertyDetail> details = searcher.getDetailsForView(view,userProfile);
			userProfile.setValuesFromDetails(viewkey, details);
		}
	}
	
	public void removeFieldsFromView(WebPageRequest inReq) throws Exception
	{
		String view = inReq.getRequestParameter("view");
		
		UserProfile userProfile = inReq.getUserProfile();
		String[] fields = inReq.getRequestParameters("field");
		
		String viewkey = "view_" + view.replace('/', '_');
		
		initList(inReq, view, userProfile, viewkey);

		for (int i = 0; i < fields.length; i++)
		{
			userProfile.removeValue(viewkey, fields[i]);	
		}
		
		userProfile.save(inReq.getUser());
	}
	
	public void changeResultView(WebPageRequest inReq)
	{
		UserProfile userProfile = inReq.getUserProfile();
		String resultview = userProfile.getValue("resultview");
		if (resultview == null || resultview.equalsIgnoreCase("table"))
		{
			userProfile.setProperty("resultview", "gallery");
		}
		else
		{
			userProfile.setProperty("resultview", "table");
		}
		HitTracker hits = (HitTracker)inReq.getPageValue("hits");
		if( hits == null)
		{
			hits = (HitTracker)inReq.getPageValue("albumitems");
			inReq.putPageValue("hits", hits);
		}

	}
	public void saveValues( WebPageRequest inReq) throws Exception
	{
		String[] fields = inReq.getRequestParameters("profilesearchfilters");
		if( fields != null)
		{
			UserProfile profile = inReq.getUserProfile();
			for (int i = 0; i < fields.length; i++) 
			{
				String[] values = inReq.getRequestParameters(fields[i]);
				List list = new ArrayList();
				if( values != null)
				{
					list.addAll(Arrays.asList(values));
				}
				profile.setValues(fields[i], list);
			}
			profile.save(inReq.getUser());
		}

	}
	
	public void saveProperties(WebPageRequest inReq)
	{
		UserProfile prof = loadUserProfile(inReq);
		
		Searcher profilesearcher = getSearcherManager().getSearcher(prof.getCatalogId(), "userprofile");
		String[] fields = inReq.getRequestParameters("field");
		if(fields == null)
		{
			return;
		}
		profilesearcher.updateData(inReq, fields, prof);
		profilesearcher.saveData(prof, inReq.getUser());
	}
	
	public void toggleUserPreference(WebPageRequest inReq)
	{
		UserProfile prof = loadUserProfile(inReq);
		String field = inReq.getRequestParameter("field");
		if(field == null)
		{
			return;
		}
		Boolean val = Boolean.parseBoolean(prof.get(field));
		if(val)
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
		//View
		String oldresulttype = inReq.getRequestParameter("oldresulttype");
		
		for(int i =0; i<resulttypes.length;i++)
		{
			if(newsettings != null)
			{
				pref.setResultViewPreference(resulttypes[i], newsettings[i]);
			}
			if(sortbys != null)
			{
				pref.setSortForSearchType(resulttypes[i], sortbys[i]);
			}
			if(hitsperpage != null)
			{
				int hpp = Integer.parseInt(hitsperpage[i]);
				pref.setHitsPerPageForSearchType(resulttypes[i], hpp);
			}
		}
		
		String sid = inReq.getRequestParameter("hitssessionid");
		if( sid != null)
		{
			HitTracker hits = (HitTracker)inReq.getSessionValue(sid);
			
			if( hits != null)
			{
				String currentview = hits.getResultType();
				//TODO: maybe these should all be re-loaded in velocity?
				hits.getSearchQuery().setSortBy(pref.getSortForSearchType(currentview));
				hits.setHitsPerPage(pref.getHitsPerPageForSearchType(currentview));
				hits.setIndexId(String.valueOf(System.currentTimeMillis()));
				Searcher searcher = getSearcherManager().getSearcher(hits.getCatalogId(), "asset");
				searcher.cachedSearch(inReq, hits.getSearchQuery());
			}
		}
	}
}
