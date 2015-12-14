package org.entermediadb.asset;

import org.entermediadb.error.EmailErrorHandler;
import org.openedit.ModuleManager;
import org.openedit.data.SearcherManager;

public class EnterMedia
{
	protected String fieldApplicationId;
	protected ModuleManager fieldModuleManager;
	protected EmailErrorHandler fieldEmailErrorHandler;
	
	protected SearcherManager fieldSearcherManager;

	public EmailErrorHandler getEmailErrorHandler()
	{
		return fieldEmailErrorHandler;
	}

	public void setEmailErrorHandler(EmailErrorHandler emailErrorHandler)
	{
		fieldEmailErrorHandler = emailErrorHandler;
	}

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

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager searcherManager)
	{
		fieldSearcherManager = searcherManager;
	}
	
}
