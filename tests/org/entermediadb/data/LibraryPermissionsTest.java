package org.entermediadb.data;

import java.util.Collection;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.modules.ProfileModule;
import org.entermediadb.projects.ProjectManager;
import org.entermediadb.users.UserProfileManager;
import org.entermediadb.projects.LibraryCollection;
import org.junit.Test;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.profile.UserProfile;
import org.openedit.users.Group;
import org.openedit.users.User;
import org.openedit.users.UserManager;

public class LibraryPermissionsTest extends BaseEnterMediaTest
{
	@Test
	public void testLibraryPermissions() throws Exception
	{
		ProjectManager manager = (ProjectManager)getFixture().getModuleManager().getBean(getMediaArchive().getCatalogId(),"projectManager");
		
		WebPageRequest req = getFixture().createPageRequest("/testcatalog/index.html");
		
		Searcher lsearcher = getMediaArchive().getSearcher("library");
		Searcher upsearcher = getMediaArchive().getSearcher("userprofile");
		
		Data library = lsearcher.createNewData();
		library.setId("library1");
		library.setName("Library One");
		lsearcher.saveData(library, null);
		
		Data library2 = lsearcher.createNewData();
		library2.setId("library2");
		library2.setName("Library Two");
		lsearcher.saveData(library2, null);
		
		UserManager uman = getMediaArchive().getUserManager();
		
		
		User roleuser = uman.getUser("roleuser");
		User groupuser = uman.getUser("groupuser");
		if (roleuser == null) {
			
			roleuser = uman.createUser("roleuser", "junk");	
			uman.saveUser(roleuser);
		}
		if (groupuser == null) {
			
			groupuser = uman.createUser("groupuser", "junk");
			uman.saveUser(groupuser);	
		}
		
		Group usersgroup = uman.getGroup("users");
		
		groupuser.addGroup(usersgroup);
		roleuser.addGroup(usersgroup);
		uman.saveUser(groupuser);
		uman.saveUser(roleuser);
		
		
		ProfileModule module = (ProfileModule)getFixture().getModuleManager().getBean("ProfileModule");
		
		UserProfileManager upmanager = module.getUserProfileManager();
		
		
		
		
		Searcher lgsearcher = getMediaArchive().getSearcher("librarygroups");
		
		Data libgroup = lgsearcher.query().match("libraryid", "library1").match("groupid", "users").searchOne();
		if (libgroup == null) {

			libgroup = lgsearcher.createNewData();
			
			libgroup.setProperty("libraryid", "library1");
			libgroup.setProperty("groupid", "users");
			
			lgsearcher.saveData(libgroup, groupuser);
		
		}
		req = getFixture().createPageRequest("/testcatalog/indexASDA.html");		
		UserProfile groupprofile = upmanager.loadUserProfile(req, getMediaArchive().getCatalogId(), "groupuser");
		
//		String lib1 = groupprofile.getViewCategories().iterator().next();
//		
//		assertEquals(library.getId(), lib1);
		
		// TODO: add searching by users for assets to ensure different between users
				
	}
	
}