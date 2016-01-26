package org.entermediadb.asset.convert.inputloaders;

import java.awt.Dimension;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.InputLoader;
import org.openedit.repository.ContentItem;
import org.openedit.util.Exec;

public class ImageCacheLoader implements InputLoader
{
	private static final Log log = LogFactory.getLog(ImageCacheLoader.class);

	@Override
	public ContentItem loadInput(ConvertInstructions inStructions)
	{
		boolean isDocument = inStructions.isDocumentFormat();		
		String cachetype = "jpg";
		if( isDocument )
		{
			 cachetype = "png";
		}
		String page = null;
		if( inStructions.getPageNumber() > 1 )
		{
			page = "page" + inStructions.getPageNumber();
		}
		else
		{
			page = "";
		}
		ContentItem item = loadFile(inStructions, page, cachetype);
		if(item == null && isDocument){
			 item = loadFile(inStructions, page, "jpg");
		}
		
		
		return item;
		

	}
	
	
	protected ContentItem loadFile(ConvertInstructions inStructions, String page, String cachetype)
	{
		ContentItem input = null;
		if( inStructions.getMaxScaledSize() != null && inStructions.getTimeOffset() == null ) //page numbers are 1 based
		{
			Dimension box = inStructions.getMaxScaledSize();

			if( box.getWidth() < 1025 )
			{
				input = inStructions.getMediaArchive().getContent("/WEB-INF/data" +  inStructions.getMediaArchive().getCatalogHome() + "/generated/" + inStructions.getAssetSourcePath() + "/image1024x768" + page + "." + cachetype);
			}
		}

		if( (input == null || input.getLength() < 2) && inStructions.getPageNumber() == 1 )
		{
			input = inStructions.getMediaArchive().getContent("/WEB-INF/data/" + inStructions.getMediaArchive().getCatalogId() + "/generated/" + inStructions.getAssetSourcePath() + "/customthumb." + cachetype);
			if( input != null && input.getLength() < 2 && input.exists())
			{
				//TODO: Save the fact that we used a cached file
				return input;
			}
		}
		else
		{
			return input;
		}
		return null;
	}
	@Override
	public void setExec(Exec inExec)
	{
		// TODO Auto-generated method stub
		
	}
	

	
}