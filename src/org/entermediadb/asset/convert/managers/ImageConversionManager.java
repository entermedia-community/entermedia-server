package org.entermediadb.asset.convert.managers;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.convert.BaseConversionManager;
import org.entermediadb.asset.convert.BaseTranscoder;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.convert.MediaTranscoder;
import org.entermediadb.asset.convert.transcoders.CMYKTranscoder;
import org.entermediadb.asset.convert.transcoders.WaterMarkTranscoder;
import org.openedit.repository.ContentItem;

public class ImageConversionManager extends BaseConversionManager
{
	protected WaterMarkTranscoder fieldWaterMarkTranscoder;
	protected BaseTranscoder fieldCMYKTranscoder;
	protected MediaTranscoder fieldExiftoolThumbTranscoder;
	//To create the file we need to Look for input in several places
	//CR 1024x768
	//Custom thumb
	//document.pdf
	//video.mp4
	//Original file
	
	public BaseTranscoder getCMYKTranscoder()
	{
		if (fieldCMYKTranscoder == null)
		{
			fieldCMYKTranscoder = (BaseTranscoder)getMediaArchive().getModuleManager().getBean(getMediaArchive().getCatalogId(),"cmykTranscoder");
		}

		return fieldCMYKTranscoder;
	}


	public void setCMYKTranscoder(CMYKTranscoder inCMYKTranscoder)
	{
		fieldCMYKTranscoder = inCMYKTranscoder;
	}


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
	
	public MediaTranscoder getExiftoolThumbTranscoder()
	{
		return fieldExiftoolThumbTranscoder;
	}

	public void setExiftoolThumbTranscoder(MediaTranscoder inExiftoolThumbTranscoder)
	{
		fieldExiftoolThumbTranscoder = inExiftoolThumbTranscoder;
	}
	
    protected ConvertResult transcode(ConvertInstructions inStructions)
    {
    	ContentItem input = makeCustomInput(getCMYKTranscoder(),"jpg",inStructions);
    	if( input != null)
    	{
    		inStructions.setInputFile(input);
    	}
    	else
    	{
    		input = makeIndd(getExiftoolThumbTranscoder(),inStructions);
    		if( input != null)
        	{
        		inStructions.setInputFile(input);
        	}
    	}
    	ConvertResult result = super.transcode(inStructions);
    	if(inStructions.isWatermark())
    	{
    		inStructions.setInputFile(inStructions.getOutputFile());
    		result = getWaterMarkTranscoder().convert(inStructions);
    	}
    	return result;
    }



	
	private ContentItem makeIndd(MediaTranscoder inExiftoolThumbTranscoder, ConvertInstructions inStructions)
	{
		Asset asset = inStructions.getAsset();
		String format = asset.getFileFormat();
		if ("indd".equalsIgnoreCase(format))  //TODO: Move to image
		{
			//log.info("Extracting thumb from "+ inStructions.getInputFile().getAbsolutePath() );

			ContentItem custom = getMediaArchive().getContent( "/WEB-INF/data/" + getMediaArchive().getCatalogId() + "/generated/" + asset.getSourcePath() + "/customthumb.jpg");
			if( custom.exists() )
			{
				return custom;
			}
			//if we have embdeded thumb 
			ConvertInstructions instructions = new ConvertInstructions(getMediaArchive());
			instructions.setForce(true);
			instructions.setInputFile(instructions.getInputFile());
			instructions.setOutputFile(custom);
			ConvertResult res = inExiftoolThumbTranscoder.convert(instructions);
			if (res.isOk())
			{
				return custom;
			}
		}
		
		return null;
	}


	protected String getRenderType()
	{
		return "image";
	}


}
