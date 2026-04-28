package org.entermediadb.modules;

import org.entermediadb.asset.Category;
import org.openedit.data.BaseData;
import org.openedit.data.DataLoaded;
import org.openedit.data.SaveableData;

public class BaseDataEntity extends BaseData implements SaveableData, DataLoaded
{
	protected Category fieldCategoryRoot;

	public boolean hasRootCategory()
	{

		return getRootCategoryId() != null;
	}

	public String getRootCategoryId()
	{
		String catid = get("rootcategory");
		if (catid == null || catid.isEmpty())
		{
			return null;

		}

		return catid;
	}

}
