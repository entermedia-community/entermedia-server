package org.openedit.entermedia.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openedit.Data;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;

import com.openedit.OpenEditException;
import com.openedit.util.Replacer;

public class AssetSecurityDataArchive implements AssetSecurityArchive {

	protected SearcherManager fieldSearcherManager;
	protected Replacer fieldReplacer;

	@Override
	public List getAccessList(MediaArchive inArchive, Asset inAsset)
			throws OpenEditException {

		if( inAsset.isPropertyTrue("public") )
		{
			List permission = new ArrayList();
			permission.add("true");
			return permission; //Nothing else matters
		}
		
		List<String> permissions = loadBasePermissions(inArchive);

		String users = inAsset.get("viewusers");
		if (users != null) {
			permissions.addAll(asList("user_", users.split("\\s+"))); 
		}

		String groups = inAsset.get("viewgroups");
		if (groups != null) {
			permissions.addAll(asList("group_", groups.split("\\s+"))); 
		}

		// What Libraries is this asset part of?
		String libraries = inAsset.get("viewlibraries");
		if (libraries != null) {
			permissions.addAll(asList("library_", libraries.split("\\s+"))); 
		}

		// clean up variables? add a bunch, then they can resolve in index time
		// tmp.put("asset.owner", inAsset.get("owner"));
		Map tmp = new HashMap();
		tmp.put("asset.owner", inAsset.get("owner"));
		for (int i = 0; i < permissions.size(); i++) {
			String value = permissions.get(i);
			String value2 = getReplacer().replace(value, tmp);
			if (value != value2) {
				permissions.set(i, value2);
			}
		}
		return permissions;
	}

	protected Collection asList(String inPrefix, String[] inSplit) {
		for (int i = 0; i < inSplit.length; i++) {
			inSplit[i] = inPrefix + inSplit[i];
		}
		List things = Arrays.asList(inSplit);
		return things;
	}

	protected List<String> loadBasePermissions(MediaArchive inArchive) {
		List<String> permissions = new ArrayList();

		collectUsers(inArchive, "catalogassetviewusers", "user_", permissions);
		collectUsers(inArchive, "catalogassetviewgroups", "group_", permissions);

		// collectUsers(inArchive, "catalogassetviewlibraries" , permissions);

		return permissions;
	}

	protected void collectUsers(MediaArchive inArchive, String inType,
			String inPrefix, List permissions) {
		Data value = getSearcherManager().getData(inArchive.getCatalogId(),
				"catalogsettings", inType);
		if (value != null) {
			String groups = value.get("value");
			if (groups != null) {
				permissions.addAll(asList(inPrefix, groups.split("\\s+")));
			}
		}
	}

	@Override
	public void revokeViewAccess(MediaArchive inArchive, String inUsername,
			Asset inAsset) {
		Collection users = inAsset.getValues("viewusers");
		users.remove(inUsername);
		inAsset.setValues("viewusers", users);
		inArchive.saveAsset(inAsset, null);
	}

	@Override
	public void revokeGroupViewAccess(MediaArchive inArchive,
			String inGroupname, Asset inAsset) {
		Collection<String> users = inAsset.getValues("viewgroups");
		users.remove(inGroupname);
		inAsset.setValues("viewgroups", users);
		inArchive.saveAsset(inAsset, null);

	}

	@Override
	public void grantViewAccess(MediaArchive inArchive, String inUsername,
			Asset inAsset) throws OpenEditException {

		Collection<String> users = inAsset.getValues("viewusers");
		if (users == null) {
			users = new ArrayList<String>();
		}
		users.add(inUsername);
		inAsset.setValues("viewusers", users);
		inArchive.saveAsset(inAsset, null);

	}

	@Override
	public void grantGroupViewAccess(MediaArchive inArchive,
			String inGroupname, Asset inAsset) throws OpenEditException {
		Collection<String> users = inAsset.getValues("viewgroups");
		if (users == null) {
			users = new ArrayList<String>();
		}
		users.add(inGroupname);
		inAsset.setValues("viewgroups", users);
		inArchive.saveAsset(inAsset, null);
	}

	@Override
	public void grantGroupViewAccess(MediaArchive inArchive,
			Collection<String> inGroupnames, Asset inAsset)
			throws OpenEditException {
		Collection<String> users = inAsset.getValues("viewgroups");
		if (users == null) {
			users = new ArrayList<String>();
		}
		users.addAll(inGroupnames);
		inAsset.setValues("viewgroups", users);
		inArchive.saveAsset(inAsset, null);

	}

	@Override
	public void grantAllAccess(MediaArchive inArchive, Asset inAsset) {
		inAsset.removeProperty("viewgroups");
		inAsset.removeProperty("viewusers");
		inAsset.setProperty("public", "true");
		inArchive.saveAsset(inAsset, null);
	}

	@Override
	public void clearAssetPermissions(MediaArchive inArchive, Asset inAsset) {
		// TODO Auto-generated method stub
		inAsset.removeProperty("public");
		inAsset.removeProperty("viewgroups");
		inAsset.removeProperty("viewusers");
		inArchive.saveAsset(inAsset, null);

	}

	public SearcherManager getSearcherManager() {
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager) {
		fieldSearcherManager = inSearcherManager;
	}

	public Replacer getReplacer() {
		if (fieldReplacer == null) {
			fieldReplacer = new Replacer();
		}
		return fieldReplacer;
	}

}
