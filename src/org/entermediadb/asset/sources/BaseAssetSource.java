package org.entermediadb.asset.sources;

import java.util.ArrayList;
import java.util.List;

import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.data.Searcher;

public abstract class BaseAssetSource implements AssetSource
{
	protected List fieldImportLogs;
	protected Searcher fieldFolderSearcher;
	
	public Searcher getFolderSearcher()
	{
		return getMediaArchive().getSearcher("hotfolder");
	}

	

	public List getImportLogs()
	{
		if (fieldImportLogs == null)
		{
			fieldImportLogs = new ArrayList();
		}
		return fieldImportLogs;
	}

	public void setImportLogs(List inImportLogs)
	{
		fieldImportLogs = inImportLogs;
	}

	protected MediaArchive fieldMediaArchive;
	protected Data fieldConfig;
	
	public Data getConfig()
	{
		return fieldConfig;
	}

	public void setConfig(Data inConfig)
	{
		fieldConfig = inConfig;
	}

	public MediaArchive getMediaArchive()
	{
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}

	protected String getExternalPath()
	{
		return getConfig().get("externalpath");
	}

	protected String getFolderPath()
	{
		return getConfig().get("subfolder");
	}

	
}
