package org.entermediadb.asset.scanner;

import java.util.List;

public class Dvd
{
	protected String fieldTitle;
	protected String fieldPath;
	protected String fieldLastModified;
	protected int fieldTotalChapters;
	protected List fieldChapterNames;

	public String getTitle()
	{
		return fieldTitle;
	}

	public void setTitle(String inTitle)
	{
		fieldTitle = inTitle;
	}

	public String getPath()
	{
		return fieldPath;
	}

	public void setPath(String inPath)
	{
		fieldPath = inPath;
	}

	public String getLastModified()
	{
		return fieldLastModified;
	}

	public void setLastModified(String inLastModified)
	{
		fieldLastModified = inLastModified;
	}

	public int getTotalChapters()
	{
		return fieldTotalChapters;
	}

	public void setTotalChapters(int inTotalChapters)
	{
		fieldTotalChapters = inTotalChapters;
	}

	public List getChapterNames()
	{
		return fieldChapterNames;
	}

	public void setChapterNames(List inChapterNames)
	{
		fieldChapterNames = inChapterNames;
	}

}
