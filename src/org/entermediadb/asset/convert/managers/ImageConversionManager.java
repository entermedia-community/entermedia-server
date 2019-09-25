package org.entermediadb.asset.convert.managers;

import java.util.ArrayList;
import java.util.List;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.convert.BaseConversionManager;
import org.entermediadb.asset.convert.BaseTranscoder;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.convert.MediaTranscoder;
import org.entermediadb.asset.convert.transcoders.CMYKTranscoder;
import org.entermediadb.asset.convert.transcoders.WaterMarkTranscoder;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.util.ExecResult;

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
			fieldCMYKTranscoder = (BaseTranscoder) getMediaArchive().getModuleManager().getBean(getMediaArchive().getCatalogId(), "cmykTranscoder");
		}

		return fieldCMYKTranscoder;
	}

	public void setCMYKTranscoder(CMYKTranscoder inCMYKTranscoder)
	{
		fieldCMYKTranscoder = inCMYKTranscoder;
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
		ContentItem input = makeCustomInput(getCMYKTranscoder(), "jpg", inStructions);
		if (input != null)
		{
			inStructions.setInputFile(input);
		}
		else
		{
			input = makeIndd(getExiftoolThumbTranscoder(), inStructions);
			if (input != null)
			{
				inStructions.setInputFile(input);
			}
		}

		Page alternativeprofile = findProfileForAsset(inStructions);
		if (alternativeprofile != null)
		{
			inStructions.setImageProfile(alternativeprofile);
		}

		ConvertResult result = super.transcode(inStructions);
		if (inStructions.isWatermark())
		{
			inStructions.setInputFile(inStructions.getOutputFile());
			result = getWaterMarkTranscoder().convert(inStructions);
		}
		return result;
	}

	@Override
	protected ContentItem makeCustomInput(BaseTranscoder inImTranscoder, String inFormat, ConvertInstructions inStructions)
	{
		Asset asset = inStructions.getAsset();
//		if ("png".equals(asset.getFileFormat()))
//		{
//
//			ContentItem custom = getMediaArchive().getContent("/WEB-INF/data/" + getMediaArchive().getCatalogId() + "/generated/" + asset.getSourcePath() + "/customthumb.png");
//			ContentItem originalDocument = inStructions.getOriginalDocument();
//
//			if (!custom.exists())
//			{
//
//				List<String> com = new ArrayList<String>();
//
//				int finalwidth = asset.getInt("width");
//				int finalheight = asset.getInt("height");
//				//com.add("\\( -size " + finalwidth + "x" + finalheight + " tile:pattern:checkerboard \\)");
//				com.add("\\)");
//				
//				com.add("-size");
//				com.add(finalwidth + "x" + finalheight);
//				com.add("tile:pattern:checkerboard");
//				com.add("\\)");
//				
//				
//				com.add(originalDocument.getAbsolutePath());
//				com.add("-compose");
//				com.add("over");
//				com.add("-composite ");
//				com.add(custom.getAbsolutePath());
//				ExecResult execresult = getDefaultTranscoder().getExec().runExec("convert", com, true, 50000);
//				if(!execresult.isRunOk()) {
//						String output = execresult.getStandardOut();
//						if(output != null && output.contains("warning/tiff.c")) {
//							
//						}else {
//						//	execresult.setError(execresult.getStandardOut());
//						}
//				}
//			}
//
//		}

		return super.makeCustomInput(inImTranscoder, inFormat, inStructions);
	}

	private Page findProfileForAsset(ConvertInstructions inStructions)
	{
		String profiledescip = inStructions.getAsset().get("colorprofiledescription");
		if (profiledescip == null)
		{
			profiledescip = "";
		}

		if (profiledescip.contains("ProPhoto"))
		{

			Page profile = getMediaArchive().getPageManager().getPage("/system/components/conversions/ProPhoto.icc");

			return profile;

		}
		return null;
	}

	private ContentItem makeIndd(MediaTranscoder inExiftoolThumbTranscoder, ConvertInstructions inStructions)
	{
		Asset asset = inStructions.getAsset();
		if (asset == null)
		{
			return null;
		}
		String format = asset.getFileFormat();
		if ("indd".equalsIgnoreCase(format)) //TODO: Move to image
		{
			//log.info("Extracting thumb from "+ inStructions.getInputFile().getAbsolutePath() );

			ContentItem custom = getMediaArchive().getContent("/WEB-INF/data/" + getMediaArchive().getCatalogId() + "/generated/" + asset.getSourcePath() + "/customthumb.jpg");
			if (custom.exists())
			{
				return custom;
			}
			//if we have embdeded thumb 
			ConvertInstructions instructions = new ConvertInstructions(getMediaArchive());
			instructions.setForce(true);
			instructions.setInputFile(inStructions.getInputFile());
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
