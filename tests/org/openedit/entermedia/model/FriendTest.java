package org.openedit.entermedia.model;

import java.util.Collection;

import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.EnterMedia;
import org.openedit.entermedia.friends.FriendManager;

import com.openedit.WebPageRequest;
import com.openedit.users.User;

public class FriendTest extends BaseEnterMediaTest
{
	public void testAddRemoveFriend() throws Exception
	{
		WebPageRequest req = getFixture().createPageRequest("/entermedia/index.html");
		User currentUser = req.getUser();
		User testUser = getFixture().getUserManager().createGuestUser("test", "test", "guest");
		//FriendModule friendModule = (FriendModule)getFixture().getModuleManager().getModule("FriendModule");
		//assertNotNull(friendModule);
		EnterMedia media = getEnterMedia("entermedia");
		FriendManager friendManager = media.getFriendManager();
		assertNotNull(testUser);
		friendManager.makeFriends(currentUser.getId(), testUser.getId(), currentUser);

//		req.setRequestParameter("ownerid", currentUser.getId());
//		req.setRequestParameter("targetid", testUser.getId());

		//		friendModule.addFriend(req);
//		HitTracker friends = friendModule.getFriends(req);
//		assertTrue(friends.size() == 1);
//		friendModule.removeFriend(req);
//		friends = friendModule.getFriends(req);
//		assertTrue(friends.size() == 0);
	}
	
	public void testInviteExternalFriend() throws Exception
	{
		WebPageRequest req = getFixture().createPageRequest("/entermedia/index.html");
		User owner = req.getUser();
		FriendManager friendManager = getEnterMedia("entermedia").getFriendManager();
		//Email is sent out and invite ID is added to the current users valid id list
		Collection invites = friendManager.getOpenInvitations(owner.getUserName());
		int count = invites.size();
		
		String inviteId = friendManager.nextInviteId();
		
		friendManager.saveInvite(owner, inviteId, "cburkey@openedit.org", "Test", "Body here");
	
		invites = friendManager.getOpenInvitations(owner.getUserName());
		assertEquals( invites.size(),  count + 1 );

		//Time passes and now they accept the invitation and are now friends
		User testUser = getFixture().getUserManager().createGuestUser("testexternal", "testexternal", "guest");
		friendManager.acceptInvitation(owner.getId(), inviteId, testUser );
		assertTrue(friendManager.isFriend(owner.getId(), testUser.getId()) );
		
		//make sure the invitation has been removed from the list
		invites = friendManager.getOpenInvitations(owner.getUserName());
		assertEquals(invites.size(), count);
	}	

//	public Album createAlbum(String inName, User inUser)
//	{
//		WebPageRequest req = getFixture().createPageRequest("/entermedia/index.html");
//		AlbumModule albumModule = (AlbumModule)getFixture().getModuleManager().getModule("AlbumModule");
//		assertNotNull(albumModule);
//		EnterMedia em = albumModule.getEnterMedia(req);
//		Album album = em.getAlbumArchive().createAlbum();
//		album.setName(inName);
//		album.setUser(inUser);
//		getEnterMedia().getAlbumSearcher().saveData(album, inUser);
//		return album;
//	}
}
