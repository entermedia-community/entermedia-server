package org.openedit.entermedia.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openedit.entermedia.Asset;
import org.openedit.entermedia.Category;
import org.openedit.entermedia.MediaArchive;

import com.openedit.BaseWebPageRequest;
import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.page.Page;
import com.openedit.page.PageSettings;
import com.openedit.page.Permission;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.users.UserManager;
import com.openedit.util.Replacer;
import com.openedit.util.RequestUtils;
import com.openedit.util.strainer.ActionFilter;
import com.openedit.util.strainer.BlankFilter;
import com.openedit.util.strainer.BooleanFilter;
import com.openedit.util.strainer.Filter;
import com.openedit.util.strainer.GroupFilter;
import com.openedit.util.strainer.NotFilter;
import com.openedit.util.strainer.OrFilter;
import com.openedit.util.strainer.SettingsGroupFilter;
import com.openedit.util.strainer.UserFilter;
import com.openedit.util.strainer.UserProfileFilter;

public class AssetSecurityArchive 
{
	protected PageManager fieldPageManager;
	protected RequestUtils fieldRequestUtils;
	protected UserManager fieldUserManager;
	protected Replacer fieldReplacer;
	
	
	public UserManager getUserManager()
	{
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}

	public RequestUtils getRequestUtils()
	{
		return fieldRequestUtils;
	}

	public void setRequestUtils(RequestUtils inRequestUtils)
	{
		fieldRequestUtils = inRequestUtils;
	}

	public List getAccessList(MediaArchive inArchive, Page inPage, Asset inAsset, String inPermission)
	{
		Permission permission = inPage.getPermission(inPermission);
		ArrayList users = new ArrayList();
		if (permission != null && permission.getRootFilter() != null)
		{
			collectUsers(users, permission.getRootFilter(), inAsset);
		}
		return users;
	}

	public List getAccessList(MediaArchive inArchive, Asset inAsset) throws OpenEditException
	{
		String path = inAsset.getSourcePath();
		// $home$cataloghome/assets/${store.assetPathFinder.idToPath($cell.id
		// )}.html
		//Page page = getPageManager().getPage(inArchive.getCatalogHome() + "/assets/" + path + ".html");
		Page page = getPageManager().getPage(inArchive.getCatalogHome() + "/assets/" + path + "/_site.xconf", true);

		List users = getAccessList(inArchive, page, inAsset, "viewasset");
		List assetadminusers = getAccessList(inArchive, page, inAsset, "viewassetadmin");
		users.addAll(assetadminusers);
		return users;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}
	
	public Replacer getReplacer()
	{
		if( fieldReplacer == null)
		{
			fieldReplacer = new Replacer();
		}
		return fieldReplacer;
	}

	private void collectUsers(List add, Filter inRoot, Asset inAsset)
	{
			if (inRoot instanceof UserFilter)
			{
				
				String username = ((UserFilter) inRoot).getUsername();
				if( username != null)
				{
					Map tmp = new HashMap();
					tmp.put("asset.owner", inAsset.get("owner"));
					username = getReplacer().replace(username, tmp);
					add.add(username);
				}
			}
			else if (inRoot instanceof GroupFilter)
			{
				String groupid = ((GroupFilter) inRoot).getGroupId();
				add.add(groupid); //TODO we should add group-
			}
			else if (inRoot instanceof BlankFilter)
			{
				add.add("true");
			}
			else if (inRoot instanceof BooleanFilter)
			{
				if(((BooleanFilter) inRoot).isTrue())
				{
					add.add("true");
				}
				else
				{
					add.add("false");					
				}
			}
			else if (inRoot instanceof SettingsGroupFilter)
			{
				String groupid = ((SettingsGroupFilter) inRoot).getGroupId();
				add.add("sgroup" + groupid); 
			}
			else if (inRoot instanceof UserProfileFilter)
			{
				UserProfileFilter filter = (UserProfileFilter)inRoot;
				String propname = filter.getPropertyName();
				add.add("profile" + propname); 
			}
			else if (inRoot instanceof NotFilter)
			{
				//Cant process not filters within an index
				return;
			}
			else if( inRoot instanceof ActionFilter)
			{
				//This is used mostly on editasset permissions
				//<action name="AssetControlModule.checkFolderMatchesUserName" />
				//we might be in a users home folder. Add their username in
				String sp = inAsset.getSourcePath();
				if( sp.startsWith("users"))
				{
					String[] paths = sp.split("/");
					String username = paths[1];
					add.add(username);
				}
			}
			else
			{
				Filter[] filters = inRoot.getFilters(); //top level is a container like a list of groups?
				for (int i = 0; i < filters.length; i++)
				{
					Filter filter = filters[i];
					collectUsers(add, filter, inAsset);
				}
			}

	}
	
	private void revokeUserViewAccess(List toremove, Filter inRoot, String inUserName)
	{
			if (inRoot instanceof UserFilter)
			{
				UserFilter filter = (UserFilter)inRoot;
				if(filter.getUsername().equals(inUserName))
				{
					toremove.add(filter);
				}
			}
			else if(inRoot.getFilters() == null)
			{
				return;
			}
			else
			{
				Filter[] filters = inRoot.getFilters(); //top level is a container like a list of groups?
				ArrayList<Filter> remove = new ArrayList<Filter>();
				for (int i = 0; i < filters.length; i++)
				{
					Filter filter = filters[i];
					revokeUserViewAccess(remove, filter, inUserName);
				}
				for (Filter filter : remove)
				{
					inRoot.removeFilter(filter);
				}
			}
	}
	
	private void revokeGroupViewAccess(List toremove, Filter inRoot, String inGroupName)
	{
			if (inRoot instanceof GroupFilter)
			{
				GroupFilter filter = (GroupFilter)inRoot;
				if(filter.getGroupId().equals(inGroupName))
				{
					toremove.add(filter);
				}
			}
			else if(inRoot.getFilters() == null)
			{
				return;
			}
			else
			{
				Filter[] filters = inRoot.getFilters(); //top level is a container like a list of groups?
				ArrayList<Filter> remove = new ArrayList<Filter>();
				for (int i = 0; i < filters.length; i++)
				{
					Filter filter = filters[i];
					revokeGroupViewAccess(remove, filter, inGroupName);
				}
				for (Filter filter : remove)
				{
					inRoot.removeFilter(filter);
				}
			}
	}
	
	public void revokeViewAccess(MediaArchive inArchive, String username, Asset inAsset)
	{
		String path = inAsset.getSourcePath();
		Page page = getPageManager().getPage(inArchive.getCatalogHome() + "/assets/" + path + "/_site.xconf",true);
		Permission permission = page.getPermission("viewasset");
		
		if (permission != null && permission.getRootFilter() != null)
		{
			ArrayList<Filter> remove = new ArrayList<Filter>();
			revokeUserViewAccess(remove, permission.getRootFilter(), username);
			for (Filter filter : remove)
			{
				permission.getRootFilter().removeFilter(filter);
			}
		}
		getPageManager().getPageSettingsManager().saveSetting(page.getPageSettings());
		inArchive.getAssetSearcher().updateIndex(inAsset);

	}
	
	public void revokeGroupViewAccess(MediaArchive inArchive, String groupname, Asset inAsset)
	{
		String path = inAsset.getSourcePath();
		Page page = getPageManager().getPage(inArchive.getCatalogHome() + "/assets/" + path + "/_site.xconf",true);
		Permission permission = page.getPermission("viewasset");
		
		if (permission != null && permission.getRootFilter() != null)
		{
			ArrayList<Filter> remove = new ArrayList<Filter>();
			revokeGroupViewAccess(remove, permission.getRootFilter(), groupname);
			for (Filter filter : remove)
			{
				permission.getRootFilter().removeFilter(filter);
			}
		}
		if( permission.getRootFilter() instanceof OrFilter )
		{
			if( permission.getRootFilter().getFilters().length == 0 )
			{
				page.getPageSettings().removePermission(permission);
			}
		}
		getPageManager().getPageSettingsManager().saveSetting(page.getPageSettings());
		inArchive.getAssetSearcher().updateIndex(inAsset);

	}
	
	public void grantViewAccess(MediaArchive inArchive, String username, Asset inAsset) throws OpenEditException
	{
		String path = inArchive.getCatalogHome() + "/assets/" + inAsset.getSourcePath() + "/_site.xconf";

		// $home$cataloghome/assets/${store.assetPathFinder.idToPath($cell.id
		// )}.html
		Page page = getPageManager().getPage(path,true);

		grantAccess(inArchive, username, page, "viewasset");
		// update the index
		inArchive.getAssetSearcher().updateIndex(inAsset);

	}
	
	public void grantGroupViewAccess(MediaArchive inArchive, String groupname, Asset inAsset) throws OpenEditException
	{
		String path = inArchive.getCatalogHome() + "/assets/" + inAsset.getSourcePath() + "/_site.xconf";

		// $home$cataloghome/assets/${store.assetPathFinder.idToPath($cell.id
		// )}.html
		Page page = getPageManager().getPage(path,true);

		grantGroupAccess(inArchive, groupname, page, "viewasset");
		// update the index
		inArchive.getAssetSearcher().updateIndex(inAsset);

		
	}
	public void grantGroupViewAccess(MediaArchive inArchive, Collection<String> groupnames, Asset inAsset) throws OpenEditException
	{
		String path = inArchive.getCatalogHome() + "/assets/" + inAsset.getSourcePath() + "/_site.xconf";
		
		// $home$cataloghome/assets/${store.assetPathFinder.idToPath($cell.id
		// )}.html
		Page page = getPageManager().getPage(path,true);
		
		grantGroupAccess(inArchive, groupnames, page, "viewasset");
		// update the index
		inArchive.getAssetSearcher().updateIndex(inAsset);
		
	}
	
	public void grantAllAccess(MediaArchive inArchive, Asset inAsset)
	{
		String path = inArchive.getCatalogHome() + "/assets/" + inAsset.getSourcePath() + "/_site.xconf";
		Page page = getPageManager().getPage(path, true);
		grantAccess(inArchive, page, "viewasset");
		inArchive.getAssetSearcher().updateIndex(inAsset);
	}
	
	public void revokeAllAccess(MediaArchive inArchive, Asset inAsset)
	{
		String path = inArchive.getCatalogHome() + "/assets/" + inAsset.getSourcePath() + "/_site.xconf";
		Page page = getPageManager().getPage(path,true);
		clearAccess(inArchive, page, "viewasset");
		inArchive.getAssetSearcher().updateIndex(inAsset);

	}

	public void grantAccess(MediaArchive inArchive, String username, Asset inAsset, String inView) throws OpenEditException
	{
		String path = inArchive.getCatalogHome() + "/assets/" + inAsset.getSourcePath() + "/_site.xconf";

		// $home$cataloghome/assets/${store.assetPathFinder.idToPath($cell.id
		// )}.html
		Page page = getPageManager().getPage(path,true);

		grantAccess(inArchive, username, page, inView);
		// update the index
		inArchive.getAssetSearcher().updateIndex(inAsset);

	}

	public void grantViewAccess(MediaArchive inArchive, User inUser, Category inCat) throws OpenEditException
	{
		// $home$cataloghome/assets/${store.assetPathFinder.idToPath($cell.id
		// )}.html
		Page page = getPageManager().getPage(inArchive.getCatalogHome() + "/categories/" + inCat.getId() + ".html",true);
		
		grantViewAccess(inArchive, inUser.getUserName(), page);
	}

	public void clearViewAccess(MediaArchive inArchive, Page inPage)
	{
		clearAccess(inArchive, inPage, "view");
	}
	
	public void clearAccess(MediaArchive inArchive, Page inPage, String inPermission)
	{
		Permission permission = inPage.getPageSettings().getLocalPermission(inPermission);
		if (permission != null)
		{
			inPage.getPageSettings().removePermission(permission);
			getPageManager().getPageSettingsManager().saveSetting(inPage.getPageSettings());
		}
	}

	public void grantViewAccess(MediaArchive inArchive, String inUserName, Page inPage) throws OpenEditException
	{
		grantAccess(inArchive, inUserName, inPage, "view");
	}
	
	public void grantAccessIfNeeded(MediaArchive inArchive, String inUserName, Page inPage, String inPermission)
	{
		PageSettings settings = inPage.getPageSettings();
		Permission permission = settings.getPermission(inPermission);
				
		if( permission == null ) //null is bad, nobody has it
		{
			grantAccess(inArchive, inUserName, inPage, inPermission);
			return;
		}
		User user = getUserManager().getUser(inUserName);
		WebPageRequest req = getRequestUtils().createPageRequest(inPage,user);
		boolean ok = permission.passes(req);
		if( !ok)
		{
			grantAccess(inArchive, inUserName, inPage, inPermission);
		}
	}

	public void grantAccess(MediaArchive inArchive, Page inPage, String inPermission)
	{
		PageSettings settings = inPage.getPageSettings();
		Permission permission = settings.getPermission(inPermission);

		if( permission == null || !permission.getPath().equals(settings.getPath()))
		{
			Permission per = new Permission();
			per.setName(inPermission);
			per.setPath(settings.getPath());
			permission = per;
			settings.addPermission(per);
		}
		BooleanFilter test = new BooleanFilter();
		test.setTrue(true);
		permission.setRootFilter(test);

		getPageManager().getPageSettingsManager().saveSetting(inPage.getPageSettings());
	}
	
	public void setupPermission(PageSettings inSettings, Permission inPermission, String inPermissionString)
	{
		Filter rootFilter = inPermission.getRootFilter();
		if (rootFilter == null || rootFilter instanceof BooleanFilter)
		{
			rootFilter = new OrFilter();
			inPermission.setRootFilter(rootFilter);
		}
		else if (!rootFilter.getType().equalsIgnoreCase("or"))
		{
			inPermission.setRootFilter(new OrFilter());
			inPermission.getRootFilter().addFilter(rootFilter); //the old value to the OR list
		}
	}
	
	public Permission createPermission(PageSettings inSettings, String inPermissionString)
	{
		Permission per = new Permission();
		per.setName(inPermissionString);
		if( per != null && per.getRootFilter() != null)
		{
			per.setRootFilter(per.getRootFilter().copy(inPermissionString));
		}
		per.setPath(inSettings.getPath());
		inSettings.addPermission(per);
		return per;
	}

	
	public void grantAccess(MediaArchive inArchive, String inUserName, Page inPage, String inPermission)
	{
		PageSettings settings = inPage.getPageSettings();
		Permission permission = settings.getPermission(inPermission);
		if( permission == null || !permission.getPath().equals(settings.getPath()))
		{
			permission = createPermission(settings, inPermission);
		}
		setupPermission(settings, permission, inPermission);
		
		UserFilter filter = new UserFilter();
		filter.setUsername(inUserName);
		permission.getRootFilter().addFilter(filter);
		getPageManager().getPageSettingsManager().saveSetting(inPage.getPageSettings());
		// update the index
		// saveAsset(inAsset);

	}
	
	public void grantGroupAccess(MediaArchive inArchive, String inGroupName, Page inPage, String inPermission)
	{
		PageSettings settings = inPage.getPageSettings();
		Permission permission = settings.getPermission(inPermission);

		if( permission == null || !permission.getPath().equals(settings.getPath()))
		{
			permission = createPermission(settings, inPermission);
		}
		setupPermission(settings, permission, inPermission);
		
		GroupFilter filter = new GroupFilter();
		filter.setGroupId(inGroupName);
		permission.getRootFilter().addFilter(filter);
		getPageManager().getPageSettingsManager().saveSetting(inPage.getPageSettings());
		getPageManager().clearCache(inPage);
		// update the index
		// saveAsset(inAsset);

	}
	public void grantGroupAccess(MediaArchive inArchive, Collection<String> inGroups, Page inPage, String inPermission)
	{
		PageSettings settings = inPage.getPageSettings();
		Permission permission = settings.getPermission(inPermission);
		
		if( permission == null || !permission.getPath().equals(settings.getPath()))
		{
			permission = createPermission(settings, inPermission);
		}
		setupPermission(settings, permission, inPermission);
		
		for (String groupName : inGroups)
		{
			GroupFilter filter = new GroupFilter();
			filter.setGroupId(groupName);
			permission.getRootFilter().addFilter(filter);
		}
		getPageManager().getPageSettingsManager().saveSetting(inPage.getPageSettings());
		getPageManager().clearCache(inPage);
		// update the index
		// saveAsset(inAsset);
		
	}

	public Map checkAssetPermissions(User inUser, String inCatalogId, String sourcePath)
	{
		String path = "/" + inCatalogId + "/assets/" + sourcePath + "/_site.xconf";
		List names = Arrays.asList(new String[]{"customdownload","download","forcewatermark","editasset","viewasset","view"});
		Page page = getPageManager().getPage(path,true);

		WebPageRequest req = new BaseWebPageRequest();
		req.setUser(inUser);
		req.setPage(page);
		
		for (Iterator iterator = names.iterator(); iterator.hasNext();)
		{
			String pername = (String) iterator.next();
			Permission per = page.getPermission(pername);
			if (per != null)
			{
				boolean value = per.passes(req);
				req.putPageValue("can" + per.getName(), Boolean.valueOf(value) );
			}
		}
		return req.getPageMap();
	}

	public void clearViewAccess(MediaArchive inArchive, Asset inJobfolder) {
		// $home$cataloghome/assets/${store.assetPathFinder.idToPath($cell.id
		// )}.html
		Page page = getPageManager().getPage(inArchive.getCatalogHome() + "/assets/" + inJobfolder.getSourcePath() + "/_site.xconf",true);
		clearViewAccess(inArchive, page);		
	}
	
	public boolean hasAnonymousViewAsset(Asset inAsset) throws OpenEditException
	{
		Map all = checkAssetPermissions(null,inAsset.getCatalogId(),inAsset.getSourcePath());

		Boolean canassetview = (Boolean)all.get("canviewasset");
		if( canassetview != null && canassetview.booleanValue())
		{
			return true;
		}
		return false;
		
	}

}
