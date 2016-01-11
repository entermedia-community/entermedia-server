package org.entermediadb.asset.convert.outputfilters;

import org.entermediadb.asset.convert.ImageOutputFilter;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;

public class waterMarkOuputFilter implements ImageOutputFilter
{

	protected String fieldWatermarkPath;
	protected PageManager fieldPageManager;
	
	
	
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}



	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}



	public String getWatermarkPath()
	{
		return fieldWatermarkPath;
	}



	public void setWatermarkPath(String inWatermarkPath)
	{
		fieldWatermarkPath = inWatermarkPath;
	}



	public String getWaterMarkPath(String inThemePrefix)
	{
		if (fieldWatermarkPath == null)
		{
			Page water = getPageManager().getPage(inThemePrefix + "/images/watermark.png");
			fieldWatermarkPath = water.getContentItem().getAbsolutePath(); // Strings for performance
		}
		return fieldWatermarkPath;
	}

	//	if(inStructions.isWatermark())
	//
	//	{
	//		//Page inputPage = getPageManager().getPage(inStructions.getAssetSourcePath());
	//		String fullInputPath = input.getContentItem().getAbsolutePath();
	//		String tmpoutputpath = PathUtilities.extractPagePath(outputpath) + ".wm.jpg";
	//		applyWaterMark(inArchive, fullInputPath, tmpoutputpath, inStructions);
	//		input = getPageManager().getPage(tmpoutputpath);
	//	}

}