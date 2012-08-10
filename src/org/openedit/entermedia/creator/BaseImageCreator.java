package org.openedit.entermedia.creator;

import java.awt.Dimension;
import java.util.Iterator;
import java.util.Map;

import org.openedit.Data;
import org.openedit.entermedia.MediaArchive;

public abstract class BaseImageCreator extends BaseCreator
{
	public ConvertInstructions createInstructions(Map inProperties, MediaArchive inArchive, String inOutputType, String inSourcePath)
	{
		ConvertInstructions inStructions = new ConvertInstructions();
		
		//Ok the source path that was passed in here will never be a folder. It is allways a pointer to the attachment
//		String attachment = inReq.getRequestParameter("attachment");
//		if( attachment != null)
//		{
//			inSourcePath = inSourcePath + "/" + attachment;
//		}		
		
		inStructions.setOutputExtension(inOutputType);
		String pageString = (String)inProperties.get("pagenum");
		// changed to take a request parameter.
		if( pageString != null && pageString.length() == 0 )
		{
			pageString = null;
		}
		if (pageString != null)
		{
			inStructions.setPageNumber(Integer.parseInt(pageString));
		}
		//Maybe this is too much stuff? 
		//Yes it is.
		for (Iterator iterator = inProperties.keySet().iterator(); iterator.hasNext();)
		{
			String key = (String) iterator.next();
			Object value = inProperties.get(key);
			if( value == null)
			{
				continue;
			}
			
			//I think we need some objects dont we?
			if( (value instanceof Boolean ))
			{
				value = value.toString();
			}
			if( value instanceof String)
			{
				inStructions.addProperty(key,(String)value);
			}
		}


		
		
		// Create temporary location for previews
		String w = inStructions.getProperty("prefwidth");
		String h = inStructions.getProperty("prefheight");

		if (w != null || h != null) // If either is set then set both
		{
			if (w == null || "".equals(w))
			{
				w = "10000";
			}
			if (h == null || "".equals(h))
			{
				h = "10000";
			}
			inStructions.setMaxScaledSize(new Dimension(Integer.parseInt(w), Integer.parseInt(h)));
		}
		
		String crop = inStructions.getProperty("crop");
		if(crop != null && Boolean.parseBoolean(crop))
		{
			inStructions.setCrop(Boolean.parseBoolean(crop));
		}
		
		String watermark = inStructions.getProperty("canforcewatermarkasset");
		if (watermark != null)
		{
			inStructions.setWatermark(Boolean.valueOf(watermark));
		
		}
		
		String watermarkselected = inStructions.getProperty("watermark");
		if (watermarkselected != null)
		{
			inStructions.setWatermark(Boolean.valueOf(watermarkselected));
		
		}
		
		
		

		inStructions.setAssetSourcePath(inSourcePath);

//		String subfolder = (String)inProperties.get("subfolder");
//		if(subfolder != null)
//		{
//			populateOutputPath(inArchive, inStructions, subfolder);
//		}
//		else
//		{
			//populateOutputPath(inArchive,inStructions);
//		}
		
		return inStructions;
	}

	public String populateOutputPath(MediaArchive inArchive, ConvertInstructions inStructions)
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
			path.append(inArchive.getCatalogHome());
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
			path.append(inStructions.get("colorspace"));
			
		}
		if(inStructions.isCrop())
		{
			path.append("cropped");
		}
		if (inStructions.getOutputExtension() != null)
		{
			path.append("." + inStructions.getOutputExtension());
		}
		inStructions.setOutputPath(path.toString());
		return inStructions.getOutputPath();
	}


}
