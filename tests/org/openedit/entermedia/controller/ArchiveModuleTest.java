package org.openedit.entermedia.controller;

import org.openedit.entermedia.BaseEnterMediaTest;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.users.User;
import com.openedit.users.filesystem.FileSystemUser;

public class ArchiveModuleTest extends BaseEnterMediaTest
{

	public ArchiveModuleTest(String inName)
	{
		super(inName);
	}
	
	//Depends on a guest group with 
	public void testShowAll() throws Exception 
	{
		//ArchiveModule mod = (ArchiveModule)getModule("ArchiveModule");
		WebPageRequest req = getFixture().createPageRequest("/entermedia/catalogs/testcatalog/categories/index.html");
		//User user = getFixture().getUserManager().getUser("guest");
		//user.clearGroups();
		User user = new FileSystemUser();
		user.put("datalevel","guest");
		user.put("viewarchive","true");
		user.setUserName("admin");
		req.setUser(user);

		//mod.createLogin(req);
		
		assertNotNull( req.getUser());
		
		//boolean has = req.getUser().hasPermission("limittocategory:test1");
		//assertTrue( "Did not have permission" , has );
		getFixture().getEngine().executePathActions(req);
				
		HitTracker hits = (HitTracker)req.getSessionValue("hitsassetentermedia/catalogs/testcatalog");
		assertNotNull(hits);
		assertTrue( hits.getQuery().contains("category:(index)") );
		
	}
}
