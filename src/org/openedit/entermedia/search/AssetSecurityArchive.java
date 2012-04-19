package org.openedit.entermedia.search;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.openedit.entermedia.Asset;
import org.openedit.entermedia.Category;
import org.openedit.entermedia.MediaArchive;

import com.openedit.OpenEditException;
import com.openedit.page.Page;
import com.openedit.users.User;
import com.openedit.users.UserManager;

public interface AssetSecurityArchive {

	public abstract List getAccessList(MediaArchive inArchive, Asset inAsset)
			throws OpenEditException;

	public abstract void revokeViewAccess(MediaArchive inArchive,
			String username, Asset inAsset);

	public abstract void revokeGroupViewAccess(MediaArchive inArchive,
			String groupname, Asset inAsset);

	public abstract void grantViewAccess(MediaArchive inArchive,
			String username, Asset inAsset) throws OpenEditException;

	public abstract void grantGroupViewAccess(MediaArchive inArchive,
			String groupname, Asset inAsset) throws OpenEditException;

	public abstract void grantGroupViewAccess(MediaArchive inArchive,
			Collection<String> groupnames, Asset inAsset)
			throws OpenEditException;

	public abstract void grantAllAccess(MediaArchive inArchive, Asset inAsset);

	//This is for users?
	public abstract void clearAssetPermissions(MediaArchive inArchive, Asset inAsset);

	//public abstract void grantAccess(MediaArchive inArchive, String username, Asset inAsset, String inView) throws OpenEditException;

	//public abstract void grantViewAccess(MediaArchive inArchive, User inUser, Category inCat) throws OpenEditException;

	//public abstract Map checkAssetPermissions(User inUser, String inCatalogId, String sourcePath);

	//public abstract void clearViewAccess(MediaArchive inArchive,	Asset inJobfolder);

	//use asset.isPropertyTrue("public")
	///public abstract boolean hasAnonymousViewAsset(Asset inAsset)		throws OpenEditException;

}