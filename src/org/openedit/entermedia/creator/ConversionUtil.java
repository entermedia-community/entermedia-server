package org.openedit.entermedia.creator;

import java.awt.Dimension;
import java.text.DecimalFormat;

import org.openedit.data.SearcherManager;
import org.openedit.entermedia.MediaArchive;

public class ConversionUtil {
	
	protected MediaArchive fieldMediaArchive;
	protected SearcherManager fieldSearcherManager;
	
	public MediaArchive getMediaArchive()
	{
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}
	
	public Dimension getConvertPresetDimensions(String inPrecetId){
		Dimension dimension = null;
		
		return dimension;
	}
	
	public String getConvertPresetAspectRatio(String inPrecetId){
		Dimension dimension = getConvertPresetDimensions(inPrecetId);
		double height = dimension.getHeight();
		double width = dimension.getWidth();
		DecimalFormat format = new DecimalFormat("#.000");
		String ratio = format.format(width / height) + ":1";
		return ratio;
	}
	
	
	

}
