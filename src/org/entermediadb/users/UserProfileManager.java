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

		if (inUserName == null)
		{
			inUserName = "anonymous";
		}
		String appid = inReq.findValue("applicationid");
		if(appid == null){
			return null;
		}
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

		MediaArchive mediaArchive = getMediaArchive(inCatalogId);

		UserProfile userprofile = null;
		if (inReq != null)
		{
			boolean reload = Boolean.parseBoolean(inReq.findValue("reloadprofile"));
			
			userprofile = (UserProfile) inReq.getPageValue("userprofile");
			if (userprofile == null)
			{
				userprofile = (UserProfile) inReq.getSessionValue(id);
			}

			if (!reload && userprofile != null && inUserName.equals(userprofile.getUserId()) && inCatalogId.equals(userprofile.getCatalogId()))
			{
				String index = mediaArchive.getSearcher("settingsgroup").getIndexId();
				if( !index.equals(userprofile.getIndexId()) )
				{
					reload = true;
				}
				else
				{
					reload = false;
				}
			}

			if (!reload && userprofile != null && inUserName.equals(userprofile.getUserId()))
			{
				inReq.putPageValue("userprofile", userprofile);
				return userprofile;
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
			String index = mediaArchive.getSearcher("settingsgroup").getIndexId();
			if( userprofile == null || !index.equals(userprofile.getIndexId()) )
			{
				userprofile = readProfileOptions(inReq, id, inUserName, appid, mediaArchive);
			}
			inReq.putSessionValue(id, userprofile);
			inReq.putPageValue("userprofile", userprofile);
		}
		finally
		{
			mediaArchive.releaseLock(lock);
		}

		return userprofile;
	}

	protected UserProfile readProfileOptions(WebPageRequest inReq, String id, String inUserName, String appid, MediaArchive mediaArchive)
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

		//Must be set before we run actions below
		inReq.putSessionValue(id, userprofile);
		inReq.putPageValue("userprofile", userprofile);

		userprofile.setIndexId( mediaArchive.getSearcher("settingsgroup").getIndexId() );

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
		
		//Load all the collections they have rights to based okcategories + their parents
		//categories+parents
		//lava loop over every collection and mesh 		
		inUserprofile.setViewCategories(okcategories);
		inUserprofile.setCollectionIds(null);
		if( !okcategories.isEmpty() )
		{
			Collection<String> okcollectionids = new ArrayList<String>();
			HitTracker found = mediaArchive.query("librarycollection").all().search();
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
