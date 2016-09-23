package org.entermediadb.projects;

import java.util.Collection;
import java.util.Map;

import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.hittracker.HitTracker;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;

public interface ProjectManager
{
	public Data getCurrentLibrary( UserProfile inProfile);

	public abstract String getCatalogId();

	public abstract void setCatalogId(String inCatId);

	public Collection<UserCollection> loadCollections(WebPageRequest inReq, MediaArchive archive);
	
	public void addAssetToLibrary(MediaArchive archive, String libraryid, String assetid);

	public void addAssetToLibrary(MediaArchive archive, String libraryid, HitTracker inAssets );

	public void addAssetToCollection(MediaArchive archive, String libraryid, String collectionid, HitTracker inAssets );
	public void addAssetToCollection(MediaArchive archive, String collectionid, HitTracker inAssets );
	public void addAssetToCollection(MediaArchive archive, String libraryid, String collectionid, String assetid);
	public void addAssetToCollection(MediaArchive archive, String collectionid, String assetid);

	public Data addCategoryToCollection(User inUser, MediaArchive archive, String collectionid, String categoryid );
	
//	public void removeCategoryFromCollection(MediaArchive archive, String collectionid, String categoryid );

	public void removeAssetFromLibrary(MediaArchive archive, String libraryid, HitTracker inAssets );

	public void removeAssetFromCollection(MediaArchive archive, String collectionid, HitTracker inAssets );

	public HitTracker loadAssetsInCollection(WebPageRequest inReq, MediaArchive archive, String inCollectionId);
	
	public boolean addUserToLibrary(MediaArchive archive, Data inLibrary, User inUser);
	
	public HitTracker loadAssetsInLibrary(Data inLibrary,  MediaArchive archive, WebPageRequest inReq);

	public Collection<UserCollection> loadOpenCollections(WebPageRequest inReq);
	
	public HitTracker loadCategoriesOnCollection(MediaArchive inArchive, String inCollectionid);

	//public void importCollection(WebPageRequest inReq, MediaArchive inArchive, String inCollectionid);
	
	//public String exportCollectionTo(WebPageRequest inReq, MediaArchive inArchive, String inCollectionid, String inLibraryid);
	//public void moveCollectionTo(WebPageRequest inReq, MediaArchive inArchive, String inCollectionid, String inLibraryid);

	//public Map loadFileSizes(WebPageRequest inReq, MediaArchive inArchive, String inCollectionid);

	//public void savedCollection(MediaArchive archive, Data inLibrary, User inUser);
	
	public void loadCategoriesOnCollections(MediaArchive inArchive, Collection inCollections);
	
	public Data loadUserLibrary(MediaArchive inArchive, UserProfile inProfile);
	public void snapshotAndImport(WebPageRequest inReq, User inUser, MediaArchive inArchive,  String inCollectionid, String inImportPath);

}