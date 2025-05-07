package org.entermediadb.asset.convert.managers;

import org.entermediadb.asset.convert.BaseConversionManager;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.openedit.Data;

public class VideoConversionManager extends BaseConversionManager
{
//	public ContentItem findOutputFile(ConvertInstructions inStructions)
//	{
//		StringBuffer outputpage = new StringBuffer();
//		outputpage.append("/WEB-INF/data/" );
//		outputpage.append(getMediaArchive().getCatalogId());
//		outputpage.append("/generated/" );
//		outputpage.append(inStructions.getAssetSourcePath() );
//		outputpage.append("/" );
//		String cachefilename = inStructions.get("cachefilename");
//		if( cachefilename == null)
//		{
//			outputpage.append("video");
//			if( inStructions.isWatermark() )
//			{
//				outputpage.append("wm");
//			}
//			
//			if( inStructions.get("timeoffset") != null)
//			{
//				//TODO: deal with this
//			}
//			String frame = inStructions.getProperty("frame");
//			if( frame != null)
//			{
//				outputpage.append("frame" + frame );
//			}
//			
//			outputpage.append(".mp4");
//		}
//		else
//		{
//			outputpage.append(cachefilename);
//		}
////		String output = inPreset.get("outputfile");
////		int pagenumber = inStructions.getPageNumber();
////		if( pagenumber > 1 )
////		{
////			String name = PathUtilities.extractPageName(output);
////			String ext = PathUtilities.extractPageType(output);
////			output = name + "page" + pagenumber + "." + ext;
////		}
////		outputpage.append(output);
//
//		return getMediaArchive().getContent( outputpage.toString() );
//	}

/**
 * 
 */
	public ConvertResult transcode(ConvertInstructions inStructions)
	{
		//if output == jpg and no time offset - standard
		String outputRenderType = inStructions.getOutputRenderType();
		String inputRenderType = inStructions.getInputPath();
		if(outputRenderType.equals("video") || outputRenderType.equals("audio")  )
		{
			return findTranscoder(inStructions).convertIfNeeded(inStructions);
		} 
		 
		//Do the video conversion first. Then do the standard image conversion
		
		Data preset = getMediaArchive().getPresetManager().getPresetByOutputName(inStructions.getMediaArchive(),"video","video.m3u8");
		if( preset == null || !Boolean.parseBoolean( preset.get("onimport")) )
		{
			//legacy
			preset = getMediaArchive().getPresetManager().getPresetByOutputName(inStructions.getMediaArchive(),"video","video.mp4");
		}
		ConvertInstructions proxyinstructions = createInstructions(inStructions.getAsset(),preset);
		if( proxyinstructions.getInputFile() == null)
		{
			proxyinstructions.setInputFile(getMediaArchive().getOriginalContent(inStructions.getAsset()));
		}
		ConvertResult result = findTranscoder(proxyinstructions).convertIfNeeded(proxyinstructions);
		if(!result.isComplete())
		{
			return result;
		}
		//Video done, now create preview image
		
		
		ConvertInstructions instructions2;
		String defaultpresetoutput = "image1900x1080.webp";
		Data imagepreviewpreset = getMediaArchive().getPresetManager().getPresetByOutputName(inStructions.getMediaArchive(),"video", defaultpresetoutput);
		if (imagepreviewpreset == null) 
		{
			defaultpresetoutput = "image1900x1080.jpg";
			imagepreviewpreset = getMediaArchive().getPresetManager().getPresetByOutputName(inStructions.getMediaArchive(),"video", defaultpresetoutput);
		}
		if (imagepreviewpreset == null) 
		{
			defaultpresetoutput = "image1024x768.jpg";
			imagepreviewpreset = getMediaArchive().getPresetManager().getPresetByOutputName(inStructions.getMediaArchive(),"video", defaultpresetoutput);
		}
		if( imagepreviewpreset != null && inStructions.getTimeOffset() == null)
		{
			//Create the Preview Image using video as the input
			instructions2 = createInstructions(inStructions.getAsset(), imagepreviewpreset);
			instructions2.setProperty("outputfile", defaultpresetoutput);
			instructions2.setOutputFile(null);
		}
		else
		{
			instructions2 = inStructions.copy(imagepreviewpreset);
			
		}
		instructions2.setInputFile( proxyinstructions.getOutputFile() );
		result = findTranscoder(instructions2).convertIfNeeded(instructions2);
		if(!result.isComplete())
		{
			return result;
		}
		
		//Finally use ImageMagick to transform the final image using the image above as the input
		//preset = getMediaArchive().getPresetManager().getPresetByOutputName(inStructions.getMediaArchive(),"image","image1024x768.jpg");
		//ConvertInstructions IMinstructions = inStructions.copy(preset);
		//IMinstructions.setMaxScaledSize(inStructions.getMaxScaledSize()); 
		inStructions.setInputFile(instructions2.getOutputFile());
		result = findTranscoder(inStructions).convertIfNeeded(inStructions);
		if(inStructions.isWatermark())
    	{
    		inStructions.setInputFile(inStructions.getOutputFile());
    		result = getWaterMarkTranscoder().convert(inStructions);
    	}
		
		
		if(!result.isComplete())
		{
			return result;
		}
		return result;
		
	}
	
	protected String getRenderType()
	{
		return "video";
	}

}
