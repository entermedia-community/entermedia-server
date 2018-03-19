package org.entermediadb.asset.convert.managers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.convert.BaseConversionManager;
import org.entermediadb.asset.convert.BaseTranscoder;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.convert.MediaTranscoder;
import org.entermediadb.asset.convert.transcoders.CMYKTranscoder;
import org.openedit.Data;
import org.openedit.repository.ContentItem;

public class DocumentConversionManager extends BaseConversionManager
{
	private static final Log log = LogFactory.getLog(DocumentConversionManager.class);
	
	protected BaseTranscoder fieldCMYKTranscoder;
	protected BaseTranscoder fieldGsTranscoder;
	
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

	public BaseTranscoder getGsTranscoder()
	{
		if (fieldGsTranscoder == null)
		{
			fieldGsTranscoder = (BaseTranscoder)getMediaArchive().getModuleManager().getBean(getMediaArchive().getCatalogId(),"gsTranscoder");
		}
		return fieldGsTranscoder;
	}

	
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
			//Lets always have a PDF version of all document formats?
			Data preset = getMediaArchive().getPresetManager().getPresetByOutputName(inStructions.getMediaArchive(),"document","document.pdf");
			ConvertInstructions instructions2 = inStructions.copy(preset);

			MediaTranscoder findTranscoder = findTranscoder(instructions2);
			ConvertResult result = findTranscoder.convertIfNeeded(instructions2);
			//log.info("Created document.pdf");
			if(inStructions.getOutputExtension().equals("pdf")){
				return result; //Why shortcut?
			}
			inStructions.setInputFile(instructions2.getOutputFile());
		}
		else if( inStructions.getInputFile() == null)
		{
	    	ContentItem tmpinput = null;
			if( tmpinput == null && inStructions.getPageNumber() == 1)
			{	
				tmpinput = inStructions.getMediaArchive().getContent("/WEB-INF/data/" + inStructions.getMediaArchive().getCatalogId() + "/generated/" + inStructions.getAssetSourcePath() + "/customthumb.png");
			}	
			if( tmpinput == null || !tmpinput.exists() )
			{
				tmpinput = getMediaArchive().getOriginalDocument(inStructions.getAsset()).getContentItem();
			}	
			inStructions.setInputFile(tmpinput);
		}

		ContentItem tmpinput = makeCustomInput(getCMYKTranscoder(),"png",inStructions);
		if( tmpinput != null)
		{
			inStructions.setInputFile(tmpinput);
		}
		
		
		//Step 2 make PNG
		//Now make the input image needed using the document as the input
		
		Data preset = getMediaArchive().getPresetManager().getPresetByOutputName(inStructions.getMediaArchive(),"document","image1024x768.png");
		if(preset == null) {
			 preset = getMediaArchive().getPresetManager().getPresetByOutputName(inStructions.getMediaArchive(),"document","image1024x768.jpg");
		
		}
		
		
		if( preset == null) //Legacy check
		{
			preset = getMediaArchive().getPresetManager().getPresetByOutputName(inStructions.getMediaArchive(),"document","image1500x1500.png");
		}	

		ConvertInstructions instructions2 = inStructions.copy(preset);
		instructions2.setPageNumber(inStructions.getPageNumber());
		instructions2.setAsset(inStructions.getAsset());

		if( tmpinput == null)
		{ //Not CMYK, is PDF
			if("pdf".equals(fileFormat))
			{
				instructions2.setInputFile(getMediaArchive().getOriginalDocument(inStructions.getAsset()).getContentItem());
				ConvertResult pre = getGsTranscoder().convertIfNeeded( instructions2 ); //pre convert
				instructions2.setInputFile(pre.getOutput());
				instructions2.setForce(true);

			}
		}
		
		MediaTranscoder transcoder = findTranscoder(instructions2);
		
		ConvertResult result = transcoder.convertIfNeeded(instructions2);
		
		inStructions.setInputFile(result.getOutput());
		
		//Step 3 Make jpg
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
