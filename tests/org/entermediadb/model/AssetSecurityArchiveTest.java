package org.entermediadb.model;

import java.util.List;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.openedit.WebPageRequest;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;

public class AssetSecurityArchiveTest  extends BaseEnterMediaTest
{
	/**
	 * @param arg0
	 */
	public AssetSecurityArchiveTest(String arg0)
	{
		super(arg0);
	}
	
	public void testEdit() throws Exception
	{
		Asset asset = getMediaArchive().getAssetBySourcePath("users/admin/101");
		asset.setProperty("owner",	"admin");
		List things = getMediaArchive().getAssetSecurityArchive().getAccessList(getMediaArchive(), asset);
		assertTrue(things.size() > 0);
		
		//make sure admin is in there and administrators
		assertTrue( things.contains("user_admin"));
		assertTrue( things.contains("group_administrators"));
		//assertTrue( things.contains("library_default"));
		
		getMediaArchive().getAssetSecurityArchive().grantAllAccess(getMediaArchive(), asset);
		things = getMediaArchive().getAssetSecurityArchive().getAccessList(getMediaArchive(), asset);
		assertTrue( things.contains("true"));
		
		getMediaArchive().getAssetSecurityArchive().clearAssetPermissions(getMediaArchive(), asset);
		things = getMediaArchive().getAssetSecurityArchive().getAccessList(getMediaArchive(), asset);
		assertTrue( things.contains("user_admin"));
		assertTrue( things.contains("group_administrators"));
		
	}

	public void testSearch() throws Exception
	{
		

		//make this asset public
		Asset asset = getMediaArchive().getAssetBySourcePath("users/admin/101");
		getMediaArchive().getAssetSecurityArchive().grantAllAccess(getMediaArchive(), asset);

		//Search for it		
		WebPageRequest req = getFixture().createPageRequest(getMediaArchive().getCatalogHome());
		req.setUser(null); //anonymous
		SearchQuery q = getMediaArchive().getAssetSearcher().createSearchQuery();
		q.addMatches("description", "*");
		HitTracker tracker = getMediaArchive().getAssetSearcher().cachedSearch(req,q);
		assertTrue( tracker.size() > 0);
		
		
		//Run it again, should be gone
		getMediaArchive().getAssetSecurityArchive().clearAssetPermissions(getMediaArchive(), asset);
		tracker = getMediaArchive().getAssetSearcher().cachedSearch(req,q);
		assertTrue( tracker.size() == 0);

		
		//TODO Test the library search feature
		
	}
}
