package org.entermediadb.asset.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.SearcherManager;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;
import org.openedit.util.Replacer;

public class AssetSecurityDataArchive implements AssetSecurityArchive
{

	private static final Log log = LogFactory.getLog(AssetSecurityDataArchive.class);

	protected SearcherManager fieldSearcherManager;
	protected Replacer fieldReplacer;

	@Override
	public List getAccessList(MediaArchive inArchive, Asset inAsset) throws OpenEditException
	{
		return getAccessList(inArchive, "view", inAsset);
	}

	public List getAccessList(MediaArchive inArchive, String inType, Asset inAsset) throws OpenEditException
	{

		if (inAsset == null)
		{
			return null;
		}
		boolean editing = false;
		if ("edit".equals(inType))
		{
			editing = true;
		}
		if (!editing && inAsset.isPropertyTrue("public"))
		{
			List permission = new ArrayList();
			permission.add("true");
			return permission; // Nothing else matters
		}

		Set<String> permissions = new HashSet(loadBasePermissions(inArchive, inType));

		String users = inAsset.get(inType + "users");
		if (users != null)
		{
			permissions.addAll(asList("user_", users.split("\\s+")));
		}

		String groups = inAsset.get(inType + "groups");
		if (groups != null)
		{
			permissions.addAll(asList("group_", groups.split("\\s+")));
		}

		String libraries = null;
		if (editing)
		{
			libraries = inAsset.get(inType + "_libraries");
		}
		else
		{
			libraries = inAsset.get("libraries");
		}

		if (libraries != null)
		{
			Collection values = inAsset.getValues("libraries");
			for (Iterator iter = values.iterator(); iter.hasNext();)
			{
				String value = (String) iter.next();
				permissions.add("library_" + value);

			}

		}
		// clean up variables? add a bunch, then they can resolve in index time
		// tmp.put("asset.owner", inAsset.get("owner"));
		Map tmp = new HashMap();

		List values = new ArrayList(permissions.size());
		tmp.put("asset", inAsset);
		tmp.put("asset.owner", inAsset.get("owner"));
		for (Iterator iterator = permissions.iterator(); iterator.hasNext();)
		{
			String value = (String) iterator.next();
			value = getReplacer().replace(value, tmp);
			values.add(value);
		}
		return values;
	}

	protected Collection asList(String inPrefix, String[] inSplit)
	{
		for (int i = 0; i < inSplit.length; i++)
		{
			inSplit[i] = inPrefix + inSplit[i];
		}
		List things = Arrays.asList(inSplit);
		return things;
	}

	protected List<String> loadBasePermissions(MediaArchive inArchive, String inType)
	{
		List<String> permissions = new ArrayList();

		if ("view".equals(inType))
		{
			String ispublic = inArchive.getCatalogSettingValue("catalogassetviewispublic");
			if (Boolean.parseBoolean(ispublic))
			{
				permissions.add("true");
			}
		}
		collectUsers(inArchive, "catalogasset" + inType + "users", "user_", permissions);
		collectUsers(inArchive, "catalogasset" + inType + "groups", "group_", permissions);

		// collectUsers(inArchive, "catalogassetviewlibraries" , permissions);

		return permissions;
	}

	protected void collectUsers(MediaArchive inArchive, String inType, String inPrefix, List permissions)
	{
		Data value = getSearcherManager().getData(inArchive.getCatalogId(), "catalogsettings", inType);
		if (value != null)
		{
			String groups = value.get("value");
			if (groups != null && groups.length() > 0)
			{
				permissions.addAll(asList(inPrefix, groups.split("\\s+")));
			}
		}
	}

	@Override
	public void revokeViewAccess(MediaArchive inArchive, String inUsername, Asset inAsset)
	{
		Collection users = inAsset.getValues("viewusers");
		if (users == null)
		{
			users = new ArrayList<String>();
		}
		else
		{
			users = new ArrayList<String>(users);
		}
		users.remove(inUsername);
		inAsset.setValues("viewusers", users);
		inArchive.saveAsset(inAsset, null);
	}

	@Override
	public void revokeGroupViewAccess(MediaArchive inArchive, String inGroupname, Asset inAsset)
	{
		Collection<String> users = inAsset.getValues("viewgroups");
		if (users == null)
		{
			users = new ArrayList<String>();
		}
		else
		{
			users = new ArrayList<String>(users);
		}
		users.remove(inGroupname);
		inAsset.setValues("viewgroups", users);
		inArchive.saveAsset(inAsset, null);

	}

	@Override
	public void grantViewAccess(MediaArchive inArchive, String inUsername, Asset inAsset) throws OpenEditException
	{

		Collection<String> users = inAsset.getValues("viewusers");
		if (users == null)
		{
			users = new ArrayList<String>();
		}
		else
		{
			users = new ArrayList<String>(users);
		}

		users.add(inUsername);
		inAsset.removeProperty("public");
		users.remove("true");
		inAsset.setValues("viewusers", users);
		inArchive.saveAsset(inAsset, null);

	}

	@Override
	public void grantGroupViewAccess(MediaArchive inArchive, String inGroupname, Asset inAsset) throws OpenEditException
	{
		Collection<String> users = inAsset.getValues("viewgroups");
		if (users == null)
		{
			users = new ArrayList<String>();
		}
		else
		{
			users = new ArrayList<String>(users);
		}
		users.add(inGroupname);
		inAsset.removeProperty("public");
		inAsset.setValues("viewgroups", users);
		inArchive.saveAsset(inAsset, null);
	}

	@Override
	public void grantGroupViewAccess(MediaArchive inArchive, Collection<String> inGroupnames, Asset inAsset) throws OpenEditException
	{
		Collection<String> users = inAsset.getValues("viewgroups");
		if (users == null)
		{
			users = new ArrayList<String>();
		}
		else
		{
			users = new ArrayList<String>(users);
		}
		users.addAll(inGroupnames);
		inAsset.removeProperty("public");
		inAsset.setValues("viewgroups", users);
		inArchive.saveAsset(inAsset, null);

	}

	@Override
	public void grantAllAccess(MediaArchive inArchive, Asset inAsset)
	{
		inAsset.removeProperty("viewgroups");
		inAsset.removeProperty("viewusers");
		inAsset.setProperty("public", "true");
		inArchive.saveAsset(inAsset, null);
	}

	@Override
	public void clearAssetPermissions(MediaArchive inArchive, Asset inAsset)
	{
		// TODO Auto-generated method stub
		inAsset.removeProperty("public");
		inAsset.removeProperty("viewgroups");
		inAsset.removeProperty("viewusers");
		inArchive.saveAsset(inAsset, null);

	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public Replacer getReplacer()
	{
		if (fieldReplacer == null)
		{
			fieldReplacer = new Replacer();
		}
		return fieldReplacer;
	}

	public Boolean canDo(MediaArchive inArchive, User inUser, UserProfile inProfile, String inType, Asset inAsset)
	{
		if (inAsset == null)
		{
			return true;
		}
		if (inUser != null && inUser.isInGroup("administrators"))
		{
			return true;
		}
		Collection<Category> exactcategories = inAsset.getCategories();
		if (inProfile != null)
		{
			Collection<Category> allowedcats = inProfile.getViewCategories();
			if( allowedcats != null)
			{
				for (Category cat : exactcategories)
				{
					if (cat.hasParentCategory(allowedcats))
					{
						return true;
					}
				}
			}	
		}
		String owner = inAsset.get("owner");
		if( owner != null && inUser != null && owner.equals(inUser.getId()))
		{
			return true;
		}
		// tmp.put("asset.owner", );
		if( "view".equals(inType))
		{
			Collection<Category> publiccategories = inArchive.listHiddenCategories();
			for (Category cat : exactcategories)
			{
				for (Iterator iterator = publiccategories.iterator(); iterator.hasNext();)
				{
					Category publiccategory = (Category)iterator.next();
					if (cat.hasParent(publiccategory.getId()))
					{
						return false;
					}					
				}
			}
			return true;
		}
		
		return false;
		/*
		 * Collection allowed = getAccessList(inArchive, inType, inAsset);
		 * 
		 * if (allowed.size() == 0) { return Boolean.FALSE; } if
		 * (allowed.contains("true")) { return Boolean.TRUE; } if (inUser !=
		 * null) { for (Iterator iterator = inUser.getGroups().iterator();
		 * iterator .hasNext();) { Group group = (Group) iterator.next(); if
		 * (allowed.contains("group_" + group.getId())) { return Boolean.TRUE; }
		 * } if (allowed.contains("user_" + inUser.getUserName())) { return
		 * Boolean.TRUE; } }
		 * 
		 * // TODO: Add libraries from user , profile and each group Collection
		 * values = inAsset.getValues("libraries");
		 * 
		 * if( log.isDebugEnabled() ) { log.debug("Checking libraries " +
		 * values); }
		 * 
		 * if( values != null && inType.equals("view") && inProfile != null ) {
		 * Searcher searcher =
		 * getSearcherManager().getSearcher(inArchive.getCatalogId(),
		 * "libraryroles"); if( inProfile.getSettingsGroup() != null ) {
		 * SearchQuery query = searcher.createSearchQuery().append("roleid",
		 * inProfile.getSettingsGroup().getId()); query.addOrsGroup("libraryid",
		 * values); Data found = searcher.searchByQuery(query); if( found !=
		 * null ) { return Boolean.TRUE; } if( inUser != null ) { //Search for
		 * all the libraries defined then check groups searcher =
		 * getSearcherManager().getSearcher(inArchive.getCatalogId(),
		 * "librarygroups"); query = searcher.createSearchQuery();
		 * query.addOrsGroup("libraryid", values);
		 * 
		 * List groupids = new ArrayList(); for (Iterator iterator2 =
		 * inUser.getGroups().iterator(); iterator2.hasNext();) { Group group =
		 * (Group)iterator2.next(); groupids.add(group.getId()); }
		 * query.addOrsGroup("groupid", groupids); found =
		 * searcher.searchByQuery(query); if( found != null ) { return
		 * Boolean.TRUE; }
		 * 
		 * searcher = getSearcherManager().getSearcher(inArchive.getCatalogId(),
		 * "libraryusers"); query =
		 * searcher.createSearchQuery().append("userid",inUser.getId());
		 * query.addOrsGroup("_parent", values); found =
		 * searcher.searchByQuery(query); if( found != null ) { return
		 * Boolean.TRUE; } } else if( log.isDebugEnabled() ) {
		 * log.debug("No user found and profile has no libraries " +
		 * inProfile.getSettingsGroup().getId() ); } } } if(
		 * log.isDebugEnabled() ) { log.debug("No rights for " + inType + " on "
		 * + inProfile ); }
		 */
	}

}
