package org.entermediadb.users;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.locks.Lock;
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
		String appid = inReq.findValue("applicationid");
		if(appid == null)
		{
			return null;
		}
		if (inUserName == null)
		{
			inUserName = "anonymous";
		}
		
		UserProfile userprofile = loadProfile(inReq, inCatalogId, appid, inUserName);

		return userprofile;
	}

	protected UserProfile loadProfile(WebPageRequest inReq, String inCatalogId, String appid, String inUserName)
	{
		MediaArchive mediaArchive = getMediaArchive(inCatalogId);

		boolean forcereload = false;
		
		UserProfile userprofile = null;
		if (inReq != null)
		{
			forcereload = Boolean.parseBoolean( inReq.findValue("reloadprofile"));
			userprofile = (UserProfile) inReq.getPageValue("userprofile");
			if( userprofile == null)
			{
				userprofile = (UserProfile)mediaArchive.getCacheManager().get("userprofile", inUserName);
			}
			if (forcereload == false && userprofile != null)
			{
				String index = mediaArchive.getSearcher("settingsgroup").getIndexId();
				if( index.equals(userprofile.getSettingsGroupIndexId()) )
				{
					inReq.putPageValue("userprofile", userprofile);
					return userprofile;
				}	
			}

			if (inCatalogId == null)
			{
				return null;
			}
		}
		Lock lock = null;
		try
		{
			lock = mediaArchive.getLockManager().lock("userprofileloading/" + inUserName, "UserProfileManager.loadProfile");
			userprofile = (UserProfile)mediaArchive.getCacheManager().get("userprofile", inUserName);
			String index = mediaArchive.getSearcher("settingsgroup").getIndexId();

			if( forcereload == false && userprofile != null && index.equals(userprofile.getSettingsGroupIndexId()) )
			{
				inReq.putPageValue("userprofile", userprofile);
				return userprofile;
			}
			userprofile = loadUserProfile(mediaArchive, appid,inUserName);

			inReq.putPageValue("userprofile", userprofile);
			mediaArchive.getCacheManager().put("userprofile", inUserName,userprofile);
		}
		catch( OpenEditException ex)
		{
			if( userprofile != null)
			{
				inReq.putPageValue("userprofile", userprofile); //Better to grab something than nothing
				log.error("Could not lock user profile table " + inUserName);
			}
		}
		finally
		{
			mediaArchive.releaseLock(lock);
		}

		return userprofile;
	}
	
	/*  TODO: User module query filter to filter the list like we do libraries
	protected void loadModules(UserProfile inProfile)
	{
		//Must be set before we run actions below
		inReq.putPageValue("userprofile", userprofile);

		userprofile.setIndexId( mediaArchive.getSearcher("settingsgroup").getIndexId() );

		log.info("Checking modules for " + inUserName + " catalog:" + inCatalogId);
		List<Data> okmodules = new ArrayList<Data>();
		if( inUserName != null && !inUserName.equals("anonymous"))
		{
			Collection modules = getSearcherManager().getSearcher(inCatalogId, "module").query().match("id", "*").sort("name").search(inReq);
			if( userprofile.isInRole("administrator"))
			{
				okmodules.addAll(modules);
			}
			else
			{
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
			}
		}
		userprofile.setModules(okmodules);
	}
	*/
	public UserProfile loadUserProfile(MediaArchive mediaArchive, String appid,String inUserName)
	{
		String inCatalogId = mediaArchive.getCatalogId();
		UserProfile userprofile;
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
				searcher.saveData(userprofile, null);
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
			else if( user != null && !"anonymous".equals(inUserName))
			{
				String profiletype = mediaArchive.getCatalogSettingValue("defaultrole");
				if(profiletype == null) {
					profiletype = "users";
				}
				userprofile.setProperty("settingsgroup", profiletype);
			}
			else
			{
				userprofile.setProperty("settingsgroup", "guest");
			}
			userprofile.setSourcePath(inUserName);
			userprofile.setCatalogId(inCatalogId);
						
			try
			{
				saveUserProfile(userprofile);
			}
			catch( Exception ex)
			{
				log.error("Error saving " + inUserName ,ex);
			}
		}
		userprofile.setUser(user);
		userprofile.setSourcePath(inUserName);
		userprofile.setCatalogId(inCatalogId);
		loadLibraries(userprofile, inCatalogId);
		
		if( user != null)
		{
			if( userprofile.hasPermission("viewsettings"))
			{
				HitTracker modules  = mediaArchive.query("module").all().
					/*orgroup("viewgroups", user.getGroups()).
					match("viewroles", userprofile.getSettingsGroup().getId()).
					match("viewusers", inUserName)*/
					search();
					///log.info(modules.size() + " for " + modules.getSearchQuery().toQuery());
				userprofile.setModules(modules);
			}
			else
			{
				QueryBuilder builder = mediaArchive.query("module").or().
				exact("securityenabled", false).
				orgroup("viewgroups", user.getGroups()).
				match("viewusers", inUserName);
				if(userprofile.getSettingsGroup() != null)
				{
					builder.match("viewroles", userprofile.getSettingsGroup().getId());
				}
				HitTracker modules  = builder.search();
					//log.info(modules.size() + " for " + modules.getSearchQuery().toQuery());
				userprofile.setModules(modules);
			}
		}
		else
		{
			userprofile.setModules(Collections.EMPTY_LIST);
		}
		String lastviewedapp = userprofile.get("lastviewedapp");
		if(lastviewedapp == null || !appid.equals(lastviewedapp) )
		{
			if( !appid.endsWith("mediadb"))
			{
				userprofile.setProperty("lastviewedapp", appid);
				try
				{
					saveUserProfile(userprofile);
				}
				catch( Exception ex)
				{
					log.error("Error saving " + inUserName ,ex);
				}
			}	
		}
		String index = mediaArchive.getSearcher("settingsgroup").getIndexId();
		userprofile.setSettingsGroupIndexId(index);
		
		return userprofile;
	}

	protected void loadLibraries(UserProfile inUserprofile, String inCatalogId)
	{
		Set<Category> okcategories = findCategoriesForUser(inCatalogId, inUserprofile);

		
		//Load all the collections they have rights to based okcategories + their parents
		//categories+parents
		//lava loop over every collection and mesh 		
		inUserprofile.setViewCategories(okcategories);
		//inUserprofile.setCollectionIds(new ArrayList());
		/*
		if( !inUserprofile.hasPermission("viewsettings"))
		{
			if( !okcategories.isEmpty() )
			{
				Collection<String> okcollectionids = new ArrayList<String>();
				HitTracker found = mediaArchive.query("librarycollection").all().search();
				found.enableBulkOperations();
				for (Iterator iterator = found.iterator(); iterator.hasNext();)
				{
					Data collection = (Data) iterator.next();
					Category root = mediaArchive.getCategory(collection.get("rootcategory"));
					if( root != null && root.hasParentCategory(okcategories) )
					{
						okcollectionids.add(collection.getId());
					}
				}
				inUserprofile.setCollectionIds(okcollectionids);
			}
		}
		*/
	}

	public Set<Category> findCategoriesForUser(String inCatalogId, UserProfile inUserprofile)
	{
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
		//log.info("Searching categories");

		QueryBuilder querybuilder = searcher.query().or()
			.match("ownerid", inUserprofile.getUserId())
			.orgroup("viewgroups", groupids)
			.orgroup("viewonlygroups",groupids)
			.exact("viewonlyroles", roleid)
			.exact("viewroles", roleid)
			.exact("viewusers", inUserprofile.getUserId() );
		
		HitTracker categories = querybuilder.search();

		//log.info("Checking modules found " + categories.size());

		MediaArchive mediaArchive = getMediaArchive(inCatalogId);

		Set<Category> okcategories = new HashSet<Category>();
		for (Iterator iterator = categories.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			Category cat = mediaArchive.getCategory(data.getId());
			if( cat != null)
			{
				okcategories.add(cat);
			}
		}
		
		loadUsers(mediaArchive, inUserprofile, okcategories);
		return okcategories;
	}

	protected void loadUsers(MediaArchive mediaArchive, UserProfile inUserprofile, Set<Category> okcategories)
	{
		Collection editors = mediaArchive.query("librarycollectionusers").
				//exact("ontheteam","true").
				exact("followeruser",inUserprofile.getUserId()).search();
		
		Set collectionids = new HashSet();
		for (Iterator iterator = editors.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			String collectionid = data.get("collectionid");
			collectionids.add(collectionid);
		}
		if( !collectionids.isEmpty())
		{
			Collection collections = mediaArchive.query("librarycollection").
						ids(collectionids).search();
			for (Iterator iterator = collections.iterator(); iterator.hasNext();)
			{
				Data collection = (Data)iterator.next();
				String rootid = collection.get("rootcategory");
				if( rootid != null)
				{
					Category cat = mediaArchive.getCategory(rootid);  //cached
					if( cat != null)
					{
						okcategories.add(cat);
					}
				}
			}
		}
	}

	public void saveUserProfile(UserProfile inUserProfile)
	{
		if ("anonymous".equals(inUserProfile.getUserId()))
		{
			return;
		}
		MediaArchive archive = getMediaArchive(inUserProfile.getCatalogId());
		Lock lock = archive.lock("userprofilesave" + inUserProfile.getUserId() , getClass().getName());
		try
		{
			Searcher searcher = getSearcherManager().getSearcher(inUserProfile.getCatalogId(), "userprofile");
			if (inUserProfile.getSourcePath() == null)
			{
				throw new OpenEditException("user profile source path is null");
			}
			searcher.saveData(inUserProfile, null);
			archive.getCacheManager().remove("userprofile", inUserProfile.getUserId());
			archive.getCacheManager().put("userprofile", inUserProfile.getUserId(),inUserProfile);

		}
		finally
		{
			archive.releaseLock(lock);
		}
	}

	public void setRoleOnUser(String inCatalogId, User inNewuser, String inRole)
	{
		
		Searcher searcher = getSearcherManager().getSearcher(inCatalogId, "userprofile");
		UserProfile userprofile = (UserProfile) searcher.searchById(inNewuser.getId());
		if(userprofile == null)
		{
			userprofile = (UserProfile) searcher.searchByField("userid", inNewuser.getId());
		}
		if(userprofile == null)
		{
			userprofile = (UserProfile)searcher.createNewData();
			userprofile.setProperty("userid", inRole);
			userprofile.setId(inNewuser.getId());
		}
		userprofile.setProperty("settingsgroup", inRole);
		searcher.saveData(userprofile);
		clearProfile(inCatalogId, inNewuser.getId());
	}

	public void clearProfile(String inCatalogId, String id) {
		MediaArchive mediaArchive = getMediaArchive(inCatalogId);
		mediaArchive.getCacheManager().remove("userprofile", id);
		
	}
}
