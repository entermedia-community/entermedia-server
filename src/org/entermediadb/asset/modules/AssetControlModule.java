package org.entermediadb.asset.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.projects.LibraryCollection;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Permission;
import org.openedit.users.Group;
import org.openedit.users.User;
import org.openedit.users.UserManager;
/**
 * @deprecated not used. See  AdminModule.loadCustomModulePermissions
 * @author shanti
 *
 */
public class AssetControlModule extends BaseMediaModule 
{
	
	private static final Log log = LogFactory.getLog(AssetControlModule.class);

	/**
	 * This is a funny action that actually checks the permissions of the assets
	 * directory
	 * 
	 * @param inReq
	 * @return
	 * @throws Exception
	 */
	
	public void loadAssetPermissions(WebPageRequest inReq) throws Exception 
	{
		// look in the assets xconf and check those permissions
		MediaArchive archive = getMediaArchive(inReq);
		String sourcepath = archive.getSourcePathForPage(inReq);

		if (sourcepath != null) 
		{
			archive.loadAssetPermissions(sourcepath, inReq);
		}
		else
		{	
			log.error("No sourcepath passed in " + inReq);
		}
		//loadAssetCollectionPermissions(inReq);
	}
	
	public Boolean canViewAsset(WebPageRequest inReq)
	{
		Asset asset = (Asset)inReq.getPageValue("asset"); 
		if(asset == null) {
			String assetid = inReq.findValue("assetid");
			if(assetid != null && assetid.contains("multi")) {
				
			
			asset = getAsset(inReq);
			log.info("can view asset was checking :" + assetid);
			}
		}
		
		if(asset == null)
		{
			MediaArchive archive = getMediaArchive(inReq);
			String ispublic = archive.getCatalogSettingValue("catalogassetviewispublic");
			if( Boolean.parseBoolean(ispublic) )
			{
				return true;
			}
		}
		
		MediaArchive archive = getMediaArchive(inReq);

		if(asset == null) {
			String assetid = inReq.findValue("assetid");
			if(assetid != null) {
				asset = archive.getAsset(assetid);

			}
		}
		if(asset == null)
		{
			return false;
		}
		Boolean cando = archive.getAssetSecurityArchive().canDo(archive,inReq.getUser(),inReq.getUserProfile(),"view",asset);
		return cando;
		
		
	}
	
	
	/*
	
	protected void loadAssetCollectionPermissions(WebPageRequest inReq) {
		String permissiontype = "librarycollection";
		MediaArchive archive = getMediaArchive(inReq);
		LibraryCollection collection = (LibraryCollection) inReq.getPageValue("librarycol");
		if(collection == null) {
			
			String collectionid = inReq.findValue("collectionid");
			if(collectionid == null) {
				return;
			}
			collection = (LibraryCollection) archive.getData("librarycollection", collectionid);
			if(collection == null) {
				return;
			}
			
		}
		HitTracker <Data> permissions = archive.query("datapermissions").exact("permissiontype", permissiontype).search();
		for (Iterator iterator = permissions.iterator(); iterator.hasNext();)
		{
			Data permission = (Data) iterator.next();
			
			Permission per = archive.getPermission(permissiontype + "-" + collection.getId() + "-" +  permission.getId());
			
			if(per != null) {
			boolean value = per.passes(inReq);
			inReq.putPageValue("can" + permission.getId(), Boolean.valueOf(value));
			}	
			
		}
		
	}
	
	*/
	
	
	
	public void resetPermissions(WebPageRequest inReq){
		MediaArchive archive = getMediaArchive(inReq);

		HitTracker allassets = archive.getAssetSearcher().getAllHits();
		allassets.enableBulkOperations();
		
		
		ArrayList assets = new ArrayList();
		for (Iterator iterator = allassets.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			Asset asset = archive.getAsset(hit.getId());
			assets.add(asset);
			if(assets.size() > 1000){
				archive.getAssetSearcher().saveAllData(assets, null);
				assets.clear();
			}
			
		}
		
		archive.getAssetSearcher().saveAllData(assets, null);

	}
	
	
	
	
	
	public Boolean canEditAsset(WebPageRequest inReq)
	{
		Asset asset = (Asset)inReq.getPageValue("asset"); 
		MediaArchive archive = getMediaArchive(inReq);

		if(asset == null) {
			String assetid = inReq.findValue("assetid");
			if(assetid != null) {
				asset = archive.getAsset(assetid);

			}
		}
		if(asset == null)
		{
			return false;
		}
		Boolean cando = archive.getAssetSecurityArchive().canDo(archive,inReq.getUser(),inReq.getUserProfile(),"edit",asset);
		return cando;
	}
	/*
	public void loadAllAssetPermissions(WebPageRequest inReq) throws Exception {
		MediaArchive archive = getMediaArchive(inReq);
		String sourcepath = archive.getSourcePathForPage(inReq);
		if (sourcepath == null) {
			sourcepath = "";
		}
		archive.loadAllAssetPermissions(sourcepath, inReq);
	}
	*/
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
		List<User> users = findUsersByName(inReq, userNames);
		
		inReq.putPageValue("peoples", users);

		
		List<Group> groups = findGroupByIds(inReq, userNames);
		Collections.sort(groups);

		inReq.putPageValue("groups", groups);

		return users;
	}

	public List<User> findUsersByName(WebPageRequest inReq, List<String> inUserNames)
	{
		List<User> users = new ArrayList<User>();
		UserManager mgr = getUserManager(inReq);
		for (String name : inUserNames)
		{
			if(name.contains("user_")){
				name = name.substring(5, name.length());
			}
			
					
			User user = mgr.getUser(name);
			if( user != null)
			{
				users.add(user);
			}
		}
		return users;
	}
	protected List<Group> findGroupByIds(WebPageRequest inReq, List<String> inIds)
	{
		List<Group> groups = new ArrayList<Group>();
		UserManager mgr = getUserManager(inReq);
		for (String id: inIds)
		{
			if( id.startsWith("group_" ))
			{
				id = id.substring(6);
				Group group = mgr.getGroup(id);
				if( group != null)
				{
					groups.add(group);
				}
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
//		String path = "/" + asset.getCatalogId() + "/assets/"
//				+ asset.getSourcePath() + "/";
//		archive.loadAllAssetPermissions(asset.getSourcePath(), inReq);
//		Boolean viewasset = (Boolean) inReq.getPageValue("canviewasset");
//		if (viewasset != null && viewasset.booleanValue()) {
//			//Page page = getPageManager().getPage(path);
			archive.getAssetSecurityArchive().grantAllAccess(archive, asset);
			archive.getAssetSearcher().updateIndex(asset);
//		} else {
//			throw new OpenDataException("You do not have viewasset permission "
//					+ path);
//		}
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
		
		Set<String> existingGroupIDs = new HashSet(archive.getAssetSecurityArchive().getAccessList(archive, asset));
		Collection<Group> groups = user.getGroups();
		List<String> addedGroups = new ArrayList<String>();
		
		for (Group group : groups)
		{
			if (group!=null&&group.getId()!=null&&!existingGroupIDs.contains(group.getId()))
			{
				addedGroups.add(group.getId());
			}
		}
		if (addedGroups.size()>0)
		{
			archive.getAssetSecurityArchive().grantGroupViewAccess(archive, addedGroups, asset);
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
	
	public void grantAll(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		archive.getAssetSecurityArchive().grantAllAccess(archive, asset);
	}
	
	public void revokeAll(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		archive.getAssetSecurityArchive().clearAssetPermissions(archive, asset);
	}
	
	public void isAllGroups(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		//get all of the user's groups
		User user = inReq.getUser();
		Collection<Group> groups = new ArrayList<Group>(user.getGroups());
		List groupids = archive.getAssetSecurityArchive().getAccessList(archive, asset);
		List<Group> allowedgroups = findGroupByIds(inReq, groupids);
		
		groups.removeAll(allowedgroups);
		if(groups.size() == 0)
		{
			inReq.putPageValue("isallgroups", true);
		}
		else
		{
			inReq.putPageValue("isallgroups", false);
		}
	}
	
	public void isAll(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		//TODO: Make this simpler:  inAsset.isPropertyTrue("public")
		List<String> users = archive.getAssetSecurityArchive().getAccessList(archive, asset);
		for( String permission : users)
		{
			if("true".equals(permission))
			{
				inReq.putPageValue("isall", true);
				return;
			}
		}
		inReq.putPageValue("isall", false);
	}
}
