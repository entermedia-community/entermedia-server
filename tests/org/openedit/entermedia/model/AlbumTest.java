package org.openedit.entermedia.model;

import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.EnterMedia;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.albums.Album;
import org.openedit.entermedia.albums.AlbumSearcher;
import org.openedit.entermedia.modules.AlbumModule;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.User;

public class AlbumTest extends BaseEnterMediaTest
{

	/*
	 * These are new low-level test cases for new album stuff
	 */
	public void testAddMember() throws Exception
	{
		WebPageRequest req = getFixture().createPageRequest("/entermedia/index.html");
		EnterMedia em = getAlbumModule().getEnterMedia(req);
		
		HitTracker hits = em.getAlbumSearcher().searchForAlbums("testuser", true, req);
		int before = hits.size();
		
		Album album = createTestAlbum("new test album", req.getUser());
		
		User user = getFixture().getUserManager().getUser( "testuser");
		assertNotNull( user);
		album.addParticipant(user);
		
		assertEquals( album.getParticipants().size() , 2);
		
		em.getAlbumSearcher().saveData(album, req.getUser());
		
		
		hits = em.getAlbumSearcher().searchForAlbums("testuser", true, req);
		int after = hits.size();
		assertEquals(before + 1, after);
		
		album = em.getAlbumArchive().loadAlbum(album.getId(), album.getUserName());
		assertEquals( album.getParticipants().size() , 2);
		
		
	}
	
	
	public void testCreateAndDeleteAlbum() throws Exception
	{
		WebPageRequest req = getFixture().createPageRequest("/entermedia/index.html");
		EnterMedia em = getAlbumModule().getEnterMedia(req);
		
		Album album = createTestAlbum("new test album", req.getUser());
		assertEquals(0, album.size(req));
		Data data = (Data) em.getAlbumSearcher().searchById(album.getId());
		assertNotNull(data);
		assertEquals(album.getId(), data.getId());
		assertEquals(album.getName(), data.getName());
		
		em.getAlbumSearcher().delete(album, req.getUser());
		album = em.getAlbumArchive().loadAlbum(album.getId(), album.getUserName());
		assertNull(album);
	}
	
	public void testAddToAndRemoveFromAlbum() throws Exception
	{
		WebPageRequest req = getFixture().createPageRequest("/entermedia/index.html");
		EnterMedia em = getAlbumModule().getEnterMedia(req);
		
		MediaArchive archive = em.getMediaArchive("entermedia/catalogs/testcatalog");
		Asset asset = createAsset(archive);
		archive.saveAsset(asset, req.getUser());
		
		AlbumSearcher searcher = em.getAlbumSearcher();
		// Add asset to album; size should be 1
		Album album = createTestAlbum("test add to album", req.getUser());
		searcher.addAssetToAlbum(asset, album, req);
		//searcher.saveData(album, req.getUser());
		assertEquals(1, album.size(req));
		
		//remove the asset from the album; size should go back to 0
		searcher.removeAssetFromAlbum(asset, album, req);
		//searcher.saveData(album, req.getUser());
		assertEquals(0, album.size(req));
		
		//delete the album; assets in this album should be 0
		searcher.addAssetToAlbum(asset, album, req);
		searcher.saveData(album, req.getUser());
		searcher.delete(album, req.getUser());
		
		Searcher assetSearcher = archive.getAssetSearcher();
		SearchQuery query = assetSearcher.createSearchQuery();
		query.addMatches("album",album.getId());
		HitTracker result = assetSearcher.search(query);
		assertEquals(0, result.size());
	}
	
	/*
	 * Utilities
	 */
	protected AlbumModule getAlbumModule()
	{
		AlbumModule module = (AlbumModule)getFixture().getModuleManager().getModule("AlbumModule");
		return module;
	}
	
	public Album createTestAlbum( String inName, User inUser ) throws Exception
	{
		WebPageRequest req = getFixture().createPageRequest("/entermedia/index.html");
		EnterMedia em = getAlbumModule().getEnterMedia(req);
		Album album = em.getAlbumArchive().createAlbum();
		album.setName(inName);
		album.setUser(inUser);
		getEnterMedia().getAlbumSearcher().saveData(album, inUser);
		return album;
	}
	
}
