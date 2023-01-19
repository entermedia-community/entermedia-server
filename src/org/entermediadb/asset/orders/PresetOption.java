package org.entermediadb.asset.orders;

import java.util.ArrayList;
import java.util.Collection;

import org.openedit.Data;

public class PresetOption
{
	protected Collection fieldOrderItems = new ArrayList();
	protected Collection fieldDowloadPaths = new ArrayList();

	public Collection getDowloadPaths()
	{
		return fieldDowloadPaths;
	}

	public void setDowloadPaths(Collection inDowloadPaths)
	{
		fieldDowloadPaths = inDowloadPaths;
	}
	public void addDownloadPath(String inPath)
	{
		getDowloadPaths().add(inPath);
	}
	protected Data fieldPreset; //render type
	public Data getPreset()
	{
		return fieldPreset;
	}

	public void setPreset(Data inPreset)
	{
		fieldPreset = inPreset;
	}
	public Collection getOrderItems()
	{
		return fieldOrderItems;
	}

	public void setOrderItems(Collection inItems)
	{
		fieldOrderItems = inItems;
	}
	public void addOrderItem(Data inItem)
	{
		getOrderItems().add(inItem);
	}

}
