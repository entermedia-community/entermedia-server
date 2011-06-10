package org.openedit.entermedia.creator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openedit.Data;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;

import com.openedit.page.Page;
import com.openedit.page.PageProperty;
import com.openedit.util.ExecResult;

public class ExifToolThumbCreator extends BaseImageCreator
{
	public boolean canReadIn(MediaArchive inArchive, String inInputType)
	{
		return inInputType.endsWith("indd");
	}

	public ConvertResult convert(MediaArchive inArchive, Asset inAsset, Page inOut, ConvertInstructions inStructions)
	{
		ConvertResult result = new ConvertResult();
		result.setOk(false);
		
		Page input = inArchive.findOriginalMediaByType("image",inAsset);
		if( input != null)
		{
			List command = new ArrayList();
			//command.add("-b");
			//command.add("-ThumbnailImage");
			command.add(input.getContentItem().getAbsolutePath());
			//command.add("-o");
			command.add(inOut.getContentItem().getAbsolutePath());
			
			new File( inOut.getContentItem().getAbsolutePath() ).getParentFile().mkdirs();
			//FileInputStream out = new FileOutputStream(inOut.getContentItem().getAbsolutePath());
			ExecResult done = getExec().runExec("exiftoolthumb",command);
			
			result.setOk(done.isRunOk());
		}
		return result;
	}

	
}
