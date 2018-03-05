package org.entermediadb.video;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.google.GoogleManager;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.data.Searcher;

public class CloudTrancodeManager implements CatalogEnabled
{
	protected String fieldCatalogId;
	protected ModuleManager fieldModuleManager;
	protected MediaArchive fieldMediaArchive;
	protected GoogleManager fieldGoogleManager;
	
	public GoogleManager getGoogleManager()
	{
		return fieldGoogleManager;
	}


	public void setGoogleManager(GoogleManager inGoogleManager)
	{
		fieldGoogleManager = inGoogleManager;
	}


	public MediaArchive getMediaArchive()
	{
		if (fieldMediaArchive == null)
		{
			fieldMediaArchive = (MediaArchive)getModuleManager().getBean(getCatalogId(),"mediaArchive");
		}
		return fieldMediaArchive;
	}


	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}


	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}


	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}


	public String getCatalogId()
	{
		return fieldCatalogId;
	}


	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}


	public void transcodeCaptions(Asset inAsset, String inLang)
	{
		//start saving the transcode into the database
		Searcher tracksearcher = getMediaArchive().getSearcher("videotrack");
		Data lasttrack = tracksearcher.query().exact("assetid", inAsset.getId()).exact("sourcelang", inLang).searchOne();
		tracksearcher.delete(lasttrack, null);
	}
	
}
