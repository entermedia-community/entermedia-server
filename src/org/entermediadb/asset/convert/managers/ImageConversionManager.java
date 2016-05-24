package org.entermediadb.asset.convert.managers;

import org.entermediadb.asset.convert.BaseConversionManager;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.convert.transcoders.WaterMarkTranscoder;

public class ImageConversionManager extends BaseConversionManager
{
	protected WaterMarkTranscoder fieldWaterMarkTranscoder;
	//To create the file we need to Look for input in several places
	//CR 1024x768
	//Custom thumb
	//document.pdf
	//video.mp4
	//Original file
	
	public WaterMarkTranscoder getWaterMarkTranscoder()
	{
		if (fieldWaterMarkTranscoder == null)
		{
			fieldWaterMarkTranscoder = 	(WaterMarkTranscoder)getMediaArchive().getModuleManager().getBean(getMediaArchive().getCatalogId(),"waterMarkTranscoder");
		}
		return fieldWaterMarkTranscoder;
	}


	public void setWaterMarkTranscoder(WaterMarkTranscoder inWaterMarkTranscoder)
	{
		fieldWaterMarkTranscoder = inWaterMarkTranscoder;
	}


	//	protected ContentItem createCacheFile(ConvertInstructions inStructions, ContentItem input)
//	{
//			
//		
//			TranscodeTools creatorManager = inStructions.getMediaArchive().getTranscodeTools();
//			HashMap map = new HashMap();
//			map.put("prefwidth", "1024");
//			map.put("prefheight", "768");
//			map.put("outputextension", "jpg");
//			Data preset = getMediaArchive().getPresetManager().getPresetByOutputName(inStructions.getMediaArchive(),"image","image1024x768.jpg");
//
//			ConvertInstructions proxyinstructions = createInstructions(inStructions.getAsset(), preset);
//			
//			proxyinstructions.setInputFile(inStructions.getOriginalDocument());
//			ConvertResult result = findTranscoderByPreset(preset).convert(proxyinstructions);
//			return result.getOutput();
//	}
//
    protected ConvertResult transcode(ConvertInstructions inStructions)
    {
    	ConvertResult result = super.transcode(inStructions);
    	if(inStructions.isWatermark())
    	{
    		inStructions.setInputFile(inStructions.getOutputFile());
    		result = getWaterMarkTranscoder().convert(inStructions);
    	}
    	return result;
    }

	
	protected String getRenderType()
	{
		return "image";
	}


}
