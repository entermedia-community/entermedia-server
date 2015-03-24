package org.openedit.users;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.openedit.data.SearcherManager;
import org.openedit.event.WebEventHandler;

import com.openedit.OpenEditException;
import com.openedit.hittracker.HitTracker;
import com.openedit.users.Authenticator;
import com.openedit.users.Group;
import com.openedit.users.User;
import com.openedit.users.UserManager;
import com.openedit.users.UserManagerException;
import com.openedit.users.authenticate.AuthenticationRequest;
import com.openedit.users.filesystem.PermissionsManager;
import com.openedit.users.filesystem.XmlUserArchive;
import com.openedit.util.StringEncryption;

public class BaseUserManager implements UserManager
{
	protected String fieldCatalogId;
	protected SearcherManager fieldSearcherManager;
	
	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public GroupSearcher getGroupSearcher()
	{
		return (GroupSearcher)getSearcherManager().getSearcher(getCatalogId(), "group");
	}

	
	public UserSearcher getUserSearcher()
	{
		return (UserSearcher)getSearcherManager().getSearcher(getCatalogId(), "user");
	}

	
	protected XmlUserArchive getXmlUserArchive()
	{
		return getUserSearcher().getXmlUserArchive(); //This might be in another catalogid
	}

	@Override
	public List getSystemPermissionGroups()
	{
		return getXmlUserArchive().getSystemPermissionGroups();
	}

	@Override
	public Group getGroup(String inGroupId) throws UserManagerException
	{
		return getGroupSearcher().getGroup(inGroupId);
	}

	@Override
	public HitTracker getGroups()
	{

		return getXmlUserArchive().getGroups();
	}

	@Override
	public User getUser(String inUserName) throws UserManagerException
	{

		return getUserSearcher().getUser(inUserName);
	}

	@Override
	public HitTracker getUsers()
	{

		return getUserSearcher().getAllHits();
	}

	@Override
	public boolean authenticate(AuthenticationRequest inReq)
	{

		return getXmlUserArchive().authenticate(inReq);
	}

	@Override
	public boolean authenticate(User inUser, String inPassword)
	{

		return  getXmlUserArchive().authenticate(inUser, inPassword);
	}

	@Override
	public Group createGroup() throws UserManagerException
	{

		return getXmlUserArchive().createGroup();
	}

	@Override
	public Group createGroup(String inGroupId) throws UserManagerException
	{

		return getXmlUserArchive().createGroup(inGroupId);
	}

	@Override
	public Group createGroup(String inGroupId, String inGroupName) throws UserManagerException
	{

		return getXmlUserArchive().createGroup(inGroupId, inGroupName);
	}

	@Override
	public User createUser(String inUserName, String inPassword) throws UserManagerException
	{
		return getXmlUserArchive().createUser(inUserName, inPassword);
	}

	@Override
	public void deleteGroup(Group inGroup) throws UserManagerException
	{
		getGroupSearcher().delete(inGroup,null);
	}

	@Override
	public void deleteUser(User inUser) throws UserManagerException
	{
		getUserSearcher().delete(inUser, null);
	}

	public void deleteGroups(List inGroups) throws UserManagerException {
		if (inGroups != null) {
			for (Iterator iter = inGroups.iterator(); iter.hasNext();) {
				Group group = (Group) iter.next();
				deleteGroup(group);
			}
		}
	}

	public void deleteUsers(List inUsers) throws UserManagerException {
		if (inUsers != null) {
			for (Iterator iter = inUsers.iterator(); iter.hasNext();) {
				User user = (User) iter.next();
				deleteUser(user);
			}
		}
	}

	@Override
	public User getUserByEmail(String inEmailaddress) throws UserManagerException
	{
		return getUserSearcher().getUserByEmail(inEmailaddress);
	}

	@Override
	public void saveUser(User inUser)
	{
		getUserSearcher().saveData(inUser,null);
	}

	@Override
	public void saveGroup(Group inGroup)
	{
		getGroupSearcher().saveData(inGroup,null);
	}

	@Override
	//TODO: Use the Searcher here
	public HitTracker getUsersInGroup(Group inGroup)
	{
		return getXmlUserArchive().getUsersInGroup(inGroup);
	}

	@Override
	//TODO: Use the Searcher here
	public HitTracker getUsersInGroup(String inString)
	{
		return getXmlUserArchive().getUsersInGroup(inString);
	}

	///TODO: Refactor all the authentication to here
	@Override
	public Authenticator getAuthenticator()
	{

		return getXmlUserArchive().getAuthenticator();
	}

	@Override
	public StringEncryption getStringEncryption()
	{

		return getXmlUserArchive().getStringEncryption();
	}

	@Override
	public String encryptPassword(User inUser) throws OpenEditException
	{

		return getXmlUserArchive().encryptPassword(inUser);
	}

	@Override
	public String decryptPassword(User inUser) throws OpenEditException
	{
		return getXmlUserArchive().decryptPassword(inUser);
	}

	@Override
	public void setWebEventHandler(WebEventHandler inHandler)
	{
		getXmlUserArchive().setWebEventHandler(inHandler);
	}

	@Override
	public void logout(User inUser)
	{
		getXmlUserArchive().logout(inUser);

	}

	@Override
	public PermissionsManager getPermissionsManager()
	{
		return getXmlUserArchive().getPermissionsManager();
	}

	@Override
	public User createGuestUser(String inAccount, String inPassword, String inGroupname)
	{
		return getXmlUserArchive().createGuestUser(inAccount, inPassword, inGroupname);
	}

	@Override
	public String getScreenName(String inUserName)
	{
		return getXmlUserArchive().getScreenName(inUserName);
	}

	@Override
	public void flush()
	{
		getXmlUserArchive().flush();
	}

	@Override
	public Collection listGroupIds()
	{
		return getXmlUserArchive().listGroupIds();
	}

	@Override
	public String nextId()
	{
		return getXmlUserArchive().nextId();
	}

	@Override
	@Deprecated
	/**
	 */
	public User loadUser(String inId)
	{

		return getXmlUserArchive().loadUser(inId);
	}

	@Override
	public Collection listUserNames()
	{

		return getXmlUserArchive().listUserNames();
	}

}
