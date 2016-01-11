package org.entermedia.elasticsearch.searchers;

import java.util.Collection;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.elasticsearch.searchers.ElasticUserSearcher;
import org.junit.Test;
import org.openedit.Data;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.users.BaseGroup;
import org.openedit.users.BaseUser;
import org.openedit.users.Group;
import org.openedit.users.User;

public class ElasticUserSearcherTest extends BaseEnterMediaTest
{
	@Test
	public void testVerifyConfiguration()
	{
		ElasticUserSearcher userSearcher = (ElasticUserSearcher) getMediaArchive().getSearcherManager().getSearcher("system", "user");
		assertNotNull("user searcher is NULL!", userSearcher);
		assertTrue("user searcher is elastic", userSearcher instanceof ElasticUserSearcher);
	}

	@Test
	public void testCreateNewData()
	{
		ElasticUserSearcher searcher = new ElasticUserSearcher();
		Data data = null;
		data = searcher.createNewData();
		assertNotNull("data is NULL!", data);
		
		BaseUser base = (BaseUser) data;
		assertTrue("not enabled!", base.isEnabled());
	}

	@Test
	public void testGetUsers() throws Exception
	{
//		Client client = new ClientPool().getClient();
//		Thread.sleep(5000);
//		client.close();
		
		ElasticUserSearcher userSearcher = (ElasticUserSearcher) getMediaArchive().getSearcherManager().getSearcher("system", "user");
		userSearcher.reIndexAll();
		
//		GetResponse response = userSearcher.getClient().prepareGet("entermedia_catalogs_testcatalog", "user", "admin")
//		        .execute()
//		        .actionGet();
//		
//		assertEquals("admin", response.getId());
//		
//		SearchQuery query = new SearchQuery();
//		query.setId("admin");
//		ElasticHitTracker hit = (ElasticHitTracker) userSearcher.search( query );
//		assertNotNull("hit is null", hit);
		
//		Object result = null;
//		result = hit.getById("admin");
//		assertNotNull("result is NULL", result); 
		
		User result = null;
		result = userSearcher.getUser("admin");
		assertNotNull("user is NULL, cannot find 'admin'!", result);
		
		SearchQuery q = userSearcher.createSearchQuery();
		q.addOrsGroup("id", "admin testuser");
		Collection col = userSearcher.search(q);
		assertTrue(col.size() > 0);

		q = userSearcher.createSearchQuery();
		q.addMatches("description", "admin");
		col = userSearcher.search(q);
		assertTrue(col.size() > 0);

	}

	@Test
	public void testGetUserByEmail() throws Exception
	{
		ElasticUserSearcher userSearcher = (ElasticUserSearcher) getMediaArchive().getSearcherManager().getSearcher("system", "user");
		userSearcher.reIndexAll();
		
		Data hit = (Data) userSearcher.searchByField("screenname", "Admin");
		assertEquals("screen name ","Admin",hit.get("screenname") );
		assertNotNull("screen Name is null" , hit);

		 hit = (Data) userSearcher.searchByField("screenname", "ADMIN");
		assertNull("screen name is not null", hit);

		hit = (Data) userSearcher.searchByField("lastName", "Administrator");
		assertNotNull("Last Name is null" , hit);
		
		String email = "support.openedit";
		
//		GetResponse response = userSearcher.getClient().prepareGet("entermedia_catalogs_testcatalog", "user", "admin")
//		        .execute()
//		        .actionGet();
//		Map fields = response.getFields();
//		String result = (String) fields.get("email");
//		User user = userSearcher.getUserByEmail(email);
		//assertEquals(email,	result);
		hit = (Data) userSearcher.searchByField("email", email);
		assertNotNull(hit);
		
	}

	@Test
	public void testGetUsersInGroup()
	{
		ElasticUserSearcher userSearcher = (ElasticUserSearcher) getMediaArchive().getSearcherManager().getSearcher("system", "user");
		Group group = new BaseGroup();
		group.setId("administrators");
		HitTracker hit = userSearcher.getUsersInGroup(group);
		assertTrue("no results", hit.size()>0);
	}

	@Test
	public void testSaveUsers()
	{
		ElasticUserSearcher userSearcher = (ElasticUserSearcher) getMediaArchive().getSearcherManager().getSearcher("system", "user");
		
		BaseUser user = (BaseUser) userSearcher.createNewData();
		user.setId("1");
		user.setName("test");
		
		//List<User> users = new ArrayList<User>();
		//users.add(usr);
		
		//userSearcher.saveUsers(users, usr);
		User admin = userSearcher.getUser("admin");
		userSearcher.saveData(user, admin);
		
		PageManager pManager = getMediaArchive().getPageManager();
		Page page = pManager.getPage("WEB-INF/data/system/users/1.xml");
		assertTrue("user file not found!", page.exists());
		
		//do a search to verify the user is in the index
		User test = userSearcher.getUser("1");
		assertNotNull("user not in index", test);
		
		//now get rid of the user
		userSearcher.delete(user, admin);
		
		//make sure the user is gone from the index
		test = userSearcher.getUser("1");
		assertNull("user still in index", test);
	}

}
