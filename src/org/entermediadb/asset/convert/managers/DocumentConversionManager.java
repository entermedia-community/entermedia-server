package org.entermediadb.asset.convert.managers;

import org.entermediadb.asset.convert.BaseConversionManager;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.convert.MediaTranscoder;
import org.openedit.Data;

public class DocumentConversionManager extends BaseConversionManager
{
	//To create the file we need to Look for input in several places
	//CR 1024x768
	//Custom thumb
	//document.pdf
	//video.mp4
	//Original file
	
//	public ContentItem findOutputFile(ConvertInstructions inStructions)
//	{
//		StringBuffer path = new StringBuffer();
//
//		//legacy for people who want to keep their images in the old location
//		String prefix = inStructions.getProperty("pathprefix");
//		if( prefix != null)
//		{
//			path.append(prefix);
//		}
//		else
//		{
//			path.append("/WEB-INF/data");
//			path.append(getMediaArchive().getCatalogHome());
//			path.append("/generated/");
//		}
//		path.append(inStructions.getAssetSourcePath());
//		path.append("/");
//
//		String postfix = inStructions.getProperty("pathpostfix");
//		if( postfix != null)
//		{
//			path.append(postfix);
//		}
//		String cachefilename = inStructions.get("cachefilename");
//		if( cachefilename != null)
//		{
//			path.append(cachefilename);
//			return getMediaArchive().getContent( path.toString() );
//		}
//
////		if( "pdf".equals(inStructions.getOutputExtension()) )
////		{
////			path.append("document");
////		}
////		else
////		{
//			path.append(getCacheName()); //part of filename
////		}
////		if (inStructions.getMaxScaledSize() != null) // If either is set then
////		{
////			path.append(Math.round(inStructions.getMaxScaledSize().getWidth()));
////			path.append("x");
////			path.append(Math.round(inStructions.getMaxScaledSize().getHeight()));
////		}
//		if (inStructions.getPageNumber() > 1)
//		{
//			path.append("page");
//			path.append(inStructions.getPageNumber());
//		}
////		if(inStructions.getProperty("timeoffset") != null)
////		{
////			path.append("offset");
////			path.append(inStructions.getProperty("timeoffset"));
////		}
////		if(inStructions.isWatermark())
////		{
////			path.append("wm");
////		}
////		
////		if(inStructions.getProperty("colorspace") != null){
////			path.append(inStructions.getProperty("colorspace"));
////		}
////		if(inStructions.isCrop())
////		{
////			path.append("cropped");
////		}
//		if (inStructions.getOutputExtension() != null)
//		{
//			path.append("." + inStructions.getOutputExtension());
//		}
//		return getMediaArchive().getContent( path.toString() );
//	}

//	protected String getCacheName()
//	{
//		return "document";
//	}

//	protected ContentItem createCacheFile(ConvertInstructions inStructions, ContentItem input)
//	{
//			TranscodeTools creatorManager = inStructions.getMediaArchive().getTranscodeTools();
//			HashMap map = new HashMap();
//			map.put("prefwidth", "1024");
//			map.put("prefheight", "768");
//			map.put("outputextension", "pdf");
//			
//			Data preset = getMediaArchive().getPresetManager().getPresetByOutputName(inStructions.getMediaArchive(),"document","document.pdf");
//			ConvertInstructions proxyinstructions = createInstructions(inStructions.getAsset(), preset);
//
//			inStructions.setInputFile(inStructions.getOriginalDocument());
//			ConvertResult result = findTranscoderByPreset(preset).convert(proxyinstructions);
//			return result.getOutput();
//	}
	
	
	
	public ConvertResult transcode(ConvertInstructions inStructions)
	{
		//if output == jpg and no time offset - standard
		
		
		String fileFormat = inStructions.getAsset().getFileFormat();
		if(!"pdf".equals(fileFormat))
		{
			
			Data preset = getMediaArchive().getPresetManager().getPresetByOutputName(inStructions.getMediaArchive(),"document","document.pdf");
			ConvertInstructions instructions2 = inStructions.copy(preset);

			ConvertResult result = findTranscoder(instructions2).convert(instructions2);
			if(inStructions.getOutputRenderType().equals("document")){
				return result;
			}
			 inStructions.setInputFile(inStructions.getOutputFile());
			
		}
		else {
			inStructions.setInputFile(getMediaArchive().getOriginalDocument(inStructions.getAsset()).getContentItem());
		}
		
		
		//Now make the input image needed using the document as the input
		Data preset = getMediaArchive().getPresetManager().getPresetByOutputName(inStructions.getMediaArchive(),"document","image1024x768.jpg");
		
		ConvertInstructions instructions2 = inStructions.copy(preset);
		instructions2.setPageNumber(inStructions.getPageNumber());
		MediaTranscoder transcoder = findTranscoder(instructions2);
		
		ConvertResult result = transcoder.convertIfNeeded(instructions2);
		
		inStructions.setInputFile(result.getOutput());
		
		result = transcoder.convertIfNeeded(inStructions);
		if(!result.isComplete())
		{
			return result;
		}
		return result;
		
	}
	
	
	
	
	

	protected String getRenderType()
	{
		return "document";
	}
	
	
	

}
