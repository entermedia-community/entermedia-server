package org.entermediadb.asset.convert.outputfilters;

import java.util.ArrayList;
import java.util.List;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.convert.OutputFilter;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.ContentItem;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;

public class WaterMarkOuputFilter implements OutputFilter
{

	protected String fieldWatermarkPath;
	protected PageManager fieldPageManager;
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}



	protected Exec fieldExec;
	
	
	
	public Exec getExec()
	{
		return fieldExec;
	}



	public void setExec(Exec inExec)
	{
		fieldExec = inExec;
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



	@Override
	public ConvertResult filterOutput(ConvertInstructions inStructions)
	{
		if(inStructions.isWatermark())
		{
			ContentItem newinput = inStructions.getOutputFile();
			ContentItem newouput = inStructions.getMediaArchive().getContent(newinput.getPath() + ".wm.jpg");
			// composite -dissolve 15 -tile watermark.png src.jpg dst.jpg
			List<String> com = new ArrayList<String>();
			com.add("-dissolve");
			com.add("100");

			String placement = inStructions.getWatermarkPlacement();
			if(placement == null)
			{
				placement = "tile";//"SouthWest";
			}

			if (placement.equals("tile"))
			{
				com.add("-tile");

			}
			else
			{
				com.add("-gravity");
				com.add(placement);
			}
			com.add(getWaterMarkPath(inStructions.getMediaArchive().getThemePrefix()));
			com.add(newinput.getAbsolutePath());
			com.add(newouput.getAbsolutePath());
			ExecResult result = getExec().runExec("composite", com, inStructions.getConversionTimeout());
			boolean ok = result.isRunOk();
			ConvertResult completed = new ConvertResult();
			completed.setOk(ok);
			completed.setOutput(newouput);
			return completed;
		}
		return null;
	}
}