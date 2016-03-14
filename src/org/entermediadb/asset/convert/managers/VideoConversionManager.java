package org.entermediadb.asset.convert.managers;

import org.entermediadb.asset.convert.BaseConversionManager;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.repository.ContentItem;

public class VideoConversionManager extends BaseConversionManager
{
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
			outputpage.append("video");
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


	public ConvertResult transcode(ConvertInstructions inStructions)
	{
		
		//if output == jpg and no time offset - standard
		if(inStructions.getOutputRenderType().equals("video"))
		{
			return findTranscoder(inStructions).convert(inStructions);
		}
		Data preset = getMediaArchive().getPresetManager().getPresetByOutputName(inStructions.getMediaArchive(),"video","video.mp4");
		ConvertInstructions proxyinstructions = createInstructions(inStructions.getAsset(), preset);
		if( !proxyinstructions.getOutputFile().exists() )
		{
			ConvertResult result = findTranscoderByPreset(preset).convert(proxyinstructions);
			if(!result.isComplete())
			{
				throw new OpenEditException("Could not create proxy");
			}
		}
		
		preset = getMediaArchive().getPresetManager().getPresetByOutputName(inStructions.getMediaArchive(),"video","image1024x768.jpg");
		ConvertInstructions instructions2 = createInstructions(inStructions.getAsset(), preset);
		if( !instructions2.getOutputFile().exists() )
		{
			instructions2.setInputFile( proxyinstructions.getOutputFile() );
			ConvertResult result = findTranscoderByPreset(preset).convert(instructions2);
			if(!result.isComplete())
			{
				throw new OpenEditException("Could not create large");
			}
		}

		inStructions.setInputFile(instructions2.getOutputFile());
		ConvertResult result = findTranscoderByPreset(inStructions.getConvertPreset()).convert(inStructions);
		return result;
	}
	
	protected String getRenderType()
	{
		return "video";
	}

}
