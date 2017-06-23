package org.entermediadb.users;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.profile.UserProfile;
import org.openedit.users.Group;
import org.openedit.users.User;
import org.openedit.users.UserManager;

public class UserProfileManager
{
	protected SearcherManager fieldSearcherManager;
	protected ModuleManager	 fieldModuleManager;

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public UserManager getUserManager(String inCatalogId)
	{
		return (UserManager)getModuleManager().getBean(inCatalogId,"userManager");
	}

	public MediaArchive getMediaArchive(String inCatalogId)
	{
		return (MediaArchive)getModuleManager().getBean(inCatalogId,"mediaArchive");
	}

	private static final Log log = LogFactory.getLog(UserProfileManager.class);

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public UserProfile loadUserProfile(WebPageRequest inReq, String inCatalogId, String inUserName)
	{

		if (inUserName == null)
		{
			inUserName = "anonymous";
		}
		String appid = inReq.findValue("applicationid");
		UserProfile userprofile = loadProfile(inReq, inCatalogId, appid, inUserName);

		if( userprofile != null)
		{
			String lastviewedapp = userprofile.get("lastviewedapp");
			if(lastviewedapp == null || !appid.equals(lastviewedapp))
			{
				userprofile.setProperty("lastviewedapp", appid);
				saveUserProfile(userprofile);
			}
		}
		return userprofile;
	}

	protected UserProfile loadProfile(WebPageRequest inReq, String inCatalogId, String appid, String inUserName)
	{
		String id = inCatalogId + "userprofile" + inUserName;

		UserProfile userprofile;
		if (inReq != null)
		{
			boolean reload = Boolean.parseBoolean(inReq.findValue("reloadprofile"));
			userprofile = (UserProfile) inReq.getPageValue("userprofile");
			if (!reload && userprofile != null && inUserName.equals(userprofile.getUserId()) && inCatalogId.equals(userprofile.getCatalogId()))
			{
				return userprofile;
			}
			if (!reload)
			{
				userprofile = (UserProfile) inReq.getSessionValue(id);
			}
			if (!reload && userprofile != null && inUserName.equals(userprofile.getUserId()))
			{
				// check searcher cache?
				inReq.putPageValue("userprofile", userprofile);

				return userprofile;
			}
			if (inCatalogId == null)
			{
				return null;
			}
		}
		Searcher searcher = getSearcherManager().getSearcher(inCatalogId, "userprofile");
		userprofile = (UserProfile) searcher.searchById(inUserName);
		if(userprofile == null)
		{
			userprofile = (UserProfile) searcher.searchByField("userid", inUserName);
		}
		
		if( userprofile != null )
		{
			userprofile.setCatalogId(inCatalogId);
			if( userprofile.get("userid") != null)
			{
				String dataid = userprofile.getId();
				if(!inUserName.equals(dataid))
				{
					searcher.delete(userprofile, null);
					userprofile.setSourcePath(inUserName);
					userprofile.setId(inUserName);
					
				}
				userprofile.setProperty("userid", null);
				searcher.saveData(userprofile, inReq.getUser());
			}
		}
		User user = getUserManager(inCatalogId).getUser(inUserName);
		if (userprofile == null)
		{
			userprofile = (UserProfile) searcher.createNewData();
			userprofile.setId(inUserName);
			if (inUserName.equals("admin"))
			{
				userprofile.setProperty("settingsgroup", "administrator");
			}
			else if( user != null)
			{
				userprofile.setProperty("settingsgroup", "users");
			}
			else
			{
				userprofile.setProperty("settingsgroup", "guest");
			}
			userprofile.setSourcePath(inUserName);
			userprofile.setCatalogId(inCatalogId);
			saveUserProfile(userprofile);
		}
		userprofile.setUser(user);
		userprofile.setSourcePath(inUserName);
		userprofile.setCatalogId(inCatalogId);

//		String preferedapp = userprofile.get("preferedapp");
//		if(preferedapp == null)
//		{
//			userprofile.setValue("preferedapp", appid);
//			saveUserProfile(userprofile);
//		}
		
		inReq.putSessionValue(id, userprofile);
		inReq.putPageValue("userprofile", userprofile);

		List ok = new ArrayList();

		// check the parent first, then the appid
//		String parentid = inReq.findValue("parentapplicationid");
//		Collection catalogs = getSearcherManager().getSearcher(parentid, "catalogs").getAllHits();
//
//		for (Iterator iterator = catalogs.iterator(); iterator.hasNext();)
//		{
//			Data cat = (Data) iterator.next();
//			Boolean canview = inReq.getPageStreamer().canView("/" + cat.getId());
//			if (canview != null && canview)
//			{
//				ok.add(cat);
//			}
//		}
//		userprofile.setCatalogs(new ListHitTracker(ok));
//		userprofile.setUploadCatalogs(new ListHitTracker(ok));



		Collection modules = getSearcherManager().getSearcher(inCatalogId, "module").query().match("id", "*").sort("name").search(inReq);
		List<Data> okmodules = new ArrayList<Data>();
		for (Iterator iterator = modules.iterator(); iterator.hasNext();)
		{
			Data module = (Data) iterator.next();
			// MediaArchive archive = getMediaArchive(cat.getId());
			WebPageRequest catcheck = inReq.getPageStreamer().canDoPermissions("/" + appid + "/views/modules/" + module.getId());
			Boolean canview = (Boolean) catcheck.getPageValue("canview");
			if (canview != null && canview)
			{
				okmodules.add(module);
			}
		}
		userprofile.setModules(okmodules);
		loadLibraries(userprofile, inCatalogId);

		//Why do we do this? Seems like we already check this when we load up the profile above
		//		if (inReq.getUserName().equals(userprofile.getUserId()))
		//		{
		//		}

		//		if (inReq.getUserName().equals(userprofile.getUserId())) {
		//			inReq.putSessionValue(id, userprofile);
		//		}
		return userprofile;
	}

	protected void loadLibraries(UserProfile inUserprofile, String inCatalogId)
	{
		Set<String> all = new HashSet<String>();
		Searcher searcher = getSearcherManager().getSearcher(inCatalogId, "category");

		Collection groupids = new ArrayList();
		if( inUserprofile == null || inUserprofile.getUser() == null)
		{
			groupids.add("anonymous");
		}
		else
		{
			for (Iterator iterator = inUserprofile.getUser().getGroups().iterator(); iterator.hasNext();)
			{
				Group group = (Group) iterator.next();
				groupids.add(group.getId());
			}
		}
		String roleid = null;
		if( inUserprofile.getSettingsGroup() != null)
		{
			roleid = inUserprofile.getSettingsGroup().getId();
		}
		else
		{
			roleid = "anonymous";
		}
		HitTracker categories = searcher.query().or().
			orgroup("viewgroups", groupids).
			match("viewroles", roleid).
			match("viewusers", inUserprofile.getUserId()).search();

		MediaArchive mediaArchive = getMediaArchive(inCatalogId);

		Collection<Category> okcategories = new ArrayList<Category>();
		for (Iterator iterator = categories.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			Category cat = mediaArchive.getCategory(data.getId());
			if( cat != null)
			{
				okcategories.add(cat);
			}
		}
		
		inUserprofile.setViewCategories(okcategories);
		/*
		Collection found = searcher.fieldSearch("userid", inUserprofile.getUserId());

		for (Iterator iterator = found.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			all.add(data.get("_parent"));
		}

		if (!"anonymous".equals(inUserprofile.getUserId()))
		{
			searcher = getSearcherManager().getSearcher(inCatalogId, "librarygroups");

			SearchQuery query = searcher.createSearchQuery();
			StringBuffer groups = new StringBuffer();

			User user = (User) getSearcherManager().getData(inCatalogId, "user", inUserprofile.getUserId());
			if (user != null)
			{
				for (Iterator iterator = user.getGroups().iterator(); iterator.hasNext();)
				{
					Group group = (Group) iterator.next();
					if (group != null)
					{
						groups.append(group.getId());
						if (iterator.hasNext())
						{
							groups.append(" ");
						}
					}
				}
				query.addOrsGroup("groupid", groups.toString());
				found = searcher.search(query);
				for (Iterator iterator = found.iterator(); iterator.hasNext();)
				{
					Data data = (Data) iterator.next();
					all.add(data.get("libraryid"));
				}
			}
		}

		searcher = getSearcherManager().getSearcher(inCatalogId, "libraryroles");
		if (inUserprofile.getSettingsGroup() != null)
		{
			SearchQuery query = searcher.createSearchQuery();
			query.addOrsGroup("roleid", "anonymous " + inUserprofile.getSettingsGroup().getId());
			found = searcher.search(query);
			
			//= searcher.fieldSearch("roleid", inUserprofile.getSettingsGroup().getId());

			for (Iterator iterator = found.iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				all.add(data.get("libraryid"));
			}
		}
		//Add in special adminstrator rights?
		inUserprofile.setViewCategories(all);
	*/	
	}

	public void saveUserProfile(UserProfile inUserProfile)
	{
		if ("anonymous".equals(inUserProfile.getUserId()))
		{
			return;
		}
		Searcher searcher = getSearcherManager().getSearcher(inUserProfile.getCatalogId(), "userprofile");
		if (inUserProfile.getSourcePath() == null)
		{
			throw new OpenEditException("user profile source path is null");
		}
		searcher.saveData(inUserProfile, null);
	}
}
