package org.openedit.entermedia.albums;

import java.util.Collection;

import org.openedit.data.Searcher;
import org.openedit.entermedia.Asset;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.CompositeHitTracker;
import com.openedit.hittracker.HitTracker;

public interface AlbumSearcher extends Searcher {

	public void addAssetToAlbum(Asset inAsset, Album inAlbum, WebPageRequest inReq);
	public void removeAssetFromAlbum(Asset inAsset, Album inAlbum, WebPageRequest inReq);
	public void addAssetsToAlbum(Collection<Asset> inAssets, Album inAlbum, WebPageRequest inReq);
	public void removeAssetsFromAlbum(Collection<Asset> inAssets, Album inAlbum, WebPageRequest inReq);
	public void clearAlbum(Album inAlbum, WebPageRequest inReq);
	public Album getAlbum(String inAlbumId, String inUserName);
	public CompositeHitTracker getAlbumItems(String inAlbumId, String inUserName, String inHitsName, WebPageRequest inReq);
	public CompositeHitTracker getAlbumItems(String inAlbumId, String inUserName, WebPageRequest inReq);
	public HitTracker getAssets(String inCatalogId, String inAlbumId,String inUserName, String inHitsName, WebPageRequest inReq);
	public HitTracker searchForAlbums(String inOwner, boolean asParticipant, WebPageRequest inReq);
	public Album createAlbum(String albumid);
}