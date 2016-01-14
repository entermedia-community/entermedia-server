package org.entermediadb.asset.convert.managers;

import org.entermediadb.asset.convert.BaseConversionManager;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.openedit.repository.ContentItem;

public class VideoConversionManager extends BaseConversionManager
{
	@Override
	public ContentItem findOutputFile(ConvertInstructions inStructions)
	{
		StringBuffer outputpage = new StringBuffer();
		outputpage.append("/WEB-INF/data/" );
		outputpage.append(getMediaArchive().getCatalogId());
		outputpage.append("/generated/" );
		outputpage.append(inStructions.getAssetSourcePath() );
		outputpage.append("/" );
		String cachefilename = inStructions.get("cachefilename");
		if( cachefilename == null)
		{
			outputpage.append(getCacheName());
			if( inStructions.isWatermark() )
			{
				outputpage.append("wm");
			}
			//if( inStructions.get)
			outputpage.append(".mp4");
		}
		else
		{
			outputpage.append(cachefilename);
		}
//		String output = inPreset.get("outputfile");
//		int pagenumber = inStructions.getPageNumber();
//		if( pagenumber > 1 )
//		{
//			String name = PathUtilities.extractPageName(output);
//			String ext = PathUtilities.extractPageType(output);
//			output = name + "page" + pagenumber + "." + ext;
//		}
//		outputpage.append(output);

		return getMediaArchive().getContent( outputpage.toString() );
	}

	@Override
	protected ContentItem createCacheFile(ConvertInstructions inStructions, ContentItem inInput)
	{
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	protected String getCacheName()
	{
		return "video";
	}

}
