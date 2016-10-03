package org.entermediadb.projects;

import org.entermediadb.asset.Category;
import org.openedit.Data;
import org.openedit.data.BaseData;
import org.openedit.data.SaveableData;

public class LibraryCollection extends BaseData implements SaveableData
{
	protected Data fieldLibrary;
	protected Category fieldCategoryRoot;
	protected int fieldAssetCount;
	
	public Data getLibrary()
	{
		return fieldLibrary;
	}
	public void setLibrary(Data inLibrary)
	{
		fieldLibrary = inLibrary;
	}

	public int getAssetCount()
	{
		return fieldAssetCount;
	}
	public void setAssetCount(int inCount)
	{
		fieldAssetCount = inCount;
	}

	public long getCurentRevision()
	{
		Object obj = getValue("revisions");
		if( obj == null)
		{
			obj = 0;
		}

		Long revisionnumber = null;
		if( obj instanceof Integer)
		{
			revisionnumber = (long)(Integer)obj;
		}
		else
		{
			revisionnumber = (Long)obj;
		}

		return revisionnumber;
	}
	public boolean hasRootCategory()
	{
		return getRootCategoryId() != null;
	}
	public String getRootCategoryId()
	{
		return get("rootcategory");
	}


}