package model.projects;

import java.util.Collection;

import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;

import com.openedit.WebPageRequest;

public interface ProjectManager
{

	public abstract String getCatalogId();

	public abstract void setCatalogId(String inCatId);

	public Collection<UserCollection> loadCollections(WebPageRequest inReq);
	
	public void addAssetToLibrary(WebPageRequest inReq, MediaArchive archive, String libraryid, String assetid);

	public void addAssetToCollection(WebPageRequest inReq, MediaArchive archive, String libraryid, String assetid);
	
	public Collection<String> loadAssetsInCollection(WebPageRequest inReq, MediaArchive archive, String inCollectionId);
	
}