package org.entermediadb.asset.trancode;

import java.util.List;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.openedit.repository.ContentItem;

public abstract class BaseHandler implements TranscodeHandler
{
	protected MediaArchive fieldMediaArchive;
	protected List fieldInputLoaders;
	
	public List getInputLoaders()
	{
		return fieldInputLoaders;
	}

	public void setInputLoaders(List inInputLoaders)
	{
		fieldInputLoaders = inInputLoaders;
	}

	public MediaArchive getMediaArchive()
	{
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}
	protected abstract ContentItem findOutputFile(ConvertInstructions inStructions);

}
