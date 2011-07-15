package org.openedit.entermedia.controller;

import java.util.ArrayList;
import java.util.List;

import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.modules.AssetControlModule;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.ListHitTracker;
import com.openedit.users.BaseUser;
import com.openedit.users.User;

public class AssetControlModuleTest extends BaseEnterMediaTest
{

	public void testListAssetGetTypeAheadPeople() throws Exception
	{
		AssetControlModule module = (AssetControlModule) getFixture().getModuleManager().getModule("AssetControlModule");
		WebPageRequest req = getFixture().createPageRequest("/testcatalog/views/assets/index.html?assetid=101");
		assertNotNull("assetid is NULL!", req.getRequestParameter("assetid"));
		List peoples = module.listAssetViewPermissions(req);
		assertNotNull("people are NULL!", peoples);
		assertTrue("not the right ammount of users!", peoples.size() == 1);
	}

	public void testfindUsersByName() throws Exception
	{
		AssetControlModule module = (AssetControlModule) getFixture().getModuleManager().getModule("AssetControlModule");
		List<User> users = null;
		List names = new ArrayList();
		names.add("admin");
		users = module.findUsersByName(names);
		assertNotNull("users are NULL!", users);
		assertTrue(users.size() > 0);
	}

}
