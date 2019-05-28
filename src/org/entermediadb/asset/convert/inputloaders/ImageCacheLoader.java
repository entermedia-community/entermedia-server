package org.entermediadb.asset.convert.inputloaders;

import java.awt.Dimension;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.InputLoader;
import org.openedit.repository.ContentItem;

public class ImageCacheLoader implements InputLoader
{
	private static final Log log = LogFactory.getLog(ImageCacheLoader.class);

	@Override
	public ContentItem loadInput(ConvertInstructions inStructions)
	{
		boolean useoriginal = Boolean.parseBoolean(inStructions.get("useoriginalasinput"));
		if (useoriginal)
		{
			return null;
		}

		String page = null;
		if (inStructions.getPageNumber() > 1)
		{
			page = "page" + inStructions.getPageNumber();
		}

		else
		{
			page = "";
		}
		ContentItem item = null;
		boolean isDocument = inStructions.isDocumentFormat();
		if (isDocument)
		{
			item = loadFile(inStructions, page, "png");
		}
		if (item == null)
		{
			if("png".equals( inStructions.getOutputExtension() ) )
			{
				if( "png".equals( inStructions.getAsset().getFileFormat() ) )
				{
					return null;//use original
				}
				item = loadFile(inStructions, page, "png");
			}
			item = loadFile(inStructions, page, "jpg");
		}

		return item;

	}

	protected ContentItem loadFile(ConvertInstructions inStructions, String page, String cachetype)
	{
		ContentItem input = null;

		if (inStructions.getPageNumber() == 1 && !inStructions.isCrop() ) //Can only crop jpg preview
		{
			input = inStructions.getMediaArchive().getContent("/WEB-INF/data/" + inStructions.getMediaArchive().getCatalogId() + "/generated/" + inStructions.getAssetSourcePath() + "/customthumb." + cachetype);
			if (input != null && input.getLength() > 2)
			{
				//TODO: Save the fact that we used a cached file
				return input;
			}
		}
		if (inStructions.getMaxScaledSize() != null && inStructions.getTimeOffset() == null) //page numbers are 1 based
		{
			Dimension box = inStructions.getMaxScaledSize();

			if (box.getWidth() < 1024) //Make sure we dont use the same file as the input and output
			{
				input = inStructions.getMediaArchive().getContent("/WEB-INF/data" + inStructions.getMediaArchive().getCatalogHome() + "/generated/" + inStructions.getAssetSourcePath() + "/image1024x768" + page + "." + cachetype);
				if (input.exists())
				{
					return input;
				}

			}
		}
		if ("png".equals(cachetype))
		{
			input = inStructions.getMediaArchive().getContent("/WEB-INF/data/" + inStructions.getMediaArchive().getCatalogId() + "/generated/" + inStructions.getAssetSourcePath() + "/document.pdf");
			if (input.exists())
			{
				return input;
			}
		}
		return null;
	}

}