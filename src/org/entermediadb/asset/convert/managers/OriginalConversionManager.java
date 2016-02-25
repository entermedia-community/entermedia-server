package org.entermediadb.asset.convert.managers;

import java.util.HashMap;

import org.entermediadb.asset.convert.BaseConversionManager;
import org.entermediadb.asset.convert.ConversionManager;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.convert.TranscodeTools;
import org.joda.time.convert.ConverterManager;
import org.entermediadb.asset.convert.MediaTranscoder;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;

public class OriginalConversionManager extends BaseConversionManager
{
	//To create the file we need to Look for input in several places
	//CR 1024x768
	//Custom thumb
	//document.pdf
	//video.mp4
	//Original file
	
	public ContentItem findOutputFile(ConvertInstructions inStructions)
	{
		Page page = getMediaArchive().getOriginalDocument(inStructions.getAsset());
		return page.getContentItem();
	}

	protected String getCacheName()
	{
		return "original";
	}

	protected ContentItem createCacheFile(ConvertInstructions inStructions, ContentItem input)
	{
		Page page = getMediaArchive().getOriginalDocument(inStructions.getAsset());
		return page.getContentItem();
	}

	@Override
	public ConvertResult createOutput(ConvertInstructions inStructions)
	{
		// TODO Auto-generated method stub
		ConvertResult result = new ConvertResult();
		Page page = getMediaArchive().getOriginalDocument(inStructions.getAsset());
		result.setComplete(true);
		result.setOutput(page.getContentItem());
		result.setOk(true);
		return result;
	}
	
	

}
