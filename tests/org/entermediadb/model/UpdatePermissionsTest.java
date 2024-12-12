package org.entermediadb.model;

import java.util.Date;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.users.PermissionManager;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.users.BaseUser;
import org.openedit.users.User;

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
		getMediaArchive().getSearcher("module").reIndexAll();
		
		MultiValued parentmodule = (MultiValued)getMediaArchive().getCachedData("module","parentmodule");
		
		
		parentmodule.setValue("permissionsupdateddate",new Date() );
		parentmodule.setValue("defaultusers",null);
		
		getMediaArchive().saveData("module",parentmodule);
		
		PermissionManager manager = (PermissionManager)getMediaArchive().getBean("permissionManager");
		manager.handleModulePermissionsUpdated(); 
		//Make sure all is clear
		Category cat = getMediaArchive().getEntityManager().loadDefaultFolderForModule(parentmodule, null);
		assertEquals(null, cat.getValue("viewusers"));

		//Make sure child is clear
		MultiValued  childrecord = (MultiValued)getMediaArchive().getSearcher("parentmodulechild").createNewData();
		childrecord.setValue("parentmodule",parentmodule.getId());
		parentmodule.setValue("defaultusers",null);
		getMediaArchive().saveData("parentmodulechild",childrecord);
		manager.handleModulePermissionsUpdated();
		assertEquals(null, childrecord.getValue("defaultusers"));
		
		
		//Now set the parent to something
		parentmodule.addValue("defaultusers","admin");
		parentmodule.setValue("permissionsupdateddate",new Date() );
		getMediaArchive().saveData("module",parentmodule);
		manager.handleModulePermissionsUpdated();
		
		
//		childrecord = (MultiValued)getMediaArchive().getData("parentmodulechild", childrecord.getId()); //Reload
//		assertTrue(childrecord.getValue("viewusers") != null);
//		assertTrue(childrecord.containsValue("viewusers","admin"));
		
	}

	public void testSearch() throws Exception
	{
		MediaArchive archive = getMediaArchive();
		archive.getSearcherManager().setShowSearchLogs(archive.getCatalogId(),true);
		archive.setCatalogSettingValue("log_all_searches","true");
		
		WebPageRequest req = getFixture().createPageRequest("/index.html");
		User testuser = new BaseUser();
		testuser.setId("testuser");
		
		User external = new BaseUser();
		external.setId("externaluser");	

		PermissionManager manager = (PermissionManager)getMediaArchive().getBean("permissionManager");

		MultiValued parentmodule = (MultiValued)getMediaArchive().getCachedData("module","parentmodule");
		parentmodule.setValue("autocreatestartingpath", "parentmodule");		
		parentmodule.setValue("defaultusers","testuser");
		parentmodule.setValue("permissionsupdateddate",new Date() );
		getMediaArchive().saveData("module",parentmodule);
		
		manager.handleModulePermissionsUpdated();

		Category cat = getMediaArchive().getCategorySearcher().createCategoryPath("parentmodule/assets/security");
		Asset childrecord = (Asset)getMediaArchive().query("asset").exact("id", "lockedasset").searchOne();
		
		if( childrecord  != null )
		{
			getMediaArchive().deleteAsset(childrecord, false);
		}
		
		childrecord = (Asset)getMediaArchive().getSearcher("asset").createNewData();
		childrecord.setId("lockedasset");
		childrecord.setSourcePath("parentmodule/assets/security");
		childrecord.addCategory(cat);
		childrecord.setValue("editstatus", 6);
		getMediaArchive().saveData("asset",childrecord);	
		
		req.setUser(testuser);		
		
		
		Data testchildrecord = (MultiValued)getMediaArchive().query("asset").enduser(true).exact("id", "lockedasset").searchOne(req);
		assertNotNull("Should have been shown to TestUser",testchildrecord);

		req.setUser(external);
		
		testchildrecord = (MultiValued)getMediaArchive().query("asset").enduser(true).exact("id", "lockedasset").searchOne(req);
		assertNull("Should not have been shown to TestUser",testchildrecord);
		
		
		parentmodule.addValue("defaultusers","externaluser");
		parentmodule.setValue("permissionsupdateddate",new Date() );
		getMediaArchive().saveData("module",parentmodule);
		
		manager.handleModulePermissionsUpdated();

		
		getMediaArchive().saveData("asset",childrecord);	

		
		testchildrecord = (MultiValued)getMediaArchive().query("asset").enduser(true).exact("id", "lockedasset").searchOne(req);

		assertNotNull("Should  have been shown",testchildrecord);
		
	}
}
