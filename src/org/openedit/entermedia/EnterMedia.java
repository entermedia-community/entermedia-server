package org.openedit.entermedia;

import org.entermedia.error.EmailErrorHandler;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.albums.AlbumArchive;
import org.openedit.entermedia.albums.AlbumGroupManager;
import org.openedit.entermedia.albums.AlbumSearcher;
import org.openedit.entermedia.friends.FriendManager;

import com.openedit.ModuleManager;

public class EnterMedia
{
	protected String fieldApplicationId;
	protected ModuleManager fieldModuleManager;
	protected EmailErrorHandler fieldEmailErrorHandler;
	protected AlbumSearcher fieldAlbumSearcher;
	
	public FriendManager getFriendManager()
	{
		return (FriendManager) getModuleManager().getBean(getApplicationId(), "friendManager");
	}

	public AlbumGroupManager getAlbumGroupManager()
	{
		return (AlbumGroupManager) getModuleManager().getBean(getApplicationId(), "albumGroupManager");
	}

	protected SearcherManager fieldSearcherManager;

	public EmailErrorHandler getEmailErrorHandler()
	{
		return fieldEmailErrorHandler;
	}

	public void setEmailErrorHandler(EmailErrorHandler emailErrorHandler)
	{
		fieldEmailErrorHandler = emailErrorHandler;
	}

	protected AlbumArchive fieldAlbumArchive;

	public String getApplicationId()
	{
		return fieldApplicationId;
	}

	public void setApplicationId(String inApplicationId)
	{
		fieldApplicationId = inApplicationId;
	}

	public MediaArchive getMediaArchive(String inCatalogId)
	{
		if( inCatalogId == null)
		{
			return null;
		}
		return (MediaArchive) getModuleManager().getBean(inCatalogId, "mediaArchive");
	}

	public Asset getAsset(String inCatalogId, String inAssetId)
	{
		return getMediaArchive(inCatalogId).getAsset(inAssetId);
	}

	public Asset getAssetBySourcePath(String inCatalogId, String inSourcePath)
	{
		return getMediaArchive(inCatalogId).getAssetBySourcePath(inSourcePath);

	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public AlbumArchive getAlbumArchive()
	{
		if (fieldAlbumArchive == null)
		{
			fieldAlbumArchive = (AlbumArchive) getModuleManager().getBean("albumArchive"); // creates
			// a
			// new
			// one
			fieldAlbumArchive.setApplicationId(getApplicationId());
		}
		return fieldAlbumArchive;
	}

	public void setAlbumArchive(AlbumArchive inAlbumArchive)
	{
		fieldAlbumArchive = inAlbumArchive;
	}

	public AlbumSearcher getAlbumSearcher()
	{
		if (fieldAlbumSearcher == null)
		{
			fieldAlbumSearcher = (AlbumSearcher) getSearcherManager().getSearcher(getApplicationId(), "album");
		}
		return fieldAlbumSearcher;
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager searcherManager)
	{
		fieldSearcherManager = searcherManager;
	}
	
}
