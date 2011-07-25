package org.openedit.entermedia.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.management.openmbean.OpenDataException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;

import com.openedit.WebPageRequest;
import com.openedit.page.Page;
import com.openedit.users.Group;
import com.openedit.users.User;
import com.openedit.users.UserManager;

public class AssetControlModule extends BaseMediaModule {
	private static final Log log = LogFactory.getLog(AssetControlModule.class);

	/**
	 * This is a funny action that actually checks the permissions of the assets
	 * directory
	 * 
	 * @param inReq
	 * @return
	 * @throws Exception
	 */
	public void loadAssetPermissions(WebPageRequest inReq) throws Exception {
		// look in the assets xconf and check those permissions
		MediaArchive archive = getMediaArchive(inReq);
		String sourcepath = archive.getSourcePathForPage(inReq);

		if (sourcepath != null) {
			archive.loadAssetPermissions(sourcepath, inReq);
		} else {
			log.error("No sourcepath passed in " + inReq);
		}
	}

	public void loadAllAssetPermissions(WebPageRequest inReq) throws Exception {
		MediaArchive archive = getMediaArchive(inReq);
		String sourcepath = archive.getSourcePathForPage(inReq);
		if (sourcepath == null) {
			sourcepath = "";
		}
		archive.loadAllAssetPermissions(sourcepath, inReq);
	}

	/*
	 * protected String findSourcePath(WebPageRequest inReq) throws Exception {
	 * if(!(inReq.getPageValue("asset") instanceof Asset)) { return null; }
	 * Asset asset = (Asset) inReq.getPageValue("asset");
	 * 
	 * if (asset != null) { return asset.getSourcePath(); } MediaArchive archive
	 * = getMediaArchive(inReq); String sourcePath =
	 * �
	 * archive.getSourcePathForPage(inReq);
	 * 
	 * if( sourcePath == null) { String assetid =
	 * inReq.getRequestParameter("assetid");
	 * 
	 * //look for if (assetid != null) { return
	 * archive.getAssetSearcher().idToPath(assetid); }
	 * 
	 * } return sourcePath; }
	 */
	public List<User> listAssetViewPermissions(WebPageRequest inReq) throws Exception {
		Asset asset = getAsset(inReq);
		MediaArchive mediaArchive = getMediaArchive(inReq);

		// this is failing, getAccessList is throwing NUll
		List userNames = mediaArchive.getAssetSecurityArchive().getAccessList(mediaArchive, asset);
		List<User> users = findUsersByName(userNames);
		
		inReq.putPageValue("peoples", users);

		
		List groupids = mediaArchive.getAssetSecurityArchive().getAccessList(mediaArchive, asset);
		List<Group> groups = findGroupByIds(groupids);
		
		inReq.putPageValue("groups", groups);

		return users;
	}

	public List<User> findUsersByName(List<String> inUserNames)
	{
		List<User> users = new ArrayList<User>();
		UserManager mgr = getUserManager();
		for (String name : inUserNames)
		{
			User user = mgr.getUser(name);
			if( user != null)
			{
				users.add(user);
			}
		}
		return users;
	}
	protected List<Group> findGroupByIds(List<String> inIds)
	{
		List<Group> groups = new ArrayList<Group>();
		UserManager mgr = getUserManager();
		for (String id: inIds)
		{
			Group group = mgr.getGroup(id);
			if( group != null)
			{
				groups.add(group);
			}
		}
		return groups;
	}

	public boolean checkFolderMatchesUserName(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		if (archive != null) {
			String sourcePath = archive.getSourcePathForPage(inReq);
			if (sourcePath != null
					&& inReq.getUser() != null
					&& sourcePath.startsWith("users/" + inReq.getUser().getId()
							+ "/")) {
				return true;
			}
		}
		return false;
	}
	
	public boolean checkAssetOwnership(WebPageRequest inReq) {
		Asset asset = getAsset(inReq);
		if (asset != null && inReq.getUser() != null) {
			if(inReq.getUser().getId().equalsIgnoreCase(asset.get("owner")))
			{
				return true;
			}
		}
		return true;
	}

	public void openAssetViewPermissions(WebPageRequest inReq) throws Exception {
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		String path = "/" + asset.getCatalogId() + "/assets/"
				+ asset.getSourcePath() + "/";
		archive.loadAllAssetPermissions(asset.getSourcePath(), inReq);
		Boolean viewasset = (Boolean) inReq.getPageValue("canviewasset");
		if (viewasset != null && viewasset.booleanValue()) {
			Page page = getPageManager().getPage(path);
			archive.getAssetSecurityArchive().grantAccess(archive, page,
					"viewasset");
			archive.getAssetSearcher().updateIndex(asset);
		} else {
			throw new OpenDataException("You do not have viewasset permission "
					+ path);
		}
	}
	
	public void grantGroupAccess(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		String groupid = inReq.getRequestParameter("groupid");
		archive.getAssetSecurityArchive().grantGroupViewAccess(archive, groupid, asset);
	}
	
	public void grantUserAccess(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		String userid = inReq.getRequestParameter("userid");
		archive.getAssetSecurityArchive().grantViewAccess(archive, userid, asset);
	}

	public void revokeGroupAccess(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		String groupid = inReq.getRequestParameter("groupid");
		archive.getAssetSecurityArchive().revokeGroupViewAccess(archive, groupid, asset);
	}
	
	public void revokeUserAccess(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		String userid = inReq.getRequestParameter("userid");
		archive.getAssetSecurityArchive().revokeViewAccess(archive, userid, asset);
	}
	
	public void grantAllGroups(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		//get all of the user's groups
		User user = inReq.getUser();
		Collection<Group> groups = user.getGroups();
		//groups = getUserManager().getGroups();
		for (Group group : groups) {
			archive.getAssetSecurityArchive().grantGroupViewAccess(archive, group.getId(), asset);
		}
	}
	
	public void revokeAllGroups(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		//get all of the user's groups
		User user = inReq.getUser();
		Collection<Group> groups = user.getGroups();
		//groups = getUserManager().getGroups();
		for (Group group : groups) {
			archive.getAssetSecurityArchive().revokeGroupViewAccess(archive, group.getId(), asset);
		}
	}
}
