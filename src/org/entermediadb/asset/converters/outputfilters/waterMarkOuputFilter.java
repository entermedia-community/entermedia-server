package org.entermediadb.asset.converters.outputfilters;

import org.entermediadb.asset.creator.ImageOutputFilter;
import org.openedit.page.Page;
import org.openedit.util.PathUtilities;


public class waterMarkOuputFilter implements ImageOutputFilter
{

public String getWaterMarkPath(String inThemePrefix)
{
	if (fieldWaterMarkPath == null)
	{
		Page water = getPageManager().getPage(inThemePrefix + "/images/watermark.png");
		fieldWaterMarkPath = water.getContentItem().getAbsolutePath(); // Strings for performance
	}
	return fieldWaterMarkPath;
}

if(inStructions.isWatermark())
{
	//Page inputPage = getPageManager().getPage(inStructions.getAssetSourcePath());
	String fullInputPath = input.getContentItem().getAbsolutePath();
	String tmpoutputpath = PathUtilities.extractPagePath(outputpath) + ".wm.jpg";
	applyWaterMark(inArchive, fullInputPath, tmpoutputpath, inStructions);
	input = getPageManager().getPage(tmpoutputpath);
}

}