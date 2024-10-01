package org.entermediadb.controller;

import java.util.ArrayList;
import java.util.List;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.modules.AssetControlModule;
import org.openedit.WebPageRequest;
import org.openedit.users.User;

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
		WebPageRequest req = getFixture().createPageRequest("/testcatalog/views/assets/index.html?assetid=101");

		users = module.findUsersByName(req,names);
		assertNotNull("users are NULL!", users);
		assertTrue(users.size() > 0);
	}

}
