package org.entermediadb.model;

import java.util.Date;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.Category;
import org.entermediadb.users.PermissionManager;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;

public class UpdatePermissionsTest  extends BaseEnterMediaTest
{
	/**
	 * @param arg0
	 */
	public UpdatePermissionsTest(String arg0)
	{
		super(arg0);
	}
	
	public void testFixModuleChildren() throws Exception
	{
		MultiValued parentmodule = (MultiValued)getMediaArchive().getCachedData("module","parentmodule");
		parentmodule.setValue("users",null);
		getMediaArchive().saveData("module",parentmodule);
		PermissionManager manager = (PermissionManager)getMediaArchive().getBean("permissionManager");
		manager.handleModulePermissionsUpdated(); 
		//Make sure all is clear
		Category cat = getMediaArchive().getEntityManager().loadDefaultFolderForModule(parentmodule, null);
		assertEquals(null, cat.getValue("viewusers"));

		//Make sure child is clear
		MultiValued  childrecord = (MultiValued)getMediaArchive().getSearcher("parentmodulechild").createNewData();
		childrecord.setValue("parentmodule",parentmodule.getId());
		parentmodule.setValue("users",null);
		getMediaArchive().saveData("parentmodulechild",childrecord);
		manager.handleModulePermissionsUpdated();
		assertEquals(null, childrecord.getValue("viewusers"));
		
		
		//Now set the parent to something
		parentmodule.addValue("viewusers","admin");
		parentmodule.setValue("permissionsupdateddate",new Date() );
		getMediaArchive().saveData("module",parentmodule);
		manager.handleModulePermissionsUpdated();
		
		
		childrecord = (MultiValued)getMediaArchive().getData("parentmodulechild", childrecord.getId()); //Reload
		assertTrue(childrecord.getValue("viewusers") != null);
		assertTrue(childrecord.containsValue("viewusers","admin"));
		
	}

	public void testSearch() throws Exception
	{
		WebPageRequest req = getFixture().createPageRequest("/index.html");
		req.setUser(getFixture().getUserManager().getUser("testuser"));
		
		Data childrecord = (MultiValued)getMediaArchive().query("parentmodulechild").all().searchOne();
		if( childrecord  == null )
		{
			childrecord = (MultiValued)getMediaArchive().getSearcher("parentmodulechild").createNewData();
		}
		childrecord.setValue("users",null);
		getMediaArchive().saveData("parentmodulechild",childrecord);
		PermissionManager manager = (PermissionManager)getMediaArchive().getBean("permissionManager");
		manager.handleModulePermissionsUpdated();
		
		Data testchildrecord = (MultiValued)getMediaArchive().query("parentmodulechild").enduser(true).all().searchOne(req);

		assertNull("Should not have been shown",testchildrecord);
		
	}
}
