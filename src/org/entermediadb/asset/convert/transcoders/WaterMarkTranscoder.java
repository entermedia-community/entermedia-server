package org.entermediadb.asset.convert.transcoders;

import java.util.ArrayList;
import java.util.List;

import org.entermediadb.asset.convert.BaseTranscoder;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.ContentItem;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;

public class WaterMarkTranscoder extends BaseTranscoder
{

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



	public String getWaterMarkPath(String inThemePrefix)
	{
		Page water = getPageManager().getPage(inThemePrefix );
		String fieldWatermarkPath = water.getContentItem().getAbsolutePath(); // Strings for performance
		return fieldWatermarkPath;
	}



	@Override
	public ConvertResult convert(ConvertInstructions inStructions)
	{
		ContentItem newinput = inStructions.getInputFile();
		ContentItem newouput = inStructions.getOutputFile();
		// composite -dissolve 15 -tile watermark.png src.jpg dst.jpg
		List<String> com = new ArrayList<String>();
		setValue("dissolve", "100", inStructions, com);

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
		
		String watermarkfile = inStructions.get("watermarkpath");  //Can be anyplace in the webapp
		
		if( watermarkfile == null)
		{
			watermarkfile = inStructions.getMediaArchive().getCatalogSettingValue("watermarkpath");
		}
		
		if( watermarkfile == null)
		{
			watermarkfile =  inStructions.getMediaArchive().getCatalogHome() + "/images/watermark.png";
		}		
		String abs = getWaterMarkPath(watermarkfile);
		com.add(abs);
		com.add(newinput.getAbsolutePath());
		com.add(newouput.getAbsolutePath());
		ExecResult result = getExec().runExec("composite", com, inStructions.getConversionTimeout());
		boolean ok = result.isRunOk();
		ConvertResult completed = new ConvertResult();
		completed.setOk(ok);
		completed.setComplete(true);
		completed.setOutput(newouput);
		return completed;
	}
}