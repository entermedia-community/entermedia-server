package org.entermediadb.asset.convert.managers;

import java.util.HashMap;

import org.entermediadb.asset.convert.BaseConversionManager;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.convert.MediaTranscoder;
import org.openedit.Data;
import org.openedit.repository.ContentItem;

public class VideoConversionManager extends BaseConversionManager
{
	

	protected MediaTranscoder fieldVideoImageTranscoder;
	protected MediaTranscoder fieldImageTranscoder;
	
	
	
	
	
	
	public ContentItem findOutputFile(ConvertInstructions inStructions)
	{
		StringBuffer outputpage = new StringBuffer();
		outputpage.append("/WEB-INF/data/" );
		outputpage.append(getMediaArchive().getCatalogId());
		outputpage.append("/generated/" );
		outputpage.append(inStructions.getAssetSourcePath() );
		outputpage.append("/" );
		String cachefilename = inStructions.get("cachefilename");
		if( cachefilename == null)
		{
			outputpage.append(getCacheName());
			if( inStructions.isWatermark() )
			{
				outputpage.append("wm");
			}
			//if( inStructions.get)
			outputpage.append(".mp4");
		}
		else
		{
			outputpage.append(cachefilename);
		}
//		String output = inPreset.get("outputfile");
//		int pagenumber = inStructions.getPageNumber();
//		if( pagenumber > 1 )
//		{
//			String name = PathUtilities.extractPageName(output);
//			String ext = PathUtilities.extractPageType(output);
//			output = name + "page" + pagenumber + "." + ext;
//		}
//		outputpage.append(output);

		return getMediaArchive().getContent( outputpage.toString() );
	}

	@Override
	protected ContentItem createCacheFile(ConvertInstructions inStructions, ContentItem inInput)
	{
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	protected String getCacheName()
	{
		return "video";
	}
	
	
	
	
	public MediaTranscoder getVideoImageTranscoder()
	{
		return fieldVideoImageTranscoder;
	}

	public void setVideoImageTranscoder(MediaTranscoder inVideoImageTranscoder)
	{
		fieldVideoImageTranscoder = inVideoImageTranscoder;
	}

	public MediaTranscoder getImageTranscoder()
	{
		return fieldImageTranscoder;
	}

	public void setImageTranscoder(MediaTranscoder inImageTranscoder)
	{
		fieldImageTranscoder = inImageTranscoder;
	}

	public ConvertResult transcode(ConvertInstructions inStructions)
	{
	
		
		//if output == jpg and no time offset - standard
		if(inStructions.getOutputRenderType().equals("video")){
			return getMediaTranscoder().convert(inStructions);

		}
	
		ContentItem input = inStructions.getMediaArchive().getContent("/WEB-INF/data/" + inStructions.getMediaArchive().getCatalogId() + "/generated/" + inStructions.getAssetSourcePath() + "/video.mp4");
		if (input != null && input.getLength() < 2 && input.exists())
		{
			//TODO: Save the fact that we used a cached file
			//make some new/better instructions?
			Data preset = getMediaArchive().getPresetManager().getPresetByOutputName("video.mp4");
			HashMap map = new HashMap();
			ConvertInstructions proxyinstructions = createInstructions(map, inStructions.getAsset(), preset);
			ConvertResult result = getMediaTranscoder().convert(proxyinstructions);
			if(result.isOk() && result.isComplete()){
				input = result.getOutput();
			}
			
		}
		
		
		ContentItem imageinput = inStructions.getMediaArchive().getContent("/WEB-INF/data/" + inStructions.getMediaArchive().getCatalogId() + "/generated/" + inStructions.getAssetSourcePath() + "/image1024x768.jpg");
		if (imageinput != null && imageinput.getLength() < 2 && imageinput.exists())
		{

			inStructions.setInputFile(input);
			ConvertResult result = getVideoImageTranscoder().convert(inStructions);
			if(result.isOk() && result.isComplete()){
				input = result.getOutput();
			}
			
		}
		
		inStructions.setInputFile(imageinput);
		getImageTranscoder().convert(inStructions);
		
		
		
		
		
		
		
		
		
		
		
		
	
	
	}
	

}
