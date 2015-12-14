package org.entermediadb.asset.creator;

import java.awt.Dimension;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;

public class ConvertToImageInstructions extends ConvertInstructions
{
	public ConvertToImageInstructions(MediaArchive inArchive)
	{
		super(inArchive);
	}

	@Override
	public void loadSettings(Map inProperties, Data inPreset)
	{
		setSettings(inProperties);
		loadPreset(inPreset);

		String pageString = getProperty("pagenum");
		// changed to take a request parameter.
		if( pageString != null && pageString.length() == 0 )
		{
			pageString = null;
		}
		if (pageString != null)
		{
			setPageNumber(Integer.parseInt(pageString));
		}
		
		// Create temporary location for previews
		String w = getProperty("prefwidth");
		String h = getProperty("prefheight");

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
			setMaxScaledSize(new Dimension(Integer.parseInt(w), Integer.parseInt(h)));
		}
		
		String crop = getProperty("crop");
		if(crop != null && Boolean.parseBoolean(crop))
		{
			setCrop(Boolean.parseBoolean(crop));
		}
		
		String watermark = getProperty("canforcewatermarkasset");
		if (watermark != null)
		{
			setWatermark(Boolean.valueOf(watermark));
		}
		
		String watermarkselected = getProperty("watermark");
		if (watermarkselected != null)
		{
			setWatermark(Boolean.valueOf(watermarkselected));
		}
		calculateOutputPath(inPreset);
		
	}	
	public void calculateOutputPath(Data inPreset)
	{
		StringBuffer path = new StringBuffer();

		//legacy for people who want to keep their images in the old location
		String prefix = getProperty("pathprefix");
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
		path.append(getAssetSourcePath());
		path.append("/");

		String postfix = getProperty("pathpostfix");
		if( postfix != null)
		{
			path.append(postfix);
		}
		if( "pdf".equals(getOutputExtension()) )
		{
			path.append("document");
		}
		else
		{
			path.append("image"); //part of filename
		}
		if (getMaxScaledSize() != null) // If either is set then
		{
			path.append(Math.round(getMaxScaledSize().getWidth()));
			path.append("x");
			path.append(Math.round(getMaxScaledSize().getHeight()));
		}
		if (getPageNumber() > 1)
		{
			path.append("page");
			path.append(getPageNumber());
		}
		if(getProperty("timeoffset") != null)
		{
			path.append("offset");
			path.append(getProperty("timeoffset"));
		}
		if(isWatermark())
		{
			path.append("wm");
		}
		
		if(getProperty("colorspace") != null){
			path.append(getProperty("colorspace"));
		}
		if(isCrop())
		{
			path.append("cropped");
		}
		if (getOutputExtension() != null)
		{
			path.append("." + getOutputExtension());
		}
		setOutputPath(path.toString());
	}


}
