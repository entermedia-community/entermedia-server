package org.entermediadb.asset.convert.inputloaders;

import java.awt.Dimension;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.InputLoader;
import org.openedit.repository.ContentItem;

public class VideoCacheLoader implements InputLoader
{
	private static final Log log = LogFactory.getLog(VideoCacheLoader.class);

	@Override
	public ContentItem loadInput(ConvertInstructions inStructions)
	{
		boolean useoriginal = Boolean.parseBoolean(inStructions.get("useoriginalasinput"));
    	if(useoriginal)
    	{
    		return null;
    	}

		ContentItem input = null;

		if (inStructions.getOutputRenderType().equals("image"))
		{

			if (inStructions.getMaxScaledSize() != null) //page numbers are 1 based
			{
				Dimension box = inStructions.getMaxScaledSize();

				if (box.getWidth() < 1501)
				{
					if (inStructions.getTimeOffset() == null)
					{
						input = inStructions.getMediaArchive().getContent("/WEB-INF/data" + inStructions.getMediaArchive().getCatalogHome() + "/generated/" + inStructions.getAssetSourcePath() + "/image1900x1080.jpg");
						if(! input.exists() )
						{
							input = inStructions.getMediaArchive().getContent("/WEB-INF/data" + inStructions.getMediaArchive().getCatalogHome() + "/generated/" + inStructions.getAssetSourcePath() + "/image1500x1500.jpg"); //Remove this
						}
						if(! input.exists() )
						{
							input = inStructions.getMediaArchive().getContent("/WEB-INF/data" + inStructions.getMediaArchive().getCatalogHome() + "/generated/" + inStructions.getAssetSourcePath() + "/image1024x768.jpg"); //Old!
						}
					}
					else
					{
						input = inStructions.getMediaArchive().getContent("/WEB-INF/data" + inStructions.getMediaArchive().getCatalogHome() + "/generated/" + inStructions.getAssetSourcePath() + "/image1900x1080offset" + inStructions.getTimeOffset() + ".jpg");
						if(! input.exists() )
						{
							input = inStructions.getMediaArchive().getContent("/WEB-INF/data" + inStructions.getMediaArchive().getCatalogHome() + "/generated/" + inStructions.getAssetSourcePath() + "/image1900x1080offset" + inStructions.getTimeOffset() + ".jpg");
						}
					}
					if (!input.exists())
					{
						input = null;
					}
				}
			}
			if ((input == null || input.getLength() < 2) )
			{
				input = inStructions.getMediaArchive().getContent("/WEB-INF/data/" + inStructions.getMediaArchive().getCatalogId() + "/generated/" + inStructions.getAssetSourcePath() + "/customthumb.jpg");
				if (input != null && input.getLength() < 2 && input.exists())
				{
					//TODO: Save the fact that we used a cached file
					return input;
				}
			} else{
				return input;
			}
		
		} 
		
		input = inStructions.getMediaArchive().getContent("/WEB-INF/data/" + inStructions.getMediaArchive().getCatalogId() + "/generated/" + inStructions.getAssetSourcePath() + "/video.mp4");
		if (input == null || input.getLength() < 2 )
		{
			//TODO: Save the fact that we used a cached file
			//try HLS?
			input = inStructions.getMediaArchive().getContent("/WEB-INF/data/" + inStructions.getMediaArchive().getCatalogId() + "/generated/" + inStructions.getAssetSourcePath() + "/video.m3u8/1080/video.m3u8");
			if (!input.exists() || input.getLength() == 0 )
			{
				input = inStructions.getMediaArchive().getContent("/WEB-INF/data/" + inStructions.getMediaArchive().getCatalogId() + "/generated/" + inStructions.getAssetSourcePath() + "/video.m3u8/720/video.m3u8");
			}
			if (input == null || input.getLength() < 2 )
			{
				return null;
			}
		}
		else
		{
			//inStructions.setUsedCache(true);
		}
		return input;

	}

}