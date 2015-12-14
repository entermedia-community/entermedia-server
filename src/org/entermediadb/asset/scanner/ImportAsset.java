package org.entermediadb.asset.scanner;

import java.io.File;

public class ImportAsset
{
	protected String fieldSourcePath; // 
	protected String fieldImportDirectoryRoot;
	protected String fieldCategoryDirectoryRoot; //  
	protected File fieldFile;

	public String getSourcePath()
	{
		return fieldSourcePath;
	}

	/**
	 * /labels/img123.jpg
	 * 
	 * @param inSourcePath
	 */
	public void setSourcePath(String inSourcePath)
	{
		fieldSourcePath = inSourcePath;
	}

	public String getImportDirectoryRoot()
	{
		return fieldImportDirectoryRoot;
	}

	/**
	 * // home/cburkey/images
	 * 
	 * @param inImportDirectoryRoot
	 */
	public void setImportDirectoryRoot(String inImportDirectoryRoot)
	{
		fieldImportDirectoryRoot = inImportDirectoryRoot;
	}

	public String getCategoryDirectoryRoot()
	{
		return fieldCategoryDirectoryRoot;
	}

	/**
	 * /testcatalog/assets/photos
	 * 
	 * @param inCategoryDirectoryRoot
	 */
	public void setCategoryDirectoryRoot(String inCategoryDirectoryRoot)
	{
		fieldCategoryDirectoryRoot = inCategoryDirectoryRoot;
	}

	public File getFile()
	{
		return fieldFile;
	}

	public void setFile(File inFile)
	{
		fieldFile = inFile;
	}

}
