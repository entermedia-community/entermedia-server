package org.entermediadb.asset.search;

import org.openedit.repository.ContentItem;
import org.openedit.util.PathProcessor;

public abstract class AssetProcessor extends PathProcessor
{

	protected String makeSourcePath(ContentItem inItem)
	{
		String path = inItem.getPath();
		path = path.substring(getRootPath().length());
		if (path.endsWith("/data.xml"))
		{
			path = path.substring(0, path.length() - "/data.xml".length());
		}
		else if (path.endsWith(".xconf")) //take off xconf
		{
			path = path.substring(0, path.length() - ".xconf".length());			
		}
		path = path.replace('\\', '/');
		return path;
	}

	public boolean acceptFile(ContentItem inFile)
	{
		String path = inFile.getPath();
		if (path.endsWith("data.xml"))
		{
			return true;
		}
		return false;
	}

	public boolean acceptDir(ContentItem inDir)
	{
		String sourcePath = makeSourcePath(inDir);
		return (super.acceptDir(inDir) && !sourcePath.equals("images") && !sourcePath.equals("search") && !sourcePath.equals("index"));
	}

}
