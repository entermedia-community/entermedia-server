package org.entermediadb.asset.convert.managers;

import java.util.HashMap;

import org.entermediadb.asset.convert.BaseConversionManager;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.convert.MediaCreator;
import org.entermediadb.asset.convert.MediaTranscoder;
import org.openedit.repository.ContentItem;

public class ImageConversionManager extends BaseConversionManager
{
	//To create the file we need to Look for input in several places
	//CR 1024x768
	//Custom thumb
	//document.pdf
	//video.mp4
	//Original file
	
	public ContentItem findOutputFile(ConvertInstructions inStructions)
	{
		StringBuffer path = new StringBuffer();

		//legacy for people who want to keep their images in the old location
		String prefix = inStructions.getProperty("pathprefix");
		if( prefix != null)
		{
			path.append(prefix);
		}
		else
		{
			path.append("/WEB-INF/data");
			path.append(getMediaArchive().getCatalogHome());
			path.append("/generated/");
		}
		path.append(inStructions.getAssetSourcePath());
		path.append("/");

		String postfix = inStructions.getProperty("pathpostfix");
		if( postfix != null)
		{
			path.append(postfix);
		}
		if( "pdf".equals(inStructions.getOutputExtension()) )
		{
			path.append("document");
		}
		else
		{
			path.append("image"); //part of filename
		}
		if (inStructions.getMaxScaledSize() != null) // If either is set then
		{
			path.append(Math.round(inStructions.getMaxScaledSize().getWidth()));
			path.append("x");
			path.append(Math.round(inStructions.getMaxScaledSize().getHeight()));
		}
		if (inStructions.getPageNumber() > 1)
		{
			path.append("page");
			path.append(inStructions.getPageNumber());
		}
		if(inStructions.getProperty("timeoffset") != null)
		{
			path.append("offset");
			path.append(inStructions.getProperty("timeoffset"));
		}
		if(inStructions.isWatermark())
		{
			path.append("wm");
		}
		
		if(inStructions.getProperty("colorspace") != null){
			path.append(inStructions.getProperty("colorspace"));
		}
		if(inStructions.isCrop())
		{
			path.append("cropped");
		}
		if (inStructions.getOutputExtension() != null)
		{
			path.append("." + inStructions.getOutputExtension());
		}
		return getMediaArchive().getContent( path.toString() );
	}

	
	public ConvertResult createOutput(ConvertInstructions inStructions, ContentItem input)
    {
    	
		if(input == null){
			input = createInput(inStructions, input);
			

		}
		
		
		
		if(input == null){
    		input = inStructions.getOriginalDocument();
    	}
		
		
		
		inStructions.setInputFile(input);


		
    	return getMediaTranscoder().convert(inStructions);
    	
    }


	protected ContentItem createInput(ConvertInstructions inStructions, ContentItem input)
	{
		
		
			MediaCreator creatorManager = inStructions.getMediaArchive().getMediaCreator();
			MediaTranscoder c = creatorManager.getMediaCreatorByOutputFormat("jpg");
			HashMap map = new HashMap();
			map.put("prefwidth", "1024");
			map.put("prefheight", "768");
			ConvertInstructions cacheInsructions = createInstructions(map, inStructions.getAsset(), "jpg");
			inStructions.setInputFile(inStructions.getOriginalDocument());

	    	return getMediaTranscoder().convert(inStructions).getOutput();
		
	}

	
	
	

}
