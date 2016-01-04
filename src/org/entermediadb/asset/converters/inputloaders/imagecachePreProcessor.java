package org.entermediadb.asset.converters.inputloaders;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.creator.
import org.openedit.util.ExecResult;
import org.openedit.util.FileUtils;
import org.openedit.util.PathUtilities;

import com.openedit.page.Page;

public class imagecachePreProcessor extends BaseCreator implements MediaCreator
{
	private static final Log log = LogFactory.getLog(imagecachePreProcessor.class);

	public boolean canReadIn(MediaArchive inArchive, String inOutputType)
	{
		return true;
	}
	public ConvertResult convert(MediaArchive inArchive, Asset inAsset, Page input, ConvertInstructions inStructions)
	{
		if( !transparent  && ( inStructions.getMaxScaledSize() != null && offset == null ) ) //page numbers are 1 based
		{
			String page = null;
			if( inStructions.getPageNumber() > 1 )
			{
				page = "page" + inStructions.getPageNumber();
			}
			else
			{
				page = "";
			}

			Dimension box = inStructions.getMaxScaledSize();
			//			if (input == null && inStructions.getProperty("useinput")!=null)
			//			{
			//				input = getPageManager().getPage("/WEB-INF/data" + inArchive.getCatalogHome() + "/generated/" + inAsset.getSourcePath() + "/"+inStructions.getProperty("useinput") + page + ".jpg");
			//				if( !input.exists()  || input.length() < 2)
			//				{
			//					input = null;
			//				}
			//				else
			//				{
			//					autocreated = true;
			//				}
			//			}
			//			if( input == null &&  box.getWidth() < 300 )
			//			{
			//				input = getPageManager().getPage("/WEB-INF/data" + inArchive.getCatalogHome() + "/generated/" + inAsset.getSourcePath() + "/image640x480" + page + ".jpg");
			//				if( !input.exists()  || input.length() < 2)
			//				{
			//					input = null;
			//				}
			//				else
			//				{
			//					autocreated = true;
			//				}
			//			}
			if( input == null && box.getWidth() < 1025 )
			{
				//input = getPageManager().getPage("/WEB-INF/data" + inArchive.getCatalogHome() + "/generated/" + inAsset.getSourcePath() + "/image1920x1080" + page + ".jpg");
				//if( !input.exists() )
				//{
					if (transparent) {
						
						input = getPageManager().getPage("/WEB-INF/data" + inArchive.getCatalogHome() + "/generated/" + inAsset.getSourcePath() + "/image1024x768" + page + ".png");
					} else {
						
						input = getPageManager().getPage("/WEB-INF/data" + inArchive.getCatalogHome() + "/generated/" + inAsset.getSourcePath() + "/image1024x768" + page + ".jpg");
					}
				//}
				if( input.length() < 2 )
				{
					input = null;
				}
				else
				{
					autocreated = true;
				}
			}
		}

		boolean hascustomthumb = false;
		Page customthumb = null;

		if("png".equals(ext)){
			customthumb = getPageManager().getPage("/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + inAsset.getSourcePath() + "/customthumb.png");

		}	else
		{
			customthumb = getPageManager().getPage("/WEB-INF/data" + inArchive.getCatalogHome() + "/generated/" + inAsset.getSourcePath() + "/customthumb.jpg");
		}
		String filetype = inArchive.getMediaRenderType(inAsset.getFileFormat());
		if(customthumb.exists()){
			hascustomthumb = true;
			if(input == null && !"document".equals(filetype)){
				input = customthumb;
				log.info("Length was ${input.length()}");
				if( input.length() < 2 )
				{
					input = null;
				}
				else
				{
					autocreated = true;
				}
			}
		}

	}
	

	
}